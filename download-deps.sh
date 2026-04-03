#!/bin/bash

echo "========================================"
echo "Downloading dependencies for Spring AI"
echo "Zero-to-Hero Workshop"
echo "========================================"
echo ""

echo "--- Pulling Ollama models ---"
ollama pull mistral
ollama pull nomic-embed-text
ollama pull llava
echo ""

echo "--- Pulling Docker images ---"
docker compose -f docker/postgres/docker-compose.yaml pull
docker compose -f docker/observability-stack/docker-compose.yaml pull
echo ""

echo "--- Building Maven project ---"
./mvnw clean compile -T 4
echo ""

echo "========================================"
echo "All dependencies downloaded."
echo "Run ./check-deps.sh to verify."
echo "========================================"
