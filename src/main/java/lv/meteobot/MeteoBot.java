package lv.meteobot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.longpolling.util.DefaultLongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.message.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.sql.SQLException;
import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/** Telegram command handlers. */
final class MeteoBot extends DefaultLongPollingUpdateConsumer {

    private static final Logger log = LoggerFactory.getLogger("meteobot");

    static final int DEFAULT_FORECAST_HOURS = 12;
    private static final int MAX_FORECAST_HOURS = 48;
    private static final int MAX_MESSAGE_LENGTH = 4096;
    private static final String LANGUAGE_CALLBACK_PREFIX = "lang_";
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final ReplyKeyboardRemove REMOVE_KEYBOARD = new ReplyKeyboardRemove(true);

    /** A command addressed to this bot, e.g. {@code /forecast_h Ventspils 24}. */
    private record Command(String name, List<String> args) {
    }

    /** Who is in the middle of picking a city after /city; per chat and user, as several may do so in a group. */
    private record ConversationKey(long chatId, long userId) {
    }

    private final TelegramClient telegram;
    private final String botUsername;
    private final Database db;
    private final WeatherReports reports;
    private final Set<ConversationKey> waitingForCity = ConcurrentHashMap.newKeySet();

    MeteoBot(TelegramClient telegram, String botUsername, Database db, WeatherReports reports) {
        this.telegram = telegram;
        this.botUsername = botUsername;
        this.db = db;
        this.reports = reports;
    }

    @Override
    public void consume(Update update) {
        try {
            handle(update);
        } catch (Exception e) {
            log.error("Failed to handle update {}", update.getUpdateId(), e);
        }
    }

    /** Sends a Markdown message, split if it is too long for one; used for scheduled messages. */
    void send(long chatId, String text) throws TelegramApiException {
        for (String part : split(text)) {
            telegram.execute(SendMessage.builder().chatId(chatId).text(part).parseMode(ParseMode.MARKDOWN).build());
        }
    }

    private void handle(Update update) throws TelegramApiException, SQLException {
        if (update.hasCallbackQuery()) {
            CallbackQuery query = update.getCallbackQuery();
            if (query.getData() != null && query.getData().startsWith(LANGUAGE_CALLBACK_PREFIX)) {
                languageSelected(query);
            }
            return;
        }
        Message message = update.getMessage();
        if (message == null || !message.hasText() || message.getFrom() == null) {
            return;
        }

        ConversationKey conversation = new ConversationKey(message.getChatId(), message.getFrom().getId());
        if (!message.isCommand()) {
            if (waitingForCity.remove(conversation)) {
                citySelected(message);
            }
            return;
        }
        Optional<Command> command = command(message);
        if (command.isEmpty()) {
            return;
        }

        List<String> args = command.get().args();
        switch (command.get().name()) {
            case "city" -> {
                if (city(message, args)) {
                    waitingForCity.add(conversation);
                } else {
                    waitingForCity.remove(conversation);
                }
            }
            case "start" -> start(message);
            case "help" -> help(message);
            case "weather", "weater" -> weather(message, args);
            case "forecast" -> forecast(message, args);
            case "forecast_h" -> hourlyForecast(message, args);
            case "synoptic" -> synoptic(message);
            case "schedule" -> schedule(message, args);
            case "unschedule" -> unschedule(message, args);
            case "language" -> language(message);
            default -> {
            }
        }
    }

    /** {@code /start} */
    private void start(Message message) throws TelegramApiException, SQLException {
        User user = message.getFrom();
        log.info("/start from user {} ({})", user.getId(), fullName(user));
        Language lang;
        if (db.userExists(user.getId())) {
            lang = db.getUserLanguage(user.getId());
        } else {
            lang = Language.ofTelegramUser(user.getLanguageCode());
            db.setUserLanguage(user.getId(), lang);
        }
        reply(message, Text.START.get(lang), ParseMode.MARKDOWN, null);
    }

