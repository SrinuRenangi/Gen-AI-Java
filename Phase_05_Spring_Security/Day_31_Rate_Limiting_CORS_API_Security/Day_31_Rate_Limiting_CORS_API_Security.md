# Day 31: Rate Limiting, CORS & API Security
## Defending Against Denial-of-Wallet (DoW), Token-Bucket Rate Limiting (Bucket4j), CORS Hardening & OWASP Headers

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [◀ Day 30: OAuth2 & Social Login](../Day_30_OAuth2_Social_Login/Day_30_OAuth2_Social_Login.md) | [All 60 Days Overview](../../README.md) | [Day 32: Introduction to Spring AI ▶](../../Phase_06_Spring_AI/Day_32_Introduction_to_Spring_AI/Day_32_Introduction_to_Spring_AI.md) |

---

## Friendly Welcome: Protecting Your Company's Wallet

Hey there, friend! Welcome to Day 31—the grand finale of **Phase 5: Spring Security**!

Think about an exclusive VIP nightclub serving $500 vintage champagne. If the club had an open door with no bouncer and an all-you-can-drink free-for-all, a single rowdy party could drink the entire wine cellar dry in two hours, bankrupting the club before midnight!

In Generative AI engineering, calling models like GPT-4o or Claude is just like that $500 vintage champagne: every single request costs real money on your company credit card. If a bot discovers an unthrottled endpoint or a developer accidentally writes an infinite loop in a script, your company could wake up to an **$80,000 cloud bill**! This attack even has an official name in cybersecurity: **Denial of Wallet (DoW)**.

Today, we are going to build an impenetrable defense using **Token-Bucket Rate Limiting with Bucket4j**, lock down browser access with **CORS**, and install essential OWASP security headers so your AI platform stays fast, reliable, and financially safe!

---

> 💡 **New Word Alert! Key Concepts for Today**
>
> - **Rate Limiting**: Putting a speed limit on your API. For example: *"You are allowed at most 10 AI prompts per minute."*
> - **Denial of Wallet (DoW)**: A scary cyber-attack where an attacker floods your pay-per-token AI endpoints with expensive requests, specifically designed to run up huge bills on your company's credit card until you run out of money!
> - **Token Bucket Algorithm (Bucket4j)**: A brilliant rate-limiting model. Imagine a physical bucket that holds 10 tokens. Every request takes 1 token out. Tokens drip back into the bucket at a steady rate (e.g. 1 token per second). If the bucket is empty, requests are rejected with `429 Too Many Requests`!
> - **HTTP 429 Too Many Requests**: The official HTTP status code that says: *"Slow down! You've exceeded your allowed rate limit."* It usually comes with a `Retry-After: 15` header telling the client how many seconds to wait.
> - **CORS (Cross-Origin Resource Sharing)**: A browser security mechanism that stops malicious third-party websites from secretly sending requests to your API in the background using a user's browser.
> - **OWASP Security Headers**: Essential HTTP response headers (like `Content-Security-Policy`, `X-Frame-Options`, and `X-Content-Type-Options: nosniff`) that protect your users from clickjacking and script injection attacks.

---

## What Will You Learn Today?

Congratulations on reaching the final day of **Phase 5: Spring Security**! Over the past four days, you built authentication, JWT validation, role-based method guards, and enterprise OAuth2 resource server mechanics.

However, deploying an AI application without **API Security and Rate Limiting** is like leaving a company credit card on a public sidewalk. In conventional CRUD applications, an unthrottled API endpoint consumes a few megabytes of RAM and CPU cycles. In a Generative AI application, an unthrottled endpoint calling GPT-4o, Claude 3.5 Sonnet, or Gemini 1.5 Pro will burn through a **$10,000 cloud budget in less than an hour**—an attack known as **Denial of Wallet (DoW)**.

