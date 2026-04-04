package com.example.tracing;

import io.micrometer.observation.ObservationPredicate;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.stereotype.Component;

/**
 * OpenTelemetry configuration for tracing and logging.
 *
 * <p>Provides:
 *
 * <ul>
 *   <li>A {@link Tracer} bean for the custom tracing aspects
 *   <li>An {@link ObservedAspect} for Micrometer's @Observed annotation
 *   <li>Logback appender installation for OTLP log export
 * </ul>
 */
@Configuration
public class OpenTelemetryConfig {

  private static final Logger logger = LoggerFactory.getLogger(OpenTelemetryConfig.class);

  /**
   * Creates a Tracer for the manual TracingAspects. The Tracer is obtained from the global
   * OpenTelemetry SDK provided by Spring Boot 4.x.
   */
  @Bean
  public Tracer tracer(OpenTelemetry openTelemetry) {
    logger.info("OpenTelemetry Tracer created for spring-ai-workshop");
    return openTelemetry.getTracer("spring-ai-workshop", "1.0.0");
  }

  /**
   * Suppress the generic HTTP server observation (http get /**) so @TracedEndpoint spans are the
   * root spans in Tempo. Also excludes non-API paths (swagger, static resources).
   */
  @Bean
  public ObservationPredicate suppressHttpServerObservation() {
    return (name, context) -> {
      if ("http.server.requests".equals(name)
          && context instanceof ServerRequestObservationContext) {
        return false;
      }
      return true;
    };
  }

  /**
   * Registers the ObservedAspect for AOP-based processing of the @Observed annotation (Micrometer).
   */
  @Bean
  public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
    logger.info("ObservedAspect for @Observed annotation activated");
    return new ObservedAspect(observationRegistry);
  }

  /**
   * Installs the OpenTelemetry Logback Appender after the Spring Context is initialized. This is
   * required because the OpenTelemetry SDK must be fully configured before the Logback Appender can
   * use it.
   */
  @Component
  static class InstallOpenTelemetryAppender implements InitializingBean {

    private static final Logger logger =
        LoggerFactory.getLogger(InstallOpenTelemetryAppender.class);

    private final OpenTelemetry openTelemetry;

    InstallOpenTelemetryAppender(OpenTelemetry openTelemetry) {
      this.openTelemetry = openTelemetry;
    }

    @Override
    public void afterPropertiesSet() {
      OpenTelemetryAppender.install(openTelemetry);
      logger.info("OpenTelemetry Logback Appender installed");
    }
  }
}
