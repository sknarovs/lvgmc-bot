package lv.meteobot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchedulerTest {

    @TempDir
    Path dir;

    @Test
    void sendsWhatIsScheduledForThatMinute() throws Exception {
        try (FakeLvgmc server = FakeLvgmc.withRecordedData();
             Database db = new Database(dir.resolve("meteobot.db").toString())) {
            db.init();
            db.setSchedule(1, LocalTime.of(7, 30), ScheduleType.FORECAST);
            db.setSchedule(2, LocalTime.of(7, 30), ScheduleType.WEATHER);
            db.setUserCity(2, "Ape");
            db.setUserLanguage(2, Language.EN);
            db.setSchedule(3, LocalTime.of(8, 0), ScheduleType.FORECAST);
            server.respond("weather_forecast_for_location_hourly?nosaukums=Ape",
                    Fixtures.read("weather_forecast_for_location_hourly_riga.json"));

            Clock clock = Clock.fixed(Instant.parse("2026-10-16T20:00:00Z"), Lvgmc.ZONE);
            WeatherReports reports = new WeatherReports(server.client(), clock);
            RecordingTelegram telegram = new RecordingTelegram();
            Scheduler scheduler = new Scheduler(db, reports, new MeteoBot(telegram.client(), "MeteoTestBot", db, reports), clock);

            scheduler.sendScheduledMessages(LocalTime.of(7, 30));

            Map<String, String> textByChat = telegram.messages().stream()
                    .collect(Collectors.toMap(SendMessage::getChatId, SendMessage::getText));
            assertEquals(2, textByChat.size());
            assertTrue(textByChat.get("1").startsWith("📅 *Prognoze: Rīga*"), textByChat.get("1"));
            // Ape has no weather station, so like /weather it shows the forecast for this hour.
            assertTrue(textByChat.get("2").startsWith("📍 *Ape* (hourly forecast 16.10.2026 23:00)"), textByChat.get("2"));
        }
    }
}