Today, you will master production API security for Generative AI applications using Spring Boot 3 (Spring Security 6) and Java 21:
- The economics of AI endpoints: Why conventional rate limiting is insufficient and how to defend against **Denial of Wallet (DoW)** and **Prompt Injection DoS**.
- The **Token Bucket algorithm** (the math behind Bucket4j) and why it is superior for handling bursty AI traffic.
- Multi-dimensional rate limiting: Throttling by Client IP, User Tier, and actual **LLM token consumption**.
- Returning compliant RFC 6585 headers: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`, and `Retry-After` with HTTP `429 Too Many Requests`.
- Hardening **Cross-Origin Resource Sharing (CORS)** for browser chat interfaces without opening security holes.
- Implementing defense-in-depth **OWASP Security Headers** (`Content-Security-Policy`, `HSTS`, `X-Frame-Options`, `nosniff`).
- Protecting streaming Server-Sent Events (SSE) connections from connection exhaustion (Slowloris attacks).

---

## Real-World Analogy: Nightclub Doorman & The Metered Bar Tab

Imagine an exclusive VIP lounge with an open bar serving $500 vintage champagne:

```
+---------------------------------------------------------------------------------------------------+
|                                  THE AI NIGHTCLUB DEFENSE MODEL                                   |
|                                                                                                   |
|  1. THE DOORMAN (CORS & IP Rate Limiting)                                                         |
|  - Checks if you came from a reputable hotel (Whitelisted Origin).                                |
|  - If 50 people suddenly rush the front door from the same suspicious alleyway (IP flood),         |
|    the doorman tells them: "Wait outside for 60 seconds!" (HTTP 429 Too Many Requests).           |
|                                                                                                   |
|  2. THE DRESS CODE & ENTRY PASS (JWT / OAuth2 Authentication)                                     |
|  - Confirms you have a valid VIP wristband before you sit at a booth.                             |
|                                                                                                   |
|  3. THE METERED BAR TAB (Token-Bucket Rate Limiter / Quota)                                       |
|  - You cannot drink unlimited $500 champagne. Your wristband has 3 drink tokens per hour.        |
|  - When your 3 tokens are gone, the bartender slides a card saying: "Next pour available at 10:15pm"|
|    (Retry-After: 15s).                                                                            |
|  - RESULT: The bar never goes bankrupt from a rogue customer drinking the entire cellar!         |
+---------------------------------------------------------------------------------------------------+
```

In your Gen AI backend:
- The **Lightweight Model / Free Tier** is like draft beer.
- The **GPT-4o / Claude 3.5 Sonnet / 1M-context Gemini** model is the $500 vintage champagne.
- Rate limiting ensures no single customer or rogue loop can bankrupt your business!

---

## The Economics of AI APIs: Denial-of-Wallet (DoW) & Prompt Injection DoS

Why is API security fundamentally different for Generative AI systems compared to traditional REST APIs?

| Threat Dimension | Traditional Web API (e.g. CRUD User) | Generative AI API (e.g. LLM / RAG) |
|:---|:---|:---|
| **Cost Per Request** | $0.000001 (Negligible database CPU query). | **$0.01 – $0.20** per API call! |
| **Execution Latency** | 5ms – 50ms. | **2,000ms – 30,000ms** (Streaming LLM inference). |
| **Compute Consumption** | Instantaneous I/O bound. | GPU clusters running matrix multiplication. |
| **Payload Vulnerability** | SQL Injection, XSS. | **Denial of Wallet**, Prompt Injection, Memory Exhaustion. |
| **Attacker Objective** | Data exfiltration or server crash. | Financial exhaustion of the target company. |

```
                               DENIAL-OF-WALLET (DoW) ATTACK VECTOR
                               
 Attacker (Automated Script)
           │
           │ Sends 1,000 requests/minute to: POST /api/v1/ai/generate
           │ Payload: "Generate a 10,000 word philosophical treatise comparing..."
           ▼
