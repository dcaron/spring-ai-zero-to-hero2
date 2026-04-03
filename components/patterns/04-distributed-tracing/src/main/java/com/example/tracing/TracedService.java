package com.example.tracing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks service methods for distributed tracing. Creates a child span with SpanKind.INTERNAL for
 * business logic.
 *
 * <p>Can be applied to a class (all public methods traced) or individual methods.
 *
 * @param module Module name for span attribute (e.g., "chat", "embedding", "rag").
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TracedService {
  String module() default "";
}
