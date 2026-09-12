# Day 31: Rate Limiting, CORS & API Security
## Defending Against Denial-of-Wallet (DoW), Token-Bucket Rate Limiting (Bucket4j), CORS Hardening & OWASP Headers

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [◀ Day 30: OAuth2 & Social Login](../Day_30_OAuth2_Social_Login/Day_30_OAuth2_Social_Login.md) | [All 60 Days Overview](../../README.md) | [Day 32: Introduction to Spring AI ▶](../../Phase_06_Spring_AI/Day_32_Introduction_to_Spring_AI/Day_32_Introduction_to_Spring_AI.md) |

---

## 1. Topic Overview

API security for Generative AI applications combines Token-Bucket rate limiting, Cross-Origin Resource Sharing (CORS) origin restrictions, payload size caps, and OWASP defense-in-depth headers to safeguard services from Denial-of-Wallet (DoW) attacks. In enterprise systems, where commercial LLM inference calls cost substantial cloud credits per request, robust throttling and security policies prevent financial exhaustion, stop cross-origin prompt theft, and insulate streaming connections from denial-of-service starvation.

---

## 2. Basic Foundations (True Zero)

### What is Denial-of-Wallet (DoW) and Rate Limiting?
In a standard REST API, an unthrottled request uses fractions of a microsecond of CPU time and costs virtually nothing. In an enterprise Generative AI API, an unthrottled endpoint invoking models like `gpt-4o`, `claude-3-5-sonnet`, or `gemini-1.5-pro` costs real money on your company credit card ($0.01 to $0.20 per prompt). 

If an automated bot, an attacker, or a buggy frontend script executes an infinite loop firing 1,000 requests per minute, your company could face an **$86,000 bill in 24 hours**. In cybersecurity, this attack is termed **Denial of Wallet (DoW)**.

**Rate Limiting** enforces traffic boundaries: each user, IP, or tenant is allocated a strictly metered quota (e.g., 10 prompts per minute). When the quota is exceeded, the server stops the request in 0.2 milliseconds with **HTTP 429 Too Many Requests**, protecting downstream LLM budgets.

```
+-----------------------------------------------------------------------------------+
|               THE VIP NIGHTCLUB & METERED BAR TAB ANALOGY                         |
|                                                                                   |
|  1. THE DOORMAN (CORS & IP Pre-Filter)                                            |
|  - Checks where you came from. Only patrons arriving from reputable partner       |
|    hotels (Whitelisted Origins) are permitted past the velvet rope.               |
|  - If 50 people suddenly rush the door simultaneously from a suspicious alley,    |
|    the doorman halts them: "Wait 60 seconds!" (HTTP 429 Too Many Requests).       |
|                                                                                   |
|  2. THE DRESS CODE & VIP PASS (Authentication)                                    |
|  - Verifies your wristband before you are seated at an AI table.                  |
|                                                                                   |
|  3. THE METERED BAR TAB (Token-Bucket Rate Limiting)                              |
|  - You cannot drink unlimited $500 vintage champagne. Your wristband holds 3     |
|    drink tokens. Each pour consumes 1 token.                                      |
|  - Tokens drip back into your account at a rate of 1 token every 20 seconds.      |
|  - If your tokens reach 0, the bartender hands you a card: "Next pour at 10:15pm" |
|    (Retry-After: 20s). The club never goes bankrupt from a single customer!       |
+-----------------------------------------------------------------------------------+
```

### Minimal Beginner-Friendly Working Code Example

Below is a self-contained Java 21 implementation of the **Token Bucket Algorithm**, demonstrating refill rates, burst capacity, and throttling:

