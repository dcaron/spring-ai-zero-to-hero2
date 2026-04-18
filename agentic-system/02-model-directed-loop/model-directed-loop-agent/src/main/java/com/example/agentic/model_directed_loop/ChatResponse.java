package com.example.agentic.model_directed_loop;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ChatResponse(
    String message, String innerThoughts, Boolean requestReinvocation, boolean isFallback) {
  @JsonCreator
  public ChatResponse(
      @JsonProperty("message") String message,
      @JsonProperty("innerThoughts") String innerThoughts,
      @JsonProperty("requestReinvocation") Boolean requestReinvocation) {
    this(message, innerThoughts, requestReinvocation, false);
  }
}
