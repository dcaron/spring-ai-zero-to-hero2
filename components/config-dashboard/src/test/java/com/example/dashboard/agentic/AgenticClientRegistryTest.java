package com.example.dashboard.agentic;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.ServerSocket;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class AgenticClientRegistryTest {

  AgenticDemoCatalog catalog = new AgenticDemoCatalog();

  @Test
  void reportsDownWhenPortRefusesConnection() {
    AgenticClientRegistry reg = new AgenticClientRegistry(catalog, RestClient.builder());
    AgenticStatus s =
        reg.probe(
            new AgenticDemo("test", "T", "one", 1, "/x", false, AgenticDemo.TraceKind.SINGLE));
    assertThat(s.up()).isFalse();
    assertThat(s.provider()).isEqualTo("unknown");
  }

  @Test
  void reportsUpWhenPortIsListening() throws Exception {
    try (ServerSocket server = new ServerSocket(0)) {
      int port = server.getLocalPort();
      AgenticClientRegistry reg = new AgenticClientRegistry(catalog, RestClient.builder());
      AgenticStatus s =
          reg.probe(
              new AgenticDemo("test", "T", "one", port, "/x", false, AgenticDemo.TraceKind.SINGLE));
      assertThat(s.up()).isTrue();
    }
  }
}
