import asyncio
import logging
import os
import sqlite3
from datetime import datetime, timezone, timedelta

from telegram import Update, ReplyKeyboardMarkup, KeyboardButton, InlineKeyboardButton, InlineKeyboardMarkup
from telegram.ext import (
    Application,
    CommandHandler,
    ContextTypes,
    ConversationHandler,
    MessageHandler,
    CallbackQueryHandler,
    filters,
)

from db import init_db, get_user_city, set_user_city, set_schedule, remove_schedule, get_all_schedules, get_user_schedule, get_user_language, set_user_language, user_exists
from translations import t
from weather import (
    get_current_weather,
    get_forecast,
    get_synoptic_forecast,
    format_current_weather,
    format_daily_forecast,
    format_hourly_forecast,
    format_synoptic_forecast,
    get_forecast_cities,
    get_popular_cities,
    find_matching_city,
    icon_to_emoji,
    wind_direction_emoji,
    _lv_now,
)

BOT_TOKEN = os.environ.get("TELEGRAM_BOT_TOKEN", "")
TIMEZONE_OFFSET = timedelta(hours=3)

WAITING_FOR_CITY = 1

logger = logging.getLogger("meteobot")
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)


async def start(update: Update, context: ContextTypes.DEFAULT_TYPE):
    logger.info("/start from user %s (%s)", update.effective_user.id, update.effective_user.full_name)
    user_id = update.effective_user.id
    if not user_exists(user_id):
        user_lang = (update.effective_user.language_code or "").split("_")[0]
        lang = "en" if user_lang == "en" else "lv"
        set_user_language(user_id, lang)
    else:
        lang = get_user_language(user_id)
    await update.message.reply_text(t("start", lang), parse_mode="Markdown")


async def city_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    logger.info("/city from user %s", update.effective_user.id)
    user_id = update.effective_user.id
    lang = get_user_language(user_id)

    if context.args:
        city = " ".join(context.args)
        all_cities = get_forecast_cities()
        match = find_matching_city(city, all_cities)
        if match:
            set_user_city(user_id, match)
            await update.message.reply_text(
                t("city_set", lang, city=match),
                parse_mode="Markdown",
                reply_markup=ReplyKeyboardMarkup([[]], resize_keyboard=True),
            )
        else:
            await update.message.reply_text(
                t("city_not_found", lang, city=city),
                reply_markup=ReplyKeyboardMarkup([[]], resize_keyboard=True),
            )
        return ConversationHandler.END

    cities = get_popular_cities()
    keyboard = []
    row = []
    for i, city in enumerate(cities):
        row.append(KeyboardButton(city))
        if (i + 1) % 2 == 0:
            keyboard.append(row)
            row = []
    if row:
        keyboard.append(row)
    keyboard.append([KeyboardButton(t("city_cancel_button", lang))])

    reply_markup = ReplyKeyboardMarkup(keyboard, one_time_keyboard=True, resize_keyboard=True)
    await update.message.reply_text(
        t("city_prompt", lang),
        reply_markup=reply_markup,
    )
    return WAITING_FOR_CITY


async def city_selected(update: Update, context: ContextTypes.DEFAULT_TYPE):
    selected = update.message.text
    user_id = update.effective_user.id
    logger.info("City selection: '%s' from user %s", selected, user_id)
    lang = get_user_language(user_id)

    cancel_text = t("city_cancel_button", lang)
    if selected == cancel_text:
        await update.message.reply_text(t("city_cancelled", lang), reply_markup=ReplyKeyboardMarkup([[]], resize_keyboard=True))
        return ConversationHandler.END

    cities = get_forecast_cities()
    match = find_matching_city(selected, cities)
    if not match:
        await update.message.reply_text(
            t("city_not_found", lang, city=selected), reply_markup=ReplyKeyboardMarkup([[]], resize_keyboard=True)
        )
        return ConversationHandler.END

    set_user_city(user_id, match)
    logger.info("User %s set city to '%s'", user_id, match)

    await update.message.reply_text(
        t("city_set", lang, city=match),
        parse_mode="Markdown",
        reply_markup=ReplyKeyboardMarkup([[]], resize_keyboard=True),
    )
    return ConversationHandler.END


