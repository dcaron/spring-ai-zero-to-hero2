package com.example.dashboard.agentic;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AgenticInspectorController.class)
@ActiveProfiles("ui")
class AgenticInspectorControllerTest {

  @SpringBootConfiguration
  @ComponentScan(
      basePackageClasses = AgenticInspectorController.class,
      includeFilters =
          @ComponentScan.Filter(
              type = FilterType.ASSIGNABLE_TYPE,
              classes = AgenticInspectorController.class),
      useDefaultFilters = false)
  static class TestConfig {}

  @Autowired MockMvc mvc;
  @MockitoBean AgenticClientRegistry registry;
  @MockitoBean AgenticDemoCatalog catalog;

  @Test
  void statusReturnsDownPayloadWhenRegistrySaysDown() throws Exception {
    AgenticDemo d =
        new AgenticDemo(
            "01",
            "Inner Monologue",
            "x",
            8091,
            "/agents/inner-monologue",
            false,
            AgenticDemo.TraceKind.SINGLE);
    when(catalog.get("01")).thenReturn(d);
    when(registry.probe(any()))
        .thenReturn(
            new AgenticStatus(false, "unknown", "unknown", 0, "./workshop.sh agentic start 01"));

    mvc.perform(get("/dashboard/agentic/01/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.up").value(false))
        .andExpect(jsonPath("$.startCommand").value("./workshop.sh agentic start 01"));
  }

  @Test
  void proxySendReturns503WhenOffline() throws Exception {
    AgenticDemo d =
        new AgenticDemo(
            "01",
            "Inner Monologue",
            "x",
            8091,
            "/agents/inner-monologue",
            false,
            AgenticDemo.TraceKind.SINGLE);
    when(catalog.get("01")).thenReturn(d);
    when(registry.probe(any()))
        .thenReturn(
            new AgenticStatus(false, "unknown", "unknown", 0, "./workshop.sh agentic start 01"));

    mvc.perform(
            post("/dashboard/agentic/01/agents/alice/messages")
                .contentType("application/json")
                .content("{\"text\":\"hi\"}"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.error").value("offline"))
        .andExpect(jsonPath("$.hint").exists());
  }
}
