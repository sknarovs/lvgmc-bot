import logging
import sqlite3
import os

DB_PATH = os.environ.get("DB_PATH", os.path.join(os.path.dirname(os.path.abspath(__file__)), "meteobot.db"))

_conn = None
logger = logging.getLogger("meteobot.db")


def get_db():
    global _conn
    if _conn is None:
        _conn = sqlite3.connect(DB_PATH)
        _conn.row_factory = sqlite3.Row
        _conn.execute("PRAGMA journal_mode=WAL")
        _conn.execute("PRAGMA foreign_keys=ON")
    return _conn


def init_db():
    db = get_db()
    db.executescript("""
        CREATE TABLE IF NOT EXISTS users (
            user_id INTEGER PRIMARY KEY,
            city TEXT NOT NULL DEFAULT 'Rīga',
            language TEXT NOT NULL DEFAULT 'lv'
        );

        CREATE TABLE IF NOT EXISTS schedules (
            user_id INTEGER NOT NULL,
            hour INTEGER NOT NULL,
            minute INTEGER NOT NULL,
            type TEXT NOT NULL DEFAULT 'forecast',
            FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
            UNIQUE(user_id, type)
        );
    """)
    try:
        db.execute("ALTER TABLE users ADD COLUMN language TEXT NOT NULL DEFAULT 'lv'")
    except sqlite3.OperationalError:
        pass
    db.commit()
    logger.info("Database initialized at %s", DB_PATH)


def get_user_city(user_id):
    db = get_db()
    row = db.execute("SELECT city FROM users WHERE user_id = ?", (user_id,)).fetchone()
    if row:
        return row["city"]
    return "Rīga"


def set_user_city(user_id, city):
    db = get_db()
    db.execute(
        "INSERT INTO users (user_id, city) VALUES (?, ?) ON CONFLICT(user_id) DO UPDATE SET city = ?",
        (user_id, city, city),
    )
    db.commit()


def user_exists(user_id):
    db = get_db()
    return db.execute("SELECT 1 FROM users WHERE user_id = ?", (user_id,)).fetchone() is not None


def get_user_language(user_id):
    db = get_db()
    row = db.execute("SELECT language FROM users WHERE user_id = ?", (user_id,)).fetchone()
    if row:
        return row["language"]
    return "lv"


def set_user_language(user_id, language):
    db = get_db()
    db.execute(
        "INSERT INTO users (user_id, language) VALUES (?, ?) ON CONFLICT(user_id) DO UPDATE SET language = ?",
        (user_id, language, language),
    )
    db.commit()


def set_schedule(user_id, hour, minute, schedule_type="forecast"):
    db = get_db()
    db.execute(
        "INSERT INTO users (user_id, city) VALUES (?, 'Rīga') ON CONFLICT(user_id) DO NOTHING",
        (user_id,),
    )
    db.execute(
        "INSERT INTO schedules (user_id, hour, minute, type) VALUES (?, ?, ?, ?) "
        "ON CONFLICT(user_id, type) DO UPDATE SET hour = ?, minute = ?",
        (user_id, hour, minute, schedule_type, hour, minute),
    )
    db.commit()
    logger.info("Schedule set: user=%s, %02d:%02d type=%s", user_id, hour, minute, schedule_type)


def remove_schedule(user_id, schedule_type="forecast"):
    db = get_db()
    db.execute(
        "DELETE FROM schedules WHERE user_id = ? AND type = ?",
        (user_id, schedule_type),
    )
    db.commit()
    logger.info("Schedule removed: user=%s, type=%s", user_id, schedule_type)


def get_all_schedules():
    db = get_db()
    rows = db.execute(
        "SELECT s.user_id, s.hour, s.minute, s.type, u.city "
        "FROM schedules s JOIN users u ON s.user_id = u.user_id"
    ).fetchall()
    return [dict(r) for r in rows]


def get_user_schedule(user_id):
    db = get_db()
    rows = db.execute(
        "SELECT hour, minute, type FROM schedules WHERE user_id = ?", (user_id,)
    ).fetchall()
    return [dict(r) for r in rows]