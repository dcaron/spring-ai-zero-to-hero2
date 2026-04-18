# 02 — Model-Directed Loop Agent

> **New in Stage 7 UI (2.3.1):** This demo now runs on dedicated port `:8092` and is accessible from the dashboard at `/dashboard/stage/7`. The **ACME login** flow and the **multi-step trace rendering** both work from the UI. Supports OpenAI (default) and Ollama. When a weak local model skips the `send_message` tool, the `AgentFallbackHandler` forces `requestReinvocation=false` so the loop stops cleanly on a single bubble (see [fallback behavior](../../docs/spring-ai/SPRING_AI_STAGE_7.md#ollama-fallback-behavior)). See [WHATS_NEW_STAGE_07_AGENTIC.md](../../WHATS_NEW_STAGE_07_AGENTIC.md) for the walkthrough. Runs as a **standalone Spring Boot app** on `:8092` — no longer bundled with any provider app.

## Overview

Extends the inner-monologue pattern by giving the **model** control over whether the agent loop continues. Each turn, the model emits a `requestReinvocation` signal alongside its `send_message` call: the runtime then either reinvokes the model with fresh context or exits. This is the first example where the model drives genuine multi-step behavior — turning inner monologue + tool calls into a full agent loop with a visible trace.

## Modules

| Module | Purpose | Port |
|---|---|---|
| [`model-directed-loop-agent`](model-directed-loop-agent/) | REST API (Spring Boot) — `/agents/model-directed-loop/**` | 8092 |
| [`model-directed-loop-cli`](model-directed-loop-cli/README.md) | Spring Shell CLI that drives the REST API | — |

## Run

```bash
# Start the agent app (OpenAI default)
./workshop.sh agentic start 02

# Or with Ollama
./workshop.sh agentic start 02 --provider=ollama

# Status / logs / stop
./workshop.sh agentic status
./workshop.sh agentic logs 02
./workshop.sh agentic stop 02
```

Then open the Stage 7 dashboard at <http://localhost:8080/dashboard/stage/7>. The card's inline chat console renders the **multi-step trace** returned by the agent and supports the **ACME login** flow end-to-end.

## Key endpoints

```
POST   /agents/model-directed-loop/{id}            Create agent
POST   /agents/model-directed-loop/{id}/messages   Send message (returns ChatTraceResponse)
GET    /agents/model-directed-loop/{id}            Get agent + trace
POST   /agents/model-directed-loop/{id}/reset      Clear memory
GET    /agents/model-directed-loop/{id}/log        Inspect message history
```

## Fallback behavior

When a weak local model (typical with smaller Ollama models) replies without invoking `send_message`, the `AgentFallbackHandler` wraps the free-form reply with a `[fallback: model replied without tool]` marker **and** forces `requestReinvocation=false` so the loop stops cleanly on a single bubble. This is by design (spec decision D7). To see full multi-step traces, run with OpenAI or a stronger Ollama model such as `qwen3`.

## Further reading

- Attendee + trainer walkthrough: [`WHATS_NEW_STAGE_07_AGENTIC.md`](../../WHATS_NEW_STAGE_07_AGENTIC.md)
- Deep dive incl. fallback anchor: [`docs/spring-ai/SPRING_AI_STAGE_7.md`](../../docs/spring-ai/SPRING_AI_STAGE_7.md)
