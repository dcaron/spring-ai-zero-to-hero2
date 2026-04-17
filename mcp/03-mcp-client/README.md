> **✨ New in Stage 6 UI:** This demo is runnable from the workshop dashboard at [**/dashboard/stage/6**](http://localhost:8080/dashboard/stage/6).
>
> Start the demo's server (where applicable) via:
>
> ```bash
> ./workshop.sh mcp start <id>   # 02, 04, or 05
> ./workshop.sh mcp build-01     # for 01 STDIO
> ./workshop.sh mcp status
> ```
>
> See [`docs/spring-ai/SPRING_AI_STAGE_6.md`](../../docs/spring-ai/SPRING_AI_STAGE_6.md) for the full walkthrough.

---

# MCP Client (03)

Demonstrates a Spring AI MCP client that connects to MCP servers via STDIO and HTTP, and uses discovered tools through `ChatClient`. Two modes:

- **Local (default):** connects to 01 (STDIO jar) + 02 (HTTP on :8081).
- **External (`mcp-external` profile):** connects to Brave Search + filesystem MCP servers via `npx`. Requires `BRAVE_API_KEY`.

See `src/main/resources/README.md` for internal configuration details.
