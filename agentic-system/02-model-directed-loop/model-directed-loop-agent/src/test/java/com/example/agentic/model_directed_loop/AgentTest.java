package com.example.agentic.model_directed_loop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;

class AgentTest {

  private ChatClient.Builder stubBuilder(String... responsesInOrder) {
    ChatClient.Builder builder = mock(ChatClient.Builder.class);
    ChatClient.Builder clone = mock(ChatClient.Builder.class);
    ChatClient client = mock(ChatClient.class);
    ChatClient.ChatClientRequestSpec req = mock(ChatClient.ChatClientRequestSpec.class);
    ChatClient.CallResponseSpec call = mock(ChatClient.CallResponseSpec.class);

    when(builder.clone()).thenReturn(clone);
    when(clone.defaultOptions(any(ChatOptions.Builder.class))).thenReturn(clone);
    when(clone.defaultTools(any(Object[].class))).thenReturn(clone);
    when(clone.defaultSystem(any(String.class))).thenReturn(clone);
    // Spring AI 2.0.0-M6: Agent now wires the chat-memory advisor via the Consumer<AdvisorSpec>
    // overload (so it can also set the ChatMemory.CONVERSATION_ID param). Stub both overloads.
    when(clone.defaultAdvisors(any(org.springframework.ai.chat.client.advisor.api.Advisor.class)))
        .thenReturn(clone);
    when(clone.defaultAdvisors(any(java.util.function.Consumer.class))).thenReturn(clone);
    when(clone.build()).thenReturn(client);
    when(client.prompt()).thenReturn(req);
    when(req.user(any(String.class))).thenReturn(req);
    when(req.call()).thenReturn(call);
    if (responsesInOrder.length == 1) {
      when(call.content()).thenReturn(responsesInOrder[0]);
    } else {
      when(call.content())
          .thenReturn(
              responsesInOrder[0],
              java.util.Arrays.copyOfRange(responsesInOrder, 1, responsesInOrder.length));
    }
    return builder;
  }

  @Test
  void stopsOnFirstStepWhenReinvokeIsFalse() {
    String resp =
        "{\"message\":\"Done\",\"innerThoughts\":\"no more\",\"requestReinvocation\":false}";
    Agent a = new Agent(stubBuilder(resp), "x", OpenAiChatOptions.builder());
    ChatTraceResponse trace = a.userMessage(new ChatRequest("hi"));
    assertThat(trace.steps()).hasSize(1);
    assertThat(trace.steps().get(0).requestReinvocation()).isFalse();
  }

  @Test
  void stopsImmediatelyOnFallbackEvenIfModelSignalsReinvoke() {
    Agent a = new Agent(stubBuilder("sure I can help!"), "x", OpenAiChatOptions.builder());
    ChatTraceResponse trace = a.userMessage(new ChatRequest("plan a trip"));
    assertThat(trace.steps()).hasSize(1);
    assertThat(trace.steps().get(0).isFallback()).isTrue();
    assertThat(trace.steps().get(0).requestReinvocation())
        .as("D7: fallback forces reinvoke=false")
        .isFalse();
  }

  @Test
  void enforcesMaxStepCountEvenWhenModelWantsToKeepGoing() {
    String keepGoing =
        "{\"message\":\"step\",\"innerThoughts\":\"more\",\"requestReinvocation\":true}";
    Agent a =
        new Agent(
            stubBuilder(keepGoing, keepGoing, keepGoing, keepGoing, keepGoing, keepGoing),
            "x",
            OpenAiChatOptions.builder());
    ChatTraceResponse trace = a.userMessage(new ChatRequest("go"));
    assertThat(trace.steps()).hasSize(5);
  }
}
