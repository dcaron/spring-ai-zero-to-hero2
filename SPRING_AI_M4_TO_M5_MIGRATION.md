# Spring AI 2.0.0-M4 → 2.0.0-M5 Migration

**Workshop release:** 2.3.4 (2026-04-28)
**Stack:** Spring Boot 4.0.6 · Spring AI 2.0.0-M5 · Java 25 · Maven 3.9.14
**Reference release notes:** <https://github.com/spring-projects/spring-ai/releases/tag/v2.0.0-M5>

This document is the post-mortem of bumping the workshop from Spring AI 2.0.0-M4 to 2.0.0-M5. It records (a) every Spring AI breaking change that touched our code, (b) the surrounding bugs that surfaced *because of* the bump even though they're not strictly Spring-AI changes (gateway URL handling, missing deps, missing config), and (c) a per-provider checklist. The goal is that the next time someone in this repo bumps a Spring AI milestone, they don't have to rediscover any of this.

> Companion to `migration/upgrade.md` (which covers the earlier Spring Boot 3.5.x + Spring AI 1.x → Spring Boot 4.0.x + Spring AI 2.0.0-M4 migration). This file is the same shape, one milestone later.

---

## TL;DR

| Layer | Change | Action |
|---|---|---|
| Maven | `spring-ai.version` 2.0.0-M4 → 2.0.0-M5 in root `pom.xml` | one-line bump |
| Maven | `spring-ai-azure-openai` removed upstream | swap `provider-azure` to `spring-ai-starter-model-openai` |
| Maven | `org.springframework.ai.openai.api.*` package gone (openai-java SDK now used directly) | update one import in `TranscribeController` |
| Maven | `provider-openai` & `provider-ollama` need `spring-boot-restclient` | add dep (latent — only surfaces when `ui` profile is on) |
| Maven | `provider-azure` needs Flyway + `db/migration/V1__test.sql` | add deps + migration script |
| Spring AI API | `ChatClient.Builder.defaultOptions(...)` and `ChatClientRequestSpec.options(...)` now take **`ChatOptions.Builder`** instead of a built `ChatOptions` | adapt 1 controller, 2 agent classes, 4 config beans, 6 tests |
| Spring AI / Azure | Azure OpenAI is now reached through the unified `spring-ai-openai` module in **"Microsoft Foundry"** mode | rewrite `application.yaml`, `creds-template.yaml`, `DebugController`, readme |
| Gateway | `openai-java` SDK constructs request paths itself; the gateway can no longer hardcode the upstream path | rewrite `RouteConfig` to do explicit per-provider URI + path rewriting |
| Provider apps | `spring.application.name` was missing in `provider-azure` / `-aws` / `-google` (latent — surfaced as "unknown" badge in the dashboard) | add it |
| Provider apps | `provider-azure` `pgvector` profile block was empty | copy the OpenAI block (HNSW/COSINE_DISTANCE/1536) |
| Docs / UI | Version label sweep `2.0.0-M4` → `2.0.0-M5` everywhere except history files | mechanical |
| Workshop | bumped 2.3.3 → 2.3.4 (`VERSION`, `workshop.properties`, `layout.html` placeholder) | mechanical |

Total reactor stays green: `./mvnw clean verify` — 29 modules, all tests pass.

---

## Part 1 — Spring AI 2.0.0-M5 breaking changes that touched us

### 1.1 `spring-ai-azure-openai` module entirely removed

Azure OpenAI is now served by the unified `spring-ai-openai` module under the **"Microsoft Foundry"** label. The module auto-detects Foundry mode when:

- `spring.ai.openai.base-url` ends with `openai.azure.com` (or `cognitiveservices.azure.com`), **or**
- a connection-level / chat-level `deployment-name` is set, **or**
- `microsoft-foundry: true` is set explicitly.

Source-of-truth: `org.springframework.ai.openai.setup.OpenAiSetup#detectModelProvider` in `models/spring-ai-openai/src/main/java/.../setup/OpenAiSetup.java` at the v2.0.0-M5 tag.

**Config keys move:**

