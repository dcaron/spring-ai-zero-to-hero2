package com.example.tracing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks controller methods for distributed tracing. Creates a root span with SpanKind.SERVER for
 * HTTP requests.
 *
 * <p>Can be applied to a class (all public methods traced) or individual methods.
 *
 * @param name Optional span name (e.g., "GET /api/chat"). Defaults to ClassName.methodName.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TracedEndpoint {
  String name() default "";
}
