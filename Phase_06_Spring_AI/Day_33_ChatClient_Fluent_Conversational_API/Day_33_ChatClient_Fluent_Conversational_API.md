# Day 33: ChatClient — The Fluent Conversational API
## Fluent Builder, Dynamic Prompt Templates, Enterprise Advisors & PII Sanitization

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [◀ Day 32: Introduction to Spring AI](../Day_32_Introduction_to_Spring_AI/Day_32_Introduction_to_Spring_AI.md) | [All 60 Days Overview](../../README.md) | [Day 34: Prompt Engineering in Java ▶](../Day_34_Prompt_Engineering_in_Java/Day_34_Prompt_Engineering_in_Java.md) |

---

## What Will You Learn Today?

Yesterday in Day 32, you were introduced to the big picture of Spring AI, setting up Ollama and exploring low-level `ChatModel` SPIs.

While calling `chatModel.call(new Prompt(...))` works, writing raw `Prompt` objects with manual message lists in every controller quickly leads to repetitive boilerplate code. You have to manually format strings, attach system prompts, configure hyperparameter options, and unpack nested `ChatResponse` objects.

To solve this, Spring AI introduced **`ChatClient`**: a modern, fluent conversational API inspired by Spring's acclaimed `RestClient` and `WebClient`.

Today, you will master:
- The anatomy of the `ChatClient.Builder`: Configuring default system instructions, default model options, and global interceptors.
- Dynamic prompt parameter substitution: Rendering `{placeholder}` templates safely without fragile string concatenations.
- The `call()` response specification: Extracting text with `.content()`, rich metadata with `.chatResponse()`, and structured Java DTOs with `.entity(Class<T>)`.
- The **Advisor Interceptor Pattern**: Spring AI's equivalent of Servlet Filters or Spring AOP for AI prompt and response pipelines.
- Built-in Advisors: `SimpleLoggerAdvisor`, `MessageChatMemoryAdvisor`, and `QuestionAnswerAdvisor`.
- Engineering custom enterprise advisors: Building a **PII Redaction Advisor** that automatically sanitizes Credit Card numbers and SSNs *before* prompts leave your JVM!

---

## Real-World Analogy: The Executive Assistant & The Briefing Dossier

Imagine a Fortune 500 CEO preparing for high-stakes business meetings:

```
+---------------------------------------------------------------------------------------------------+
|                                  THE EXECUTIVE ASSISTANT MODEL                                    |
|                                                                                                   |
|  WITHOUT AN ASSISTANT (Raw ChatModel):                                                            |
|  - The CEO must personally photocopy documents, look up phone numbers, type transcripts, and      |
|    translate foreign emails.                                                                      |
|  - High cognitive load, repetitive manual tasks, error-prone.                                     |
|                                                                                                   |
|  WITH A DEDICATED EXECUTIVE ASSISTANT (Spring AI ChatClient):                                     |
|  - Standing Orders (.defaultSystem): "Always address me respectfully and keep summaries to 1 page."|
|  - The Briefing Dossier (.prompt().user(u -> u.text("Summarize {company}").param(...))):           |
|    The assistant takes standard templates and automatically fills in client-specific variables.   |
|  - Security & Compliance Advisor (Custom Advisor Interceptor):                                   |
|    Before handing any dossier to the CEO, the security team blackouts confidential bank account   |
|    numbers and trade secrets (PII Redaction).                                                     |
|  - Executive Output (.call().entity(Report.class)):                                               |
|    The assistant doesn't just hand over messy raw notes; they format everything into an organized  |
|    executive binder matching a precise table of contents!                                         |
+---------------------------------------------------------------------------------------------------+
```

---

## The Evolution: `ChatModel` (SPI) vs. `ChatClient` (API)

It is crucial to understand the architectural distinction between these two components:

```
┌────────────────────────────────────────────────────────────────────────┐
│ APPLICATION CODE (Controllers, Services, Use Cases)                    │
│                                                                        │
│   chatClient.prompt()                                                  │
│       .system("You are a financial advisor")                          │
│       .user(u -> u.text("Analyze {stock}").param("stock", "GOOGL"))    │
│       .advisors(new LoggingAdvisor(), new PiiRedactionAdvisor())       │
│       .call()                                                          │
│       .content();                                                      │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                       (Fluent API Delegation)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ CHATCLIENT ENGINE                                                      │
│ - Renders prompt templates                                             │
│ - Executes Advisor 'before()' chain                                    │
│ - Assembles immutable Prompt(List<Message>, ChatOptions)               │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                        (Low-level SPI Invocation)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ CHATMODEL IMPLEMENTATION (OllamaChatModel / OpenAiChatModel)           │
│ - Dispatches HTTP REST / gRPC call to LLM Engine                       │
│ - Unpacks raw JSON completion into ChatResponse                        │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                        (Returns ChatResponse)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ CHATCLIENT RESPONSE PROCESSOR                                          │
│ - Executes Advisor 'after()' chain (e.g. latency logging)              │
│ - Maps JSON into Java Record if .entity(...) requested                 │
│ - Returns String, ChatResponse, or Java DTO                            │
└────────────────────────────────────────────────────────────────────────┘
```

---

## Configuring the `ChatClient.Builder`

In a Spring Boot application, Spring AI automatically provides a pre-configured `ChatClient.Builder` bean.

You customize it using the builder pattern to establish application-wide defaults:

```java
package com.genai.springai.config;

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
            // 1. Default System Directive (Applied to every prompt)
            .defaultSystem("""
                You are a senior customer support representative for CloudTech Inc.
                Maintain a professional, empathetic tone.
                Never reveal internal server infrastructure or database passwords.
            """)
            // 2. Default Model Options
            .defaultOptions(ChatOptions.builder()
                .temperature(0.3)      // Low temperature for factual consistency
                .maxTokens(1024)        // Enforce token boundary
                .build())
            // 3. Default Advisors (Interceptors)
            .defaultAdvisors(new SimpleLoggerAdvisor())
            .build();
    }
}
```

---

## Dynamic Prompt Templates & Parameter Substitution

Never concatenate user input directly into prompt strings using `+` or `String.format`:

```java
// ❌ DANGEROUS ANTI-PATTERN: Vulnerable to prompt injection and unreadable
String badPrompt = "Translate " + text + " to " + targetLang;

// ✅ PRODUCTION BEST PRACTICE: Using ChatClient fluent parameter binding
String goodResponse = chatClient.prompt()
    .user(u -> u.text("Translate the following {text} into {targetLang}. Retain formatting.")
        .param("text", rawUserInput)
        .param("targetLang", "German"))
    .call()
    .content();
```

### Why use parameterized prompt templates?
1. **Safety**: Separates template syntax from dynamic variables.
2. **Reusability**: Templates can be stored in external `.st` (StringTemplate) resource files and loaded dynamically.
3. **Observability**: Advisors can inspect template variables independently from the base prompt.

---

## Handling Responses: `.content()`, `.chatResponse()`, and `.entity()`

`ChatClient` gives you three distinct ways to consume the model's output:

### 1. Simple String Content (`.content()`)
When you only need the plain text generated by the model:

```java
String summary = chatClient.prompt()
    .user("Explain Quarkus in one sentence")
    .call()
    .content();
```

### 2. Full Metadata & Token Telemetry (`.chatResponse()`)
When you need to audit token usage, finish reasons, or inspect model metadata:

```java
ChatResponse response = chatClient.prompt()
    .user("Generate a 3-day travel itinerary for Tokyo")
    .call()
    .chatResponse();

// Access generation output
String text = response.getResult().getOutput().getContent();

// Inspect finish reason (e.g. "STOP", "LENGTH")
String finishReason = response.getResult().getMetadata().getFinishReason();

// Inspect exact token count
Usage usage = response.getMetadata().getUsage();
long promptTokens = usage.getPromptTokens();
long completionTokens = usage.getGenerationTokens();
long totalTokens = usage.getTotalTokens();
```

### 3. Structured Java Record Output (`.entity(Class<T>)`)
> [!TIP]
> **No Manual JSON Parsing Required!**
> Spring AI automatically instructs the LLM to format its response as JSON adhering to your Java record's schema, and deserializes the result directly into your strongly-typed Java DTO!

```java
public record MovieRecommendation(
    String title,
    int releaseYear,
    String director,
    List<String> genres,
    double rating
) {}

// LLM response is automatically mapped directly into your Java record!
MovieRecommendation movie = chatClient.prompt()
    .user("Recommend a mind-bending sci-fi movie from the 2010s")
    .call()
    .entity(MovieRecommendation.class);

System.out.println("Title: " + movie.title() + " (" + movie.releaseYear() + ")");
System.out.println("Directed by: " + movie.director());
```

---

## The Advisor Interceptor Pattern: AOP for Artificial Intelligence