    /** {@code /city [name]}; returns whether the bot now waits for the user to pick a city. */
    private boolean city(Message message, List<String> args) throws TelegramApiException, SQLException {
        long userId = message.getFrom().getId();
        log.info("/city from user {}", userId);
        Language lang = db.getUserLanguage(userId);

        if (!args.isEmpty()) {
            String query = String.join(" ", args);
            Optional<String> city = Lvgmc.findCity(query);
            if (city.isPresent()) {
                db.setUserCity(userId, city.get());
                reply(message, Text.CITY_SET.get(lang, city.get()), ParseMode.MARKDOWN, REMOVE_KEYBOARD);
            } else {
                reply(message, Text.CITY_NOT_FOUND.get(lang, query), null, REMOVE_KEYBOARD);
            }
            return false;
        }

        List<KeyboardRow> rows = new ArrayList<>();
        List<String> cities = Lvgmc.POPULAR_CITIES;
        for (int i = 0; i < cities.size(); i += 2) {
            rows.add(new KeyboardRow(cities.subList(i, Math.min(i + 2, cities.size())).toArray(String[]::new)));
        }
        rows.add(new KeyboardRow(Text.CITY_CANCEL_BUTTON.get(lang)));
        ReplyKeyboardMarkup keyboard = ReplyKeyboardMarkup.builder()
                .keyboard(rows)
                .oneTimeKeyboard(true)
                .resizeKeyboard(true)
                .build();
        reply(message, Text.CITY_PROMPT.get(lang), null, keyboard);
        return true;
    }

    /** The answer to /city: a button press or a typed city name. */
    private void citySelected(Message message) throws TelegramApiException, SQLException {
        String text = message.getText();
        long userId = message.getFrom().getId();
        log.info("City selection: '{}' from user {}", text, userId);
        Language lang = db.getUserLanguage(userId);

        if (text.equals(Text.CITY_CANCEL_BUTTON.get(lang))) {
            reply(message, Text.CITY_CANCELLED.get(lang), null, REMOVE_KEYBOARD);
            return;
        }
        Optional<String> city = Lvgmc.findCity(text);
        if (city.isEmpty()) {
            reply(message, Text.CITY_NOT_FOUND.get(lang, text), null, REMOVE_KEYBOARD);
            return;
        }
        db.setUserCity(userId, city.get());
        log.info("User {} set city to '{}'", userId, city.get());
        reply(message, Text.CITY_SET.get(lang, city.get()), ParseMode.MARKDOWN, REMOVE_KEYBOARD);
    }

    /** {@code /weather [city]} */
    private void weather(Message message, List<String> args) throws TelegramApiException, SQLException {
        long userId = message.getFrom().getId();
        Language lang = db.getUserLanguage(userId);
        String city = args.isEmpty() ? db.getUserCity(userId) : String.join(" ", args);
        log.info("/weather from user {} for city '{}'", userId, city);
        reply(message, reports.currentWeather(city, lang), ParseMode.MARKDOWN, null);
    }

    /** {@code /forecast [city]}: 7 days. */
    private void forecast(Message message, List<String> args) throws TelegramApiException, SQLException {
        long userId = message.getFrom().getId();
        Language lang = db.getUserLanguage(userId);
        String city = args.isEmpty() ? db.getUserCity(userId) : String.join(" ", args);
        log.info("/forecast from user {} for city '{}'", userId, city);

        Optional<String> forecastCity = Lvgmc.findCity(city);
        if (forecastCity.isEmpty()) {
            reply(message, Text.CITY_NOT_FOUND_HINT.get(lang, Markdown.escape(city)), ParseMode.MARKDOWN, null);
            return;
        }
        replyInParts(message, reports.dailyForecast(forecastCity.get(), lang));
    }

    /** {@code /forecast_h [city] [hours]}: by hour, 12 hours unless said otherwise. */
    private void hourlyForecast(Message message, List<String> args) throws TelegramApiException, SQLException {
        long userId = message.getFrom().getId();
        Language lang = db.getUserLanguage(userId);
        int hours = DEFAULT_FORECAST_HOURS;
        List<String> cityWords = args;
        if (!args.isEmpty()) {
            try {
                hours = Math.clamp(Integer.parseInt(args.getLast()), 1, MAX_FORECAST_HOURS);
                cityWords = args.subList(0, args.size() - 1);
            } catch (NumberFormatException e) {
                // no hours given, all words are the city
            }
        }
        String city = cityWords.isEmpty() ? db.getUserCity(userId) : String.join(" ", cityWords);
        log.info("/forecast_h from user {} for city '{}'", userId, city);

        Optional<String> forecastCity = Lvgmc.findCity(city);
        if (forecastCity.isEmpty()) {
            reply(message, Text.CITY_NOT_FOUND_HINT.get(lang, Markdown.escape(city)), ParseMode.MARKDOWN, null);
            return;
        }
        replyInParts(message, reports.hourlyForecast(forecastCity.get(), hours, lang));
    }

    /** {@code /synoptic}: the national text forecast. */
    private void synoptic(Message message) throws TelegramApiException, SQLException {
        long userId = message.getFrom().getId();
        log.info("/synoptic from user {}", userId);
        replyInParts(message, reports.synopticForecast(db.getUserLanguage(userId)));
    }

