# 01 — Inner Monologue Agent

> **New in Stage 7 UI (2.3.1):** This demo now runs on dedicated port `:8091` and is accessible from the dashboard at `/dashboard/stage/7`. Supports OpenAI (default) and Ollama via `./workshop.sh agentic start 01 --provider=ollama`. See [WHATS_NEW_STAGE_07_AGENTIC.md](../../WHATS_NEW_STAGE_07_AGENTIC.md) for the attendee/trainer walkthrough. Runs as a **standalone Spring Boot app** on `:8091` — no longer bundled with any provider app.

## Overview

Demonstrates the **inner monologue + tool calling** pattern — a foundational agent design where the model first emits private reasoning before taking action. Every user-facing reply is produced via a forced `send_message` tool call, establishing a cognitive flow of **perception → reasoning → action** that makes each response a transparent, explainable decision.

## Modules

| Module | Purpose | Port |
|---|---|---|
| [`inner-monologue-agent`](inner-monologue-agent/README.md) | REST API (Spring Boot) — `/agents/inner-monologue/**` | 8091 |
| [`inner-monologue-cli`](inner-monologue-cli/README.md) | Spring Shell CLI that drives the REST API | — |

## Run

```bash
# Start the agent app (OpenAI default)
./workshop.sh agentic start 01

# Or with Ollama
./workshop.sh agentic start 01 --provider=ollama

# Status / logs / stop
./workshop.sh agentic status
./workshop.sh agentic logs 01
./workshop.sh agentic stop 01
```

Then open the Stage 7 dashboard at <http://localhost:8080/dashboard/stage/7> and drive the agent from the card's inline chat console, or use the CLI.

## Key endpoints

```
POST   /agents/inner-monologue/{id}            Create agent
POST   /agents/inner-monologue/{id}/messages   Send message (forced tool call)
GET    /agents/inner-monologue/{id}            Get agent state
POST   /agents/inner-monologue/{id}/reset      Clear memory
GET    /agents/inner-monologue/{id}/log        Inspect message history
```

## Further reading

- Attendee + trainer walkthrough: [`WHATS_NEW_STAGE_07_AGENTIC.md`](../../WHATS_NEW_STAGE_07_AGENTIC.md)
- Deep dive: [`docs/spring-ai/SPRING_AI_STAGE_7.md`](../../docs/spring-ai/SPRING_AI_STAGE_7.md)
