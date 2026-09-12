# 📊 Day 14: Spring Boot Actuator & Production Readiness
## Health Checks, Micrometer Metrics & Monitoring for Live AI Microservices

| ⬅️ Previous Day | 📚 Course Hub | ➡️ Next Day |
|:---|:---:|---:|
| [← Day 13: AOP — Cross-Cutting Concerns](../Day_13_AOP_Cross_Cutting_Concerns/Day_13_AOP_Cross_Cutting_Concerns.md) | [All 60 Days Overview](../../README.md) | [Day 15: HTTP Deep Dive & First REST Controller →](../../Phase_03_Spring_Web_REST_APIs/Day_15_HTTP_Deep_Dive_First_REST_Controller/Day_15_HTTP_Deep_Dive_First_REST_Controller.md) |

[![Phase](https://img.shields.io/badge/Phase_02-Spring_Core_%26_DI-brightgreen.svg?style=for-the-badge)](../../README.md)
[![Day](https://img.shields.io/badge/Day-14_of_60-blue.svg?style=for-the-badge)](../../README.md)
[![Difficulty](https://img.shields.io/badge/Difficulty-Intermediate-blue.svg?style=for-the-badge)](../../README.md)
[![Milestone](https://img.shields.io/badge/Milestone-Phase_2_Graduation!-brightgreen.svg?style=for-the-badge)](../../README.md)

---

## 1. Topic Overview

Spring Boot Actuator is an enterprise operational framework that exposes production-grade telemetry, diagnostic endpoints, and health sensors directly from a running Spring Boot microservice. In enterprise Generative AI systems, Actuator provides real-time visibility into AI provider connectivity, vector database index latency, token consumption rates, and JVM container health, ensuring automated orchestrators like Kubernetes can detect anomalies, route traffic reliably, and scale infrastructure without downtime.

---

## 2. Basic Foundations (True Zero)

### Plain English Definitions
- **Spring Boot Actuator**: The built-in operational telemetry dashboard inside your Spring Boot application that exposes ready-to-use HTTP and JMX endpoints (e.g., `/actuator/health`, `/actuator/metrics`) to observe system vitals.
- **Health Indicator (`HealthIndicator`)**: A diagnostic probe in Java that tests a specific subsystem (such as a database, cache, or external LLM API) and reports whether it is `UP`, `DOWN`, or `DEGRADED`.
- **Liveness Probe**: A cloud container diagnostic check asking: *"Is the JVM process alive, or is it frozen/deadlocked?"* If it fails, the orchestrator (like Kubernetes) kills the container and boots a fresh replacement.
- **Readiness Probe**: A diagnostic check asking: *"Has the application finished initializing its vector indexes and warming up connections, and is it ready to handle user prompts?"* If it reports `DOWN`, the load balancer pauses routing user queries until recovery.
- **Micrometer**: A dimensional, vendor-neutral application metrics facade for Java (analogous to SLF4J for logging) that collects operational counters, timers, and gauges and exports them to monitoring platforms like Prometheus, Datadog, or Grafana.

### Relatable Physical Analogy: The Fighter Jet Cockpit Display
```
                A MICROSERVICE WITHOUT ACTUATOR (Flying Blind)
┌─────────────────────────────────────────────────────────────────────────────┐
│ You fly a supersonic jet through heavy fog. You have no altimeter, no fuel  │
│ gauge, no radar, and no oil pressure light. You only discover you are out   │
│ of fuel when both engines suddenly flame out mid-flight!                    │
└─────────────────────────────────────────────────────────────────────────────┘
                                      vs.
                A MICROSERVICE WITH ACTUATOR (The Heads-Up Display)
┌─────────────────────────────────────────────────────────────────────────────┐
│ The pilot is equipped with a digital Heads-Up Display:                      │
│ • Green Status Light: Oxygen and engines nominal (Health Check: UP).        │
│ • Digital Gauges: Real-time speed and burn rate (Micrometer Metrics).       │
│ • Telemetry Transponder: Streaming diagnostic data to base (Prometheus).   │
└─────────────────────────────────────────────────────────────────────────────┘
```

Spring Boot Actuator is the **Heads-Up Display (HUD)** of your cloud-native enterprise AI microservice.

### Minimal Beginner-Friendly Working Code Example

Let us examine how to register a minimal custom health indicator in pure Spring Boot:

```java
package com.javagenai.day14;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class MinimalAiHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        boolean llmProviderReachable = checkConnection();

        if (llmProviderReachable) {
            return Health.up()
                .withDetail("provider", "OpenAI")
                .withDetail("latency_ms", 42)
                .build();
        } else {
            return Health.down()
                .withDetail("error", "LLM Gateway connection timed out")
                .build();
        }
    }

    private boolean checkConnection() {
        // Simulates a quick network health ping
        return true;
    }
}
```

#### Line-by-Line Walkthrough
1. `@Component`: Registers this class as a Spring bean inside the IoC container.
2. `implements HealthIndicator`: Marks this bean as a health contributor that Actuator automatically invokes during `/actuator/health` requests.
3. `Health.up().withDetail(...)`: Constructs an `UP` status object accompanied by diagnostic metadata (e.g., latency in ms).
4. `Health.down().withDetail(...)`: Constructs a `DOWN` status object with error context if connectivity fails.
5. Spring Actuator aggregates this check into the global application status JSON payload automatically.

---

## 3. Core Concept Walkthrough (Basic → Intermediate)

### 3.1 Actuator Architecture & Configuration
To enable Actuator, add the starter dependency to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

By default, for enterprise security, Spring Boot only exposes `/actuator/health` to HTTP requests. In `application.yml`, you explicitly control which management endpoints are accessible:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: "health,info,metrics,prometheus"
  endpoint:
    health:
      show-details: always # Exposes granular health of vector DB and AI subsystems
```

### 3.2 Cloud Probes: Liveness vs. Readiness
In containerized cloud environments (Kubernetes, AWS ECS, Google Cloud Run), the infrastructure monitors microservices via two specialized probe endpoints:

```
                            KUBERNETES CONTAINER POD
                                       │
                 ┌─────────────────────┴─────────────────────┐
                 ▼                                           ▼
      [ /actuator/health/liveness ]               [ /actuator/health/readiness ]
      "Are you alive or deadlocked?"              "Are you ready for traffic?"
                 │                                           │
      • If FAIL: Restart container pod!           • If FAIL: Stop routing user traffic,
        (Kill deadlocked JVM process)               allow index warmup to complete!
```

- **Liveness (`/actuator/health/liveness`)**: Assesses whether the JVM runtime is responsive. If deadlocked or out of memory, Kubernetes terminates and restarts the container pod.
- **Readiness (`/actuator/health/readiness`)**: Assesses whether the service is prepared to handle live customer traffic (e.g., database connection pool established, vector embedding cache primed). If not ready, traffic is held without killing the container.

### 3.3 Building Custom AI Health Indicators
Spring Boot automatically checks relational databases and disk storage. For enterprise AI services, you must provide health indicators for **Vector Databases** and **LLM Gateways**:

```java
package com.javagenai.day14.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class VectorDatabaseHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        try {
            long pingLatencyMs = executeVectorPing();

            if (pingLatencyMs < 300) {
                return Health.up()
                    .withDetail("vector_engine", "PostgreSQL pgvector")
                    .withDetail("index_structure", "HNSW_COSINE")
                    .withDetail("ping_latency_ms", pingLatencyMs)
                    .build();
            } else {
                return Health.status("DEGRADED")
                    .withDetail("warning", "Vector search latency elevated: " + pingLatencyMs + "ms")
                    .build();
            }
        } catch (Exception ex) {
            return Health.down(ex)
                .withDetail("error", "Unable to establish socket connection to pgvector cluster")
                .build();
        }
    }

    private long executeVectorPing() {
        // Simulated sub-15ms vector index round-trip
        return 12L;
    }
}
```

When visiting `http://localhost:8080/actuator/health`, Actuator responds:
```json
{
  "status": "UP",
  "components": {
    "diskSpace": { "status": "UP" },
    "vectorDatabase": {
      "status": "UP",
      "details": {
        "vector_engine": "PostgreSQL pgvector",
        "index_structure": "HNSW_COSINE",
        "ping_latency_ms": 12
      }
    }
  }
}
```

### 3.4 Telemetry with Micrometer: Counters & Timers
Micrometer standardizes metrics collection across the JVM.

#### 1. Counter: Monitoring Total Token Consumption
```java
package com.javagenai.day14.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class TokenMetricsService {
    private final Counter promptTokensCounter;
    private final Counter completionTokensCounter;

    public TokenMetricsService(MeterRegistry registry) {
        this.promptTokensCounter = Counter.builder("ai.tokens.prompt")
            .description("Total prompt tokens sent to LLM")
            .tag("provider", "openai")
            .register(registry);

        this.completionTokensCounter = Counter.builder("ai.tokens.completion")
            .description("Total completion tokens received from LLM")
            .tag("provider", "openai")
            .register(registry);
    }

    public void recordTokenUsage(int promptTokens, int completionTokens) {
        promptTokensCounter.increment(promptTokens);
        completionTokensCounter.increment(completionTokens);
    }
}
```

#### 2. Timer: Measuring P95/P99 Inference Latency
```java
package com.javagenai.day14.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
public class MonitoredInferenceService {
    private final Timer inferenceTimer;

    public MonitoredInferenceService(MeterRegistry registry) {
        this.inferenceTimer = Timer.builder("ai.llm.inference.latency")
            .description("Inference latency distribution")
            .publishPercentiles(0.50, 0.95, 0.99) // P50, P95, P99 percentile distributions!
            .register(registry);
    }

    public String executeWithTelemetry(Supplier<String> inferenceCall) {
        // Automatically records execution duration into Prometheus metrics registry
        return inferenceTimer.record(inferenceCall);
    }
}
```

---

## 4. Prerequisite & Supporting Concepts

### Prerequisite / Supporting Concept: The Plain English Bridge to Actuator

| Production Concept | What Actuator Exposes | Plain English Translation |
| :--- | :--- | :--- |
| **`/actuator/health`** | Aggregated health status JSON (`UP`, `DOWN`, `OUT_OF_SERVICE`). | The medical stethoscope: confirms the application is breathing. |
| **Liveness Probe** | `/actuator/health/liveness` | *"Is the app completely frozen in a deadlock?"* (If yes, restart). |
| **Readiness Probe** | `/actuator/health/readiness` | *"Has the app warmed up its models and connections?"* (If no, pause traffic). |
| **`/actuator/metrics`** | Micrometer performance indicators (JVM heap, GC, token counts). | The speedometer, fuel gauge, and RPM tachometer on your car dashboard. |
| **`/actuator/prometheus`**| Formatted metrics scraped by Prometheus server every 15 seconds. | High-frequency telemetry stream feeding real-time Grafana dashboards. |

### Prerequisite / Supporting Concept: Metric Types
- **Counter**: Monotonically increasing number (e.g., total tokens consumed, total HTTP requests).
- **Gauge**: Instantaneous value that can rise or fall (e.g., active database connections, current JVM heap used).
- **Timer**: Measures both frequency and duration of short-lived events (e.g., API latency percentiles).

---

## 5. Advanced Depth (Intermediate → Advanced)

### 5.1 Hardening Actuator Security for Production
> [!CAUTION]
> Never expose `/actuator/env`, `/actuator/heapdump`, or `/actuator/beans` to the public internet! They expose memory dumps, database credentials, and secret API keys.

In enterprise production architectures, implement two layers of defense:

#### 1. Isolate Actuator on an Internal Management Port
Bind all actuator traffic to a separate private network port that is unreachable from external internet load balancers:
```yaml
management:
  server:
    port: 9090 # Internal DevOps/monitoring port only (public traffic on 8080)
    address: 127.0.0.1
```

#### 2. Restrict Sensitive Actuator Endpoints via Security
Only expose harmless diagnostic endpoints publicly, and protect the rest behind authentication:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: "health,info,prometheus" # Strictly exclude env, heapdump, beans
```

### 5.2 Why P95/P99 Percentiles Matter for Generative AI
A common enterprise mistake is monitoring **average latency** for LLM calls:
```
Total Requests: 100
- 90 requests take 300 ms
- 10 requests timeout at 25,000 ms (25 seconds)

Average Latency: ~2.77 seconds (Looks acceptable on an executive summary!)
P99 Latency:     25.00 seconds (Catastrophic degradation for 1 in 10 users!)
```
Micrometer's `.publishPercentiles(0.95, 0.99)` exposes the exact latency suffered by the slowest 5% and 1% of users, highlighting model queue bottlenecks and network degradation before widespread outages occur.

---

## 6. Quick Recap

| Component | Endpoint / Class | Primary Function | Enterprise AI Role |
| :--- | :--- | :--- | :--- |
| **Health Indicator** | `HealthIndicator` | Evaluates subsystem operational readiness | Pings vector databases and LLM endpoints |
| **Liveness Check** | `/actuator/health/liveness` | Detects JVM deadlock or fatal freeze | Triggers automatic container restart |
| **Readiness Check**| `/actuator/health/readiness`| Detects if app is ready for queries | Buffers traffic while warming embeddings |
| **Token Counter** | `io.micrometer.core...Counter` | Accumulates token volume | Audits cost allocation across business units |
| **Latency Timer** | `io.micrometer.core...Timer` | Tracks P50/P95/P99 duration distributions | Monitors LLM inference response times |
| **Security Control**| `management.server.port` | Separates admin telemetry port | Prevents credential leaks to open web |

---

## 7. Self-Check Questions & Practice Exercises

### Self-Check Questions

1. **What is the architectural distinction between Kubernetes Liveness and Readiness probes?**
   - *Answer*: A Liveness probe verifies the JVM process is alive and responsive; if it fails, Kubernetes terminates and restarts the container pod. A Readiness probe verifies whether the microservice is prepared to serve incoming user queries; if it fails, traffic is temporarily stopped from reaching the pod without terminating the process.
2. **How do you register a custom health check for an AI vector store in Spring Boot?**
   - *Answer*: Define a `@Component` class that implements Spring Boot's `HealthIndicator` interface and return `Health.up()` or `Health.down()` with diagnostic metadata from its `health()` method.
3. **What role does Micrometer serve in the Java observability ecosystem?**
   - *Answer*: Micrometer acts as a dimensional, vendor-neutral metrics facade that records application telemetry (counters, timers, gauges) and exports them seamlessly to monitoring backends like Prometheus, Datadog, or Grafana.
4. **Why is tracking P95/P99 latency more reliable than tracking average latency in LLM applications?**
   - *Answer*: LLM responses exhibit high latency variance. Averages mask extreme tail-latency spikes where a percentage of queries experience timeouts or multi-second delays, while percentiles expose the exact experience of the slowest requests.
5. **How should Spring Boot Actuator be secured against credential leakage in production?**
   - *Answer*: Move Actuator to a private internal management port (`management.server.port`), expose only safe endpoints (`health`, `prometheus`), exclude sensitive endpoints (`env`, `heapdump`), and enforce role-based access control via Spring Security.

---

### Hands-On Practice Exercises

#### 🏋️ Exercise 1: Build an AI Cluster Health Evaluation Engine
**Objective**: Construct an `AiClusterHealthEvaluator` that aggregates the status of three critical subsystems (Vector DB, Primary LLM Gateway, and Cache) and determines if the overall cluster is `HEALTHY`, `DEGRADED`, or `DOWN`.

```java
package com.javagenai.day14;

import java.util.Map;

public class AiClusterHealthEvaluator {

    public enum ClusterStatus { HEALTHY, DEGRADED, DOWN }

    public static ClusterStatus evaluate(Map<String, Boolean> subsystemStatuses) {
        boolean vectorDbOnline = subsystemStatuses.getOrDefault("vector_db", false);
        boolean primaryLlmOnline = subsystemStatuses.getOrDefault("primary_llm", false);
        boolean cacheOnline = subsystemStatuses.getOrDefault("redis_cache", false);

        // If core vector storage is down, the entire RAG pipeline cannot function
        if (!vectorDbOnline) {
            return ClusterStatus.DOWN;
        }

        // If primary LLM is down but fallback model can be used, status is degraded
        if (!primaryLlmOnline || !cacheOnline) {
            return ClusterStatus.DEGRADED;
        }

        return ClusterStatus.HEALTHY;
    }

    public static void main(String[] args) {
        Map<String, Boolean> testStatus = Map.of(
            "vector_db", true,
            "primary_llm", false,
            "redis_cache", true
        );

        ClusterStatus result = evaluate(testStatus);
        System.out.println("Evaluated Cluster Status: " + result); // Output: DEGRADED
    }
}
```

#### 🏋️ Exercise 2: Build a Thread-Safe In-Memory Metrics Recorder
**Objective**: Build a simulated pure Java metrics recorder that measures prompt execution latency and computes P95 percentile response times without external libraries.

```java
package com.javagenai.day14;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryLatencyMetrics {

    private final List<Long> latencyRecordings = new CopyOnWriteArrayList<>();

    public void record(long durationMs) {
        latencyRecordings.add(durationMs);
    }

    public double calculatePercentile(double percentile) {
        if (latencyRecordings.isEmpty()) return 0.0;

        List<Long> sorted = new ArrayList<>(latencyRecordings);
        Collections.sort(sorted);

        int index = (int) Math.ceil(percentile * sorted.size()) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));
        return sorted.get(index);
    }

    public static void main(String[] args) {
        InMemoryLatencyMetrics metrics = new InMemoryLatencyMetrics();
        for (int i = 1; i <= 100; i++) {
            metrics.record(i * 10L); // 10ms to 1000ms
        }

        System.out.println("P50 Latency: " + metrics.calculatePercentile(0.50) + " ms"); // 500 ms
        System.out.println("P95 Latency: " + metrics.calculatePercentile(0.95) + " ms"); // 950 ms
        System.out.println("P99 Latency: " + metrics.calculatePercentile(0.99) + " ms"); // 990 ms
    }
}
```

---

## 🎓 Phase 2 Graduation Milestone: You Did It!

You have completed **Phase 2: Spring Core & Dependency Injection**! You have mastered the foundational enterprise mechanics:
- **Day 09**: Solving Dependency Hell with IoC Containers
- **Day 10**: The Spring Bean Lifecycle Pipeline & Stereotypes
- **Day 11**: Constructor Injection, `@Qualifier`, and Environmental Profiles
- **Day 12**: Spring Boot Auto-Configuration & Conditional Matching
- **Day 13**: Aspect-Oriented Programming (AOP) for Clean Cross-Cutting Concerns
- **Day 14**: Spring Boot Actuator, Health Checks, and Micrometer Telemetry

You are now prepared to build production REST APIs in **Phase 3: Spring Web (Days 15–20)**!

---

| ⬅️ Previous Day | 📚 Course Hub | ➡️ Next Day |
|:---|:---:|---:|
| [← Day 13: AOP — Cross-Cutting Concerns](../Day_13_AOP_Cross_Cutting_Concerns/Day_13_AOP_Cross_Cutting_Concerns.md) | [All 60 Days Overview](../../README.md) | [Day 15: HTTP Deep Dive & First REST Controller →](../../Phase_03_Spring_Web_REST_APIs/Day_15_HTTP_Deep_Dive_First_REST_Controller/Day_15_HTTP_Deep_Dive_First_REST_Controller.md) |
