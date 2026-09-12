# Day 33: ChatClient — The Fluent Conversational API
## Fluent Builder, Dynamic Prompt Templates, Enterprise Advisors & PII Sanitization

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [◀ Day 32: Introduction to Spring AI](../Day_32_Introduction_to_Spring_AI/Day_32_Introduction_to_Spring_AI.md) | [All 60 Days Overview](../../README.md) | [Day 34: Prompt Engineering in Java ▶](../Day_34_Prompt_Engineering_in_Java/Day_34_Prompt_Engineering_in_Java.md) |

---

## 1. Topic Overview

Spring AI's `ChatClient` provides an ergonomic, fluent builder API inspired by `RestClient` to compose conversational prompts, bind template parameters, configure default system directives, and intercept calls via modular Advisors. In enterprise Generative AI systems, `ChatClient` abstracts low-level model drivers into readable, type-safe workflows while supporting cross-cutting interceptors for PII sanitization, conversational memory, and token telemetry.

---

## 2. Basic Foundations (True Zero)

### What is `ChatClient`?
Calling the low-level `ChatModel.call(Prompt)` SPI requires manual instantiation of `Prompt`, packaging of `Message` lists, and unpacking of nested `ChatResponse` structures. 

`ChatClient` sits above `ChatModel` as a high-level, fluent developer interface. It allows you to write conversational AI interactions that read like plain English sentences, supporting chained configurations for system personas, user parameters, advisors, and automatic serialization into Java records:

```java
String response = chatClient.prompt()
    .system("You are an expert Java architect.")
    .user(u -> u.text("Explain {topic}").param("topic", "Virtual Threads"))
    .call()
    .content();
```

```
+-----------------------------------------------------------------------------------+
|               THE EXECUTIVE ASSISTANT BRIEFING DOSSIER ANALOGY                    |
|                                                                                   |
|  WITHOUT AN ASSISTANT (Raw ChatModel SPI):                                        |
|  - The CEO must personally photocopy documents, look up phone numbers, type      |
|    transcripts, and format raw notes.                                             |
|  - High cognitive overhead, repetitive boilerplate, error-prone.                  |
|                                                                                   |
|  WITH A DEDICATED EXECUTIVE ASSISTANT (Spring AI ChatClient):                     |
|  - Standing Orders (.defaultSystem): "Always address clients respectfully and     |
|    summarize reports to 1 page."                                                  |
|  - The Briefing Dossier (.user(u -> u.text("Summarize {company}").param(...))):   |
|    The assistant takes standard templates and automatically binds parameters.     |
|  - Security & Compliance Advisor (Custom Advisor Interceptor):                    |
|    Before handing any dossier to the CEO, compliance officers redact credit card  |
|    numbers and trade secrets (PII Redaction).                                     |
|  - Executive Output (.call().entity(Report.class)):                               |
|    The assistant formats output into an organized Java Record DTO!                |
+-----------------------------------------------------------------------------------+
```

### Minimal Beginner-Friendly Working Code Example

Below is a self-contained Java 21 simulation demonstrating how `ChatClient` executes a fluent builder chain with intercepting advisors:

```java
import java.util.*;

public class BasicChatClientExample {

    // 1. Advisor Interceptor Interface
    interface SimpleAdvisor {
        String before(String prompt);
        String after(String response);
    }

    // 2. PII Sanitization Advisor
    static class PiiMaskingAdvisor implements SimpleAdvisor {
        public String before(String prompt) {
            return prompt.replaceAll("\\b\\d{4}-\\d{4}-\\d{4}-\\d{4}\\b", "[REDACTED_CARD]");
        }
        public String after(String response) { return response; }
    }

    // 3. Fluent ChatClient Simulation
    static class FluentChatClient {
        private String systemPrompt = "You are an AI assistant.";
        private final List<SimpleAdvisor> advisors = new ArrayList<>();

        public FluentChatClient system(String sys) { this.systemPrompt = sys; return this; }
        public FluentChatClient addAdvisor(SimpleAdvisor adv) { this.advisors.add(adv); return this; }

        public String ask(String userPrompt) {
            String processed = userPrompt;
            for (SimpleAdvisor adv : advisors) processed = adv.before(processed);

            System.out.println("  [LLM Inbound Network Payload] " + processed);
            String aiRawOutput = "[AI Answer to: " + processed + "]";

            for (SimpleAdvisor adv : advisors) aiRawOutput = adv.after(aiRawOutput);
            return aiRawOutput;
        }
    }

    public static void main(String[] args) {
        FluentChatClient client = new FluentChatClient()
            .system("You are a financial banking assistant.")
            .addAdvisor(new PiiMaskingAdvisor());

        String userQuery = "Customer wants refund on card 1111-2222-3333-4444. Please assist.";
        System.out.println("Original Input: " + userQuery);

        String result = client.ask(userQuery);
        System.out.println("Final Output: " + result);
    }
}
```

