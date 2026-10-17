package lv.sknarovs.lvgmcbot.util;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Checks text the way the Bot API's legacy Markdown parser does (tdlib {@code parse_markdown}): every
 * {@code _ * ` [} opens an entity that must be closed by the same character ({@code ]} for {@code [}),
 * unless escaped with a backslash outside an entity. Otherwise, Telegram rejects the whole message.
 */
public final class MarkdownAssert {

    private static final String SPECIAL = "_*`[";

    private MarkdownAssert() {
    }

    public static void assertValidMarkdown(String text) {
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length() && SPECIAL.indexOf(text.charAt(i + 1)) >= 0) {
                i += 2;
            } else if (SPECIAL.indexOf(c) >= 0) {
                int end = text.indexOf(c == '[' ? ']' : c, i + 1);
                if (end < 0) {
                    fail("Telegram would reject this message, '" + c + "' at " + i + " is never closed:\n" + text);
                }
                i = end + 1;
            } else {
                i++;
            }
        }
    }
}
