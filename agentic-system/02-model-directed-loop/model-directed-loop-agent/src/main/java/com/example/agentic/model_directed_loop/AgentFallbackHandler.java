package com.example.agentic.model_directed_loop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.util.json.JsonParser;

public class AgentFallbackHandler {

  private static final Logger log = LoggerFactory.getLogger(AgentFallbackHandler.class);
  static final String FALLBACK_MARKER = "[fallback: model replied without tool]";

  public ChatResponse parseOrFallback(String content) {
    try {
      ChatResponse parsed = JsonParser.fromJson(content, ChatResponse.class);
      if (parsed != null && parsed.message() != null) {
        return parsed;
      }
    } catch (Exception e) {
      log.warn("Fallback: agent content not parseable as send_message tool call. Raw: {}", content);
    }
    return new ChatResponse(content, FALLBACK_MARKER, false, true);
  }
}
