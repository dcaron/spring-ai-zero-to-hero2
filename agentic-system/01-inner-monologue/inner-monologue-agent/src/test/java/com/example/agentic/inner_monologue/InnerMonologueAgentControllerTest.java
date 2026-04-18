package com.example.agentic.inner_monologue;

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

@WebMvcTest(InnerMonologueAgentController.class)
class InnerMonologueAgentControllerTest {

  @SpringBootConfiguration
  @ComponentScan(
      basePackageClasses = InnerMonologueAgentController.class,
      includeFilters =
          @ComponentScan.Filter(
              type = FilterType.ASSIGNABLE_TYPE,
              classes = InnerMonologueAgentController.class),
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
    mvc.perform(post("/agents/inner-monologue/ghost/reset")).andExpect(status().isNotFound());
  }

  @Test
  void logOnUnknownAgentReturns404() throws Exception {
    mvc.perform(get("/agents/inner-monologue/ghost/log")).andExpect(status().isNotFound());
  }

  @Test
  void listStartsEmpty() throws Exception {
    mvc.perform(get("/agents/inner-monologue/"))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }
}