    /**
     * {@code /schedule [HH:MM [weather|forecast|forecast_h]]}: list the user's schedules, or set one, by default
     * of the hourly forecast. An unknown type sets nothing and lists the valid ones.
     */
    private void schedule(Message message, List<String> args) throws TelegramApiException, SQLException {
        long userId = message.getFrom().getId();
        Language lang = db.getUserLanguage(userId);
        String city = db.getUserCity(userId);

        if (args.isEmpty()) {
            List<Database.Schedule> schedules = db.getUserSchedules(userId);
            if (schedules.isEmpty()) {
                reply(message, Text.SCHEDULE_NONE.get(lang) + "\n\n" + Text.SCHEDULE_USAGE.get(lang), ParseMode.MARKDOWN, null);
                return;
            }
            List<String> lines = new ArrayList<>();
            lines.add(Text.SCHEDULE_LIST_TITLE.get(lang) + "\n");
            for (Database.Schedule s : schedules) {
                lines.add(Text.SCHEDULE_LIST_ITEM.get(lang, s.time().format(HH_MM), s.type().description(lang), city));
            }
            lines.add(Text.SCHEDULE_SET_HINT.get(lang));
            reply(message, String.join("\n", lines), ParseMode.MARKDOWN, null);
            return;
        }

        LocalTime time;
        try {
            time = parseTime(args.getFirst());
        } catch (NumberFormatException e) {
            reply(message, Text.SCHEDULE_INVALID_FORMAT.get(lang), ParseMode.MARKDOWN, null);
            return;
        } catch (DateTimeException e) {
            reply(message, Text.SCHEDULE_INVALID_TIME.get(lang), ParseMode.MARKDOWN, null);
            return;
        }
        ScheduleType type = ScheduleType.FORECAST_H;
        if (args.size() > 1) {
            Optional<ScheduleType> requested = ScheduleType.fromCode(args.get(1));
            if (requested.isEmpty()) {
                reply(message, unknownScheduleType(args.get(1), lang), ParseMode.MARKDOWN, null);
                return;
            }
            type = requested.get();
        }

        db.setSchedule(userId, time, type);
        log.info("Schedule set: user {}, {} {} ({})", userId, time, type.code(), city);
        reply(message, Text.SCHEDULE_SET.get(lang) + "\n"
                        + Text.SCHEDULE_DETAIL.get(lang, time.format(HH_MM), type.description(lang), city)
                        + Text.SCHEDULE_REMOVE_HINT.get(lang),
                ParseMode.MARKDOWN, null);
    }

    /**
     * {@code /unschedule [weather|forecast|forecast_h]}: remove one schedule, or all of them. An unknown type
     * removes nothing and lists the valid ones.
     */
    private void unschedule(Message message, List<String> args) throws TelegramApiException, SQLException {
        long userId = message.getFrom().getId();
        Language lang = db.getUserLanguage(userId);
        log.info("/unschedule from user {} (type={})", userId, args.isEmpty() ? "all" : args.getFirst());

        if (args.isEmpty()) {
            db.removeAllSchedules(userId);
            reply(message, Text.UNSCHEDULE_REMOVED_ALL.get(lang), ParseMode.MARKDOWN, null);
            return;
        }
        Optional<ScheduleType> type = ScheduleType.fromCode(args.getFirst());
        if (type.isEmpty()) {
            reply(message, unknownScheduleType(args.getFirst(), lang), ParseMode.MARKDOWN, null);
            return;
        }
        db.removeSchedule(userId, type.get());
        reply(message, Text.UNSCHEDULE_REMOVED_TYPE.get(lang, Markdown.escape(type.get().code())), ParseMode.MARKDOWN, null);
    }

    /** {@code /help} */
    private void help(Message message) throws TelegramApiException, SQLException {
        long userId = message.getFrom().getId();
        log.info("/help from user {}", userId);
        Language lang = db.getUserLanguage(userId);
        List<Database.Schedule> schedules = db.getUserSchedules(userId);

        String scheduleLines = schedules.isEmpty()
                ? Text.HELP_NO_SCHEDULES.get(lang)
                : schedules.stream()
                .map(s -> "  ⏰ " + s.time().format(HH_MM) + " — " + s.type().helpLabel(lang))
                .collect(Collectors.joining("\n"));
        reply(message, Text.HELP.get(lang, db.getUserCity(userId), scheduleLines), ParseMode.MARKDOWN, null);
    }