```diff
 spring:
   ai:
-    azure:
-      openai:
-        api-key: ...
-        endpoint: https://your-resource.openai.azure.com/
-        chat:
-          options:
-            deployment-name: gpt-41-mini
+    openai:
+      api-key: ...
+      base-url: https://your-resource.openai.azure.com/
+      chat:
+        options:
+          deployment-name: gpt-41-mini
+          model: gpt-41-mini   # see §5.1 — used as the URL deployment segment
```

**Maven dependency change:**

```diff
-<dependency>
-  <groupId>org.springframework.ai</groupId>
-  <artifactId>spring-ai-starter-model-azure-openai</artifactId>
-</dependency>
-<dependency>
-  <groupId>org.springframework.ai</groupId>
-  <artifactId>spring-ai-azure-store</artifactId>
-</dependency>
+<dependency>
+  <groupId>org.springframework.ai</groupId>
+  <artifactId>spring-ai-starter-model-openai</artifactId>
+</dependency>
```

`spring-ai-azure-store` (the Azure AI Search vector store) is **still present** in M5 — it's separate from the chat module that was removed. We dropped it because it wasn't actually used (workshop uses `SimpleVectorStore` or `PgVectorStore` exclusively).

### 1.2 `org.springframework.ai.openai.api.*` package gone

Spring AI 2.0.0-M5 removed its in-house OpenAI HTTP shim (`org.springframework.ai.openai.api.OpenAiAudioApi`, `OpenAiApi`, etc.) and now delegates to the official `openai-java` SDK directly. Constants previously under that package now live in `com.openai.models.*`.

Concrete change in this repo:

```diff
 // components/apis/audio/.../TranscribeController.java
-import org.springframework.ai.openai.api.OpenAiAudioApi;
+import com.openai.models.audio.AudioResponseFormat;
 ...
-    .responseFormat(OpenAiAudioApi.TranscriptResponseFormat.TEXT)
+    .responseFormat(AudioResponseFormat.TEXT)
```

### 1.3 `ChatClient.Builder.defaultOptions(...)` and `ChatClientRequestSpec.options(...)` signatures

Both methods now take `ChatOptions.Builder`, not a built `ChatOptions` instance. The release notes describe this as "options merging moved to ChatClient level" — the chat client now combines its own defaults with the builder before building.

Verified at `v2.0.0-M5` tag in `spring-ai-client-chat/src/main/java/org/springframework/ai/chat/client/ChatClient.java`:

```java
Builder defaultOptions(ChatOptions.Builder chatOptions);
```

Three call patterns in this repo had to change:

**(a) Inline call site** — `components/apis/chat/.../MultiModalController.java`:

```diff
-prompt = prompt.options(ChatOptions.builder().model("llava").build());
+prompt = prompt.options(ChatOptions.builder().model("llava"));   // pass the Builder, not the built thing
```

**(b) `@Bean ChatOptions` providers** — both `agentic-system/01-inner-monologue/.../config/AgentOptionsConfig.java` and `agentic-system/02-model-directed-loop/.../config/AgentOptionsConfig.java`:

```diff
 @Bean
 @Profile("!ollama")
-public ChatOptions openAiAgentOptions() {
-  return OpenAiChatOptions.builder().toolChoice("required").build();
+public ChatOptions.Builder openAiAgentOptions() {
+  return OpenAiChatOptions.builder().toolChoice("required");
 }
```

**(c) Constructor parameter type propagation** — both agentic `Agent` classes + their `*AgentController`s. Field type, constructor parameter type, and any test mocks all flip from `ChatOptions` to `ChatOptions.Builder`.

**(d) Test side** — Mockito `any(ChatOptions.class)` → `any(ChatOptions.Builder.class)`; `OpenAiChatOptions.builder().build()` test fixtures → `OpenAiChatOptions.builder()` (drop the `.build()`).

### 1.4 Modules removed (we didn't use any of these, but flagging for completeness)

- `spring-ai-azure-openai` — see §1.1
- `spring-ai-openai-sdk` — folded into `spring-ai-openai`
- Vertex AI non-embedding modules — only `spring-ai-vertex-ai-embedding` remains. **Note:** `spring-ai-starter-model-google-genai` (which `provider-google` uses) is a *separate* module and is **unaffected**.
- ZhipuAI model
- OCI GenAI
- Pixtral 12B (Pixtral Large is deprecated)
- `SpringAiTestAutoConfigurations` (test infra)
- `ModelOptionsUtils.merge()`