┌────────────────────────────────────────────────────────────────────────┐
│ UNPROTECTED SPRING BOOT BACKEND                                        │
│                                                                        │
│ Forwards all 1,000 requests to OpenAI / Anthropic / Bedrock:           │
│ 1,000 requests * 4,000 tokens * $0.015 / 1k tokens = $60 PER MINUTE!  │
│                                                                        │
│ 💥 After 1 hour: $3,600 bill!                                          │
│ 💥 After 24 hours: $86,400 bill -> Startup Bankruptcy!                │
└────────────────────────────────────────────────────────────────────────┘
```

With **Token-Bucket Rate Limiting (Day 31)** in place:
1. The 4th request from the unauthorized IP is rejected with `429 Too Many Requests` in **0.2 milliseconds**.
2. Zero requests hit OpenAI.
3. Cost to your company: **$0.00**.

---

## Rate Limiting Algorithms Deep Dive

There are four primary rate limiting algorithms. Here is how they compare:

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                               RATE LIMITING ALGORITHMS                                  │
├───────────────────────┬───────────────────────────────────┬─────────────────────────────┤
│ Algorithm             │ How It Works                      │ Best For                    │
├───────────────────────┼───────────────────────────────────┼─────────────────────────────┤
│ 1. Token Bucket       │ Tokens refill at fixed rate into   │ Bursty traffic, AI APIs     │
│    (Bucket4j / Redis) │ a bucket with max capacity.       │ (Industry Standard)         │
├───────────────────────┼───────────────────────────────────┼─────────────────────────────┤
│ 2. Leaky Bucket       │ Requests queue up and process     │ Smooth constant-rate        │
│                       │ at a constant output drip rate.   │ message queues              │
├───────────────────────┼───────────────────────────────────┼─────────────────────────────┤
│ 3. Fixed Window       │ Resets counter at top of minute   │ Basic APIs; vulnerable to   │
│                       │ (e.g. 100 requests per 12:00-12:01)│ boundary burst attacks      │
├───────────────────────┼───────────────────────────────────┼─────────────────────────────┤
│ 4. Sliding Window Log │ Tracks timestamp of every call.   │ Precise mathematical bounds,│
│                       │ High memory overhead.             │ low traffic volumes         │
└───────────────────────┴───────────────────────────────────┴─────────────────────────────┘
```

### The Token Bucket Algorithm in Action
The **Token Bucket** algorithm is universally favored for AI applications because it naturally accommodates **bursts**:
- An engineer writing code might run 3 AI prompt tests in 10 seconds (burst allowed), then read the results for 2 minutes (bucket refills).

```
                      THE TOKEN BUCKET MECHANISM
                      
   Refill Stream (e.g., 2 tokens added every second)
          │
          │  💧 💧 💧
          ▼
    ┌───────────┐
    │  Capacity │  Max 10 Tokens (Burst Limit)
    │ ═════════ │
    │ 🟡 🟡 🟡  │  Available Tokens (Ready for immediate use)
    │ 🟡 🟡 🟡  │
    └─────┬─────┘
          │
          │ Request arrives -> Consumes 1 Token
          ▼
    ┌───────────┐
    │  Decision │ ──► Tokens >= 1? ──► ✅ Allow request (Tokens--)
    └───────────┘                    └──► ❌ Reject: HTTP 429 Too Many Requests!
```

---

## Standard RFC 6585 Headers

When building an enterprise API, you must never return a plain `429` without context. Your response must include the standard RFC 6585 and IETF rate limiting headers:

```http
HTTP/1.1 429 Too Many Requests
Content-Type: application/json
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1788956400
Retry-After: 15

{
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Try again in 15 seconds.",
  "retryAfterSeconds": 15
}
```

- `X-RateLimit-Limit`: Maximum tokens available in the bucket.
- `X-RateLimit-Remaining`: Tokens currently remaining in the bucket.
- `X-RateLimit-Reset`: Unix timestamp when the bucket will be completely full.
- `Retry-After`: Number of seconds the client must wait before making another attempt.

---

## CORS (Cross-Origin Resource Sharing) Hardening

Because modern AI chat interfaces (e.g., React, Next.js, Vue) typically run on a different domain or port (e.g., `https://chat.mycompany.com` or `http://localhost:3000`) than the Spring Boot API backend (`https://api.mycompany.com:8080`), web browsers enforce the **Same-Origin Policy (SOP)**.

### The CORS Pre-flight (`OPTIONS`) Handshake:

