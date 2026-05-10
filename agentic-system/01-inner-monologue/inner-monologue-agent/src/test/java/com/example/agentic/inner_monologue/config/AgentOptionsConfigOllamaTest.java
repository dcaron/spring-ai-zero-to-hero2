package com.example.agentic.inner_monologue.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = AgentOptionsConfig.class)
@ActiveProfiles("ollama")
class AgentOptionsConfigOllamaTest {

  // Spring AI 2.0.0-M6: bean exposes a ChatOptions.Builder so the chat client can merge defaults.
  @Autowired ChatOptions.Builder chatOptionsBuilder;

  @Test
  void selectsOllamaChatOptionsWithConfiguredModel() {
    ChatOptions chatOptions = chatOptionsBuilder.build();
    assertThat(chatOptions).isInstanceOf(OllamaChatOptions.class);
    OllamaChatOptions opts = (OllamaChatOptions) chatOptions;
    assertThat(opts.getModel()).isEqualTo("qwen3");
  }
}
