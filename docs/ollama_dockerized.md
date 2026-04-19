# Ollama — Dockerized Alternative

A step-by-step reference for attendees who want to run Ollama in a container
instead of installing it natively. This keeps the workshop fully runnable
without admin rights and works in airgapped rooms with only the two portable
archives (`models/models.tar.gz` and `models/containers.tar.gz`).

---

## 1. What is Ollama

[Ollama](https://ollama.com/) is a local LLM runtime. It serves an
OpenAI-compatible REST API on `http://localhost:11434/`, stores models in
GGUF format under a single models directory, and pulls images from a registry
at `registry.ollama.ai`. The workshop's `provider-ollama` application connects
to this API; it does not care whether Ollama runs natively or in a container
as long as port 11434 responds.

---

## 2. How dockerized Ollama works in this workshop

The compose file `docker/ollama/docker-compose.yaml`:

- Uses the multi-arch image `ollama/ollama:latest`.
- Binds host port `11434` (same as native Ollama).
- Mounts the repo's `models/ollama/` directory into the container at
  `/root/.ollama/models`. This is the same layout the native Ollama uses,
  so the tarball produced by `models/ollama.sh export` is portable between
  native and dockerized installs.
- Container name is fixed as `ollama` so `workshop.sh` can detect it.

### Three-state runtime indicator

`workshop.sh` reports one of three states in its header and `status` command:

| State | Meaning | How it's detected |
|---|---|---|
| `ollama:docker` | The `ollama` container is running | `docker ps` lists a container named `ollama` |
| `ollama:local`  | The host Ollama responds on `:11434` | `curl http://localhost:11434/api/tags` returns 200 |
| `ollama:off`    | Neither is running | Neither check matches |

Docker is checked first because both modes share port 11434.

---

## 3. Getting models into the container

Three supported paths, all driven by `models/ollama.sh import`:

| Target | Source | Requires | Command |
|---|---|---|---|
| 1 `ollama`      | `models/models.tar.gz` | Native Ollama installed | `./ollama.sh import --target=ollama` |
| 2 `docker`      | `models/models.tar.gz` | None (offline-friendly) | `./ollama.sh import --target=docker` |
| 3 `docker-pull` | Ollama registry (online) | `ollama` container running, internet access | `./ollama.sh import --target=docker-pull` |

Run `./ollama.sh import` (no args) for an interactive prompt.

**Target 1** is the traditional path for an attendee who installed Ollama
natively and just wants to restore workshop models without pulling each from
the registry.

**Target 2** is the offline/airgapped path: extract the tarball directly into
`models/ollama/`, then start the container. The volume mount picks up the
models immediately — no `pull` needed.

**Target 3** is the online convenience path: with the container already
running, it runs `docker exec ollama ollama pull <model>` for every entry
in `WORKSHOP_MODELS` and reports per-model success/failure.

---

## 4. Architecture & performance

### x86_64 vs arm64

`ollama/ollama` is multi-arch; Docker selects the right manifest for your
host. Indicative token/s on `qwen3:8b`, CPU-only chat workload:

| Host | Native | Dockerized |
|---|---|---|
| Apple Silicon M-series (arm64, macOS) | 25–60 tok/s (Metal) | 6–12 tok/s (CPU-only — container can't reach Metal) |
| x86_64 + NVIDIA (Linux, Container Toolkit) | 30–80 tok/s (CUDA) | 30–80 tok/s (with GPU overlay) |
| x86_64 CPU-only (Linux / Windows WSL2) | 2–5 tok/s | 2–5 tok/s |

Numbers vary wildly with model size, prompt length, and hardware. Treat them
as ballparks, not benchmarks.

### GPU vs CPU

**macOS (arm64 or x86_64).** Containers have no access to Metal or AMD GPUs.
Dockerized Ollama is always CPU-only on macOS, typically 3–10× slower than
native `Ollama.app`. This is the primary reason native is recommended on
Apple Silicon.

**Linux + NVIDIA.** Install the [NVIDIA Container
Toolkit](https://docs.nvidia.com/datacenter/cloud-native/container-toolkit/latest/install-guide.html)
on the host, then add the GPU overlay when starting:

```bash
docker compose -f docker/ollama/docker-compose.yaml \
               -f docker/ollama/docker-compose.gpu.yaml up -d
```

`workshop.sh` auto-detects `nvidia-smi` on `PATH` and the `nvidia` Docker
runtime; when both are present, it appends the overlay automatically. Force
on or off with `WORKSHOP_OLLAMA_GPU=1` / `WORKSHOP_OLLAMA_GPU=0`.

**Linux arm64.** CPU-only unless you're on an NVIDIA Jetson with the
proprietary runtime.

### When dockerized wins

- Airgapped workshop rooms — ship the two `.tar.gz` archives, skip all
  downloads.
- Managed laptops without admin rights to install Ollama.
- Reproducibility — the same image everyone uses.
- Attendees who don't want to install anything on their host.

### When native wins

- Apple Silicon — Metal gives a large performance margin.
- Day-to-day use outside the workshop.

---

## 5. Basic commands

Start / stop:

```bash
docker compose -f docker/ollama/docker-compose.yaml up -d
docker compose -f docker/ollama/docker-compose.yaml down
```

List / pull / run from the container:

```bash
docker exec ollama ollama list
docker exec ollama ollama pull qwen3
docker exec -it ollama ollama run qwen3
```

Tail logs:

```bash
docker logs -f ollama
```

Verify from host:

```bash
curl http://localhost:11434/api/tags
```

Inside `workshop.sh`:

```bash
./workshop.sh infra ollama           # start container
./workshop.sh status                 # includes three-state indicator
./workshop.sh stop <<< "y"           # y at the Docker prompt also stops ollama
./workshop.sh start ollama --profiles pgvector --ollama-docker
```

---

## 6. Troubleshooting

**Port 11434 is already in use.** Native Ollama is running. Quit it (macOS:
right-click the Ollama menu-bar icon → Quit) before `docker compose up -d`.
The workshop only ever talks to one Ollama at a time on port 11434.

**Container is up but `docker exec ollama ollama list` is empty.** The mount
isn't picking up models:

1. Confirm the host directory has content: `ls models/ollama/manifests`.
2. Re-create the container so the mount is re-read: `docker compose -f
   docker/ollama/docker-compose.yaml down && ... up -d`.
3. On SELinux (Fedora/RHEL), add `:z` to the volume or relabel:
   `chcon -Rt svirt_sandbox_file_t models/ollama`.
4. On rootless Docker, `models/ollama/` needs to be owned by your user —
   check `ls -ld models/ollama`.

**Very slow first response.** The container lazy-loads the model into RAM
on the first request (can take 30+ seconds for 8B models). Subsequent
requests reuse the loaded model until it's evicted.

**Disk space.** `qwen3` + `nomic-embed-text` + `llava` is roughly 10 GB.
Ensure your Docker disk image has headroom (Docker Desktop → Settings →
Resources → Disk image size).

**Switching between native and dockerized.** Only one should own port 11434
at a time. The workshop's status line shows which is active so you don't
have to guess.