    /** {@code /language}: buttons for each language. */
    private void language(Message message) throws TelegramApiException, SQLException {
        long userId = message.getFrom().getId();
        Language current = db.getUserLanguage(userId);
        log.info("/language from user {} (current: {})", userId, current.code());

        List<InlineKeyboardRow> rows = Arrays.stream(Language.values())
                .map(lang -> new InlineKeyboardRow(InlineKeyboardButton.builder()
                        .text(lang.label() + (lang == current ? " ✅" : ""))
                        .callbackData(LANGUAGE_CALLBACK_PREFIX + lang.code())
                        .build()))
                .toList();
        reply(message, Text.LANGUAGE_PROMPT.get(current), null, new InlineKeyboardMarkup(rows));
    }

    /** A button press from {@link #language}. */
    private void languageSelected(CallbackQuery query) throws TelegramApiException, SQLException {
        telegram.execute(new AnswerCallbackQuery(query.getId()));
        long userId = query.getFrom().getId();
        Language lang = Language.fromCode(query.getData().substring(LANGUAGE_CALLBACK_PREFIX.length()));
        db.setUserLanguage(userId, lang);
        log.info("User {} set language to '{}'", userId, lang.code());

        EditMessageText.EditMessageTextBuilder<?, ?> edit = EditMessageText.builder().text(Text.LANGUAGE_SET.get(lang));
        MaybeInaccessibleMessage original = query.getMessage();
        if (original != null) {
            edit.chatId(original.getChatId()).messageId(original.getMessageId());
        } else {
            edit.inlineMessageId(query.getInlineMessageId());
        }
        telegram.execute(edit.build());
    }

    /** The command if it is meant for this bot: {@code /name}, or {@code /name@ThisBot} as used in groups. */
    private Optional<Command> command(Message message) {
        String command = message.getCommand();
        String[] nameAndBot = command.substring(1).split("@", 2);
        if (nameAndBot.length == 2 && !nameAndBot[1].equalsIgnoreCase(botUsername)) {
            return Optional.empty();
        }
        String rest = message.getText().substring(command.length()).strip();
        List<String> args = rest.isEmpty() ? List.of() : List.of(rest.split("\\s+"));
        return Optional.of(new Command(nameAndBot[0].toLowerCase(Locale.ROOT), args));
    }

    /** Replies in the chat of {@code message}; in groups as a reply to it, so it is clear whom the answer is for. */
    private void reply(Message message, String text, String parseMode, ReplyKeyboard keyboard) throws TelegramApiException {
        SendMessage.SendMessageBuilder<?, ?> send = SendMessage.builder()
                .chatId(message.getChatId())
                .text(text)
                .parseMode(parseMode)
                .replyMarkup(keyboard);
        if (!message.getChat().isUserChat()) {
            send.replyToMessageId(message.getMessageId());
        }
        if (message.isTopicMessage()) {
            send.messageThreadId(message.getMessageThreadId());
        }
        telegram.execute(send.build());
    }

    private void replyInParts(Message message, String text) throws TelegramApiException {
        for (String part : split(text)) {
            reply(message, part, ParseMode.MARKDOWN, null);
        }
    }

    /** For a mistyped schedule type: lists the valid ones, as code so that tapping one copies it. */
    private static String unknownScheduleType(String typed, Language lang) {
        String validTypes = Arrays.stream(ScheduleType.values())
                .map(type -> "`" + type.code() + "` — " + type.description(lang))
                .collect(Collectors.joining("\n"));
        return Text.SCHEDULE_UNKNOWN_TYPE.get(lang, Markdown.escape(typed), validTypes);
    }

    /** {@code "9"} or {@code "9:30"}. */
    private static LocalTime parseTime(String text) {
        String[] parts = text.split(":", -1);
        int hour = Integer.parseInt(parts[0]);
        int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        return LocalTime.of(hour, minute);
    }

    /** Splits at line ends into messages within Telegram's length limit. */
    private static List<String> split(String text) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + MAX_MESSAGE_LENGTH, text.length());
            if (end < text.length()) {
                int lineEnd = text.lastIndexOf('\n', end - 1);
                if (lineEnd > start) {
                    end = lineEnd + 1;
                } else if (Character.isHighSurrogate(text.charAt(end - 1))) {
                    end--;
                }
            }
            parts.add(text.substring(start, end));
            start = end;
        }
        return parts;
    }

    private static String fullName(User user) {
        return user.getLastName() == null ? user.getFirstName() : user.getFirstName() + " " + user.getLastName();
    }
}
