# 01 - Inner Monologue Agent

> **Standalone Spring Boot app.** This module is a **separate Spring Boot application** that runs in its own JVM on port `:8091`. It is **not** embedded into any provider app (provider-openai, provider-ollama, etc.) and does not use port `:8080`. Launch it with `./workshop.sh agentic start 01` (recommended) or `./mvnw spring-boot:run -pl agentic-system/01-inner-monologue/inner-monologue-agent`. The dashboard at `http://localhost:8080/dashboard/stage/7` proxies to this app over HTTP.

This project allows you to interact with an AI agent that uses private inner thoughts before responding with a message. All interactions go through a tool call named `send_message`.

The agent server runs separately and manages the lifecycle of agents. The CLI connects to it and provides a stateful shell for creating and talking to agents.

---

## 🎯 Target Management

Before interacting with an agent, you need to select the active agent system (called a “target”).

```bash
target list
```

This shows all available agent systems, such as:

```
inner-monologue -> /agents/inner-monologue
```

Then select the `inner-monologue` target:

```bash
target use --name inner-monologue
```

You can check your current target at any time:

```bash
target status
```

---

## 🤖 Agent Lifecycle and Interaction

### Create an Agent

To create a new agent with a randomly chosen name:

```bash
agent create
```

Or create one with a specific ID:

```bash
agent create --id neo
```

### Switch Between Agents

```bash
agent list
agent switch --id neo
```

### Send a Message

Once an agent is active, you can talk to it:

```bash
agent send "What's the weather like today?"
```

Example output:

```
[USER] What's the weather like today?
[THOUGHT] Check the weather and respond briefly.
[AGENT] It's currently 22°C and sunny.
```

You can view the chat history at any time:

```bash
agent log
```

And check which agent is currently active:

```bash
agent status
```
