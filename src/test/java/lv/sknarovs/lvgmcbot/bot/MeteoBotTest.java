package lv.sknarovs.lvgmcbot.bot;

import com.fasterxml.jackson.databind.ObjectMapper;
import lv.sknarovs.lvgmcbot.i18n.Language;
import lv.sknarovs.lvgmcbot.lvgmc.FakeLvgmc;
import lv.sknarovs.lvgmcbot.lvgmc.Lvgmc;
import lv.sknarovs.lvgmcbot.model.Schedule;
import lv.sknarovs.lvgmcbot.model.ScheduleType;
import lv.sknarovs.lvgmcbot.storage.Database;
import lv.sknarovs.lvgmcbot.weather.WeatherReports;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static lv.sknarovs.lvgmcbot.util.MarkdownAssert.assertValidMarkdown;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Command routing and replies; LVĢMC is faked with recorded data, observed 40 minutes before "now". */
class MeteoBotTest {

    private static final long USER = 42;
    private static final long GROUP = -100;
    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir
    Path dir;

    private final RecordingTelegram telegram = new RecordingTelegram();
    private FakeLvgmc lvgmc;
    private Database db;
    private MeteoBot bot;
    private int nextMessageId = 1;

    @BeforeEach
    void setUp() throws Exception {
        lvgmc = FakeLvgmc.withRecordedData();
        db = new Database(dir.resolve("meteobot.db").toString());
        db.init();
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T20:40:00Z"), Lvgmc.ZONE);
        bot = new MeteoBot(telegram.client(), "MeteoTestBot", db, new WeatherReports(lvgmc.client(), clock));
    }

    @AfterEach
    void tearDown() throws Exception {
        for (SendMessage message : telegram.messages()) {
            if (ParseMode.MARKDOWN.equals(message.getParseMode())) {
                assertValidMarkdown(message.getText());
            }
        }
        db.close();
        lvgmc.close();
    }

    @Test
    void startDetectsEnglishFromTheTelegramApp() throws Exception {
        send(message("/start", USER, "private", "en-GB"));
        assertTrue(lastText().startsWith("🌤️ *LVĢMC Weather Bot*"));
        assertEquals(ParseMode.MARKDOWN, lastMessage().getParseMode());
        assertEquals(Language.EN, db.getUserLanguage(USER));

        send(message("/start", 7, "private", "ru"));
        assertTrue(lastText().startsWith("🌤️ *LVĢMC Laika apstākļu bots*"));
        assertEquals(Language.LV, db.getUserLanguage(7));
    }

    @Test
    void startKeepsTheChosenLanguage() throws Exception {
        db.setUserLanguage(USER, Language.LV);
        send(message("/start", USER, "private", "en"));
        assertTrue(lastText().startsWith("🌤️ *LVĢMC Laika apstākļu bots*"));
    }

    @Test
    void cityConversationShowsKeyboardThenAcceptsAnyMatchingName() throws Exception {
        send(message("/city", USER, "private", null));
        assertEquals("📍 Izvēlies pilsētu (vai uzraksti jebkuru pilsētas nosaukumu):", lastText());
        ReplyKeyboardMarkup keyboard = assertInstanceOf(ReplyKeyboardMarkup.class, lastMessage().getReplyMarkup());
        assertEquals(List.of(
                        List.of("Rīga", "Daugavpils"), List.of("Liepāja", "Jelgava"), List.of("Jūrmala", "Ventspils"),
                        List.of("Rēzekne", "Ogre"), List.of("Valmiera", "Jēkabpils"), List.of("❌ Atcelt")),
                buttonTexts(keyboard));
        assertEquals(true, keyboard.getOneTimeKeyboard());
        assertEquals(true, keyboard.getResizeKeyboard());

        send(message("liep", USER, "private", null));
        assertEquals("✅ Pilsēta iestatīta: *Liepāja*", lastText());
        assertEquals("Liepāja", db.getUserCity(USER));
        assertInstanceOf(ReplyKeyboardRemove.class, lastMessage().getReplyMarkup());

        int before = telegram.executed.size();
        send(message("Ogre", USER, "private", null));
        assertEquals(before, telegram.executed.size(), "plain text outside the conversation is ignored");
    }

    @Test
    void otherCommandsWorkWhileWaitingForCity() throws Exception {
        send(message("/city", USER, "private", null));
        send(message("/help", USER, "private", null));
        assertTrue(lastText().startsWith("🌤️ *LVĢMC Laika apstākļu bots*\n\n📍 *Pilsēta:*"));

        send(message("Ogre", USER, "private", null));
        assertEquals("✅ Pilsēta iestatīta: *Ogre*", lastText());
    }

    @Test
    void cityConversationCanBeCancelledOrFail() throws Exception {
        send(message("/city", USER, "private", null));
        send(message("❌ Atcelt", USER, "private", null));
        assertEquals("Pilsētas izvēle atcelta.", lastText());

        send(message("/city", USER, "private", null));
        send(message("London_2", USER, "private", null));
        assertEquals("❌ Pilsēta \"London_2\" nav atrasta.", lastText());
        assertNull(lastMessage().getParseMode());
        assertEquals("Rīga", db.getUserCity(USER));
    }