One of the most powerful innovations in Spring AI is the **Advisor** concept.

An Advisor intercepts the conversational pipeline in two places:
1. `before(Prompt)`: Executed **before** the prompt is sent to the LLM.
2. `after(ChatResponse)`: Executed **after** the response is received from the LLM.

```
                         THE ADVISOR EXECUTION PIPELINE
                         
 User Request
      │
      ▼
┌──────────────┐
│ Prompt Input │
└──────┬───────┘
       │
       ▼
┌────────────────────────────────────────────────────────┐
│ Advisor 1 (before): PiiRedactionAdvisor                │ ── Masks credit cards & SSNs
└──────┬─────────────────────────────────────────────────┘
       │
       ▼
┌────────────────────────────────────────────────────────┐
│ Advisor 2 (before): SimpleLoggerAdvisor                │ ── Records start time and prompt
└──────┬─────────────────────────────────────────────────┘
       │
       ▼
┌────────────────────────────────────────────────────────┐
│ ChatModel SPI                                          │ ── Calls OpenAI / Ollama
└──────┬─────────────────────────────────────────────────┘
       │
       ▼
┌────────────────────────────────────────────────────────┐
│ Advisor 2 (after): SimpleLoggerAdvisor                 │ ── Logs elapsed ms and token usage
└──────┬─────────────────────────────────────────────────┘
       │
       ▼
┌────────────────────────────────────────────────────────┐
│ Advisor 1 (after): PiiRedactionAdvisor                 │ ── Returns final ChatResponse
└──────┬─────────────────────────────────────────────────┘
       │
       ▼
 Caller Receives Result
```

### Built-in Spring AI Advisors:
1. **`SimpleLoggerAdvisor`**: Logs outgoing prompts and incoming responses with elapsed milliseconds and token usage.
2. **`MessageChatMemoryAdvisor`**: Connects to a conversational memory store (in-memory or Redis) and automatically injects conversation history so the LLM remembers previous messages.
3. **`QuestionAnswerAdvisor`**: Injects relevant document snippets from a vector store (RAG) directly into the prompt.

---

## Building an Enterprise PII Redaction Advisor

In enterprise banking and healthcare environments, sending unmasked user data (credit cards, social security numbers) to external cloud LLMs violates GDPR, HIPAA, and PCI-DSS regulations.

Let's build a production-grade **`PiiRedactionAdvisor`** that intercepts outgoing prompts, detects sensitive patterns, masks them with `[REDACTED_CARD]` and `[REDACTED_SSN]`, and logs the security event before any network packet leaves the machine:

```java
package com.genai.springai.chatclient;

import com.genai.springai.core.ChatResponse;
import com.genai.springai.core.Message;
import com.genai.springai.core.Prompt;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PiiRedactionAdvisor implements Advisor {

    private static final Pattern CREDIT_CARD_PATTERN = 
            Pattern.compile("\\b(?:\\d{4}[ -]?){3}\\d{4}\\b");
    
    private static final Pattern SSN_PATTERN = 
            Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");

    @Override
    public String getName() {
        return "PiiRedactionAdvisor";
    }

    @Override
    public Prompt before(Prompt prompt) {
        List<Message> sanitized = new ArrayList<>();
        boolean redactedAny = false;

        for (Message msg : prompt.messages()) {
            String content = msg.getContent();
            String redacted = CREDIT_CARD_PATTERN.matcher(content).replaceAll("[REDACTED_CARD]");
            redacted = SSN_PATTERN.matcher(redacted).replaceAll("[REDACTED_SSN]");

            if (!redacted.equals(content)) {
                redactedAny = true;
            }

            if (msg.getMessageType() == Message.MessageType.SYSTEM) {
                sanitized.add(new Message.SystemMessage(redacted, msg.getMetadata()));
            } else if (msg.getMessageType() == Message.MessageType.USER) {
                sanitized.add(new Message.UserMessage(redacted, msg.getMetadata()));
            } else {
                sanitized.add(new Message.AssistantMessage(redacted, msg.getMetadata()));
            }
        }

        if (redactedAny) {
            System.out.println("  [ADVISOR: PII_GUARD] 🛡️ Sensitive PII detected and redacted before reaching LLM provider!");
        }

        return new Prompt(sanitized, prompt.options());
    }

    @Override
    public ChatResponse after(ChatResponse response) {
        return response;
    }
}
```

---

## Step-by-Step Production Code Walkthrough

