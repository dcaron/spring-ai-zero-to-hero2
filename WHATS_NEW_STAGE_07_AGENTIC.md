# What's New — Stage 7: Agentic Systems

> A walkthrough of the Stage 7 chapter for workshop attendees and trainers.
> Covers what agentic systems are, how the two demos work, how to run them from the dashboard **or** the CLI, and the OpenAI vs Ollama trade-offs.

## TL;DR

Stage 7 used to be CLI-only (Spring Shell + separate agent apps). Now the same two demos (01 Inner Monologue, 02 Model-Directed Loop) are **runnable, inspectable, and chattable from the workshop dashboard** at `http://localhost:8080/dashboard/stage/7`, with a single coordinated lifecycle script and **Ollama support alongside OpenAI**.

> **Architecture note:** The Stage 7 agents are **standalone Spring Boot apps running as separate processes** on `:8091` and `:8092` — they are NOT embedded in any provider app's JVM. The dashboard on `:8080` proxies to them over HTTP. The legacy "all-in-one on :8080" mode has been removed (commit `c0759a6`).

```bash
./workshop.sh start ollama ui                           # provider app with dashboard
./workshop.sh agentic start all                         # starts 01 (:8091) + 02 (:8092) on OpenAI
./workshop.sh agentic start all --provider=ollama       # or on Ollama
open http://localhost:8080/dashboard/stage/7
```

Two cards with live status pills, provider pills (openai/ollama), model badges, inline chat consoles, multi-step trace rendering for demo 02, live curl lines, ACME login for demo 02, and ⚠ fallback badges when a local model can't drive the agentic pattern.

---

## What is an agentic system?

An **agentic system** is an AI application where the model controls its own execution flow. Unlike stages 1–6 — where the developer defines the exact sequence of calls — an agentic system lets the model decide what to do next, when to use tools, and when to stop.

Stage 7 demonstrates two patterns, both built on a key technique: **forced tool calling** via `toolChoice("required")`, which guarantees the model responds through a structured tool rather than free-form text.

| Pattern | Steps | Mechanism |
|---|---|---|
| **Inner Monologue** | Single call | Model always invokes `send_message(message, innerThoughts)` — the inner thoughts are private reasoning |
| **Model-Directed Loop** | Multi-step, up to 5 | Model invokes `send_message(message, innerThoughts, requestReinvocation)` — the boolean decides "keep thinking or stop" |

---

## The two demos at a glance

| # | Module | What it shows | Port | Runs on |
|---|---|---|---|---|
| **01** | `agentic-system/01-inner-monologue/` | Single-step agent with visible inner thoughts | 8091 | OpenAI or Ollama |
| **02** | `agentic-system/02-model-directed-loop/` | Multi-step agent with a reasoning trace + ACME login | 8092 | OpenAI or Ollama |

---

## Recommended walkthrough

### 1. Start everything

```bash
./workshop.sh start openai ui        # provider + dashboard
./workshop.sh agentic start all      # 01 + 02, profile=openai,observation by default
./workshop.sh agentic status         # confirm both up
```

Open `http://localhost:8080/dashboard/stage/7` — two cards, both green pills, `openai` + `gpt-4o-mini` visible.

### 2. Demo 01 — Inner Monologue

> **Concept to land:** The model *always* calls `send_message`. Every response has `message` (user-facing) and `innerThoughts` (private) — no free-form text escapes the tool.

1. Click **Open console** on card 01. Left rail: empty agent list. Create an agent `alice`.
2. Send: `What is 2+2?`
3. Observe: a bubble with `4`, and a collapsed `▸ inner thoughts` disclosure. Expand it — "Simple arithmetic" or similar.
4. The live curl line below shows the exact POST that happened — copy it, paste in a terminal, same response.

### 3. Demo 02 — Model-Directed Loop

> **Concept to land:** The model explicitly controls the loop. `requestReinvocation: true` = "give me another tick", `false` = "I'm done".

1. Open console on card 02. Create an agent `planner`.
2. Send: `Plan a trip to Berlin`.
3. Watch: 2–4 bubbles appear in sequence, each with a `step N/M · reinvoke=true` badge. The last step has `reinvoke=false` — the model decided to stop.
4. Each bubble has its own collapsed inner thoughts.

### 4. Switch to Ollama

```bash
./workshop.sh agentic stop all
./workshop.sh agentic start all --provider=ollama
```

Provider pill turns orange (`ollama`), model badge becomes `qwen3` (or whatever you configured).

1. Redo the demo 01 query — works reliably on qwen3.
2. Switch to `llama3.2:3b`: stop, edit the YAML override or set `AGENT_OLLAMA_MODEL=llama3.2:3b`, start again. Occasionally the fallback fires: bubble gets a ⚠ amber badge, inner thoughts say `[fallback: model replied without tool]`.
3. Try demo 02 on `llama3.2:3b`: one bubble with the ⚠ fallback badge, the loop stops immediately (per design, to keep demos clean).

### 5. ACME login

On card 02, right-rail form: enter one of the seeded emails (see `components/data/...`). Returns `Welcome, <customer>!` — same as CLI `login` command.

### 6. Observability in Grafana

Open `http://localhost:3000` (default credentials `admin`/`admin`). Tempo datasource → search `service.name=inner-monologue-agent`. Pick a trace — waterfall shows dashboard proxy → agent → ChatClient → OpenAI. Click on `ChatClient` span → attributes include the prompt and inner thoughts.

### 7. Gateway spy (both providers)