#### Line-by-Line Walkthrough:
- **Lines 6–9**: Defines `SimpleAdvisor` exposing `before()` and `after()` lifecycle hooks around model calls.
- **Lines 12–17**: `PiiMaskingAdvisor` scans outgoing text with regular expressions, replacing credit card patterns with `[REDACTED_CARD]` before transmission.
- **Lines 20–34**: `FluentChatClient` chains methods (`.system()`, `.addAdvisor()`, `.ask()`), passing the prompt through the advisor pipeline before invoking the mock LLM engine.
- **Lines 37–46**: Demonstrates fluent initialization and execution. The original credit card number is sanitized before the model processes it.

---

## 3. Core Concept Walkthrough (Basic → Intermediate)

### From `RestClient` to `ChatClient`

If you know Spring Web's `RestClient`, you already understand `ChatClient`:

```
+-----------------------------------+-----------------------------------+---------------------------------------+
| Spring Web (`RestClient`)         | Spring AI (`ChatClient`)          | Plain-English Purpose                 |
+-----------------------------------+-----------------------------------+---------------------------------------+
| `restClient.get()`                | `chatClient.prompt()`             | Initiates fluent request builder.     |
| `.uri("/users/{id}", 42)`         | `.user(u -> u.text(...).param())` | Safe parameter substitution.          |
| `ClientHttpRequestInterceptor`    | `RequestResponseAdvisor`          | Pre/post request interception.        |
| `.retrieve().body(User.class)`    | `.call().entity(UserRecord.class)`| Deserializes straight to Java Record. |
| `.retrieve().body(String.class)`  | `.call().content()`               | Returns raw string response.          |
+-----------------------------------+-----------------------------------+---------------------------------------+
```

---

### The Evolution: `ChatModel` (SPI) vs. `ChatClient` (API)

```
+------------------------------------------------------------------------+
| APPLICATION CODE (Controllers, Services, Use Cases)                    |
|                                                                        |
|   chatClient.prompt()                                                  |
|       .system("You are a financial advisor")                          |
|       .user(u -> u.text("Analyze {stock}").param("stock", "GOOGL"))    |
|       .advisors(new LoggingAdvisor(), new PiiRedactionAdvisor())       |
|       .call()                                                          |
|       .content();                                                      |
+-----------------------------------+------------------------------------+
                                    |
                       (Fluent API Delegation)
                                    v
+------------------------------------------------------------------------+
| CHATCLIENT ENGINE                                                      |
| - Renders prompt templates                                             |
| - Executes Advisor 'before()' chain                                    |
| - Assembles immutable Prompt(List<Message>, ChatOptions)               |
+-----------------------------------+------------------------------------+
                                    |
                        (Low-level SPI Invocation)
                                    v
+------------------------------------------------------------------------+
| CHATMODEL IMPLEMENTATION (OllamaChatModel / OpenAiChatModel)           |
| - Dispatches HTTP REST / gRPC call to LLM Engine                       |
| - Unpacks raw JSON completion into ChatResponse                        |
+-----------------------------------+------------------------------------+
                                    |
                        (Returns ChatResponse)
                                    v
+------------------------------------------------------------------------+
| CHATCLIENT RESPONSE PROCESSOR                                          |
| - Executes Advisor 'after()' chain (e.g. latency logging)              |
| - Maps JSON into Java Record if .entity(...) requested                 |
| - Returns String, ChatResponse, or Java DTO                            |
+------------------------------------------------------------------------+
```

---

### Configuring the `ChatClient.Builder`

Spring AI auto-configures a `ChatClient.Builder` bean. You customize it in a `@Configuration` class to establish application-wide defaults:

```java
package com.example.genai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiClientConfiguration {

    @Bean
    public ChatClient customerSupportChatClient(ChatClient.Builder builder) {
        return builder
            // 1. Default System Persona
            .defaultSystem("""
                You are a senior customer support representative for CloudTech Inc.
                Maintain a professional, empathetic tone.
                Never reveal internal server infrastructure or credentials.
            """)
            // 2. Default Model Options
            .defaultOptions(ChatOptions.builder()
                .temperature(0.3)      // Low temperature for factual consistency
                .maxTokens(1024)        // Strict output ceiling
                .build())
            // 3. Default Advisors (Interceptors)
            .defaultAdvisors(new SimpleLoggerAdvisor())
            .build();
    }
}
```

