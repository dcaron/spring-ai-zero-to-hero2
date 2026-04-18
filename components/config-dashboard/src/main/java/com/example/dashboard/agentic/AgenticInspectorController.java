package com.example.dashboard.agentic;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@Profile("ui")
@RestController
@RequestMapping("/dashboard/agentic")
public class AgenticInspectorController {

  private static final Logger log = LoggerFactory.getLogger(AgenticInspectorController.class);
  private final AgenticDemoCatalog catalog;
  private final AgenticClientRegistry registry;
  private final Tracer tracer;

  public AgenticInspectorController(
      AgenticDemoCatalog catalog,
      AgenticClientRegistry registry,
      @Autowired(required = false) Tracer tracer) {
    this.catalog = catalog;
    this.registry = registry;
    this.tracer = tracer;
  }

  @GetMapping("/{demoId}/status")
  public ResponseEntity<AgenticStatus> status(@PathVariable String demoId) {
    return ResponseEntity.ok(registry.probe(catalog.get(demoId)));
  }

  @GetMapping("/{demoId}/agents")
  public ResponseEntity<?> listAgents(@PathVariable String demoId) {
    return proxyGet(demoId, catalog.get(demoId).basePath() + "/", Object.class);
  }

  @PostMapping("/{demoId}/agents")
  public ResponseEntity<?> createAgent(
      @PathVariable String demoId,
      jakarta.servlet.http.HttpServletRequest request,
      @RequestBody(required = false) String rawBody) {
    // Read the raw request body as a string so we can see EXACTLY what the browser sent.
    // We then parse it to a Map manually — this sidesteps any hidden ObjectMapper config
    // that might be filtering fields.
    log.info(
        "Proxy /dashboard/agentic/{}/agents contentType={} contentLength={} rawBody={}",
        demoId,
        request.getContentType(),
        request.getContentLength(),
        rawBody);
    Map<String, Object> body;
    try {
      body =
          rawBody == null || rawBody.isBlank()
              ? new HashMap<>()
              : new com.fasterxml.jackson.databind.ObjectMapper()
                  .readValue(
                      rawBody,
                      new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
    } catch (Exception e) {
      log.warn("Failed to parse request body as Map: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(Map.of("error", "invalid-json", "detail", e.getMessage()));
    }
    AgenticDemo d = catalog.get(demoId);
    String id = String.valueOf(body.get("id"));
    Object customer = body.get("customer");
    log.info(
        "Proxy parsed: id={} bodyKeys={} customerPresent={}", id, body.keySet(), customer != null);
    return proxyPost(demoId, d.basePath() + "/" + id, body, Object.class);
  }

  @DeleteMapping("/{demoId}/agents/{agentId}")
  public ResponseEntity<?> deleteAgent(@PathVariable String demoId, @PathVariable String agentId) {
    return proxyDelete(demoId, catalog.get(demoId).basePath() + "/" + agentId, Object.class);
  }

  @PostMapping("/{demoId}/agents/{agentId}/reset")
  public ResponseEntity<?> reset(@PathVariable String demoId, @PathVariable String agentId) {
    return proxyPost(
        demoId, catalog.get(demoId).basePath() + "/" + agentId + "/reset", null, Object.class);
  }

  @PostMapping("/{demoId}/agents/{agentId}/context")
  public ResponseEntity<?> updateContext(
      @PathVariable String demoId,
      @PathVariable String agentId,
      @RequestBody Map<String, Object> body) {
    return proxyPost(
        demoId, catalog.get(demoId).basePath() + "/" + agentId + "/context", body, Object.class);
  }

  @GetMapping("/{demoId}/agents/{agentId}")
  public ResponseEntity<?> getAgent(@PathVariable String demoId, @PathVariable String agentId) {
    return proxyGet(demoId, catalog.get(demoId).basePath() + "/" + agentId, Object.class);
  }

  @GetMapping("/{demoId}/agents/{agentId}/log")
  public ResponseEntity<?> getLog(@PathVariable String demoId, @PathVariable String agentId) {
    return proxyGet(demoId, catalog.get(demoId).basePath() + "/" + agentId + "/log", Object.class);
  }

  @PostMapping("/{demoId}/agents/{agentId}/messages")
  public ResponseEntity<?> sendMessage(
      @PathVariable String demoId,
      @PathVariable String agentId,
      @RequestBody Map<String, String> body) {
    return proxyPost(
        demoId, catalog.get(demoId).basePath() + "/" + agentId + "/messages", body, Object.class);
  }

  @PostMapping("/02/acme/login")
  public ResponseEntity<?> acmeLogin(@RequestBody Map<String, String> body) {
    return proxyPost("02", "/acme/login", body, Object.class);
  }

  private <T> ResponseEntity<?> proxyGet(String demoId, String path, Class<T> type) {
    AgenticDemo demo = catalog.get(demoId);
    AgenticStatus s = registry.probe(demo);
    if (!s.up()) return offline(s);
    try {
      RestClient c = registry.clientFor(demo);
      T body = c.get().uri(path).retrieve().body(type);
      return ResponseEntity.ok(body);
    } catch (Exception e) {
      return providerError(e);
    }
  }

  private <T> ResponseEntity<?> proxyPost(String demoId, String path, Object body, Class<T> type) {
    AgenticDemo demo = catalog.get(demoId);
    AgenticStatus s = registry.probe(demo);
    if (!s.up()) return offline(s);
    try {
      RestClient c = registry.clientFor(demo);
      T out =
          body == null
              ? c.post().uri(path).retrieve().body(type)
              : c.post()
                  .uri(path)
                  .contentType(MediaType.APPLICATION_JSON)
                  .body(body)
                  .retrieve()
                  .body(type);
      return ResponseEntity.ok(out);
    } catch (Exception e) {
      return providerError(e);
    }
  }

  private <T> ResponseEntity<?> proxyDelete(String demoId, String path, Class<T> type) {
    AgenticDemo demo = catalog.get(demoId);
    AgenticStatus s = registry.probe(demo);
    if (!s.up()) return offline(s);
    try {
      RestClient c = registry.clientFor(demo);
      T body = c.delete().uri(path).retrieve().body(type);
      return ResponseEntity.ok(body);
    } catch (Exception e) {
      return providerError(e);
    }
  }

  private ResponseEntity<Map<String, Object>> offline(AgenticStatus s) {
    Map<String, Object> body = new HashMap<>();
    body.put("error", "offline");
    body.put("hint", s.startCommand());
    body.put("traceId", currentTraceId());
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
  }

  private ResponseEntity<Map<String, Object>> providerError(Exception e) {
    log.warn("Provider error: {}", e.getMessage());
    Map<String, Object> body = new HashMap<>();
    body.put("error", "provider-error");
    body.put("detail", e.getMessage());
    body.put("traceId", currentTraceId());
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
  }

  private String currentTraceId() {
    if (tracer == null) return "";
    Span span = tracer.currentSpan();
    return span == null ? "" : span.context().traceId();
  }
}
