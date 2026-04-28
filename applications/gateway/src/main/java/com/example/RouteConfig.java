package com.example;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.filter.BodyFilterFunctions.adaptCachedBody;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

import com.example.log.AuditLogEntry;
import com.example.log.OpenAiAuditor;
import com.example.log.RequestLogEntry;
import com.example.log.ResponseLogEntry;
import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import org.springframework.cloud.gateway.server.mvc.common.MvcUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StreamUtils;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import org.springframework.web.util.UriComponentsBuilder;

@Configuration(proxyBeanMethods = false)
public class RouteConfig {

  @Bean
  public RouterFunction<ServerResponse> readBodyRoute(
      HttpServletRequest httpServletRequest, OpenAiAuditor openAiAuditor) {
    return route("gateway")
        .route(
            path("/letta/**")
                .or(path("/openai/**"))
                .or(path("/anthropic/**"))
                .or(path("/ollama/**")),
            http())
        .before(
            request -> {
              // Spring Cloud Gateway MVC's proxy only takes scheme/host/port from `uri(...)` —
              // the upstream path is the REQUEST URI's path. So we must rewrite the request URI
              // to the upstream path here (provider prefix stripped, API version prefix added),
              // and set the route URI for scheme/host/port.
              //
              // Spring AI 2.0.0-M5: the openai-java SDK treats `base-url` as the API root and
              // appends `/chat/completions` (or `/embeddings`, etc.) itself, so a call to
              // `http://localhost:7777/openai/chat/completions` must become
              // `https://api.openai.com/v1/chat/completions`.
              String requestPath = request.uri().getPath();
              String routeBase;
              String upstreamPath;
              if (requestPath.startsWith("/openai/")) {
                routeBase = "https://api.openai.com";
                upstreamPath = "/v1" + requestPath.substring("/openai".length());
              } else if (requestPath.startsWith("/letta/")) {
                routeBase = "https://api.openai.com";
                upstreamPath = "/v1" + requestPath.substring("/letta".length());
              } else if (requestPath.startsWith("/anthropic/")) {
                routeBase = "https://api.anthropic.com";
                upstreamPath = requestPath.substring("/anthropic".length());
              } else if (requestPath.startsWith("/ollama/")) {
                routeBase = "http://localhost:11434";
                upstreamPath = requestPath.substring("/ollama".length());
              } else {
                return request;
              }
              URI rewrittenUri =
                  UriComponentsBuilder.fromUri(request.uri())
                      .replacePath(upstreamPath)
                      .build(true)
                      .toUri();
              ServerRequest pathRewritten = ServerRequest.from(request).uri(rewrittenUri).build();
              return uri(routeBase).apply(pathRewritten);
            })
        .before(
            request -> {
              var requestLogEntry = new RequestLogEntry();

              // log the body
              Optional<String> body = MvcUtils.cacheAndReadBody(request, String.class);
              requestLogEntry.setBody(body.orElse(""));

              // log the headers
              Map<String, String> headers = request.headers().asHttpHeaders().toSingleValueMap();
              requestLogEntry.setHeaders(headers);

              // log the http method and incoming request uri
              requestLogEntry.setMethod(request.method().name());
              requestLogEntry.setOriginalUri(request.uri().toString());

              // put audit log entry in the request
              var auditLogEntry = new AuditLogEntry();
              auditLogEntry.setRequest(requestLogEntry);
              MvcUtils.putAttribute(request, AuditLogEntry.AUDIT_LOG_ENTRY, auditLogEntry);

              return request;
            })
        .before(adaptCachedBody()) // make the body readable for next step
        .after(
            (request, response) -> {
              var responseLogEntry = new ResponseLogEntry();

              // log the response headers
              Map<String, String> headers = response.headers().toSingleValueMap();
              responseLogEntry.setHeaders(headers);

              // log the response body
              Object o = request.attributes().get(MvcUtils.CLIENT_RESPONSE_INPUT_STREAM_ATTR);
              if (o instanceof InputStream) {
                try {
                  byte[] bytes = StreamUtils.copyToByteArray((InputStream) o);
                  String body = new String(bytes, StandardCharsets.UTF_8);
                  responseLogEntry.setBody(body);
                  ByteArrayInputStream bais = new ByteArrayInputStream(body.getBytes());
                  request.attributes().put(MvcUtils.CLIENT_RESPONSE_INPUT_STREAM_ATTR, bais);
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              }

              // add the response to audit entry
              AuditLogEntry auditLogEntry =
                  MvcUtils.getAttribute(request, AuditLogEntry.AUDIT_LOG_ENTRY);
              auditLogEntry.setResponse(responseLogEntry);

              // log the uri where the request was sent to
              URI uri = MvcUtils.getAttribute(request, MvcUtils.GATEWAY_REQUEST_URL_ATTR);
              auditLogEntry.getRequest().setDestinationUri(uri.toString());

              openAiAuditor.log(auditLogEntry);

              return response;
            })
        .build();
  }
}
