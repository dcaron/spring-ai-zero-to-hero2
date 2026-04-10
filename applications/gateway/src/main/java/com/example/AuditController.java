package com.example;

import com.example.log.AuditLogEntry;
import com.example.log.OpenAiAuditor;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
public class AuditController {

  private final OpenAiAuditor auditor;

  public AuditController(OpenAiAuditor auditor) {
    this.auditor = auditor;
  }

  @GetMapping("/audit/latest")
  public ResponseEntity<?> latest() {
    AuditLogEntry entry = auditor.getLatestEntry();
    if (entry == null) {
      return ResponseEntity.noContent().build();
    }
    return ResponseEntity.ok(
        Map.of(
            "id", entry.getId().toString().substring(0, 8),
            "request",
                Map.of(
                    "method", nullSafe(entry.getRequest().getMethod()),
                    "uri", nullSafe(entry.getRequest().getDestinationUri()),
                    "body", nullSafe(entry.getRequest().getBody())),
            "response", Map.of("body", nullSafe(entry.getResponse().getBody()))));
  }

  private static String nullSafe(String value) {
    return value == null ? "" : value;
  }
}
