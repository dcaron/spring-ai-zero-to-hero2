package com.example.dashboard.mcp;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard/mcp")
@Profile("ui")
public class McpInspectorController {

  private final McpDemoCatalog catalog;
  private final McpClientRegistry registry;
  private final McpStdioInvoker stdio;

  public McpInspectorController(
      McpDemoCatalog catalog, McpClientRegistry registry, McpStdioInvoker stdio) {
    this.catalog = catalog;
    this.registry = registry;
    this.stdio = stdio;
  }

  @GetMapping("/{id}/status")
  public ResponseEntity<Map<String, Object>> status(@PathVariable String id) {
    McpDemo demo = catalog.get(id);
    boolean up;
    String startCommand;
    if (demo.port() == null) {
      up = stdio.jarPresent();
      startCommand = "./workshop.sh mcp build-01";
    } else {
      up = registry.isUp(id);
      startCommand = "./workshop.sh mcp start " + id;
    }
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("id", id);
    body.put("title", demo.title());
    body.put("transport", demo.transport().name());
    body.put("port", demo.port());
    body.put("modulePath", demo.modulePath());
    body.put("capabilities", demo.capabilities().stream().map(Enum::name).toList());
    body.put("status", up ? "up" : "down");
    body.put("startCommand", startCommand);
    return ResponseEntity.ok(body);
  }
}
