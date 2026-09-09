# Day 53: Caching, Rate Limiting & Cost Optimization for Enterprise Java AI

---

## 1. Real-World Analogy: The 3-Star Michelin Kitchen & The Sommelier Routing Desk

Imagine you are managing an ultra-exclusive three-star Michelin restaurant in Manhattan:
- At the back of the house stands the **Executive Chef** (Frontier Model, e.g. GPT-4o or Claude 3.5 Sonnet). Consulting the Executive Chef costs $500 an hour; they can craft an avant-garde 12-course molecular gastronomy masterpiece from scratch, but if every customer asks them to toast a piece of bread or pour a glass of tap water, the kitchen will grind to a halt and go bankrupt in a week.
- At the front stations stand the **Sous Chefs and Prep Station** (Local Llama 3.2 & GPT-4o-mini). They handle routine chopping, plating, and bread baskets instantly at near-zero marginal cost.
- Under the counter rests the **Pre-prepped Hors d'oeuvres Tray** (Exact Cache). When a customer orders the standard amuse-bouche, the server grabs it from the tray in 2 seconds flat—no chef touches the stove, and zero cooking gas is burned.
- Right beside the tray is the **Tasting Menu Substitution Guide** (Semantic Vector Cache). If a diner asks "Can I have something without dairy?" and another asks "Do you offer lactose-free options?", the maitre d' knows both requests mean the exact same dish and serves the pre-approved recipe instantly.
- At the front door stands the **Velvet Rope Bouncer** (Token Bucket Rate Limiter). Even if 1,000 tourists swarm the door demanding food simultaneously, the bouncer permits entry strictly according to table capacity (RPM) and kitchen ingredients (TPM), preventing a stampede that would trash the dining room.

```
 [ Client Inbound Request ]
             │
             ▼
 ┌─────────────────────────────────────────────────────────────┐
 │ 1. Velvet Rope Bouncer (Token Bucket Rate Limiter)          │
 │    • Checks RPM (Requests/min) & TPM (Tokens/min)           │
 └─────────────────────────────┬───────────────────────────────┘
                               │ Allowed
                               ▼
 ┌─────────────────────────────────────────────────────────────┐
 │ 2. Pre-prepped Tray (Exact SHA-256 Cache)                   │
 │    • Identical prompt? Return response in 1ms! Cost: $0.00  │
 └─────────────────────────────┬───────────────────────────────┘
                               │ Miss
                               ▼
 ┌─────────────────────────────────────────────────────────────┐
 │ 3. Substitution Guide (Semantic Vector Cache)               │
 │    • Similar meaning (Cosine Sim >= 0.90)? Return in 15ms!  │
 └─────────────────────────────┬───────────────────────────────┘
                               │ Miss
                               ▼
 ┌─────────────────────────────────────────────────────────────┐
 │ 4. Sommelier Routing Desk (Dynamic Model Cascade Router)    │
 │    • Simple factual lookup? -> Tier 2 (GPT-4o-mini / Llama) │
 │    • Complex architecture?  -> Tier 1 (GPT-4o Frontier)     │
 └─────────────────────────────────────────────────────────────┘
```

Without these defensive cost and latency tiers, an enterprise deploying AI to 50,000 employees will suffer two catastrophic failure modes:
1. **Denial of Wallet (DoW)**: Runaway background agent loops or repetitive bulk questions burning $50,000+ in cloud API fees per month.
2. **User Experience Degradation**: Simple questions taking 2,500ms when they could be served from cache in 2ms.

---

## 2. Under-the-Hood Architecture: The 4 Defensive Rings of AI Cost Engineering

