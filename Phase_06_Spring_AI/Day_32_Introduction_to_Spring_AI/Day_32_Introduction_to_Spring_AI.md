# Day 32: Introduction to Spring AI — The Big Picture
## Architecture, Portable Model Abstractions, Local Ollama Setup & First ChatClient Call

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [◀ Day 31: Rate Limiting, CORS & API Security](../../Phase_05_Spring_Security/Day_31_Rate_Limiting_CORS_API_Security/Day_31_Rate_Limiting_CORS_API_Security.md) | [All 60 Days Overview](../../README.md) | [Day 33: ChatClient — The Fluent Conversational API ▶](../Day_33_ChatClient_Fluent_Conversational_API/Day_33_ChatClient_Fluent_Conversational_API.md) |

---

## What Will You Learn Today?

Welcome to **Phase 6: Spring AI — The Core Framework**!

In Phases 1 through 5, you built a robust, enterprise-grade Java 21 and Spring Boot 3 foundation: Object-Oriented Design, Virtual Threads, Dependency Injection, REST APIs, JPA with `pgvector`, and bulletproof Spring Security.

Now, you begin connecting your Java backends directly to Large Language Models (LLMs).

Today, you will master:
- The genesis and architectural philosophy of **Spring AI**: Why Java is rapidly taking over enterprise AI from Python.
- The **"JDBC of AI"** abstraction model: How Spring AI enables you to switch between OpenAI, Anthropic Claude, Google Gemini, and local open-weight models (Llama 3.2, Mistral) with a single property change in `application.yml` and **zero Java code rewrites**.
- The core Spring AI building blocks: `ChatModel`, `Prompt`, `Message` (`SystemMessage`, `UserMessage`, `AssistantMessage`), `ChatResponse`, and the fluent `ChatClient`.
- Setting up **Ollama** locally for **100% free, private, offline AI execution** without sharing confidential data with third-party cloud APIs.
- Setting up the Spring AI Maven BOM (`spring-ai-bom`) and starters in Spring Boot 3.
- Building and invoking your very first conversational `ChatClient` endpoint with token usage telemetry.

---

## Real-World Analogy: JDBC for Databases vs. Spring AI for LLMs

To understand why Spring AI is revolutionary, look back at the history of relational databases:

```
+----------------------------------------------------------------------------------------------------+
|                                    THE POWER OF PORTABLE ABSTRACTION                               |
|                                                                                                    |
|  BEFORE JDBC (1995): Vendor Lock-in Chaos                                                          |
|  - Oracle wrote a C library with proprietary function names (`ora_connect`, `ora_exec`).          |
|  - MySQL had completely different functions (`mysql_real_query`).                                  |
|  - If your company switched from Oracle to PostgreSQL, you had to rewrite every single line of DB  |
|    access code!                                                                                    |
|                                                                                                    |
|  THE JDBC REVOLUTION: One Interface, Infinite Drivers                                              |
|  - Java introduced `java.sql.Connection` and `java.sql.PreparedStatement`.                         |
|  - Your application code only talks to standard Java interfaces.                                   |
|  - Switch from Oracle to PostgreSQL? Just change the driver JAR and JDBC URL in `application.yml`!|
|                                                                                                    |
|  SPRING AI (2024+): The "JDBC of Artificial Intelligence"                                          |
|  - OpenAI has an API. Anthropic has an API. Google has an API. Ollama has an API.                  |
|  - Spring AI introduces `ChatModel`, `EmbeddingModel`, and `VectorStore`.                          |
|  - Your Java code speaks to `ChatClient`.                                                          |
|  - Switch from OpenAI GPT-4o to a self-hosted Llama 3.2 on Kubernetes?                             |
|    Change ONE line in `application.yml`. ZERO Java code changes!                                   |
+----------------------------------------------------------------------------------------------------+
```

---

## Why Java is Winning the Enterprise AI Race

In 2023, the initial wave of Generative AI experimentation happened in Python using Jupyter notebooks, LangChain, and Streamlit.