### 1.5 MCP SDK upgraded to 2.0.0-M2

The Spring AI release notes warn of "breaking API changes" in the MCP SDK. **In our codebase nothing broke** — the M2 changes don't affect any of the imports our 5 `mcp/` submodules use (`io.modelcontextprotocol.client.*`, `io.modelcontextprotocol.server.*`, `io.modelcontextprotocol.spec.McpSchema.*`, `org.springframework.ai.mcp.annotation.*`). The whole reactor's tests pass without code changes in `mcp/`.

If you bump again and *do* hit MCP breakage, the SDK changelog lives at <https://github.com/modelcontextprotocol/java-sdk/releases>.

---

## Part 2 — Code & config changes (file-by-file)

### Maven / build

- `pom.xml` — `<spring-ai.version>2.0.0-M4</spring-ai.version>` → `2.0.0-M5`.

### Source

- `components/apis/chat/src/main/java/com/example/chat_07/MultiModalController.java` — drop `.build()` on `ChatOptions.builder()...`.
- `components/apis/audio/src/main/java/com/example/audio_01/TranscribeController.java` — import `com.openai.models.audio.AudioResponseFormat`, replace `OpenAiAudioApi.TranscriptResponseFormat.TEXT`.
- `agentic-system/01-inner-monologue/inner-monologue-agent/src/main/java/com/example/agentic/inner_monologue/`
  - `Agent.java` — constructor param `ChatOptions options` → `ChatOptions.Builder options`.
  - `InnerMonologueAgentController.java` — field & constructor param flipped to `ChatOptions.Builder`.
  - `config/AgentOptionsConfig.java` — beans return `ChatOptions.Builder` (drop `.build()`).
  - `src/test/.../AgentTest.java` — Mockito matcher to `any(ChatOptions.Builder.class)`; test fixtures drop `.build()`.
  - `src/test/.../InnerMonologueAgentControllerTest.java` — `@Bean ChatOptions chatOptions()` → `@Bean ChatOptions.Builder chatOptions()`.
  - `src/test/.../config/AgentOptionsConfigOpenAiTest.java` & `AgentOptionsConfigOllamaTest.java` — autowire `ChatOptions.Builder`, call `.build()` in the test body before the assertion.
- `agentic-system/02-model-directed-loop/model-directed-loop-agent/...` — same set of changes as 01 above (Agent, controller, config, three tests).

### Provider migrations

- `applications/provider-azure/pom.xml` — see §1.1; also added `spring-boot-starter-flyway` + `flyway-database-postgresql` (see §4.6).
- `applications/provider-azure/src/main/resources/application.yaml` — keys moved (§1.1); added `spring.application.name: azure` (§4.4); completed empty `pgvector` profile block (§4.5).
- `applications/provider-azure/src/main/resources/creds-template.yaml` — rewritten, M5-shaped.
- `applications/provider-azure/src/main/resources/db/migration/V1__test.sql` — new (§4.6).
- `applications/provider-azure/src/main/java/com/example/DebugController.java` — `@Value` keys updated to the new prefix.
- `applications/provider-azure/readme.md` — rewritten (note about M5 module change, new YAML shape).
- `applications/provider-openai/pom.xml` — added `spring-boot-restclient` (§4.3).
- `applications/provider-ollama/pom.xml` — added `spring-boot-restclient` (§4.3).
- `applications/provider-aws/src/main/resources/application.yaml` — added `spring.application.name: aws`.
- `applications/provider-google/src/main/resources/application.yaml` — added `spring.application.name: google`.

### Gateway

- `applications/gateway/src/main/java/com/example/RouteConfig.java` — full rewrite of the route-selection `.before` filter to do explicit per-provider URI + path rewriting (§4.1). Replaces the old `uri(...).apply(request)` + `stripPrefix(1)` pattern.

### Docs / version sweep (non-historical files only — `migration/*` and CHANGELOG entries for past releases are deliberately preserved)

