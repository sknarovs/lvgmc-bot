package lv.meteobot;

import lv.meteobot.Lvgmc.ForecastEntry;
import lv.meteobot.Lvgmc.Observation;
import lv.meteobot.Lvgmc.SynopticForecast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.util.List;

import static lv.meteobot.MarkdownAssert.assertValidMarkdown;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeatherFormatterTest {

    @Test
    void currentWeather() {
        Observation observation = observation("""
                {"station_code": "RIGASLU", "name": "Rīga Universitāte", "time": "2026.10.16 20:00:00",
                 "air_temperature_actual": "8.6", "air_temperature_feel": "7.1", "humidity_actual": "95",
                 "wind_speed_actual": "2.6", "wind_speed_gust": "4.3", "wind_direction_actual": "190",
                 "pressure_sea_level": "1022.5", "precipitation": "0.2", "clouds": "4",
                 "visibility_actual": "8560", "description": "DŪMAKA; LIETUS"}""");

        String text = WeatherFormatter.currentWeather(observation, Language.LV);

        // Observation times are UTC: 20:00 UTC is 23:00 in Riga in October.
        assertEquals("""
                📍 *Rīga Universitāte* — 2026.10.16 23:00:00

                🌧️ Temperatūra: *8.6°C* (jūtam: 7.1°C)
                💧 Mitrums: 95%
                💨 Vējš: ⬇️D 2.6 m/s (brāzmas: 4.3 m/s)
                🧭 Spiediens: 1022.5 hPa
                🌧️ Nokrišņi: 0.2 mm
                ☁️ Mākoņu daudzums: 4/8
                👁️ Redzamība: 8.6 km

                📋 Stāvoklis: DŪMAKA; LIETUS""", text);
        assertValidMarkdown(text);
    }

    @Test
    void currentWeatherWithoutValues() {
        assertEquals("""
                📍 *Unknown* — -

                🌡️ Temperature: *-°C* (feels like: -°C)
                💧 Humidity: -%
                💨 Wind: 🌀 - m/s (gusts: - m/s)
                🧭 Pressure: - hPa""", WeatherFormatter.currentWeather(observation("{}"), Language.EN));
    }

    @ParameterizedTest(name = "{0} UTC: {1}")
    @CsvSource({"2026.10.16 02:00:00, 🌙", "2026.10.16 03:00:00, ☀️", "2026.10.16 18:00:00, ☀️", "2026.10.16 19:00:00, 🌙"})
    void clearSkyShowsSunBetween6And21LatvianTime(String utc, String emoji) {
        Observation observation = observation("{\"time\": \"" + utc + "\", \"clouds\": \"1\"}");
        assertTrue(WeatherFormatter.currentWeather(observation, Language.LV).contains("\n" + emoji + " Temperatūra"));
    }

    @ParameterizedTest(name = "\"{0}\": {1}")
    @CsvSource({"SNIEGS, ❄️", "Slapjš sniegs, ❄️", "PĒRKONA NEGAISS, ⛈️", "DŪMAKA; MIGLA, 🌫️", "DŪMAKA, 🌡️"})
    void descriptionEmoji(String description, String emoji) {
        Observation observation = observation("{\"description\": \"" + description + "\"}");
        assertTrue(WeatherFormatter.currentWeather(observation, Language.EN).contains("\n" + emoji + " Temperature"));
    }

    @Test
    void visibilityIsInKilometres() {
        Observation observation = observation("{\"visibility_actual\": \"900\"}");
        assertTrue(WeatherFormatter.currentWeather(observation, Language.EN).endsWith("\n👁️ Visibility: 0.9 km"));
    }

    @Test
    void descriptionIsEscaped() {
        String text = WeatherFormatter.currentWeather(observation("{\"description\": \"A_B *C\"}"), Language.EN);
        assertTrue(text.endsWith("\n📋 Condition: A\\_B \\*C"), text);
        assertValidMarkdown(text);
    }

    @Test
    void forecastAsCurrentWeather() {
        ForecastEntry entry = forecast("""
                [{"nosaukums": "Rīga", "laiks": "202610162000", "temperatura": "5", "sajutu_temperatura": "3",
                  "relativais_mitrums": "80", "veja_atrums": "2", "veja_virziens": "200.5", "nokrisni_1h": "0.4",
                  "spiediens": "1010", "laika_apstaklu_ikona": "1202"}]""").getFirst();

        String text = WeatherFormatter.forecastAsCurrent("Rīga", entry, Language.LV);

        assertEquals("""
                📍 *Rīga* (stundu prognoze 16.10.2026 20:00)

                🌧️ Temperatūra: *5°C* (jūtam: 3°C)
                💧 Mitrums: 80%
                💨 Vējš: ⬇️D 2 m/s
                🌧️ Nokrišņi: 0.4 mm/h
                🧭 Spiediens: 1010 hPa

                _ℹ️ Novērojumu dati nav aktuāli, rāda prognozi_""", text);
        assertValidMarkdown(text);
    }

    @Test
    void dailyForecast() {
        List<ForecastEntry> forecast = forecast("""
                [{"nosaukums": "Rīga", "laiks": "202610191200", "temperatura": "2.5", "nokrisni_12h": "-", "laika_apstaklu_ikona": "1102"},
                 {"nosaukums": "Rīga", "laiks": "202610191800", "temperatura": "-0.4", "nokrisni_12h": "0.3", "laika_apstaklu_ikona": "2101"},
                 {"nosaukums": "Rīga", "laiks": "202610200600", "temperatura": "4.4", "nokrisni_12h": "1.25", "laika_apstaklu_ikona": "1201"},
                 {"nosaukums": "Rīga", "laiks": "202610201800", "temperatura": "9.5", "nokrisni_12h": "0.5", "laika_apstaklu_ikona": "2202"},
                 {"nosaukums": "Rīga", "laiks": "202610210600", "temperatura": "-", "nokrisni_12h": "0"}]""");

        assertEquals("""
                📅 *Prognoze: Rīga*

                🌤️ 19.10 (Pir): *0°C*...*3°C* 🌧️0.3mm
                🌦️ 20.10 (Otr): *4°C*...*10°C* 🌧️1.8mm
                🌡️ 21.10 (Tre): *—°C*""", WeatherFormatter.dailyForecast(forecast, Language.LV));
        assertTrue(WeatherFormatter.dailyForecast(forecast, Language.EN).contains("\n🌦️ 20.10 (Tue): "));
    }

    @Test
    void dailyForecastOfRecordedDataShowsSevenDays() {
        List<ForecastEntry> forecast = Fixtures.parseList(
                Fixtures.read("weather_forecast_for_location_daily_riga.json"), ForecastEntry.class);

        String text = WeatherFormatter.dailyForecast(forecast, Language.LV);

        assertEquals("""
                📅 *Prognoze: Rīga*

                🌤️ 16.10 (Pie): *12°C*
                🌙 17.10 (Ses): *7°C*...*12°C*
                ⛈️ 18.10 (Svē): *8°C*...*11°C* 🌧️0.4mm
                🌙 19.10 (Pir): *2°C*...*9°C*
                ☁️ 20.10 (Otr): *2°C*...*10°C*
                ☁️ 21.10 (Tre): *6°C*...*13°C*
                ⛈️ 22.10 (Cet): *9°C*...*12°C* 🌧️5.5mm""", text);
        assertValidMarkdown(text);
    }

    @Test
    void hourlyForecastStartsAtTheNextFullHour() {
        List<ForecastEntry> forecast = forecast("""
                [{"nosaukums": "Rīga", "laiks": "202610161800", "sajutu_temperatura": "10", "laika_apstaklu_ikona": "1102"},
                 {"nosaukums": "Rīga", "laiks": "202610161900", "sajutu_temperatura": "9.4", "nokrisni_1h": "0.00", "laika_apstaklu_ikona": "1102"},
                 {"nosaukums": "Rīga", "laiks": "202610162000", "sajutu_temperatura": "8.1", "nokrisni_1h": "0.25", "laika_apstaklu_ikona": "2201"},
                 {"nosaukums": "Rīga", "laiks": "202610162100", "sajutu_temperatura": "7", "nokrisni_1h": "1", "laika_apstaklu_ikona": "2101"},
                 {"nosaukums": "Rīga", "laiks": "202610162200", "sajutu_temperatura": "6.5", "laika_apstaklu_ikona": "2101"}]""");

        String text = WeatherFormatter.hourlyForecast(forecast, 3, Language.EN, LocalDateTime.of(2026, 10, 16, 18, 20));

        assertEquals("""
                🕐 *Hourly forecast: Rīga*

                🌤️ 19:00 *9.4°C*
                🌧️ 20:00 *8.1°C* 0.3mm
                🌙 21:00 *7°C* 1.0mm""", text);
        assertValidMarkdown(text);
    }

    @Test
    void synopticForecastEscapesTheText() {
        List<SynopticForecast> forecast = Fixtures.parseList("""
                [{"kods": "Latvija kom. nak. n.", "datums": "2026-10-16", "laiks_no": "21:00:00", "laiks_lidz": "09:00:00",
                  "teksti": {"teksts": "Naktī lietus_ un vējš", "teksts_html": null}}]""", SynopticForecast.class);

        String text = WeatherFormatter.synopticForecast(forecast, Language.LV);

        assertEquals("""
                📑 *Sinoptiskā prognoze*

                *Latvija kom. nak. n.* (2026-10-16 21:00:00—09:00:00)
                Naktī lietus\\_ un vējš
                """, text);
        assertValidMarkdown(text);
    }

    @ParameterizedTest(name = "\"{0}\": {1}")
    @CsvSource(value = {"1101, ☀️", "2506, ⛈️", "1999, 🌤️", "2403, 🌙", "3000, 🌡️", "'', 🌡️", "NULL, 🌡️"}, nullValues = "NULL")
    void iconEmoji(String icon, String emoji) {
        assertEquals(emoji, WeatherFormatter.iconEmoji(icon));
    }

    private static Observation observation(String json) {
        return Fixtures.parse(json, Observation.class);
    }

    private static List<ForecastEntry> forecast(String json) {
        return Fixtures.parseList(json, ForecastEntry.class);
    }
}
