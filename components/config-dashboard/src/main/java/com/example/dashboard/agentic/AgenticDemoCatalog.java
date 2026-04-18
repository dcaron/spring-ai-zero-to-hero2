package com.example.dashboard.agentic;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AgenticDemoCatalog {

  private static final List<AgenticDemo> DEMOS =
      List.of(
          new AgenticDemo(
              "01",
              "Inner Monologue",
              "Single-step agent: always responds through send_message with inner thoughts",
              8091,
              "/agents/inner-monologue",
              false,
              AgenticDemo.TraceKind.SINGLE),
          new AgenticDemo(
              "02",
              "Model-Directed Loop",
              "Multi-step agent: model sets requestReinvocation to control the loop",
              8092,
              "/agents/model-directed-loop",
              true,
              AgenticDemo.TraceKind.MULTI_STEP));

  public List<AgenticDemo> all() {
    return DEMOS;
  }

  public AgenticDemo get(String id) {
    return DEMOS.stream()
        .filter(d -> d.id().equals(id))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown agentic demo: " + id));
  }
}
