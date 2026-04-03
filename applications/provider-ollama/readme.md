# Ollama (Local)

**Spring Boot 4.0.5 | Spring AI 2.0.0-M4 | spring-ai-starter-model-ollama**

Runs entirely locally with no API keys needed.

## Setup

Install [Ollama](https://ollama.com/) and pull the required models:

```bash
ollama pull mistral             # Chat model (7B, default)
ollama pull nomic-embed-text    # Embedding model (768 dims, 8192 ctx)
ollama pull llava               # Multimodal model (auto-used for chat_07)
```

Start Ollama: `ollama serve`

## Run

```bash
./mvnw spring-boot:run -pl applications/provider-ollama \
  -Dspring-boot.run.profiles=pgvector,observation
```

Models are configured in `src/main/resources/application.yaml`.
All 44 workshop endpoints pass with Ollama.