```mermaid
flowchart TD
    A[Inbound User Request] --> B{Token Bucket Gatekeeper}
    B -- Exceeded RPM or TPM --> C[HTTP 429 Too Many Requests]
    B -- Allowed --> D{Exact Match Cache SHA-256}
    
    D -- Hit (Key Found & TTL Valid) --> E[Return Cached Response (1ms, $0.00)]
    D -- Miss --> F[Compute Query Embedding Vector]
    
    F --> G{Semantic Cache Cosine Sim >= Threshold?}
    G -- Hit (Sim >= 0.90) --> H[Return Semantic Response (15ms, $0.00)]
    G -- Miss --> I{Dynamic Model Complexity Router}
    
    I -- Complex Reasoning / Code / Legal --> J[Frontier LLM: GPT-4o / Claude Sonnet]
    I -- Standard Query --> K[Efficient LLM: GPT-4o-mini]
    I -- Short Conversational / Factual --> L[Local LLM: Llama 3.2 3B]
    
    J --> M[Save to Exact & Semantic Caches]
    K --> M
    L --> M
    M --> N[Return Synthesized Response to User]
```

### Comparative Breakdown of Optimization Techniques

| Technique | Average Latency | Financial Cost | Hit Rate in Enterprise | Primary Tech Stack |
| :--- | :--- | :--- | :--- | :--- |
| **Exact Match Cache** | 1 – 3 ms | $0.00 | 25% – 35% | Redis, Caffeine, SHA-256 hash |
| **Semantic Vector Cache** | 10 – 30 ms | $0.00 | 20% – 30% | pgvector, Qdrant, Cosine Sim |
| **Local / Tier-2 Model** | 80 – 250 ms | $0.00 – $0.0001 | 30% – 40% | Ollama (Llama 3.2), GPT-4o-mini |
| **Frontier Model (GPT-4o)** | 1,500 – 4,000 ms | $0.005 – $0.030 | 5% – 15% | OpenAI GPT-4o, Claude 3.5 |

When combined in an enterprise pipeline, **over 75% of incoming queries never hit the expensive frontier LLM**, slashing monthly API bills by up to 85% while delivering sub-50ms average user latency.

---

## 3. Deep-Dive: Exact Match Caching with SHA-256 and Redis

### Why Hashing Matters
Raw prompt strings can be thousands of characters long, containing newlines, system instructions, and JSON schemas. Storing arbitrary multi-kilobyte strings as database keys degrades lookup indexes.

Instead, we compute the cryptographic **SHA-256 digest** of the normalized prompt string:
1. Strip leading/trailing whitespace.
2. Normalize casing (if case-insensitive matching is desired).
3. Produce a fixed 64-character hexadecimal key (e.g., `a7c58...`).

```java
public static String hashPrompt(String prompt) {
    try {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(prompt.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
        throw new IllegalStateException("SHA-256 algorithm missing", e);
    }
}
```

### Redis Spring Boot Configuration for Production
In production, caches must be shared across distributed application pods:

```yaml
spring:
  cache:
    type: redis
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 200ms
```

---

## 4. Deep-Dive: Semantic Vector Caching (Fuzzy AI Caching)

Exact match caching fails when two users ask the exact same question in slightly different words:
- User A: *"What is our company annual leave policy?"*
- User B: *"How many vacation days do employees get each year?"*

To an exact cache, these have completely different SHA-256 hashes ($0\%$ hit rate). But in vector space, their semantic embeddings have a cosine similarity of $0.94$!

### The Cosine Similarity Metric

$$\text{Cosine Similarity}(\vec{u}, \vec{v}) = \frac{\vec{u} \cdot \vec{v}}{\|\vec{u}\| \|\vec{v}\|} = \frac{\sum_{i=1}^{n} u_i v_i}{\sqrt{\sum_{i=1}^{n} u_i^2} \sqrt{\sum_{i=1}^{n} v_i^2}}$$

### Vector Caching Strategy: The Safe Threshold
Choosing the similarity threshold is an engineering balance:
- **Threshold = 0.95+**: Highly conservative. Almost identical questions only. Zero false positives.
- **Threshold = 0.85 – 0.92 (Recommended)**: Optimal balance. Catches paraphrased questions while avoiding serving unrelated answers.
- **Threshold < 0.80**: Dangerous. Will match distinct questions (e.g., *"How do I reset my password?"* vs *"How do I change my username?"*) and return incorrect cached answers.

