package lv.meteobot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LanguageTest {

    @ParameterizedTest(name = "{0}° is {1}")
    @CsvSource({
            "0, ⬆️N", "22.4, ⬆️N", "22.5, ↗️NE", "90, ➡️E", "182, ⬇️S",
            "337.4, ↖️NW", "337.5, ⬆️N", "350, ⬆️N", "360, ⬆️N", "-10, ⬆️N"})
    void compassPoint(double degrees, String expected) {
        assertEquals(expected, Language.EN.compassPoint(degrees));
    }

    @Test
    void latvianCompassPoints() {
        assertEquals("⬆️Z", Language.LV.compassPoint(0));
        assertEquals("↙️DR", Language.LV.compassPoint(225));
    }

    @Test
    void latvianWeekdays() {
        assertEquals(List.of("Pir", "Otr", "Tre", "Cet", "Pie", "Ses", "Svē"),
                Arrays.stream(DayOfWeek.values()).map(Language.LV::dayAbbreviation).toList());
    }

    @ParameterizedTest(name = "\"{0}\" is {1}")
    @CsvSource(value = {"en, EN", "en-US, EN", "en-GB, EN", "lv, LV", "ru, LV", "'', LV", "NULL, LV"}, nullValues = "NULL")
    void languageOfTelegramUser(String languageCode, Language expected) {
        assertEquals(expected, Language.ofTelegramUser(languageCode));
    }

    @ParameterizedTest(name = "\"{0}\" is {1}")
    @CsvSource(value = {"en, EN", "lv, LV", "xx, LV", "NULL, LV"}, nullValues = "NULL")
    void fromCode(String code, Language expected) {
        assertEquals(expected, Language.fromCode(code));
    }
}