`README.md`, `WHATS_NEW_STAGE_06_MCP.md`, `agentic-system/readme.md`, `workshop.sh` (4 banners), `docs/README.md`, `docs/guide.md`, `docs/spring-ai/SPRING_AI_INTRODUCTION.md`, `docs/providers.md` (Azure section + new M5 callout), `support/{howto_windows11,os-compatibility-analysis,prerequisites}.md`, all 6 `applications/provider-*/readme.md`, `applications/provider-google/pom.xml` (the embedding-exclusion comment), `components/config-dashboard/src/main/resources/templates/fragments/layout.html`, `components/config-dashboard/src/main/resources/static/slides.html`, `docker/observability-stack/grafana/dashboards/spring-ai-workshop-overview.json`, `VERSION`, `components/config-dashboard/src/main/resources/workshop.properties`, and a new `[2.3.4]` entry in `CHANGELOG.md`.

Deliberately **not** touched:

- `migration/*.md` — historical record of the prior 1.x → 2.0-M4 migration.
- `CHANGELOG.md` entries for `[2.3.3]` and earlier — historical.
- `docs/spring-ai/SPRING_AI_STAGE_7.md` line "renamed from `OllamaOptions` in Spring AI 2.0.0-M4" — that's a historical fact about M4, still correct.

---

## Part 3 — Pitfalls discovered during testing

These are the things that surprised us. None of them are strictly Spring-AI-2.0.0-M5 changes — they're either (a) latent bugs that the milestone bump exposed, or (b) Spring-AI-adjacent things you only learn the hard way.

### 3.1 Gateway forwarded request URLs broke

**Symptom:** with the `spy` profile active, every chat call returned `404 NOT_FOUND`. The audit log showed an apparently-correct `https://api.openai.com/v1/chat/completions` destination URI.

**Cause:** Spring AI 2.0.0-M5 delegates HTTP to the openai-java SDK, which constructs request paths itself (`/chat/completions`, `/embeddings`, …) by appending to the configured `base-url`. With the spy profile, base-url is `http://localhost:7777/openai`, so the SDK now sends `/openai/chat/completions` to the gateway. The previous gateway code routed to `uri("https://api.openai.com/v1/chat/completions")`, which produced `https://api.openai.com/v1/chat/completions/chat/completions` — 404. The audit log was misleading because it reported the route's *destination URI base*, not the actual outbound URL after path-merging.

**Underlying gotcha:** Spring Cloud Gateway MVC's `ProxyExchangeHandlerFunction` only takes **scheme/host/port** from the route URI and uses the **request URI's path** as the outbound path. The route URI's path component is silently dropped:

```java
URI url = UriComponentsBuilder.fromUri(serverRequest.uri())
    .scheme(uri.getScheme())
    .host(uri.getHost())
    .port(uri.getPort()) // ← path NOT taken from route uri
    ...
```

**Fix:** rewrite the request URI's path explicitly per provider (don't rely on `stripPrefix` + a path-bearing route URI):

```java
.before(request -> {
  String requestPath = request.uri().getPath();
  String routeBase, upstreamPath;
  if (requestPath.startsWith("/openai/")) {
    routeBase = "https://api.openai.com";
    upstreamPath = "/v1" + requestPath.substring("/openai".length());
  } else if (...) { ... }
  URI rewritten = UriComponentsBuilder.fromUri(request.uri())
      .replacePath(upstreamPath).build(true).toUri();
  return uri(routeBase).apply(ServerRequest.from(request).uri(rewritten).build());
})
// .before(stripPrefix(1))   // no longer needed — we set the path ourselves
```

See `applications/gateway/src/main/java/com/example/RouteConfig.java`.

### 3.2 Latent missing `spring-boot-restclient` dep

**Symptom:** `provider-openai` and `provider-ollama` failed to start with the `ui` profile:

```
Parameter 2 of constructor in com.example.dashboard.DashboardController required a bean
of type 'org.springframework.web.client.RestClient$Builder' that could not be found.
```

**Cause:** Spring Boot 4 split `RestClient.Builder` autoconfig out of the web starter into a separate `spring-boot-restclient` module. A previous hotfix added it to azure/aws/google providers but missed openai and ollama.

