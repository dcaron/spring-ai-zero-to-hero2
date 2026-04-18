package com.example.dashboard.agentic;

public final class AgenticFallbackDetector {
  private static final String PREFIX = "[fallback: ";

  private AgenticFallbackDetector() {}

  public static boolean isFallback(String innerThoughts) {
    return innerThoughts != null && innerThoughts.startsWith(PREFIX);
  }
}
