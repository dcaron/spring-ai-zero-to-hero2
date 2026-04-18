package com.example.agentic.model_directed_loop;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AgentFallbackHandlerTest {

  AgentFallbackHandler handler = new AgentFallbackHandler();

  @Test
  void returnsParsedResponseForValidJson() {
    String json =
        "{\"message\":\"Hi\",\"innerThoughts\":\"Greeting\",\"requestReinvocation\":true}";
    ChatResponse r = handler.parseOrFallback(json);
    assertThat(r.message()).isEqualTo("Hi");
    assertThat(r.isFallback()).isFalse();
    assertThat(r.requestReinvocation()).isTrue();
  }

  @Test
  void returnsFallbackForFreeText() {
    ChatResponse r = handler.parseOrFallback("I'd love to plan a trip.");
    assertThat(r.isFallback()).isTrue();
    assertThat(r.innerThoughts()).startsWith("[fallback:");
    assertThat(r.requestReinvocation()).as("Fallback must force loop termination").isFalse();
  }

  @Test
  void returnsFallbackForMalformedJson() {
    ChatResponse r = handler.parseOrFallback("{ incomplete");
    assertThat(r.isFallback()).isTrue();
    assertThat(r.requestReinvocation()).isFalse();
  }

  @Test
  void returnsFallbackWhenJsonMissingRequiredFields() {
    ChatResponse r = handler.parseOrFallback("{\"unrelated\": \"field\"}");
    assertThat(r.isFallback()).isTrue();
    assertThat(r.requestReinvocation()).isFalse();
  }
}
