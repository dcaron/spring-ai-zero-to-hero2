package com.example.dashboard.mcp;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(McpInspectorController.class)
@ActiveProfiles("ui")
class McpInspectorControllerTest {

  @Autowired MockMvc mockMvc;
  @MockitoBean McpClientRegistry registry;
  @MockitoBean McpStdioInvoker stdio;

  @SpringBootConfiguration
  @ComponentScan(
      basePackageClasses = McpInspectorController.class,
      includeFilters =
          @ComponentScan.Filter(
              type = FilterType.ASSIGNABLE_TYPE,
              classes = McpInspectorController.class),
      useDefaultFilters = false)
  static class TestConfig {
    @Bean
    McpDemoCatalog catalog() {
      return new McpDemoCatalog();
    }
  }

  @Test
  void statusHttpDemoUp() throws Exception {
    when(registry.isUp("02")).thenReturn(true);

    mockMvc
        .perform(get("/dashboard/mcp/02/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("02"))
        .andExpect(jsonPath("$.port").value(8081))
        .andExpect(jsonPath("$.status").value("up"))
        .andExpect(jsonPath("$.startCommand").value("./workshop.sh mcp start 02"));
  }

  @Test
  void statusStdioJarMissing() throws Exception {
    when(stdio.jarPresent()).thenReturn(false);

    mockMvc
        .perform(get("/dashboard/mcp/01/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value("01"))
        .andExpect(jsonPath("$.status").value("down"))
        .andExpect(jsonPath("$.startCommand").value("./workshop.sh mcp build-01"));
  }
}
