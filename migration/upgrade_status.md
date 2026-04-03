# Upgrade Status — Findings & Deviations from Plan

This document tracks all migration steps performed, including discoveries not covered in the original upgrade plan.

---

## Phase 1: Root POM & Build Tooling - DONE

**Planned changes applied as expected:**
- `spring-boot-starter-parent`: 3.5.6 → 4.0.5
- `java.version`: 21 → 25
- `spring-ai.version`: 1.0.3 → 2.0.0-M4
- Maven wrapper: 3.9.9 → 3.9.14
- Removed `loki-logback-appender` from dependencyManagement

**Unplanned findings:**
- `spring-cloud.version`: 2025.1.2 does NOT exist. Correct version: **2025.1.1**
- `spring-shell.version`: 4.0.2 does NOT exist. Correct version: **4.0.1**
- `spring-cloud-azure.version`: Updated from 6.0.0 → **7.1.0** (major version jump for Boot 4 compat)

---

## Phase 2: Flyway - DONE

Applied as planned. `flyway-core` → `spring-boot-starter-flyway` in:
- `applications/provider-openai/pom.xml`
- `applications/provider-ollama/pom.xml`

No unplanned findings.

---

## Phase 3: MCP SSE → Streamable HTTP - DONE

**Applied as planned:**
- `mcp/02-basic-http-mcp-server/pom.xml`: webflux → webmvc
- `mcp/04-dynamic-tool-calling/server/pom.xml`: webflux → webmvc
- `mcp/04-dynamic-tool-calling/client/application.yaml`: `sse:` → `http:`
- Test clients: `WebFluxSseClientTransport` / `HttpClientSseClientTransport` → `HttpClientStreamableHttpTransport`

**Unplanned findings:**
- `mcp/04-dynamic-tool-calling/client/pom.xml` had `<java.version>17</java.version>` override — updated to 25
- The Streamable HTTP transport class is `HttpClientStreamableHttpTransport` (not documented in upgrade notes)

---

## Phase 4: Spring AI 2.0.0-M4 API Changes - DONE

### MCP Annotations Migration

**Planned:**
- Removed `com.logaritex.mcp:spring-ai-mcp-annotations:0.1.0`
- Updated imports to `org.springframework.ai.mcp.annotation.*`

**Unplanned findings:**
- `SpringAiMcpAnnotationProvider` does NOT exist in Spring AI 2.0. The replacement is `SyncMcpAnnotationProviders` (note: plural, different package)
- New import: `org.springframework.ai.mcp.annotation.spring.SyncMcpAnnotationProviders`
- Method names changed:
  - `createSyncResourceSpecifications()` → `resourceSpecifications()`
  - `createSyncPromptSpecifications()` → `promptSpecifications()`
  - `createSyncCompleteSpecifications()` → `completeSpecifications()`
