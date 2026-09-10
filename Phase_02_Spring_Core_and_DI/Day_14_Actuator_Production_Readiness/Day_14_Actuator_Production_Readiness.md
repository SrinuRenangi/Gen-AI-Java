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

## 📌 What Will You Learn Today?

Hey there, my friend! Huge congratulations on reaching **Day 14** — the graduation capstone of **Phase 2: Spring Core & Dependency Injection**! 🎉

In hobby coding, your program is considered "done" when it runs on your laptop. But in real-world enterprise jobs, writing code is only step one: **You must prove your application is alive, healthy, and ready to serve thousands of users 24/7.**

Think about it:
- If your vector database crashes in the middle of the night, how does your cloud server know to stop sending user requests there?
- If an AI API suddenly gets 10x slower, how does your team see the alert before your customers get angry?

**Spring Boot Actuator** gives you an entire production monitoring dashboard right out of the box with zero extra code!

By the end of today, you will clearly understand:
- ✅ **What is Spring Boot Actuator?**: The built-in health inspection and metrics system.
- ✅ **Health Checks (`/actuator/health`)**: Liveness (is the app alive?) vs. Readiness (is the app ready to take AI questions?).
- ✅ **Custom `HealthIndicator` for AI**: Writing our own health checks for vector databases and AI models.
- ✅ **Metrics with Micrometer (`/actuator/metrics`)**: Tracking token counts, user requests, and response times.
- ✅ **Prometheus & Grafana Integration**: Exporting real-time numbers to beautiful visual dashboards.
- ✅ **Actuator Security**: Keeping health checks visible while locking down sensitive internal passwords and keys.
- ✅ **Phase 2 Graduation Review**: Mastering IoC, Beans, Scopes, Auto-Configuration, AOP, and Production Health.

---

## 🗺️ Table of Contents