However, when Fortune 500 enterprises, banks, healthcare conglomerates, and defense contractors moved from **AI Prototypes** to **Mission-Critical Production Systems**, Python hit a brick wall:

```
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                           PYTHON PROTOTYPING vs. JAVA 21 PRODUCTION                             │
├───────────────────────────────┬─────────────────────────────────────────────────────────────────┤
│ Dimension                     │ The Enterprise Reality                                          │
├───────────────────────────────┼─────────────────────────────────────────────────────────────────┤
│ 1. Existing Infrastructure    │ 80%+ of Fortune 500 transactional core systems run on the JVM.  │
│                               │ Rewriting core banking/ERP to Python is impossible.             │
├───────────────────────────────┼─────────────────────────────────────────────────────────────────┤
│ 2. Concurrency & Throughput   │ Python's Global Interpreter Lock (GIL) struggles under high     │
│                               │ load. Java 21's Virtual Threads handle 100,000 concurrent       │
│                               │ streaming LLM connections with negligible RAM overhead.         │
├───────────────────────────────┼─────────────────────────────────────────────────────────────────┤
│ 3. Type Safety & Maintainability│ Dynamic Python scripts break at runtime with `KeyError` or      │
│                               │ `TypeError`. Java 21 Records, sealed classes, and strict type   │
│                               │ safety eliminate runtime hallucination parsing bugs.            │
├───────────────────────────────┼─────────────────────────────────────────────────────────────────┤
│ 4. Enterprise Security        │ Spring Security, OAuth2, RBAC, and auditing are unmatched in    │
│                               │ enterprise compliance. Python web frameworks require ad-hoc glue│
│                               │ that frequently fails penetration testing.                      │
└───────────────────────────────┴─────────────────────────────────────────────────────────────────┘
```

Spring AI bridges this gap, allowing enterprise Java developers to build production AI without leaving the JVM ecosystem!

---

## Spring AI Core Architecture

Spring AI organizes all artificial intelligence interactions around clean, decoupled interfaces:

```
                               SPRING AI ARCHITECTURE OVERVIEW
                               
                               ┌─────────────────────────────┐
                               │         ChatClient          │  (High-level Fluent API)
                               └──────────────┬──────────────┘
                                              │ delegates to
                                              ▼
                               ┌─────────────────────────────┐
                               │          ChatModel          │  (Core Low-level SPI)
                               └──────────────┬──────────────┘
                                              │
                 ┌────────────────────────────┼────────────────────────────┐
                 ▼                            ▼                            ▼
      ┌──────────────────────┐   ┌──────────────────────────┐   ┌──────────────────────┐
      │   OllamaChatModel    │   │     OpenAiChatModel      │   │   AnthropicChatModel │
      │  (Llama 3.2 / Local) │   │    (GPT-4o / Cloud)      │   │   (Claude 3.5 Sonnet)│
      └──────────┬───────────┘   └────────────┬─────────────┘   └──────────┬───────────┘
                 │                            │                            │
                 ▼                            ▼                            ▼
          Local GPU/CPU                OpenAI Cloud API            Anthropic Cloud API
```

### The Request / Response Data Flow:
Every interaction with an LLM follows this immutable object pipeline:

```
┌────────────────┐
│     Prompt     │ ── Contains: List<Message> (System + User) + ChatOptions (temp, tokens)
└───────┬────────┘
        │ passed into
        ▼
┌────────────────┐
│   ChatModel    │ ── Calls model provider (HTTP REST / gRPC)
└───────┬────────┘
        │ returns
        ▼
┌────────────────┐
│  ChatResponse  │ ── Contains: List<Generation> (AssistantMessage) + UsageMetadata (tokens)
└────────────────┘
```

---

## Understanding the Message Roles

LLMs do not understand concepts like "users" or "files"; they process conversations as a sequence of **Role-tagged Messages**:

