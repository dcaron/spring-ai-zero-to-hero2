# Provider Setup

How to configure each AI provider, what credentials are needed, and which demos work with which provider.

---

## Provider Comparison

| Provider | Chat | Embedding | Multimodal | Tool Calling | Local | Cost |
|----------|:----:|:---------:|:----------:|:------------:|:-----:|------|
| **Ollama** | qwen3 (8B) | nomic-embed-text | llava (auto) | Yes | Yes | Free |
| **OpenAI** | gpt-4o-mini | text-embedding-3 | gpt-4o | Yes | No | Pay-per-use |
| **Anthropic** | Claude | — | Claude 3+ | Yes | No | Pay-per-use |
| **Azure OpenAI** | gpt-4.1-mini | text-embedding-3 | gpt-4o | Yes | No | Enterprise |
| **Google** | Gemini 2.5 Flash | text-embedding-004 | Gemini | Yes | No | Pay-per-use |
| **AWS Bedrock** | Amazon Nova Lite | — | — | Yes | No | Enterprise |

### Choosing a provider

| Use case | Recommended provider | Why |
|----------|---------------------|-----|
| Workshop / Offline | Ollama | Free, no API keys, all 44 demos work |
| Full feature demos | OpenAI | Chat + image + audio + multimodal + tools |
| Best reasoning | Anthropic or OpenAI | Claude / GPT-4o for complex tasks |
| Enterprise / Compliance | Azure OpenAI or AWS Bedrock | Data residency, SLAs, corporate billing |
| Cost-conscious | Ollama or OpenAI gpt-4o-mini | Free (local) or very cheap (cloud) |
| Google Cloud users | Google Vertex AI / GenAI | Native GCP integration |

---

## Provider Compatibility Matrix

Verified 2026-04-03.

| Stage | Endpoints | Ollama | OpenAI | Anthropic | AWS Bedrock | Google | Azure |
|-------|:---------:|:------:|:------:|:---------:|:-----------:|:------:|:-----:|
| 1. Chat | 14 | 14/14 | 14/14 | 14/14 | 7/14 | 13/13 | 7/8 |
| 2. Embeddings | 10 | 10/10 | 10/10 | N/A | N/A | 13/13 | 7/8 |
| 3. Vector Stores | 2 | 2/2 | 2/2 | N/A | N/A | 13/13 | 7/8 |
| 4. Patterns | 7 | 7/7 | 7/7 | N/A | 1 (stuffit) | 13/13 | 7/8 |
| 5. Agents | 4 | 4/4 | 4/4 | N/A | N/A | 13/13 | 7/8 |
| 6. MCP | 6 | 6/6 | 6/6 | N/A | N/A | 13/13 | 7/8 |
| 7. Agentic | 4 | 2 (CLIs) | 4/4 | N/A | N/A | 13/13 | 7/8 |
| 8. Observability | 5 | 5/5 | 5/5 | N/A | N/A | 13/13 | 7/8 |
| **Total** | **52** | **50/52** | **52/52** | **14/14** | **8/8** | **13/13** | **8/8** |

**Notes:**
- **Anthropic:** No embedding API — stages 2-8 not applicable. All 14 chat endpoints pass.
- **AWS Bedrock:** Amazon Nova Lite (eu-central-1). The Converse starter is chat-only (no `EmbeddingModel` auto-config). Anthropic models on Bedrock require a use-case form.
- **Google:** 13/13 PASS with gemini-2.5-flash. Requires protobuf and okhttp version overrides in pom.xml (already applied).
- **Azure:** 8/8 PASS with gpt-4.1-mini in East US. Standard SKU required (not GlobalStandard).
- **Ollama:** 50/52 — agentic agent REST apps (Stage 7) require OpenAI (`OpenAiChatOptions`). Agentic CLIs work with any provider.

---

## Ollama (Local, No API Key — Optional)

### System requirements

| Model | Purpose | Parameters | RAM | Disk | Context |
|-------|---------|-----------|-----|------|---------|
| `qwen3` | Chat (default) | 8B | ~8 GB | 5.2 GB | 32k |
| `nomic-embed-text` | Embeddings (default) | 137M | ~1 GB | 274 MB | 8192 |
| `llava` | Multimodal — auto-used for chat_07 | 7B | ~8 GB | 4.7 GB | 4096 |
| `llama3.2` | Optional: faster chat for simple demos | 3B | ~4 GB | 2.0 GB | 128k |

**16 GB macOS:** `qwen3` + `nomic-embed-text` runs at ~10 GB active. `llava` loads on-demand for chat_07 only.

