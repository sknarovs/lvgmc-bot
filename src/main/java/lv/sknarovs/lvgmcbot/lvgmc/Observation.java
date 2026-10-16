package lv.sknarovs.lvgmcbot.lvgmc;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * An hourly station observation ({@code weather_monitoring_data}); values are as LVĢMC formats
 * them.
 */
public record Observation(
    @JsonProperty("station_code") String stationCode,
    @JsonProperty("name") String stationName,
    @JsonProperty("time") @JsonFormat(pattern = "yyyy.MM.dd HH:mm:ss") LocalDateTime timeUtc,
    @JsonProperty("air_temperature_actual") String temperature,
    @JsonProperty("air_temperature_feel") String feelsLike,
    @JsonProperty("humidity_actual") String humidity,
    @JsonProperty("wind_speed_actual") String windSpeed,
    @JsonProperty("wind_speed_gust") String windGust,
    @JsonProperty("wind_direction_actual") String windDirection,
    @JsonProperty("pressure_sea_level") String pressure,
    @JsonProperty("precipitation") String precipitation,
    @JsonProperty("clouds") String clouds,
    @JsonProperty("visibility_actual") String visibilityMeters,
    @JsonProperty("description") String description) {

  /** When it was observed, in Latvian time; {@code null} if unknown. */
  public ZonedDateTime time() {
    return timeUtc == null ? null : timeUtc.atOffset(ZoneOffset.UTC).atZoneSameInstant(Lvgmc.ZONE);
  }
}
