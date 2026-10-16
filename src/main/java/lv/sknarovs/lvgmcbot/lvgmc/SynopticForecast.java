package lv.sknarovs.lvgmcbot.lvgmc;

import com.fasterxml.jackson.annotation.JsonProperty;

/** The national text forecast ({@code sinopt_prognozes}), only published in Latvian. */
public record SynopticForecast(
    @JsonProperty("kods") String title,
    @JsonProperty("datums") String date,
    @JsonProperty("laiks_no") String from,
    @JsonProperty("laiks_lidz") String to,
    @JsonProperty("teksti") Texts texts) {

  public String text() {
    return texts == null || texts.text() == null ? "" : texts.text();
  }

  public record Texts(@JsonProperty("teksts") String text) {}
}