**Minimum system:** 8 GB RAM for qwen3 + nomic-embed-text. 16 GB recommended for running all models.

### Alternative: dockerized Ollama

If you can't or don't want to install Ollama natively, run it as a container
instead. The compose file in `docker/ollama/` mounts `models/ollama/` as the
model store, so the archives produced by `models/ollama.sh export` are
portable between the native and dockerized paths.

```bash
./workshop.sh infra ollama        # start container on port 11434
./workshop.sh status              # confirm: ollama:docker
```

Full reference — including CPU vs GPU, x86 vs arm64, and the airgapped
workflow — in [docs/ollama_dockerized.md](ollama_dockerized.md).

### Install and start

```bash
# macOS
brew install ollama

# Linux
curl -fsSL https://ollama.com/install.sh | sh

# Start Ollama
ollama serve   # runs on http://localhost:11434/

# Pull required models
ollama pull qwen3
ollama pull nomic-embed-text

# Optional
ollama pull llava        # multimodal (chat_07)
ollama pull llama3.2     # faster chat for simple demos
```

### Verify

```bash
ollama list
curl http://localhost:11434/api/tags
```

No `creds.yaml` needed — Ollama is fully local.

---

## OpenAI

Supports all 52 endpoints. Recommended for the full workshop experience.

### creds.yaml

File location: `applications/provider-openai/src/main/resources/creds.yaml` (gitignored)

```yaml
spring:
  ai:
    openai:
      api-key: sk-...your-key...
```

### Run

```bash
./mvnw spring-boot:run -pl applications/provider-openai \
  -Dspring-boot.run.profiles=pgvector,observation
```

---

## Anthropic

Supports all 14 chat endpoints. No embedding or vector store demos (no embedding API).

### creds.yaml

File location: `applications/provider-anthropic/src/main/resources/creds.yaml`

```yaml
spring:
  ai:
    anthropic:
      api-key: sk-ant-...your-key...
```

### Run

```bash
./mvnw spring-boot:run -pl applications/provider-anthropic \
  -Dspring-boot.run.profiles=observation
```

---

## Azure OpenAI

Supports chat, embeddings, tool calling. Requires Standard SKU (not GlobalStandard).

> **Spring AI 2.0.0-M5 note** — `spring-ai-azure-openai` was removed in M5. Azure OpenAI is now served by the unified `spring-ai-openai` module under the **Microsoft Foundry** label. Foundry mode is auto-detected when `spring.ai.openai.base-url` ends with `openai.azure.com` (or `cognitiveservices.azure.com`), or when a `deployment-name` is set. Config keys moved from `spring.ai.azure.openai.*` → `spring.ai.openai.*`. See `migration/SPRING_AI_M4_TO_M5_MIGRATION.md` for the full migration record.

Azure has more setup nuance than the other providers. The bullet points below capture every gotcha we hit during the M4 → M5 migration; if you skim only one section in this file, this is the one.

### Quick reference — what trips you up

