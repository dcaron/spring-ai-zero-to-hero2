package com.example;

import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;

@Component
public class JsonUtils {

  private final ObjectWriter prettyWriter;

  public JsonUtils(ObjectMapper objectMapper) {
    this.prettyWriter = objectMapper.writerWithDefaultPrettyPrinter();
  }

  public String toPrettyJson(Object obj) {
    try {
      return prettyWriter.writeValueAsString(obj);
    } catch (Exception e) {
      return "[ERROR] Failed to format JSON: " + e.getMessage();
    }
  }
}