```
 Browser (chat.mycompany.com)                      Spring Boot (api.mycompany.com)
            │                                                      │
            │ 1. Preflight Request:                                │
            │    OPTIONS /api/v1/ai/stream                         │
            │    Origin: https://chat.mycompany.com                │
            │    Access-Control-Request-Method: POST               │
            │    Access-Control-Request-Headers: Authorization     │
            ├─────────────────────────────────────────────────────>│
            │                                                      │
            │ 2. Preflight Response:                               │
            │    HTTP/1.1 204 No Content                           │
            │    Access-Control-Allow-Origin: https://chat...      │
            │    Access-Control-Allow-Methods: POST, GET, OPTIONS  │
            │    Access-Control-Allow-Headers: Authorization, ...  │
            │    Access-Control-Max-Age: 3600                      │
            │<─────────────────────────────────────────────────────┤
            │                                                      │
            │ 3. Actual Request:                                   │
            │    POST /api/v1/ai/stream (Bearer JWT)               │
            ├─────────────────────────────────────────────────────>│
            │                                                      │
            │ 4. HTTP/1.1 200 OK (Stream AI response)              │
            │<─────────────────────────────────────────────────────┤
```

### Critical CORS Vulnerabilities to Avoid:
1. **Never use wildcard with credentials**: `allowedOrigins("*")` combined with `allowCredentials(true)` is rejected by modern browsers and leaks session cookies.
2. **Preflight rejection**: If your Spring Security filter chain requires authentication for `OPTIONS` requests, browsers will fail before ever sending the actual POST request! Always permit `HttpMethod.OPTIONS` or use Spring's `CorsFilter`.

---

## Hardening Security Headers (OWASP Recommendations)

Modern web security requires defense-in-depth headers configured on every response:

```java
http.headers(headers -> headers
    // 1. Prevent MIME-sniffing: Browser must respect the declared Content-Type
    .contentTypeOptions(Customizer.withDefaults()) // X-Content-Type-Options: nosniff
    
    // 2. Prevent Clickjacking: Never allow your API or login pages inside an iframe
    .frameOptions(frame -> frame.deny())           // X-Frame-Options: DENY
    
    // 3. Enforce HTTPS: Instruct browsers to only communicate over TLS for 1 year
    .httpStrictTransportSecurity(hsts -> hsts
        .includeSubDomains(true)
        .maxAgeInSeconds(31536000)
    )
    
    // 4. Content Security Policy (CSP): Restrict script, frame, and connect sources
    .contentSecurityPolicy(csp -> csp
        .policyDirectives("default-src 'self'; frame-ancestors 'none'")
    )
);
```

### What about CSRF (Cross-Site Request Forgery)?
> [!NOTE]
> **Why we disable CSRF for REST APIs:**
> In stateless REST APIs that authenticate exclusively using HTTP `Authorization: Bearer <JWT>` headers, **CSRF attacks are mathematically impossible**. CSRF relies on browsers automatically attaching session cookies. Browsers NEVER automatically attach custom `Authorization` headers!
> Therefore:
> ```java
> http.csrf(csrf -> csrf.disable());
> ```
> is safe and standard practice for stateless APIs. (If your application uses session cookies, CSRF must remain enabled).

---

## Step-by-Step Production Code Walkthrough

Let's review the runnable companion code built for today's lesson in `Phase_05_Spring_Security/Day_31_Rate_Limiting_CORS_API_Security/code/`:

### 1. `TokenBucket.java`
Implements the high-performance Token Bucket algorithm with nanosecond precision:

```java
public synchronized boolean tryConsume(long tokens) {
    refill();
    if (availableTokens >= tokens) {
        availableTokens -= tokens;
        return true;
    }
    return false;
}

private void refill() {
    long now = System.nanoTime();
    long elapsedNanos = now - lastRefillNanos;
    if (elapsedNanos > 0) {
        double newlyGenerated = elapsedNanos * refillTokensPerNano;
        availableTokens = Math.min(capacity, availableTokens + newlyGenerated);
        lastRefillNanos = now;
    }
}
```

### 2. `CorsPolicyValidator.java`
Validates origins and generates standard CORS preflight headers:

```java
public CorsValidationResult validateAndBuildHeaders(String origin, String method, String requestHeaders) {
    if (origin == null || origin.isBlank()) {
        return new CorsValidationResult(true, null, Map.of());
    }

    if (!allowedOrigins.contains("*") && !allowedOrigins.contains(origin)) {
        return new CorsValidationResult(false, "CORS origin '" + origin + "' is not whitelisted", Map.of());
    }

    Map<String, String> headers = new HashMap<>();
    headers.put("Access-Control-Allow-Origin", allowedOrigins.contains("*") ? "*" : origin);
    headers.put("Access-Control-Allow-Methods", String.join(", ", allowedMethods));
    headers.put("Access-Control-Allow-Headers", String.join(", ", allowedHeaders));
    headers.put("Access-Control-Max-Age", "3600");
    return new CorsValidationResult(true, null, headers);
}
```