```java
public class BasicTokenBucketExample {

    static class TokenBucket {
        private final long capacity;
        private final double refillTokensPerSecond;
        private double availableTokens;
        private long lastRefillNanos;

        public TokenBucket(long capacity, double refillTokensPerSecond) {
            this.capacity = capacity;
            this.refillTokensPerSecond = refillTokensPerSecond;
            this.availableTokens = capacity;
            this.lastRefillNanos = System.nanoTime();
        }

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
            double elapsedSeconds = (now - lastRefillNanos) / 1_000_000_000.0;
            if (elapsedSeconds > 0) {
                availableTokens = Math.min(capacity, availableTokens + (elapsedSeconds * refillTokensPerSecond));
                lastRefillNanos = now;
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // Bucket with max burst capacity of 3 tokens, refilling 1 token every second
        TokenBucket bucket = new TokenBucket(3, 1.0);

        System.out.println("--- Rapid Request Burst (Capacity: 3) ---");
        for (int i = 1; i <= 5; i++) {
            boolean allowed = bucket.tryConsume(1);
            System.out.printf("Request #%d: %s%n", i, allowed ? "200 OK (Allowed)" : "429 Too Many Requests (THROTTLED!)");
        }

        System.out.println("\nWaiting 1.2 seconds for token refill...");
        Thread.sleep(1200);

        boolean retryAllowed = bucket.tryConsume(1);
        System.out.printf("Request after wait: %s%n", retryAllowed ? "200 OK (Refill Success)" : "429 Too Many Requests");
    }
}
```

#### Line-by-Line Walkthrough:
- **Lines 5–9**: `TokenBucket` tracks `capacity` (maximum burst allowance), `refillTokensPerSecond` (sustained generation rate), and `lastRefillNanos` for high-precision time calculations.
- **Lines 17–24**: `tryConsume` atomically checks if sufficient tokens exist. If yes, it decrements the count and returns `true`. If not, it rejects the call without blocking.
- **Lines 26–33**: `refill` calculates elapsed nanoseconds since the last check, adds newly generated tokens up to the maximum capacity, and resets the timestamp.
- **Lines 39–44**: Simulates 5 back-to-back requests. The first 3 succeed (using up burst capacity), while requests 4 and 5 are throttled with `429 Too Many Requests`.
- **Lines 46–50**: After waiting 1.2 seconds, a token has dripped into the bucket, allowing the next request to succeed cleanly.

---

## 3. Core Concept Walkthrough (Basic → Intermediate)

### Rate Limiting Algorithms Compared

```
+-------------------+-----------------------------------+---------------------------------------+
| Algorithm         | Operating Mechanism               | Best Application                      |
+-------------------+-----------------------------------+---------------------------------------+
| 1. Token Bucket   | Fixed-rate token drip into bucket | Bursty human traffic & AI prompts     |
|    (Bucket4j)     | with maximum capacity limit.      | (Industry standard for Gen AI).       |
+-------------------+-----------------------------------+---------------------------------------+
| 2. Leaky Bucket   | Requests queue up and process at  | Smoothing constant-rate message       |
|                   | a strictly fixed output rate.     | queues and background database writes.|
+-------------------+-----------------------------------+---------------------------------------+
| 3. Fixed Window   | Counter resets at top of window   | Simple APIs. Vulnerable to boundary   |
|                   | (e.g. 100 requests per minute).   | burst attacks (2x limit at edges).    |
+-------------------+-----------------------------------+---------------------------------------+
| 4. Sliding Window | Tracks timestamp of every call in | Ultra-precise limits on low-volume    |
|    Log            | Redis/memory. High memory usage.  | financial transactions.               |
+-------------------+-----------------------------------+---------------------------------------+
```

---

### Standard RFC 6585 Headers

When throttling a client, an enterprise REST API must communicate quota boundaries using standardized HTTP response headers:

```http
HTTP/1.1 429 Too Many Requests
Content-Type: application/json
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1788956400
Retry-After: 15

{
  "error": "Too Many Requests",
  "message": "Rate limit exceeded. Please retry after 15 seconds.",
  "retryAfterSeconds": 15
}
```

