package com.example.agentic.model_directed_loop.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = AgentOptionsConfig.class)
@ActiveProfiles("openai")
class AgentOptionsConfigOpenAiTest {

  @Autowired ChatOptions chatOptions;

  @Test
  void selectsOpenAiOptionsWithRequiredToolChoice() {
    assertThat(chatOptions).isInstanceOf(OpenAiChatOptions.class);
    OpenAiChatOptions opts = (OpenAiChatOptions) chatOptions;
    assertThat(opts.getToolChoice()).isEqualTo("required");
  }
}
