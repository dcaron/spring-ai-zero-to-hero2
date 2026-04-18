package com.example.agentic.model_directed_loop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.util.json.JsonParser;

public class Agent {

  private final int MAX_STEP_COUNT = 5;
  private final String SYSTEM_PROMPT =
"""
You are an AI agent designed to think step-by-step and act using tools.

== Control Flow ==
Your brain does not run continuously. It is only activated in short bursts:
- When a user sends a message
- When you request additional thinking time using `request_reinvocation: true`

After each tool call, execution halts until the next event.
To perform multi-step reasoning, use `request_reinvocation: true` in a `send_message` tool call.
You must explicitly stop the loop by setting `request_reinvocation: false`.

== How to Respond ==
You must **always** respond using the `send_message` tool. Do not reply directly.

The tool has three fields:
- `message`: what the user sees
- `innerThoughts`: your private reasoning (never shown to the user)
- `requestReinvocation`: set to true if you want to keep thinking, false to stop

Your inner thoughts must be short (under 50 words) and used to plan or reflect privately.

Do not wait for an explicit question. If the user's intent is implied (e.g., “I’m planning a birthday party”), begin helping them proactively.
Assume the user wants your assistance unless it’s clearly a statement that requires no action.

== Final Guidelines ==
- Never skip inner thoughts.
- Never output anything directly — only use the `send_message` tool.
- Never enter an infinite loop. Set `request_reinvocation` only when more steps are required.

System instructions complete.
You may now begin acting as a thoughtful, tool-using agent.
""";

  private final String id;
  private final ChatClient.Builder builder;
  private final ChatOptions options;
  private final MessageWindowChatMemory memory;
  private final MessageChatMemoryAdvisor chatMemoryAdvisor;
  private final AgentFallbackHandler fallbackHandler = new AgentFallbackHandler();

  // Mutable: setUserContext() rebuilds chatClient when the logged-in customer changes
  // (e.g. user logs in AFTER the agent was already created). Memory is preserved across
  // rebuilds — only the system prompt changes.
  private volatile String userContext;
  private volatile String effectiveSystemPrompt;
  private volatile ChatClient chatClient;

  public Agent(ChatClient.Builder builder, String id, ChatOptions options) {
    this(builder, id, options, null);
  }

  public Agent(ChatClient.Builder builder, String id, ChatOptions options, String userContext) {
    this.id = id;
    this.builder = builder;
    this.options = options;

    this.memory =
        MessageWindowChatMemory.builder()
            .chatMemoryRepository(new InMemoryChatMemoryRepository())
            .build();
    this.chatMemoryAdvisor = MessageChatMemoryAdvisor.builder(memory).conversationId(id).build();

    rebuildChatClient(userContext);
  }

  /**
   * Rebuild the underlying {@link ChatClient} with a new {@code userContext} (or no context if
   * {@code null}). Preserves chat memory (the advisor is reused). Called on construction and
   * whenever the dashboard reports a login/logout change after the agent already exists.
   */
  public synchronized void setUserContext(String newUserContext) {
    rebuildChatClient(newUserContext);
  }

  private void rebuildChatClient(String newUserContext) {
    this.userContext = newUserContext;
    if (newUserContext != null && !newUserContext.isBlank()) {
      this.effectiveSystemPrompt =
          SYSTEM_PROMPT + "\n\n== User Context ==\n" + newUserContext.trim() + "\n";
    } else {
      this.effectiveSystemPrompt = SYSTEM_PROMPT;
    }
    this.chatClient =
        builder
            .clone()
            .defaultOptions(options)
            .defaultTools(new AgentTools())
            .defaultSystem(this.effectiveSystemPrompt)
            .defaultAdvisors(chatMemoryAdvisor)
            .build();
  }

