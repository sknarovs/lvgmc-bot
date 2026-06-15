import json
import logging
import os
import urllib.request
import urllib.parse
import ssl
from datetime import datetime, timezone, timedelta

logger = logging.getLogger("meteobot.weather")

EET = timezone(timedelta(hours=2))
EEST = timezone(timedelta(hours=3))

def _lv_now():
    now_utc = datetime.now(timezone.utc)
    july = datetime(now_utc.year, 7, 1, tzinfo=timezone.utc)
    jan = datetime(now_utc.year, 1, 1, tzinfo=timezone.utc)
    dst_offset = timedelta(hours=1) if now_utc.dst() is not None and now_utc.dst() != timedelta(0) else timedelta(hours=1) if now_utc > july or now_utc < jan else timedelta(0)
    try:
        import zoneinfo
        tz = zoneinfo.ZoneInfo("Europe/Riga")
        return datetime.now(tz)
    except Exception:
        return now_utc.astimezone(EEST)

def _lv_tz():
    try:
        import zoneinfo
        return zoneinfo.ZoneInfo("Europe/Riga")
    except Exception:
        now_utc = datetime.now(timezone.utc)
        return EEST if now_utc.month >= 4 and now_utc.month <= 10 else EET

BASE_URL = "https://videscentrs.lvgmc.lv/data"

CITIES_CACHE = None
CITIES_CACHE_TIME = None
MONITORING_POINTS_CACHE = None
MONITORING_POINTS_CACHE_TIME = None
CACHE_TTL = 3600

ICON_TO_EMOJI = {
    "1101": "☀️",
    "1102": "🌤️",
    "1103": "⛅",
    "1104": "🌥️",
    "1105": "☁️",
    "1106": "🌫️",
    "1201": "🌦️",
    "1202": "🌧️",
    "1203": "❄️",
    "1204": "🌨️",
    "1205": "🌦️",
    "1206": "🌧️",
    "1207": "❄️",
    "1208": "🌨️",
    "1301": "⛈️",
    "1302": "⛈️",
    "1303": "⛈️",
    "1304": "⛈️",
    "1305": "🌧️",
    "1306": "🌧️",
    "1307": "🌨️",
    "1308": "🌨️",
    "1309": "🌫️",
    "1310": "🌫️",
    "1311": "🌧️",
    "1312": "🌧️",
    "1313": "🌧️",
    "1504": "🌦️",
    "1506": "⛈️",
    "2101": "🌙",
    "2102": "🌙",
    "2103": "☁️",
    "2104": "🌥️",
    "2105": "☁️",
    "2201": "🌧️",
    "2202": "🌧️",
    "2203": "❄️",
    "2204": "🌨️",
    "2205": "🌧️",
    "2206": "🌧️",
    "2301": "⛈️",
    "2302": "⛈️",
    "2504": "🌧️",
    "2506": "⛈️",
}

DESCRIPTION_TO_EMOJI = {
    "SKAIDRS": "☀️",
    "MAKONAINS": "☁️",
    "LIETUS": "🌧️",
    "SLIKTS LAIKS": "⛈️",
    "SNEGGS": "❄️",
    "SNEIGS": "❄️",
    "MIGLA": "🌫️",
    "PERKONA NEGAISS": "⛈️",
    "DUSKJ LIETUS": "🌧️",
    "DUSEKLI": "🌧️",
}

WIND_DIRECTIONS = {
    0: "⬆️Z", 22: "⬆️Z", 45: "↗️ZA", 67: "↗️ZA",
    90: "➡️A", 112: "➡️A", 135: "↘️DA", 157: "↘️DA",
    180: "⬇️D", 202: "⬇️D", 225: "↙️DR", 247: "↙️DR",
    270: "⬅️R", 292: "⬅️R", 315: "↖️ZR", 337: "↖️ZR",
}


