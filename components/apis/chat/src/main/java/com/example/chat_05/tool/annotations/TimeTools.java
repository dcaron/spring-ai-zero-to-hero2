package com.example.chat_05.tool.annotations;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class TimeTools {

  @Tool(
      description =
          "Returns the current time in a specific timezone based on IANA time zone identifier",
      returnDirect = false)
  public String currentTimeIn(
      @ToolParam(required = false, description = "IANA time zone identifiers") String timeZone) {
    ZonedDateTime now;
    if (timeZone == null) {
      now = ZonedDateTime.now();
    } else {
      now = ZonedDateTime.now(ZoneId.of(timeZone));
    }
    ZonedDateTime tomorrow = now.plusDays(1);
    return "Today is "
        + now.getDayOfWeek()
        + " "
        + now.format(DateTimeFormatter.ISO_DATE_TIME)
        + ". Tomorrow is "
        + tomorrow.getDayOfWeek()
        + " "
        + tomorrow.toLocalDate()
        + ".";
  }
}
