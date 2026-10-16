package lv.sknarovs.lvgmcbot.lvgmc;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * One step of a daily or hourly forecast; values are as LVĢMC formats them, {@code "-"} if unknown.
 */
public record ForecastEntry(
    @JsonProperty("nosaukums") String city,
    @JsonProperty("laiks") @JsonFormat(pattern = "yyyyMMddHHmm") LocalDateTime time,
    @JsonProperty("temperatura") String temperature,
    @JsonProperty("sajutu_temperatura") String feelsLike,
    @JsonProperty("relativais_mitrums") String humidity,
    @JsonProperty("veja_atrums") String windSpeed,
    @JsonProperty("veja_virziens") String windDirection,
    @JsonProperty("spiediens") String pressure,
    @JsonProperty("nokrisni_1h") String precipitation1h,
    @JsonProperty("nokrisni_12h") String precipitation12h,
    @JsonProperty("laika_apstaklu_ikona") String icon) {}
