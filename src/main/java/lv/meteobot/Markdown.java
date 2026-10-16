package lv.meteobot;

import java.util.regex.Pattern;

/** Telegram's legacy Markdown, the parse mode of the bot's formatted messages. */
final class Markdown {

    private static final Pattern SPECIAL = Pattern.compile("[_*`\\[]");

    private Markdown() {
    }

    /**
     * Escapes text from users or LVĢMC, so that it shows as typed instead of Telegram rejecting the whole message
     * over an unmatched {@code _} or {@code *}. Telegram only honours the escapes outside {@code *bold*} and
     * {@code _italic_} text.
     */
    static String escape(String text) {
        return SPECIAL.matcher(text).replaceAll("\\\\$0");
    }
}