async def weather_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    user_id = update.effective_user.id
    lang = get_user_language(user_id)
    city = get_user_city(user_id)
    if context.args:
        city = " ".join(context.args)
    logger.info("/weather from user %s for city '%s'", user_id, city)

    use_forecast = False
    data = get_current_weather(city)
    if data:
        time_str = data.get("time", "")
        try:
            obs_dt = datetime.strptime(time_str, "%Y.%m.%d %H:%M:%S")
            age_hours = (_lv_now().replace(tzinfo=None) - obs_dt).total_seconds() / 3600
            if age_hours > 2:
                logger.info("Observation data is %.1fh old, using forecast instead", age_hours)
                use_forecast = True
        except (ValueError, TypeError):
            pass

    cities = get_forecast_cities()
    match = find_matching_city(city, cities)

    if data and not use_forecast:
        msg = format_current_weather(data, lang=lang)
    elif match:
        forecast_data = get_forecast(match, hourly=True)
        if forecast_data and len(forecast_data) > 0:
            now = _lv_now().replace(tzinfo=None)
            entry = None
            for e in forecast_data:
                try:
                    ed = datetime.strptime(e.get("laiks", ""), "%Y%m%d%H%M")
                    if ed >= now:
                        entry = e
                        break
                except (ValueError, TypeError):
                    continue
            if not entry:
                entry = forecast_data[0]

            icon = entry.get("laika_apstaklu_ikona", "")
            emoji = icon_to_emoji(icon)
            temp = entry.get("temperatura", "-")
            feels = entry.get("sajutu_temperatura", "-")
            hum = entry.get("relativais_mitrums", "-")
            wind = entry.get("veja_atrums", "-")
            wind_dir = entry.get("veja_virziens", "-")
            wind_emoji = wind_direction_emoji(float(wind_dir) if wind_dir and wind_dir != "-" else None, lang=lang)
            precip = entry.get("nokrisni_1h", "-")
            pressure = entry.get("spiediens", "-")
            time_str = entry.get("laiks", "")
            try:
                dt = datetime.strptime(time_str, "%Y%m%d%H%M")
                time_str = dt.strftime("%d.%m.%Y %H:%M")
            except Exception:
                pass

            msg = f"📍 *{match}* ({t('type_forecast_h', lang)} {time_str})\n\n"
            msg += f"{emoji} {t('current_temp', lang)}: *{temp}°C* ({t('current_feels', lang)}: {feels}°C)\n"
            msg += f"💧 {t('current_humidity', lang)}: {hum}%\n"
            msg += f"💨 {t('current_wind', lang)}: {wind_emoji} {wind} m/s\n"
            if precip and precip != "-" and float(precip) > 0:
                msg += f"🌧️ {t('current_precip', lang)}: {precip} mm/h\n"
            msg += f"🧭 {t('current_pressure', lang)}: {pressure} hPa\n"
            msg += f"\n_{t('weather_fallback_note', lang)}_"
        else:
            msg = t("weather_no_data", lang, city=city)
    else:
        msg = t("weather_city_not_found", lang, city=city)

    await update.message.reply_text(msg, parse_mode="Markdown")


async def forecast_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    user_id = update.effective_user.id
    lang = get_user_language(user_id)
    city = get_user_city(user_id)
    if context.args:
        city = " ".join(context.args)
    logger.info("/forecast from user %s for city '%s'", user_id, city)

    cities = get_forecast_cities()
    match = find_matching_city(city, cities)
    if not match:
        await update.message.reply_text(t("forecast_city_not_found", lang, city=city), parse_mode="Markdown")
        return

    data = get_forecast(match)
    if data:
        msg = format_daily_forecast(data, lang=lang)
        if len(msg) > 4096:
            for i in range(0, len(msg), 4096):
                await update.message.reply_text(msg[i:i + 4096], parse_mode="Markdown")
            return
    else:
        msg = t("forecast_no_data", lang, city=match)

    await update.message.reply_text(msg, parse_mode="Markdown")


async def forecast_hourly_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    user_id = update.effective_user.id
    lang = get_user_language(user_id)
    city = get_user_city(user_id)
    hours = 12
    if context.args:
        try:
            hours = int(context.args[-1])
            hours = min(max(hours, 1), 48)
            city_args = context.args[:-1]
            if city_args:
                city = " ".join(city_args)
        except ValueError:
            city = " ".join(context.args)
    logger.info("/forecast_h from user %s for city '%s'", user_id, city)

    cities = get_forecast_cities()
    match = find_matching_city(city, cities)
    if not match:
        await update.message.reply_text(t("forecast_city_not_found", lang, city=city), parse_mode="Markdown")
        return

    data = get_forecast(match, hourly=True)
    if data:
        msg = format_hourly_forecast(data, hours=hours, lang=lang)
        if len(msg) > 4096:
            for i in range(0, len(msg), 4096):
                await update.message.reply_text(msg[i:i + 4096], parse_mode="Markdown")
            return
    else:
        msg = t("forecast_hourly_no_data", lang, city=match)

    await update.message.reply_text(msg, parse_mode="Markdown")