---

### Dynamic Prompt Templates & Parameter Substitution

Never concatenate unvalidated user input directly into prompts using `+` or `String.format`:

```java
// ❌ INSECURE ANTI-PATTERN: Vulnerable to prompt injection
String badPrompt = "Translate " + text + " to " + targetLang;

// ✅ ENTERPRISE BEST PRACTICE: Parameterized template substitution
String goodResponse = chatClient.prompt()
    .user(u -> u.text("Translate the following {text} into {targetLang}. Retain formatting.")
        .param("text", rawUserInput)
        .param("targetLang", "German"))
    .call()
    .content();
```

---

### Handling Responses: `.content()`, `.chatResponse()`, and `.entity()`

```java
// 1. Simple String Content
String summary = chatClient.prompt()
    .user("Explain Quarkus in one sentence")
    .call()
    .content();

// 2. Full Metadata & Token Telemetry
ChatResponse response = chatClient.prompt()
    .user("Generate a 3-day travel itinerary for Tokyo")
    .call()
    .chatResponse();

long totalTokens = response.getMetadata().getUsage().getTotalTokens();
String finishReason = response.getResult().getMetadata().getFinishReason();

// 3. Strongly Typed Java Record Output
public record MovieRecommendation(String title, int releaseYear, String director, List<String> genres) {}

MovieRecommendation movie = chatClient.prompt()
    .user("Recommend a sci-fi movie from the 2010s")
    .call()
    .entity(MovieRecommendation.class);
```

---

## 4. Prerequisite & Supporting Concepts

### Prerequisite / Supporting Concept: The Advisor Interceptor Chain
The Advisor pattern implements Aspect-Oriented Programming (AOP) for LLM interactions. Advisors execute in priority order on `before(Prompt)` and in reverse order on `after(ChatResponse)`:

```
 User Request -> Advisor 1 before() -> Advisor 2 before() -> ChatModel SPI
                                                                   |
 Caller Result <- Advisor 1 after() <- Advisor 2 after()  <-------+
```

### Prerequisite / Supporting Concept: Built-in Spring AI Advisors
- **`SimpleLoggerAdvisor`**: Logs outgoing prompts and incoming responses with elapsed milliseconds and token usage.
- **`MessageChatMemoryAdvisor`**: Injects multi-turn conversation history from memory or Redis.
- **`QuestionAnswerAdvisor`**: Injects relevant document chunks from a vector store (RAG) into the prompt context.

### Prerequisite / Supporting Concept: PII (Personally Identifiable Information) Compliance
Under GDPR, HIPAA, and PCI-DSS, transmitting raw customer credit cards or medical numbers to third-party cloud LLMs constitutes an unlawful data breach. Advisors inspect and sanitize data at the JVM boundary before outbound HTTP packets are created.

---

## 5. Advanced Depth (Intermediate → Advanced)

### Building an Enterprise PII Redaction Advisor

```java
package com.example.genai.advisor;

import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import java.util.*;
import java.util.regex.Pattern;

public class PiiRedactionAdvisor implements CallAroundAdvisor {

    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\b(?:\\d{4}[ -]?){3}\\d{4}\\b");
    private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");

    @Override
    public String getName() { return "PiiRedactionAdvisor"; }

    @Override
    public int getOrder() { return 0; } // High priority

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        String userText = advisedRequest.userText();
        if (userText != null) {
            String sanitized = CREDIT_CARD_PATTERN.matcher(userText).replaceAll("[REDACTED_CARD]");
            sanitized = SSN_PATTERN.matcher(sanitized).replaceAll("[REDACTED_SSN]");
            
            advisedRequest = AdvisedRequest.from(advisedRequest)
                .withUserText(sanitized)
                .build();
        }
        return chain.nextAroundCall(advisedRequest);
    }
}
```

---

### Hands-On Simulation Code Walkthrough

The companion code repository demonstrates this architecture:
- `PromptTemplate.java`: Renders parameterized string templates without string concatenation.
- `Advisor.java`: Bidirectional interceptor hooks (`before()` and `after()`).
- `PiiRedactionAdvisor.java`: Sanitizes credit card numbers and SSNs before calling the model.
- `ChatClientDemo.java`: 3-scenario verification test suite validating default system directives, dynamic prompt parameters, and PII masking.

