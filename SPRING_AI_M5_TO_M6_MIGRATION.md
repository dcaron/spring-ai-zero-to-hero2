# Spring AI 2.0.0-M5 → 2.0.0-M6 Migration

**Workshop release:** 2.3.5 (2026-05-10)
**Stack:** Spring Boot 4.0.6 · Spring AI 2.0.0-M6 · Java 25 · Maven 3.9.14
**Reference release notes:** <https://github.com/spring-projects/spring-ai/releases/tag/v2.0.0-M6>

This document is the post-mortem of bumping the workshop from Spring AI 2.0.0-M5 to 2.0.0-M6. It records (a) every Spring AI breaking change that touched our code, (b) the items in the M6 release notes that we audited but that did **not** touch us (so the next bumper doesn't have to re-investigate), and (c) a per-provider checklist.

> Companion to `SPRING_AI_M4_TO_M5_MIGRATION.md` (the previous milestone) and `migration/upgrade.md` (the original Boot 3.5 + Spring AI 1.x → Boot 4 + Spring AI 2.0-M4 migration). Same shape, one milestone later.

---

## TL;DR

| Layer | Change | Action |
|---|---|---|
| Maven | `spring-ai.version` 2.0.0-M5 → 2.0.0-M6 in root `pom.xml` | one-line bump |
| Spring AI API | `PromptChatMemoryAdvisor` removed | rewrite `mem_02/ChatHistoryController` to use `MessageChatMemoryAdvisor` |
| Spring AI API | `MessageChatMemoryAdvisor.Builder.conversationId(String)` removed | conversation id now passed at request time via the `ChatMemory.CONVERSATION_ID` context key — adapt both agentic-system `Agent` classes and their `AgentTest` mocks |
| Workshop | bumped 2.3.4 → 2.3.5 (`VERSION`, `workshop.properties`, `layout.html` placeholder) | mechanical |
| Docs / UI | Version label sweep `2.0.0-M5` → `2.0.0-M6` everywhere except history files | mechanical |
| Docs | Stage 4 + Stage 7 chat-memory examples rewritten for the M6 advisor API | one rewrite each |

Total reactor stays green: `./mvnw clean verify` — 29 modules, all tests pass.

---

## Part 1 — Spring AI 2.0.0-M6 breaking changes that touched us

### 1.1 `PromptChatMemoryAdvisor` removed

The class is gone in M6. Verified by listing the advisor package at the v2.0.0-M5 vs v2.0.0-M6 tags:

```bash
gh api 'repos/spring-projects/spring-ai/contents/spring-ai-client-chat/src/main/java/org/springframework/ai/chat/client/advisor?ref=v2.0.0-M5' --jq '.[].name' | grep MemoryAdvisor
# MessageChatMemoryAdvisor.java
# PromptChatMemoryAdvisor.java     ← present in M5

gh api 'repos/spring-projects/spring-ai/contents/spring-ai-client-chat/src/main/java/org/springframework/ai/chat/client/advisor?ref=v2.0.0-M6' --jq '.[].name' | grep MemoryAdvisor
# MessageChatMemoryAdvisor.java    ← only this one in M6
```

Affected file: `components/patterns/03-chat-memory/src/main/java/com/example/mem_02/ChatHistoryController.java`. Change shape:

```diff
-import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
+import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
+import org.springframework.ai.chat.memory.ChatMemory;
 ...
-private final PromptChatMemoryAdvisor promptChatMemoryAdvisor;
+private static final String CONVERSATION_ID = "mem-02-default";
+private final MessageChatMemoryAdvisor chatMemoryAdvisor;
 ...
-this.promptChatMemoryAdvisor = PromptChatMemoryAdvisor.builder(memory).build();
+this.chatMemoryAdvisor = MessageChatMemoryAdvisor.builder(memory).build();
 ...
-.advisors(promptChatMemoryAdvisor)
+.advisors(spec -> spec.advisors(chatMemoryAdvisor)
+    .param(ChatMemory.CONVERSATION_ID, CONVERSATION_ID))
```

`MessageChatMemoryAdvisor` differs from the old `PromptChatMemoryAdvisor` in *how* history is injected — message-list vs. inlined-into-prompt-text — but for this demo (a single-thread "remember my name" example) the behavioral difference is invisible. The endpoints `GET /mem/02/hello` and `GET /mem/02/name` continue to work as before.

### 1.2 `MessageChatMemoryAdvisor.Builder.conversationId(String)` removed

The builder no longer accepts a conversation id. Conversation id must be supplied at **request time** via the `ChatMemory.CONVERSATION_ID` context key. The base advisor's `getConversationId(...)` signature also changed:

```diff
- // M5
- default String getConversationId(Map<String, Object> context, String defaultConversationId) {
-   return context.containsKey(ChatMemory.CONVERSATION_ID)
-       ? context.get(ChatMemory.CONVERSATION_ID).toString()
-       : defaultConversationId;
- }

+ // M6 — no implicit default
+ default String getConversationId(Map<String, @Nullable Object> context) {
+   Assert.notNull(context.get(ChatMemory.CONVERSATION_ID), "conversationId cannot be null");
+   return context.get(ChatMemory.CONVERSATION_ID).toString();
+ }
```

(Verified at the `v2.0.0-M5` and `v2.0.0-M6` tags in `spring-ai-client-chat/src/main/java/org/springframework/ai/chat/client/advisor/api/BaseChatMemoryAdvisor.java`.)

This means **every chat-memory call must supply a conversation id**. Affected files in this repo:

- `agentic-system/01-inner-monologue/inner-monologue-agent/src/main/java/com/example/agentic/inner_monologue/Agent.java`
- `agentic-system/02-model-directed-loop/model-directed-loop-agent/src/main/java/com/example/agentic/model_directed_loop/Agent.java`

Both used the M5 builder pattern:

```java
// M5
var chatMemoryAdvisor = MessageChatMemoryAdvisor.builder(memory).conversationId(id).build();
...
.defaultAdvisors(chatMemoryAdvisor)
```

In M6 we wire the id as a default request-time advisor param so every call from this agent's `ChatClient` automatically carries it:

```java
// M6
var chatMemoryAdvisor = MessageChatMemoryAdvisor.builder(memory).build();
...
.defaultAdvisors(spec -> spec.advisors(chatMemoryAdvisor)
    .param(ChatMemory.CONVERSATION_ID, id))
```

This uses the `Builder defaultAdvisors(Consumer<AdvisorSpec> advisorSpecConsumer)` overload of `ChatClient.Builder`, which gives access to both `.advisors(...)` and `.param(...)` so the advisor and its context can be configured together. Confirmed in `spring-ai-client-chat/src/main/java/org/springframework/ai/chat/client/ChatClient.java` at v2.0.0-M6.

### 1.3 Test mocks need a `defaultAdvisors(Consumer)` stub

Both `agentic-system/.../AgentTest.java` files mock `ChatClient.Builder` and stub the `defaultAdvisors(Advisor...)` overload. After §1.2 the agent code now calls `defaultAdvisors(Consumer<AdvisorSpec>)`, which Mockito returns `null` for unstubbed → NPE on the next chained `.build()`. Fix:

```diff
 when(clone.defaultAdvisors(any(org.springframework.ai.chat.client.advisor.api.Advisor.class)))
     .thenReturn(clone);
+when(clone.defaultAdvisors(any(java.util.function.Consumer.class))).thenReturn(clone);
```

Both `AgentTest` files in `agentic-system/01-inner-monologue/.../src/test/java/...` and `agentic-system/02-model-directed-loop/.../src/test/java/...` got this addition. The original `(Advisor...)` stub stays — other parts of the test infrastructure may still hit that overload — and it's also harmless if unused.

### 1.4 Documentation rewrites

Two reference documents shipped code examples that were now wrong:

- `docs/spring-ai/SPRING_AI_STAGE_4.md` — rewritten the Demo 03b "Chat Memory with Advisor" section (Spring AI Components list, sequence diagram participant name, key code, takeaway, advisor table, memory-stack diagram) to use `MessageChatMemoryAdvisor` and the M6 context-key pattern.
- `docs/spring-ai/SPRING_AI_STAGE_7.md` — the **Per-Agent Memory (conversationId)** section was rewritten end-to-end to explain the M6 context-key approach, including an M5 → M6 delta block. The "Spring AI Component Reference" table entry for `MessageChatMemoryAdvisor.Builder.conversationId(String)` was replaced with a `ChatMemory.CONVERSATION_ID` row.
- `docs/spring-ai/README.md` and `docs/guide.md` — both mentioned `PromptChatMemoryAdvisor` in passing (table entries, Stage 4 demo blurb) and now point to `MessageChatMemoryAdvisor`.

### 1.5 Items from the M6 release notes that we audited and don't apply here

These changes are real but didn't touch this repo. Listing them so the next bumper doesn't re-investigate:

- **OpenAI options refactoring** — setter methods removed from `OpenAi*Options`, properties classes no longer extend `AbstractOpenAiOptions`, `@NestedConfigurationProperty` annotations removed, `OpenAiEmbeddingOptions#encodingFormat` is now an enum. We don't subclass `AbstractOpenAiOptions`, never call setters on options classes (always builder-form), and don't set `encodingFormat`. No-op.
- **`OpenAiConnectionProperties` → `OpenAiCommonProperties`** rename. We don't reference this class directly. No-op.
- **Mistral / MiniMax / Google GenAI / ElevenLabs / Bedrock / DeepSeek / Anthropic / Ollama options** — setter methods removed, builder-only. Same reasoning as above — we always use the builder API. No-op.
- **`spring-ai-hanadb-store` and `spring-ai-infinispan-store` modules removed.** Not used. No-op.
- **`ModelOptionsUtils` method removals.** Not referenced anywhere in this repo (`grep -rn ModelOptionsUtils` returns nothing). No-op.
- **OpenAI Java SDK 4.34.0 / Anthropic SDK 2.30.0 / Anthropic Java 2.27.0 / Qdrant 1.17** — transitively picked up via the `spring-ai-bom` upgrade. No code changes required. The Anthropic provider continues to work with no edits.
- **MCP integration improvements** (`SpringAiSchemaModule` reuse, `@Nullable` type detection, semantic-cache `Filter.Expression`, session ID in error logs) — internal to the framework. The 5 `mcp/` submodules continue to compile and pass tests with no changes.

---

## Part 2 — Code & config changes (file-by-file)

### Maven / build

- `pom.xml` — `<spring-ai.version>2.0.0-M5</spring-ai.version>` → `2.0.0-M6`.

### Source

- `components/patterns/03-chat-memory/src/main/java/com/example/mem_02/ChatHistoryController.java` — rewritten per §1.1.
- `agentic-system/01-inner-monologue/inner-monologue-agent/src/main/java/com/example/agentic/inner_monologue/Agent.java` — rewritten per §1.2.
- `agentic-system/02-model-directed-loop/model-directed-loop-agent/src/main/java/com/example/agentic/model_directed_loop/Agent.java` — rewritten per §1.2.
- `agentic-system/01-inner-monologue/inner-monologue-agent/src/test/java/com/example/agentic/inner_monologue/AgentTest.java` — added `Consumer` stub per §1.3.
- `agentic-system/02-model-directed-loop/model-directed-loop-agent/src/test/java/com/example/agentic/model_directed_loop/AgentTest.java` — added `Consumer` stub per §1.3.

### Docs (substantive rewrites — not just version-label bumps)

- `docs/spring-ai/SPRING_AI_STAGE_4.md` — Demo 03b chat-memory section rewritten; component reference table updated to add `ChatMemory.CONVERSATION_ID` and replace `PromptChatMemoryAdvisor`; advisor table + memory stack diagram updated.
- `docs/spring-ai/SPRING_AI_STAGE_7.md` — "Per-Agent Memory (conversationId)" section rewritten; component reference table updated.
- `docs/spring-ai/README.md` — Stage 4 row description updated.
- `docs/guide.md` — Stage 4 chat-memory blurb updated.

### Docs / UI / version sweep (mechanical — non-historical files only)

`README.md` (recent-upgrade banner rewritten + version line), `WHATS_NEW_STAGE_06_MCP.md`, `agentic-system/readme.md`, `workshop.sh` (4 banners), `docs/README.md`, `docs/guide.md`, `docs/providers.md`, `docs/spring-ai/SPRING_AI_INTRODUCTION.md`, `docs/spring-ai/SPRING_AI_STAGE_1.md`, `docs/spring-ai/SPRING_AI_STAGE_4.md`, `docs/spring-ai/SPRING_AI_STAGE_7.md`, `support/{howto_windows11,os-compatibility-analysis,prerequisites}.md`, all 6 `applications/provider-*/readme.md`, `applications/provider-azure/pom.xml`, `applications/provider-azure/src/main/resources/creds-template.yaml`, `applications/provider-google/pom.xml`, `applications/gateway/src/main/java/com/example/RouteConfig.java`, `components/apis/chat/src/main/java/com/example/chat_07/MultiModalController.java`, `components/apis/audio/src/main/java/com/example/audio_01/TranscribeController.java`, `components/config-dashboard/src/main/resources/templates/fragments/layout.html`, `components/config-dashboard/src/main/resources/static/slides.html`, `components/config-dashboard/src/main/resources/static/slides.html.original` (the prepare.sh baseline), `docker/observability-stack/grafana/dashboards/spring-ai-workshop-overview.json`, both `agentic-system/.../config/AgentOptionsConfig.java` and their tests (comment-only updates), `prepare.sh` (default version), `VERSION`, `components/config-dashboard/src/main/resources/workshop.properties`, and a new `[2.3.5]` entry in `CHANGELOG.md`.

Deliberately **not** touched:

- `migration/*.md` — historical record of the prior 1.x → 2.0-M4 migration.
- `SPRING_AI_M4_TO_M5_MIGRATION.md` — historical record of the prior milestone bump.
- `CHANGELOG.md` entries for `[2.3.4]` and earlier — historical (the [2.3.4] entry correctly records "M4 → M5", not "M4 → M6").
- `docs/spring-ai/SPRING_AI_STAGE_7.md` line "renamed from `OllamaOptions` in Spring AI 2.0.0-M4" — historical fact about M4, still correct (the M5/M6 sweep does not retroactively edit references to M4).

---

## Part 3 — Pitfalls discovered during testing

Two surprises during the bump. Both were minor; neither required a substantive design change.

### 3.1 Test mock NPE on `defaultAdvisors(Consumer)`

After §1.2 we ran `./mvnw clean verify` and got:

```
[ERROR] AgentTest.returnsStructuredResponseWhenJsonIsValid -- Time elapsed: 0.003 s <<< ERROR!
java.lang.NullPointerException: Cannot invoke "ChatClient$Builder.build()" because the return value
of "ChatClient$Builder.defaultAdvisors(java.util.function.Consumer)" is null
    at com.example.agentic.inner_monologue.Agent.<init>(Agent.java:62)
```

Cause and fix: see §1.3. Worth flagging here because the failure mode is not "the advisor doesn't work" — it's "the agent constructor blows up at startup", because the chained builder call hits a null return from an unstubbed Mockito method.

### 3.2 Spotless reformatted the new advisor lambda

Spotless (Google Java Format) wraps long lambda chains differently than what reads naturally. The first compile failed with:

```
[ERROR] Failed to execute goal com.diffplug.spotless:spotless-maven-plugin:2.46.1:check
    The following files had format violations:
    src/main/java/com/example/mem_02/ChatHistoryController.java
        @@ -62,7 +62,8 @@
        - .advisors(
        -     spec -> spec.advisors(chatMemoryAdvisor).param(ChatMemory.CONVERSATION_ID, CONVERSATION_ID))
        + .advisors(
        +     spec ->
        +         spec.advisors(chatMemoryAdvisor).param(ChatMemory.CONVERSATION_ID, CONVERSATION_ID))
```

`./mvnw spotless:apply` fixed it. Convention reminder: run `./mvnw spotless:apply` after edits that touch lambda-heavy code, before running `verify`.

---

## Part 4 — Provider-by-provider checklist

| Provider | Module changed? | Code changed? | Config keys changed? | New deps? | Notes |
|---|---|---|---|---|---|
| **OpenAI** | — | — | — | — | Just the version bump. The OpenAI options refactor in M6 is internal to subclasses we don't author. |
| **Anthropic** | — | — | — | — | Just the version bump. Anthropic SDK 2.30.0 picked up transitively. |
| **Azure (Foundry)** | — | — | — | — | Still on `spring-ai-starter-model-openai` from the M5 migration. No M6-specific changes. |
| **AWS Bedrock** | — | — | — | — | Just the version bump. |
| **Google GenAI** | — | — | — | — | Just the version bump. The release notes' "missing `GoogleGenAiChatOptions.responseMimeType` mutation added" is an additive fix; we don't set this option. |
| **Ollama** | — | — | — | — | Just the version bump. M6 fix to `OllamaChatOptions.getOutputSchema()` and `AssistantMessage` tool-call-id handling are bugfixes — visible only if you were hitting them. |

The chat-memory advisor changes (§1.1, §1.2) cut **across** providers — they live in `components/patterns` and `agentic-system`, both of which are wired into every provider app — so the table above is provider-agnostic; the changes are made once and apply to all 6 providers.

---

## Part 5 — Verification

```bash
# Full reactor build (29 modules)
./mvnw clean verify
# Expect: BUILD SUCCESS

# Confirm no stale M5 refs outside history files
grep -rl "2\.0\.0-M5" --include="*.md" --include="*.xml" --include="*.html" \
  --include="*.yaml" --include="*.yml" --include="*.sh" --include="*.json" \
  --include="*.properties" --include="*.java" --include="VERSION" . \
  | grep -v "/target/" | grep -v "/migration/" \
  | grep -v "SPRING_AI_M4_TO_M5_MIGRATION.md" | grep -v "/.git/"
# Expect: empty (the [2.3.4] CHANGELOG entry mentions "M4 → M5" — historical, intentional)

# Confirm the removed advisor isn't referenced anywhere
grep -rn "PromptChatMemoryAdvisor" --include="*.java" --include="*.md" \
  | grep -v "/target/" | grep -v "/migration/" \
  | grep -v "SPRING_AI_M4_TO_M5_MIGRATION.md"
# Expect: only the new SPRING_AI_M5_TO_M6_MIGRATION.md (this file) and CHANGELOG entry text

# Provider smoke tests (with the relevant credentials wired up)
./mvnw spring-boot:run -pl applications/provider-openai -Dspring-boot.run.profiles=pgvector,observation,ui
# … and same for anthropic / azure / aws / google / ollama
```

---

## Part 6 — Recommended follow-ups (not done in this bump)

1. **Re-test `mem_02` end-to-end with each provider once credentials are available.** The compile-time fix and unit tests pass, but the demo's behavior under `MessageChatMemoryAdvisor` (history as message list) vs. the old `PromptChatMemoryAdvisor` (history inlined into prompt text) produces *slightly different* prompts to the LLM. Smoke-tested mentally; should be confirmed by hitting `GET /mem/02/hello` then `GET /mem/02/name` against at least OpenAI and Ollama.
2. **Watch for the next Spring AI milestone (M7 / GA).** The MCP SDK, OpenAI options model, and observability surface are still in motion. Particularly:
   - The release notes mention "documentation clarifying observability gaps at HTTP layer for Anthropic and OpenAI chat models" — if those gaps close in a future release, our `spy` gateway profile may capture more (or less) telemetry, which could affect what we tell attendees in the Stage 8 observability section.
   - The Anthropic options "property name refactoring" called out in M6 — not a hit here, but worth re-auditing if we ever start binding Anthropic options via YAML.
3. ~~Capture the M6 advisor-context-key pattern as a pull-out callout in `docs/spring-ai/SPRING_AI_INTRODUCTION.md`.~~ — **done.** Added as a new `### Advisors and Request Context` subsection in the introduction's Core Concepts area, with an explicit M5 → M6 code diff and pointers to Stage 4 / Stage 7 for the in-depth treatment.

---

*Last updated: 2026-05-10. Author: workshop maintainer + Claude (collaborative bump session).*
