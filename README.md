# spring-ai-zero-to-hero

Example applications showing how to use Spring AI to build Generative
AI projects.

## What's New: Spring Boot 4 + Spring AI 2

This workshop has been upgraded to the latest Spring ecosystem:

| Component | Version |
|-----------|---------|
| Spring Boot | **4.0.5** |
| Spring AI | **2.0.0-M4** |
| Spring Framework | **7.x** |
| Java | **25** |
| Maven | **3.9.14** |

### Key Changes in Spring Boot 4

- **Jackson 3** — `com.fasterxml.jackson.databind` moved to `tools.jackson.databind` (annotations stay `com.fasterxml.jackson.annotation`)
- **Spring Cloud Gateway 5** — artifact renamed to `spring-cloud-starter-gateway-server-webmvc`
- **Spring Shell 4** — `@CommandScan` replaced by `@EnableCommand`, package restructured
- **Flyway** — requires `spring-boot-starter-flyway` instead of `flyway-core`
- **OpenTelemetry** — native `spring-boot-starter-opentelemetry` replaces Brave/Zipkin
- **AutoConfiguration packages** moved (jdbc, flyway)

### Key Changes in Spring AI 2.0

- **MCP Streamable HTTP** — SSE transport replaced with Streamable HTTP
- **MCP annotations** — `com.logaritex.mcp` integrated into `org.springframework.ai.mcp.annotation`
- **ToolCallAdvisor** — advisor-based tool execution (new pattern)
- **Native Structured Output** — `AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT`
- **Google provider** renamed — `vertex-ai-gemini` to `google-genai`

### New: Distributed Tracing Module

Custom AOP-based tracing with OpenTelemetry:
- `@TracedEndpoint` — controller spans (SpanKind.SERVER)
- `@TracedService` — service spans (SpanKind.INTERNAL)
- `@TracedRepository` — repository spans (SpanKind.CLIENT)

### New: LGTM Observability Stack

Single `grafana/otel-lgtm:latest` container replaces 6 separate containers. Includes Grafana, Loki (logs), Tempo (traces), Mimir (metrics), and OTel Collector.

---

## Software Prerequisites

**You need: Java 25+, Docker, Ollama, and your favourite Java IDE.**

### Java development tooling

