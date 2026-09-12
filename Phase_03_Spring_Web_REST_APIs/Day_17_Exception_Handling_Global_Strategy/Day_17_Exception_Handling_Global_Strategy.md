# 🚨 Day 17: Exception Handling & Global Error Strategy
## Bulletproof Enterprise Fault Tolerance: `@RestControllerAdvice`, ProblemDetail & Correlation IDs

| ⬅️ Previous Day | 📚 Course Hub | ➡️ Next Day |
|:---|:---:|---:|
| [← Day 16: Request Validation, DTOs & Response Design](../Day_16_Validation_DTOs_Response_Design/Day_16_Validation_DTOs_Response_Design.md) | [All 60 Days Overview](../../README.md) | [Day 18: Async APIs, Streaming & SSE →](../Day_18_Async_Streaming_SSE/Day_18_Async_Streaming_SSE.md) |

[![Phase](https://img.shields.io/badge/Phase_03-Spring_Web_REST_APIs-yellow.svg?style=for-the-badge)](../../README.md)
[![Day](https://img.shields.io/badge/Day-17_of_60-blue.svg?style=for-the-badge)](../../README.md)
[![Difficulty](https://img.shields.io/badge/Difficulty-Intermediate-blue.svg?style=for-the-badge)](../../README.md)
[![Topic](https://img.shields.io/badge/Spring_Web-Exception_Handling-orange.svg?style=for-the-badge)](../../README.md)

---

## 1. Topic Overview

Global Exception Handling is an architectural design pattern that captures unhandled runtime faults across all application layers and transforms them into standardized, sanitized HTTP responses before they leave the server boundary. In enterprise Generative AI engineering, where external LLM providers regularly return rate limits (`429`), GPU inference timeouts (`504`), and content policy blocks (`403`), a centralized error strategy prevents cascading microservice failures, sanitizes sensitive database credentials from leaking, and equips operations teams with correlation IDs for instantaneous log forensics.

---

## 2. Basic Foundations (True Zero)

### Plain English Definitions
- **Exception**: A runtime disruptive event triggered when an unexpected condition occurs (e.g., downstream LLM network timeout, database connection drop, or invalid JSON payload).
- **`@RestControllerAdvice`**: A centralized Spring interceptor annotation that monitors all `@RestController` beans in your application, intercepts any uncaught exceptions thrown during request execution, and serializes clean JSON error bodies directly to the caller.
- **`@ExceptionHandler`**: A method-level annotation inside an advice class that specifies which exact exception class (e.g., `ModelRateLimitException.class`) that method is responsible for resolving.
- **Correlation ID (Trace ID)**: A unique identifier (UUID) assigned to an incoming HTTP request at the gateway filter boundary, carried across all internal logs and threads, and returned in error responses so developers can pinpoint the exact failure sequence in production log aggregators.
- **Error Sanitization**: The mandatory practice of scrubbing internal stack traces, database credentials, internal IP addresses, and source line numbers from public HTTP responses, replacing them with a polite user-facing error message and an incident tracking reference.

### Relatable Physical Analogy: Hospital Emergency Room Triage
```
[ Patient Arrives with Crisis ] 
              │
              ▼
    [ Triage Receptionist ] ──────► Assigns Wristband: "Patient ID #7f3b" (Correlation ID)
              │
              ▼
   [ Condition Assessment ] ──────► Matches specific condition:
              │
     ┌────────┴─────────┬───────────────────┐
     ▼                  ▼                   ▼
[ Cardiology Unit ]  [ Burn Unit ]   [ General Medical Officer ]
 (@ExceptionHandler) (@ExceptionHandler) (@ExceptionHandler Fallback)
     │                  │                   │
     ▼                  ▼                   ▼
Administers Stent    Applies Salve       Logs detailed pathology internally;
                                         gives family a calm summary (NO PANIC!)
```

In a hospital emergency room:
1. **Triage Wristband (Correlation ID)**: Every patient receives a unique wristband upon entry. Every medication, lab test, and surgical record references this ID.
2. **Specialized Treatment Units (`@ExceptionHandler`)**: Cardiac events are routed to cardiologists, not dermatologists.
3. **Calm Public Communication (RFC 7807 Problem Details)**: When an unexpected surgical complication happens, the chief surgeon does not toss bloody instruments and confusing medical lab sheets into the waiting room. They give the family a calm, standardized briefing with an incident reference ID.

### Minimal Beginner-Friendly Working Code Example

Let us examine how a minimal `@RestControllerAdvice` captures an unhandled exception and returns a structured JSON payload:

```java
package com.javagenai.day17;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@SpringBootApplication
@RestController
@RequestMapping("/api/v1/demo")
public class MinimalExceptionApp {

    public static void main(String[] args) {
        SpringApplication.run(MinimalExceptionApp.class, args);
    }

    @GetMapping("/fail")
    public String triggerError() {
        throw new IllegalStateException("Simulated upstream LLM network failure");
    }
}

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        Map<String, Object> errorPayload = Map.of(
            "title", "Downstream Service Error",
            "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
            "detail", ex.getMessage(),
            "timestamp", Instant.now().toString()
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorPayload);
    }
}
```

#### Line-by-Line Walkthrough
1. `@GetMapping("/fail")`: Defines an endpoint that throws an unhandled `IllegalStateException`.
2. `@RestControllerAdvice`: Automatically registers `GlobalExceptionHandler` with Spring's `HandlerExceptionResolverComposite`.
3. `@ExceptionHandler(IllegalStateException.class)`: Intercepts the exception before it can trigger an ugly 500 error page.
4. `ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)`: Converts the failure into a graceful HTTP `503 Service Unavailable` response with a structured JSON payload.

---

## 3. Core Concept Walkthrough (Basic → Intermediate)

### 3.1 The Spectrum of Enterprise AI Failure Modes
When orchestrating Generative AI microservices, failures are not rare anomalies—they are everyday operating conditions:

| Failure Category | HTTP Status | Retryable? | Root Causes | Enterprise Remediation |
| :--- | :---: | :---: | :--- | :--- |
| **Client Fault** | `400 Bad Request` | ❌ No | Context window exceeded, invalid prompt structure | Truncate prompt or select larger context model |
| **Rate Limiting** | `429 Too Many Requests` | ✅ Yes | Upstream TPM (tokens/min) or RPM limit hit | Parse `Retry-After` header; exponential backoff |
| **Gateway Timeout**| `504 Gateway Timeout` | ✅ Yes | Deep reasoning model takes > 60s to generate | Increase read timeout; switch to SSE streaming |
| **Moderation Block**| `403 Forbidden` | ❌ No | Prompt violated safety or content filter policies | Redact input; inform user of policy boundary |
| **Infrastructure Fault**| `503 Service Unavailable`| ✅ Yes | Vector DB pool exhausted; local GPU worker down | Trigger Resilience4j Circuit Breaker; route to replica |
| **Internal Fault** | `500 Internal Error` | ❌ No | Bug in application code, unexpected NPE | Sanitize message; log full stack trace with correlation ID |

### 3.2 Spring MVC Exception Architecture Sequence

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant DS as DispatcherServlet
    participant CTL as RestController
    participant SVC as AiService (LLM Client)
    participant HEC as HandlerExceptionResolverComposite
    participant EHR as ExceptionHandlerExceptionResolver
    participant ADV as @RestControllerAdvice (GlobalExceptionHandler)

    Client->>DS: POST /api/v1/chat/completions
    DS->>CTL: invoke chat()
    CTL->>SVC: generateResponse()
    SVC-->>CTL: throw ModelRateLimitException("TPM exceeded", 30)
    CTL-->>DS: Uncaught Exception bubbles to DispatcherServlet
    DS->>HEC: processHandlerException(request, response, handler, ex)
    HEC->>EHR: resolveException()
    EHR->>ADV: Find matching @ExceptionHandler(ModelRateLimitException.class)
    ADV-->>EHR: Returns ResponseEntity<ProblemDetail> (Status: 429)
    EHR-->>DS: Handled response model
    DS-->>Client: 429 Too Many Requests (application/problem+json)
```

### 3.3 Designing a Domain-Specific Exception Taxonomy
Never throw generic `RuntimeException`s. Create a structured domain hierarchy:

```
                      ┌──────────────────────────────┐
                      │  java.lang.RuntimeException  │
                      └──────────────┬───────────────┘
                                     │
                      ┌──────────────▼───────────────┐
                      │        GenAiException        │
                      │  - errorCode: String         │
                      │  - retryable: boolean        │
                      │  - modelName: String         │
                      │  - metadata: Map<String,Obj> │
                      └──────────────┬───────────────┘
         ┌───────────────────────────┼───────────────────────────┐
         ▼                           ▼                           ▼
┌──────────────────┐       ┌────────────────────┐      ┌─────────────────────┐
│ModelRateLimit    │       │ContextWindow       │      │ModelProviderTimeout │
│Exception (429)   │       │ExceededException   │      │Exception (504)      │
│- retryAfterSecs  │       │(400)               │      │- timeoutMs          │
└──────────────────┘       │- actualTokens      │      └─────────────────────┘
                           │- maxAllowedTokens  │
                           └────────────────────┘
```

#### Base Exception Implementation: `GenAiException.java`
```java
package com.javagenai.day17.exception;

import java.util.Collections;
import java.util.Map;

public abstract class GenAiException extends RuntimeException {
    private final String errorCode;
    private final boolean retryable;
    private final String modelName;
    private final Map<String, Object> metadata;

    public GenAiException(
        String message,
        String errorCode,
        boolean retryable,
        String modelName,
        Map<String, Object> metadata,
        Throwable cause
    ) {
        super(message, cause);
        this.errorCode = errorCode;
        this.retryable = retryable;
        this.modelName = modelName;
        this.metadata = metadata != null ? Collections.unmodifiableMap(metadata) : Map.of();
    }

    public String getErrorCode() { return errorCode; }
    public boolean isRetryable() { return retryable; }
    public String getModelName() { return modelName; }
    public Map<String, Object> getMetadata() { return metadata; }
}
```

#### Specific Subtype: `ModelRateLimitException.java`
```java
package com.javagenai.day17.exception;

import java.util.Map;

public class ModelRateLimitException extends GenAiException {
    private final long retryAfterSeconds;

    public ModelRateLimitException(String modelName, long retryAfterSeconds, String detail) {
        super(
            detail,
            "AI_RATE_LIMIT_EXCEEDED",
            true,
            modelName,
            Map.of("retryAfterSeconds", retryAfterSeconds),
            null
        );
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() { return retryAfterSeconds; }
}
```

### 3.4 Implementing Distributed Traceability with MDC & Correlation IDs
In a microservice mesh, logs without trace identifiers are unsearchable:

```java
package com.javagenai.day17.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements Filter {

    public static final String CORRELATION_HEADER = "X-Correlation-Id";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) req;
        HttpServletResponse httpResponse = (HttpServletResponse) res;

        String correlationId = httpRequest.getHeader(CORRELATION_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = "corr_" + UUID.randomUUID().toString().substring(0, 8);
        }

        MDC.put("correlationId", correlationId);
        httpResponse.setHeader(CORRELATION_HEADER, correlationId);

        try {
            chain.doFilter(req, res);
        } finally {
            // CRITICAL: Always clean up MDC to prevent leaking IDs across thread-pool worker threads
            MDC.remove("correlationId");
        }
    }
}
```

---

## 4. Prerequisite & Supporting Concepts

### Prerequisite / Supporting Concept: Most-Specific Handler Selection
When an exception occurs, Spring calculates the **inheritance distance** between the thrown exception and the classes declared in `@ExceptionHandler` methods. For example, if both `handleGenAiException(GenAiException.class)` and `handleRateLimit(ModelRateLimitException.class)` exist, Spring will select `handleRateLimit` for a `ModelRateLimitException` because its inheritance distance is 0.

### Prerequisite / Supporting Concept: The Plain English Bridge to Exception Handling

| Concept | The Old Anti-Pattern | Modern Spring Boot Way | Why It Matters |
| :--- | :--- | :--- | :--- |
| **Try-Catch** | 15 lines of `try-catch` inside every single controller method | One centralized `@RestControllerAdvice` | Eliminates duplicated boilerplate; guarantees uniform responses |
| **Error Format** | Inconsistent custom maps or plain error strings | Standard RFC 7807 `ProblemDetail` | Predictable error structure for web and mobile frontends |
| **Log Forensic** | Searching through 100,000 log lines to find one failed request | Tracking requests with `X-Correlation-Id` and MDC | Find the complete lifecycle of any request within 5 seconds |
| **Internal Errors** | Exposing `e.printStackTrace()` to browser clients | Returning sanitized generic message + correlation ID | Prevents disclosing passwords, IP addresses, and CVE exploits |

---

## 5. Advanced Depth (Intermediate → Advanced)

### 5.1 Security Alert: Scrubbing Internal 500 Server Errors
> [!CAUTION]
> Never return `ex.getMessage()` or stack traces from the catch-all `Exception.class` handler to the public internet! (CWE-209: Information Exposure Through an Error Message).

An unhandled `SQLException` or network timeout might leak:
`postgresql://db_admin:master_key_99@10.0.1.5:5432/rag_vector_db`

#### The Enterprise Pattern:
1. Log the full internal stack trace stamped with the Correlation ID to private secure logs.
2. Return a sanitized, generic user-facing response embedding the Correlation ID.

```java
@ExceptionHandler(Throwable.class)
public ResponseEntity<ProblemDetail> handleUnexpectedError(Throwable ex, HttpServletRequest req) {
    String correlationId = MDC.get("correlationId");

    // 1. Log sensitive trace to internal secure log system
    logger.error("[{}] Internal server fault during {}: {}", correlationId, req.getRequestURI(), ex.getMessage(), ex);

    // 2. Return sanitized, safe response to external client
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        "An unexpected internal error occurred. Quote reference '" + correlationId + "' to support."
    );
    pd.setTitle("Internal Server Error");
    pd.setType(URI.create("https://api.enterprise-ai.internal/errors/internal-server-error"));
    pd.setInstance(URI.create(req.getRequestURI()));
    pd.setProperty("correlationId", correlationId);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(pd);
}
```

### 5.2 Common Mistakes & Misconceptions

#### Mistake 1: Forgetting to Clear MDC in a `finally` Block
Servlet containers (Tomcat, Jetty) maintain pools of reusable worker threads. If you fail to call `MDC.remove(...)` inside a `finally` block, the next completely unrelated HTTP request processed by that thread will inherit the previous request's correlation ID, creating phantom log traces.

#### Mistake 2: Returning HTTP `200 OK` with an Error Envelope
Some legacy APIs return HTTP 200 with `{ "success": false, "error": "Rate limit exceeded" }`. This breaks HTTP caching proxies, prevents API gateways from tracking 4xx/5xx failure rates, and disables browser fetch error handling. Always use appropriate HTTP status codes (`429`, `504`, `422`, `503`).

---

## 6. Quick Recap

| Concept | Spring / Java Construct | Primary Purpose | Enterprise AI Application |
| :--- | :--- | :--- | :--- |
| **Global Interceptor** | `@RestControllerAdvice` | Centralizes exception handling | Global safety net for all AI controllers |
| **Handler Method** | `@ExceptionHandler` | Maps exception to response | Transforms rate limits into 429 status |
| **Trace Identifier** | `MDC` + Filter | Stamped correlation tracking | Links client errors to backend server logs |
| **Response Format** | `ProblemDetail` (RFC 7807) | Standardized error payload | Predictable contract for frontend SDKs |
| **Security Shield** | Sanitized 500 handler | Prevents credential leaks | Protects vector database connection strings |

---

## 7. Self-Check Questions & Practice Exercises

### Self-Check Questions

1. **How does Spring choose which `@ExceptionHandler` to execute when multiple handlers match an inheritance hierarchy?**
   - *Answer*: Spring evaluates the inheritance distance between the thrown exception and the parameter types declared in `@ExceptionHandler` methods, selecting the most specific (lowest distance) handler.
2. **Why must `MDC.remove()` or `MDC.clear()` always be called inside a `finally` block in servlet filters?**
   - *Answer*: Because servlet containers reuse worker threads across different client requests. Failing to clear the MDC causes subsequent requests on that thread to inherit stale correlation IDs, corrupting log traces.
3. **Why should an LLM timeout return `504 Gateway Timeout` instead of `500 Internal Server Error`?**
   - *Answer*: `500 Internal Server Error` indicates an unexpected bug in your own code. `504 Gateway Timeout` indicates that your gateway timed out waiting for an upstream provider (e.g. OpenAI or Ollama), informing client SDKs that the request is retryable.
4. **What header must be included with an HTTP `429 Too Many Requests` response?**
   - *Answer*: The `Retry-After` header, indicating the number of seconds the client must wait before retrying the request.
5. **How does error sanitization protect an organization's internal security perimeter?**
   - *Answer*: It prevents sensitive internal architecture details—such as database connection URLs, credentials, internal hostnames, and library versions—from leaking in unhandled stack traces.

---

### Hands-On Practice Exercises

#### 🏋️ Exercise 1: Build a Content Moderation Filter Policy Exception Handler
**Objective**: Create a `ContentFilterBlockedException` and an `@ExceptionHandler` method that returns HTTP `403 Forbidden` with the flagged policy categories:

```java
package com.javagenai.day17;

import com.javagenai.day17.exception.GenAiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.List;
import java.util.Map;

public class ContentFilterBlockedException extends GenAiException {
    private final List<String> categories;

    public ContentFilterBlockedException(String modelName, List<String> categories) {
        super("Prompt blocked by safety filters: " + categories, "CONTENT_FILTER_VIOLATION", false, modelName, Map.of("categories", categories), null);
        this.categories = List.copyOf(categories);
    }

    public List<String> getCategories() { return categories; }
}

@RestControllerAdvice
class ContentModerationAdvice {

    @ExceptionHandler(ContentFilterBlockedException.class)
    public ResponseEntity<ProblemDetail> handleContentBlocked(ContentFilterBlockedException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN.value(), ex.getMessage());
        pd.setTitle("Content Moderation Policy Violation");
        pd.setType(URI.create("https://api.enterprise-ai.internal/errors/content-filter"));
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("categories", ex.getCategories());
        pd.setProperty("errorCode", ex.getErrorCode());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(pd);
    }
}
```

#### 🏋️ Exercise 2: Build a Resilience4j Circuit Breaker Fallback Handler
**Objective**: Build an exception handler for Resilience4j's `CallNotPermittedException` returning `503 Service Unavailable` with a `Retry-After: 60` header:

```java
package com.javagenai.day17;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

// Simulated CallNotPermittedException from Resilience4j
class CallNotPermittedException extends RuntimeException {
    public CallNotPermittedException(String message) { super(message); }
}

@RestControllerAdvice
class CircuitBreakerExceptionHandler {

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ProblemDetail> handleCircuitOpen(CallNotPermittedException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.SERVICE_UNAVAILABLE.value(),
            "AI inference circuit breaker is OPEN due to high failure rate. Traffic is being shed."
        );
        pd.setTitle("Service Unavailable");
        pd.setType(URI.create("https://api.enterprise-ai.internal/errors/circuit-breaker-open"));
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("retryable", true);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .header("Retry-After", "60")
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(pd);
    }
}
```

---

| ⬅️ Previous Day | 📚 Course Hub | ➡️ Next Day |
|:---|:---:|---:|
| [← Day 16: Request Validation, DTOs & Response Design](../Day_16_Validation_DTOs_Response_Design/Day_16_Validation_DTOs_Response_Design.md) | [All 60 Days Overview](../../README.md) | [Day 18: Async APIs, Streaming & SSE →](../Day_18_Async_Streaming_SSE/Day_18_Async_Streaming_SSE.md) |