**Fix:** add the dep:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-restclient</artifactId>
</dependency>
```

### 3.3 Latent missing `spring.application.name`

**Symptom:** dashboard topbar badge shows `unknown` for `provider-azure` (and would for aws/google).

**Cause:** `DashboardController.detectProvider()` reads `spring.application.name` and falls back to `"unknown"`. Three providers never set it.

**Fix:** add `spring.application.name: <provider>` in each provider's `application.yaml`. (Also makes OpenTelemetry resource attributes correct; otherwise `service.name=unknown_service` in Loki/Tempo too.)

### 3.4 Empty `pgvector` profile block in `provider-azure`

**Symptom:** would have produced obscure pgvector errors at runtime. Caught while doing the M5 review.

**Cause:** the `application.yaml` `---\non-profile: pgvector` block had a `spring.ai:` key with nothing under it (whereas `provider-openai` has the full `vectorstore.pgvector` config block). Pre-M5 nobody had tried Azure with `pgvector`.

**Fix:** copy the OpenAI provider's block (HNSW / COSINE_DISTANCE / dimension 1536).

### 3.5 Postgres pgvector extension is per-database, not per-cluster

**Symptom:**

```
PreparedStatementCallback; bad SQL grammar [INSERT INTO public.vector_store (...) ...]
caused by: org.postgresql.util.PSQLException: Unbekannter Typ vector.
```

**Cause:** `docker/postgres/db/docker_postgres_init.sql` creates the databases (`openai`, `azure`, `ollama`) but never installs the `vector` extension. The OpenAI/Ollama providers each ship a Flyway migration `V1__test.sql` that runs `CREATE EXTENSION IF NOT EXISTS vector` on startup. The Azure provider never had this — pgvector was uncharted territory there.

Spring AI's `PgVectorStore` *deliberately* does not auto-create the extension or table — it logs `"Skipping the schema initialization for the table"` and expects you to manage schema yourself.

**Fix:** add `spring-boot-starter-flyway` + `flyway-database-postgresql` to `provider-azure/pom.xml`, plus `provider-azure/src/main/resources/db/migration/V1__test.sql` matching `provider-openai`'s:

```sql
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE TABLE IF NOT EXISTS vector_store (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    content text,
    metadata json,
    embedding vector(1536)
);
CREATE INDEX ON vector_store USING HNSW (embedding vector_cosine_ops);
```

**Update (post-investigation):** an earlier draft of this document recommended replicating the Flyway migration to `provider-aws` and `provider-google`. Closer audit shows that's **not** needed and would actively make things worse. Those two providers (and `provider-anthropic`) deliberately exclude `embedding`, `vector-store`, and `config-pgvector` from their poms — Spring AI 2.0.0-M5 doesn't auto-configure an `EmbeddingModel` for AWS Bedrock Converse (chat-only starter), Google Gemini in API-key mode, or Anthropic (no native embeddings). They also have no `pgvector` profile block in `application.yaml`. So `--profiles=pgvector` on those providers is a clean no-op: the profile activates, but no pgvector code path is wired, no JDBC autoconfig fires, and no Flyway runs. Adding Flyway there would force Postgres to be running on startup for no benefit. The gap is closed only for providers that actually use pgvector (openai, ollama, azure).

---

## Part 4 — Azure-specific deep dive (the long pole)

This was the bulk of the migration work. Six pitfalls in a row, in roughly the order we hit them:

### 4.1 Deployment-name-in-URL semantics

The openai-java SDK's Foundry mode constructs Azure URLs as:

```
<base-url>/openai/deployments/<MODEL_FROM_REQUEST_BODY>/chat/completions?api-version=<...>
```

The `<MODEL_FROM_REQUEST_BODY>` is the `model` field of the chat completion request — i.e., `spring.ai.openai.chat.options.model` in YAML. So **the `model` you set in Spring AI's chat options must equal your Azure deployment name** (not the underlying model identifier).

Source: `com.openai.core.PrepareRequest#prepare` calls `params.modelNameOrNull()` (reflection on `model()`), passes it to `addPathSegmentsForAzure`, which appends `/openai/deployments/<that>` for `AZURE_LEGACY` URLs.

Setting both `deployment-name` and `model` to the same value is the safe pattern:

```yaml
spring.ai.openai.chat.options.deployment-name: gpt-41-mini
spring.ai.openai.chat.options.model:           gpt-41-mini   # ← path segment + request body
```

Same for embeddings:

```yaml
spring.ai.openai.embedding.options.deployment-name: text-embedding-3-small
spring.ai.openai.embedding.options.model:           text-embedding-3-small
```

