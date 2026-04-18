package com.example.agentic.inner_monologue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.agentic.inner_monologue.dto.ChatRequest;
import com.example.agentic.inner_monologue.dto.ChatResponse;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;

class AgentTest {

  private ChatClient.Builder stubBuilder(String responseContent) {
    ChatClient.Builder builder = mock(ChatClient.Builder.class);
    ChatClient.Builder clone = mock(ChatClient.Builder.class);
    ChatClient client = mock(ChatClient.class);
    ChatClient.ChatClientRequestSpec req = mock(ChatClient.ChatClientRequestSpec.class);
    ChatClient.CallResponseSpec call = mock(ChatClient.CallResponseSpec.class);

    when(builder.clone()).thenReturn(clone);
    when(clone.defaultOptions(any(ChatOptions.class))).thenReturn(clone);
    when(clone.defaultTools(any(Object[].class))).thenReturn(clone);
    when(clone.defaultSystem(any(String.class))).thenReturn(clone);
    when(clone.defaultAdvisors(any(org.springframework.ai.chat.client.advisor.api.Advisor.class)))
        .thenReturn(clone);
    when(clone.build()).thenReturn(client);
    when(client.prompt()).thenReturn(req);
    when(req.user(any(String.class))).thenReturn(req);
    when(req.call()).thenReturn(call);
    when(call.content()).thenReturn(responseContent);
    return builder;
  }

  @Test
  void returnsStructuredResponseWhenJsonIsValid() {
    String validJson = "{\"message\":\"Hello\",\"innerThoughts\":\"Simple reply\"}";
    ChatClient.Builder b = stubBuilder(validJson);
    Agent agent = new Agent(b, "test", OpenAiChatOptions.builder().build());

    ChatResponse result = agent.userMessage(new ChatRequest("hi"));

    assertThat(result.message()).isEqualTo("Hello");
    assertThat(result.innerThoughts()).isEqualTo("Simple reply");
    assertThat(result.isFallback()).isFalse();
  }

  @Test
  void returnsFallbackResponseWhenContentIsFreeText() {
    String freeText = "I'd love to help you with that!";
    ChatClient.Builder b = stubBuilder(freeText);
    Agent agent = new Agent(b, "test", OpenAiChatOptions.builder().build());

    ChatResponse result = agent.userMessage(new ChatRequest("hi"));

    assertThat(result.message()).isEqualTo(freeText);
    assertThat(result.innerThoughts()).startsWith("[fallback:");
    assertThat(result.isFallback()).isTrue();
  }

  @Test
  void returnsFallbackResponseWhenJsonIsMalformed() {
    String badJson = "{not even close";
    ChatClient.Builder b = stubBuilder(badJson);
    Agent agent = new Agent(b, "test", OpenAiChatOptions.builder().build());

    ChatResponse result = agent.userMessage(new ChatRequest("hi"));

    assertThat(result.isFallback()).isTrue();
    assertThat(result.message()).contains("not even close");
  }
}