| # | Gotcha | Symptom | Fix |
|---|---|---|---|
| 1 | Resource created without `--custom-domain` | `401 invalid subscription key or wrong API endpoint` even with a known-good key | Recreate the resource with `--custom-domain "$RESOURCE"`; the endpoint must be `https://<resource>.openai.azure.com/`, not a regional shared URL |
| 2 | `deployment-name` mismatch in `creds.yaml` | `404: The API deployment for this resource does not exist` | The `model:` in chat-options must equal your Azure deployment name (the SDK uses it verbatim as the URL deployment segment) |
| 3 | Embeddings 404 even though chat works | `404` only on `/embed/*` and `/vector/*` | Azure requires a *separate* deployment per model — deploy `text-embedding-3-small` independently |
| 4 | Default capacity is too low | `429 ... call rate limit for your current OpenAI S0 pricing tier` (or apparent "hangs" — see #5) | Use `--sku-capacity 30` for chat, `--sku-capacity 50` for embeddings; capacity is free, only rate-limits |
| 5 | Hangs lasting 1–3 minutes | Request never returns; eventually 429 | The openai-java SDK silently retries up to 3× with 60s timeout. Looks like a hang. Bumping capacity (#4) fixes it. |
| 6 | `Unbekannter Typ vector` / "Unknown type vector" SQL error | Stage 3 `/vector/01/load` fails after embeddings succeed | The pgvector Postgres extension isn't installed in the `azure` database. Flyway migration `V1__test.sql` ships in `provider-azure/src/main/resources/db/migration/` — make sure the Flyway deps + the script are present |
| 7 | "unknown" badge in dashboard topbar | Cosmetic but confusing | `spring.application.name: azure` must be set in `application.yaml` (also makes OTel `service.name` correct) |

### Azure resource provisioning (idempotent, copy-paste)

```bash
RG="spring-ai-workshop-rg"
RESOURCE="spring-ai-workshop-$(date +%s)"   # globally-unique; becomes the subdomain
LOCATION="eastus"

# 1. Resource group
az group create --name "$RG" --location "$LOCATION"

# 2. Azure OpenAI resource (note: --custom-domain is mandatory!)
az cognitiveservices account create \
  --name "$RESOURCE" --resource-group "$RG" --location "$LOCATION" \
  --kind OpenAI --sku S0 \
  --custom-domain "$RESOURCE" --yes

# 3. Chat deployment (capacity 30 = ~30k TPM — workshop-realistic)
az cognitiveservices account deployment create \
  --name "$RESOURCE" --resource-group "$RG" \
  --deployment-name gpt-41-mini \
  --model-name gpt-4.1-mini --model-version "2025-04-14" --model-format OpenAI \
  --sku-capacity 30 --sku-name Standard

# 4. Embedding deployment (capacity 50 = ~50k TPM — Stage 3 batches 70+ chunks at once)
az cognitiveservices account deployment create \
  --name "$RESOURCE" --resource-group "$RG" \
  --deployment-name text-embedding-3-small \
  --model-name text-embedding-3-small --model-version "1" --model-format OpenAI \
  --sku-capacity 50 --sku-name Standard

# 5. Pull endpoint + key1 (key never echoed)
ENDPOINT=$(az cognitiveservices account show --name "$RESOURCE" --resource-group "$RG" --query properties.endpoint -o tsv)
KEY=$(az cognitiveservices account keys list --name "$RESOURCE" --resource-group "$RG" --query key1 -o tsv)
echo "endpoint=$ENDPOINT"   # https://<resource>.openai.azure.com/
echo "key length=${#KEY}"   # ~84 chars
```

**Tear-down when done:** `az group delete --name "$RG" --yes --no-wait` (deletes resources + deployments).
**Rotate key1:** `az cognitiveservices account keys regenerate --name "$RESOURCE" --resource-group "$RG" --key-name key1`. Keys are resource-level (not deployment-level), so this doesn't break deployments.

### creds.yaml

File location: `applications/provider-azure/src/main/resources/creds.yaml` (gitignored).

```yaml
spring:
  ai:
    openai:
      api-key: ...your-key...
      base-url: https://your-resource.openai.azure.com/
      chat:
        options:
          deployment-name: gpt-41-mini
          model: gpt-41-mini              # MUST match your Azure deployment name
      embedding:
        options:
          deployment-name: text-embedding-3-small
          model: text-embedding-3-small   # MUST match your embedding deployment name
```

> **Why `model` repeats `deployment-name`:** the `openai-java` SDK reads the request body's `model` field and uses it verbatim as the URL deployment segment for Azure. So in Foundry mode, the chat-options `model` is **the deployment name**, not the underlying model identifier (e.g. it's `gpt-41-mini`, not `gpt-4.1-mini`).

### URL construction (for debugging)

The actual URL the SDK calls in Foundry mode is:

```
<base-url>/openai/deployments/<MODEL_FROM_REQUEST>/chat/completions?api-version=<version>
```

- `<base-url>` ← `spring.ai.openai.base-url` (host portion only matters for routing — must end with `openai.azure.com` for auto-detection).
- `<MODEL_FROM_REQUEST>` ← the chat-options `model` field. **This is your Azure deployment name.**
- `<version>` ← defaults to `AzureOpenAIServiceVersion.latestStableVersion()` (currently `2024-10-21`). Override with `spring.ai.openai.microsoft-foundry-service-version: 2024-10-21` (or any value from `com.openai.azure.AzureOpenAIServiceVersion`).

To debug a 404 from Azure, reproduce the SDK's URL with a direct curl:

```bash
curl -i \
  -H "api-key: $KEY" -H "Content-Type: application/json" \
  -d '{"messages":[{"role":"user","content":"ping"}],"max_tokens":5}' \
  "${ENDPOINT}openai/deployments/<deployment-name>/chat/completions?api-version=2024-10-21"
```

- `200` → Azure side is fine; check `creds.yaml` matches the values you used in curl.
- `404 The API deployment for this resource does not exist` → wrong `deployment-name` or wrong subdomain.
- `401 invalid subscription key or wrong API endpoint` → wrong endpoint (probably the regional shared URL — see #1) or wrong key.
- `429` → capacity too low (see #4/#5).

### Run

```bash
./mvnw spring-boot:run -pl applications/provider-azure \
  -Dspring-boot.run.profiles=pgvector,observation,ui
```

First run with the `pgvector` profile: Flyway will execute `db/migration/V1__test.sql` to install the `vector`/`hstore`/`uuid-ossp` extensions and create the `vector_store` table in the `azure` database. If you see `Migrating schema "public" to version 1` in the log, you're good.

### Models

- **Chat:** `gpt-4.1-mini` (deployed as `gpt-41-mini`); `gpt-4.1`, `gpt-4o`, `gpt-5` available with separate deployments.
- **Embeddings:** `text-embedding-3-small` (1536-dim, matches `pgvector.dimension: 1536`); `text-embedding-3-large` (3072-dim) requires bumping the pgvector dimension.
- **Tool calling:** Yes (provider-agnostic via Spring AI).
- **Image / TTS / Whisper:** require their own deployments; not exercised by the workshop currently.

### Notes

- **Custom subdomain is mandatory** — without `--custom-domain` Azure gives you a regional shared URL (e.g. `https://eastus.api.cognitive.microsoft.com/`) that openai-java doesn't recognize as Azure mode. The fix is to delete and recreate the resource with `--custom-domain "$RESOURCE"`.
- **`Standard` SKU only** — `GlobalStandard` is not supported on the workshop's free-tier-friendly path. The deployment SKU (`--sku-name Standard`) is independent of the resource SKU (`--sku S0`).
- **Capacity is free** — Azure OpenAI Standard is pay-per-token regardless of capacity. Higher capacity only raises rate-limit ceilings; it doesn't add a fixed cost. The constraint is per-region quota across deployments.
- **Capacity isn't editable** — `az cognitiveservices account deployment update` doesn't accept `--sku-capacity`. To change it, delete and recreate the deployment. Vector data already in pgvector is preserved (1536-dim floats aren't bound to the deployment that produced them).

---

## Google (Gemini / Vertex AI)

Supports chat, embeddings, tool calling. Requires API key or GCP service account.

### creds.yaml

File location: `applications/provider-google/src/main/resources/creds.yaml`

```yaml
spring:
  ai:
    google:
      genai:
        project-id: your-gcp-project
        location: us-central1
        # Or use API key mode:
        # api-key: ...your-key...
```

### Run

```bash
./mvnw spring-boot:run -pl applications/provider-google \
  -Dspring-boot.run.profiles=pgvector,observation
```

---

## AWS Bedrock

Chat-only provider (no EmbeddingModel auto-config). Amazon Nova Lite works without any approval form. Anthropic models on Bedrock require submitting a use-case form.

### Credentials

Uses AWS CLI credentials — no `creds.yaml`:

```bash
aws configure
# or set environment variables:
export AWS_ACCESS_KEY_ID=...
export AWS_SECRET_ACCESS_KEY=...
export AWS_REGION=eu-central-1
```

### Run

```bash
./mvnw spring-boot:run -pl applications/provider-aws \
  -Dspring-boot.run.profiles=observation
```

---

## Docker Infrastructure

### PostgreSQL with pgvector

Required for the `pgvector` profile.

```bash
docker compose -f docker/postgres/docker-compose.yaml up -d
```

| Service | Port | Credentials |
|---------|------|------------|
| PostgreSQL 18 (pgvector) | 15432 | postgres / password |
| pgAdmin | 15433 | admin@example.com / admin |

Databases pre-created by init script: `openai`, `azure`, `ollama`

### Observability stack (LGTM)

Required for the `observation` profile.

```bash
docker compose -f docker/observability-stack/docker-compose.yaml up -d
```

| Service | Port | Purpose |
|---------|------|---------|
| Grafana | 3000 | Dashboards, trace viewer, log explorer |
| OTLP gRPC | 4317 | Telemetry ingestion (gRPC) |
| OTLP HTTP | 4318 | Telemetry ingestion (HTTP) |
| MailDev Web UI | 1080 | Email testing interface |
| MailDev SMTP | 1025 | SMTP server for test emails |

### Docker resource allocation

Recommended Docker Desktop settings:

- CPU: 4+ cores
- Memory: 8 GB minimum (16 GB recommended when running Ollama + Docker simultaneously)
- Disk: 20 GB for images and volumes

---

## IDE Setup

### IntelliJ IDEA

- Version: 2025.1 or later (Spring Boot 4 support)
- SDK: JDK 25
- Import as Maven project from root `pom.xml`
- Maven: bundled or 3.9.14

### VS Code

- Extensions: Extension Pack for Java, Spring Boot Extension Pack
- Set `java.configuration.runtimes` to point to JDK 25
- Maven for Java extension handles multi-module builds
