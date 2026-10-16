package lv.sknarovs.lvgmcbot.i18n;

import java.util.Locale;

/**
 * Every message the bot sends, in Latvian and English. Placeholders are {@link java.util.Formatter}
 * specifiers. Most texts are sent as legacy Markdown, so literal {@code _ * ` [} must be escaped
 * with a backslash.
 */
public enum Text {

  // ── /start ──
  START(
      """
            🌤️ *LVĢMC Laika apstākļu bots*

            Komandas:
            /city — izvēlēties pilsētu
            /weather — pašreizējie laika apstākļi
            /forecast — 7 dienu prognoze
            /forecast\\_h — stundu prognoze (12h)
            /synoptic — sinoptiskā prognoze
            /schedule HH:MM — iestatīt ikdienas prognozi
            /unschedule — noņemt ikdienas prognozi
            /help — palīdzība
            /language — valoda

            Pēc noklusējuma: Rīga""",
      """
            🌤️ *LVĢMC Weather Bot*

            Commands:
            /city — select a city
            /weather — current weather
            /forecast — 7-day forecast
            /forecast\\_h — hourly forecast (12h)
            /synoptic — synoptic forecast
            /schedule HH:MM — set daily schedule
            /unschedule — remove daily schedule
            /help — help
            /language — language

            Default: Rīga"""),

  // ── /city ──
  CITY_PROMPT(
      "📍 Izvēlies pilsētu (vai uzraksti jebkuru pilsētas nosaukumu):",
      "📍 Choose a city (or type any city name):"),
  CITY_CANCEL_BUTTON("❌ Atcelt", "❌ Cancel"),
  CITY_CANCELLED("Pilsētas izvēle atcelta.", "City selection cancelled."),
  CITY_SET("✅ Pilsēta iestatīta: *%s*", "✅ City set: *%s*"),
  CITY_NOT_FOUND("❌ Pilsēta \"%s\" nav atrasta.", "❌ City \"%s\" not found."),
  CITY_NOT_FOUND_HINT(
      "❌ Pilsēta nav atrasta: %s\n\nIzmanto /city lai izvēlētos.",
      "❌ City not found: %s\n\nUse /city to select one."),

  // ── /weather, /forecast, /forecast_h, /synoptic ──
  WEATHER_NO_DATA("❌ Nav datu par: %s", "❌ No data for: %s"),
  WEATHER_FALLBACK_NOTE(
      "_ℹ️ Novērojumu dati nav aktuāli, rāda prognozi_",
      "_ℹ️ Observation data is outdated, showing forecast_"),
  FORECAST_NO_DATA("❌ Nav prognozes datu par: %s", "❌ No forecast data for: %s"),
  FORECAST_HOURLY_NO_DATA(
      "❌ Nav stundu prognozes datu par: %s", "❌ No hourly forecast data for: %s"),
  SYNOPTIC_NO_DATA("❌ Nav pieejama sinoptiskā prognoze.", "❌ Synoptic forecast not available."),

  // ── /schedule, /unschedule ──
  SCHEDULE_LIST_TITLE("📅 Tavi grafiki:", "📅 Your schedules:"),
  SCHEDULE_LIST_ITEM("  ⏰ %s — %s (%s)", "  ⏰ %s — %s (%s)"),
  SCHEDULE_SET_HINT("\nLai iestatītu: /schedule HH:MM", "\nTo set: /schedule HH:MM"),
  SCHEDULE_NONE("❌ Nav iestatīts grafiks.", "❌ No schedule set."),
  SCHEDULE_USAGE(
      "Lieto: `/schedule 9:00` vai `/schedule 9:00 weather`",
      "Usage: `/schedule 9:00` or `/schedule 9:00 weather`"),
  SCHEDULE_INVALID_FORMAT(
      "❌ Nepareizs formāts. Lieto: `/schedule 9:00`", "❌ Invalid format. Usage: `/schedule 9:00`"),
  SCHEDULE_INVALID_TIME(
      "❌ Nepareizs laiks. Stundas 0-23, minūtes 0-59.",
      "❌ Invalid time. Hours 0-23, minutes 0-59."),
  SCHEDULE_UNKNOWN_TYPE(
      "❌ Nezināms grafika veids: %s\n\nIespējamie veidi:\n%s",
      "❌ Unknown schedule type: %s\n\nValid types:\n%s"),
  SCHEDULE_SET("✅ Iestatīts!", "✅ Schedule set!"),
  SCHEDULE_DETAIL("⏰ %s — %s\n📍 Pilsēta: %s", "⏰ %s — %s\n📍 City: %s"),
  SCHEDULE_REMOVE_HINT("\n\nLai noņemtu: /unschedule", "\n\nTo remove: /unschedule"),
  UNSCHEDULE_REMOVED_TYPE("✅ Noņemts %s grafiks.", "✅ Removed %s schedule."),
  UNSCHEDULE_REMOVED_ALL("✅ Visi grafiki noņemti.", "✅ All schedules removed."),

  TYPE_FORECAST("dienas prognoze", "daily forecast"),
  TYPE_WEATHER("pašreizējie laika apstākļi", "current weather"),
  TYPE_FORECAST_H("stundu prognoze", "hourly forecast"),
  TYPE_FORECAST_EMOJI("📊 Prognoze", "📊 Forecast"),
  TYPE_WEATHER_EMOJI("🌤️ Laika apstākļi", "🌤️ Current weather"),
  TYPE_FORECAST_H_EMOJI("🕐 Stundu prognoze", "🕐 Hourly forecast"),