### 3. `AiSecurityGateway.java`
Integrates CORS checking, OWASP headers, payload size limits (DoS defense), and token-bucket throttling:

```java
boolean consumed = bucket.tryConsume(1);
if (!consumed) {
    long retryAfterSeconds = bucket.getSecondsUntilNextToken();
    responseHeaders.put("Retry-After", String.valueOf(retryAfterSeconds));
    responseHeaders.put("X-RateLimit-Remaining", "0");
    return new HttpResponse(
            429,
            "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please retry in " + retryAfterSeconds + "s.\"}",
            responseHeaders
    );
}
```

### 4. Running the Verification Suite
Compile and execute the simulation:

```bash
javac -d out Phase_05_Spring_Security/Day_31_Rate_Limiting_CORS_API_Security/code/*.java
java -cp out com.genai.security.apisec.ApiSecurityDemo
```

Output:
```text
================================================================================
  DAY 31: API SECURITY, RATE LIMITING & CORS DEFENSE DEMONSTRATION             
================================================================================

[TEST 1] Testing Allowed CORS Request from Whitelisted Origin...
  Status Code: 200
  CORS Origin: https://chat.myenterprise.com
  CSP Header:  default-src 'self'
  Rate Remaining: 2
  ✅ TEST 1 PASSED!

[TEST 2] Testing Blocked CORS Request from Rogue Origin...
  Status Code: 403 (CORS Error: CORS origin 'https://evil-hacker.com' is not whitelisted)
  ✅ TEST 2 PASSED: Rogue CORS request blocked!

[TEST 3] Testing CORS Preflight OPTIONS Request...
  Status Code: 204
  Allowed Methods: POST, GET, OPTIONS
  ✅ TEST 3 PASSED: Preflight handled cleanly with 204 No Content.

[TEST 4 & 5] Simulating Rapid AI Inference Requests (Burst Capacity = 3)...
  Request #1 -> Status: 200 | Remaining: 2 | Retry-After: 0s
  Request #2 -> Status: 200 | Remaining: 1 | Retry-After: 0s
  Request #3 -> Status: 200 | Remaining: 0 | Retry-After: 0s
  Request #4 -> Status: 429 | Remaining: 0 | Retry-After: 1
  Request #5 -> Status: 429 | Remaining: 0 | Retry-After: 1

[TEST 6] Waiting 600ms for Token Bucket to Refill...
  Retry after wait -> Status: 200 | Remaining: 0
  ✅ TEST 6 PASSED: Request succeeded after token refill.

[TEST 7] Testing Oversized Prompt Payload DoS Guard (>64KB)...
  Status Code: 413 (Payload Too Large: Maximum prompt size is 64KB)
  ✅ TEST 7 PASSED: Giant prompt rejected before reaching JSON parser.

================================================================================
  ALL API SECURITY, RATE LIMITING & CORS TESTS PASSED!                         
================================================================================
```

---

## Why It Matters for Gen AI Applications

| Attack / Risk Vector | Without Day 31 Defenses | With Day 31 Defenses |
|:---|:---|:---|
| **Denial of Wallet (DoW)** | Attacker bombards endpoints with loops, incurring $10,000+ bills from OpenAI / Anthropic. | Bucket4j halts callers after burst capacity; requests fail with HTTP 429 before invoking LLMs. |
| **Cross-Site Prompt Stealing** | Malicious websites read AI responses via unrestricted browser CORS. | Strict origin whitelisting ensures only your trusted frontend domain receives chat completions. |
| **Prompt Injection Payload DoS** | Attacker posts 50MB junk text files into the prompt parser, crashing JVM heap with OutOfMemoryError. | Payload filter checks `Content-Length` and rejects oversized prompts with `413 Payload Too Large`. |
| **Slowloris Connection Exhaustion** | Attackers hold hundreds of streaming SSE connections open without reading tokens, tying up Tomcat threads. | Asynchronous timeouts automatically disconnect idle clients after 15 seconds of inactivity. |