* Java 25+ — install via [sdkman.io](https://sdkman.io/): `sdk install java 25-open`
* [Maven](https://maven.apache.org/index.html) (3.9.14 via included wrapper)
* Favourite Java IDE:
    * [IntelliJ](https://www.jetbrains.com/idea/download) (2025.1+)
    * [VSCode](https://code.visualstudio.com/) with Java Extension Pack
    * [Eclipse Spring Tool Suite](https://spring.io/tools)

### Containerization tools

* [Docker](https://www.docker.com/products/docker-desktop) for PostgreSQL/pgvector and observability stack

### Local AI Models

[Ollama](https://ollama.com/) makes running models on your laptop easy.

```bash
# Install Ollama, then pull required models:
ollama pull mistral             # Chat model (7B, default)
ollama pull nomic-embed-text    # Embedding model (768 dims)
ollama pull llava               # Multimodal model (image+text)
```

**16 GB macOS:** mistral + nomic-embed-text = ~9 GB active RAM. llava loads on-demand for multimodal demos only.

### Infrastructure

```bash
# Start PostgreSQL + pgvector
docker compose -f docker/postgres/docker-compose.yaml up -d

# Start observability stack (Grafana LGTM)
docker compose -f docker/observability-stack/docker-compose.yaml up -d
```

---

## Quick Start

```bash
# Build everything
./mvnw clean compile

# Run with Ollama (local, no API keys needed)
./mvnw spring-boot:run -pl applications/provider-ollama \
  -Dspring-boot.run.profiles=pgvector,observation

# Test it
curl "http://localhost:8080/chat/01/joke?topic=spring"
curl "http://localhost:8080/rag/01/load"
curl "http://localhost:8080/rag/01/query?topic=mountain+bike"

# View traces in Grafana
open http://localhost:3000
```

---

## API Keys

For cloud AI providers, create `src/main/resources/creds.yaml` in the provider app (gitignored):

### OpenAI

```yaml
spring:
  ai:
    openai:
      api-key: sk-...your-key...
```

Get a key at [platform.openai.com/api-keys](https://platform.openai.com/api-keys)

---

## Outline

Generative AI is a transformational technology. This workshop is designed for Spring developers looking to add generative AI to existing applications or to implement new AI apps using Spring AI.

We assume no previous AI experience. The workshop teaches key AI concepts and how to apply them using the Spring AI project.

### Workshop Stages (Learning Path):

1. **Chat Fundamentals** — ChatModel, ChatClient, prompt templates, structured output, tool calling, streaming
2. **Embeddings** — vector generation, similarity, chunking, document readers (JSON, Text, PDF)
3. **Vector Stores** — pgvector, similarity search, in-memory vs persistent
4. **AI Patterns** — stuff-the-prompt, RAG (manual + advisor), chat memory
5. **Advanced Agents** — chain-of-thought, self-reflection (writer + critic)
6. **MCP** — Model Context Protocol (stdio, HTTP, dynamic tools, resources, prompts)
7. **Agentic Systems** — inner monologue, model-directed loop, Spring Shell CLIs
8. **Observability** — distributed tracing, metrics, logs with OpenTelemetry + Grafana LGTM

See [migration/flow.md](migration/flow.md) for the detailed demo flow with all endpoints.

---

## Repo Organization

Spring AI provides a consistent API across many AI providers. The same code works with OpenAI, Google, Azure, Anthropic, AWS Bedrock, and local Ollama models.

- **`/components/data/`** — shared datasets (bikes, customers, products, orders)
- **`/components/apis/`** — provider-independent API demos (chat, embedding, vector-store, audio, image)
- **`/components/patterns/`** — AI patterns (RAG, chat memory, stuff-prompt, chain-of-thought, self-reflection, distributed tracing)
- **`/components/config-pgvector/`** — PgVector auto-configuration (profile-based)
- **`/applications/`** — provider-specific Spring Boot apps (ollama, openai, anthropic, azure, google, aws, gateway)
- **`/mcp/`** — Model Context Protocol demos (stdio, HTTP, client, dynamic tools, capabilities)
- **`/agentic-system/`** — agentic AI patterns (inner monologue, model-directed loop)
- **`/docker/`** — infrastructure (PostgreSQL/pgvector, Grafana LGTM observability stack)
- **`/migration/`** — upgrade documentation, test results, model mapping

## AI Provider Options

| Provider | Chat | Embedding | Multimodal | Tool Calling | Local | Test Status |
|----------|------|-----------|------------|--------------|-------|-------------|
| **Ollama** | mistral (7B) | nomic-embed-text | llava (auto) | Yes | Yes | 44/44 PASS |
| **OpenAI** | gpt-4o-mini | text-embedding-3 | gpt-4o | Yes | No | 44/44 PASS |
| **Anthropic** | Claude (direct API) | - | Claude 3+ | Yes | No | 14/14 PASS |
| **Azure OpenAI** | gpt-4.1-mini | text-embedding-3 | gpt-4o | Yes | No | 8/8 PASS |
| **Google** | Gemini 2.5 Flash | text-embedding-004 | Gemini | Yes | No | 13/13 PASS |
| **AWS Bedrock** | Amazon Nova Lite | - | - | Yes | No | 8/8 PASS |

## Spring Profiles

| Profile | Purpose |
|---------|---------|
| `pgvector` | PostgreSQL vector store (instead of in-memory) |
| `spy` | Route traffic through gateway for inspection |
| `observation` | Full observability (traces + metrics + logs to LGTM) |

## Recommendations

1. Start with **Ollama** — no API keys needed, all core demos work locally
2. Follow the stages in order (chat -> embeddings -> vectors -> patterns -> agents)
3. Run with `observation` profile and explore traces in Grafana
4. Try the same demos with different providers to see Spring AI's portability
5. See [migration/model_mapping.md](migration/model_mapping.md) for the full provider compatibility matrix
