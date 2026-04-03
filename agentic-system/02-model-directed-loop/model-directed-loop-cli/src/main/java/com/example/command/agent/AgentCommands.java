package com.example.command.agent;

import com.example.JsonUtils;
import com.example.command.agent.dto.ChatResponse;
import com.example.command.agent.dto.ChatTraceResponse;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;

public class AgentCommands {

  private static final List<String> MATRIX_AGENT_NAMES =
      List.of(
          "neo",
          "trinity",
          "morpheus",
          "smith",
          "cypher",
          "oracle",
          "apoc",
          "switch",
          "tank",
          "dozer");

  private final AgentContext agentContext;
  private final JsonUtils jsonUtils;

  public AgentCommands(AgentContext agentContext, JsonUtils jsonUtils) {
    this.agentContext = agentContext;
    this.jsonUtils = jsonUtils;
  }

  @Command(name = "create", description = "Create a new agent")
  public void create(@Option(longName = "id", required = false) String id) {
    if (id == null || id.isBlank()) {
      id = generateUniqueAgentId();
    }
    this.agentContext.createAgent(id);
    agentContext.setCurrentAgentId(id);
    agentContext.getMessages().clear();
    agentContext.getMessages().add("[SYSTEM] Created agent with ID: " + id);
    agentContext.getMessages().add("[SYSTEM] Switched to agent: " + id + "\n");
    printChat();
  }

  private String generateUniqueAgentId() {
    List<String> existing = agentContext.listAgents();
    for (int i = 0; i < 100; i++) {
      String candidate =
          MATRIX_AGENT_NAMES.get(ThreadLocalRandom.current().nextInt(MATRIX_AGENT_NAMES.size()));
      if (!existing.contains(candidate)) {
        return candidate;
      }
    }
    int suffix = 1;
    while (true) {
      String candidate = "neo-" + suffix++;
      if (!existing.contains(candidate)) {
        return candidate;
      }
    }
  }

  @Command(name = "target", description = "set the agent to send messages to")
  public void targetAgent(@Option(longName = "id", required = true) String id) {
    List<String> ids = agentContext.listAgents();
    if (!ids.contains(id)) {
      System.out.println("[ERROR] No agent with ID: " + id + " on server.");
      return;
    }
    agentContext.setCurrentAgentId(id);
    agentContext.getMessages().clear();
    agentContext.getMessages().add("[SYSTEM] Switched to agent: " + id + "\n");
    printChat();
    refreshPrompt();
  }

  @Command(name = "login", description = "Login the user to the ACME fitness store")
  public void login(String email) {
    String customer = this.agentContext.login(email);
    System.out.println("[LOGIN] Welcome " + customer + "!");
  }

  @Command(name = "send", description = "Send a message to the current agent")
  public void send(String text) {
    String agentId = agentContext.getCurrentAgentId();
    if (agentId == null) {
      System.out.println("[ERROR] No active agent. Use `agent create` or `agent switch` first.");
      return;
    }

    agentContext.getMessages().add("[USER] " + text);

    try {
      ChatTraceResponse trace = agentContext.sendMessage(agentId, text);
      for (ChatResponse step : trace.steps()) {
        agentContext.getMessages().add("[THOUGHT] " + step.innerThoughts());
        agentContext.getMessages().add("[AGENT] " + step.message());
        agentContext.getMessages().add(""); // blank line between steps
      }
    } catch (Exception e) {
      agentContext.getMessages().add("[ERROR] Failed to send message: " + e.getMessage());
    }

    printChat();
  }

  @Command(name = "log", description = "Show the current chat log")
  public void log() {
    if (agentContext.getCurrentAgentId() == null) {
      System.out.println("[ERROR] No active agent. Use `agent create` or `agent switch` first.");
      return;
    }
    printChat();
  }

  @Command(name = "status", description = "Show the currently active agent")
  public void status() {
    String agentId = agentContext.getCurrentAgentId();
    if (agentId == null) {
      System.out.println("No active agent.");
      return;
    }
    var agent = agentContext.getAgent(agentId);
    String json = jsonUtils.toPrettyJson(agent);
    System.out.println(json);
  }

  @Command(name = "list", description = "List all created agents")
  public void list() {
    List<String> ids = agentContext.listAgents();
    if (ids.isEmpty()) {
      System.out.println("No agents found on the server.");
      return;
    }

    String current = agentContext.getCurrentAgentId();
    for (String id : ids) {
      String marker = Objects.equals(id, current) ? "*" : " ";
      System.out.println(marker + " " + id);
    }
  }

  private void printChat() {
    String agentId = agentContext.getCurrentAgentId();
    System.out.println("\n=== Agentic Chat [" + agentId + "] ===");
    agentContext.getMessages().forEach(System.out::println);
    System.out.println("===============================\n");
  }

  private void refreshPrompt() {
    System.out.println(); // move cursor to next line
  }
}
