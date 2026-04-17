package com.example.dashboard.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("ui")
public class McpStdioInvoker {

  private static final Logger log = LoggerFactory.getLogger(McpStdioInvoker.class);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

  private final String jarPath;

  public McpStdioInvoker(
      @Value(
              "${dashboard.mcp.stdio.jar-path:mcp/01-mcp-stdio-server/target/01-mcp-stdio-server-0.0.1-SNAPSHOT.jar}")
          String jarPath) {
    this.jarPath = jarPath;
  }

  public boolean jarPresent() {
    return resolveJar() != null;
  }

  public String jarPath() {
    return jarPath;
  }

  /**
   * Resolve the configured (possibly relative) jar path. Absolute paths are returned as-is;
   * relative paths are first tried against the JVM cwd, then walked up the directory tree to handle
   * the common case where the provider app runs from its module directory instead of the project
   * root. Returns {@code null} if the jar cannot be found.
   */
  Path resolveJar() {
    Path configured = Paths.get(jarPath);
    if (Files.exists(configured)) {
      return configured.toAbsolutePath();
    }
    if (configured.isAbsolute()) {
      return null;
    }
    Path current = Paths.get("").toAbsolutePath();
    for (int i = 0; i < 5; i++) {
      Path candidate = current.resolve(jarPath);
      if (Files.exists(candidate)) {
        return candidate.toAbsolutePath();
      }
      current = current.getParent();
      if (current == null) {
        break;
      }
    }
    return null;
  }

  public ListToolsResult listTools() {
    return withClient(McpSyncClient::listTools);
  }

  public CallToolResult callTool(String name, Map<String, Object> args) {
    return withClient(c -> c.callTool(new CallToolRequest(name, args)));
  }

  <T> T withClient(Function<McpSyncClient, T> fn) {
    Path resolved = resolveJar();
    if (resolved == null) {
      throw new IllegalStateException("STDIO jar not built at " + jarPath);
    }
    var params =
        ServerParameters.builder("java")
            .args(
                "-Dspring.ai.mcp.server.stdio=true",
                "-Dspring.main.web-application-type=none",
                "-Dlogging.pattern.console=",
                "-jar",
                resolved.toString())
            .build();
    var transport = new StdioClientTransport(params, McpJsonDefaults.getMapper());
    McpSyncClient client = McpClient.sync(transport).requestTimeout(REQUEST_TIMEOUT).build();
    try {
      client.initialize();
      return fn.apply(client);
    } finally {
      try {
        client.closeGracefully();
      } catch (Exception e) {
        log.warn("Error closing STDIO client: {}", e.getMessage());
      }
    }
  }
}
