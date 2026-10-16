package lv.sknarovs.lvgmcbot.i18n;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Locale;

/** The languages the bot speaks; message texts are in {@link Text}. */
public enum Language {

    LV("🇱🇻 Latviešu",
            List.of("Pir", "Otr", "Tre", "Cet", "Pie", "Ses", "Svē"),
            List.of("⬆️Z", "↗️ZA", "➡️A", "↘️DA", "⬇️D", "↙️DR", "⬅️R", "↖️ZR")),
    EN("🇬🇧 English",
            List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
            List.of("⬆️N", "↗️NE", "➡️E", "↘️SE", "⬇️S", "↙️SW", "⬅️W", "↖️NW"));

    private final String label;
    private final List<String> dayAbbreviations;
    private final List<String> compassPoints;

    Language(String label, List<String> dayAbbreviations, List<String> compassPoints) {
        this.label = label;
        this.dayAbbreviations = dayAbbreviations;
        this.compassPoints = compassPoints;
    }

    /** As stored in the database and used in callback data: {@code lv} or {@code en}. */
    public String code() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Latvian for anything but {@code en}. */
    public static Language fromCode(String code) {
        return EN.code().equals(code) ? EN : LV;
    }

    /** English if the user's Telegram app is in English (any region, e.g. {@code en-GB}), otherwise Latvian. */
    public static Language ofTelegramUser(String languageCode) {
        return languageCode != null && Locale.forLanguageTag(languageCode).getLanguage().equals("en") ? EN : LV;
    }

    /** Name with flag, for the language picker. */
    public String label() {
        return label;
    }

    public String dayAbbreviation(DayOfWeek day) {
        return dayAbbreviations.get(day.ordinal());
    }

    /** Arrow and abbreviation of the nearest of the 8 compass points, e.g. 350° is north. */
    public String compassPoint(double degrees) {
        return compassPoints.get(Math.floorMod(Math.round(degrees / 45), compassPoints.size()));
    }
}
