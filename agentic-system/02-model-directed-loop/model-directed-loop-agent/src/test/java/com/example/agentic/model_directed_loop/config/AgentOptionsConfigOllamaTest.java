package com.example.agentic.model_directed_loop.config;

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

  @Autowired ChatOptions chatOptions;

  @Test
  void selectsOllamaChatOptionsWithConfiguredModel() {
    assertThat(chatOptions).isInstanceOf(OllamaChatOptions.class);
    OllamaChatOptions opts = (OllamaChatOptions) chatOptions;
    assertThat(opts.getModel()).isEqualTo("qwen3");
  }
}
