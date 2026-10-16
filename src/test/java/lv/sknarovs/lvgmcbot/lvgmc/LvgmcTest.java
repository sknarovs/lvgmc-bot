package lv.sknarovs.lvgmcbot.lvgmc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LvgmcTest {

    private FakeLvgmc server;
    private Lvgmc lvgmc;

    @BeforeEach
    void setUp() throws IOException {
        server = FakeLvgmc.withRecordedData();
        lvgmc = server.client();
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @ParameterizedTest(name = "\"{0}\" is {1}")
    @CsvSource(value = {
            "Rīga, Rīga", "riga, Rīga", "RĪGA, Rīga", "jur mala, Jūrmala", "liep, Liepāja", "kekava, Ķekava",
            "priekuli, Priekuļi", "ape, Ape", "London, NULL", "'  ', NULL"}, nullValues = "NULL")
    void findCity(String query, String expected) {
        assertEquals(Optional.ofNullable(expected), Lvgmc.findCity(query));
    }

    @Test
    void latestObservationIgnoresDiacriticsAndIsTheNewest() {
        Observation observation = lvgmc.latestObservation("riga").orElseThrow();

        assertEquals("RIGASLU", observation.stationCode());
        assertEquals("Rīga Universitāte", observation.stationName());
        assertEquals(LocalDateTime.of(2026, 10, 16, 20, 0), observation.timeUtc());
        assertEquals(ZonedDateTime.of(2026, 10, 16, 23, 0, 0, 0, Lvgmc.ZONE), observation.time());
        assertEquals("8.6", observation.temperature());
        assertEquals("DŪMAKA", observation.description());
    }

    @Test
    void stationNameSuffixesAreIgnored() {
        assertEquals("RILP99PA", lvgmc.latestObservation("Liepāja").orElseThrow().stationCode());
    }

    @Test
    void noStationForCity() {
        assertEquals(Optional.empty(), lvgmc.latestObservation("Ape"));
        assertEquals(List.of("weather_monitoring_points"), server.requests());
    }

    @Test
    void stationsAreCached() {
        lvgmc.latestObservation("Rīga");
        lvgmc.latestObservation("Liepāja");
        assertEquals(1, server.requests().stream().filter("weather_monitoring_points"::equals).count());
    }

    @Test
    void dailyForecast() {
        List<ForecastEntry> forecast = lvgmc.dailyForecast("Rīga");

        assertEquals(List.of("weather_forecast_for_location_daily?nosaukums=Rīga"), server.requests());
        assertEquals(21, forecast.size());
        assertEquals(new ForecastEntry("Rīga", LocalDateTime.of(2026, 10, 16, 12, 0), "12.1", null, "93.1", "4.1",
                "170.0", null, null, "-", "1102"), forecast.getFirst());
    }

    @Test
    void synopticForecast() {
        List<SynopticForecast> forecast = lvgmc.synopticForecast();

        assertEquals(2, forecast.size());
        assertEquals("Latvija kom. nak. n.", forecast.getFirst().title());
        assertTrue(forecast.getFirst().text().startsWith("Saistībā ar vides datu"));
    }

    @Test
    void failuresGiveEmptyResults() {
        server.respond("sinopt_prognozes", "<html>Service Unavailable</html>");
        assertEquals(List.of(), lvgmc.synopticForecast());
        assertEquals(List.of(), lvgmc.hourlyForecast("Ape"));
        assertEquals(List.of(), new Lvgmc("http://localhost:1/data").dailyForecast("Rīga"));
    }
}
