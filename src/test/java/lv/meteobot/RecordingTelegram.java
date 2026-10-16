package lv.meteobot;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** A {@link TelegramClient} that records what the bot executes instead of calling Telegram. */
final class RecordingTelegram {

    final List<Object> executed = new CopyOnWriteArrayList<>();

    TelegramClient client() {
        return (TelegramClient) Proxy.newProxyInstance(TelegramClient.class.getClassLoader(),
                new Class<?>[]{TelegramClient.class}, (proxy, method, args) -> {
                    if (!method.getName().equals("execute")) {
                        throw new UnsupportedOperationException(method.getName());
                    }
                    executed.add(args[0]);
                    return null;
                });
    }

    List<SendMessage> messages() {
        return executed.stream().filter(SendMessage.class::isInstance).map(SendMessage.class::cast).toList();
    }
}
