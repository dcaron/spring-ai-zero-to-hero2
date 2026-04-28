package com.example.agentic.inner_monologue;

import com.example.agentic.inner_monologue.dto.ChatRequest;
import com.example.agentic.inner_monologue.dto.ChatResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.util.json.JsonParser;

public class Agent {

  private static final Logger log = LoggerFactory.getLogger(Agent.class);
  static final String FALLBACK_MARKER = "[fallback: model replied without tool]";

  private final String SYSTEM_PROMPT =
      """
      You are an AI agent.

      You always respond using the `send_message` tool. You never reply directly with a message.

      When you respond:
      - Use `inner_thoughts` to write private thoughts (the user never sees this).
      - Use `message` to write what the user will see.
      - Keep your `inner_thoughts` under 50 words.

      Never skip inner thoughts. Never output anything except by calling the `send_message` tool.
      """;

  private final String id;
  private final ChatClient chatClient;
  private final MessageWindowChatMemory memory;

  public Agent(ChatClient.Builder builder, String id, ChatOptions.Builder options) {
    this.id = id;

    this.memory =
        MessageWindowChatMemory.builder()
            .chatMemoryRepository(new InMemoryChatMemoryRepository())
            .build();
    var chatMemoryAdvisor = MessageChatMemoryAdvisor.builder(memory).conversationId(id).build();

    this.chatClient =
        builder
            .clone()
            .defaultOptions(options)
            .defaultTools(new AgentTools())
            .defaultSystem(SYSTEM_PROMPT)
            .defaultAdvisors(chatMemoryAdvisor)
            .build();
  }

  public ChatResponse userMessage(ChatRequest request) {
    String content = this.chatClient.prompt().user(request.text()).call().content();
    return parseOrFallback(content);
  }

  public void resetMemory() {
    memory.clear(id);
  }

  public List<Map<String, Object>> getLog() {
    List<Map<String, Object>> out = new ArrayList<>();
    memory
        .get(id)
        .forEach(
            m -> {
              String role = m.getMessageType().name().toLowerCase();
              String text = m.getText() == null ? "" : m.getText();
              Map<String, Object> entry = new HashMap<>();
              entry.put("role", "user".equals(role) ? "user" : "agent");
              if ("user".equals(role)) {
                entry.put("text", text);
              } else {
                ChatResponse parsed = tryParseChatResponse(text);
                if (parsed != null) {
                  entry.put("text", parsed.message());
                  entry.put("innerThoughts", parsed.innerThoughts());
                  entry.put("isFallback", parsed.isFallback());
                } else {
                  entry.put("text", text);
                }
              }
              out.add(entry);
            });
    return out;
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
    return SYSTEM_PROMPT;
  }

  private ChatResponse parseOrFallback(String content) {
    try {
      ChatResponse parsed = JsonParser.fromJson(content, ChatResponse.class);
      if (parsed != null && parsed.message() != null) {
        return parsed;
      }
    } catch (Exception e) {
      log.warn("Fallback: agent content not parseable as send_message tool call. Raw: {}", content);
    }
    return new ChatResponse(content, FALLBACK_MARKER, true);
  }
}