    @Test
    void cityWithArgumentSetsDirectly() throws Exception {
        send(message("/city jur mala", USER, "private", null));
        assertEquals("✅ Pilsēta iestatīta: *Jūrmala*", lastText());

        int before = telegram.executed.size();
        send(message("Ogre", USER, "private", null));
        assertEquals(before, telegram.executed.size());
    }

    @Test
    void commandsForOtherBotsAreIgnoredAndCaseDoesNotMatter() throws Exception {
        send(message("/help@OtherBot", USER, "private", null));
        assertTrue(telegram.executed.isEmpty());

        send(message("/HELP@meteotestbot", USER, "private", null));
        assertEquals(1, telegram.executed.size());
    }

    @Test
    void repliesQuoteTheCommandOnlyInGroups() throws Exception {
        send(message("/help", USER, "private", null));
        assertNull(lastMessage().getReplyToMessageId());

        send(message("/help", USER, "supergroup", null));
        assertEquals(String.valueOf(GROUP), lastMessage().getChatId());
        assertEquals(nextMessageId - 1, lastMessage().getReplyToMessageId());
    }

    @Test
    void weatherForTheUsersCity() throws Exception {
        send(message("/weather", USER, "private", null));
        assertTrue(lastText().startsWith("📍 *Rīga Universitāte* — 2026.10.16 23:00:00\n"), lastText());
    }

    @Test
    void forecastForUnknownCityIsEscaped() throws Exception {
        send(message("/forecast foo_bar", USER, "private", null));
        assertEquals("❌ Pilsēta nav atrasta: foo\\_bar\n\nIzmanto /city lai izvēlētos.", lastText());
    }

    @Test
    void hourlyForecastTakesCityAndHours() throws Exception {
        send(message("/forecast_h 2", USER, "private", null));
        assertEquals("""
                🕐 *Stundu prognoze: Rīga*

                🌙 00:00 *6.3°C*
                🌙 01:00 *5.7°C*""", lastText());

        send(message("/forecast_h Ventspils 24", USER, "private", null));
        assertEquals("❌ Nav stundu prognozes datu par: Ventspils", lastText());
    }

    @Test
    void scheduleDefaultsToHourlyForecastAndLists() throws Exception {
        send(message("/schedule", USER, "private", null));
        assertEquals("❌ Nav iestatīts grafiks.\n\nLieto: `/schedule 9:00` vai `/schedule 9:00 weather`", lastText());

        send(message("/schedule 7:05", USER, "private", null));
        assertEquals("✅ Iestatīts!\n⏰ 07:05 — stundu prognoze\n📍 Pilsēta: Rīga\n\nLai noņemtu: /unschedule", lastText());

        send(message("/schedule 9 WEATHER", USER, "private", null));
        assertEquals("✅ Iestatīts!\n⏰ 09:00 — pašreizējie laika apstākļi\n📍 Pilsēta: Rīga\n\nLai noņemtu: /unschedule", lastText());

        send(message("/schedule", USER, "private", null));
        assertEquals("📅 Tavi grafiki:\n\n  ⏰ 07:05 — stundu prognoze (Rīga)\n  ⏰ 09:00 — pašreizējie laika apstākļi (Rīga)"
                + "\n\nLai iestatītu: /schedule HH:MM", lastText());

        send(message("/schedule 24:00", USER, "private", null));
        assertEquals("❌ Nepareizs laiks. Stundas 0-23, minūtes 0-59.", lastText());
        send(message("/schedule 9:xx", USER, "private", null));
        assertEquals("❌ Nepareizs formāts. Lieto: `/schedule 9:00`", lastText());
        assertEquals(2, db.getUserSchedules(USER).size());
    }

    @Test
    void scheduleWithUnknownTypeListsTheValidOnesAndSetsNothing() throws Exception {
        send(message("/schedule 9:00 wether", USER, "private", null));

        assertEquals("""
                ❌ Nezināms grafika veids: wether

                Iespējamie veidi:
                `weather` — pašreizējie laika apstākļi
                `forecast` — dienas prognoze
                `forecast_h` — stundu prognoze""", lastText());
        assertTrue(db.getUserSchedules(USER).isEmpty());
    }

    @Test
    void unscheduleOneOrAll() throws Exception {
        db.setSchedule(USER, LocalTime.of(8, 0), ScheduleType.FORECAST_H);
        db.setSchedule(USER, LocalTime.of(9, 0), ScheduleType.FORECAST);

        send(message("/unschedule Forecast_H", USER, "private", null));
        assertEquals("✅ Noņemts forecast\\_h grafiks.", lastText());
        assertEquals(List.of(new Schedule(LocalTime.of(9, 0), ScheduleType.FORECAST)), db.getUserSchedules(USER));

        send(message("/unschedule", USER, "private", null));
        assertEquals("✅ Visi grafiki noņemti.", lastText());
        assertTrue(db.getUserSchedules(USER).isEmpty());
    }