def wind_direction_emoji(degrees):
    if degrees is None:
        return "🌀"
    deg = float(degrees)
    keys = sorted(WIND_DIRECTIONS.keys())
    closest = min(keys, key=lambda k: abs(k - deg))
    return WIND_DIRECTIONS[closest]


def icon_to_emoji(icon_code):
    code = str(icon_code)
    if code in ICON_TO_EMOJI:
        return ICON_TO_EMOJI[code]
    first = code[0] if code else ""
    if first == "1":
        return "🌤️"
    if first == "2":
        return "🌙"
    return "🌡️"


def _fetch_json(url, params=None):
    try:
        if params:
            separator = "&" if "?" in url else "?"
            url = url + separator + urllib.parse.urlencode(params)
        ctx = ssl.create_default_context()
        ctx.check_hostname = False
        ctx.verify_mode = ssl.CERT_NONE
        req = urllib.request.Request(url, headers={"User-Agent": "MeteoBot/1.0"})
        with urllib.request.urlopen(req, timeout=15, context=ctx) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except Exception as e:
        logger.warning("Error fetching %s: %s", url, e)
        return None


def get_forecast_cities():
    global CITIES_CACHE, CITIES_CACHE_TIME
    now = datetime.now().timestamp()
    if CITIES_CACHE and CITIES_CACHE_TIME and (now - CITIES_CACHE_TIME) < CACHE_TTL:
        return CITIES_CACHE

    data = _fetch_json(f"{BASE_URL}/weather_forecast_for_location_daily", params={"nosaukums": "Rīga"})
    if not data:
        data = _fetch_json(f"{BASE_URL}/weather_forecast_for_location_daily?nosaukums=R%C4%ABga")

    base_cities = [
        "Rīga", "Daugavpils", "Liepāja", "Ventspils", "Jelgava",
        "Alūksne", "Dobele", "Rēzekne", "Bauska", "Kolka",
        "Saldus", "Gulbene", "Madona", "Sigulda", "Zosēni",
        "Priekuļi", "Stende", "Mērsrags", "Rucava", "Dagda",
        "Pāvilosta", "Skulte", "Zīlāni",
        "Rūjiena", "Skrīveri", "Ainaži",
    ]

    CITIES_CACHE = base_cities
    CITIES_CACHE_TIME = now
    return CITIES_CACHE


def get_monitoring_points():
    global MONITORING_POINTS_CACHE, MONITORING_POINTS_CACHE_TIME
    now = datetime.now().timestamp()
    if MONITORING_POINTS_CACHE and MONITORING_POINTS_CACHE_TIME and (now - MONITORING_POINTS_CACHE_TIME) < CACHE_TTL:
        return MONITORING_POINTS_CACHE

    data = _fetch_json(f"{BASE_URL}/weather_monitoring_points")
    if data:
        MONITORING_POINTS_CACHE = data
        MONITORING_POINTS_CACHE_TIME = now
    return data or []


def get_current_weather(city_name):
    points = get_monitoring_points()
    if not points:
        return None

    match = None
    for p in points:
        pnos = p.get("nosaukums", "").lower().replace(" ", "")
        cname = city_name.lower().replace(" ", "")
        pnos_stripped = pnos.replace("(lu)", "").replace("(piekraste)", "")
        if pnos_stripped.startswith(cname) or cname.startswith(pnos_stripped):
            match = p
            break

    if not match:
        match = next((p for p in points if city_name.lower() in p.get("nosaukums", "").lower()), None)

    if not match:
        return None

    station_code = match["kods"]
    data = _fetch_json(f"{BASE_URL}/weather_monitoring_data")
    if not data:
        return None

    station_data = None
    for d in data:
        if d.get("station_code") == station_code:
            station_data = d
            break

    if not station_data:
        for d in data:
            if city_name.lower() in d.get("name", "").lower():
                station_data = d
                break

    if not station_data:
        return None

    return station_data


