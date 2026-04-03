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
 * AOP Aspect for service tracing. Creates child spans with SpanKind.INTERNAL for business logic.
 * Order(2) ensures this aspect executes after controller tracing, creating proper parent-child span
 * relationships.
 */
@Aspect
@Component
@Order(2)
public class ServiceTracingAspect {

  private static final Logger logger = LoggerFactory.getLogger(ServiceTracingAspect.class);

  private final Tracer tracer;

  public ServiceTracingAspect(Tracer tracer) {
    this.tracer = tracer;
  }

  @Around(
      "@within(com.example.tracing.TracedService) || "
          + "@annotation(com.example.tracing.TracedService)")
  public Object traceService(ProceedingJoinPoint joinPoint) throws Throwable {
    MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
    Method method = methodSignature.getMethod();

    TracedService annotation = method.getAnnotation(TracedService.class);
    if (annotation == null) {
      annotation = joinPoint.getTarget().getClass().getAnnotation(TracedService.class);
    }

    String spanName = joinPoint.getTarget().getClass().getSimpleName() + "." + method.getName();
    String moduleName =
        (annotation != null && !annotation.module().isBlank()) ? annotation.module() : "unknown";

    Span span =
        tracer
            .spanBuilder(spanName)
            .setSpanKind(SpanKind.INTERNAL)
            .setParent(Context.current())
            .startSpan();

    try (Scope scope = span.makeCurrent()) {
      logger.debug(
          "Service START: {} [traceId={}, spanId={}, module={}]",
          spanName,
          span.getSpanContext().getTraceId(),
          span.getSpanContext().getSpanId(),
          moduleName);

      span.setAttribute("component", "service");
      span.setAttribute("module", moduleName);
      span.setAttribute("method", method.getName());
      span.setAttribute("class", joinPoint.getTarget().getClass().getSimpleName());

      Object result = joinPoint.proceed();

      span.setStatus(StatusCode.OK);
      logger.debug(
          "Service END: {} [traceId={}, spanId={}, module={}]",
          spanName,
          span.getSpanContext().getTraceId(),
          span.getSpanContext().getSpanId(),
          moduleName);

      return result;
    } catch (Throwable ex) {
      span.setStatus(StatusCode.ERROR, ex.getMessage() != null ? ex.getMessage() : "Unknown error");
      span.recordException(ex);
      logger.error(
          "Service ERROR: {} [traceId={}, spanId={}, module={}] - {}",
          spanName,
          span.getSpanContext().getTraceId(),
          span.getSpanContext().getSpanId(),
          moduleName,
          ex.getMessage());
      throw ex;
    } finally {
      span.end();
    }
  }
}