```powershell
# Compile Day 32 and Day 33 code
javac -d out Phase_06_Spring_AI/Day_32_Introduction_to_Spring_AI/code/*.java Phase_06_Spring_AI/Day_33_ChatClient_Fluent_Conversational_API/code/*.java

# Run ChatClientDemo
java -cp out com.genai.springai.chatclient.ChatClientDemo
```

#### Verified Execution Output:
```
================================================================================
  DAY 33: CHATCLIENT FLUENT API & ADVISOR INTERCEPTOR DEMONSTRATION             
================================================================================

[TEST 1] Creating ChatClient with Default System Prompt & Logging Advisor...
  [ADVISOR: LOGGER] >>> Sending Prompt with 2 messages to Model.
  [ADVISOR: LOGGER] <<< Received Response in 12ms. Tokens used: 106
  Response Content:
  [Ollama - Llama 3.2] Spring AI is the official Spring ecosystem framework for building AI applications in Java. It brings portable abstractions for ChatModels, VectorStores, and Document Readers, eliminating vendor lock-in!

[TEST 2] Dynamic Prompt Template Parameter Substitution...
  [ADVISOR: LOGGER] >>> Sending Prompt with 2 messages to Model.
  [ADVISOR: LOGGER] <<< Received Response in 3ms. Tokens used: 107
  Response Content:
  [Ollama - Llama 3.2 (Local Engine)] Processed query: 'Compare Java 21 against Node.js for enterprise RAG Vector Pipelines.' (Directive: You are a Senior Spring AI Consultant. Answer with enterprise best practices.)

[TEST 3] Testing PiiRedactionAdvisor (Sanitizing Sensitive User Data)...
  Original User Prompt:
  Customer John Doe requested a refund on card 4111-2222-3333-4444 with SSN 123-45-6789. Can you generate an apology note?
  [ADVISOR: PII_GUARD] 🛡️ Sensitive PII detected and redacted before reaching LLM provider!
  [ADVISOR: LOGGER] >>> Sending Prompt with 1 messages to Model.
  [ADVISOR: LOGGER] <<< Received Response in 0ms. Tokens used: 90
  Response:
  [Ollama - Llama 3.2 (Local Engine)] Processed query: 'Customer John Doe requested a refund on card [REDACTED_CARD] with SSN [REDACTED_SSN]. Can you generate an apology note?' 

================================================================================
  CHATCLIENT FLUENT API & ADVISORS VALIDATED SUCCESSFULLY!                      
================================================================================
```

---

## 6. Quick Recap

| Concept | Description | Enterprise Rule / Best Practice |
| :--- | :--- | :--- |
| **`ChatClient`** | Fluent conversational developer API | Preferred over calling `ChatModel` directly. |
| **Prompt Template** | Parameterized prompt with `{placeholders}` | Always use `.param("key", val)` to prevent injection. |
| **`.content()`** | Extracts raw text response | Standard for plain conversational output. |
| **`.chatResponse()`** | Extracts full response with token telemetry| Essential for auditing token costs and finish reasons. |
| **`.entity(Class<T>)`** | Deserializes LLM output into Java Record | Guarantees type safety without manual JSON parsing. |
| **Advisors** | Pre- and post-invocation interceptors | Use for logging, PII masking, chat memory, and RAG. |

---

## 7. Self-Check Questions & Practice Exercises

### Conceptual & Architectural Questions

#### Q1: What was the primary motivation for introducing `ChatClient` in Spring AI?
**Answer**: `ChatClient` was introduced to provide an ergonomic, fluent builder API inspired by `RestClient`. It abstracts low-level prompt construction, message role wrapping, dynamic parameter substitution, advisor interception, and structured output parsing into readable, chained method calls.

#### Q2: In Spring AI `ChatClient`, what is an "Advisor"?
**Answer**: An Advisor is an interceptor component implementing `before()` and `after()` lifecycle hooks (or `CallAroundAdvisor`). It wraps the conversational pipeline, enabling cross-cutting concerns like logging, PII sanitization, token metering, chat memory injection, and RAG document retrieval to execute cleanly outside business logic.

#### Q3: How does `ChatClient` convert an LLM response directly into a Java record or DTO?
**Answer**: By invoking `.call().entity(MyRecord.class)`. Spring AI automatically appends JSON schema instructions to the prompt and deserializes the model's structured JSON response into the target Java class.

