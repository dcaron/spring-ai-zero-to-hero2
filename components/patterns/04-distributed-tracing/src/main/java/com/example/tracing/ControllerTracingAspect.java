package com.example.tracing;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * AOP Aspect for controller tracing. Creates root spans with SpanKind.SERVER for HTTP requests.
 * Order(1) ensures this aspect executes first in the chain.
 *
 * <p>Trace hierarchy:
 *
 * <pre>
 * HTTP Request
 *   -> @TracedEndpoint: "GET /rag/01/query" (SERVER)       <- this aspect
 *       -> @TracedService: "RagService.query" (INTERNAL)
 *           -> @TracedRepository: "VectorStore.search" (CLIENT)
 * </pre>
 */
@Aspect
@Component
@Order(1)
public class ControllerTracingAspect {

  private static final Logger logger = LoggerFactory.getLogger(ControllerTracingAspect.class);

  private final Tracer tracer;

  public ControllerTracingAspect(Tracer tracer) {
    this.tracer = tracer;
  }

  @Around(
      "@within(com.example.tracing.TracedEndpoint) || "
          + "@annotation(com.example.tracing.TracedEndpoint)")
  public Object traceEndpoint(ProceedingJoinPoint joinPoint) throws Throwable {
    MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
    Method method = methodSignature.getMethod();

    TracedEndpoint annotation = method.getAnnotation(TracedEndpoint.class);
    if (annotation == null) {
      annotation = joinPoint.getTarget().getClass().getAnnotation(TracedEndpoint.class);
    }

    String spanName;
    if (annotation != null && !annotation.name().isBlank()) {
      spanName = annotation.name();
    } else {
      spanName = joinPoint.getTarget().getClass().getSimpleName() + "." + method.getName();
    }

    Span span = tracer.spanBuilder(spanName).setSpanKind(SpanKind.SERVER).startSpan();

    try (Scope scope = span.makeCurrent()) {
      logger.debug(
          "Endpoint START: {} [traceId={}, spanId={}]",
          spanName,
          span.getSpanContext().getTraceId(),
          span.getSpanContext().getSpanId());

      span.setAttribute("component", "controller");
      span.setAttribute("method", method.getName());
      span.setAttribute("class", joinPoint.getTarget().getClass().getSimpleName());

      Object result = joinPoint.proceed();

      span.setStatus(StatusCode.OK);
      logger.debug(
          "Endpoint END: {} [traceId={}, spanId={}]",
          spanName,
          span.getSpanContext().getTraceId(),
          span.getSpanContext().getSpanId());

      return result;
    } catch (Throwable ex) {
      span.setStatus(StatusCode.ERROR, ex.getMessage() != null ? ex.getMessage() : "Unknown error");
      span.recordException(ex);
      logger.error(
          "Endpoint ERROR: {} [traceId={}, spanId={}] - {}",
          spanName,
          span.getSpanContext().getTraceId(),
          span.getSpanContext().getSpanId(),
          ex.getMessage());
      throw ex;
    } finally {
      span.end();
    }
  }
}
