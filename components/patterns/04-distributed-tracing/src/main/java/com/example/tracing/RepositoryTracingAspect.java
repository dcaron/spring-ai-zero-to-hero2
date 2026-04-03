package com.example.tracing;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
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
 * AOP Aspect for repository tracing. Creates child spans with SpanKind.CLIENT for database and
 * external service access. Order(3) ensures this aspect executes after service tracing, creating
 * the deepest level of the span hierarchy.
 */
@Aspect
@Component
@Order(3)
public class RepositoryTracingAspect {

  private static final Logger logger = LoggerFactory.getLogger(RepositoryTracingAspect.class);

  private final Tracer tracer;

  public RepositoryTracingAspect(Tracer tracer) {
    this.tracer = tracer;
  }

  @Around(
      "@within(com.example.tracing.TracedRepository) || "
          + "@annotation(com.example.tracing.TracedRepository)")
  public Object traceRepository(ProceedingJoinPoint joinPoint) throws Throwable {
    MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
    Method method = methodSignature.getMethod();

    TracedRepository annotation = method.getAnnotation(TracedRepository.class);
    if (annotation == null) {
      annotation = joinPoint.getTarget().getClass().getAnnotation(TracedRepository.class);
    }

    String spanName = joinPoint.getTarget().getClass().getSimpleName() + "." + method.getName();
    String operation =
        (annotation != null && !annotation.operation().isBlank())
            ? annotation.operation()
            : "QUERY";

    Span span =
        tracer
            .spanBuilder(spanName)
            .setSpanKind(SpanKind.CLIENT)
            .setParent(Context.current())
            .startSpan();

    try (Scope scope = span.makeCurrent()) {
      logger.debug(
          "Repository START: {} [traceId={}, spanId={}, operation={}]",
          spanName,
          span.getSpanContext().getTraceId(),
          span.getSpanContext().getSpanId(),
          operation);

      span.setAttribute("component", "repository");
      span.setAttribute("db.operation", operation);
      span.setAttribute("db.type", "sql");
      span.setAttribute("method", method.getName());
      span.setAttribute("class", joinPoint.getTarget().getClass().getSimpleName());

      Object result = joinPoint.proceed();

      span.setStatus(StatusCode.OK);
      logger.debug(
          "Repository END: {} [traceId={}, spanId={}, operation={}]",
          spanName,
          span.getSpanContext().getTraceId(),
          span.getSpanContext().getSpanId(),
          operation);

      return result;
    } catch (Throwable ex) {
      span.setStatus(StatusCode.ERROR, ex.getMessage() != null ? ex.getMessage() : "Unknown error");
      span.recordException(ex);
      logger.error(
          "Repository ERROR: {} [traceId={}, spanId={}, operation={}] - {}",
          spanName,
          span.getSpanContext().getTraceId(),
          span.getSpanContext().getSpanId(),
          operation,
          ex.getMessage());
      throw ex;
    } finally {
      span.end();
    }
  }
}
