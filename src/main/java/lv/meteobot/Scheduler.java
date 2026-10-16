package lv.meteobot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Sends the daily scheduled messages. */
final class Scheduler implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger("meteobot");
    private static final DateTimeFormatter TICK = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Database db;
    private final WeatherReports reports;
    private final MeteoBot bot;
    private final Clock clock;
    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().name("scheduler").daemon().factory());

    /** Only touched by the scheduler thread. */
    private LocalDateTime lastMinute;

    Scheduler(Database db, WeatherReports reports, MeteoBot bot, Clock clock) {
        this.db = db;
        this.reports = reports;
        this.bot = bot;
        this.clock = clock;
    }

    /** Checks twice a minute and sends each minute's schedules once. */
    void start() {
        executor.scheduleWithFixedDelay(this::tick, 30, 30, TimeUnit.SECONDS);
    }

    private void tick() {
        try {
            LocalDateTime minute = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
            if (!minute.equals(lastMinute)) {
                lastMinute = minute;
                log.info("Scheduler tick: {}", minute.format(TICK));
                sendScheduledMessages(minute.toLocalTime());
            }
        } catch (Exception e) {
            // An exception escaping here would cancel all further ticks.
            log.error("Scheduler tick failed", e);
        }
    }

    void sendScheduledMessages(LocalTime time) throws SQLException {
        for (Database.ScheduledDelivery delivery : db.getSchedulesAt(time)) {
            try {
                Language lang = db.getUserLanguage(delivery.userId());
                log.info("Sending scheduled {} to user {} (city: {})", delivery.type().code(), delivery.userId(), delivery.city());
                String text = switch (delivery.type()) {
                    case WEATHER -> reports.currentWeather(delivery.city(), lang);
                    case FORECAST -> reports.dailyForecast(delivery.city(), lang);
                    case FORECAST_H -> reports.hourlyForecast(delivery.city(), MeteoBot.DEFAULT_FORECAST_HOURS, lang);
                };
                bot.send(delivery.userId(), text);
            } catch (Exception e) {
                log.error("Error sending scheduled message to user {}: {}", delivery.userId(), e.toString());
            }
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
