package com.example.dashboard.agentic;

public record AgenticDemo(
    String id,
    String title,
    String oneLiner,
    int port,
    String basePath,
    boolean supportsLogin,
    TraceKind traceKind) {

  public enum TraceKind {
    SINGLE,
    MULTI_STEP
  }
}
