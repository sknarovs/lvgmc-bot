package lv.sknarovs.lvgmcbot.weather;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lv.sknarovs.lvgmcbot.i18n.Language;
import lv.sknarovs.lvgmcbot.i18n.Text;
import lv.sknarovs.lvgmcbot.lvgmc.ForecastEntry;
import lv.sknarovs.lvgmcbot.lvgmc.Lvgmc;
import lv.sknarovs.lvgmcbot.lvgmc.Observation;
import lv.sknarovs.lvgmcbot.lvgmc.SynopticForecast;
import lv.sknarovs.lvgmcbot.util.Markdown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Fetches LVĢMC data and composes the weather messages, for commands and daily schedules alike. */
public final class WeatherReports {

  private static final Logger log = LoggerFactory.getLogger("meteobot");
  private static final Duration MAX_OBSERVATION_AGE = Duration.ofHours(2);

  private final Lvgmc lvgmc;
  private final Clock clock;

  public WeatherReports(Lvgmc lvgmc, Clock clock) {
    this.lvgmc = lvgmc;
    this.clock = clock;
  }

  /**
   * The latest observation of the station named like {@code city}; if there is none, or it is more
   * than two hours old, this hour's forecast for the matching forecast city instead.
   */
  public String currentWeather(String city, Language lang) {
    Optional<Observation> observation = lvgmc.latestObservation(city);
    if (observation.isPresent() && isRecent(observation.get())) {
      return WeatherFormatter.currentWeather(observation.get(), lang);
    }

    Optional<String> forecastCity = Lvgmc.findCity(city);
    if (forecastCity.isEmpty()) {
      return Text.CITY_NOT_FOUND_HINT.get(lang, Markdown.escape(city));
    }
    List<ForecastEntry> forecast = lvgmc.hourlyForecast(forecastCity.get());
    if (forecast.isEmpty()) {
      return Text.WEATHER_NO_DATA.get(lang, Markdown.escape(city));
    }
    LocalDateTime now = LocalDateTime.now(clock);
    ForecastEntry entry =
        forecast.stream()
            .filter(e -> e.time() != null && !e.time().isBefore(now))
            .findFirst()
            .orElse(forecast.getFirst());
    return WeatherFormatter.forecastAsCurrent(forecastCity.get(), entry, lang);
  }

  /**
   * @param city one of {@link Lvgmc#FORECAST_CITIES}
   */
  public String dailyForecast(String city, Language lang) {
    List<ForecastEntry> forecast = lvgmc.dailyForecast(city);
    return forecast.isEmpty()
        ? Text.FORECAST_NO_DATA.get(lang, city)
        : WeatherFormatter.dailyForecast(forecast, lang);
  }

  /**
   * @param city one of {@link Lvgmc#FORECAST_CITIES}
   */
  public String hourlyForecast(String city, int hours, Language lang) {
    List<ForecastEntry> forecast = lvgmc.hourlyForecast(city);
    return forecast.isEmpty()
        ? Text.FORECAST_HOURLY_NO_DATA.get(lang, city)
        : WeatherFormatter.hourlyForecast(forecast, hours, lang, LocalDateTime.now(clock));
  }

  public String synopticForecast(Language lang) {
    List<SynopticForecast> forecast = lvgmc.synopticForecast();
    return forecast.isEmpty()
        ? Text.SYNOPTIC_NO_DATA.get(lang)
        : WeatherFormatter.synopticForecast(forecast, lang);
  }

  private boolean isRecent(Observation observation) {
    if (observation.time() == null) {
      return true;
    }
    Duration age = Duration.between(observation.time().toInstant(), clock.instant());
    if (age.compareTo(MAX_OBSERVATION_AGE) > 0) {
      log.info("Observation data is {} min old, using forecast instead", age.toMinutes());
      return false;
    }
    return true;
  }
}
