package com.example.agentic.model_directed_loop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

// scanBasePackages=com.example so beans outside the agent's own package are registered too:
//   - com.example.fitness.AcmeFitnessController (ACME login endpoint, from the data module)
//   - com.example.tracing.SpanLoggingObservationHandler (AI-call logging, 04-distributed-tracing)
@SpringBootApplication
@ComponentScan(basePackages = "com.example")
public class ModelDirectedLoopAgentApplication {

  public static void main(String[] args) {
    SpringApplication.run(ModelDirectedLoopAgentApplication.class, args);
  }
}
