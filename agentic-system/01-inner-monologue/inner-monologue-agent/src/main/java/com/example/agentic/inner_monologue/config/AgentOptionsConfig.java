package com.example.agentic.inner_monologue.config;

import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Selects the per-provider {@link ChatOptions} at agent construction time. The {@code openai} path
 * uses {@code toolChoice("required")} to force the model to always invoke {@code send_message}; the
 * {@code ollama} path selects a tool-capable model via the {@code agent.ollama.model} property
 * (defaults to {@code qwen3}).
 */
@Configuration("innerMonologueAgentOptionsConfig")
public class AgentOptionsConfig {

  @Bean
  @Profile("!ollama")
  public ChatOptions openAiAgentOptions() {
    return OpenAiChatOptions.builder().toolChoice("required").build();
  }

  @Bean
  @Profile("ollama")
  public ChatOptions ollamaAgentOptions(@Value("${agent.ollama.model:qwen3}") String model) {
    return OllamaChatOptions.builder().model(model).build();
  }
}