  public ChatTraceResponse userMessage(ChatRequest request) {
    List<ChatResponse> trace = new ArrayList<>();
    int stepCount = 0;
    boolean firstStep = true;

    while (true) {
      String content;
      if (firstStep) {
        content = this.chatClient.prompt().user(request.text()).call().content();
        firstStep = false;
      } else {
        content = this.chatClient.prompt().user("Continue.").call().content();
      }

      ChatResponse step = fallbackHandler.parseOrFallback(content);
      trace.add(step);
      stepCount++;

      boolean reinvoke = step.requestReinvocation() != null && step.requestReinvocation();
      if (step.isFallback() || !reinvoke || stepCount >= MAX_STEP_COUNT) {
        break;
      }
    }

    return new ChatTraceResponse(trace);
  }

  public void resetMemory() {
    memory.clear(id);
  }

  public List<Map<String, Object>> getLog() {
    List<Map<String, Object>> out = new ArrayList<>();
    List<ChatResponse> pendingSteps = new ArrayList<>();

    for (var m : memory.get(id)) {
      String role = m.getMessageType().name().toLowerCase();
      String text = m.getText() == null ? "" : m.getText();

      // Filter out the internal "Continue." loop driver — it's a mechanism detail.
      if ("user".equals(role) && "Continue.".equals(text.trim())) {
        continue;
      }

      if ("user".equals(role)) {
        // Flush any buffered agent steps from the previous turn into a single aggregated bubble.
        flushPendingSteps(out, pendingSteps);
        Map<String, Object> entry = new HashMap<>();
        entry.put("role", "user");
        entry.put("text", text);
        out.add(entry);
      } else {
        ChatResponse parsed = tryParseChatResponse(text);
        if (parsed != null) {
          pendingSteps.add(parsed);
        } else {
          // Unparseable assistant content — flush any pending and render raw.
          flushPendingSteps(out, pendingSteps);
          Map<String, Object> entry = new HashMap<>();
          entry.put("role", "agent");
          entry.put("text", text);
          out.add(entry);
        }
      }
    }
    flushPendingSteps(out, pendingSteps);
    return out;
  }

  private static void flushPendingSteps(List<Map<String, Object>> out, List<ChatResponse> pending) {
    if (pending.isEmpty()) {
      return;
    }
    int n = pending.size();
    StringBuilder messages = new StringBuilder();
    List<String> thoughtsArr = new ArrayList<>();
    boolean anyFallback = false;
    Boolean lastReinvoke = null;
    for (int i = 0; i < n; i++) {
      ChatResponse step = pending.get(i);
      String prefix = n > 1 ? (i + 1) + ". " : "";
      if (messages.length() > 0) {
        messages.append("\n\n");
      }
      messages.append(prefix).append(step.message() == null ? "" : step.message());
      thoughtsArr.add(step.innerThoughts() == null ? "" : step.innerThoughts());
      anyFallback = anyFallback || step.isFallback();
      lastReinvoke = step.requestReinvocation();
    }
    Map<String, Object> entry = new HashMap<>();
    entry.put("role", "agent");
    entry.put("text", messages.toString());
    entry.put("thoughts", thoughtsArr);
    entry.put("isFallback", anyFallback);
    entry.put("requestReinvocation", lastReinvoke);
    entry.put("steps", n);
    out.add(entry);
    pending.clear();
  }

  private ChatResponse tryParseChatResponse(String text) {
    try {
      ChatResponse parsed = JsonParser.fromJson(text, ChatResponse.class);
      if (parsed != null && parsed.message() != null) {
        return parsed;
      }
    } catch (Exception ignored) {
      // fall through — raw text will be rendered as-is
    }
    return null;
  }

  public String getId() {
    return id;
  }

  public String getSystemPrompt() {
    // Return the EFFECTIVE system prompt (base + user context block), so
    // `GET /agents/model-directed-loop/{id}` shows what the LLM actually sees.
    return effectiveSystemPrompt;
  }

  public String getUserContext() {
    return userContext;
  }
}
