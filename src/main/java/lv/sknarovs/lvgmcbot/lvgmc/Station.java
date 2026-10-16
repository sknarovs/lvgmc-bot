package lv.sknarovs.lvgmcbot.lvgmc;

import com.fasterxml.jackson.annotation.JsonProperty;

/** A weather station ({@code weather_monitoring_points}). */
record Station(@JsonProperty("kods") String code, @JsonProperty("nosaukums") String name) {
}