Let's inspect the runnable companion code built for today's lesson in `Phase_06_Spring_AI/Day_33_ChatClient_Fluent_Conversational_API/code/`:

### 1. `PromptTemplate.java`
Renders parameterized string templates cleanly:

```java
public String render(Map<String, Object> variables) {
    if (variables == null || variables.isEmpty()) return template;
    String result = template;
    for (Map.Entry<String, Object> entry : variables.entrySet()) {
        result = result.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
    }
    return result;
}
```

### 2. `Advisor.java` & Interceptor Chain
Defines the bidirectional interceptor hooks executed by `ChatClient`:

```java
public CallResponseSpec call() {
    Prompt prompt = new Prompt(messages, options);

    // Execute Advisors: before() hooks
    for (Advisor advisor : advisors) {
        prompt = advisor.before(prompt);
    }

    // Execute low-level ChatModel SPI
    ChatResponse response = client.chatModel.call(prompt);

    // Execute Advisors: after() hooks (in reverse order)
    for (int i = advisors.size() - 1; i >= 0; i--) {
        response = advisors.get(i).after(response);
    }

    return new CallResponseSpec(response);
}
```

### 3. Running the Complete Verification Suite
Compile and execute the demonstration:

```bash
javac -d out Phase_06_Spring_AI/Day_32_Introduction_to_Spring_AI/code/*.java Phase_06_Spring_AI/Day_33_ChatClient_Fluent_Conversational_API/code/*.java
java -cp out com.genai.springai.chatclient.ChatClientDemo
```

Output:
```text
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

## Hands-On Exercises (With Complete Solutions)

### Exercise 1: Multi-Turn Conversation with History Advisor
**Problem Statement:**  
Create an in-memory `ConversationHistoryAdvisor` that stores the last 5 turns of user and assistant messages in a `List<Message>` keyed by `conversationId`. Before each call, it injects the previous messages into the prompt, and after each call, it records the new assistant response.

<details>
<summary>👉 View Solution</summary>

```java
package com.genai.springai.chatclient;

import com.genai.springai.core.ChatResponse;
import com.genai.springai.core.Message;
import com.genai.springai.core.Prompt;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ConversationHistoryAdvisor implements Advisor {

    private final Map<String, List<Message>> memoryStore = new ConcurrentHashMap<>();
    private final int maxTurns = 5;

    @Override
    public String getName() {
        return "ConversationHistoryAdvisor";
    }

    @Override
    public Prompt before(Prompt prompt) {
        String conversationId = "default-session"; // In production, extract from context/advisorspec
        List<Message> history = memoryStore.getOrDefault(conversationId, new ArrayList<>());

        List<Message> combined = new ArrayList<>(history);
        combined.addAll(prompt.messages());

        return new Prompt(combined, prompt.options());
    }

    @Override
    public ChatResponse after(ChatResponse response) {
        String conversationId = "default-session";
        List<Message> history = memoryStore.computeIfAbsent(conversationId, k -> new ArrayList<>());
        
        history.add(response.getResult().output());
        while (history.size() > maxTurns * 2) {
            history.remove(0);
        }
        return response;
    }
}
```
</details>

---

### Exercise 2: Toxic / Jailbreak Prompt Interceptor Advisor
**Problem Statement:**  
Write a custom `SafetyGuardAdvisor` that checks incoming user prompt text for forbidden terms (e.g., `"ignore all previous instructions"`, `"jailbreak"`, `"bypass safety"`). If detected, it throws a `SecurityException("Jailbreak attempt detected and blocked")` before the prompt is sent to the LLM.

<details>
<summary>👉 View Solution</summary>

```java
package com.genai.springai.chatclient;

import com.genai.springai.core.ChatResponse;
import com.genai.springai.core.Message;
import com.genai.springai.core.Prompt;

import java.util.List;

public class SafetyGuardAdvisor implements Advisor {

    private static final List<String> FORBIDDEN_PATTERNS = List.of(
        "ignore all previous instructions",
        "bypass safety",
        "jailbreak",
        "system prompt reveal"
    );

    @Override
    public String getName() {
        return "SafetyGuardAdvisor";
    }

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
    public ChatResponse after(ChatResponse response) {
        return response;
    }
}
```
</details>

---

### Exercise 3: Strongly-Typed Code Reviewer via `.entity()`
**Problem Statement:**  
Write a Spring REST controller endpoint `POST /api/v1/ai/review` that accepts a raw Java source code snippet and uses `ChatClient` to return a strongly-typed Java record:
`public record CodeReview(int qualityScore, List<String> bugsFound, List<String> improvements, boolean approved)`.

<details>
<summary>👉 View Solution</summary>

```java
@RestController
@RequestMapping("/api/v1/ai")
public class CodeReviewController {

