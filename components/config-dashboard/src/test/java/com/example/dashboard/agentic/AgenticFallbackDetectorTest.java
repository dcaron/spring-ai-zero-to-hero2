package com.example.dashboard.agentic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AgenticFallbackDetectorTest {
  @Test
  void detectsFallbackByPrefix() {
    assertThat(AgenticFallbackDetector.isFallback("[fallback: model replied without tool]"))
        .isTrue();
  }

  @Test
  void normalInnerThoughtsAreNotFallback() {
    assertThat(AgenticFallbackDetector.isFallback("Reasoning about arithmetic")).isFalse();
  }

  @Test
  void nullAndEmptyAreNotFallback() {
    assertThat(AgenticFallbackDetector.isFallback(null)).isFalse();
    assertThat(AgenticFallbackDetector.isFallback("")).isFalse();
  }
}