- **`X-RateLimit-Limit`**: Maximum tokens allowed in the measurement window.
- **`X-RateLimit-Remaining`**: Number of available tokens left in the bucket.
- **`X-RateLimit-Reset`**: Epoch timestamp when the bucket will be completely replenished.
- **`Retry-After`**: Seconds the client must wait before retrying.

---

### CORS (Cross-Origin Resource Sharing) Hardening

Because modern AI web clients (React, Next.js) run on different hostnames or ports (e.g., `https://chat.myenterprise.com`) than the backend API (`https://api.myenterprise.com:8080`), web browsers enforce the **Same-Origin Policy (SOP)**.

```
 Browser (chat.myenterprise.com)                      Spring Boot (api.myenterprise.com)
            │                                                      │
            │ 1. Preflight Request:                                │
            │    OPTIONS /api/v1/ai/stream                         │
            │    Origin: https://chat.myenterprise.com             │
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

#### Production Spring CORS Configuration:
```java
package com.example.genai.config;

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
        config.setAllowedOrigins(List.of("https://chat.myenterprise.com", "http://localhost:3000"));
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

---

### OWASP Defense-in-Depth Security Headers

Configure response headers to shield frontend clients from clickjacking and injection attacks:

```java
http.headers(headers -> headers
    // 1. Prevent MIME-type sniffing
    .contentTypeOptions(Customizer.withDefaults()) // X-Content-Type-Options: nosniff
    
    // 2. Prevent Clickjacking (disallow embedding in iframes)
    .frameOptions(frame -> frame.deny())           // X-Frame-Options: DENY
    
    // 3. Enforce Strict HTTPS for 1 year
    .httpStrictTransportSecurity(hsts -> hsts
        .includeSubDomains(true)
        .maxAgeInSeconds(31536000)
    )
    
    // 4. Content Security Policy (CSP)
    .contentSecurityPolicy(csp -> csp
        .policyDirectives("default-src 'self'; frame-ancestors 'none'")
    )
);
```

---

## 4. Prerequisite & Supporting Concepts

### Prerequisite / Supporting Concept: Preflight Request Handling
A common bug in Spring Security occurs when `OPTIONS` preflight requests are rejected with `401 Unauthorized` because the browser does not send Authorization headers on preflights.

To prevent this:
1. Register `CorsConfigurationSource` as shown above.
2. In your `SecurityFilterChain`, call `.cors(cors -> cors.configurationSource(corsConfigurationSource()))`.
3. Spring Security's `CorsFilter` executes at the very beginning of the filter chain, responding to `OPTIONS` before authentication filters execute.

### Prerequisite / Supporting Concept: Slowloris & Streaming SSE Protection
In streaming AI architectures using Server-Sent Events (`SseEmitter`), a client can open a connection and read at 1 byte per minute, keeping worker threads active indefinitely.

Spring MVC supports explicit timeouts:
```java
SseEmitter emitter = new SseEmitter(30_000L); // 30-second connection timeout
emitter.onTimeout(() -> {
    emitter.complete();
    // Log timeout cleanup
});
```

### Prerequisite / Supporting Concept: Why CSRF is Disabled for Stateless APIs
In stateless REST backends that authenticate via `Authorization: Bearer <JWT>`, browsers do not attach custom headers automatically. Because CSRF relies strictly on ambient browser credential transmission (cookies), disabling CSRF (`csrf.disable()`) is completely safe for stateless APIs.

---

## 5. Advanced Depth (Intermediate → Advanced)

### Token-Weighted Rate Limiting

Standard rate limiting charges 1 token per HTTP request. In Generative AI, a prompt requesting a 4,000-token financial audit consumes 40x more compute than a 100-token greeting.

**Token-Weighted Metering** calculates estimated token load before consuming quota:

```java
@Service
public class AiTokenMeterService {
    private final RateLimiterRegistry registry;

    public AiTokenMeterService(RateLimiterRegistry registry) {
        this.registry = registry;
    }

    public boolean tryConsumeAiTokens(String userId, String prompt) {
        // Approximate token count: ~4 characters per token
        long estimatedTokens = Math.max(1, (long) Math.ceil(prompt.length() / 4.0));
        TokenBucket bucket = registry.resolveBucket("user:" + userId);
        return bucket.tryConsume(estimatedTokens);
    }
}
```

