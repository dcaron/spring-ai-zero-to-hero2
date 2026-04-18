package com.example.agentic.inner_monologue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

// scanBasePackages=com.example so beans outside the agent's own package are registered too —
// in particular com.example.tracing.SpanLoggingObservationHandler from 04-distributed-tracing.
@SpringBootApplication
@ComponentScan(basePackages = "com.example")
public class InnerMonologueAgentApplication {

  public static void main(String[] args) {
    SpringApplication.run(InnerMonologueAgentApplication.class, args);
  }
}