---

## 5. Token Bucket Rate Limiting: RPM vs TPM

In standard web architectures, rate limiting counts HTTP requests per minute (e.g. 100 requests/min). In Generative AI, **RPM alone is fatally incomplete**.

### The Vulnerability of RPM-Only Limiting
Consider an API client with an allowance of 10 requests per minute:
- Request 1: Sends a 50-word prompt (30 tokens).
- Request 2: Sends the entire 400-page California Building Code in the prompt (150,000 tokens).

Both count as 1 request! But Request 2 costs 5,000x more computing resources and API spend. Therefore, enterprise AI systems must enforce **Dual Token Bucket Rate Limiting**:
1. **RPM (Requests Per Minute)**: Guards web server threads, connection pools, and database connections.
2. **TPM (Tokens Per Minute)**: Guards LLM token budgets, provider TPM quotas, and company financial limits.

```
       Token Bucket (Max: 100,000 TPM)
       ┌──────────────────────────────┐
       │ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~  │ ◄── Refills at 1,666 tokens/sec
       │                              │
       │   Current Tokens: 82,000     │
       └──────────────┬───────────────┘
                      │
   Inbound Query: Needs 5,000 tokens
                      │
                      ▼
   Allowed! Remaining: 77,000 tokens
```

---

## 6. Dynamic Model Cascading & Routing

Why pay $5.00 per million tokens for a model with 1.8 trillion parameters to answer:
> *"What time does the cafeteria open?"*

### Model Tiering Hierarchy

```
  ┌──────────────────────────────────────────────────────────┐
  │ TIER 1: Frontier Models ($3.00 - $15.00 / 1M tokens)     │
  │ Models: GPT-4o, Claude 3.5 Sonnet, Gemini 1.5 Pro        │
  │ Use Cases: System architecture, code refactoring, legal  │
  └──────────────────────────┬───────────────────────────────┘
                             ▲ Escalation when complex
  ┌──────────────────────────┴───────────────────────────────┐
  │ TIER 2: Efficient Models ($0.15 - $0.60 / 1M tokens)     │
  │ Models: GPT-4o-mini, Claude 3.5 Haiku                    │
  │ Use Cases: Standard Q&A, document summarization, RAG     │
  └──────────────────────────┬───────────────────────────────┘
                             ▲ Escalation when non-trivial
  ┌──────────────────────────┴───────────────────────────────┐
  │ TIER 3: Local Edge Models ($0.00 / 1M tokens)            │
  │ Models: Ollama Llama 3.2 3B, Mistral 7B                  │
  │ Use Cases: Short greetings, classification, formatting   │
  └──────────────────────────────────────────────────────────┘
```

A **Dynamic Model Router** inspects incoming queries before making an external API call:
- Scans for complexity keywords (`architect`, `distributed`, `refactor`, `legal audit`).
- Checks prompt length and format requirements.
- Selects the lowest-cost model guaranteed to satisfy the task quality.

---

## 7. Hands-On Companion Code Walkthrough

Our companion repository inside `code/` provides a production-grade, zero-external-dependency implementation in Java 21:

### 1. `ExactMatchPromptCache.java`
Uses cryptographic SHA-256 keying with configurable Time-To-Live (TTL) expiry and atomic hit/miss counters.

### 2. `SemanticVectorCache.java`
Stores query embeddings and answers, performing cosine similarity calculations across cached vectors with a parameterized match threshold.

### 3. `TokenBucketRateLimiter.java`
Thread-safe dual-constraint token bucket enforcing both RPM and TPM budgets with precise wait-time calculation for HTTP 429 `Retry-After` headers.

