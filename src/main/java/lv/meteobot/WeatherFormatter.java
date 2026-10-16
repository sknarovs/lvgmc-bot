package lv.meteobot;

import lv.meteobot.Lvgmc.ForecastEntry;
import lv.meteobot.Lvgmc.Observation;
import lv.meteobot.Lvgmc.SynopticForecast;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Map.entry;

/** Renders LVĢMC data as the bot's (legacy Markdown) messages. */
final class WeatherFormatter {

    private static final Map<String, String> ICON_TO_EMOJI = Map.ofEntries(
            entry("1101", "☀️"), entry("1102", "🌤️"), entry("1103", "⛅"), entry("1104", "🌥️"),
            entry("1105", "☁️"), entry("1106", "🌫️"),
            entry("1201", "🌦️"), entry("1202", "🌧️"), entry("1203", "❄️"), entry("1204", "🌨️"),
            entry("1205", "🌦️"), entry("1206", "🌧️"), entry("1207", "❄️"), entry("1208", "🌨️"),
            entry("1301", "⛈️"), entry("1302", "⛈️"), entry("1303", "⛈️"), entry("1304", "⛈️"),
            entry("1305", "🌧️"), entry("1306", "🌧️"), entry("1307", "🌨️"), entry("1308", "🌨️"),
            entry("1309", "🌫️"), entry("1310", "🌫️"), entry("1311", "🌧️"), entry("1312", "🌧️"),
            entry("1313", "🌧️"),
            entry("1504", "🌦️"), entry("1506", "⛈️"),
            entry("2101", "🌙"), entry("2102", "🌙"), entry("2103", "☁️"), entry("2104", "🌥️"),
            entry("2105", "☁️"),
            entry("2201", "🌧️"), entry("2202", "🌧️"), entry("2203", "❄️"), entry("2204", "🌨️"),
            entry("2205", "🌧️"), entry("2206", "🌧️"),
            entry("2301", "⛈️"), entry("2302", "⛈️"),
            entry("2504", "🌧️"), entry("2506", "⛈️"));

    /** Words in observation descriptions such as "DŪMAKA; LIETUS"; the first one found wins. */
    private static final List<Map.Entry<String, String>> DESCRIPTION_TO_EMOJI = List.of(
            entry("SKAIDRS", "☀️"),
            entry("MĀKOŅAINS", "☁️"),
            entry("LIETUS", "🌧️"),
            entry("SLIKTS LAIKS", "⛈️"),
            entry("SNIEGS", "❄️"),
            entry("MIGLA", "🌫️"),
            entry("PĒRKONA NEGAISS", "⛈️"));

    private static final DateTimeFormatter OBSERVATION_TIME = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");
    private static final DateTimeFormatter FORECAST_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd.MM");
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm");

    private WeatherFormatter() {
    }

    static String currentWeather(Observation observation, Language lang) {
        ZonedDateTime time = observation.time();
        List<String> lines = new ArrayList<>();
        lines.add(Text.CURRENT_TITLE.get(lang,
                Objects.requireNonNullElse(observation.stationName(), Text.CURRENT_UNKNOWN_STATION.get(lang)),
                time == null ? "-" : time.format(OBSERVATION_TIME)));
        lines.add("");
        lines.add(Text.CURRENT_TEMP_LINE.get(lang, conditionEmoji(observation), orDash(observation.temperature()),
                orDash(observation.feelsLike())));
        lines.add(Text.CURRENT_HUMIDITY_LINE.get(lang, orDash(observation.humidity())));
        lines.add(Text.CURRENT_WIND_LINE.get(lang, compassPoint(observation.windDirection(), lang),
                orDash(observation.windSpeed()), orDash(observation.windGust())));
        lines.add(Text.CURRENT_PRESSURE_LINE.get(lang, orDash(observation.pressure())));
        if (number(observation.precipitation()).filter(mm -> mm > 0).isPresent()) {
            lines.add(Text.CURRENT_PRECIPITATION_LINE.get(lang, observation.precipitation()));
        }
        if (observation.clouds() != null) {
            lines.add(Text.CURRENT_CLOUDS_LINE.get(lang, observation.clouds()));
        }
        number(observation.visibilityMeters())
                .ifPresent(meters -> lines.add(Text.CURRENT_VISIBILITY_LINE.get(lang, meters / 1000)));
        if (observation.description() != null && !observation.description().isBlank()) {
            lines.add(Text.CURRENT_CONDITION_LINE.get(lang, Markdown.escape(observation.description())));
        }
        return String.join("\n", lines);
    }

    /** An hourly forecast entry laid out like {@link #currentWeather}, with a note saying so. */
    static String forecastAsCurrent(String city, ForecastEntry entry, Language lang) {
        List<String> lines = new ArrayList<>();
        lines.add(Text.FORECAST_AS_CURRENT_TITLE.get(lang, city,
                entry.time() == null ? "-" : entry.time().format(FORECAST_TIME)));
        lines.add("");
        lines.add(Text.CURRENT_TEMP_LINE.get(lang, iconEmoji(entry.icon()), orDash(entry.temperature()),
                orDash(entry.feelsLike())));
        lines.add(Text.CURRENT_HUMIDITY_LINE.get(lang, orDash(entry.humidity())));
        lines.add(Text.FORECAST_AS_CURRENT_WIND_LINE.get(lang, compassPoint(entry.windDirection(), lang),
                orDash(entry.windSpeed())));
        if (number(entry.precipitation1h()).filter(mm -> mm > 0).isPresent()) {
            lines.add(Text.FORECAST_AS_CURRENT_PRECIPITATION_LINE.get(lang, entry.precipitation1h()));
        }
        lines.add(Text.CURRENT_PRESSURE_LINE.get(lang, orDash(entry.pressure())));
        lines.add("");
        lines.add(Text.WEATHER_FALLBACK_NOTE.get(lang));
        return String.join("\n", lines);
    }

