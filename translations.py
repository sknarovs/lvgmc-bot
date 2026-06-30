TRANSLATIONS = {
    "lv": {
        # ── /start ──
        "start": (
            "🌤️ *LVĢMC Laika apstākļu bots*\n\n"
            "Komandas:\n"
            "/city — izvēlēties pilsētu\n"
            "/weather — pašreizējie laika apstākļi\n"
            "/forecast — 7 dienu prognoze\n"
            "/forecast_h — stundu prognoze (12h)\n"
            "/synoptic — sinoptiskā prognoze\n"
            "/schedule HH:MM — iestatīt ikdienas prognozi\n"
            "/unschedule — noņemt ikdienas prognozi\n"
            "/help — palīdzība\n"
            "/language — valoda\n\n"
            "Pēc noklusējuma: Rīga"
        ),

        # ── /city ──
        "city_prompt": "📍 Izvēlies pilsētu (vai uzraksti jebkuru pilsētas nosaukumu):",
        "city_cancel_button": "❌ Atcelt",
        "city_cancelled": "Pilsētas izvēle atcelta.",
        "city_set": "✅ Pilsēta iestatīta: *{city}*",
        "city_not_found": "❌ Pilsēta \"{city}\" nav atrasta.",

        # ── /weather ──
        "weather_no_data": "❌ Nav datu par: {city}",
        "weather_city_not_found": "❌ Pilsēta nav atrasta: {city}\n\nIzmanto /city lai izvēlētos.",
        "weather_fallback_note": "ℹ️ Novērojumu dati nav aktuāli, rāda prognozi",

        # ── /forecast ──
        "forecast_no_data": "❌ Nav prognozes datu par: {city}",
        "forecast_city_not_found": "❌ Pilsēta nav atrasta: {city}\n\nIzmanto /city lai izvēlētos.",
        "forecast_hourly_no_data": "❌ Nav stundu prognozes datu par: {city}",

        # ── /synoptic ──
        "synoptic_no_data": "❌ Nav pieejama sinoptiskā prognoze.",

        # ── /schedule ──
        "schedule_list_title": "📅 Tavi grafiki:",
        "schedule_list_item": "  ⏰ {hour:02d}:{minute:02d} — {type} ({city})",
        "schedule_set_hint": "\nLai iestatītu: /schedule HH:MM",
        "schedule_none": "❌ Nav iestatīts grafiks.",
        "schedule_usage": "Lieto: `/schedule 9:00` vai `/schedule 9:00 weather`",
        "schedule_invalid_format": "❌ Nepareizs formāts. Lieto: `/schedule 9:00`",
        "schedule_invalid_time": "❌ Nepatīkams laiks. Stundas 0-23, minūtes 0-59.",
        "schedule_set": "✅ Iestatīts!",
        "schedule_detail": "⏰ {hour:02d}:{minute:02d} — {type}\n📍 Pilsēta: {city}",
        "schedule_remove_hint": "\n\nLai noņemtu: /unschedule",

        # ── /unschedule ──
        "unschedule_removed_type": "✅ Noņemts {type} grafiks.",
        "unschedule_removed_all": "✅ Visi grafiki noņemti.",

        # ── /help ──
        "help_text": (
            "🌤️ *LVĢMC Laika apstākļu bots*\n\n"
            "📍 *Pilsēta:*\n"
            "/city — izvēlēties pilsētu no saraksta\n"
            "Pašreizējā: *{city}*\n\n"
            "🌧️ *Laika apstākļi:*\n"
            "/weather — pašreizējie novērojumi\n"
            "/weather Daugavpils — cita pilsēta\n\n"
            "📊 *Prognoze:*\n"
            "/forecast — 7 dienu prognoze\n"
            "/forecast Liepāja — cita pilsēta\n\n"
            "🕐 *Stundu prognoze:*\n"
            "/forecast\\_h — nākamās 12 stundas\n"
            "/forecast\\_h 24 — nākamās 24 stundas\n"
            "/forecast\\_h Ventspils — cita pilsēta\n\n"
            "📑 *Sinoptiskā prognoze:*\n"
            "/synoptic — LVĢMC teksta prognoze\n\n"
            "🌐 *Valoda:*\n"
            "/language — mainīt valodu\n\n"
            "⏰ *Ikdienas grafiki:*\n"
            "/schedule 9:00 — katru dienu 9:00 prognoze\n"
            "/schedule 8:30 weather — katru dienu 8:30 laika apstākļi\n"
            "/schedule 18:00 forecast\\_h — katru dienu 18:00 stundu prognoze\n"
            "/schedule — parādīt pašreizējos grafikus\n"
            "/unschedule — noņemt visus grafikus\n"
            "/unschedule weather — noņemt konkrētu grafiku\n\n"
            "Tavi grafiki:\n{schedules}"
        ),

        # ── /language ──
        "language_prompt": "🌐 Izvēlies valodu / Choose language:",
        "language_set_lv": "✅ Valoda iestatīta: 🇱🇻 Latviešu",
        "language_set_en": "✅ Language set: 🇬🇧 English",

        # ── schedule type names (for display) ──
        "type_forecast": "dienas prognoze",
        "type_weather": "pašreizējie laika apstākļi",
        "type_forecast_h": "stundu prognoze",
        "type_forecast_emoji": "📊 Prognoze",
        "type_weather_emoji": "🌤️ Laika apstākļi",
        "type_forecast_h_emoji": "🕐 Stundu prognoze",

        # ── weather.py: format_current_weather ──
        "current_no_data": "Nav datu.",
        "current_unknown": "Nezināma",
        "current_title": "📍 *{name}* — {time}",
        "current_temp": "Temperatūra",
        "current_feels": "jūtam",
        "current_humidity": "Mitrums",
        "current_wind": "Vējš",
        "current_precip": "Nokrišņi",
        "current_pressure": "Spiediens",
        "current_temp_line": "{emoji} Temperatūra: *{temp}°C* (jūtam: {feels}°C)",
        "current_humidity_line": "💧 Mitrums: {value}%",
        "current_wind_line": "💨 Vējš: {dir_emoji} {speed} m/s (brāzmas: {gust} m/s)",
        "current_pressure_line": "🧭 Spiediens: {value} hPa",
        "current_precip_line": "🌧️ Nokrišņi: {value}",
        "current_clouds_line": "☁️ Mākoņu daudzums: {value}/8",
        "current_visibility_line": "👁️ Redzamība: {value} km",
        "current_condition_line": "\n📋 Stāvoklis: {value}",

        # ── weather.py: format_daily_forecast ──
        "forecast_title": "📅 *Prognoze: {city}*",
        "forecast_no_data": "Nav prognozes datu.",
        "days": ["Pir", "Sek", "Tre", "Cet", "Pie", "Ses", "Svē"],

        # ── weather.py: format_hourly_forecast ──
        "forecast_hourly_title": "🕐 *Stundu prognoze: {city}*",
        "forecast_hourly_no_data": "Nav stundu prognozes datu.",

        # ── weather.py: format_synoptic_forecast ──
        "synoptic_title": "📑 *Sinoptiskā prognoze*",
        "synoptic_no_data_text": "Nav sinoptiskās prognozes.",

        # ── weather.py: wind directions ──
        "wind_N": "Z",
        "wind_NE": "ZA",
        "wind_E": "A",
        "wind_SE": "DA",
        "wind_S": "D",
        "wind_SW": "DR",
        "wind_W": "R",
        "wind_NW": "ZR",
    },

    "en": {
        # ── /start ──
        "start": (
            "🌤️ *LVĢMC Weather Bot*\n\n"
            "Commands:\n"
            "/city — select a city\n"
            "/weather — current weather\n"
            "/forecast — 7-day forecast\n"
            "/forecast_h — hourly forecast (12h)\n"
            "/synoptic — synoptic forecast\n"
            "/schedule HH:MM — set daily schedule\n"
            "/unschedule — remove daily schedule\n"
            "/help — help\n"
            "/language — language\n\n"
            "Default: Rīga"
        ),

        # ── /city ──
        "city_prompt": "📍 Choose a city (or type any city name):",
        "city_cancel_button": "❌ Cancel",
        "city_cancelled": "City selection cancelled.",
        "city_set": "✅ City set: *{city}*",
        "city_not_found": "❌ City \"{city}\" not found.",

        # ── /weather ──
        "weather_no_data": "❌ No data for: {city}",
        "weather_city_not_found": "❌ City not found: {city}\n\nUse /city to select one.",
        "weather_fallback_note": "ℹ️ Observation data is outdated, showing forecast",

        # ── /forecast ──
        "forecast_no_data": "❌ No forecast data for: {city}",
        "forecast_city_not_found": "❌ City not found: {city}\n\nUse /city to select one.",
        "forecast_hourly_no_data": "❌ No hourly forecast data for: {city}",

        # ── /synoptic ──
        "synoptic_no_data": "❌ Synoptic forecast not available.",

        # ── /schedule ──
        "schedule_list_title": "📅 Your schedules:",
        "schedule_list_item": "  ⏰ {hour:02d}:{minute:02d} — {type} ({city})",
        "schedule_set_hint": "\n\nTo set: /schedule HH:MM",
        "schedule_none": "❌ No schedule set.",
        "schedule_usage": "Usage: `/schedule 9:00` or `/schedule 9:00 weather`",
        "schedule_invalid_format": "❌ Invalid format. Usage: `/schedule 9:00`",
        "schedule_invalid_time": "❌ Invalid time. Hours 0-23, minutes 0-59.",
        "schedule_set": "✅ Schedule set!",
        "schedule_detail": "⏰ {hour:02d}:{minute:02d} — {type}\n📍 City: {city}",
        "schedule_remove_hint": "\n\nTo remove: /unschedule",

        # ── /unschedule ──
        "unschedule_removed_type": "✅ Removed {type} schedule.",
        "unschedule_removed_all": "✅ All schedules removed.",

        # ── /help ──
        "help_text": (
            "🌤️ *LVĢMC Weather Bot*\n\n"
            "📍 *City:*\n"
            "/city — select a city from the list\n"
            "Current: *{city}*\n\n"
            "🌧️ *Current weather:*\n"
            "/weather — current observations\n"
            "/weather Daugavpils — other city\n\n"
            "📊 *Forecast:*\n"
            "/forecast — 7-day forecast\n"
            "/forecast Liepāja — other city\n\n"
            "🕐 *Hourly forecast:*\n"
            "/forecast\\_h — next 12 hours\n"
            "/forecast\\_h 24 — next 24 hours\n"
            "/forecast\\_h Ventspils — other city\n\n"
            "📑 *Synoptic forecast:*\n"
            "/synoptic — LVĢMC text forecast\n\n"
            "🌐 *Language:*\n"
            "/language — change language\n\n"
            "⏰ *Daily schedules:*\n"
            "/schedule 9:00 — daily forecast at 9:00\n"
            "/schedule 8:30 weather — daily weather at 8:30\n"
            "/schedule 18:00 forecast\\_h — daily hourly forecast at 18:00\n"
            "/schedule — show current schedules\n"
            "/unschedule — remove all schedules\n"
            "/unschedule weather — remove specific schedule\n\n"
            "Your schedules:\n{schedules}"
        ),

        # ── /language ──
        "language_prompt": "🌐 Izvēlies valodu / Choose language:",
        "language_set_lv": "✅ Valoda iestatīta: 🇱🇻 Latviešu",
        "language_set_en": "✅ Language set: 🇬🇧 English",

        # ── schedule type names ──
        "type_forecast": "daily forecast",
        "type_weather": "current weather",
        "type_forecast_h": "hourly forecast",
        "type_forecast_emoji": "📊 Forecast",
        "type_weather_emoji": "🌤️ Current weather",
        "type_forecast_h_emoji": "🕐 Hourly forecast",

        # ── weather.py: format_current_weather ──
        "current_no_data": "No data.",
        "current_unknown": "Unknown",
        "current_title": "📍 *{name}* — {time}",
        "current_temp": "Temperature",
        "current_feels": "feels like",
        "current_humidity": "Humidity",
        "current_wind": "Wind",
        "current_precip": "Precipitation",
        "current_pressure": "Pressure",
        "current_temp_line": "{emoji} Temperature: *{temp}°C* (feels like: {feels}°C)",
        "current_humidity_line": "💧 Humidity: {value}%",
        "current_wind_line": "💨 Wind: {dir_emoji} {speed} m/s (gusts: {gust} m/s)",
        "current_pressure_line": "🧭 Pressure: {value} hPa",
        "current_precip_line": "🌧️ Precipitation: {value}",
        "current_clouds_line": "☁️ Cloud cover: {value}/8",
        "current_visibility_line": "👁️ Visibility: {value} km",
        "current_condition_line": "\n📋 Condition: {value}",

        # ── weather.py: format_daily_forecast ──
        "forecast_title": "📅 *Forecast: {city}*",
        "forecast_no_data": "No forecast data.",
        "days": ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"],

        # ── weather.py: format_hourly_forecast ──
        "forecast_hourly_title": "🕐 *Hourly forecast: {city}*",
        "forecast_hourly_no_data": "No hourly forecast data.",

        # ── weather.py: format_synoptic_forecast ──
        "synoptic_title": "📑 *Synoptic forecast*",
        "synoptic_no_data_text": "No synoptic forecast available.",

        # ── weather.py: wind directions ──
        "wind_N": "N",
        "wind_NE": "NE",
        "wind_E": "E",
        "wind_SE": "SE",
        "wind_S": "S",
        "wind_SW": "SW",
        "wind_W": "W",
        "wind_NW": "NW",
    },
}

WIND_DIRECTIONS_LV = {
    0: "⬆️Z", 22: "⬆️Z", 45: "↗️ZA", 67: "↗️ZA",
    90: "➡️A", 112: "➡️A", 135: "↘️DA", 157: "↘️DA",
    180: "⬇️D", 202: "⬇️D", 225: "↙️DR", 247: "↙️DR",
    270: "⬅️R", 292: "⬅️R", 315: "↖️ZR", 337: "↖️ZR",
}

WIND_DIRECTIONS_EN = {
    0: "⬆️N", 22: "⬆️N", 45: "↗️NE", 67: "↗️NE",
    90: "➡️E", 112: "➡️E", 135: "↘️SE", 157: "↘️SE",
    180: "⬇️S", 202: "⬇️S", 225: "↙️SW", 247: "↙️SW",
    270: "⬅️W", 292: "⬅️W", 315: "↖️NW", 337: "↖️NW",
}


def t(key: str, lang: str = "lv", **kwargs) -> str:
    text = TRANSLATIONS.get(lang, {}).get(key, TRANSLATIONS["lv"].get(key, key))
    if kwargs:
        try:
            text = text.format(**kwargs)
        except (KeyError, ValueError):
            pass
    return text
