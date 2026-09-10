# Day 17: Exception Handling & Global Error Strategy

Hey friend! Welcome to Day 17. Today we're learning how to build a bulletproof safety net for our backend applications: **Global Exception Handling and Error Strategy**.

Here is an honest truth about building AI applications: **external AI services fail all the time!** 
- OpenAI might hit a rate limit (`429 Too Many Requests`) during peak hours.
- A local Ollama server might run out of GPU memory (`503 Service Unavailable`).
- A reasoning model might take 60 seconds to reply and cause a timeout (`504 Gateway Timeout`).

If your application crashes or spews ugly raw stack traces (with database passwords and internal URLs) back to your users, you're in trouble! Today, you and I will ensure our Spring Boot app handles every single error calmly, gracefully, and professionally.

---

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [Day 16: Request Validation, DTOs & Response Design](../Day_16_Validation_DTOs_Response_Design/Day_16_Validation_DTOs_Response_Design.md) | [All 60 Days Overview](../../README.md) | [Day 18: Async APIs, Streaming & SSE](../Day_18_Async_Streaming_SSE/Day_18_Async_Streaming_SSE.md) |

---

## 📌 What Will You Learn Today?

Today, you and I will master:
- **The Reality of AI Failures**: Why AI errors (rate limits, token timeouts) are routine events, not rare anomalies.
- **The `@RestControllerAdvice` Pattern**: Creating a single, centralized safety net that catches exceptions from any controller.
- **Tailored Error Protocols with `@ExceptionHandler`**: Mapping rate limits to `429`, timeouts to `504`, and validation errors to `422`.
- **Correlation IDs (Trace IDs)**: Generating a unique tracking number for every request so you can trace any issue in your server logs within seconds.
- **Sanitizing Errors**: Protecting sensitive database credentials and internal stack traces from leaking to public users.

---

> 💡 **New Word Alert: Error Handling Terms Demystified**
>
> 1. **Exception**: A runtime error that happens when something unexpected goes wrong in code (like a network timeout, missing file, or invalid input).
> 2. **`@RestControllerAdvice`**: A global safety net annotation in Spring Boot. It watches all your controllers, catches any uncaught exceptions thrown anywhere in your code, and turns them into clean, friendly JSON responses!
> 3. **`@ExceptionHandler`**: A method inside your advice class that handles one specific type of error (e.g., handling `RateLimitException` differently from `DatabaseException`).
> 4. **Correlation ID (Trace ID)**: A unique tracking number (like an Amazon package tracking ID or hospital wristband) attached to every incoming request. If a customer says *"Hey, my request failed"*, you search your server logs for that exact Correlation ID and instantly find what went wrong!
> 5. **Stack Trace Sanitization**: Hiding ugly technical errors (like database passwords or internal line numbers) from users. Instead of showing the user a terrifying 50-line error trace, you show them a polite message: *"Something went wrong. Reference ID: abc-123"*, while quietly saving the full technical trace in your private server logs.

---

## 🧭 The Plain English Bridge: Exception Handling Demystified

| Error Handling Concept | What It Does Under the Hood | Plain English Meaning |
| :--- | :--- | :--- |
| **`try-catch` in every method** | The old junior habit. Clutters every controller with 15 lines of boilerplate. | Like every employee in an office personally trying to put out kitchen fires with a bucket. |
| **`@RestControllerAdvice`** | Global interceptor that catches any uncaught exception across all controllers. | The building-wide automatic sprinkler and alarm system that activates instantly when smoke is detected. |
| **`Correlation ID`** | Generates a UUID in a servlet filter and logs it with Logback MDC. | A baggage claim tag or hospital wristband: one unique number tracks everything that happens to that specific request. |
| **`RFC 7807 ProblemDetail`** | Standard JSON format (`title`, `status`, `detail`, `instance`). | A polite, standardized incident report instead of yelling confusing technical jargon at the user. |

---

## Table of Contents