    /** One line per day for up to 7 days: temperature range and total precipitation. */
    static String dailyForecast(List<ForecastEntry> forecast, Language lang) {
        Map<LocalDate, List<ForecastEntry>> days = forecast.stream()
                .filter(entry -> entry.time() != null)
                .collect(Collectors.groupingBy(entry -> entry.time().toLocalDate(), LinkedHashMap::new, Collectors.toList()));

        List<String> lines = new ArrayList<>();
        lines.add(Text.FORECAST_TITLE.get(lang, orDash(forecast.getFirst().city())) + "\n");
        days.values().stream().limit(7).map(day -> dayLine(day, lang)).forEach(lines::add);
        return String.join("\n", lines);
    }

    /** The next {@code hours} hours, starting with the next full hour after {@code now}. */
    static String hourlyForecast(List<ForecastEntry> forecast, int hours, Language lang, LocalDateTime now) {
        LocalDateTime from = now.truncatedTo(ChronoUnit.HOURS).plusHours(1);
        List<String> lines = new ArrayList<>();
        lines.add(Text.FORECAST_HOURLY_TITLE.get(lang, orDash(forecast.getFirst().city())) + "\n");
        forecast.stream()
                .filter(entry -> entry.time() != null && !entry.time().isBefore(from))
                .limit(hours)
                .map(entry -> iconEmoji(entry.icon()) + " " + entry.time().format(HOUR)
                        + " *" + orDash(entry.feelsLike()) + "°C*"
                        + number(entry.precipitation1h()).filter(mm -> mm > 0)
                        .map(mm -> String.format(Locale.ROOT, " %.1fmm", mm)).orElse(""))
                .forEach(lines::add);
        return String.join("\n", lines);
    }

    static String synopticForecast(List<SynopticForecast> forecasts, Language lang) {
        List<String> lines = new ArrayList<>();
        lines.add(Text.SYNOPTIC_TITLE.get(lang) + "\n");
        for (SynopticForecast forecast : forecasts) {
            lines.add("*" + Objects.toString(forecast.title(), "") + "* (" + Objects.toString(forecast.date(), "")
                    + " " + Objects.toString(forecast.from(), "") + "—" + Objects.toString(forecast.to(), "") + ")");
            lines.add(Markdown.escape(forecast.text()) + "\n");
        }
        return String.join("\n", lines);
    }

    static String iconEmoji(String icon) {
        if (icon == null || icon.isEmpty()) {
            return "🌡️";
        }
        String emoji = ICON_TO_EMOJI.get(icon);
        if (emoji != null) {
            return emoji;
        }
        // Unknown icon: the first digit still tells day (1) from night (2).
        return icon.startsWith("1") ? "🌤️" : icon.startsWith("2") ? "🌙" : "🌡️";
    }

    private static String dayLine(List<ForecastEntry> day, Language lang) {
        LocalDate date = day.getFirst().time().toLocalDate();
        DoubleSummaryStatistics temperatures = day.stream()
                .flatMap(entry -> number(entry.temperature()).stream())
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();
        double precipitation = day.stream()
                .flatMap(entry -> number(entry.precipitation12h()).stream())
                .mapToDouble(Double::doubleValue)
                .sum();
        String icon = day.stream().map(ForecastEntry::icon).filter(i -> i != null && !i.isEmpty()).findFirst().orElse(null);

        String temperature;
        if (temperatures.getCount() == 0) {
            temperature = "*—°C*";
        } else if (temperatures.getCount() == 1) {
            temperature = "*" + Math.round(temperatures.getMin()) + "°C*";
        } else {
            temperature = "*" + Math.round(temperatures.getMin()) + "°C*...*" + Math.round(temperatures.getMax()) + "°C*";
        }
        String rain = precipitation > 0 ? String.format(Locale.ROOT, " 🌧️%.1fmm", precipitation) : "";
        return iconEmoji(icon) + " " + date.format(DAY) + " (" + lang.dayAbbreviation(date.getDayOfWeek()) + "): "
                + temperature + rain;
    }

    private static String conditionEmoji(Observation observation) {
        String description = Objects.toString(observation.description(), "").toUpperCase(Locale.ROOT);
        for (Map.Entry<String, String> word : DESCRIPTION_TO_EMOJI) {
            if (description.contains(word.getKey())) {
                return word.getValue();
            }
        }
        Optional<Double> clouds = number(observation.clouds());
        if (clouds.isEmpty()) {
            return "🌡️";
        }
        if (clouds.get() <= 2) {
            int hour = observation.time() == null ? 12 : observation.time().getHour();
            return hour >= 6 && hour <= 21 ? "☀️" : "🌙";
        }
        return clouds.get() <= 5 ? "⛅" : "☁️";
    }

    private static String compassPoint(String degrees, Language lang) {
        return number(degrees).map(lang::compassPoint).orElse("🌀");
    }

    private static String orDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    /** LVĢMC sends numbers as strings, and "-" when there is no value. */
    private static Optional<Double> number(String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Double.parseDouble(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