#### Q4: Which built-in advisor in Spring AI is used to automatically log prompt requests, response completions, elapsed time, and token metrics?
**Answer**: **`SimpleLoggerAdvisor`**, which logs outgoing prompts and incoming responses along with latency in milliseconds and token counts.

#### Q5: Why should you use parameterized prompt templates (`u.text("...{param}...").param("param", value)`) instead of string concatenation `+`?
**Answer**: Parameterized prompt templates separate static prompt instructions from dynamic user variables. This prevents prompt injection syntax corruption, promotes template reusability, and allows advisors to inspect variables independently.

---

### Hands-On Practice Exercises

#### Exercise 1: Multi-Turn Conversation Memory Advisor
**Task**: Implement an in-memory `ConversationHistoryAdvisor` storing the last 5 turns of conversation in a `List<Message>`, injecting historical context on `before()` and recording assistant responses on `after()`.

```java
// Solution:
public class ConversationHistoryAdvisor implements Advisor {

    private final Map<String, List<Message>> memoryStore = new ConcurrentHashMap<>();
    private final int maxTurns = 5;

    @Override
    public String getName() { return "ConversationHistoryAdvisor"; }

    @Override
    public Prompt before(Prompt prompt) {
        String sessionId = "default-session";
        List<Message> history = memoryStore.getOrDefault(sessionId, new ArrayList<>());

        List<Message> combined = new ArrayList<>(history);
        combined.addAll(prompt.messages());
        return new Prompt(combined, prompt.options());
    }

    @Override
    public ChatResponse after(ChatResponse response) {
        String sessionId = "default-session";
        List<Message> history = memoryStore.computeIfAbsent(sessionId, k -> new ArrayList<>());
        
        history.add(response.getResult().output());
        while (history.size() > maxTurns * 2) {
            history.remove(0);
        }
        return response;
    }
}
```

#### Exercise 2: Toxic / Jailbreak Prompt Interceptor Advisor
**Task**: Write a `SafetyGuardAdvisor` that inspects prompt text for forbidden terms (e.g. `"ignore all previous instructions"`, `"jailbreak"`), throwing a `SecurityException` if detected.

```java
// Solution:
public class SafetyGuardAdvisor implements Advisor {

    private static final List<String> FORBIDDEN_PATTERNS = List.of(
        "ignore all previous instructions",
        "bypass safety",
        "jailbreak"
    );

    @Override
    public String getName() { return "SafetyGuardAdvisor"; }

    @Override
    public Prompt before(Prompt prompt) {
        for (Message msg : prompt.messages()) {
            String lower = msg.getContent().toLowerCase();
            for (String pattern : FORBIDDEN_PATTERNS) {
                if (lower.contains(pattern)) {
                    throw new SecurityException("400 Bad Request: Hostile prompt injection detected: '" + pattern + "'");
                }
            }
        }
        return prompt;
    }

    @Override
    public ChatResponse after(ChatResponse response) { return response; }
}
```

#### Exercise 3: Strongly-Typed Code Reviewer via `.entity()`
**Task**: Build a controller endpoint `POST /api/v1/ai/review` accepting raw Java code and returning a strongly-typed Java record `CodeReview(int qualityScore, List<String> bugsFound, List<String> improvements, boolean approved)`.

```java
// Solution:
@RestController
@RequestMapping("/api/v1/ai")
public class CodeReviewController {

    private final ChatClient chatClient;

    public CodeReviewController(ChatClient.Builder builder) {
        this.chatClient = builder
            .defaultSystem("You are a Principal Java Architect and automated static analyzer.")
            .build();
    }

    public record CodeReview(int qualityScore, List<String> bugsFound, List<String> improvements, boolean approved) {}

    @PostMapping(value = "/review", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<CodeReview> reviewCode(@RequestBody String javaCode) {
        CodeReview review = chatClient.prompt()
            .user(u -> u.text("""
                Review the following Java 21 code snippet:
                ```java
                {code}
                ```
                Provide quality score out of 100, identify potential bugs, and recommend modern Java improvements.
            """).param("code", javaCode))
            .call()
            .entity(CodeReview.class);

        return ResponseEntity.ok(review);
    }
}
```

---

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [◀ Day 32: Introduction to Spring AI](../Day_32_Introduction_to_Spring_AI/Day_32_Introduction_to_Spring_AI.md) | [All 60 Days Overview](../../README.md) | [Day 34: Prompt Engineering in Java ▶](../Day_34_Prompt_Engineering_in_Java/Day_34_Prompt_Engineering_in_Java.md) |