```bash
./workshop.sh agentic stop all
./workshop.sh agentic start all --provider=openai,spy,observation
```

Visit the gateway at `:7777` and tail its audit log — each message generates an audit entry. Same with `--provider=ollama,spy,observation` — the existing `/ollama/**` gateway route captures Ollama traffic too.

### 8. Stop everything

```bash
./workshop.sh agentic stop all
./workshop.sh stop
```

---

## UI vs CLI — two equivalent workflows

| Task | Dashboard | CLI |
|---|---|---|
| Start 01 | N/A — must be running | `./workshop.sh agentic start 01` |
| Create agent | Card 01 → **Open console** → Create form | `create --id alice` in Spring Shell |
| Send message | Chat input in console | `send "..."` in Spring Shell |
| See inner thoughts | Click `▸ inner thoughts` disclosure | Printed inline with `[THOUGHT]` marker |
| See multi-step trace | Each step is a bubble with step badge | Each step printed with `[THOUGHT]` / `[AGENT]` |
| ACME login (02) | Right-rail form | `login email@acme.com` in Spring Shell |
| Reset memory | **Reset memory** button | Not in current CLI |
| Show log | `GET /log` via clicking left-rail entry | `log` in Spring Shell |
| Stop | N/A | `./workshop.sh agentic stop 01` |

Every console has a live curl line below the input — edit the message, the curl rebuilds, Copy puts it on the clipboard.

---

## Manual verification checklist

1. `./workshop.sh agentic start all` → cards green, provider pill `openai`, model badge `gpt-4o-mini`
2. Card 01: create agent → left rail updates
3. Send "What is 2+2?" → bubble with inner-thoughts disclosure
4. `./workshop.sh agentic stop all && agentic start all --provider=ollama` → provider pill becomes `ollama`
5. Send on `qwen3` → works; switch to `llama3.2:3b` → fallback badge appears
6. Card 02 — send "Plan a trip to Berlin" → multi-step trace with step badges
7. Demo 02 + ollama + `llama3.2:3b` → clean single-bubble fallback
8. Login on card 02 → "Welcome, ..."
9. Grafana `:3000` → Tempo → filter `service.name=inner-monologue-agent` → trace visible
10. Activate `spy` profile → traffic appears at `:7777`
11. `./workshop.sh agentic stop all` → cards gray, offline hints shown

---

## Trainer notes

### Time budget (~35 min)

| Phase | Minutes |
|---|---|
| Start infra + agents | 2 |
| Demo 01 on OpenAI | 5 |
| Demo 02 on OpenAI + trace | 7 |
| Switch to Ollama / qwen3 | 4 |
| Show llama3.2 fallback (both demos) | 5 |
| Grafana / spy tour | 7 |
| Q&A | 5 |

### Common questions

- **"Why does `llama3.2:3b` skip the tool sometimes?"** — Because Ollama doesn't support `toolChoice("required")`. The system prompt asks the model to always use the tool, but there's no enforcement. Smaller models lose attention. The fallback marker makes this visible rather than silent.
- **"Why does demo 02 stop on a single bubble with llama3.2?"** — We force `requestReinvocation=false` on fallback so attendees don't see 5 garbage bubbles before `MAX_STEP_COUNT` catches it.
- **"Why isn't `llava` in the model list?"** — It's a vision model. It answers chats but has no tool-calling support. Good reminder that "chat-capable" ≠ "tool-capable".
- **"Can I run agents on Anthropic or Bedrock?"** — Not yet. `toolChoice("required")` is OpenAI-specific. Spring AI's provider-agnostic `ToolCallingChatOptions` will eventually enable this; tracked as future work.

### Pitfalls to avoid

- **Don't** forget to pull the Ollama model first: `ollama pull qwen3`.
- **Don't** start the dashboard on a non-8080 port — dashboard JS assumes `http://localhost:8080`.
- **Do** start `./workshop.sh agentic start all` before opening the dashboard — status pills will show gray for the first 3s polling interval otherwise.

---

## Troubleshooting quick reference

| Symptom | Likely cause | Fix |
|---|---|---|
| Card pill stays gray | Agent not running | `./workshop.sh agentic start 01` |
| `Port 8091 already in use` | Stale process | `lsof -ti:8091 \| xargs kill` |
| `provider-error` on send | OpenAI key missing or Ollama not running | Check `creds.yaml` or `ollama list` |
| Fallback badge on every message | Using a non-tool-capable model | Switch to `qwen3` or OpenAI |
| Demo 02 stops immediately on Ollama | Fallback handler (by design) | Use a stronger model |
| Traces not in Grafana | LGTM stack not up or `observation` profile inactive | `docker compose -f docker/observability-stack/docker-compose.yaml up -d` |
| Spy gateway has no `/ollama` route | Old gateway build | `./mvnw -pl applications/gateway package` |

Full section: `docs/troubleshooting.md § Stage 7 / Agentic`.

---

## Deeper reading

| Resource | Purpose |
|---|---|
| [`docs/spring-ai/SPRING_AI_STAGE_7.md`](docs/spring-ai/SPRING_AI_STAGE_7.md) | Canonical deep dive, endpoint inventory, model matrix, fallback behavior |
| [`docs/guide.md`](docs/guide.md) — Stage 7 section | Quick reference + `workshop.sh agentic` commands |
| [`agentic-system/*/readme.md`](agentic-system/) | Per-module quickstart |
| [`docs/troubleshooting.md`](docs/troubleshooting.md) | Stage 7 troubleshooting |