---

### Hands-On Simulation Code Walkthrough

The companion code repository demonstrates this architecture:
- `TokenBucket.java`: High-precision nanosecond token bucket rate limiter.
- `CorsPolicyValidator.java`: Validates origins and generates CORS response headers.
- `AiSecurityGateway.java`: Integrated gateway checking CORS, OWASP headers, payload size caps (> 64KB rejection), and rate limiting.
- `ApiSecurityDemo.java`: 7-scenario verification test suite validating CORS allowances, rogue origin blocks, preflight handshakes, burst capacity exhaustion, token refills, and payload size guards.

```powershell
# Compile Day 31 code
javac -d out Phase_05_Spring_Security/Day_31_Rate_Limiting_CORS_API_Security/code/*.java

# Run ApiSecurityDemo
java -cp out com.genai.security.apisec.ApiSecurityDemo
```

#### Verified Execution Output:
```
================================================================================
  DAY 31: API SECURITY, RATE LIMITING & CORS DEFENSE DEMONSTRATION             
================================================================================

[TEST 1] Testing Allowed CORS Request from Whitelisted Origin...
  Status Code: 200
  CORS Origin: https://chat.myenterprise.com
  CSP Header:  default-src 'self'
  Rate Remaining: 2
  [OK] TEST 1 PASSED!

[TEST 2] Testing Blocked CORS Request from Rogue Origin...
  Status Code: 403 (CORS Error: CORS origin 'https://evil-hacker.com' is not whitelisted)
  [OK] TEST 2 PASSED: Rogue CORS request blocked!

[TEST 3] Testing CORS Preflight OPTIONS Request...
  Status Code: 204
  Allowed Methods: POST, GET, OPTIONS
  [OK] TEST 3 PASSED: Preflight handled cleanly with 204 No Content.

[TEST 4 & 5] Simulating Rapid AI Inference Requests (Burst Capacity = 3)...
  Request #1 -> Status: 200 | Remaining: 2 | Retry-After: 0s
  Request #2 -> Status: 200 | Remaining: 1 | Retry-After: 0s
  Request #3 -> Status: 200 | Remaining: 0 | Retry-After: 0s
  Request #4 -> Status: 429 | Remaining: 0 | Retry-After: 1
  Request #5 -> Status: 429 | Remaining: 0 | Retry-After: 1

[TEST 6] Waiting 600ms for Token Bucket to Refill...
  Retry after wait -> Status: 200 | Remaining: 0
  [OK] TEST 6 PASSED: Request succeeded after token refill.

[TEST 7] Testing Oversized Prompt Payload DoS Guard (>64KB)...
  Status Code: 413 (Payload Too Large: Maximum prompt size is 64KB)
  [OK] TEST 7 PASSED: Giant prompt rejected before reaching JSON parser.

================================================================================
  ALL API SECURITY, RATE LIMITING & CORS TESTS PASSED!                         
================================================================================
```

---

## 6. Quick Recap

| Concept | Description | Enterprise Rule / Best Practice |
| :--- | :--- | :--- |
| **Denial of Wallet (DoW)** | Financial exhaustion via expensive LLM calls| Defend using Token-Bucket rate limiting at the perimeter. |
| **Token Bucket** | Bursty traffic handling with steady refill | Preferred rate limiting algorithm for AI chat APIs. |
| **HTTP 429** | Too Many Requests status code | Always return with RFC 6585 `Retry-After` header. |
| **CORS** | Browser Same-Origin Policy negotiation | Whitelist specific origins; never use wildcard `*` with credentials. |
| **Preflight (`OPTIONS`)** | Initial browser handshake | Must be handled by `CorsFilter` before authentication checks. |
| **OWASP Headers** | HSTS, CSP, X-Frame-Options, nosniff | Configured globally on `SecurityFilterChain` response headers. |
| **Payload Guards** | Enforcing `Content-Length` caps | Reject prompts > 64KB with `413 Payload Too Large`. |

