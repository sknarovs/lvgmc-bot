# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Telegram bot that answers with weather data from the LVĢMC open data API (videscentrs.lvgmc.lv): Java 25, Gradle,
TelegramBots long polling, SQLite. It is a port of an earlier Python version and runs as a Docker image from GHCR,
mainly on a Raspberry Pi. README.md lists the bot commands, the configuration and what each source file does.

## Commands

```bash
./gradlew test                                                   # all tests; needs a local JDK 25
./gradlew test --tests 'lv.sknarovs.lvgmcbot.bot.MeteoBotTest'   # one class
./gradlew test --tests 'lv.sknarovs.lvgmcbot.bot.MeteoBotTest.weatherForTheUsersCity' --rerun   # one method
TELEGRAM_BOT_TOKEN=... ./gradlew run                             # local run, database in ./meteobot.db
docker build -t lvgmc-bot .                                      # the image build runs the tests too
```

Without `--rerun`, Gradle skips a test run whose inputs have not changed and reports it as up to date. Code is
formatted with google-java-format in its default Google style (2-space indent, 100 columns, one sorted import block)
through the IntelliJ plugin; the Gradle build runs no formatter or linter. sqlite-jdbc needs
`--enable-native-access=ALL-UNNAMED`: `build.gradle.kts` passes it to `run` and `test`, and the Dockerfile `CMD`
passes it explicitly.

Dependency versions and the Java version live in the version catalog `gradle/libs.versions.toml`; declare new
dependencies there and use them as `libs.…` in `build.gradle.kts`. Changing the Java version also means changing the
`eclipse-temurin:25` images in the Dockerfile.

## Architecture

`Main` reads `TELEGRAM_BOT_TOKEN` and `DB_PATH`, opens the `Database`, deletes any webhook (long polling fails while
one is set) and creates one `WeatherReports` with a `Clock` in `Lvgmc.ZONE` (Europe/Riga). That instance is shared by
`MeteoBot`, which routes Telegram commands, and `Scheduler`, which checks every 30 s and sends each minute's schedules
once through `MeteoBot.send`, so commands and scheduled messages are built by the same code. `WeatherReports` fetches
through `lvgmc.Lvgmc` and renders through `weather.WeatherFormatter`; every user-facing string comes from `i18n.Text`.

Package dependencies have no cycles; keep it that way. `bot` uses everything; `weather` uses `lvgmc`, `i18n` and
`util`; `storage` uses `model` and `i18n`; `model` uses only `i18n`; `lvgmc`, `i18n` and `util` use no other package.
That is why the LVĢMC response records live in `lvgmc` next to the client (`Observation.time()` uses `Lvgmc.ZONE`)
and `Language` lives in `i18n` (`ScheduleType` → `Text` → `Language`). There is no `enum` package: a type goes where
its responsibility is.

### LVĢMC data

- Values stay the strings LVĢMC sends, `"-"` when missing; parse them leniently like `WeatherFormatter.number`.
- Forecast times are Latvian local time (`LocalDateTime`); observation times are UTC and `Observation.time()`
  converts them. Schedules are Latvian local times, matched against the Riga clock.
- There are two kinds of places. Weather stations give `/weather` its observations: they are matched by name prefix,
  ignoring suffixes like "(LU)", and the station list is cached for an hour. Forecasts exist only for the fixed
  `Lvgmc.FORECAST_CITIES`, which `Lvgmc.findCity` searches (exact match first, then substring). Both compare names
  lowercased and without whitespace or diacritics.
- `/weather` falls back to this hour's forecast when the station observation is missing or older than 2 hours.
- On HTTP, network or JSON errors `Lvgmc` logs and returns an empty list instead of throwing; callers turn that into
  a "no data" text.

### Messages

- Replies use Telegram's legacy Markdown, and Telegram rejects the whole message if a `_`, `*`, `` ` `` or `[` is
  left unbalanced. Escape any text from users or LVĢMC with `util.Markdown.escape`; literal special characters in
  `Text` templates need a backslash (written `/forecast\\_h` in the Java source).
- Every `Text` has a Latvian and an English template with the same format specifiers. `TextTest` checks that, and
  that every text renders valid Markdown; `MeteoBotTest` checks every Markdown message the bot sends with
  `MarkdownAssert`.
- The synoptic forecast is only published in Latvian, so `/synoptic` shows Latvian text in both languages.

### Storage and state

- `Database` holds one SQLite connection and all its methods are `synchronized`. `init()` creates the schema
  idempotently and migrates databases from the Python version (`DatabaseTest.readsDatabasesFromThePythonVersion`);
  keep that compatibility.
- `Language.code()` and `ScheduleType.code()` (lowercase constant names) are stored in the database, typed by users
  (`/schedule 9:00 forecast_h`) and used in callback data (`lang_en`). Renaming the enum constants breaks existing
  data and commands.
- Unknown users get Rīga and Latvian; on a user's first `/start` the language is taken from their Telegram app.
- Who is picking a city after `/city` is kept in memory per chat and user, so it is lost on restart.
- Logger names are plain strings (`meteobot`, `meteobot.weather`, `meteobot.db`), not class names.

### Tests

- Tests never call the real API or Telegram. `lvgmc.FakeLvgmc` is a local HTTP server serving responses recorded on
  16 October 2026 (`src/test/resources/lvgmc`). It has forecasts for Rīga only; other paths get 404, which the client
  turns into empty results, unless a test adds them with `respond(...)`. `bot.RecordingTelegram` records what the bot
  executes, and `MeteoBotTest` builds `Update`s from JSON.
- Clocks are fixed relative to the newest recorded observation, 2026-10-16T20:00Z (23:00 in Riga); many expected
  strings depend on that.
- Each test is in the package of the class it tests, so it can use package-private members such as the
  `Lvgmc(String baseUrl)` constructor. Make members public only when another package needs them.

## Deployment

- `.github/workflows/docker.yml` builds linux/amd64 and linux/arm64 images and pushes them to
  `ghcr.io/sknarovs/lvgmc-bot` on pushes to `main` (tags `main` and `sha-*`) and on `v*` git tags (semver tags).
  Pull requests are built but not pushed. There is no `latest` tag.
- The bot is deployed with the single `docker run` command in README.md, with `./data` mounted at `/app/data`; the
  image sets `DB_PATH=/app/data/meteobot.db`. There is deliberately no Compose file or start script.
