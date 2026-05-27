# Spring AI 2.0.0-M6 → 2.0.0-M7 Migration

**Workshop release:** 2.3.6 (2026-05-27)
**Stack:** Spring Boot 4.0.6 · Spring AI 2.0.0-M7 · Java 25 · Maven 3.9.14
**Reference release notes:** <https://github.com/spring-projects/spring-ai/releases/tag/v2.0.0-M7>

This document is the post-mortem of bumping the workshop from Spring AI 2.0.0-M6 to 2.0.0-M7. It records (a) the M7 items in the release notes that we audited, (b) which ones touched our code (spoiler: none — see below), and (c) a per-provider checklist.

> Companion to `SPRING_AI_M5_TO_M6_MIGRATION.md`, `SPRING_AI_M4_TO_M5_MIGRATION.md`, and `migration/upgrade.md`. Same shape, one milestone later.

---

## TL;DR

| Layer | Change | Action |
|---|---|---|
| Maven | `spring-ai.version` 2.0.0-M6 → 2.0.0-M7 in root `pom.xml` | one-line bump |
| Spring AI API | _none of the M7 breaking changes touch our code_ | nothing to rewrite |
| Workshop | bumped 2.3.5 → 2.3.6 (`VERSION`, `layout.html` footer placeholder) | mechanical |
| Docs / UI | Version label sweep `2.0.0-M6` → `2.0.0-M7` everywhere except historical narration ("M6 removed X", CHANGELOG `[2.3.5]`, M6 timeline entry on the history page) | mechanical |

Full reactor stays green: `./mvnw clean verify` — 43 modules, all tests pass.

This is the calmest milestone bump since the workshop landed on the M-series. The M5→M6 work (advisor API rewrite, `ChatOptions.Builder` everywhere) and the M5-era MCP move to Streamable HTTP transport had already positioned the codebase on the path M7 chose. Every M7 breaking change either targets a module/API we don't use or formalises a pattern we already follow.

---

## Part 1 — Spring AI 2.0.0-M7 changes that touched us

**None.** This is the rare bump where no Java file changes were required. The reactor compiled and the full test suite passed on the first build after editing the `spring-ai.version` property.

The rest of this section enumerates the M7 release notes items we audited and explains why each one is a no-op for this workshop.

### 1.1 `ChatOptions` setters removed — already on builders

M7 removes mutable setters on `ChatOptions` (`setTemperature`, `setTopP`, `setModel`, `setMaxTokens`, …). The workshop never used these — every `ChatOptions` instance in the repo is constructed via the per-provider builder, e.g.:

```java
OpenAiChatOptions.builder().toolChoice("required")
OllamaChatOptions.builder().model(model)
```

`grep -rn 'chatOptions\.set\|setTemperature\|setTopP\|setTopK\|setModel\|setMaxTokens' --include=\"*.java\"` returns zero hits. The M5→M6 bump moved every call site to the `ChatOptions.Builder` argument shape, so M7's setter removal is silent here.

### 1.2 SSE transport deprecated (Streamable HTTP becomes default) — already on Streamable

M7 marks the MCP SSE client/server transports as deprecated and makes `STREAMABLE` the default server protocol. All three workshop MCP HTTP modules already pin `protocol: STREAMABLE` in their `application.yaml`:

```
mcp/02-mcp-http-server/src/main/resources/application.yaml
mcp/04-dynamic-tool-calling/server/src/main/resources/application.yaml
mcp/05-mcp-capabilities/src/main/resources/application.yaml
```

The client-side smoke tests under `mcp/0[25]/src/test/java/…/Client*.java` use `HttpClientStreamableHttpTransport.builder(...)` and have done so since M5. Despite the misleading `ClientSse.java` file name in `mcp/05-mcp-capabilities`, the code inside is already Streamable — only the file name predates the rename.

### 1.3 CosmosDB module removed — not used

`grep -rn -i 'cosmos\|CosmosDB' --include=\"*.java\" --include=\"*.xml\" --include=\"*.yml\"` returns no hits. The workshop's vector store track is built on **pgvector** (`components/config-pgvector`). Azure Cosmos DB was never wired in.

### 1.4 `spring-ai-spring-cloud-bindings` removed — not used

The workshop does not depend on `spring-ai-spring-cloud-bindings`. Cred injection comes from `creds.yaml` + Spring Boot config import, not from Cloud Bindings.

### 1.5 `GEMINI_2_0_FLASH` → `GEMINI_2_5_FLASH` constant rename — no constant refs

We don't reference Gemini model enum constants in Java. Provider-google sets the model via configuration (`spring.ai.google.genai.chat.options.model` in `creds-template.yaml`) — a string, not the SDK enum. The constant rename does not flow through.

### 1.6 `ToolSpec` fluent API — additive

M7 introduces a fluent builder for `ToolSpec`. The workshop's only `ToolSpec` reference is `McpServerFeatures.SyncToolSpecification` in `mcp/04-dynamic-tool-calling/server/.../ServerApplication.java`, which is the **MCP SDK** type (not Spring AI's tool-spec) and is constructed via `McpToolUtils.toSyncToolSpecifications(...)`. Unaffected.