### 4.2 Custom subdomain is mandatory

**Symptom:** curl returns `401 invalid subscription key or wrong API endpoint` even with a known-good key.

**Cause:** Azure resources created without `--custom-domain` use a shared regional URL that openai-java's `AzureUrlCategory.categorizeBaseUrl` doesn't recognize as Azure (it requires `*.openai.azure.com` or `*.cognitiveservices.azure.com`).

**Fix on resource creation:**

```bash
az cognitiveservices account create \
  --name "$RESOURCE" --resource-group "$RG" --location "$LOCATION" \
  --kind OpenAI --sku S0 \
  --custom-domain "$RESOURCE" \    # ← mandatory; resource name typically reused as subdomain
  --yes
```

Endpoint will then be `https://<RESOURCE>.openai.azure.com/`. If you have an old resource on a regional URL, easiest is delete + recreate.

### 4.3 `api-version` query param

The openai-java SDK auto-attaches `?api-version=...` for Azure (Foundry) requests — defaulting to `AzureOpenAIServiceVersion.latestStableVersion()` (`V2024_10_21` in openai-java 4.28.0). To pin a specific one, set:

```yaml
spring.ai.openai.microsoft-foundry-service-version: 2024-10-21   # connection-level
```

We didn't need to override — the default works for `gpt-4.1-mini` and `text-embedding-3-small` deployments.

### 4.4 Each model needs its own deployment

**Symptom:** chat works but `/embed/01/text` returns `404: The API deployment for this resource does not exist.`

**Cause:** Azure OpenAI doesn't share deployments across model types. Chat and embeddings are two separate deployments, each with its own deployment name.

**Fix:**

```bash
az cognitiveservices account deployment create \
  --name "$RESOURCE" --resource-group "$RG" \
  --deployment-name text-embedding-3-small \
  --model-name text-embedding-3-small --model-version "1" --model-format OpenAI \
  --sku-capacity 50 --sku-name Standard
```

### 4.5 Capacity sizing — silent rate-limit retries look like "hangs"

**Symptom:** Stage 4 chat-memory or Stage 3 vector-store endpoints "hang" for 1–3 minutes before erroring or never returning.

**Cause:** the openai-java SDK silently retries on 429 (default `maxRetries=3`, `timeout=60s` → up to 180s of "hanging"). Default deployments created via `--sku-capacity 1` only allow ~1k TPM for embeddings and ~10k TPM for chat — small bursts trip the limit, especially the Vector Store loader sending 70+ chunks at once.

**Fix:** create deployments with workshop-realistic capacity from the start:

| Deployment | Capacity | Approx TPM |
|---|---|---|
| `gpt-41-mini` (chat) | `--sku-capacity 30` | ~30k TPM |
| `text-embedding-3-small` (embedding) | `--sku-capacity 50` | ~50k TPM |

Capacity is **free** — Azure OpenAI Standard SKU is pay-per-token regardless. The cap is per-region quota, not per-deployment cost.