- Had to add `spring-ai-mcp-annotations` as explicit dependency (it's a Spring AI artifact now, managed by BOM)

### MCP Client Customizer

**Unplanned finding:**
- `McpSyncClientCustomizer` renamed to `McpClientCustomizer<B>` (generic, in same package `org.springframework.ai.mcp.customizer`)
- The `toolsChangeConsumer()` method on the spec was removed/changed

### Google Provider

**Applied as planned:**
- Starter renamed: `spring-ai-starter-model-vertex-ai-gemini` → `spring-ai-starter-model-google-genai`

### Spring Cloud Gateway 5.0

**Major unplanned finding — artifact renamed:**
- `spring-cloud-starter-gateway-mvc` → `spring-cloud-starter-gateway-server-webmvc`
- `HandlerFunctions.http(String uri)` removed — now `http()` takes no args
- URI must be set via `BeforeFilterFunctions.uri("...")` filter
- Required new import: `org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri`

### Jackson 3.0 (Major Unplanned Discovery)

**The plan assumed Jackson 3 would be transparent. It was NOT.**

Spring Boot 4 ships Jackson 3 (`tools.jackson:jackson-databind:3.1.0`) as default via `spring-boot-starter-jackson`. Jackson 2 annotations (`com.fasterxml.jackson.annotation`) are still available for backward compatibility.

**Key migration rules discovered:**
- `com.fasterxml.jackson.databind.ObjectMapper` → `tools.jackson.databind.ObjectMapper`
- `com.fasterxml.jackson.databind.ObjectWriter` → `tools.jackson.databind.ObjectWriter`
- `com.fasterxml.jackson.core.JsonProcessingException` → `tools.jackson.core.JacksonException`
- `com.fasterxml.jackson.annotation.*` (@JsonProperty, @JsonIgnore, etc.) → **NO CHANGE** (kept as `com.fasterxml.jackson.annotation`)
- `JacksonException` is now a `RuntimeException` (unchecked), not checked like `JsonProcessingException`

**Files affected (not in original plan):**
- `components/data/src/main/java/com/example/data/DataFiles.java`
- `components/patterns/02-retrieval-augmented-generation/src/main/java/com/example/JsonReader2.java`
- `components/patterns/02-retrieval-augmented-generation/src/main/java/com/example/rag/bikes/BikesController.java`
- `applications/gateway/src/main/java/com/example/log/OpenAiAuditor.java`
- `agentic-system/01-inner-monologue/inner-monologue-cli/src/main/java/com/example/JsonUtils.java`
- `agentic-system/02-model-directed-loop/model-directed-loop-cli/src/main/java/com/example/JsonUtils.java`

### Spring Boot 4 AutoConfiguration Package Moves

**Unplanned finding:**
- `org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration` → `org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration`
- `org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration` → `org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration`
- Fixed by using `excludeName` (string-based) instead of class references in `@EnableAutoConfiguration`

**Files affected:**
- `components/config-pgvector/src/main/java/com/example/SimpleVectorStoreConfig.java`
- `applications/provider-openai/src/main/java/com/example/SimpleVectorStoreConfig.java` (duplicate copy!)

### Spring Shell 4.0 (Major Unplanned Discovery)

**The plan assumed Spring Shell 4 would be a minor change. It was NOT.**

Key breaking changes:
- `@CommandScan` removed → replaced by `@EnableCommand`
- `@Command(command = "name")` → `@Command(name = "name")` (attribute renamed)
- `@Option(longNames = "id")` → `@Option(longName = "id")` (singular)
- `@Command` can NO LONGER be applied to classes (only methods)
- `PromptProvider` interface (`org.springframework.shell.jline.PromptProvider`) removed entirely
- `org.jline.terminal.Terminal` and `org.jline.utils.*` — jline classes moved/removed from Shell API
- Package change: `org.springframework.shell.command.annotation.*` → `org.springframework.shell.core.command.annotation.*`

**Resolution:** Removed PromptProvider implementations and jline Terminal dependency from CLI modules. Custom prompt coloring lost (acceptable trade-off).

**Files affected (not in original plan):**
- `agentic-system/01-inner-monologue/inner-monologue-cli/src/main/java/com/example/InnerMonologueCliApplication.java`
- `agentic-system/01-inner-monologue/inner-monologue-cli/src/main/java/com/example/command/agent/AgentCommands.java`
- `agentic-system/01-inner-monologue/inner-monologue-cli/src/main/java/com/example/command/agent/AgentContext.java`
- `agentic-system/02-model-directed-loop/model-directed-loop-cli/src/main/java/com/example/ModelDirectedLoopCliApplication.java`
- `agentic-system/02-model-directed-loop/model-directed-loop-cli/src/main/java/com/example/command/agent/AgentCommands.java`
- `agentic-system/02-model-directed-loop/model-directed-loop-cli/src/main/java/com/example/command/agent/AgentContext.java`

---

## Phase 5: Docker Infrastructure - DONE

**Applied as planned:**
- `docker/postgres/docker-compose.yaml`: pgvector `pg17` → `pg18`, pgAdmin `9.8.0` → `latest`
- `docker/observability-stack/docker-compose.yaml`: Replaced 6-container stack with LGTM all-in-one

**LGTM setup:**
- Single `grafana/otel-lgtm:latest` container replaces: Loki, Tempo, OTel Collector, Prometheus, Grafana
- MailDev kept as separate container
- Dashboard JSONs moved to `grafana/dashboards/`
- Created `grafana/provisioning/dashboards.yaml` (workshop dashboard provider)
- Created `grafana/provisioning/disable-default-dashboards.yaml` (suppress LGTM defaults)

**Obsolete files to clean up later:**
- `docker/observability-stack/config/` (otel-collector.yaml, prometheus.yaml, tempo.yaml)
- `docker/observability-stack/grafana/grafana.ini`
- `docker/observability-stack/grafana/provisioning/datasources/`
- `docker/observability-stack/grafana/provisioning/dashboards/dashboard.yml`
- `docker/observability-stack/grafana/provisioning/alerting/`

---

## Phase 6: Observability - Brave/Zipkin to OpenTelemetry - DONE

**provider-openai changes:**
- Removed: `micrometer-registry-prometheus`, `micrometer-tracing-bridge-brave`, `zipkin-reporter-brave`
- Added: `spring-boot-starter-opentelemetry`, `opentelemetry-logback-appender-1.0:2.24.0-alpha`
- YAML: Replaced `management.zipkin.tracing.*` with `management.opentelemetry.*` + `management.otlp.*`
- YAML: `management.tracing.enabled` → `management.tracing.export.enabled`
- Profile renamed: `observe` → `observation` (consistent with provider-ollama)

**provider-ollama changes:**
- YAML: Replaced `management.zipkin.tracing.*` with OTel OTLP endpoints
- Removed stale datasource config from observation profile

---

## Phase 7: Distributed Tracing Pattern - DONE

Created new module: `components/patterns/04-distributed-tracing/`

**Files created:**
- `pom.xml` — dependencies: spring-boot-starter-opentelemetry, opentelemetry-logback-appender, spring-aop, aspectjweaver
- `TracedEndpoint.java` — annotation for controllers (SpanKind.SERVER)
- `TracedService.java` — annotation for services (SpanKind.INTERNAL)
- `TracedRepository.java` — annotation for repositories (SpanKind.CLIENT)
- `ControllerTracingAspect.java` — AOP aspect @Order(1), creates root spans
- `ServiceTracingAspect.java` — AOP aspect @Order(2), creates child spans with module attribute
- `RepositoryTracingAspect.java` — AOP aspect @Order(3), creates DB client spans with operation attribute
- `OpenTelemetryConfig.java` — Tracer bean, ObservedAspect, Logback OTel appender installation

**Wired into provider apps:**
- `applications/provider-ollama/pom.xml` — added 04-distributed-tracing dependency
- `applications/provider-openai/pom.xml` — added 04-distributed-tracing dependency

**Trace hierarchy:**
```
HTTP Request
  -> @TracedEndpoint: "GET /rag/01/query" (SERVER, Order 1)
      -> @TracedService: "RagService.query" (INTERNAL, Order 2)
          -> @TracedRepository: "VectorStore.similaritySearch" (CLIENT, Order 3)
              -> Database query (OTel auto-instrumentation)
```

**Usage pattern (Java):**
```java
@RestController
@TracedEndpoint
public class MyController {

  @TracedEndpoint(name = "GET /api/chat")
  @GetMapping("/api/chat")
  public String chat() { ... }
}

@Service
@TracedService(module = "chat")
public class MyChatService { ... }

@TracedRepository(operation = "similaritySearch")
public List<Document> search(String query) { ... }
```

---

## Compilation Status

**Full project compiles successfully** with:
- Spring Boot 4.0.5
- Spring AI 2.0.0-M4
- Java 25
- Maven 3.9.14
- Spring Cloud 2025.1.1
- Spring Cloud Azure 7.1.0
- Spring Shell 4.0.1

```
./mvnw compile -T 4  →  BUILD SUCCESS
./mvnw install -T 4 -DskipTests  →  BUILD SUCCESS
```

---

## Findings During Testing

### Finding T1: StdioClientTransport constructor changed (MCP SDK 1.1)
**File:** `mcp/01-basic-stdio-mcp-server/src/test/java/com/example/ClientStdio.java`
**Issue:** `StdioClientTransport(ServerParameters)` now requires a second `McpJsonMapper` parameter.
**Fix:** `new StdioClientTransport(stdioParams, new JacksonMcpJsonMapper(JsonMapper.builder().build()))`
**Imports added:** `io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper`, `tools.jackson.databind.json.JsonMapper`

### Finding T2: Ollama model limitations with structured output and tool calling
**Endpoints:** `/chat/04/plays/map` (500), `/chat/04/plays/object` (500), `/chat/05/search` (500), `/chat/05/weather` (returns raw JSON)
**Root Cause:** llama3.2 (3B) cannot reliably produce structured Map/POJO output. Tool calling with `toolNames("weatherFunction")` works with OpenAI-specific config but the function is not auto-registered for Ollama provider. These are **model/provider capability limitations**, not migration bugs.
**Action Items:**
- Document in flow.md which demos require specific providers/model capabilities
- Consider adding a larger model (e.g., llama3.1:8b) for tool calling demos
- The `weatherFunction` bean registration only works when the provider config lists it under `spring.ai.*.chat.options.functions`

### Finding T3: Vector store/RAG/large embedding batch operations fail with Ollama
**Endpoints:** `/vector/01/load` (500), `/rag/01/load` (500), `/embed/03/big` (400), `/embed/03/chunk` (500), `/embed/04/text/works` (500), `/embed/04/pdf/*` (404)
**Root Cause:** Ollama's `mxbai-embed-large` model has context length limits. Batch embedding of 25 bike documents or large Shakespeare text exceeds the embedding model's input length. Single short text embedding works fine.
**Working:** `/embed/01/text` (single text), `/embed/01/dimension`, `/embed/02/words`, `/embed/02/quotes`, `/embed/04/json/bikes` (parsed into smaller docs)
**Action Items:**
- Add chunking/batching before vector store insertion for Ollama
- Or increase Ollama context window via `num_ctx` parameter
- Document that these demos work best with OpenAI's embedding models which have larger context

### Finding T4: embed_04/pdf endpoints return 404
**Endpoints:** `/embed/04/pdf/page`, `/embed/04/pdf/paragraph`
**Root Cause:** These endpoint paths may not be registered — need to check if the PDF document reader controller uses different URL paths.

### Finding T5: chat_04/pojo returns 404
**Endpoint:** `/chat/04/plays/pojo`
**Root Cause:** Endpoint doesn't exist — the actual path is `/chat/04/plays/object`. Documentation in flow.md had wrong URL.
**Fix:** Updated test plan to use correct URL `/chat/04/plays/object`

### Finding T6: OTel Logback appender logs not reaching Loki
**Issue:** OpenTelemetryAppender.install() runs successfully, but no log streams appear in Loki
**Root Cause:** Missing `logback-spring.xml` with `OpenTelemetryAppender` configured. The `install()` call sets up the SDK, but Logback needs to be configured to use it as an appender.
**Fix needed:** Create `src/main/resources/logback-spring.xml` in provider apps with OTel appender config

### Finding T7: Micrometer 1.16 metric name changes
**Issue:** Dashboard queries for `http_server_requests_seconds_count` return empty
**Root Cause:** Micrometer 1.16 changed metric naming: `_seconds_*` → `_milliseconds_*`
**Impact:** Existing Grafana dashboards may show "No data" for HTTP request panels
**Fix needed:** Update dashboard JSON queries or add recording rules for backward compat

### Finding T8: Chain-of-thought/Self-reflection endpoints return 500 — FIXED
**Endpoints:** `/cot/bio/oneshot`, `/reflection/bio/oneshot`
**Root Cause:** Missing `Profile.pdf` file (gitignored). Both modules require `classpath:/info/Profile.pdf`.
**Fix:** Created sample Profile.pdf in both modules' `src/main/resources/info/` directories.

### Fix F1: Switched embedding model mxbai-embed-large → nomic-embed-text
**Root Cause:** `mxbai-embed-large` has only 512 token context — bike documents are 800-1500 tokens each.
**Solution:** Switched to `nomic-embed-text` (8192 context, 768 dims, 274MB — fits 16GB macOS easily).
**Changes:**
- `applications/provider-ollama/src/main/resources/application.yaml`: model and pgvector dimension (1024→768)

### Fix F2: Added TokenTextSplitter chunking to vector store and RAG loaders
**Root Cause:** Even with 8192 context, batch embedding of 25 concatenated bike docs could hit limits.
**Solution:** Added `TokenTextSplitter` before `vectorStore.add()` in:
- `components/apis/vector-store/src/main/java/com/example/vector_01/VectorStoreController.java`
- `components/patterns/02-retrieval-augmented-generation/src/main/java/com/example/rag_01/RagController.java`
- `components/patterns/02-retrieval-augmented-generation/src/main/java/com/example/rag_02/AdvisorController.java`
**Result:** 25 docs → 72 chunks, all load and query successfully.

### Fix F3: Created logback-spring.xml for OTel log export — VERIFIED
**Files:** `applications/provider-ollama/src/main/resources/logback-spring.xml`, `applications/provider-openai/src/main/resources/logback-spring.xml`
**Result:** Loki now receives logs with `service_name=ollama-provider`. Console logs include `[traceId,spanId]`.

### Fix F4: Created sample Profile.pdf for CoT/Reflection — VERIFIED
**Files:** `components/patterns/chain-of-thought/src/main/resources/info/Profile.pdf`, `components/patterns/self-reflection-agent/src/main/resources/info/Profile.pdf`
**Result:** All CoT and reflection endpoints now return 200 with generated bios.

### Fix F5: Removed deprecated `functions` config from OpenAI provider — VERIFIED
**File:** `applications/provider-openai/src/main/resources/application.yaml`
**Issue:** `spring.ai.openai.chat.options.functions: [weatherFunction]` is a deprecated Spring AI 1.0 property. In Spring AI 2.0, it causes all ChatClient-based endpoints to fail with 500 (the function bean can't be resolved via this config path).
**Fix:** Removed `functions` and `n` from default chat options. Tools are now registered inline via `.tools()` or `.toolNames()` in the controller code.
**Result:** All ChatClient endpoints (chat_02/client, chat_07, etc.) now work with OpenAI.

### Fix F6: MCP 05 bean name conflict with Spring AI 2.0 auto-configuration
**File:** `mcp/05-mcp-capabilities/src/main/java/mcp/capabilities/McpServerApplication.java`
**Issue:** Spring AI 2.0 auto-configuration creates beans named `resourceSpecs`, `promptSpecs`, `completionSpecs` — conflicting with our manual beans.
**Fix:** Renamed manual beans to `customResourceSpecs`, `customPromptSpecs`, `customCompletionSpecs`.
**Result:** MCP 05 starts with 2 tools, 9 resource templates, 12 prompts, 6 completions.

---

## Provider Integration Test Findings (2026-04-03)

### Finding P1: Anthropic — spy profile hardcoded active
**Provider:** Anthropic (Claude, direct API)
**Issue:** Default `spy` profile was hardcoded active in provider-anthropic application config, causing requests to be routed through the gateway even when not intended.
**Fix:** Commented out the hardcoded `spy` profile activation.
**Result:** All 14 chat endpoints pass including structured output, tool calling, and streaming.

### Finding P2: AWS Bedrock — Anthropic model access requires form submission
**Provider:** AWS Bedrock (eu-central-1 / Frankfurt)
**Issue:** Anthropic models on Bedrock require a use case form submission before they can be used. Amazon Nova models (e.g., Nova Lite) work instantly without additional approval.
**Model used:** Amazon Nova Lite
**Additional issue:** Bedrock Converse starter (`spring-ai-starter-model-bedrock-converse`) does not auto-configure `EmbeddingModel`. The embedding module was excluded from the build to avoid startup errors.
**Result:** 8/8 PASS — 7 chat endpoints + 1 stuff-the-prompt endpoint.

### Finding P3: Google GenAI — protobuf and okio dependency conflicts
**Provider:** Google (Gemini 2.5 Flash)
**Status:** RESOLVED — dependency conflicts fixed, 13/13 PASS with gemini-2.5-flash
**Dependency conflict 1 (fixed):** protobuf 3.25.5 (pulled by Google AI SDK) vs Spring Boot 4's protobuf 4.32.0. Resolved with explicit `<protobuf.version>` override in pom.xml.
**Dependency conflict 2 (FIXED):** okio excluded from Google AI SDK, forced okhttp-jvm 5.2.1 + okio-jvm 3.16.1.
**Additional finding:** Embedding auto-config requires a separate api-key property under `spring.ai.google.genai.embedding.*` (not shared with the chat api-key).
**Expected resolution:** Spring AI M5 or RC1, when the Google GenAI SDK is updated for okhttp 5.x compatibility.

### Finding P4: Azure OpenAI — deprecated models, Standard SKU, functions config
**Provider:** Azure OpenAI (East US)
**Model used:** gpt-4.1-mini (gpt-4o-mini v2024-07-18 deprecated since 03/31/2026)
**Issue 1:** gpt-4o-mini and gpt-35-turbo deprecated on Azure — must use gpt-4.1-mini or newer
**Issue 2:** GlobalStandard SKU has 0 quota on free tier — must use Standard SKU
**Issue 3:** Deprecated `functions` config and azure vector store config removed from application.yaml
**Setup:** Created via `az cognitiveservices account create` (East US, S0) + `az cognitiveservices account deployment create` (Standard SKU, capacity 1)
**Result:** 8/8 PASS — all chat endpoints work (slow due to 1 TPM rate limit but functional)