    @Test
    void unscheduleWithUnknownTypeListsTheValidOnesAndRemovesNothing() throws Exception {
        db.setSchedule(USER, LocalTime.of(8, 0), ScheduleType.FORECAST_H);

        send(message("/unschedule forecast_hh", USER, "private", null));

        assertEquals("""
                ❌ Nezināms grafika veids: forecast\\_hh

                Iespējamie veidi:
                `weather` — pašreizējie laika apstākļi
                `forecast` — dienas prognoze
                `forecast_h` — stundu prognoze""", lastText());
        assertEquals(List.of(new Schedule(LocalTime.of(8, 0), ScheduleType.FORECAST_H)), db.getUserSchedules(USER));

        db.setUserLanguage(USER, Language.EN);
        send(message("/unschedule x", USER, "private", null));
        assertTrue(lastText().startsWith("❌ Unknown schedule type: x\n\nValid types:\n`weather` — current weather\n"), lastText());
    }

    @Test
    void helpListsSchedules() throws Exception {
        send(message("/help", USER, "private", null));
        assertTrue(lastText().endsWith("Tavi grafiki:\n  Nav iestatītu grafiku."), lastText());

        db.setSchedule(USER, LocalTime.of(6, 30), ScheduleType.FORECAST);
        db.setSchedule(USER, LocalTime.of(7, 0), ScheduleType.WEATHER);
        send(message("/help", USER, "private", null));
        assertTrue(lastText().endsWith("Tavi grafiki:\n  ⏰ 06:30 — 📊 Prognoze\n  ⏰ 07:00 — 🌤️ Laika apstākļi"), lastText());
    }

    @Test
    void languageKeyboardAndCallback() throws Exception {
        send(message("/language", USER, "private", null));
        assertEquals("🌐 Izvēlies valodu / Choose language:", lastText());
        InlineKeyboardMarkup keyboard = assertInstanceOf(InlineKeyboardMarkup.class, lastMessage().getReplyMarkup());
        assertEquals("🇱🇻 Latviešu ✅", keyboard.getKeyboard().get(0).get(0).getText());
        assertEquals("lang_lv", keyboard.getKeyboard().get(0).get(0).getCallbackData());
        assertEquals("🇬🇧 English", keyboard.getKeyboard().get(1).get(0).getText());

        send(update("""
                {"update_id": 1, "callback_query": {"id": "cb1", "chat_instance": "x", "data": "lang_en",
                 "from": {"id": %d, "is_bot": false, "first_name": "A"},
                 "message": {"message_id": 77, "date": 1760630400, "chat": {"id": %d, "type": "private"}, "text": "x"}}}
                """.formatted(USER, USER)));
        List<Object> executed = telegram.executed;
        assertEquals("cb1", assertInstanceOf(AnswerCallbackQuery.class, executed.get(executed.size() - 2)).getCallbackQueryId());
        EditMessageText edit = assertInstanceOf(EditMessageText.class, executed.getLast());
        assertEquals("✅ Language set: 🇬🇧 English", edit.getText());
        assertEquals(String.valueOf(USER), edit.getChatId());
        assertEquals(77, edit.getMessageId());
        assertEquals(Language.EN, db.getUserLanguage(USER));
    }

    private void send(Update update) {
        bot.consume(update);
    }

    private Update message(String text, long userId, String chatType, String languageCode) throws Exception {
        long chatId = chatType.equals("private") ? userId : GROUP;
        String entities = "";
        if (text.startsWith("/")) {
            int length = text.contains(" ") ? text.indexOf(' ') : text.length();
            entities = ", \"entities\": [{\"type\": \"bot_command\", \"offset\": 0, \"length\": %d}]".formatted(length);
        }
        return update("""
                {"update_id": 1, "message": {"message_id": %d, "date": 1760630400, "text": %s%s,
                 "chat": {"id": %d, "type": "%s"},
                 "from": {"id": %d, "is_bot": false, "first_name": "A"%s}}}
                """.formatted(nextMessageId++, JSON.writeValueAsString(text), entities, chatId, chatType, userId,
                languageCode == null ? "" : ", \"language_code\": \"" + languageCode + "\""));
    }

    private static Update update(String json) throws Exception {
        return JSON.readValue(json, Update.class);
    }

    private SendMessage lastMessage() {
        return assertInstanceOf(SendMessage.class, telegram.executed.getLast());
    }

    private String lastText() {
        return lastMessage().getText();
    }

    private static List<List<String>> buttonTexts(ReplyKeyboardMarkup keyboard) {
        List<List<String>> rows = new ArrayList<>();
        for (KeyboardRow row : keyboard.getKeyboard()) {
            rows.add(row.stream().map(KeyboardButton::getText).toList());
        }
        return rows;
    }
}