def get_forecast(city_name, hourly=False):
    endpoint = "weather_forecast_for_location_hourly" if hourly else "weather_forecast_for_location_daily"
    data = _fetch_json(f"{BASE_URL}/{endpoint}", params={"nosaukums": city_name})
    if not data:
        from urllib.parse import quote
        data = _fetch_json(f"{BASE_URL}/{endpoint}?nosaukums={quote(city_name)}")
    return data


def get_synoptic_forecast():
    data = _fetch_json(f"{BASE_URL}/sinopt_prognozes")
    return data


def format_current_weather(data):
    if not data:
        return "Nav datu."

    temp = data.get("air_temperature_actual", "-")
    feels = data.get("air_temperature_feel", "-")
    humidity = data.get("humidity_actual", "-")
    wind_speed = data.get("wind_speed_actual", "-")
    wind_gust = data.get("wind_speed_gust", "-")
    wind_dir = data.get("wind_direction_actual", "-")
    wind_dir_emoji = wind_direction_emoji(float(wind_dir) if wind_dir and wind_dir != "-" else None)
    pressure = data.get("pressure_sea_level", "-")
    precipitation = data.get("precipitation", "-")
    description = data.get("description") or ""
    clouds = data.get("clouds", "-")
    visibility = data.get("visibility_actual", "-")
    name = data.get("name", "Nezināma")
    time_str = data.get("time", "-")

    desc_emoji = ""
    if description:
        for key, emoji in DESCRIPTION_TO_EMOJI.items():
            if key.upper() in description.upper():
                desc_emoji = emoji
                break
    
    if not desc_emoji:
        if clouds and clouds not in ("-", None):
            c = int(clouds)
            if c <= 2:
                desc_emoji = "☀️" if 6 <= int(time_str.split(" ")[0].split(".")[0] if " " in time_str else "12") <= 21 else "🌙"
            elif c <= 5:
                desc_emoji = "⛅"
            else:
                desc_emoji = "☁️"
        else:
            desc_emoji = "🌡️"

    precip_str = "—"
    if precipitation and precipitation != "-" and precipitation != "0" and precipitation != "0.0":
        precip_str = f"{precipitation} mm"
    lines = [
        f"📍 *{name}* — {time_str}",
        f"",
        f"{desc_emoji} Temperatūra: *{temp}°C* (jūtam: {feels}°C)",
        f"💧 Mitrums: {humidity}%",
        f"💨 Vējš: {wind_dir_emoji} {wind_speed} m/s (brāzmas: {wind_gust} m/s)",
        f"🧭 Spiediens: {pressure} hPa",
    ]
    if precip_str != "—":
        lines.append(f"🌧️ Nokrišņi: {precip_str}")

    if clouds and clouds != "-":
        lines.append(f"☁️ Mākoņu daudzums: {clouds}/8")
    if visibility and visibility != "-":
        vis_km = float(visibility) / 1000 if float(visibility) > 1000 else visibility
        lines.append(f"👁️ Redzamība: {vis_km} km")

    if description:
        lines.append(f"\n📋 Stāvoklis: {description}")

    return "\n".join(lines)


