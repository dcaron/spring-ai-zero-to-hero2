package com.example.dashboard.agentic;

import io.micrometer.observation.ObservationRegistry;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AgenticClientRegistry {

  private static final Logger log = LoggerFactory.getLogger(AgenticClientRegistry.class);
  private static final int PROBE_TIMEOUT_MS = 300;
  private static final int PROBE_READ_TIMEOUT_MS = 500;
  private static final int MESSAGE_READ_TIMEOUT_MS =
      300_000; // 5 min — Ollama inference can be slow
  private static final long INFO_CACHE_MS = 1_000;

  private final AgenticDemoCatalog catalog;
  private final RestClient.Builder restClientBuilder;
  private final Map<String, CachedInfo> infoCache = new ConcurrentHashMap<>();

  public AgenticClientRegistry(AgenticDemoCatalog catalog, RestClient.Builder builder) {
    this.catalog = catalog;
    this.restClientBuilder = builder;
  }

  public AgenticStatus probe(AgenticDemo demo) {
    boolean up = tcpProbe(demo.port());
    if (!up) {
      return new AgenticStatus(false, "unknown", "unknown", 0, startCommand(demo.id()));
    }
    CachedInfo info = getInfo(demo);
    int agentCount = getAgentCount(demo);
    return new AgenticStatus(
        true, info.provider(), info.model(), agentCount, startCommand(demo.id()));
  }

  public AgenticStatus probeById(String demoId) {
    return probe(catalog.get(demoId));
  }

  /**
   * RestClient with a long read timeout — for proxying actual message sends / LLM calls. Keeps
   * observation enabled so the dashboard→agent hop shows up in Tempo traces.
   */
  public RestClient clientFor(AgenticDemo demo) {
    return newClient(demo, MESSAGE_READ_TIMEOUT_MS, true);
  }

  /**
   * RestClient with a short read timeout — for actuator probes and lightweight info lookups.
   * Observation is disabled so the dashboard's 3-second status polling doesn't pollute LGTM traces
   * with health-check noise.
   */
  public RestClient probeClient(AgenticDemo demo) {
    return newClient(demo, PROBE_READ_TIMEOUT_MS, false);
  }

  private RestClient newClient(AgenticDemo demo, int readTimeoutMs, boolean observe) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofMillis(PROBE_TIMEOUT_MS));
    factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
    // .clone() is essential — RestClient.Builder is a shared Spring bean, and baseUrl() mutates
    // it. Without cloning, concurrent probes across demos race and corrupt each other's baseUrl.
    RestClient.Builder b =
        restClientBuilder
            .clone()
            .baseUrl("http://localhost:" + demo.port())
            .requestFactory(factory);
    if (!observe) {
      b = b.observationRegistry(ObservationRegistry.NOOP);
    }
    return b.build();
  }

  private boolean tcpProbe(int port) {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress("localhost", port), PROBE_TIMEOUT_MS);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private CachedInfo getInfo(AgenticDemo demo) {
    long now = System.currentTimeMillis();
    CachedInfo cached = infoCache.get(demo.id());
    if (cached != null && now - cached.at() < INFO_CACHE_MS) {
      return cached;
    }
    try {
      RestClient c = probeClient(demo);
      @SuppressWarnings("unchecked")
      Map<String, Object> body = c.get().uri("/actuator/info").retrieve().body(Map.class);
      Object agentObj = body == null ? null : body.get("agent");
      String provider = "unknown";
      String model = "unknown";
      if (agentObj instanceof Map<?, ?> agent) {
        Object providerObj = agent.get("provider");
        provider = providerObj == null ? "unknown" : String.valueOf(providerObj);
        Object modelObj =
            "ollama".equals(provider) ? agent.get("ollama-model") : agent.get("openai-model");
        model = modelObj == null ? "unknown" : String.valueOf(modelObj);
      }
      CachedInfo fresh = new CachedInfo(provider, model, now);
      infoCache.put(demo.id(), fresh);
      return fresh;
    } catch (Exception e) {
      log.debug("Actuator info probe failed for {}: {}", demo.id(), e.getMessage());
      return new CachedInfo("unknown", "unknown", now);
    }
  }

  private int getAgentCount(AgenticDemo demo) {
    try {
      RestClient c = probeClient(demo);
      Object[] ids = c.get().uri(demo.basePath() + "/").retrieve().body(Object[].class);
      return ids == null ? 0 : ids.length;
    } catch (Exception e) {
      return 0;
    }
  }

  private String startCommand(String demoId) {
    return "./workshop.sh agentic start " + demoId;
  }

  private record CachedInfo(String provider, String model, long at) {}
}