1. [Why This Day Matters for a 3-Year Enterprise Gen AI Engineer](#1-why-this-day-matters-for-a-3-year-enterprise-gen-ai-engineer)
2. [Real-World Analogy: Hospital Emergency Room Triage & Chart Tracking](#2-real-world-analogy-hospital-emergency-room-triage--chart-tracking)
3. [The Anatomy of Gen AI Failure Modes](#3-the-anatomy-of-gen-ai-failure-modes)
4. [Under the Hood: Spring MVC Exception Handling Architecture](#4-under-the-hood-spring-mvc-exception-handling-architecture)
5. [Designing the Enterprise Gen AI Exception Taxonomy](#5-designing-the-enterprise-gen-ai-exception-taxonomy)
6. [Correlation IDs & MDC: Distributed Traceability](#6-correlation-ids--mdc-distributed-traceability)
7. [The `@RestControllerAdvice` Pattern](#7-the-restcontrolleradvice-pattern)
8. [Security Alert: Sanitizing Internal 500 Server Errors](#8-security-alert-sanitizing-internal-500-server-errors)
9. [Hands-On Code Walkthrough](#9-hands-on-code-walkthrough)
10. [Step-by-Step Compilation & Execution](#10-step-by-step-compilation--execution)
11. [Hands-On Exercises (With Complete Solutions)](#11-hands-on-exercises-with-complete-solutions)
12. [Self-Check Quiz](#12-self-check-quiz)

---

## 1. Why This Day Matters for a 3-Year Enterprise Gen AI Engineer

When you connect a Spring Boot service to upstream AI providers (OpenAI, Anthropic, Azure OpenAI, Hugging Face, or a local Ollama cluster), failure is not an edge case—**it is the baseline reality**.

Consider what happens under real enterprise load:
1. **OpenAI Returns HTTP 429 Too Many Requests**: Your organization runs into its TPM (Tokens Per Minute) limit during peak hours. If your backend doesn't parse the `Retry-After` header and returns a generic `500 Internal Server Error`, your frontend clients immediately retry in a tight loop, triggering an exponential **thundering herd** that knocks down your entire gateway.
2. **Context Window Exceeded**: An automated RAG document pipeline retrieves 35 search chunks and appends them to a user prompt, totaling 145,000 tokens for a model with a 128,000 token limit. The upstream API responds with an unparseable error.
3. **GPU Inference Timeouts (HTTP 504)**: A large reasoning model (such as DeepSeek-R1 or o1) takes 65 seconds to generate complex architectural proofs. The default HTTP client timeout fires after 30 seconds.
4. **Credential & Stack Trace Leakage**: When an internal PostgreSQL vector database fails, an unhandled `SQLException` bubble leaks raw database URLs, database usernames, and internal table structures directly to the public web client.

A 3-year experienced backend engineer never allows unhandled exceptions to leave the server boundary. You design a **centralized, resilient exception handling strategy** with structured RFC 7807 problem details, correlation IDs, and clear retry semantics.

---

## 2. Real-World Analogy: Hospital Emergency Room Triage & Chart Tracking

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
Prescribes Digitalis   Applies Salve       Logs detailed pathology internally;
                                           gives family a calm summary (NO PANIC!)
```

Imagine an emergency hospital:
1. **Triage Wristband (Correlation ID)**: The moment a patient arrives, they receive a unique wristband ID. Every blood test, X-ray, prescription, and nurse's note references this ID. If an issue arises three days later, doctors don't search through thousands of file cabinets—they query that single patient ID.
2. **Specialized Treatment Teams (`@ExceptionHandler`)**: Cardiac arrests don't go to the dermatologist. Dedicated specialists handle specific emergencies with tailored protocols.
3. **Calm Public Communication (RFC 7807 Problem Details)**: When an unexpected surgical complication occurs, the chief surgeon doesn't throw a box of gory medical instruments and raw pathology reports into the waiting room. They give the family a calm, standardized briefing explaining what happened and provide the incident reference ID.

---

## 3. The Anatomy of Gen AI Failure Modes

In enterprise AI engineering, errors fall into three architectural categories:

| Category | HTTP Status | Retryable? | Example Causes | Enterprise Remedy |
| :--- | :---: | :---: | :--- | :--- |
| **Client Fault** | `400 Bad Request` | ❌ No | Context window exceeded, invalid prompt structure. | Truncate prompt or select higher-capacity model. |
| **Rate Limiting** | `429 Too Many Requests` | ✅ Yes | Upstream TPM/RPM limits exceeded. | Inspect `Retry-After` header; apply exponential backoff. |
| **Gateway Timeout** | `504 Gateway Timeout` | ✅ Yes | Deep reasoning models taking > 60s to generate first token. | Increase socket read timeout; switch to Server-Sent Events (SSE). |
| **Moderation Block** | `403 Forbidden` | ❌ No | Prompt triggered safety or self-harm filters. | Notify user of policy violation; redact flagged tokens. |
| **Infrastructure Fault** | `503 Service Unavailable` | ✅ Yes | Vector DB connection pool exhaustion, Ollama worker crash. | Trigger Circuit Breaker (Resilience4j); failover to replica. |
| **Internal Server Error** | `500 Internal Server Error` | ❌ No | Bug in application logic, unexpected null reference. | Sanitize message; log full stack trace with correlation ID. |

---

## 4. Under the Hood: Spring MVC Exception Handling Architecture

How does Spring MVC capture an exception thrown 5 layers deep inside an `@Service` or `@Repository`?

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant DS as DispatcherServlet
    participant CTL as RestController
    participant SVC as AIService (LLM Client)
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
    EHR-->>DS: ModelAndView (marked as resolved)
    DS-->>Client: 429 Too Many Requests (application/problem+json)
```

### The Exception Resolution Chain

When an exception bubbles out of a controller method:
1. `DispatcherServlet.doDispatch()` catches the `Exception` in a `catch (Exception ex)` block and delegates to `processHandlerException()`.
2. Spring queries the `HandlerExceptionResolverComposite`, which iterates through a list of resolvers:
   - **`ExceptionHandlerExceptionResolver`**: Inspects all `@ControllerAdvice` beans for `@ExceptionHandler` methods.
   - **`ResponseStatusExceptionResolver`**: Checks for `@ResponseStatus` on custom exceptions.
   - **`DefaultHandlerExceptionResolver`**: Maps standard Spring exceptions (e.g., `HttpRequestMethodNotSupportedException` -> 405).
3. If two `@ExceptionHandler` methods qualify (e.g. `handleGenAIException` vs `handleException`), Spring calculates **inheritance distance** and invokes the **most specific handler**.

---

## 5. Designing the Enterprise Gen AI Exception Taxonomy

Avoid scattering ad-hoc `RuntimeException`s across your codebase. Build a structured domain hierarchy:

```
                      ┌──────────────────────────────┐
                      │      java.lang.Exception     │
                      └──────────────┬───────────────┘
                                     │
                      ┌──────────────▼───────────────┐
                      │  java.lang.RuntimeException  │
                      └──────────────┬───────────────┘
                                     │
                      ┌──────────────▼───────────────┐
                      │        GenAIException        │
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

### Root Exception: `GenAIException.java`

```java
public abstract class GenAIException extends RuntimeException {
    private final String errorCode;
    private final boolean retryable;
    private final String modelName;
    private final Map<String, Object> metadata;

    public GenAIException(
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

---

## 6. Correlation IDs & MDC: Distributed Traceability

When an enterprise AI gateway handles 50,000 requests per minute across 10 Kubernetes pods, logs without correlation IDs are completely useless.

### What is MDC (Mapped Diagnostic Context)?
MDC is an SLF4J / Logback abstraction built on top of `ThreadLocal`. It allows you to stamp key-value pairs (like `correlationId`, `userId`, `tenantId`) into the current thread. Every logger call on that thread automatically includes those tags in the log output.

```
Incoming Request (Header: X-Correlation-Id: "corr_9a8b")
   │
   ▼
Servlet Filter:
   MDC.put("correlationId", "corr_9a8b");
   │
   ├── Controller: log.info("Generating prompt..."); 
   │   --> [corr_9a8b] INFO  - Generating prompt...
   │
   ├── LLM Client: log.warn("Upstream rate limit hit");
   │   --> [corr_9a8b] WARN  - Upstream rate limit hit
   │
   ▼
Filter finally:
   MDC.clear(); // Prevents thread-pool memory leaks!
```

---

## 7. The `@RestControllerAdvice` Pattern

`@RestControllerAdvice` is a meta-annotation that combines `@ControllerAdvice` and `@ResponseBody`. It tells Spring: *"Any JSON returned by these exception handler methods should be serialized directly into the HTTP response body."*

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ModelRateLimitException.class)
    public ResponseEntity<ProblemDetail> handleRateLimit(
            ModelRateLimitException ex, HttpServletRequest request) {

        String corrId = CorrelationContext.get();

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.TOO_MANY_REQUESTS.value(),
            ex.getMessage()
        );
        pd.setTitle("Rate Limit Exceeded");
        pd.setType(URI.create("https://api.enterprise-ai.internal/errors/rate-limit-exceeded"));
        pd.setInstance(URI.create(request.getRequestURI()));
        
        pd.setProperty("correlationId", corrId);
        pd.setProperty("errorCode", ex.getErrorCode());
        pd.setProperty("retryable", ex.isRetryable());
        pd.setProperty("retryAfterSeconds", ex.getRetryAfterSeconds());
        pd.setProperty("model", ex.getModelName());

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
            .header("X-Correlation-Id", corrId)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(pd);
    }
}
```

---

## 8. Security Alert: Sanitizing Internal 500 Server Errors

> **CRITICAL SECURITY RULE**: Never return `ex.getMessage()` or stack traces from the catch-all `Exception.class` handler to the public Internet!

### Why? (CWE-209: Information Exposure Through an Error Message)
An unhandled `SQLException` or `NullPointerException` might contain:
- Database hostnames, port numbers, and database usernames: `postgresql://postgres:master_key@10.0.1.5:5432/rag_db`
- Cloud storage keys or API tokens from environment configurations.
- Internal package names and library versions, helping attackers plan CVE exploits.

### The Production Pattern
1. Log the full stack trace and internal message to your internal secure logging system (Datadog, CloudWatch, Splunk) stamped with the **Correlation ID**.
2. Return a safe, generic message to the client, embedding the **Correlation ID** so they can quote it to support.

```java
@ExceptionHandler(Throwable.class)
public ResponseEntity<ProblemDetail> handleUnexpected(Throwable ex, HttpServletRequest req) {
    String corrId = CorrelationContext.get();
    
    // 1. Log sensitive stack trace INTERNALLY
    log.error("[{}] Internal server fault: {}", corrId, ex.getMessage(), ex);

    // 2. Return SANITIZED message to client
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        "An unexpected internal error occurred. Quote reference '" + corrId + "' to support."
    );
    pd.setProperty("correlationId", corrId);
    
    return ResponseEntity.status(500)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(pd);
}
```

---

## 9. Hands-On Code Walkthrough

In this day's companion code (`Phase_03_Spring_Web_REST_APIs/Day_17_Exception_Handling_Global_Strategy/code/`), we have implemented:

1. **`GenAIException.java`**: Root exception encapsulating error codes, model name, and retryability flags.
2. **`ModelRateLimitException.java`**: Domain exception modeling upstream HTTP 429 and `retryAfterSeconds`.
3. **`ContextWindowExceededException.java`**: Models token budget overflows (e.g., 145,200 tokens submitted vs 128,000 allowed).
4. **`ModelProviderTimeoutException.java`**: Models upstream inference timeouts.
5. **`CorrelationContext.java`**: Thread-safe correlation context tracking.
6. **`GlobalExceptionHandler.java`**: Enterprise controller advice simulating Spring MVC exception routing.
7. **`ExceptionHandlingDemo.java`**: Driver executing 4 realistic enterprise scenarios.

---

## 10. Step-by-Step Compilation & Execution

```powershell
# 1. Navigate to course root
cd "c:\Users\sriva\OneDrive\Desktop\GEN AI COURSE\JAVA"

# 2. Compile Day 17 code
javac Phase_03_Spring_Web_REST_APIs/Day_17_Exception_Handling_Global_Strategy/code/*.java

# 3. Execute ExceptionHandlingDemo
java -cp Phase_03_Spring_Web_REST_APIs/Day_17_Exception_Handling_Global_Strategy code.ExceptionHandlingDemo
```

### Verified Output

```
================================================================================
 DAY 17: EXCEPTION HANDLING & GLOBAL ERROR STRATEGY IN GEN AI SYSTEMS          
================================================================================

--- SCENARIO 1: Upstream Model Rate Limit (HTTP 429) ---
[req_openai_rate_9812] [WARN] Rate limit hit on model 'gpt-4o': OpenAI TPM threshold of 250,000 exceeded (Retry after 30s)
 Status: 429
 Headers: {Content-Type=application/problem+json, Retry-After=30, X-Correlation-Id=req_openai_rate_9812}
 Payload (application/problem+json):
{
  "type": "https://api.enterprise-ai.internal/errors/rate-limit-exceeded",
  "title": "Rate Limit Exceeded",
  "status": 429,
  "detail": "OpenAI TPM threshold of 250,000 exceeded for organization org_corp_123",
  "instance": "/api/v1/chat/completions",
  "correlationId": "req_openai_rate_9812",
  "errorCode": "AI_RATE_LIMIT_EXCEEDED",
  "retryable": true,
  "timestamp": "2026-09-09T08:57:09.857Z",
  "details": {
    "retryAfterSeconds": 30,
    "model": "gpt-4o"
  }
}

--- SCENARIO 4: Unexpected Exception Scrubbing (HTTP 500) ---
[req_db_crash_1104] [FATAL] Unexpected unhandled exception: SQLException: Connection to postgres://admin:superSecretP@ssword@db.internal:5432 failed
 Status: 500
 Headers: {X-Correlation-Id=req_db_crash_1104, Content-Type=application/problem+json}
 Payload (application/problem+json):
{
  "type": "https://api.enterprise-ai.internal/errors/internal-server-error",
  "title": "Internal Server Error",
  "status": 500,
  "detail": "An unexpected internal error occurred. Please quote correlation ID 'req_db_crash_1104' to support.",
  "instance": "/api/v1/prompts/templates",
  "correlationId": "req_db_crash_1104",
  "errorCode": "INTERNAL_ERROR",
  "retryable": false,
  "timestamp": "2026-09-09T08:57:09.899Z"
}
```

---

## 11. Hands-On Exercises (With Complete Solutions)

### Exercise 1: Content Moderation Policy Exception
**Task**: Define `ContentFilterBlockedException` representing an Azure OpenAI or Anthropic moderation violation (e.g., hate speech, violence, self-harm). Write the `@ExceptionHandler` method that returns HTTP `403 Forbidden` with the flagged categories list in the ProblemDetail payload.

#### Solution:
```java
public class ContentFilterBlockedException extends GenAIException {
    private final List<String> flaggedCategories;

    public ContentFilterBlockedException(String modelName, List<String> flaggedCategories) {
        super(
            "Prompt was blocked by safety filters due to violations in: " + flaggedCategories,
            "AI_CONTENT_POLICY_VIOLATION",
            false,
            modelName,
            Map.of("categories", flaggedCategories),
            null
        );
        this.flaggedCategories = List.copyOf(flaggedCategories);
    }

    public List<String> getFlaggedCategories() { return flaggedCategories; }
}

// In @RestControllerAdvice:
@ExceptionHandler(ContentFilterBlockedException.class)
public ResponseEntity<ProblemDetail> handleContentBlocked(
        ContentFilterBlockedException ex, HttpServletRequest req) {

    ProblemDetail pd = ProblemDetail.forStatusAndDetail(
        HttpStatus.FORBIDDEN.value(),
        ex.getMessage()
    );
    pd.setTitle("Content Moderation Policy Violation");
    pd.setType(URI.create("https://api.enterprise-ai.internal/errors/content-filter"));
    pd.setInstance(URI.create(req.getRequestURI()));
    pd.setProperty("flaggedCategories", ex.getFlaggedCategories());
    pd.setProperty("errorCode", ex.getErrorCode());
    pd.setProperty("correlationId", CorrelationContext.get());

    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(pd);
}
```

---

### Exercise 2: Correlation ID Servlet Filter
**Task**: Implement a standard `Filter` that extracts the `X-Correlation-Id` header from an incoming HTTP request, creates one if missing, populates SLF4J MDC, appends it to the HTTP response header, and guarantees MDC cleanup.

#### Solution:
```java
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
            MDC.remove("correlationId"); // Essential to prevent thread leak in pooled threads
        }
    }
}
```

---

### Exercise 3: Resilience4j Circuit Breaker Open Exception Handler
**Task**: When Resilience4j's circuit breaker opens due to an upstream Ollama or vector database outage, it throws `CallNotPermittedException`. Write an `@ExceptionHandler` that returns HTTP `503 Service Unavailable` with a `Retry-After: 60` header.

#### Solution:
```java
@ExceptionHandler(CallNotPermittedException.class)
public ResponseEntity<ProblemDetail> handleCircuitOpen(
        CallNotPermittedException ex, HttpServletRequest req) {

    ProblemDetail pd = ProblemDetail.forStatusAndDetail(
        HttpStatus.SERVICE_UNAVAILABLE.value(),
        "AI inference circuit breaker is OPEN due to upstream failure rate. Requests are temporarily shed."
    );
    pd.setTitle("Service Unavailable");
    pd.setType(URI.create("https://api.enterprise-ai.internal/errors/circuit-breaker-open"));
    pd.setInstance(URI.create(req.getRequestURI()));
    pd.setProperty("circuitBreakerName", ex.getCausingCircuitBreakerName());
    pd.setProperty("retryable", true);
    pd.setProperty("correlationId", CorrelationContext.get());

    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .header("Retry-After", "60")
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(pd);
}
```

---

## 12. Self-Check Quiz

### Q1: What happens if two `@ExceptionHandler` methods in the same `@RestControllerAdvice` match an exception (e.g. one matches `GenAIException` and one matches `ModelRateLimitException`)?
> **Answer**: Spring selects the **most specific handler**. It measures the inheritance depth between the thrown exception and the parameter type in the handler. Since `ModelRateLimitException` is a direct subtype of `GenAIException`, the distance to `ModelRateLimitException` is 0, so Spring selects `handleRateLimit()`.

### Q2: Why is calling `MDC.clear()` or `MDC.remove()` inside a `finally` block mandatory in servlet filters?
> **Answer**: In Tomcat / Jetty / Spring Boot, HTTP worker threads belong to a **thread pool** and are reused across different client requests. If you fail to clear the MDC in a `finally` block, the next completely unrelated HTTP request processed by that thread will inherit the previous request's `correlationId`, polluting your production logs with misleading traces.

### Q3: Why is `504 Gateway Timeout` preferred over `500 Internal Server Error` when an LLM inference call times out?
> **Answer**: `500 Internal Server Error` implies an unrecoverable bug in your own application code. `504 Gateway Timeout` indicates that your gateway acted as a reverse proxy or intermediary, and the upstream server (OpenAI, Anthropic, or local GPU cluster) took longer to respond than your configured timeout threshold. This signals to client SDKs that the issue is upstream and may be safely retried.

### Q4: When should you include a `Retry-After` header in an HTTP error response?
> **Answer**: You should include `Retry-After` whenever the response status is `429 Too Many Requests` (specifying how many seconds until the rate-limit bucket replenishes) or `503 Service Unavailable` (specifying when maintenance or circuit breaker cool-down is expected to finish).

### Q5: Can `@ExceptionHandler` methods be declared inside a standard `@RestController` instead of `@RestControllerAdvice`?
> **Answer**: Yes. An `@ExceptionHandler` declared inside an individual `@RestController` handles exceptions thrown **only within that specific controller**. An `@ExceptionHandler` declared inside `@RestControllerAdvice` applies globally across all controllers in the application.

---

## Day 17 Summary & Next Steps

You've turned unexpected failures into smooth, professional responses today! Let's review what you've achieved:
1. **The Global Safety Net**: You used `@RestControllerAdvice` to catch errors from anywhere in your app without writing `try-catch` blocks in every method.
2. **Specialized Doctors**: You mapped different exception types to precise HTTP status codes (`429`, `504`, `422`).
3. **Correlation IDs**: You added unique tracking IDs so you can debug any user's issue in your server logs in seconds.
4. **Leak-Proof Errors**: You sanitized internal errors so hackers never see your database passwords or internal code structure.

👉 **Tomorrow in Day 18: Async APIs, Streaming & Server-Sent Events (SSE)** — Waiting 30 seconds for an AI to finish writing a whole paragraph in silence is a terrible user experience. Tomorrow, we'll learn how to build asynchronous streaming in Spring Boot using `SseEmitter` and Virtual Threads so words appear on screen in real time! See you tomorrow! ⚡🌊