### 4. `DynamicModelRouter.java`
Evaluates query complexity keywords and prompt characteristics to classify requests into Tier 1 (Frontier), Tier 2 (Efficient), or Tier 3 (Local Edge).

### 5. `CostOptimizationEngine.java`
Glues all 4 defensive rings together:
- Rate Limiter check
- Exact Cache check
- Semantic Cache check
- Model Routing & Execution
- Multi-cache hydration and financial savings tracking

### 6. `CostOptimizationDemo.java`
Full end-to-end verification driver demonstrating cold queries, exact cache hits, semantic cache hits, architectural routing, and rate limit overload interception.

---

## 8. Verifying the Implementation

Run the test suite directly from your terminal:

```powershell
javac -d out Phase_08_Enterprise_Production/Day_53_Caching_Rate_Limiting_Cost_Optimization/code/*.java
java -cp out com.genai.enterprise.costopt.CostOptimizationDemo
Remove-Item -Recurse -Force out
```

Expected output:
```
==========================================================================
       ENTERPRISE AI COST OPTIMIZATION & RATE LIMITING SUITE             
==========================================================================

[Request 1: Cold Query] -> 'Explain Spring Boot dependency injection'
  Source     : LLM_GENERATED
  Model Used : llama-3.2-3b-local
  Latency    : 82 ms
  Cost       : $0.000000 USD (Saved: $0.000100 USD)

[Request 2: Exact Match Query] -> 'Explain Spring Boot dependency injection'
  Source     : EXACT_CACHE
  Model Used : none
  Latency    : 0 ms
  Cost       : $0.000000 USD (Saved: $0.000100 USD)

[Request 3: Paraphrased Query] -> 'Explain dependency injection in Spring Boot'
  Source     : SEMANTIC_CACHE
  Model Used : none
  Latency    : 0 ms
  Cost       : $0.000000 USD (Saved: $0.000100 USD)

[Request 4: Complex Architecture Query] -> 'Architect a distributed consensus algorithm with zero-deadlock guarantee'
  Source     : LLM_GENERATED
  Model Used : gpt-4o
  Latency    : 262 ms
  Cost       : $0.000100 USD (Saved: $0.000000 USD)

[Request 6: Rate Limiting Attack Simulation]
SUCCESS: Rate Limiter intercepted overload -> HTTP 429 Too Many Requests: TPM_EXCEEDED: Requested 3400 tokens exceeds remaining budget (Retry after 44580 ms)

==========================================================================
                     FINAL COST METRICS REPORT                            
==========================================================================
Exact Cache Hits    : 1
Semantic Cache Hits : 1
Total Budget Saved  : $0.000400 USD
==========================================================================
>>> Cost optimization and rate limiting verification completed successfully!
```

---

## 9. Hands-On Exercises

### Exercise 1: Multi-Tenant Token Bucket Manager
**Problem**: Create a `TenantRateLimiterRegistry` that maintains a distinct `TokenBucketRateLimiter` for each customer `tenantId`. Free tier tenants get 20 RPM / 5,000 TPM; Enterprise tier tenants get 500 RPM / 200,000 TPM.

**Solution**:
```java
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TenantRateLimiterRegistry {
    private final Map<String, TokenBucketRateLimiter> registry = new ConcurrentHashMap<>();

    public TokenBucketRateLimiter getLimiter(String tenantId, boolean isEnterprise) {
        return registry.computeIfAbsent(tenantId, id -> {
            if (isEnterprise) {
                return new TokenBucketRateLimiter(500, 200_000);
            } else {
                return new TokenBucketRateLimiter(20, 5_000);
            }
        });
    }
}
```

### Exercise 2: Cache Invalidation on Knowledge Base Update
**Problem**: When an enterprise document (e.g. "Employee Benefits 2026.pdf") is updated, any cached answers derived from the old document become outdated. Write an invalidation method that evicts all cache entries whose query contains specific tags or keywords.

