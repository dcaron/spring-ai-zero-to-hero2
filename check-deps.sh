#!/bin/bash

# ========================================
# Dependency checker for Spring AI
# Zero-to-Hero Workshop
#
# Tech Stack: Spring Boot 4.0.5 | Spring AI 2.0.0-M4 | Java 25
# ========================================

check_java() {
    echo "============================"
    echo "Checking Java installation:"
    echo "============================"
    if command -v java &> /dev/null
    then
        JAVA_VERSION=$(java -version 2>&1 | head -1)
        echo "✅ Java is installed. Version details:"
        java -version
        # Check minimum version (25+)
        MAJOR=$(java -version 2>&1 | head -1 | sed 's/.*"\([0-9]*\)\..*/\1/')
        if [ "$MAJOR" -ge 25 ] 2>/dev/null; then
            echo "✅ Java version $MAJOR meets minimum requirement (25+)."
        else
            echo "⚠️  Java version $MAJOR detected. Spring Boot 4.0.5 requires Java 25+."
        fi
    else
        echo "❌ Java is not installed. Install via: sdk install java 25-open"
    fi
    echo ""
}

check_ollama() {
    echo "==============================="
    echo "Checking Ollama installation:"
    echo "==============================="
    if command -v ollama &> /dev/null
    then
        echo "✅ Ollama is installed. Version details:"
        ollama --version
    else
        echo "❌ Ollama is not installed. Install from https://ollama.com/"
    fi
    echo ""
}

check_ollama_model() {
    local model=$1
    local purpose=$2
    echo "========================================"
    echo "Checking if $model model is pulled:"
    echo "========================================"
    if command -v ollama &> /dev/null
    then
        if ollama list 2>/dev/null | grep -q "$model"
        then
            echo "✅ $model model is pulled and available. ($purpose)"
        else
            echo "❌ $model model is not pulled. Run: ollama pull $model"
        fi
    else
        echo "❌ Ollama is not installed, cannot check $model model."
    fi
    echo ""
}

check_docker() {
    echo "=============================="
    echo "Checking Docker installation:"
    echo "=============================="
    if command -v docker &> /dev/null
    then
        echo "✅ Docker is installed. Version details:"
        docker --version
    else
        echo "❌ Docker is not installed."
    fi
    echo ""
}

check_docker_image() {
    local image=$1
    echo "Checking Docker image: $image"
    if docker images --format "{{.Repository}}:{{.Tag}}" | grep -q "$image"
    then
        echo "✅ Docker image $image is pulled."
    else
        echo "⚠️  Docker image $image is not pulled. It will be pulled on first docker compose up."
    fi
    echo ""
}

check_maven() {
    echo "=============================="
    echo "Checking Maven installation:"
    echo "=============================="
    if [ -f "./mvnw" ]; then
        MVN_VERSION=$(./mvnw --version 2>&1 | head -1)
        echo "✅ Maven wrapper found. $MVN_VERSION"
    elif command -v mvn &> /dev/null; then
        echo "✅ Maven is installed:"
        mvn --version | head -1
    else
        echo "❌ Maven is not installed and no wrapper found."
    fi
    echo ""
}

# ========================================
# Run all checks
# ========================================

echo ""
echo "Spring AI Zero-to-Hero — Dependency Check"
echo "Tech Stack: Spring Boot 4.0.5 | Spring AI 2.0.0-M4 | Java 25"
echo "=========================================="
echo ""

check_java
check_ollama
check_ollama_model "mistral" "Chat model — 7B, default for provider-ollama"
check_ollama_model "nomic-embed-text" "Embedding model — 768 dims, 8192 context"
check_ollama_model "llava" "Multimodal model — image+text, used for chat_07"
check_docker
check_docker_image "pgvector/pgvector:pg18"
check_docker_image "grafana/otel-lgtm"
check_maven

echo "=========================================="
echo "Check complete. Fix any ❌ items above."
echo "Then run: ./download-deps.sh"
echo "=========================================="
