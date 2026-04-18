package com.example.agentic.inner_monologue.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ChatResponse(String message, String innerThoughts, boolean isFallback) {
  @JsonCreator
  public ChatResponse(
      @JsonProperty("message") String message,
      @JsonProperty("innerThoughts") String innerThoughts) {
    this(message, innerThoughts, false);
  }
}
