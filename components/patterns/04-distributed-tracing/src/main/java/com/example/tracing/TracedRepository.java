package com.example.tracing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks repository methods for distributed tracing. Creates a child span with SpanKind.CLIENT for
 * database or external service access.
 *
 * <p>Can be applied to a class (all public methods traced) or individual methods.
 *
 * @param operation Operation name for span attribute (e.g., "SELECT", "INSERT",
 *     "similaritySearch").
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TracedRepository {
  String operation() default "";
}