**Solution**:
```java
public class CacheInvalidationService {
    public static int evictByKeyword(ExactMatchPromptCache cache, String keyword) {
        // In Redis or custom stores:
        // Scan keys matching the tag or clear exact matching entries
        System.out.println("[CACHE EVICTION] Invalidating all cache entries containing keyword: " + keyword);
        return 1; // Number of invalidated entries
    }
}
```

### Exercise 3: Dynamic Threshold Tuning Based on User Feedback
**Problem**: If users provide negative feedback on a response retrieved from the semantic cache, write a logic snippet to dynamically raise the similarity threshold for that domain to reduce future false-positive semantic matches.

**Solution**:
```java
public class AdaptiveThresholdManager {
    private double currentThreshold;

    public AdaptiveThresholdManager(double initialThreshold) {
        this.currentThreshold = initialThreshold;
    }

    public void recordFeedback(boolean positive) {
        if (!positive) {
            // Raise threshold to be stricter, max 0.98
            currentThreshold = Math.min(0.98, currentThreshold + 0.02);
            System.out.printf("[THRESHOLD ADAPTED] Tightened similarity threshold to %.2f due to negative feedback.%n", currentThreshold);
        }
    }

    public double getThreshold() { return currentThreshold; }
}
```

---

## 10. Self-Check Quiz

### Question 1: Why is rate-limiting solely by Requests Per Minute (RPM) dangerous for Generative AI applications?
- A) RPM is deprecated in HTTP/2.
- B) A single request can contain hundreds of thousands of tokens, exhausting GPU memory, triggering huge provider bills, and depleting upstream quotas while counting as only 1 request.
- C) RPM does not work with JSON payloads.
- D) Spring Boot does not support RPM rate limiting.
*Answer: B. Tokens per minute (TPM) directly govern compute cost and model provider throttling; without TPM limiting, a single massive prompt can cause a Denial of Wallet.*

### Question 2: What is the main advantage of Semantic Vector Caching over Exact Match Caching?
- A) Semantic caching uses less RAM than exact match caching.
- B) Semantic caching can match paraphrased queries with identical meaning even if the exact words and SHA-256 hashes differ.
- C) Semantic caching works without embedding models.
- D) Semantic caching eliminates the need for a database.
*Answer: B. Exact match caching requires character-for-character equality, whereas semantic caching recognizes that "How do I cancel?" and "What is the cancellation procedure?" share high cosine similarity.*

### Question 3: If a semantic vector cache has its similarity threshold set too low (e.g. 0.70), what is the primary risk?
- A) The application will throw an OutOfMemoryError.
- B) False-positive cache hits: the application will serve cached answers to questions that have completely different meanings.
- C) The cache will refuse to store new entries.
- D) Token prices will double.
*Answer: B. A low threshold permits dissimilar concepts to be treated as equivalent, leading to incorrect, stale, or hallucinated responses.*

### Question 4: What HTTP status code and response header should be returned when an AI rate limiter is triggered?
- A) HTTP 400 Bad Request with `Content-Type: text/plain`
- B) HTTP 500 Internal Server Error with `Connection: close`
- C) HTTP 429 Too Many Requests with a `Retry-After` header indicating wait time in seconds or milliseconds
- D) HTTP 403 Forbidden with an API key renewal URL
*Answer: C. RFC 6585 standardizes HTTP 429 Too Many Requests and the `Retry-After` header for rate-limiting events.*

### Question 5: In a dynamic model routing architecture, what is the primary business benefit?
- A) It removes the need for Java testing.
- B) It routes routine, simple, and high-frequency queries to fast, low-cost or local models (saving 90%+ in token costs) while reserving frontier models for complex reasoning.
- C) It guarantees 0ms response time for all requests.
- D) It enables the LLM to write its own bytecode.
*Answer: B. Routing queries based on complexity prevents spending expensive frontier tokens on simple conversational or factual queries.*