async def synoptic_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    user_id = update.effective_user.id
    lang = get_user_language(user_id)
    logger.info("/synoptic from user %s", user_id)
    data = get_synoptic_forecast()
    if data:
        msg = format_synoptic_forecast(data, lang=lang)
    else:
        msg = t("synoptic_no_data", lang)

    if len(msg) > 4096:
        for i in range(0, len(msg), 4096):
            await update.message.reply_text(msg[i:i + 4096], parse_mode="Markdown")
    else:
        await update.message.reply_text(msg, parse_mode="Markdown")


async def schedule_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    user_id = update.effective_user.id
    lang = get_user_language(user_id)
    city = get_user_city(user_id)

    type_names = {"forecast": t("type_forecast", lang), "weather": t("type_weather", lang), "forecast_h": t("type_forecast_h", lang)}

    if not context.args:
        schedules = get_user_schedule(user_id)
        if schedules:
            lines = [t("schedule_list_title", lang) + "\n"]
            for s in schedules:
                lines.append(t("schedule_list_item", lang, hour=s["hour"], minute=s["minute"], type=type_names.get(s["type"], s["type"]), city=city))
            lines.append(t("schedule_set_hint", lang))
            await update.message.reply_text("\n".join(lines), parse_mode="Markdown")
        else:
            await update.message.reply_text(
                t("schedule_none", lang) + "\n\n" + t("schedule_usage", lang),
                parse_mode="Markdown",
            )
        return

    time_arg = context.args[0]
    schedule_type = "forecast_h"
    if len(context.args) > 1 and context.args[1].lower() in ("weather", "forecast", "forecast_h"):
        schedule_type = context.args[1].lower()

    try:
        parts = time_arg.split(":")
        hour = int(parts[0])
        minute = int(parts[1]) if len(parts) > 1 else 0
    except (ValueError, IndexError):
        await update.message.reply_text(t("schedule_invalid_format", lang), parse_mode="Markdown")
        return

    if not (0 <= hour <= 23 and 0 <= minute <= 59):
        await update.message.reply_text(t("schedule_invalid_time", lang), parse_mode="Markdown")
        return

    set_schedule(user_id, hour, minute, schedule_type)
    logger.info("Schedule set: user %s, %02d:%02d %s (%s)", user_id, hour, minute, schedule_type, city)
    await update.message.reply_text(
        t("schedule_set", lang) + "\n"
        + t("schedule_detail", lang, hour=hour, minute=minute, type=type_names.get(schedule_type, schedule_type), city=city)
        + t("schedule_remove_hint", lang),
        parse_mode="Markdown",
    )


async def unschedule_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    user_id = update.effective_user.id
    lang = get_user_language(user_id)
    logger.info("/unschedule from user %s (type=%s)", user_id, context.args[0] if context.args else "all")
    schedule_type = None
    if context.args and context.args[0].lower() in ("weather", "forecast", "forecast_h"):
        schedule_type = context.args[0].lower()

    if schedule_type:
        remove_schedule(user_id, schedule_type)
        await update.message.reply_text(t("unschedule_removed_type", lang, type=schedule_type), parse_mode="Markdown")
    else:
        remove_schedule(user_id, "forecast")
        remove_schedule(user_id, "weather")
        remove_schedule(user_id, "forecast_h")
        await update.message.reply_text(t("unschedule_removed_all", lang), parse_mode="Markdown")


async def help_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    user_id = update.effective_user.id
    lang = get_user_language(user_id)
    logger.info("/help from user %s", user_id)
    city = get_user_city(user_id)
    schedules = get_user_schedule(user_id)

    sched_lines = ""
    if schedules:
        type_names = {"forecast": t("type_forecast_emoji", lang), "weather": t("type_weather_emoji", lang), "forecast_h": t("type_forecast_h_emoji", lang)}
        for s in schedules:
            sched_lines += f"  ⏰ {s['hour']:02d}:{s['minute']:02d} — {type_names.get(s['type'], s['type'])}\n"
    else:
        sched_lines = t("help_schedule_none", lang) + "\n"

    await update.message.reply_text(
        t("help_text", lang, city=city, schedules=sched_lines),
        parse_mode="Markdown",
    )


async def language_command(update: Update, context: ContextTypes.DEFAULT_TYPE):
    user_id = update.effective_user.id
    lang = get_user_language(user_id)
    logger.info("/language from user %s (current: %s)", user_id, lang)

    keyboard = [
        [InlineKeyboardButton("🇱🇻 Latviešu" + (" ✅" if lang == "lv" else ""), callback_data="lang_lv")],
        [InlineKeyboardButton("🇬🇧 English" + (" ✅" if lang == "en" else ""), callback_data="lang_en")],
    ]
    await update.message.reply_text(
        t("language_prompt", lang),
        reply_markup=InlineKeyboardMarkup(keyboard),
    )


