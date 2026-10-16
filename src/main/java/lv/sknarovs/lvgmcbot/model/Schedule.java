package lv.sknarovs.lvgmcbot.model;

import java.time.LocalTime;

/** One of a user's daily schedules: when, and what it sends. */
public record Schedule(LocalTime time, ScheduleType type) {}