  // ── /help ──
  HELP(
      """
            🌤️ *LVĢMC Laika apstākļu bots*

            📍 *Pilsēta:*
            /city — izvēlēties pilsētu no saraksta
            Pašreizējā: *%s*

            🌧️ *Laika apstākļi:*
            /weather — pašreizējie novērojumi
            /weather Daugavpils — cita pilsēta

            📊 *Prognoze:*
            /forecast — 7 dienu prognoze
            /forecast Liepāja — cita pilsēta

            🕐 *Stundu prognoze:*
            /forecast\\_h — nākamās 12 stundas
            /forecast\\_h 24 — nākamās 24 stundas
            /forecast\\_h Ventspils — cita pilsēta

            📑 *Sinoptiskā prognoze:*
            /synoptic — LVĢMC teksta prognoze

            🌐 *Valoda:*
            /language — mainīt valodu

            ⏰ *Ikdienas grafiki:*
            /schedule 9:00 — katru dienu 9:00 stundu prognoze
            /schedule 8:30 weather — katru dienu 8:30 laika apstākļi
            /schedule 18:00 forecast — katru dienu 18:00 7 dienu prognoze
            /schedule — parādīt pašreizējos grafikus
            /unschedule — noņemt visus grafikus
            /unschedule weather — noņemt konkrētu grafiku

            Tavi grafiki:
            %s""",
      """
            🌤️ *LVĢMC Weather Bot*

            📍 *City:*
            /city — select a city from the list
            Current: *%s*

            🌧️ *Current weather:*
            /weather — current observations
            /weather Daugavpils — other city

            📊 *Forecast:*
            /forecast — 7-day forecast
            /forecast Liepāja — other city

            🕐 *Hourly forecast:*
            /forecast\\_h — next 12 hours
            /forecast\\_h 24 — next 24 hours
            /forecast\\_h Ventspils — other city

            📑 *Synoptic forecast:*
            /synoptic — LVĢMC text forecast

            🌐 *Language:*
            /language — change language

            ⏰ *Daily schedules:*
            /schedule 9:00 — daily hourly forecast at 9:00
            /schedule 8:30 weather — daily weather at 8:30
            /schedule 18:00 forecast — daily 7-day forecast at 18:00
            /schedule — show current schedules
            /unschedule — remove all schedules
            /unschedule weather — remove specific schedule

            Your schedules:
            %s"""),
  HELP_NO_SCHEDULES("  Nav iestatītu grafiku.", "  No schedules set."),

  // ── /language ──
  LANGUAGE_PROMPT("🌐 Izvēlies valodu / Choose language:", "🌐 Izvēlies valodu / Choose language:"),
  LANGUAGE_SET("✅ Valoda iestatīta: 🇱🇻 Latviešu", "✅ Language set: 🇬🇧 English"),

  // ── Current weather (station observations) ──
  CURRENT_UNKNOWN_STATION("Nezināma", "Unknown"),
  CURRENT_TITLE("📍 *%s* — %s", "📍 *%s* — %s"),
  CURRENT_TEMP_LINE(
      "%s Temperatūra: *%s°C* (jūtam: %s°C)", "%s Temperature: *%s°C* (feels like: %s°C)"),
  CURRENT_HUMIDITY_LINE("💧 Mitrums: %s%%", "💧 Humidity: %s%%"),
  CURRENT_WIND_LINE("💨 Vējš: %s %s m/s (brāzmas: %s m/s)", "💨 Wind: %s %s m/s (gusts: %s m/s)"),
  CURRENT_PRESSURE_LINE("🧭 Spiediens: %s hPa", "🧭 Pressure: %s hPa"),
  CURRENT_PRECIPITATION_LINE("🌧️ Nokrišņi: %s mm", "🌧️ Precipitation: %s mm"),
  CURRENT_CLOUDS_LINE("☁️ Mākoņu daudzums: %s/8", "☁️ Cloud cover: %s/8"),
  CURRENT_VISIBILITY_LINE("👁️ Redzamība: %.1f km", "👁️ Visibility: %.1f km"),
  CURRENT_CONDITION_LINE("\n📋 Stāvoklis: %s", "\n📋 Condition: %s"),

  // ── Current weather from the hourly forecast, when observations are missing or outdated ──
  FORECAST_AS_CURRENT_TITLE("📍 *%s* (stundu prognoze %s)", "📍 *%s* (hourly forecast %s)"),
  FORECAST_AS_CURRENT_WIND_LINE("💨 Vējš: %s %s m/s", "💨 Wind: %s %s m/s"),
  FORECAST_AS_CURRENT_PRECIPITATION_LINE("🌧️ Nokrišņi: %s mm/h", "🌧️ Precipitation: %s mm/h"),

  // ── Forecasts ──
  FORECAST_TITLE("📅 *Prognoze: %s*", "📅 *Forecast: %s*"),
  FORECAST_HOURLY_TITLE("🕐 *Stundu prognoze: %s*", "🕐 *Hourly forecast: %s*"),
  SYNOPTIC_TITLE("📑 *Sinoptiskā prognoze*", "📑 *Synoptic forecast*");

  private final String lv;
  private final String en;

  Text(String lv, String en) {
    this.lv = lv;
    this.en = en;
  }

  public String get(Language lang, Object... args) {
    return String.format(Locale.ROOT, template(lang), args);
  }

  String template(Language lang) {
    return lang == Language.EN ? en : lv;
  }
}