    private final ChatClient chatClient;

    public CodeReviewController(ChatClient.Builder builder) {
        this.chatClient = builder
            .defaultSystem("You are a Principal Java Architect and automated static analyzer.")
            .build();
    }

    public record CodeReview(
        int qualityScore,
        List<String> bugsFound,
        List<String> improvements,
        boolean approved
    ) {}

    @PostMapping(value = "/review", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<CodeReview> reviewCode(@RequestBody String javaCode) {
        CodeReview review = chatClient.prompt()
            .user(u -> u.text("""
                Perform an in-depth code review on the following Java 21 code snippet:
                ```java
                {code}
                ```
                Provide a quality score out of 100, identify potential bugs (concurrency, null pointers, resource leaks),
                and recommend idiomatic modern Java improvements.
            """).param("code", javaCode))
            .call()
            .entity(CodeReview.class);

        return ResponseEntity.ok(review);
    }
}
```
</details>

---

## 5-Question Self-Check Quiz

#### 1. What was the primary motivation for introducing `ChatClient` in Spring AI?
- A) To replace Java with Python in Spring Boot.
- B) To provide a fluent, ergonomic builder API similar to `RestClient` that abstracts prompt building, options, advisors, and structured output parsing.
- C) Because `ChatModel` was deleted from the library.
- D) To enforce billing on every API call.

#### 2. In Spring AI `ChatClient`, what is an "Advisor"?
- A) A customer service chatbot.
- B) An interceptor component with `before()` and `after()` hooks that wraps prompt creation and response processing (similar to an HTTP Filter or AOP aspect).
- C) A database schema generator.
- D) An IDE plugin for IntelliJ.

#### 3. How does `ChatClient` support converting an LLM response directly into a Java record or DTO?
- A) By calling `.call().entity(MyRecord.class)`.
- B) By running `eval()` on Python scripts.
- C) Through XML serialization.
- D) Java records cannot be generated by LLMs.

#### 4. Which built-in advisor in Spring AI is used to automatically log prompt requests, response completions, elapsed time, and token metrics?
- A) `MetricsAdvisor`
- B) `SimpleLoggerAdvisor`
- C) `TomcatLoggingFilter`
- D) `AuditAspect`

#### 5. Why should you use `u.text("...{param}...").param("param", value)` instead of string concatenation `+`?
- A) String concatenation causes memory leaks in JVM.
- B) Parameterized prompt templates prevent prompt injection syntax breaking, allow template reusability, and integrate cleanly with advisors.
- C) Because Java 21 removed the `+` operator.
- D) String concatenation only works with integers.

---

### Quiz Answers & Explanations

1. **B is correct**: `ChatClient` was introduced to provide an ergonomic, fluent builder API that makes working with LLMs as natural as calling REST APIs with `RestClient`.
2. **B is correct**: The Advisor pattern provides a standardized pre/post interception mechanism for cross-cutting concerns (logging, chat memory, RAG, PII sanitization).
3. **A is correct**: `.call().entity(TargetClass.class)` leverages Spring AI's structured output converters to return strongly-typed Java objects.
4. **B is correct**: `SimpleLoggerAdvisor` is the official Spring AI advisor for request/response logging and latency metrics.
5. **B is correct**: Using template parameters cleanly separates static instructions from dynamic user inputs.

---

## Day 33 Summary & Next Steps

Today you mastered:
1. **The `ChatClient` Architecture**: Designing clean conversational applications with the fluent builder API.
2. **Template Parameter Substitution**: Safely injecting dynamic runtime variables into structured prompt templates.
3. **Structured Entity Mapping**: Mapping LLM output directly into strongly-typed Java records using `.call().entity(...)`.
4. **The Advisor Interceptor Pattern**: Utilizing pre- and post-invocation hooks to handle cross-cutting concerns.
5. **PII Redaction Defense**: Building an enterprise advisor that sanitizes credit cards and SSNs before data leaves your JVM.

👉 **Tomorrow in Day 34: Prompt Engineering in Java** — You will master advanced prompt engineering techniques in Java: Few-Shot Prompting, Chain-of-Thought (CoT), System Personas, and loading external prompt templates from `.st` resource files!