### 1.7 `ToolCallAdvisor` becomes the default tool-call management option — silent

M7 makes `ToolCallAdvisor` the default rather than requiring opt-in. The workshop's tool-calling demos (`components/apis/chat/.../tool_*`, `agentic-system/.../Agent.java`) declare tools via `defaultTools(...)` on `ChatClient.Builder` and let Spring AI choose the call-management strategy. The default flip therefore lights up automatically without code changes.

### 1.8 `DefaultChatClient` single-`ToolAdvisor` invariant — no multi-advisor configs

The release notes call out a bug fix that now enforces a single `ToolAdvisor` per client. The workshop never registers more than one. The agentic-system clients chain `defaultAdvisors(spec -> spec.advisors(...).param(...))` with `MessageChatMemoryAdvisor` — that's the chat-memory advisor, not a tool advisor.

---

## Part 2 — Code & config changes (file-by-file)

### Maven / build

- `pom.xml` — `<spring-ai.version>2.0.0-M6</spring-ai.version>` → `2.0.0-M7`. That's the entire functional diff.

### Source

_No source files were modified for the M7 bump itself._

A small housekeeping commit landed immediately before this bump (`5da470b` — "housekeeping ahead of Spring AI M7 bump") and tidied three unrelated pre-existing items: deprecated `new TokenTextSplitter()` → `TokenTextSplitter.builder().build()` in five controllers, Jackson 3 `JsonNode.asText()` → `asString()` in `OpenApiSpecReader`, and comment polish. None of those depend on M7 — they were already correct against M6 — but they had drifted into the working tree before this bump began.

### Docs (substantive rewrites — not just version-label bumps)

- `README.md` — replaced the M5→M6 callout block with an M6→M7 one noting that no code changes were needed and linking to this file alongside the prior two migration docs.

### Docs / UI / version sweep (mechanical — non-historical files only)

Swept `Spring AI 2.0.0-M6` → `Spring AI 2.0.0-M7` in:

