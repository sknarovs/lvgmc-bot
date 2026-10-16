package lv.meteobot;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static lv.meteobot.MarkdownAssert.assertValidMarkdown;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeatherReportsTest {

    /** Time of the newest recorded observation; 23:00 in Riga. */
    private static final Instant OBSERVED = Instant.parse("2026-10-16T20:00:00Z");

    private FakeLvgmc server;

    @BeforeEach
    void setUp() throws IOException {
        server = FakeLvgmc.withRecordedData();
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void recentObservation() {
        String text = reportsAt(OBSERVED.plus(Duration.ofMinutes(40))).currentWeather("Rīga", Language.LV);

        assertEquals("""
                📍 *Rīga Universitāte* — 2026.10.16 23:00:00

                ⛅ Temperatūra: *8.6°C* (jūtam: 7.1°C)
                💧 Mitrums: 95%
                💨 Vējš: ⬇️D 2.6 m/s (brāzmas: 4.3 m/s)
                🧭 Spiediens: 1022.5 hPa
                ☁️ Mākoņu daudzums: 4/8
                👁️ Redzamība: 8.6 km

                📋 Stāvoklis: DŪMAKA""", text);
    }

    @Test
    void outdatedObservationFallsBackToThisHoursForecast() {
        // Three hours after the observation: 02:00 on 17 October in Riga.
        String text = reportsAt(OBSERVED.plus(Duration.ofHours(3))).currentWeather("Rīga", Language.LV);

        assertEquals("""
                📍 *Rīga* (stundu prognoze 17.10.2026 02:00)

                ☁️ Temperatūra: *7.7°C* (jūtam: 5.8°C)
                💧 Mitrums: 96.4%
                💨 Vējš: ⬇️D 2.9 m/s
                🧭 Spiediens: 1021.7 hPa

                _ℹ️ Novērojumu dati nav aktuāli, rāda prognozi_""", text);
    }

    @Test
    void cityWithoutStationUsesTheForecast() {
        server.respond("weather_forecast_for_location_hourly?nosaukums=Ape",
                Fixtures.read("weather_forecast_for_location_hourly_riga.json"));

        String text = reportsAt(OBSERVED).currentWeather("ape", Language.EN);

        assertTrue(text.startsWith("📍 *Ape* (hourly forecast 16.10.2026 23:00)\n"), text);
    }

    @Test
    void unknownCityIsEscaped() {
        String text = reportsAt(OBSERVED).currentWeather("foo_bar", Language.LV);

        assertEquals("❌ Pilsēta nav atrasta: foo\\_bar\n\nIzmanto /city lai izvēlētos.", text);
        assertValidMarkdown(text);
    }

    @Test
    void hourlyForecastStartsAfterNow() {
        assertEquals("""
                🕐 *Hourly forecast: Rīga*

                🌙 00:00 *6.3°C*
                🌙 01:00 *5.7°C*""", reportsAt(OBSERVED).hourlyForecast("Rīga", 2, Language.EN));
    }

    @Test
    void missingData() {
        WeatherReports reports = reportsAt(OBSERVED);
        server.respond("sinopt_prognozes", "[]");

        assertEquals("❌ Nav prognozes datu par: Ape", reports.dailyForecast("Ape", Language.LV));
        assertEquals("❌ No hourly forecast data for: Ape", reports.hourlyForecast("Ape", 12, Language.EN));
        assertEquals("❌ Synoptic forecast not available.", reports.synopticForecast(Language.EN));
        assertEquals("❌ No data for: Ape", reports.currentWeather("Ape", Language.EN));
    }

    private WeatherReports reportsAt(Instant now) {
        return new WeatherReports(server.client(), Clock.fixed(now, Lvgmc.ZONE));
    }
}