| Message Class | Role Name | Purpose | Example |
|:---|:---|:---|:---|
| `SystemMessage` | `system` | Sets the persona, guardrails, tone, and operational boundaries of the AI. The end user cannot override this directly. | *"You are an expert Java Architect. Answer only questions about Spring Boot. Never write Python code."* |
| `UserMessage` | `user` | The prompt or question asked by the real human or calling client application. | *"How do I configure connection pooling with HikariCP?"* |
| `AssistantMessage` | `assistant` | The response generated by the LLM. In multi-turn chat, previous assistant messages are passed back into the prompt to provide conversational context. | *"To configure HikariCP in Spring Boot, add the following properties to application.yml..."* |

---

## Setting Up Your Free Local AI Engine: Ollama

To develop AI applications without paying for OpenAI API credits or risking enterprise data privacy, we use **Ollama**.

Ollama is an open-source tool that lets you run modern open-weight LLMs (Meta's Llama 3.2, Mistral, Microsoft Phi-3, Google Gemma 2) directly on your local computer using CPU or GPU.

### Step 1: Install Ollama
- **Windows / Mac**: Download the native installer from [ollama.com](https://ollama.com/download).
- **Docker**: If you prefer Docker, you can run Ollama using the course `docker-compose.yml`:

```yaml
version: '3.8'
services:
  ollama:
    image: ollama/ollama:latest
    container_name: ollama-local
    ports:
      - "11434:11434"
    volumes:
      - ollama_data:/root/.ollama

volumes:
  ollama_data:
```

### Step 2: Download a Model
Open your terminal and pull **Llama 3.2** (Meta's lightweight, high-performance 3B model):

```bash
ollama run llama3.2
```

Ollama downloads the weights (~2.0 GB) and starts a local REST server on port `11434`.

### Step 3: Verify the Ollama REST API
Test that Ollama is responding via HTTP:

```bash
curl http://localhost:11434/api/generate -d '{
  "model": "llama3.2",
  "prompt": "Say hello in one word",
  "stream": false
}'
```

You will receive an instant JSON response:
```json
{"model":"llama3.2","response":"Hello!","done":true}
```

---

## Setting Up a Spring AI Project (Maven)

### Step 1: Add the Spring AI BOM (Bill of Materials)
Because Spring AI is an umbrella project with multiple providers, declare its BOM inside your `pom.xml`:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>1.0.0-M4</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### Step 2: Add Model Starters
To use Ollama locally:
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-ollama-spring-boot-starter</artifactId>
</dependency>
```

To support OpenAI simultaneously (for production cloud deployment):
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>
```

### Step 3: Configure `application.yml`
```yaml
spring:
  application:
    name: enterprise-genai-service
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: llama3.2
          temperature: 0.7
          top-p: 0.9
    openai:
      api-key: ${OPENAI_API_KEY:dummy-key}
      chat:
        options:
          model: gpt-4o
          temperature: 0.5
```

---

## Building Your First Spring AI Controller

In Spring AI, Spring Boot automatically creates a pre-configured `ChatClient.Builder` bean for you. You simply inject it into your service or controller:

```java
package com.genai.springai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
public class FirstAiController {

    private final ChatClient chatClient;

    // Spring automatically injects the auto-configured ChatClient.Builder
    public FirstAiController(ChatClient.Builder builder) {
        this.chatClient = builder
            .defaultSystem("You are an expert enterprise Java engineer. Answer concisely with runnable code.")
            .build();
    }

    @GetMapping("/ask")
    public String askQuestion(@RequestParam String question) {
        return chatClient.prompt()
            .user(question)
            .call()
            .content();
    }
}
```

That's it! In less than 10 lines of Java, your application is connected to an LLM, complete with system guardrails, thread safety, connection pooling, and error handling.

---

## Step-by-Step Production Code Walkthrough

Let's inspect the companion classes written for Day 32 in `Phase_06_Spring_AI/Day_32_Introduction_to_Spring_AI/code/`:

### 1. `Message.java` & Message Hierarchy
Encapsulates `SystemMessage`, `UserMessage`, and `AssistantMessage` using Java 21 records:

```java
public interface Message {
    enum MessageType { SYSTEM, USER, ASSISTANT }
    MessageType getMessageType();
    String getContent();
    Map<String, Object> getMetadata();

    record SystemMessage(String content, Map<String, Object> metadata) implements Message {
        public SystemMessage(String content) { this(content, Map.of()); }
        @Override public MessageType getMessageType() { return MessageType.SYSTEM; }
        @Override public String getContent() { return content; }
        @Override public Map<String, Object> getMetadata() { return metadata; }
    }
    // UserMessage and AssistantMessage records...
}
```

### 2. `Prompt.java` & `ChatOptions.java`
Models the input contract passed to the LLM:

```java
public record Prompt(
        List<Message> messages,
        ChatOptions options
) {
    public Prompt(String singleUserPrompt) {
        this(List.of(new Message.UserMessage(singleUserPrompt)), ChatOptions.defaults());
    }
}
```

### 3. `ChatModel.java` & Pluggable Implementations
Defines the low-level provider SPI:

```java
public interface ChatModel {
    ChatResponse call(Prompt prompt);
    default String call(String message) { ... }
    String getProviderName();
}
```

Both `OllamaChatModel` and `OpenAiChatModel` implement this exact interface, proving that your business logic remains completely insulated from the underlying AI vendor.

### 4. `ChatClient.java`
Implements the high-level fluent API that developers use daily:

```java
ChatResponse response = chatClient.prompt()
        .user("Compare Virtual Threads with WebFlux for AI streaming workloads.")
        .options(ChatOptions.builder()
                .model("llama3.2")
                .temperature(0.2)
                .maxTokens(2048)
                .build())
        .call()
        .chatResponse();
```

### 5. Running the Verification Test Suite
Compile and execute the demonstration:

```bash
javac -d out Phase_06_Spring_AI/Day_32_Introduction_to_Spring_AI/code/*.java
java -cp out com.genai.springai.core.SpringAiDemo
```

Output:
```text
================================================================================
  DAY 32: SPRING AI FOUNDATIONS & CHATCLIENT DEMONSTRATION                      
================================================================================

[TEST 1] Initializing Local Ollama ChatModel (Llama 3.2)...
  Active Provider: Ollama (Local - llama3.2 at http://localhost:11434)
  Response: [Ollama - Llama 3.2] Spring AI is the official Spring ecosystem framework for building AI applications in Java. It brings portable abstractions for ChatModels, VectorStores, and Document Readers, eliminating vendor lock-in!

[TEST 2] Provider Portability: Switching to OpenAI Cloud Model...
  Active Provider: OpenAI (Cloud - gpt-4o)
  Response: [OpenAI - gpt-4o] Advanced reasoning output for: 'Explain Spring AI architecture'

[TEST 3] Fluent ChatClient Builder with System Directives...
  Generated Content:
  [Ollama - Llama 3.2 (Local Engine)] Processed query: 'Compare Virtual Threads with WebFlux for AI streaming workloads.' (Directive: You are a Principal Java Cloud Architect. Answer strictly with technical rigor.)

  Finish Reason: STOP

[TEST 4] Token Usage Telemetry:
  Prompt Tokens:     45
  Generation Tokens: 61
  Total Tokens:      106

================================================================================
  SPRING AI FOUNDATIONS VERIFIED SUCCESSFULLY! READY FOR PRODUCTION LLMS.       
================================================================================
```

---

## Why It Matters for Gen AI Applications

| Feature / Challenge | Direct Vendor SDK (e.g. OpenAI Python) | Spring AI (Java 21) |
|:---|:---|:---|
| **Vendor Lock-in** | Your codebase is tightly bound to `import openai`. Switching to Anthropic or Llama requires rewriting calls. | One interface (`ChatClient`). Switch providers via `application.yml` without modifying Java code. |
| **Data Privacy & Compliance** | Data must leave your network to third-party cloud APIs. | Route sensitive queries to local **Ollama**; zero bytes leave your data center. |
| **Token Auditing** | Manual calculation of tokens and billing logs. | Built-in `UsageMetadata` returned in every `ChatResponse` for centralized metering. |
| **Concurrency** | Blocked threads or complex async callbacks. | Seamless integration with Java 21 **Virtual Threads** for maximum streaming scale. |

---

## Hands-On Exercises (With Complete Solutions)

### Exercise 1: Multi-Provider Fallback Service
**Problem Statement:**  
Write a Spring service `ResilientAiService` that takes two `ChatModel` beans: a primary local `OllamaChatModel` and a secondary fallback `OpenAiChatModel`. When `generateResponse(String prompt)` is called, it attempts to use Ollama first. If Ollama throws an exception (e.g. local machine is overloaded or offline), it catches the error, logs a warning, and falls back to OpenAI seamlessly.

<details>
<summary>👉 View Solution</summary>

```java
package com.genai.springai.service;

import com.genai.springai.core.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ResilientAiService {

    private final ChatModel primaryModel;
    private final ChatModel fallbackModel;

    public ResilientAiService(
            @Qualifier("ollamaChatModel") ChatModel primaryModel,
            @Qualifier("openAiChatModel") ChatModel fallbackModel
    ) {
        this.primaryModel = primaryModel;
        this.fallbackModel = fallbackModel;
    }

    public String generateResponse(String prompt) {
        try {
            return primaryModel.call(prompt);
        } catch (Exception ex) {
            System.err.println("Primary model (" + primaryModel.getProviderName() 
                    + ") failed: " + ex.getMessage() + ". Failing over to fallback provider...");
            return fallbackModel.call(prompt);
        }
    }
}
```
*Explanation:* Spring's Dependency Injection allows you to inject multiple `ChatModel` beans using `@Qualifier` and implement enterprise resilience patterns (fallback, circuit breaker, retry) cleanly.
</details>

---

### Exercise 2: Token Telemetry & Cost Calculator
**Problem Statement:**  
Create a component `TokenCostTracker` that accepts a `ChatResponse` and calculates the financial cost of the call based on standard cloud pricing:
- Input Prompt Tokens: $0.005 per 1,000 tokens
- Output Generation Tokens: $0.015 per 1,000 tokens

Return a record `CostReport(long promptTokens, long genTokens, double totalCostUsd)`.

<details>
<summary>👉 View Solution</summary>

```java
package com.genai.springai.service;

import com.genai.springai.core.ChatResponse;
import org.springframework.stereotype.Component;

@Component
public class TokenCostTracker {

    private static final double INPUT_COST_PER_1K = 0.005;
    private static final double OUTPUT_COST_PER_1K = 0.015;

    public record CostReport(long promptTokens, long generationTokens, double totalCostUsd) {}

    public CostReport calculateCost(ChatResponse response) {
        ChatResponse.UsageMetadata usage = response.usage();
        if (usage == null) {
            return new CostReport(0, 0, 0.0);
        }

        long promptTokens = usage.promptTokens();
        long genTokens = usage.generationTokens();

        double inputCost = (promptTokens / 1000.0) * INPUT_COST_PER_1K;
        double outputCost = (genTokens / 1000.0) * OUTPUT_COST_PER_1K;
        double totalCost = inputCost + outputCost;

        return new CostReport(promptTokens, genTokens, Math.round(totalCost * 10000.0) / 10000.0);
    }
}
```
</details>

---

### Exercise 3: Dynamic Persona Switching in `ChatClient`
**Problem Statement:**  
Build a REST endpoint `POST /api/v1/ai/persona-chat` that takes a JSON body containing `userPrompt` and `personaType` (`"EXPLAIN_LIKE_IM_5"`, `"SENIOR_ARCHITECT"`, `"PIRATE"`).  
Using `ChatClient`, dynamically configure the system prompt based on the requested persona.

<details>
<summary>👉 View Solution</summary>

```java
@RestController
@RequestMapping("/api/v1/ai")
public class PersonaChatController {

    private final ChatClient chatClient;

    public PersonaChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public record PersonaRequest(String userPrompt, String personaType) {}

    @PostMapping("/persona-chat")
    public String chatWithPersona(@RequestBody PersonaRequest request) {
        String systemInstruction = switch (request.personaType()) {
            case "EXPLAIN_LIKE_IM_5" -> "Explain this concept as if you are speaking to a five-year-old child. Use simple analogies and toys.";
            case "PIRATE" -> "You are a 17th-century swashbuckling pirate. Answer in authentic nautical pirate slang!";
            case "SENIOR_ARCHITECT" -> "You are a Principal Software Architect. Focus strictly on distributed systems design, latency, and CAP theorem trade-offs.";
            default -> "You are a helpful and polite software engineering assistant.";
        };

        return chatClient.prompt()
            .system(systemInstruction)
            .user(request.userPrompt())
            .call()
            .content();
    }
}
```
</details>

---

## 5-Question Self-Check Quiz

#### 1. Why is Spring AI described as the "JDBC of Artificial Intelligence"?
- A) Because it uses SQL to train neural networks.
- B) Because it provides a single set of standardized Java interfaces (`ChatModel`, `EmbeddingModel`) that allow switching between different LLM providers via configuration without code rewrites.
- C) Because it requires an Oracle database license.
- D) Because it only works with stored procedures.

