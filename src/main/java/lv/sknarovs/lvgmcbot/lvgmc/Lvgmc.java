package lv.sknarovs.lvgmcbot.lvgmc;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client for the LVĢMC open data API behind videscentrs.lvgmc.lv, plus the list of forecast cities.
 */
public final class Lvgmc {

  /** Forecast times are local Latvian time; observation times are UTC. */
  public static final ZoneId ZONE = ZoneId.of("Europe/Riga");

  /** Offered as buttons by /city. */
  public static final List<String> POPULAR_CITIES =
      List.of(
          "Rīga",
          "Daugavpils",
          "Liepāja",
          "Jelgava",
          "Jūrmala",
          "Ventspils",
          "Rēzekne",
          "Ogre",
          "Valmiera",
          "Jēkabpils");

  /** Cities LVĢMC publishes forecasts for. */
  public static final List<String> FORECAST_CITIES =
      List.of(
          "Ainaži",
          "Aizkraukle",
          "Aizpute",
          "Aknīste",
          "Alūksne",
          "Ape",
          "Auce",
          "Baldone",
          "Baloži",
          "Bauska",
          "Brocēni",
          "Cēsis",
          "Dagda",
          "Daugavpils",
          "Dobele",
          "Gulbene",
          "Ikšķile",
          "Ilūkste",
          "Jelgava",
          "Jēkabpils",
          "Jūrmala",
          "Kolka",
          "Krāslava",
          "Kuldīga",
          "Kārsava",
          "Lielvārde",
          "Liepāja",
          "Limbaži",
          "Ludza",
          "Līvāni",
          "Madona",
          "Mārupe",
          "Mērsrags",
          "Ogre",
          "Olaine",
          "Piltene",
          "Preiļi",
          "Priekule",
          "Priekuļi",
          "Pāvilosta",
          "Rucava",
          "Rēzekne",
          "Rīga",
          "Rūjiena",
          "Sabile",
          "Salaspils",
          "Saldus",
          "Saulkrasti",
          "Sigulda",
          "Skrīveri",
          "Skulte",
          "Smiltene",
          "Stende",
          "Strenči",
          "Subate",
          "Talsi",
          "Tukums",
          "Valdemārpils",
          "Valka",
          "Valmiera",
          "Varakļāni",
          "Ventspils",
          "Viesīte",
          "Viļaka",
          "Zilupe",
          "Zīlāni",
          "Ādaži",
          "Ķekava");

  static final String BASE_URL = "https://videscentrs.lvgmc.lv/data";
  static final JsonMapper JSON =
      JsonMapper.builder()
          .addModule(new JavaTimeModule())
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .build();
  private static final Logger log = LoggerFactory.getLogger("meteobot.weather");
  private static final Duration TIMEOUT = Duration.ofSeconds(15);
  private static final Duration STATIONS_CACHE_TTL = Duration.ofHours(1);
  private final String baseUrl;
  private final HttpClient http =
      HttpClient.newBuilder()
          .connectTimeout(TIMEOUT)
          .followRedirects(HttpClient.Redirect.NORMAL)
          .build();

  private List<Station> stations = List.of();
  private Instant stationsExpireAt = Instant.MIN;

  public Lvgmc() {
    this(BASE_URL);
  }

  Lvgmc(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  /**
   * The forecast city matching {@code query}: exact match, else the first containing it; like
   * {@link #normalize}.
   */
  public static Optional<String> findCity(String query) {
    String q = normalize(query);
    if (q.isEmpty()) {
      return Optional.empty();
    }
    return firstCity(city -> normalize(city).equals(q))
        .or(() -> firstCity(city -> normalize(city).contains(q)));
  }

  /**
   * Lowercase, without whitespace and diacritics: {@code "Rīga (LU)"} becomes {@code "riga(lu)"}.
   */
  static String normalize(String text) {
    return Normalizer.normalize(text, Normalizer.Form.NFD)
        .replaceAll("[\\p{M}\\s]", "")
        .toLowerCase(Locale.ROOT);
  }

  private static Optional<String> firstCity(Predicate<String> condition) {
    return FORECAST_CITIES.stream().filter(condition).findFirst();
  }

  /** Station names may carry a suffix, e.g. "Rīga (LU)" for the University of Latvia station. */
  private static Optional<Station> findStation(List<Station> stations, String query) {
    List<Station> named =
        stations.stream().filter(s -> s.code() != null && s.name() != null).toList();
    return named.stream()
        .filter(
            s -> {
              String name = normalize(s.name()).replace("(lu)", "").replace("(piekraste)", "");
              return !name.isEmpty() && (name.startsWith(query) || query.startsWith(name));
            })
        .findFirst()
        .or(() -> named.stream().filter(s -> normalize(s.name()).contains(query)).findFirst());
  }

  /**
   * Latest observation of the station named like {@code city}, ignoring case, spaces and
   * diacritics.
   */
  public Optional<Observation> latestObservation(String city) {
    String query = normalize(city);
    Optional<Station> station = findStation(stations(), query);
    if (station.isEmpty()) {
      return Optional.empty();
    }
    // Several hours per station, newest first.
    List<Observation> observations = fetchList("weather_monitoring_data", Observation.class);
    return observations.stream()
        .filter(o -> station.get().code().equals(o.stationCode()))
        .findFirst()
        .or(
            () ->
                observations.stream()
                    .filter(
                        o -> o.stationName() != null && normalize(o.stationName()).contains(query))
                    .findFirst());
  }

  public List<ForecastEntry> dailyForecast(String city) {
    return fetchList(
        "weather_forecast_for_location_daily?nosaukums=" + URLEncoder.encode(city, UTF_8),
        ForecastEntry.class);
  }

  public List<ForecastEntry> hourlyForecast(String city) {
    return fetchList(
        "weather_forecast_for_location_hourly?nosaukums=" + URLEncoder.encode(city, UTF_8),
        ForecastEntry.class);
  }

  public List<SynopticForecast> synopticForecast() {
    return fetchList("sinopt_prognozes", SynopticForecast.class);
  }

  private synchronized List<Station> stations() {
    if (Instant.now().isAfter(stationsExpireAt)) {
      List<Station> fetched = fetchList("weather_monitoring_points", Station.class);
      if (!fetched.isEmpty()) {
        stations = fetched;
        stationsExpireAt = Instant.now().plus(STATIONS_CACHE_TTL);
      }
    }
    return stations;
  }

  /** GETs a JSON array; on any failure logs it and returns an empty list. */
  private <T> List<T> fetchList(String pathAndQuery, Class<T> type) {
    String url = baseUrl + "/" + pathAndQuery;
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(url))
            .header("User-Agent", "MeteoBot/1.0")
            .timeout(TIMEOUT)
            .build();
    try {
      HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(UTF_8));
      if (response.statusCode() != 200) {
        throw new IOException("HTTP " + response.statusCode());
      }
      List<T> list =
          JSON.readValue(
              response.body(), JSON.getTypeFactory().constructCollectionType(List.class, type));
      return list == null ? List.of() : list;
    } catch (IOException e) {
      log.warn("Error fetching {}: {}", url, e.toString());
    } catch (InterruptedException _) {
      Thread.currentThread().interrupt();
    }
    return List.of();
  }
}
