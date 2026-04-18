package com.example.agentic.model_directed_loop;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/agents/model-directed-loop")
public class ModelDirectedLoopAgentController {

  private final ChatClient.Builder builder;
  private final ChatOptions chatOptions;
  private Map<String, Agent> agents = new HashMap<>();

  public ModelDirectedLoopAgentController(ChatClient.Builder builder, ChatOptions chatOptions) {
    this.builder = builder;
    this.chatOptions = chatOptions;
  }

  private static final org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(ModelDirectedLoopAgentController.class);

  @PostMapping("/{id}")
  public AgentJson createAgent(
      @PathVariable(name = "id") String agentId,
      @org.springframework.web.bind.annotation.RequestBody(required = false)
          Map<String, Object> body) {
    String userContext = formatUserContext(body);
    if (userContext != null && !userContext.isBlank()) {
      log.info(
          "Agent '{}' created WITH user context (length={} chars):\n----\n{}\n----",
          agentId,
          userContext.length(),
          userContext);
    } else {
      log.info("Agent '{}' created WITHOUT user context (no customer in request body).", agentId);
    }
    Agent agent = new Agent(this.builder, agentId, this.chatOptions, userContext);
    agents.put(agentId, agent);
    return AgentJson.from(agent);
  }

  /**
   * Build a compact "User Context" block from the customer payload the dashboard sends after an
   * ACME login. Safe if {@code body} or its {@code customer} key is missing/empty.
   */
  @SuppressWarnings("unchecked")
  private static String formatUserContext(Map<String, Object> body) {
    if (body == null) return null;
    Object c = body.get("customer");
    if (!(c instanceof Map<?, ?>)) return null;
    Map<String, Object> customer = (Map<String, Object>) c;
    StringBuilder sb = new StringBuilder();
    sb.append("The user you are talking to is a known ACME Fitness customer:\n");
    append(sb, customer, "name", "Name");
    append(sb, customer, "email", "Email");
    append(sb, customer, "id", "Customer ID");
    Object addr = customer.get("address");
    if (addr instanceof Map<?, ?> a) {
      Object street = a.get("street");
      Object city = a.get("city");
      Object province = a.get("province");
      Object country = a.get("country");
      Object postalCode = a.get("postalCode");
      StringBuilder line = new StringBuilder();
      if (street != null) line.append(street);
      if (city != null) line.append(line.length() > 0 ? ", " : "").append(city);
      if (province != null) line.append(line.length() > 0 ? ", " : "").append(province);
      if (postalCode != null) line.append(line.length() > 0 ? " " : "").append(postalCode);
      if (country != null) line.append(line.length() > 0 ? ", " : "").append(country);
      if (line.length() > 0) sb.append("- Address: ").append(line).append("\n");
    }
    append(sb, customer, "created_at", "Customer since");
    sb.append(
        "\nAddress the user by first name when appropriate. You may reference this context"
            + " naturally (\"since you're in %s, …\") but never invent details not shown above.");
    return sb.toString();
  }

  private static void append(StringBuilder sb, Map<String, Object> m, String key, String label) {
    Object v = m.get(key);
    if (v != null && !String.valueOf(v).isBlank()) {
      sb.append("- ").append(label).append(": ").append(v).append("\n");
    }
  }

  @GetMapping("/{id}")
  public AgentJson getAgent(@PathVariable(name = "id") String agentId) {
    Agent agent = this.agents.get(agentId);
    if (agent == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found");
    }
    return AgentJson.from(agent);
  }

  @GetMapping(path = {"", "/"})
  public List<String> getAgentIds() {
    return this.agents.keySet().stream().collect(Collectors.toList());
  }

  @PostMapping("/{id}/messages")
  public ChatTraceResponse sendMessage(
      @PathVariable(name = "id") String agentId, @RequestBody ChatRequest request) {
    Agent agent = this.agents.get(agentId);
    if (agent == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found");
    }
    return agent.userMessage(request);
  }

  @PostMapping("/{id}/reset")
  public void resetAgent(@PathVariable(name = "id") String agentId) {
    Agent agent = this.agents.get(agentId);
    if (agent == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found");
    }
    agent.resetMemory();
  }

  @DeleteMapping("/{id}")
  public void deleteAgent(@PathVariable(name = "id") String agentId) {
    Agent agent = this.agents.remove(agentId);
    if (agent == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found");
    }
    agent.resetMemory();
  }

  /**
   * Update an existing agent's user context (system-prompt folding) without losing its chat memory.
   * The dashboard calls this endpoint after an ACME login so agents created BEFORE login also gain
   * the customer context. Send {@code {"customer": {...}}} to set or {@code {}} to clear.
   */
  @PostMapping("/{id}/context")
  public void updateContext(
      @PathVariable(name = "id") String agentId,
      @org.springframework.web.bind.annotation.RequestBody(required = false)
          Map<String, Object> body) {
    Agent agent = this.agents.get(agentId);
    if (agent == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found");
    }
    String newContext = formatUserContext(body);
    agent.setUserContext(newContext);
    log.info(
        "Agent '{}' context updated ({}).",
        agentId,
        newContext == null ? "cleared" : "length=" + newContext.length() + " chars");
  }

  @GetMapping("/{id}/log")
  public List<Map<String, Object>> getLog(@PathVariable(name = "id") String agentId) {
    Agent agent = this.agents.get(agentId);
    if (agent == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent not found");
    }
    return agent.getLog();
  }
}
