# LVĢMC Weather Telegram Bot

Telegram bot that fetches weather data from the Latvian Environment, Geology and Meteorology Centre (LVĢMC) and delivers it to your chat.

## Commands

| Command | Description |
|---------|-------------|
| `/city` | Select city from keyboard |
| `/weather` | Current weather observations |
| `/weather Daugavpils` | Current weather for another city |
| `/forecast` | 7-day forecast |
| `/forecast Liepāja` | Forecast for another city |
| `/forecast_h` | Hourly forecast (next 12h) |
| `/forecast_h 24` | Hourly forecast for next 24h |
| `/synoptic` | National synoptic forecast text |
| `/schedule 9:00` | Daily hourly forecast at 9:00 |
| `/schedule 8:30 weather` | Daily current weather at 8:30 |
| `/schedule 18:00 forecast` | Daily 7-day forecast at 18:00 |
| `/schedule` | View your active schedules |
| `/unschedule` | Remove all schedules |
| `/unschedule weather` | Remove specific schedule type |
| `/language` | Switch between Latvian and English |

Default city: **Rīga**. Data source: [videscentrs.lvgmc.lv](https://videscentrs.lvgmc.lv)

## Language

The bot supports **Latvian** (default) and **English**. On first start it auto-detects your Telegram language setting. Use `/language` to switch at any time. The synoptic forecast (`/synoptic`) remains in Latvian as it comes directly from LVĢMC.

## Deployment

### Docker

```bash
docker run -d \
  --name meteo-bot \
  --restart unless-stopped \
  -e TELEGRAM_BOT_TOKEN=your_token \
  -v "$PWD/data:/app/data" \
  ghcr.io/sknarovs/lvgmc-bot:main
```

The database is kept in `./data`. To update, `docker pull ghcr.io/sknarovs/lvgmc-bot:main`, remove the container
with `docker rm -f meteo-bot` and run the command again.

### Local run

Requires JDK 25.

```bash
TELEGRAM_BOT_TOKEN=your_token ./gradlew run
```

### Tests

```bash
./gradlew test
```

Tests run against a local fake of the LVĢMC API serving responses recorded from the real one
(`src/test/resources/lvgmc`), and check that every Markdown message would be accepted by Telegram.

## Configuration

| Variable | Description | Default |
|---|---|---|
| `TELEGRAM_BOT_TOKEN` | Telegram bot token (required) | — |
| `DB_PATH` | SQLite database path | `meteobot.db` in the working directory; `/app/data/meteobot.db` in the Docker image |

## Raspberry Pi

The Docker image supports `linux/arm64`, so the `docker run` command above works on a Pi as is — no `docker build`
needed, the image is pulled from GHCR.

## Architecture

Java 25, Gradle, [TelegramBots](https://github.com/rubenlagus/TelegramBots), SQLite. Sources in `src/main/java/lv/sknarovs/lvgmcbot`:

- `Main.java` — configuration from the environment, startup
- `bot/`
  - `MeteoBot.java` — Telegram command handlers and the `/city` conversation
  - `Scheduler.java` — daily scheduled messages
- `weather/`
  - `WeatherReports.java` — fetches LVĢMC data and composes the weather messages, for commands and schedules
  - `WeatherFormatter.java` — message formatting with emoji
- `lvgmc/`
  - `Lvgmc.java` — LVĢMC API client, city lookup
  - `Observation.java`, `ForecastEntry.java`, `SynopticForecast.java`, `Station.java` — API response types
- `storage/Database.java` — SQLite storage for user cities, language preference, and schedules
- `model/` — `Schedule.java`, `ScheduledDelivery.java`, `ScheduleType.java` — users' schedules and schedule types
- `i18n/`
  - `Language.java` — supported languages
  - `Text.java` — all user-facing messages in Latvian and English
- `util/Markdown.java` — Markdown escaping

Tests use the same packages under `src/test/java`.

## License

MIT
