# observability-stack

This project includes a Docker Compose configuration to launch a local 
observability stack using the **grafana/otel-lgtm:latest** all-in-one container. 
This single container bundles **Grafana**, **Tempo**, **Loki**, **Prometheus**, 
and an **OpenTelemetry Collector** into one image, providing an easy way to 
monitor, trace, and log your applications using industry-standard tools. It’s 
ideal for testing observability configurations on a laptop without needing a 
complex cloud setup.

## Components

The `grafana/otel-lgtm:latest` container includes:

- **Grafana**: Provides a dashboard for visualizing metrics, logs, and traces.
- **Prometheus**: Collects and stores application metrics, making them available
  for monitoring and alerting.
- **Loki**: Handles log aggregation, allowing you to query and analyze 
  application logs within Grafana.
- **Tempo**: Provides distributed tracing, enabling you to follow the flow of 
  requests across services.
- **OpenTelemetry Collector**: Receives OTLP signals (traces, metrics, logs) 
  and routes them to the appropriate backends.

## Usage

The `ostack` script provides a simple interface to manage the observability stack containers. Make sure to make the script executable with `chmod +x ostack` before using it.

1. **Start the Stack**: Run `./ostack start`
   - This starts all containers in detached mode
   - Shows container status and connection information when complete

2. **Check Status**: Run `./ostack status`
   - Displays the status of all containers
   - Shows connection information for each service

3. **Stop the Stack**: Run `./ostack stop`
   - Stops all running containers
   - Removes associated networks

4. **Clean Up**: Run `./ostack clean`
   - Stops all containers
   - Removes all associated volumes (data cleanup)

5. **Fix Port Conflicts**: Run `./ostack fix`
   - Automatically detects and resolves port conflicts
   - Useful if you see errors about ports already being in use

6. **View Logs**: Run `docker compose logs -f <service-name>` to view logs for
   a specific service. For example, `docker compose logs -f tempo`.

## Spring Boot Observability: OpenTelemetry Integration

### Export Formats: OTLP Traces, Metrics, and Logs (push)

The **Spring Boot application** uses the **spring-boot-starter-opentelemetry** starter to export all observability signals -- **traces**, **metrics**, and **logs** -- in **OTLP (OpenTelemetry Protocol)** format to the all-in-one LGTM container.

**Traces** are generated using **Micrometer Tracing** with the **OpenTelemetry bridge**, and exported over HTTP to the **OpenTelemetry Collector** via the standard OTLP HTTP endpoint (`POST /v1/traces`).

**Metrics** are exported via the **Micrometer OTLP registry** and sent to the collector using `POST /v1/metrics`.

For **logs**, the application uses a **logback-spring.xml** configuration with the **OpenTelemetry Logback appender**, which serializes structured logs and sends them via OTLP to the collector (`POST /v1/logs`).

**Grafana** serves as the UI frontend for all observability data, connecting to **Tempo** (traces), **Loki** (logs), and **Prometheus** (metrics) within the same container.

### Architecture Diagram

```text

   +-----------------------------------------------------------+
   |                  Spring Boot App                          |
   |  spring-boot-starter-opentelemetry                        |
   |  Micrometer otel metrics registry                         |
   |  Micrometer otel tracing bridge                           |
   |  OpenTelemetry logback appender (logback-spring.xml)      |
   +-----------------------------------------------------------+
          |                   |                     |
POST :4318/v1/traces          |           POST :4318/v1/metrics
          |                   |                     |
          |         POST :4318/v1/logs              |
          ↓                   ↓                     ↓
      +----------------------------------------------------+
      |          grafana/otel-lgtm:latest                  |
      |  OpenTelemetry Collector (OTLP :4318 / :4317)      |
      |  Grafana (3000) | Tempo | Loki | Prometheus        |
      +----------------------------------------------------+
```

## Spring Boot configuration 

### Dependencies 

```xml
<dependencies>
  <!--
    Spring Boot OpenTelemetry starter.
    Provides auto-configuration for OTLP export of traces, metrics, and logs.
    Includes Micrometer OTel tracing bridge, OTLP metrics registry,
    and actuator support.
  -->
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-opentelemetry</artifactId>
  </dependency>

  <!--
    OpenTelemetry Logback Appender.
    Configure via logback-spring.xml to serialize logs to OTLP format
    and send them to the OpenTelemetry Collector via POST /v1/logs.
  -->
  <dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>opentelemetry-logback-appender</artifactId>
  </dependency>
</dependencies>
```

### Configuration


## Note on the All-in-One LGTM Container

The `grafana/otel-lgtm:latest` image bundles an OpenTelemetry Collector, 
Grafana, Loki, Tempo, and Prometheus into a single container. This replaces 
the previous multi-container setup that required separate Docker Compose 
services for each component. The embedded OTel Collector accepts OTLP signals 
on ports 4317 (gRPC) and 4318 (HTTP) and routes them to the appropriate 
backends automatically.
