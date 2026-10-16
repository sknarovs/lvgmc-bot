package lv.meteobot;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** What a daily schedule sends. */
enum ScheduleType {

    WEATHER(Text.TYPE_WEATHER, Text.TYPE_WEATHER_EMOJI),
    FORECAST(Text.TYPE_FORECAST, Text.TYPE_FORECAST_EMOJI),
    FORECAST_H(Text.TYPE_FORECAST_H, Text.TYPE_FORECAST_H_EMOJI);

    private final Text description;
    private final Text helpLabel;

    ScheduleType(Text description, Text helpLabel) {
        this.description = description;
        this.helpLabel = helpLabel;
    }

    /** As typed by users and stored in the database: {@code weather}, {@code forecast} or {@code forecast_h}. */
    String code() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Case-insensitive. */
    static Optional<ScheduleType> fromCode(String code) {
        return Arrays.stream(values()).filter(type -> type.code().equalsIgnoreCase(code)).findFirst();
    }

    String description(Language lang) {
        return description.get(lang);
    }

    /** With emoji, for /help. */
    String helpLabel(Language lang) {
        return helpLabel.get(lang);
    }
}