#### 2. What is the primary operational advantage of using Ollama during early development of a Spring AI application?
- A) Ollama models are 10x smarter than GPT-4o.
- B) It runs models locally on your machine for 100% free with zero cloud API keys and complete data privacy.
- C) Ollama only runs on Kubernetes clusters.
- D) It compiles Python code to Java bytecode.

#### 3. In the Spring AI `Message` hierarchy, which message type is used to define the AI's immutable behavioral rules, tone, and guardrails?
- A) `UserMessage`
- B) `AssistantMessage`
- C) `SystemMessage`
- D) `FunctionMessage`

#### 4. What is the difference between `ChatModel` and `ChatClient` in modern Spring AI?
- A) `ChatModel` is deprecated; only `ChatClient` can make network requests.
- B) `ChatModel` is the low-level provider interface (SPI); `ChatClient` is the fluent, high-level developer API providing builders and prompt specifications.
- C) `ChatClient` is written in Python; `ChatModel` is written in Java.
- D) `ChatModel` only works with local models; `ChatClient` only works with cloud models.

#### 5. How does Spring AI extract token consumption metrics from a generation?
- A) By counting words in the returned string using `String.split(" ")`.
- B) From the `UsageMetadata` object encapsulated inside `ChatResponse.usage()`.
- C) By inspecting HTTP headers on Tomcat.
- D) Tokens cannot be measured in Java.

