package lv.meteobot;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

/** Test data: recorded LVĢMC responses and JSON parsing like {@link Lvgmc} does it. */
final class Fixtures {

    private Fixtures() {
    }

    static String read(String name) {
        try (InputStream in = Fixtures.class.getResourceAsStream("/lvgmc/" + name)) {
            if (in == null) {
                throw new IllegalArgumentException("No fixture " + name);
            }
            return new String(in.readAllBytes(), UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static <T> List<T> parseList(String json, Class<T> type) {
        try {
            return Lvgmc.JSON.readValue(json, Lvgmc.JSON.getTypeFactory().constructCollectionType(List.class, type));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static <T> T parse(String json, Class<T> type) {
        try {
            return Lvgmc.JSON.readValue(json, type);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
