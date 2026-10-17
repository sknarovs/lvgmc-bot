package lv.sknarovs.lvgmcbot.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import lv.sknarovs.lvgmcbot.i18n.Language;
import lv.sknarovs.lvgmcbot.model.Schedule;
import lv.sknarovs.lvgmcbot.model.ScheduleType;
import lv.sknarovs.lvgmcbot.model.ScheduledDelivery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** SQLite storage for user cities, language preference and schedules. Thread-safe. */
public final class Database implements AutoCloseable {

  static final String DEFAULT_CITY = "Rīga";
  private static final Logger log = LoggerFactory.getLogger("meteobot.db");
  private final String path;
  private final Connection conn;

  public Database(String path) throws SQLException {
    this.path = path;
    this.conn = DriverManager.getConnection("jdbc:sqlite:" + path);
    try (Statement st = conn.createStatement()) {
      st.execute("PRAGMA journal_mode=WAL");
      st.execute("PRAGMA foreign_keys=ON");
    }
  }

  public synchronized void init() throws SQLException {
    try (Statement st = conn.createStatement()) {
      st.execute(
          """
                    CREATE TABLE IF NOT EXISTS users (
                        user_id INTEGER PRIMARY KEY,
                        city TEXT NOT NULL DEFAULT 'Rīga',
                        language TEXT NOT NULL DEFAULT 'lv'
                    )""");
      st.execute(
          """
                    CREATE TABLE IF NOT EXISTS schedules (
                        user_id INTEGER NOT NULL,
                        hour INTEGER NOT NULL,
                        minute INTEGER NOT NULL,
                        type TEXT NOT NULL DEFAULT 'forecast',
                        FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                        UNIQUE(user_id, type)
                    )""");
      try {
        // Databases created before language support lack this column.
        st.execute("ALTER TABLE users ADD COLUMN language TEXT NOT NULL DEFAULT 'lv'");
      } catch (SQLException _) {
        // column already exists
      }
    }
    log.info("Database initialized at {}", path);
  }

  public synchronized boolean userExists(long userId) throws SQLException {
    try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM users WHERE user_id = ?")) {
      ps.setLong(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    }
  }

  public synchronized String getUserCity(long userId) throws SQLException {
    try (PreparedStatement ps = conn.prepareStatement("SELECT city FROM users WHERE user_id = ?")) {
      ps.setLong(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getString("city") : DEFAULT_CITY;
      }
    }
  }

  public synchronized void setUserCity(long userId, String city) throws SQLException {
    try (PreparedStatement ps =
        conn.prepareStatement(
            "INSERT INTO users (user_id, city) VALUES (?, ?) ON CONFLICT(user_id) DO UPDATE SET city = excluded.city")) {
      ps.setLong(1, userId);
      ps.setString(2, city);
      ps.executeUpdate();
    }
  }

  public synchronized Language getUserLanguage(long userId) throws SQLException {
    try (PreparedStatement ps =
        conn.prepareStatement("SELECT language FROM users WHERE user_id = ?")) {
      ps.setLong(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? Language.fromCode(rs.getString("language")) : Language.LV;
      }
    }
  }

  public synchronized void setUserLanguage(long userId, Language language) throws SQLException {
    try (PreparedStatement ps =
        conn.prepareStatement(
            "INSERT INTO users (user_id, language) VALUES (?, ?) ON CONFLICT(user_id) DO UPDATE SET language = excluded.language")) {
      ps.setLong(1, userId);
      ps.setString(2, language.code());
      ps.executeUpdate();
    }
  }

  /** Replaces the user's schedule of that type, if any. */
  public synchronized void setSchedule(long userId, LocalTime time, ScheduleType type)
      throws SQLException {
    conn.setAutoCommit(false);
    try (PreparedStatement user =
            conn.prepareStatement(
                "INSERT INTO users (user_id) VALUES (?) ON CONFLICT(user_id) DO NOTHING");
        PreparedStatement schedule =
            conn.prepareStatement(
                "INSERT INTO schedules (user_id, hour, minute, type) VALUES (?, ?, ?, ?) "
                    + "ON CONFLICT(user_id, type) DO UPDATE SET hour = excluded.hour, minute = excluded.minute")) {
      user.setLong(1, userId);
      user.executeUpdate();
      schedule.setLong(1, userId);
      schedule.setInt(2, time.getHour());
      schedule.setInt(3, time.getMinute());
      schedule.setString(4, type.code());
      schedule.executeUpdate();
      conn.commit();
    } catch (SQLException e) {
      conn.rollback();
      throw e;
    } finally {
      conn.setAutoCommit(true);
    }
    log.info("Schedule set: user={}, {} type={}", userId, time, type.code());
  }

  public synchronized void removeSchedule(long userId, ScheduleType type) throws SQLException {
    try (PreparedStatement ps =
        conn.prepareStatement("DELETE FROM schedules WHERE user_id = ? AND type = ?")) {
      ps.setLong(1, userId);
      ps.setString(2, type.code());
      ps.executeUpdate();
    }
    log.info("Schedule removed: user={}, type={}", userId, type.code());
  }

  public synchronized void removeAllSchedules(long userId) throws SQLException {
    try (PreparedStatement ps = conn.prepareStatement("DELETE FROM schedules WHERE user_id = ?")) {
      ps.setLong(1, userId);
      ps.executeUpdate();
    }
    log.info("Schedules removed: user={}", userId);
  }

  public synchronized List<Schedule> getUserSchedules(long userId) throws SQLException {
    List<Schedule> result = new ArrayList<>();
    try (PreparedStatement ps =
        conn.prepareStatement("SELECT hour, minute, type FROM schedules WHERE user_id = ?")) {
      ps.setLong(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          LocalTime time = LocalTime.of(rs.getInt("hour"), rs.getInt("minute"));
          ScheduleType.fromCode(rs.getString("type"))
              .ifPresent(type -> result.add(new Schedule(time, type)));
        }
      }
    }
    return result;
  }

  /** Everything to send at {@code time} (hours and minutes). */
  public synchronized List<ScheduledDelivery> getSchedulesAt(LocalTime time) throws SQLException {
    List<ScheduledDelivery> result = new ArrayList<>();
    try (PreparedStatement ps =
        conn.prepareStatement(
            "SELECT s.user_id, s.type, u.city FROM schedules s "
                + "JOIN users u ON s.user_id = u.user_id WHERE s.hour = ? AND s.minute = ?")) {
      ps.setInt(1, time.getHour());
      ps.setInt(2, time.getMinute());
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          long userId = rs.getLong("user_id");
          String city = rs.getString("city");
          ScheduleType.fromCode(rs.getString("type"))
              .ifPresent(type -> result.add(new ScheduledDelivery(userId, type, city)));
        }
      }
    }
    return result;
  }

  @Override
  public synchronized void close() throws SQLException {
    conn.close();
  }
}