---

## 7. Self-Check Questions & Practice Exercises

### Conceptual & Architectural Questions

#### Q1: What is a "Denial of Wallet" (DoW) attack in the context of Generative AI?
**Answer**: A Denial of Wallet attack specifically targets the pay-per-token or pay-per-inference billing model of commercial AI providers (OpenAI, Anthropic, Bedrock). Attackers flood unthrottled endpoints with high-volume, maximum-context requests to run up massive cloud bills, financially exhausting the targeted organization.

#### Q2: Why is the Token Bucket algorithm preferred over the Fixed Window algorithm for AI chat APIs?
**Answer**: Human interaction with AI chat applications is inherently bursty: an engineer may submit 3 prompt iterations in 15 seconds, followed by several minutes of reading. The Token Bucket algorithm allows burst consumption up to its maximum capacity while refilling at a steady, sustainable rate, avoiding the unfair boundary burst vulnerabilities of Fixed Window counters.

#### Q3: What HTTP status code should your backend return when a user exhausts their rate limit?
**Answer**: **HTTP 429 Too Many Requests** (defined in RFC 6585), accompanied by a `Retry-After` header indicating how many seconds to wait before retrying.

#### Q4: Which HTTP header informs the client how many seconds they must wait before making another request after hitting a rate limit?
**Answer**: The **`Retry-After`** header (e.g., `Retry-After: 15`).

#### Q5: Why is CSRF protection usually disabled (`http.csrf(csrf -> csrf.disable())`) in stateless REST backends using Bearer JWTs?
**Answer**: CSRF attacks exploit ambient browser credential transmission, where browsers automatically attach stored session cookies to cross-origin requests. Because modern REST backends require explicit `Authorization: Bearer <token>` headers—which browsers never automatically attach—cross-origin requests cannot forge identity, making CSRF attacks impossible.

---

### Hands-On Practice Exercises

#### Exercise 1: Token-Weighted Rate Limiting
**Task**: Write a service method `tryConsumeAiTokens(String userId, String prompt)` that estimates token load (~4 characters per token) and consumes that weight from the user's bucket.

```java
// Solution:
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

#### Exercise 2: Spring Boot `CorsConfigurationSource` Bean
**Task**: Configure a `CorsConfigurationSource` bean permitting `https://chat.enterprise.com` and `http://localhost:3000` to execute `GET`, `POST`, `DELETE`, exposing rate limit headers to the browser.

```java
// Solution:
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
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

#### Exercise 3: Server-Sent Events (SSE) Streaming Connection Timeout Guard
**Task**: Write a controller endpoint `/api/v1/ai/stream` with a 30-second timeout that closes the emitter and cleans up resources if the client stalls.

```java
// Solution:
@RestController
@RequestMapping("/api/v1/ai")
public class StreamingAiController {

    private final AiStreamingService streamingService;

    public StreamingAiController(AiStreamingService streamingService) {
        this.streamingService = streamingService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChatResponse(@RequestParam String prompt) {
        SseEmitter emitter = new SseEmitter(30_000L);

        emitter.onTimeout(() -> {
            emitter.complete();
            System.err.println("SSE Stream timed out to prevent thread starvation!");
        });

        emitter.onError(ex -> {
            emitter.completeWithError(ex);
            System.err.println("SSE Stream error: " + ex.getMessage());
        });

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

---

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [◀ Day 30: OAuth2 & Social Login](../Day_30_OAuth2_Social_Login/Day_30_OAuth2_Social_Login.md) | [All 60 Days Overview](../../README.md) | [Day 32: Introduction to Spring AI ▶](../../Phase_06_Spring_AI/Day_32_Introduction_to_Spring_AI/Day_32_Introduction_to_Spring_AI.md) |
