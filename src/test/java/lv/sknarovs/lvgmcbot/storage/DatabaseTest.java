package lv.sknarovs.lvgmcbot.storage;

import lv.sknarovs.lvgmcbot.i18n.Language;
import lv.sknarovs.lvgmcbot.model.Schedule;
import lv.sknarovs.lvgmcbot.model.ScheduleType;
import lv.sknarovs.lvgmcbot.model.ScheduledDelivery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseTest {

    @TempDir
    Path dir;

    @Test
    void defaultsForUnknownUser() throws Exception {
        try (Database db = open()) {
            assertFalse(db.userExists(1));
            assertEquals("Rīga", db.getUserCity(1));
            assertEquals(Language.LV, db.getUserLanguage(1));
        }
    }

    @Test
    void storesUsersAndSchedules() throws Exception {
        try (Database db = open()) {
            db.setUserLanguage(1, Language.EN);
            db.setUserCity(1, "Ogre");
            db.setSchedule(1, LocalTime.of(7, 30), ScheduleType.FORECAST);
            db.setSchedule(1, LocalTime.of(8, 15), ScheduleType.FORECAST);
            db.setSchedule(1, LocalTime.of(8, 15), ScheduleType.WEATHER);
            db.setSchedule(2, LocalTime.of(8, 15), ScheduleType.FORECAST_H);

            assertTrue(db.userExists(1));
            assertEquals(Language.EN, db.getUserLanguage(1));
            assertEquals("Ogre", db.getUserCity(1));
            assertEquals(List.of(
                    new Schedule(LocalTime.of(8, 15), ScheduleType.FORECAST),
                    new Schedule(LocalTime.of(8, 15), ScheduleType.WEATHER)), db.getUserSchedules(1));
            assertEquals(List.of(
                    new ScheduledDelivery(1, ScheduleType.FORECAST, "Ogre"),
                    new ScheduledDelivery(1, ScheduleType.WEATHER, "Ogre"),
                    new ScheduledDelivery(2, ScheduleType.FORECAST_H, "Rīga")), db.getSchedulesAt(LocalTime.of(8, 15)));
            assertEquals(List.of(), db.getSchedulesAt(LocalTime.of(7, 30)));

            db.removeSchedule(1, ScheduleType.FORECAST);
            assertEquals(List.of(new Schedule(LocalTime.of(8, 15), ScheduleType.WEATHER)), db.getUserSchedules(1));
            db.removeAllSchedules(1);
            assertEquals(List.of(), db.getUserSchedules(1));
            assertEquals(1, db.getUserSchedules(2).size());
        }
    }

    @Test
    void readsDatabasesFromThePythonVersion() throws Exception {
        String path = dir.resolve("old.db").toString();
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + path);
             Statement st = conn.createStatement()) {
            // Schema from before language support was added.
            st.execute("CREATE TABLE users (user_id INTEGER PRIMARY KEY, city TEXT NOT NULL DEFAULT 'Rīga')");
            st.execute("CREATE TABLE schedules (user_id INTEGER NOT NULL, hour INTEGER NOT NULL, minute INTEGER NOT NULL, "
                    + "type TEXT NOT NULL DEFAULT 'forecast', UNIQUE(user_id, type))");
            st.execute("INSERT INTO users (user_id, city) VALUES (5, 'Talsi')");
            st.execute("INSERT INTO schedules (user_id, hour, minute, type) VALUES (5, 6, 45, 'forecast_h')");
        }
        try (Database db = new Database(path)) {
            db.init();
            db.init();
            assertEquals("Talsi", db.getUserCity(5));
            assertEquals(Language.LV, db.getUserLanguage(5));
            assertEquals(List.of(new Schedule(LocalTime.of(6, 45), ScheduleType.FORECAST_H)), db.getUserSchedules(5));
        }
    }

    private Database open() throws Exception {
        Database db = new Database(dir.resolve("meteobot.db").toString());
        db.init();
        return db;
    }
}
