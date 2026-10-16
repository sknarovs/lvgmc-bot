package lv.sknarovs.lvgmcbot;

import java.time.Clock;
import lv.sknarovs.lvgmcbot.bot.MeteoBot;
import lv.sknarovs.lvgmcbot.bot.Scheduler;
import lv.sknarovs.lvgmcbot.lvgmc.Lvgmc;
import lv.sknarovs.lvgmcbot.storage.Database;
import lv.sknarovs.lvgmcbot.weather.WeatherReports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.methods.updates.DeleteWebhook;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.generics.TelegramClient;

/** Entry point: configuration from the environment, then long polling plus the daily scheduler. */
public final class Main {

  private static final Logger log = LoggerFactory.getLogger("meteobot");

  private Main() {}

  static void main(String[] args) {
    try {
      run();
    } catch (Exception e) {
      log.error("Bot stopped", e);
      // OkHttp's idle threads would otherwise keep the JVM alive for another minute.
      System.exit(1);
    }
  }

  private static void run() throws Exception {
    String botToken = System.getenv().getOrDefault("TELEGRAM_BOT_TOKEN", "");
    if (botToken.isEmpty()) {
      log.error("TELEGRAM_BOT_TOKEN environment variable not set");
      log.error("Usage: TELEGRAM_BOT_TOKEN=your_token ./gradlew run");
      return;
    }

    log.info(
        "Bot starting, token={}...", botToken.substring(0, Math.min(8, botToken.length())) + "***");
    String dbPath = System.getenv().getOrDefault("DB_PATH", "meteobot.db");

    try (Database db = new Database(dbPath);
        TelegramBotsLongPollingApplication botsApplication =
            new TelegramBotsLongPollingApplication()) {
      db.init();
      log.info("Database initialized");

      TelegramClient telegram = new OkHttpTelegramClient(botToken);
      User me = telegram.execute(new GetMe());
      // Polling does not work while a webhook is set.
      telegram.execute(new DeleteWebhook());

      Clock clock = Clock.system(Lvgmc.ZONE);
      WeatherReports reports = new WeatherReports(new Lvgmc(), clock);
      MeteoBot bot = new MeteoBot(telegram, me.getUserName(), db, reports);
      botsApplication.registerBot(botToken, bot);
      try (Scheduler scheduler = new Scheduler(db, reports, bot, clock)) {
        scheduler.start();
        log.info("Bot is running, polling for updates...");
        Thread.currentThread().join();
      }
    }
  }
}