def format_daily_forecast(data, days=7):
    if not data:
        return "Nav prognozes datu."

    city = data[0].get("nosaukums", "") if data else ""
    lines = [f"📅 *Prognoze: {city}*\n"]

    from collections import OrderedDict
    day_entries = OrderedDict()
    for entry in data:
        time_str = entry.get("laiks", "")
        try:
            dt = datetime.strptime(time_str, "%Y%m%d%H%M")
        except (ValueError, TypeError):
            continue
        date_key = dt.strftime("%Y-%m-%d")
        if date_key not in day_entries:
            day_entries[date_key] = []
        day_entries[date_key].append((dt, entry))

    count = 0
    for date_key, entries in day_entries.items():
        if count >= days:
            break

        dt = entries[0][0]
        day_of_week = ["Pir", "Sek", "Tre", "Cet", "Pie", "Ses", "Svē"][dt.weekday()]
        date_str = dt.strftime(f"%d.%m ({day_of_week})")

        temps = []
        precip_total = 0.0
        icons = []
        has_precip = False

        for _, entry in entries:
            temp = entry.get("temperatura", "-")
            if temp and temp != "-":
                try:
                    temps.append(float(temp))
                except ValueError:
                    pass
            precip = entry.get("nokrisni_12h", "-")
            if precip and precip != "-" and precip != "0" and precip != "0.0":
                try:
                    precip_total += float(precip)
                    has_precip = True
                except ValueError:
                    pass
            icon = entry.get("laika_apstaklu_ikona", "")
            if icon:
                icons.append(str(icon))

        day_icon = icons[0] if icons else ""
        emoji = icon_to_emoji(day_icon)

        if len(temps) >= 2:
            temp_str = f"*{min(temps):.0f}°C*...*{max(temps):.0f}°C*"
        elif len(temps) == 1:
            temp_str = f"*{temps[0]:.0f}°C*"
        else:
            temp_str = "*—°C*"

        precip_str = f" 🌧️{precip_total:.1f}mm" if has_precip else ""
        lines.append(f"{emoji} {date_str}: {temp_str}{precip_str}")
        count += 1

    return "\n".join(lines)


def format_hourly_forecast(data, hours=12):
    if not data:
        return "Nava stundu prognozes datu."

    city = data[0].get("nosaukums", "") if data else ""
    lines = [f"🕐 *Stundu prognoze: {city}*\n"]

    now = _lv_now().replace(tzinfo=None)
    cutoff = now.replace(minute=0, second=0, microsecond=0) + timedelta(hours=1)
    count = 0
    for entry in data:
        time_str = entry.get("laiks", "")
        try:
            dt = datetime.strptime(time_str, "%Y%m%d%H%M")
        except (ValueError, TypeError):
            continue

        if dt < cutoff:
            continue
        if count >= hours:
            break

        feels = entry.get("sajutu_temperatura", "-")
        precip = entry.get("nokrisni_1h", "-")
        icon = entry.get("laika_apstaklu_ikona", "")

        emoji = icon_to_emoji(icon)

        time_str = dt.strftime("%H:00")

        precip_str = ""
        if precip and precip != "-" and float(precip) > 0:
            precip_str = f" {round(float(precip), 1)}mm"

        lines.append(f"{emoji} {time_str} *{feels}°C*{precip_str}")
        count += 1

    return "\n".join(lines)


def format_synoptic_forecast(data):
    if not data:
        return "Nav sinoptiskās prognozes."

    lines = ["📑 *Sinoptiskā prognoze*\n"]
    for entry in data:
        kods = entry.get("kods", "")
        datums = entry.get("datums", "")
        laiks_no = entry.get("laiks_no", "")
        laiks_lidz = entry.get("laiks_lidz", "")
        teksts = entry.get("teksti", {}).get("teksts", "") if isinstance(entry.get("teksti"), dict) else ""

        lines.append(f"*{kods}* ({datums} {laiks_no}—{laiks_lidz})")
        lines.append(f"{teksts}\n")

    return "\n".join(lines)


def find_matching_city(query, available_cities):
    q = query.lower().strip().replace(" ", "")
    for city in available_cities:
        if city.lower().replace(" ", "") == q:
            return city
    for city in available_cities:
        if q in city.lower().replace(" ", ""):
            return city
    for city in available_cities:
        if city.lower().replace(" ", "").startswith(q):
            return city
    return None


if __name__ == "__main__":
    w = get_current_weather("Rīga")
    if w:
        print(format_current_weather(w))
    else:
        print("No data")

    f = get_forecast("Rīga")
    if f:
        print("\n" + format_daily_forecast(f))

    sf = get_synoptic_forecast()
    if sf:
        print("\n" + format_synoptic_forecast(sf))