async def language_callback(update: Update, context: ContextTypes.DEFAULT_TYPE):
    query = update.callback_query
    await query.answer()
    user_id = update.effective_user.id
    lang = query.data.split("_")[1]
    set_user_language(user_id, lang)
    logger.info("User %s set language to '%s'", user_id, lang)
    key = "language_set_lv" if lang == "lv" else "language_set_en"
    await query.edit_message_text(t(key, lang))


async def send_scheduled_messages(application):
    now = _lv_now()
    current_hour = now.hour
    current_minute = now.minute
    current_date = now.strftime("%Y-%m-%d")

    schedules = get_all_schedules()

    for sched in schedules:
        if sched["hour"] != current_hour or sched["minute"] != current_minute:
            continue

        city = sched["city"]
        schedule_type = sched["type"]
        user_id = sched["user_id"]
        lang = get_user_language(user_id)

        try:
            logger.info("Sending scheduled %s to user %s (city: %s)", schedule_type, user_id, city)
            if schedule_type == "weather":
                data = get_current_weather(city)
                if data:
                    msg = format_current_weather(data, lang=lang)
                else:
                    msg = t("weather_no_data", lang, city=city)
            elif schedule_type == "forecast_h":
                forecast_data = get_forecast(city, hourly=True)
                if forecast_data:
                    msg = format_hourly_forecast(forecast_data, hours=12, lang=lang)
                else:
                    msg = t("forecast_hourly_no_data", lang, city=city)
            else:
                forecast_data = get_forecast(city)
                if forecast_data:
                    msg = format_daily_forecast(forecast_data, lang=lang)
                else:
                    msg = t("forecast_no_data", lang, city=city)

            for i in range(0, max(len(msg), 1), 4096):
                chunk = msg[i:i + 4096]
                if chunk:
                    await application.bot.send_message(
                        chat_id=user_id,
                        text=chunk,
                        parse_mode="Markdown",
                    )
        except Exception as e:
            logger.error("Error sending scheduled message to user %s: %s", user_id, e)

    last_sent_key = f"{current_date}_{current_hour:02d}_{current_minute:02d}"
    return last_sent_key


async def scheduler_loop(application):
    last_sent = ""
    while True:
        await asyncio.sleep(30)
        now = _lv_now()
        check_key = f"{now.strftime('%Y-%m-%d')}_{now.hour:02d}_{now.minute:02d}"
        if check_key != last_sent and now.second < 30:
            last_sent = check_key
            logger.info("Scheduler tick: %s", now.strftime("%Y-%m-%d %H:%M"))
            await send_scheduled_messages(application)


def main():
    if not BOT_TOKEN:
        logger.error("TELEGRAM_BOT_TOKEN environment variable not set")
        logger.error("Usage: TELEGRAM_BOT_TOKEN=your_token python3 bot.py")
        return

    logger.info("Bot starting, token=%s...", BOT_TOKEN[:8] + "***" if BOT_TOKEN else "NOT SET")
    init_db()
    logger.info("Database initialized")

    app = Application.builder().token(BOT_TOKEN).build()

    conv_handler = ConversationHandler(
        entry_points=[CommandHandler("city", city_command)],
        states={
            WAITING_FOR_CITY: [
                MessageHandler(filters.TEXT & ~filters.COMMAND, city_selected),
            ],
        },
        fallbacks=[CommandHandler("city", city_command)],
    )

    app.add_handler(conv_handler)
    app.add_handler(CommandHandler("start", start))
    app.add_handler(CommandHandler("help", help_command))
    app.add_handler(CommandHandler("weather", weather_command))
    app.add_handler(CommandHandler("weater", weather_command))
    app.add_handler(CommandHandler("forecast", forecast_command))
    app.add_handler(CommandHandler("forecast_h", forecast_hourly_command))
    app.add_handler(CommandHandler("synoptic", synoptic_command))
    app.add_handler(CommandHandler("schedule", schedule_command))
    app.add_handler(CommandHandler("unschedule", unschedule_command))
    app.add_handler(CommandHandler("language", language_command))
    app.add_handler(CallbackQueryHandler(language_callback, pattern="^lang_"))

    async def post_init(application):
        asyncio.create_task(scheduler_loop(application))

    app.post_init = post_init

    logger.info("Bot is running, polling for updates...")
    app.run_polling(allowed_updates=Update.ALL_TYPES)


if __name__ == "__main__":
    try:
        asyncio.get_event_loop()
    except RuntimeError:
        asyncio.set_event_loop(asyncio.new_event_loop())
    main()