To bump capacity post-creation, `az cognitiveservices account deployment` does **not** support `update` — you have to delete and recreate. Existing pgvector data survives that (1536-dim floats aren't bound to the deployment that produced them).

### 4.6 Recommended provisioning script (idempotent)

```bash
RG="spring-ai-workshop-rg"
RESOURCE="<unique-name-eg-andreas-spring-ai>"
LOCATION="eastus"

az group create --name "$RG" --location "$LOCATION"

az cognitiveservices account create \
  --name "$RESOURCE" --resource-group "$RG" --location "$LOCATION" \
  --kind OpenAI --sku S0 \
  --custom-domain "$RESOURCE" --yes

az cognitiveservices account deployment create \
  --name "$RESOURCE" --resource-group "$RG" \
  --deployment-name gpt-41-mini \
  --model-name gpt-4.1-mini --model-version "2025-04-14" --model-format OpenAI \
  --sku-capacity 30 --sku-name Standard

az cognitiveservices account deployment create \
  --name "$RESOURCE" --resource-group "$RG" \
  --deployment-name text-embedding-3-small \
  --model-name text-embedding-3-small --model-version "1" --model-format OpenAI \
  --sku-capacity 50 --sku-name Standard

ENDPOINT=$(az cognitiveservices account show --name "$RESOURCE" --resource-group "$RG" --query properties.endpoint -o tsv)
KEY=$(az cognitiveservices account keys list --name "$RESOURCE" --resource-group "$RG" --query key1 -o tsv)
# write into provider-azure/src/main/resources/creds.yaml without echoing the key — see provider-azure/readme.md
```

To rotate the key: `az cognitiveservices account keys regenerate --name "$RESOURCE" --resource-group "$RG" --key-name key1`.
To delete everything: `az group delete --name "$RG" --yes --no-wait`.

---

## Part 5 — Provider-by-provider checklist

| Provider | Module changed? | Code changed? | Config keys changed? | New deps? | Notes |
|---|---|---|---|---|---|
| **Azure** | ✅ `spring-ai-starter-model-azure-openai` → `spring-ai-starter-model-openai` | `DebugController` keys updated | Full rewrite (see §1.1) | `spring-boot-starter-flyway` + `flyway-database-postgresql` | Biggest migration. Custom subdomain mandatory; capacity sizing matters. |
| **OpenAI** | — | — | — | `spring-boot-restclient` (latent fix) | The bump itself is a no-op once `spring-boot-restclient` is added. |
| **Ollama** | — | — | — | `spring-boot-restclient` (latent fix) | Same. |
| **Anthropic** | — | — | — | — | Just the version bump. |
| **AWS Bedrock** | — | — | `spring.application.name: aws` added | — | Just the version bump + the latent app-name fix. |
| **Google GenAI** | — | — | `spring.application.name: google` added | — | `spring-ai-starter-model-google-genai` is a separate module from the (removed) Vertex AI ones — not affected. |

---

## Part 6 — Verification

```bash
# Full reactor build (29 modules)
./mvnw clean verify
# Expect: BUILD SUCCESS

# Check version refs are clean (only history files / migration docs should still mention M4)
grep -rl "2\.0\.0-M4" --include="*.md" --include="*.xml" --include="*.html" \
  --include="*.yaml" --include="*.yml" --include="*.sh" --include="*.json" \
  --include="VERSION" . | grep -v "^\./migration/" | grep -v "/target/" | grep -v "CHANGELOG.md"
# Expect: docs/spring-ai/SPRING_AI_STAGE_7.md (the historical "renamed in M4" sentence — do not update)

# Provider smoke tests (with the relevant credentials wired up)
./mvnw spring-boot:run -pl applications/provider-openai -Dspring-boot.run.profiles=pgvector,observation,ui
# … and same for anthropic / azure / aws / google / ollama
```

---

## Part 7 — Recommended follow-ups (not done in this bump)

1. ~~Replicate the Flyway migration to `provider-aws` and `provider-google`~~ — **investigated and closed.** AWS/Google/Anthropic deliberately exclude embedding/vector-store/config-pgvector from their poms (Spring AI 2.0.0-M5 doesn't auto-configure an `EmbeddingModel` for those chat starters), and they have no `pgvector` profile block in `application.yaml`. `--profiles=pgvector` is a clean no-op on those three; no Flyway needed. See updated note in §3.5.
2. **Bake capacity flags into the Azure setup commands in `applications/provider-azure/readme.md`** — currently the readme inherits the M4-era `--sku-capacity 1`, which is the trap from §4.5.
3. **Decide on a Foundry api-version pin** — we rely on the openai-java SDK default (`V2024_10_21`). When the SDK bumps its default, we'll automatically follow; if a deployment ever needs a specific version, set `spring.ai.openai.microsoft-foundry-service-version` explicitly.
4. **Re-test the `spy` gateway path with anthropic** — the gateway rewrite now sends `/anthropic/<rest>` straight through with no `/v1` prefix. If Anthropic's SDK appends `/v1/messages` on its own, we're fine; if it appends just `/messages`, the route needs the same `/v1` prefix as `/openai`. We didn't have an active anthropic-via-spy test during this bump.
5. **Watch for the next Spring AI milestone (M6 / GA)** — particularly the Spring AI documentation site (<https://docs.spring.io/spring-ai/reference/>), which at the time of this bump still showed pre-M5 Azure config. Once M5 docs land they may supersede some of §4 here with cleaner guidance.

---

*Last updated: 2026-04-28. Author: workshop maintainer + Claude (collaborative bump session).*
