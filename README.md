# LVĢMC Weather Telegram Bot

Telegram bot that fetches weather data from the Latvian Environment, Geology and Meteorology Centre (LVĢMC) and delivers it to your chat.

## Commands

| Command | Description |
|---|---|
| `/city` | Select city from keyboard |
| `/weather` | Current weather observations |
| `/weather Daugavpils` | Current weather for another city |
| `/forecast` | 7-day forecast |
| `/forecast Liepāja` | Forecast for another city |
| `/forecast_h` | Hourly forecast (next 12h) |
| `/forecast_h 24` | Hourly forecast for next 24h |
| `/synoptic` | National synoptic forecast text |
| `/schedule 9:00` | Daily forecast notification at 9:00 |
| `/schedule 8:30 weather` | Daily current weather at 8:30 |
| `/schedule 18:00 forecast_h` | Daily hourly forecast at 18:00 |
| `/schedule` | View your active schedules |
| `/unschedule` | Remove all schedules |
| `/unschedule weather` | Remove specific schedule type |

Default city: **Rīga**. Data source: [videscentrs.lvgmc.lv](https://videscentrs.lvgmc.lt)

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

```bash
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
TELEGRAM_BOT_TOKEN=your_token python3 bot.py
```

## Configuration

| Variable | Description | Default |
|---|---|---|
| `TELEGRAM_BOT_TOKEN` | Telegram bot token (required) | — |
| `DB_PATH` | SQLite database path | `meteobot.db` (in app dir) |

## Raspberry Pi

The Docker image supports `linux/arm64`. On your Pi:

```bash
docker compose up -d
```

Works out of the box — no `docker build` needed, just pull the image from GHCR.

## Architecture

- `bot.py` — Telegram bot handlers, scheduling
- `weather.py` — LVĢMC API client, formatters with emoji
- `db.py` — SQLite storage for user cities and schedules
- Data persists in Docker volume `/app/data`

## License

MIT