---

## Hands-On Exercises (With Complete Solutions)

### Exercise 1: Token-Weighted Rate Limiting
**Problem Statement:**  
In conventional rate limiting, each HTTP request costs `1` token. In Generative AI, a request asking for a 4,000-token summary is 40x more expensive than a request asking for a 100-word definition.  
Write a method `calculateTokenCost(String prompt)` that estimates token consumption (assume roughly 1 token per 4 characters), and adjust the rate limiter to deduct that estimated weight from the user's bucket.

<details>
<summary>👉 View Solution</summary>

```java
public class AiTokenMeterService {

    private final RateLimiterRegistry registry;

    public AiTokenMeterService(RateLimiterRegistry registry) {
        this.registry = registry;
    }

    public boolean tryConsumeAiTokens(String userId, String prompt) {
        long estimatedTokens = Math.max(1, (long) Math.ceil(prompt.length() / 4.0));
        TokenBucket bucket = registry.resolveBucket("user:" + userId);
        return bucket.tryConsume(estimatedTokens);
    }
}
```
*Explanation:* Instead of treating all HTTP requests as equal, token-weighted rate limiting meters the actual computational and financial weight of the prompt before forwarding it to the LLM provider.
</details>

---

### Exercise 2: Spring Boot `CorsConfigurationSource` Bean
**Problem Statement:**  
Configure a Spring Security `CorsConfigurationSource` bean that allows `https://chat.enterprise.com` and `http://localhost:3000` to execute `GET`, `POST`, and `DELETE` requests, allows headers `Authorization` and `Content-Type`, exposes `X-RateLimit-Remaining` to the browser, and caches preflight decisions for 1 hour.

<details>
<summary>👉 View Solution</summary>

```java
package com.genai.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("https://chat.enterprise.com", "http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Workspace-Id"));
        config.setExposedHeaders(List.of("X-RateLimit-Limit", "X-RateLimit-Remaining", "Retry-After"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L); // Cache preflight for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```
</details>

---

### Exercise 3: Server-Sent Events (SSE) Streaming Connection Timeout Guard
**Problem Statement:**  
When an AI endpoint streams tokens to a frontend via `SseEmitter`, a slow client can hang the connection indefinitely. Write a Spring MVC controller endpoint with an explicit 30-second timeout that closes the emitter and cleans up resources if the client stalls.

<details>
<summary>👉 View Solution</summary>

```java
@RestController
@RequestMapping("/api/v1/ai")
public class StreamingAiController {

    private final AiStreamingService streamingService;

    public StreamingAiController(AiStreamingService streamingService) {
        this.streamingService = streamingService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChatResponse(@RequestParam String prompt) {
        // Configure 30,000ms (30 second) timeout
        SseEmitter emitter = new SseEmitter(30_000L);

        emitter.onTimeout(() -> {
            emitter.complete();
            System.err.println("SSE Stream timed out to prevent thread starvation!");
        });

        emitter.onError(ex -> {
            emitter.completeWithError(ex);
            System.err.println("SSE Stream error: " + ex.getMessage());
        });

        // Launch async worker thread to stream tokens
        Thread.startVirtualThread(() -> {
            try {
                streamingService.streamTokens(prompt, token -> {
                    emitter.send(SseEmitter.event().data(token));
                });
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
```
</details>

---

## 5-Question Self-Check Quiz

#### 1. What is a "Denial of Wallet" (DoW) attack in the context of Generative AI?
- A) Stealing a user's credit card credentials via an SQL injection flaw.
- B) Flooding an AI application with expensive prompt requests that exhaust its cloud or LLM API billing quota without crashing the server.
- C) Disabling cryptocurrency wallets on the server.
- D) A ransomware virus targeting enterprise payroll databases.

#### 2. Why is the Token Bucket algorithm preferred over the Fixed Window algorithm for AI chat APIs?
- A) Fixed Window requires double the memory of Token Bucket.
- B) Token Bucket gracefully handles bursty interactive traffic while enforcing an average rate over time, preventing sudden boundary bursts.
- C) Token Bucket only works on Windows OS.
- D) Fixed Window does not support multithreading.

