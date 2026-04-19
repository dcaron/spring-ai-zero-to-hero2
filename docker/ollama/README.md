# Dockerized Ollama (optional)

An alternative to a host-installed Ollama for the Spring AI Zero-to-Hero workshop.
Intended for attendees without Ollama installed (including airgapped rooms).

**Performance note:** macOS containers cannot access Metal — dockerized Ollama
is always CPU-only there. Native Ollama.app is typically 3–10× faster on
Apple Silicon for chat workloads.

**Start:**

```
docker compose -f docker/ollama/docker-compose.yaml up -d
```

**Verify:**

```
curl http://localhost:11434/api/tags
docker exec ollama ollama list
```

See [docs/ollama_dockerized.md](../../docs/ollama_dockerized.md) for the full
reference: model loading (online + airgapped), GPU overlay, CPU vs arm64 vs
x86 notes, basic commands, and troubleshooting.
