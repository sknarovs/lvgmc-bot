package lv.meteobot;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static lv.meteobot.MarkdownAssert.assertValidMarkdown;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TextTest {

    private static final Pattern SPECIFIER = Pattern.compile("%(\\d+\\$)?[-#+ 0,(<]*\\d*(\\.\\d+)?[a-zA-Z%]");

    @Test
    void translationsHaveTheSamePlaceholders() {
        for (Text text : Text.values()) {
            assertEquals(specifiers(text.template(Language.LV)), specifiers(text.template(Language.EN)), text.name());
        }
    }

    @Test
    void everyTextIsValidMarkdown() {
        for (Text text : Text.values()) {
            for (Language lang : Language.values()) {
                assertValidMarkdown(text.get(lang, sampleArguments(text.template(lang))));
            }
        }
    }

    private static List<String> specifiers(String template) {
        List<String> specifiers = new ArrayList<>();
        Matcher m = SPECIFIER.matcher(template);
        while (m.find()) {
            specifiers.add(m.group());
        }
        return specifiers;
    }

    private static Object[] sampleArguments(String template) {
        return specifiers(template).stream()
                .filter(specifier -> !specifier.equals("%%"))
                .map(specifier -> switch (specifier.charAt(specifier.length() - 1)) {
                    case 'd' -> (Object) 7;
                    case 'f' -> 1.5;
                    default -> "Rīga";
                })
                .toArray();
    }
}
