package com.example.agentic.model_directed_loop;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ModelDirectedLoopAgentController.class)
class ModelDirectedLoopAgentControllerTest {

  @SpringBootConfiguration
  @ComponentScan(
      basePackageClasses = ModelDirectedLoopAgentController.class,
      includeFilters =
          @ComponentScan.Filter(
              type = FilterType.ASSIGNABLE_TYPE,
              classes = ModelDirectedLoopAgentController.class),
      useDefaultFilters = false)
  static class Config {
    @Bean
    ChatClient.Builder builder() {
      return mock(ChatClient.Builder.class);
    }

    @Bean
    ChatOptions chatOptions() {
      return OpenAiChatOptions.builder().toolChoice("required").build();
    }
  }

  @Autowired MockMvc mvc;

  @Test
  void resetOnUnknownAgentReturns404() throws Exception {
    mvc.perform(post("/agents/model-directed-loop/ghost/reset")).andExpect(status().isNotFound());
  }

  @Test
  void logOnUnknownAgentReturns404() throws Exception {
    mvc.perform(get("/agents/model-directed-loop/ghost/log")).andExpect(status().isNotFound());
  }

  @Test
  void listStartsEmpty() throws Exception {
    mvc.perform(get("/agents/model-directed-loop/"))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }
}
