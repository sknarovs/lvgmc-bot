package lv.sknarovs.lvgmcbot.model;

/** A schedule that is due: whom to send it to, what, and for which city. */
public record ScheduledDelivery(long userId, ScheduleType type, String city) {}