---

### Quiz Answers & Explanations

1. **B is correct**: Just as JDBC abstracted databases behind `Connection` and `PreparedStatement`, Spring AI abstracts LLMs behind `ChatModel` and `ChatClient`.
2. **B is correct**: Ollama runs quantized models (like Llama 3.2) locally on CPU/GPU without cloud costs or outbound network calls.
3. **C is correct**: `SystemMessage` corresponds to the `system` role in LLMs, dictating behavior and safety guardrails.
4. **B is correct**: `ChatModel` is the low-level client interface implemented by each provider; `ChatClient` is the ergonomic, fluent API that developers use in their application code.
5. **B is correct**: Every provider response maps token metrics into the unified `UsageMetadata` record inside `ChatResponse`.

---

## Day 32 Summary & Next Steps

Today you launched **Phase 6: Spring AI**:
1. **The Architecture of Spring AI**: Understanding the portable abstraction model that frees enterprise Java teams from vendor lock-in.
2. **Ollama Local Engine**: Setting up a zero-cost, private AI environment running Llama 3.2.
3. **Core Building Blocks**: Mastering `ChatModel`, `Prompt`, `SystemMessage`, `UserMessage`, `AssistantMessage`, and `ChatResponse`.
4. **The Modern `ChatClient`**: Using the fluent builder API to send prompts and receive responses.
5. **Token Telemetry**: Inspecting `UsageMetadata` to meter prompt and completion tokens.

👉 **Tomorrow in Day 33: ChatClient — The Fluent Conversational API** — You will deep dive into `ChatClient`, mastering default system advice, dynamic advisors, parameter substitution, response converters, and building a full interactive conversational assistant in Java 21!
