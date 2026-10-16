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

### Docker Compose (recommended)

```bash
cp .env.example .env
# Edit .env and set your TELEGRAM_BOT_TOKEN
docker compose up -d
```

### Docker run

```bash
docker run -d \
  --name meteo-bot \
  --restart unless-stopped \
  -e TELEGRAM_BOT_TOKEN=your_token \
  -v meteobot-data:/app/data \
  ghcr.io/sknarovs/lvgmc-bot
```

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
| `DB_PATH` | SQLite database path | `meteobot.db` (in working directory) |

## Raspberry Pi

The Docker image supports `linux/arm64`. On your Pi:

```bash
docker compose up -d
```

Works out of the box — no `docker build` needed, just pull the image from GHCR.

## Architecture

Java 25, Gradle, [TelegramBots](https://github.com/rubenlagus/TelegramBots), SQLite. Sources in `src/main/java/lv/meteobot`:

- `Main.java` — configuration from the environment, startup
- `MeteoBot.java` — Telegram command handlers and the `/city` conversation
- `Scheduler.java` — daily scheduled messages
- `WeatherReports.java` — fetches LVĢMC data and composes the weather messages, for commands and schedules
- `Lvgmc.java` — LVĢMC API client and response types, city lookup
- `WeatherFormatter.java` — message formatting with emoji
- `Text.java` — all user-facing messages in Latvian and English
- `Language.java`, `ScheduleType.java`, `Markdown.java` — supported languages, schedule types, Markdown escaping
- `Database.java` — SQLite storage for user cities, language preference, and schedules
- Data persists in Docker volume `/app/data`

## License

MIT
