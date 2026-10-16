package lv.sknarovs.lvgmcbot.model;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import lv.sknarovs.lvgmcbot.i18n.Language;
import lv.sknarovs.lvgmcbot.i18n.Text;

/** What a daily schedule sends. */
public enum ScheduleType {
  WEATHER(Text.TYPE_WEATHER, Text.TYPE_WEATHER_EMOJI),
  FORECAST(Text.TYPE_FORECAST, Text.TYPE_FORECAST_EMOJI),
  FORECAST_H(Text.TYPE_FORECAST_H, Text.TYPE_FORECAST_H_EMOJI);

  private final Text description;
  private final Text helpLabel;

  ScheduleType(Text description, Text helpLabel) {
    this.description = description;
    this.helpLabel = helpLabel;
  }

  /** Case-insensitive. */
  public static Optional<ScheduleType> fromCode(String code) {
    return Arrays.stream(values()).filter(type -> type.code().equalsIgnoreCase(code)).findFirst();
  }

  /**
   * As typed by users and stored in the database: {@code weather}, {@code forecast} or {@code
   * forecast_h}.
   */
  public String code() {
    return name().toLowerCase(Locale.ROOT);
  }

  public String description(Language lang) {
    return description.get(lang);
  }

  /** With emoji, for /help. */
  public String helpLabel(Language lang) {
    return helpLabel.get(lang);
  }
}