#### 3. What HTTP status code should your backend return when a user exhausts their rate limit?
- A) `401 Unauthorized`
- B) `403 Forbidden`
- C) `429 Too Many Requests`
- D) `503 Service Unavailable`

#### 4. Which HTTP header informs the client how many seconds they must wait before making another request after hitting a rate limit?
- A) `X-Wait-Time`
- B) `Retry-After`
- C) `RateLimit-Delay`
- D) `Backoff-Seconds`

#### 5. Why is CSRF protection usually disabled (`http.csrf(csrf -> csrf.disable())`) in stateless REST backends using Bearer JWTs?
- A) Because CSRF is an obsolete security vulnerability that no longer exists in modern browsers.
- B) Because browsers do not automatically send custom `Authorization: Bearer <token>` headers on cross-site requests, making CSRF mathematically impossible for stateless token APIs.
- C) Because Spring Boot 3 no longer supports CSRF.
- D) Because CSRF slows down JSON parsing by 50%.

---

### Quiz Answers & Explanations

1. **B is correct**: Denial of Wallet targets the variable pay-per-token economics of commercial LLM APIs, intentionally driving massive API bills to financially ruin the service provider.
2. **B is correct**: The Token Bucket algorithm allows burst capacity up to the bucket's maximum size, which aligns with human typing and testing patterns, while refilling steadily at a constant rate.
3. **C is correct**: RFC 6585 defines `429 Too Many Requests` specifically for rate limiting and throttling.
4. **B is correct**: The standard `Retry-After` header indicates how many seconds (or a date) until the client can retry.
5. **B is correct**: CSRF exploits automatic browser credential attachment (cookies, basic auth). When authentication requires an explicit `Authorization: Bearer` header, third-party sites cannot forge requests.

---

## Phase 5 Retrospective & What's Next!

🎉 **PHASE 5 IS OFFICIALLY 100% COMPLETE!**

Give yourself a huge round of applause! You have conquered one of the most vital engineering phases in enterprise software: **Security**.

Look at the fortress you built:
- **Day 27**: You mastered the Spring Security Filter Chain and modern stateless architecture.
- **Day 28**: You forged custom JWT digital passports with tamper-proof HMAC signatures.
- **Day 29**: You locked the cockpit door with `@PreAuthorize`, SpEL, and role hierarchies.
- **Day 30**: You integrated Google/GitHub SSO and asymmetric RS256 token verification via JWKS.
- **Day 31**: You defended your company against Denial-of-Wallet attacks with Token-Bucket rate limiting and hardened CORS.

Your application is now **fortified like an enterprise Swiss bank vault**.

---

### 🚀 Entering Phase 6: Spring AI — The Core Framework (Days 32–42)

Now that our Java foundations, Spring Boot core, REST APIs, PostgreSQL databases, and security perimeter are rock-solid, **it is finally time to build real AI!**

Starting tomorrow, you will start programming Generative AI directly in Java using the official **Spring AI** framework:
- **Day 32**: Introduction to Spring AI — Architecture, Model Abstractions, and Why Java is Dominating Enterprise AI.
- **Day 33**: `ChatClient` — The Fluent Conversational API for Prompts, System Directives, and Dynamic Context.
- **Day 34**: Prompt Engineering in Java — Dynamic Templates, Message Roles, and Variable Substitutions.
- **Day 35**: Structured Output Converters — Forcing LLMs to Return Valid Java Records, Beans, and Enums without Hallucinations.
- **Day 36**: Streaming Responses — The ChatGPT "typewriter" effect using Flux and SSE.
- **Day 37**: Embedding Models — Turning Text into High-Dimensional Vectors.
- **Day 38**: Vector Stores & Semantic Memory — Querying `pgvector` from Java.
- **Day 39**: Retrieval-Augmented Generation (RAG) — Grounding LLMs with Proprietary Documents.
- **Day 40**: Advanced RAG — Query Transformation and Re-Ranking.
- **Day 41**: Tool Calling — Allowing LLMs to autonomously execute your Java code.
- **Day 42**: Multimodal AI — Processing Images, Audio, and Vision.

👉 **Proceed to [Day 32: Introduction to Spring AI — The Big Picture](../../Phase_06_Spring_AI/Day_32_Introduction_to_Spring_AI/Day_32_Introduction_to_Spring_AI.md) to begin Phase 6!**