- [1. Real-World Analogy: The Fighter Jet Cockpit Display](#1-real-world-analogy-the-fighter-jet-cockpit-display)
- [2. Setting Up Spring Boot Actuator](#2-setting-up-spring-boot-actuator)
- [3. Health Checks: Liveness vs. Readiness](#3-health-checks-liveness-vs-readiness)
  - [3.1 Liveness: Is the JVM Alive or Deadlocked?](#31-liveness-is-the-jvm-alive-or-deadlocked)
  - [3.2 Readiness: Can We Accept AI Queries Right Now?](#32-readiness-can-we-accept-ai-queries-right-now)
- [4. Building Custom AI Health Indicators](#4-building-custom-ai-health-indicators)
  - [4.1 `VectorDatabaseHealthIndicator`](#41-vectordatabasehealthindicator)
  - [4.2 `OllamaHealthIndicator`](#42-ollamahealthindicator)
- [5. Application Metrics with Micrometer](#5-application-metrics-with-micrometer)
  - [5.1 Counter: Tracking Total Tokens Consumed](#51-counter-tracking-total-tokens-consumed)
  - [5.2 Timer: Recording P95/P99 LLM Inference Latency](#52-timer-recording-p95p99-llm-inference-latency)
- [6. Securing Actuator in Production](#6-securing-actuator-in-production)
- [7. Phase 2 Graduation Capstone Summary](#7-phase-2-graduation-capstone-summary)
- [8. Key Takeaways & Summary](#8-key-takeaways--summary)
- [9. Practice Exercises & Full Solutions](#9-practice-exercises--full-solutions)
- [10. Self-Check Quiz](#10-self-check-quiz)

---

# 1. Real-World Analogy: The Fighter Jet Cockpit Display

```
                A MICROSERVICE WITHOUT ACTUATOR (Flying Blind)
┌─────────────────────────────────────────────────────────────────────────────┐
│ You fly a supersonic jet through heavy clouds. You have no altimeter,       │
│ no fuel gauge, no radar, and no engine temperature lights. You only find    │
│ out you are out of fuel when the engines suddenly stop!                     │
└─────────────────────────────────────────────────────────────────────────────┘
                                      vs.
                A MICROSERVICE WITH ACTUATOR (The Heads-Up Display)
┌─────────────────────────────────────────────────────────────────────────────┐
│ The pilot has a comprehensive dashboard:                                    │
│ • Green Light: Fuel pumps and oxygen nominal (Health Check: UP).            │
│ • Digital Gauges: Real-time speed and fuel burn rate (Micrometer Metrics).  │
│ • Telemetry Radio: Streaming sensor data to ground control (Prometheus).    │
└─────────────────────────────────────────────────────────────────────────────┘
```

Spring Boot Actuator is the **Heads-Up Display (HUD)** of your enterprise AI application.

---

## 🧭 The Plain English Bridge: Spring Boot Actuator Demystified

If you've only written code on your laptop, deploying to production or Kubernetes can feel scary. Actuator gives you instant visibility with zero custom code:

| Production Concept | What Actuator Provides | Plain English Meaning |
| :--- | :--- | :--- |
| **`/actuator/health`** | A JSON report showing if DB, disk, and AI connections are `UP` or `DOWN`. | The doctor's stethoscope: tells you if the app is breathing. |
| **Liveness Probe** | `/actuator/health/liveness` | *"Is the app stuck in an infinite loop?"* If YES, Kubernetes reboots the container. |
| **Readiness Probe** | `/actuator/health/readiness` | *"Has the app finished warming up AI models?"* If NO, Kubernetes pauses user traffic until ready. |
| **`/actuator/metrics`** | Micrometer telemetry (CPU, JVM heap memory, request rate, token counts). | The speedometer and fuel gauge on your car dashboard. |
| **Prometheus Exporter** | `/actuator/prometheus` format | Standard raw numbers format that Prometheus scrapes every 15 seconds to draw Grafana charts. |
| **Security Warning!** | Keep `/actuator/env` and `/beans` private! | Never expose all endpoints to the public internet! It can leak secret database passwords and API keys. |

---

# 2. Setting Up Spring Boot Actuator

To enable production endpoints, add the starter dependency to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

By default, for security reasons, Spring Boot only exposes the `/actuator/health` endpoint. In `application.yml`, you configure which endpoints are exposed over HTTP:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: "health,info,metrics,prometheus"
  endpoint:
    health:
      show-details: always # Shows granular database and AI subsystem health
```

---

# 3. Health Checks: Liveness vs. Readiness

In containerized cloud environments (like Kubernetes or AWS ECS), the orchestrator continuously polls your application using two distinct probes:

```
                            KUBERNETES CONTAINER POD
                                       │
                 ┌─────────────────────┴─────────────────────┐
                 ▼                                           ▼
      [ /actuator/health/liveness ]               [ /actuator/health/readiness ]
      "Are you alive or frozen?"                  "Are you ready for traffic?"
                 │                                           │
      • If FAIL: Restart container pod!           • If FAIL: Stop routing user traffic,
        (Kill deadlocked JVM)                       allow local model warmup to finish!
```

---

# 4. Building Custom AI Health Indicators

Spring Boot automatically includes health checks for your disk space and relational databases. For AI microservices, you should build custom indicators for your **Vector Database** and **LLM Provider**.

### 4.1 Custom `VectorDatabaseHealthIndicator`

```java
package com.javagenai.day14;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class VectorDatabaseHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        try {
            long latencyMs = pingVectorDatabase();
            if (latencyMs < 500) {
                return Health.up()
                    .withDetail("vector_db", "PostgreSQL pgvector")
                    .withDetail("index_status", "HNSW_INDEX_ONLINE")
                    .withDetail("ping_latency_ms", latencyMs)
                    .build();
            } else {
                return Health.down()
                    .withDetail("error", "Latency degraded: " + latencyMs + " ms")
                    .build();
            }
        } catch (Exception ex) {
            return Health.down(ex)
                .withDetail("error", "Cannot connect to pgvector database socket")
                .build();
        }
    }

    private long pingVectorDatabase() {
        // Simulated sub-10ms vector ping
        return 8L;
    }
}
```

When you visit `http://localhost:8080/actuator/health`, Spring returns:

```json
{
  "status": "UP",
  "components": {
    "diskSpace": { "status": "UP" },
    "vectorDatabase": {
      "status": "UP",
      "details": {
        "vector_db": "PostgreSQL pgvector",
        "index_status": "HNSW_INDEX_ONLINE",
        "ping_latency_ms": 8
      }
    }
  }
}
```

---

# 5. Application Metrics with Micrometer

**Micrometer** is the vendor-neutral metrics facade for Java (the "SLF4J for metrics"). It allows you to define metrics once and publish them to **Prometheus**, **Datadog**, **CloudWatch**, or **Dynatrace**.

### 5.1 Counter: Tracking Total Tokens Consumed

A **Counter** is a monotonically increasing metric used to count events:

```java
package com.javagenai.day14;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class TokenMeterService {
    private final Counter promptTokensCounter;
    private final Counter completionTokensCounter;

    public TokenMeterService(MeterRegistry registry) {
        this.promptTokensCounter = Counter.builder("ai.llm.tokens.prompt")
            .description("Total prompt tokens consumed")
            .tag("provider", "openai")
            .register(registry);

        this.completionTokensCounter = Counter.builder("ai.llm.tokens.completion")
            .description("Total completion tokens generated")
            .tag("provider", "openai")
            .register(registry);
    }

    public void recordUsage(int promptTokens, int completionTokens) {
        promptTokensCounter.increment(promptTokens);
        completionTokensCounter.increment(completionTokens);
    }
}
```

---

### 5.2 Timer: Recording P95/P99 LLM Inference Latency

A **Timer** records both the count of operations and their execution duration distribution:

```java
package com.javagenai.day14;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
public class MonitoredAIService {
    private final Timer llmInferenceTimer;

    public MonitoredAIService(MeterRegistry registry) {
        this.llmInferenceTimer = Timer.builder("ai.llm.inference.duration")
            .description("Latency distribution of LLM calls")
            .publishPercentiles(0.5, 0.95, 0.99) // P50, P95, P99 metrics!
            .register(registry);
    }

    public String callWithMetrics(Supplier<String> llmCall) {
        // Automatically records execution time into Prometheus metrics!
        return llmInferenceTimer.record(llmCall);
    }
}
```

---

# 6. Securing Actuator in Production

> [!CAUTION]
> Never expose `/actuator/heapdump`, `/actuator/env`, or `/actuator/configprops` to the open internet! They contain sensitive JVM memory contents and environment credentials.

In production, you secure Actuator by:
1. Running Actuator on a private internal management port:
   ```yaml
   management:
     server:
       port: 9090 # Internal management port separate from public port 8080
   ```
2. Securing endpoints with **Spring Security** (covered in Phase 5) so only authorized admin roles can inspect operational metrics.

---

# 7. Phase 2 Graduation Capstone Summary

You have officially mastered the core architecture that powers all enterprise Spring Boot and Spring AI applications!

```
┌────────────────────────────────────────────────────────────────────────┐
│             CONGRATULATIONS: PHASE 2 COMPLETED! 🎓                     │
├────────────────────────────────────────────────────────────────────────┤
│ Day 09: The Problem Spring Solves — Dependency Hell (Mini-IoC from 0)  │
│ Day 10: Spring IoC Container, Stereotypes & Bean Lifecycle Pipeline    │
│ Day 11: Dependency Injection In-Depth: @Qualifier, @Primary & Profiles │
│ Day 12: Spring Boot Auto-Configuration: Starters & Conditionals        │
│ Day 13: AOP Cross-Cutting Concerns: Logging, Auditing & Latency Proxies│
│ Day 14: Spring Boot Actuator, Health Indicators & Micrometer Metrics   │
└────────────────────────────────────────────────────────────────────────┘
```

You now possess the foundational engineering fluency of a **senior Java backend engineer**. You understand how Spring creates and manages objects, how auto-configuration activates defaults, how AOP intercepts execution, and how Actuator keeps systems healthy in production.

**You are now fully prepared for Phase 3: Spring Web — Building REST APIs (Days 15–20)!**

---

# 8. Key Takeaways & Summary

```
                  ┌─────────────────────────────────┐
                  │       DAY 14 CHEAT SHEET        │
                  └────────────────┬────────────────┘
                                   │
         ┌─────────────────────────┼─────────────────────────┐
         ▼                         ▼                         ▼
  [ Health Indicators ]    [ Micrometer Metrics ]    [ Production Safety ]
  • Liveness: checks if    • Counters: track total   • Change management port
    app needs restart        tokens consumed           to private internal port
  • Readiness: checks if   • Timers: measure P95/P99 • Never expose /heapdump
    app can take queries     latency distributions     or /env to open web
  • Implement custom       • Exports to Prometheus   • Actuator is the flight
    HealthIndicator for AI   for Grafana dashboards    recorder of your app
```

---

# 9. Practice Exercises & Full Solutions

### 🏋️ Exercise 1: Build a Simulated Health Status Evaluator
**Objective**: Create a class `AIHealthReporter` that evaluates three subsystem indicators (LLM Gateway, pgvector DB, and Token Rate Limiter) and returns an overall status of `HEALTHY`, `DEGRADED`, or `DOWN`.

#### Solution:
```java
package com.javagenai.day14;

import java.util.Map;

public class AIHealthReporter {

    public enum Status { HEALTHY, DEGRADED, DOWN }

    public static Status evaluateCluster(Map<String, Boolean> subsystemChecks) {
        if (!subsystemChecks.getOrDefault("vector_db", false)) {
            return Status.DOWN; // Core persistence is down
        }
        if (!subsystemChecks.getOrDefault("primary_llm", false)) {
            return Status.DEGRADED; // Can fallback to secondary local model
        }
        return Status.HEALTHY;
    }
}
```

---

## 10. Self-Check Quiz

1. **What is the difference between Kubernetes Liveness and Readiness probes?**
   - *Answer*: A Liveness probe checks if the JVM process is alive and responsive (if it fails, Kubernetes kills and restarts the pod). A Readiness probe checks if the application is fully initialized and capable of serving user queries (if it fails, traffic is temporarily redirected away from the pod without killing it).
2. **How do you add a custom health check for a Vector Database in Spring Boot?**
   - *Answer*: Create a `@Component` class that implements Spring Boot's `HealthIndicator` interface and return `Health.up()` or `Health.down()` from its `health()` method.
3. **What is Micrometer?**
   - *Answer*: A dimensional, vendor-neutral application metrics facade for Java that collects metrics (counters, gauges, timers) and exports them to monitoring platforms like Prometheus, Datadog, or Grafana.
4. **Why is it critical to track P95 and P99 latency rather than just average latency for LLM calls?**
   - *Answer*: Average latency hides severe bottlenecks. If 90% of calls take 500ms but 10% of calls take 15 seconds, the average might look acceptable, while 1 in 10 users experiences an unacceptable delay.
5. **How do you prevent sensitive operational endpoints from leaking to the public internet?**
   - *Answer*: By configuring `management.server.port` to an internal private port, selectively exposing only safe endpoints via `management.endpoints.web.exposure.include`, and securing the `/actuator/**` path with Spring Security.

---

<p align="center">
  <b>🎉 Congratulations on Graduating Phase 2! 🎉</b><br>
  You have officially mastered Spring Core, Dependency Injection, Beans, Auto-Configuration, AOP, and Production Actuator monitoring! You now understand the complete foundation of enterprise Spring Boot.<br>
  Tomorrow, we launch <b>Phase 3: Spring Web — Building REST APIs (Days 15–20)</b> — starting with <b>HTTP Deep Dive & Your First REST Controller</b>: GET, POST, PUT, DELETE, and building a real Prompt Library CRUD API! Celebrate your progress, you're crushing it!
</p>