- Tech-stack banners — `README.md`, `agentic-system/readme.md`, `docs/README.md`, `docs/guide.md`, `docs/spring-ai/SPRING_AI_INTRODUCTION.md`, `support/howto_windows11.md`, `support/os-compatibility-analysis.md`, `support/prerequisites.md`
- All six provider READMEs — `applications/provider-{anthropic,aws,azure,google,ollama,openai}/readme.md` (banner line only)
- `workshop.sh` banners (4 occurrences), `prepare.sh` (interactive-prompt default + `replace_once` literal)
- Dashboard footer — `components/config-dashboard/.../templates/fragments/layout.html`
- Spring AI history page closing panel — `components/config-dashboard/.../static/spring-ai-history.html` (the M6 release-tag timeline entry at line ~550 is intentionally kept — that's the historical event itself)
- Grafana dashboard description JSON
- `WHATS_NEW_STAGE_06_MCP.md`, `docs/providers.md` ("Spring AI 2.0.0-M_ note" callout prefix)
- `applications/provider-google/pom.xml` comment justifying explicit `EmbeddingModel` config
- Three `SPRING_AI_INTRODUCTION.md` lines that reference "Spring AI 2.0.0-M6 artifact names" (those names are unchanged in M7, but the current-version label should track)

**Intentionally kept on M6 (historical narration):**

- `CHANGELOG.md` — the `[2.3.5]` entry recounts the M5→M6 bump
- `SPRING_AI_M5_TO_M6_MIGRATION.md` — the entire prior post-mortem
- `applications/provider-azure/pom.xml`, `applications/provider-azure/readme.md`, `applications/provider-azure/.../creds-template.yaml` — comments narrating *when* the `spring-ai-azure-openai` removal happened ("Spring AI 2.0.0-M6 removed …"); these are facts about M6, not current-version labels
- `docs/spring-ai/SPRING_AI_STAGE_{1,4,7}.md` and several callouts in `SPRING_AI_INTRODUCTION.md` — every "**Spring AI 2.0.0-M6** removed/changed …" remains pinned because the surrounding sentences describe the M6 delta itself
- `docs/guide.md:275` — "(Spring AI 2.0.0-M6 removed `PromptChatMemoryAdvisor` …)" parenthetical
- `components/config-dashboard/.../static/spring-ai-history.html:550` — the M6 release-tag entry on the timeline (the timeline is *the* history — it must stay accurate)

The `slides.html` bundle (380 KB minified) is intentionally not touched in either the sweep or the housekeeping commit; that file is a regenerated artifact and is being updated through a separate path tracking the three-line title work.

---

## Part 3 — Pitfalls discovered during testing

None. The build was green on the first run. There were no compile errors, no test failures, no deprecation warnings introduced by the bump.

This stands in contrast to M4→M5 (Azure module removal, OpenAI URL rewriting, MCP transport rename) and M5→M6 (`PromptChatMemoryAdvisor` removal, `conversationId(String)` removal, `defaultOptions` signature change). The M7 milestone tightened a few invariants and deprecated some transports, but it didn't break this workshop.

---

## Part 4 — Provider-by-provider checklist

| Provider | Java | YAML | Dependencies | Tests | Notes |
|---|---|---|---|---|---|
| **OpenAI** | — | — | — | — | Just the version bump. The OpenAI module's switch to the official `openai-java` SDK was completed in M5; M7 changes are internal. |
| **Anthropic** | — | — | — | — | Just the version bump. Anthropic SDK picked up transitively via the BOM. |
| **Azure (Foundry)** | — | — | — | — | Still on `spring-ai-starter-model-openai` from the M5 migration. M7's CosmosDB removal does not affect the OpenAI/Foundry path. |
| **AWS Bedrock** | — | — | — | — | Just the version bump. |
| **Google GenAI** | — | — | — | — | Just the version bump. The `GEMINI_2_0_FLASH` → `GEMINI_2_5_FLASH` constant rename does not flow through because we set the Gemini model via `creds.yaml` string config, not the SDK enum. The explicit `EmbeddingModel` bean wiring in `provider-google/pom.xml` is unchanged. |
| **Ollama** | — | — | — | — | Just the version bump. |

The audit findings in §§1.1–1.8 apply across providers — there is nothing provider-specific to migrate.

---

## Part 5 — Verification

```bash
# Full reactor build (43 modules)
./mvnw clean verify
# Expect: BUILD SUCCESS

# Confirm no stale M6 refs outside historical / migration files
grep -rln "2\.0\.0-M6" --include="*.md" --include="*.xml" --include="*.html" \
  --include="*.yaml" --include="*.yml" --include="*.sh" --include="*.json" \
  --include="*.properties" --include="*.java" --include="VERSION" . \
  | grep -v "/target/" | grep -v "/migration/" \
  | grep -v "SPRING_AI_M5_TO_M6_MIGRATION.md" \
  | grep -v "SPRING_AI_M4_TO_M5_MIGRATION.md" \
  | grep -v "/.git/" | grep -v "slides\.html"
# Expect: only files containing historical "Spring AI 2.0.0-M6 removed X" narration
# (CHANGELOG.md, provider-azure docs, SPRING_AI_STAGE_*.md, spring-ai-history.html
# timeline entry, docs/guide.md M6 parenthetical, SPRING_AI_INTRODUCTION.md change
# callouts).

# Confirm removed APIs aren't referenced anywhere
grep -rn "CosmosDB\|spring-ai-spring-cloud-bindings\|GEMINI_2_0_FLASH" \
  --include="*.java" --include="*.xml" --include="*.yaml" --include="*.yml" \
  | grep -v "/target/" | grep -v "SPRING_AI_M.*_TO_M.*_MIGRATION.md"
# Expect: empty

# Confirm we're not relying on deprecated SSE MCP transport defaults
grep -rn "SseClientTransport\|SseServerTransport" \
  --include="*.java" --include="*.yaml" --include="*.yml"
# Expect: empty (we use HttpClientStreamableHttpTransport + protocol: STREAMABLE)

# Provider smoke tests (with the relevant credentials wired up)
./mvnw spring-boot:run -pl applications/provider-openai -Dspring-boot.run.profiles=pgvector,observation,ui
# … and same for anthropic / azure / aws / google / ollama
```

---

## Part 6 — Recommended follow-ups (not done in this bump)

1. **Re-run end-to-end provider smokes** once Anthropic / Google credentials are refreshed. The M5→M6 bump validated `mem_02` per-provider; for M7 the API contract didn't change, so a full re-run is a *nice to have* rather than a must.
2. **Watch the SSE → Streamable deprecation timeline.** M7 deprecates SSE. The workshop is already on Streamable, but the next milestone may *remove* the SSE transport classes entirely — at which point any community fork still using `mcp/01-mcp-stdio-server` / `mcp/03-mcp-client` with a non-default transport will need an explicit pin. Worth a callout in the Stage 6 MCP guide if a learner asks "but what about the SSE option?".
3. **Watch for an M8 / GA window.** M7 feels like a stabilisation milestone (single-`ToolAdvisor` invariant, default `ToolCallAdvisor`, transport defaults). The next milestone will likely be GA or a final `M8` with more aggressive removals (likely target: the deprecated SSE transports, the older tool-call options shape, anything currently tagged with `@Deprecated` in the M7 source). Re-audit at GA — and at that point the workshop should drop the `spring-milestones` repository from `pom.xml`.
4. **`slides.html` regeneration.** The bundle currently still embeds the M6 banner. Whichever build path regenerates `components/config-dashboard/src/main/resources/static/slides.html` (the three-line title work in `8578688` is the most recent example) should be re-run so the slide deck shows M7 in lockstep with the other UI surfaces. The dashboard footer at `templates/fragments/layout.html` is already on M7.

---

*Last updated: 2026-05-27. Author: workshop maintainer + Claude (collaborative bump session).*
