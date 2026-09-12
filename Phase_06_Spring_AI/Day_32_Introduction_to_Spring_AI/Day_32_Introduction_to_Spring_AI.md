# Day 32: Introduction to Spring AI — The Big Picture
## Architecture, Portable Model Abstractions, Local Ollama Setup & First ChatClient Call

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [◀ Day 31: Rate Limiting, CORS & API Security](../../Phase_05_Spring_Security/Day_31_Rate_Limiting_CORS_API_Security/Day_31_Rate_Limiting_CORS_API_Security.md) | [All 60 Days Overview](../../README.md) | [Day 33: ChatClient — The Fluent Conversational API ▶](../Day_33_ChatClient_Fluent_Conversational_API/Day_33_ChatClient_Fluent_Conversational_API.md) |

---

## 1. Topic Overview

Spring AI provides a unified, portable abstraction layer across diverse Generative AI model providers (OpenAI, Anthropic, Google Gemini, Ollama) using an interface-driven architecture analogous to JDBC for relational databases. In enterprise systems, it enables developers to build secure, type-safe, and vendor-agnostic AI applications in Java 21, swapping between proprietary cloud models and self-hosted open-weight LLMs via configuration without rewriting application business logic.

---

## 2. Basic Foundations (True Zero)

### Essential AI Concepts Demystified
1. **Large Language Model (LLM)**: A neural network trained on vast text corpora to predict the most statistically probable next words in a sequence given an input context.
2. **Prompt**: The textual instructions, constraints, and question provided to the model.
3. **Token**: The atomic unit of text comprehension in LLMs. One token corresponds to approximately 3 to 4 characters (or ~0.75 English words). Providers meter and bill API requests in units of 1,000 tokens.
4. **Ollama**: An open-source tool that executes quantized open-weight models (e.g., Meta's Llama 3.2, Mistral, Phi-3) locally on consumer CPUs or GPUs via a local REST server (`localhost:11434`).
5. **ChatModel vs. ChatClient**:
   - `ChatModel`: The low-level Service Provider Interface (SPI) handling direct network serialization with specific vendor APIs.
   - `ChatClient`: The high-level, fluent developer API used in daily application code to compose prompts, inject system personas, and parse outputs.

```
+-----------------------------------------------------------------------------------+
|               THE JDBC OF ARTIFICIAL INTELLIGENCE ANALOGY                         |
|                                                                                   |
|  BEFORE JDBC (1995): Vendor Lock-In Chaos                                         |
|  - Oracle used proprietary C function calls (`ora_connect`, `ora_exec`).          |
|  - MySQL used completely different functions (`mysql_real_query`).                |
|  - Switching databases meant rewriting thousands of lines of SQL access code!     |
|                                                                                   |
|  THE JDBC REVOLUTION: One Interface, Infinite Drivers                             |
|  - Java introduced `Connection` and `PreparedStatement`.                          |
|  - Applications speak exclusively to standard Java interfaces.                    |
|  - Swapping Oracle for PostgreSQL requires changing one line in `application.yml`!|
|                                                                                   |
|  SPRING AI (2024+): Universal Abstraction for Generative AI                       |
|  - OpenAI, Anthropic, Gemini, and Ollama all have different JSON schemas.         |
|  - Spring AI introduces `ChatModel`, `Prompt`, `ChatResponse`, and `ChatClient`.  |
|  - Your code talks to `ChatClient`. Swapping OpenAI for a private on-premise      |
|    Llama 3.2 model requires zero Java code changes!                               |
+-----------------------------------------------------------------------------------+
```

### Minimal Beginner-Friendly Working Code Example

Below is a pure Java 21 simulation demonstrating the decoupled pipeline connecting `Prompt`, `ChatModel`, and `ChatResponse`:

```java
import java.util.*;

public class BasicSpringAiSimulation {

    // 1. Immutable Message Contract
    public record ChatMessage(String role, String content) {}

    // 2. Telemetry Record
    public record UsageMetrics(long promptTokens, long completionTokens) {}

    // 3. Response Contract
    public record AiResponse(String text, UsageMetrics usage) {}

    // 4. Portability Interface (The "JDBC" of AI)
    public interface SimpleChatModel {
        AiResponse generate(List<ChatMessage> conversation);
        String getProvider();
    }

    // 5. Local Ollama Implementation
    static class LocalOllamaModel implements SimpleChatModel {
        public String getProvider() { return "Ollama (Llama 3.2 Local)"; }
        public AiResponse generate(List<ChatMessage> conversation) {
            String lastUserMsg = conversation.getLast().content();
            return new AiResponse(
                "[Llama 3.2 Offline] Enterprise response to: '" + lastUserMsg + "'",
                new UsageMetrics(12, 24)
            );
        }
    }

    public static void main(String[] args) {
        SimpleChatModel model = new LocalOllamaModel();
        System.out.println("Active AI Model: " + model.getProvider());

        List<ChatMessage> messages = List.of(
            new ChatMessage("system", "You are an enterprise Java architect."),
            new ChatMessage("user", "Explain how Virtual Threads benefit AI streaming.")
        );

        AiResponse response = model.generate(messages);
        System.out.println("AI Output: " + response.text());
        System.out.printf("Tokens Used: Prompt=%d, Completion=%d, Total=%d%n",
            response.usage().promptTokens(),
            response.usage().completionTokens(),
            response.usage().promptTokens() + response.usage().completionTokens());
    }
}
```

#### Line-by-Line Walkthrough:
- **Lines 6–10**: Defines immutable records mirroring Spring AI's domain objects: `ChatMessage`, `UsageMetrics`, and `AiResponse`.
- **Lines 13–16**: `SimpleChatModel` defines the vendor-neutral contract. The consumer code does not know whether the model is OpenAI, Anthropic, or Ollama.
- **Lines 19–27**: `LocalOllamaModel` implements the contract, simulating a local inference run and tracking token metrics.
- **Lines 30–42**: The client creates system and user messages, invokes the model through the generic interface, and extracts both text and token accounting metrics.

---

## 3. Core Concept Walkthrough (Basic → Intermediate)

### Why Java is Dominating Enterprise AI Production

While initial prototyping often occurs in Python notebooks, enterprise production systems are built on Java 21:

```
+-----------------------------------+---------------------------------------------------+
| Python AI Prototyping             | Java 21 Enterprise AI Production                  |
+-----------------------------------+---------------------------------------------------+
| - Global Interpreter Lock (GIL)   | - Virtual Threads handle 100,000 concurrent       |
|   blocks multi-threaded I/O.      |   streaming LLM connections with minimal RAM.     |
| - Dynamic typing causes runtime   | - Strict compile-time typing, Records, and sealed |
|   KeyError/TypeError crashes.     |   types eliminate JSON parsing hallucination bugs.|
| - Disconnected from core banking, | - Native integration with existing enterprise     |
|   ERP, and database systems.      |   Spring Boot, PostgreSQL, and Kafka pipelines.   |
| - Custom security glue often fails| - Military-grade Spring Security, OAuth2, RBAC,   |
|   enterprise SOC2/HIPAA audits.   |   and immutable audit trails out of the box.      |
+-----------------------------------+---------------------------------------------------+
```

---

### Spring AI Architectural Hierarchy

Spring AI separates developer-facing APIs from low-level network drivers:

```
                                SPRING AI ARCHITECTURE OVERVIEW
                                
                                +-----------------------------+
                                |         ChatClient          |  (High-level Fluent API)
                                +--------------+--------------+
                                               | delegates to
                                               v
                                +-----------------------------+
                                |          ChatModel          |  (Core Low-level SPI)
                                +--------------+--------------+
                                               |
                 +-----------------------------+-----------------------------+
                 v                                                           v
       +----------------------+                                   +----------------------+
       |   OllamaChatModel    |                                   |   OpenAiChatModel    |
       |  (Llama 3.2 / Local) |                                   |    (GPT-4o / Cloud)  |
       +----------+-----------+                                   +----------+-----------+
                  |                                                          |
                  v                                                          v
           Local GPU/CPU                                              OpenAI Cloud API
           (Port 11434)                                               (https://api.openai.com)
```

---

### Understanding the Message Roles

LLMs process conversations as an ordered sequence of role-tagged messages:

```
+--------------------+--------------+-------------------------------------------------------------------+
| Message Type       | Role Tag     | Plain-English Purpose & Example                                   |
+--------------------+--------------+-------------------------------------------------------------------+
| `SystemMessage`    | `system`     | Defines the persona, behavioral rules, and security boundaries.   |
|                    |              | *"You are a legal assistant. Never provide tax advice."*          |
+--------------------+--------------+-------------------------------------------------------------------+
| `UserMessage`      | `user`       | The actual question or prompt submitted by the human client.      |
|                    |              | *"Summarize Section 4 of this employment contract."*               |
+--------------------+--------------+-------------------------------------------------------------------+
| `AssistantMessage` | `assistant`  | The response previously generated by the model. Passed back in    |
|                    |              | multi-turn conversations to provide historical memory context.     |
+--------------------+--------------+-------------------------------------------------------------------+
```

---

### Setting Up Free Local Ollama

1. **Install Ollama**: Download from [ollama.com](https://ollama.com) or run via Docker:
   ```bash
   docker run -d -v ollama_data:/root/.ollama -p 11434:11434 --name ollama ollama/ollama:latest
   ```
2. **Download Model Weights**:
   ```bash
   ollama pull llama3.2
   ```
3. **Verify Local API**:
   ```bash
   curl http://localhost:11434/api/generate -d '{"model": "llama3.2", "prompt": "Hello", "stream": false}'
   ```

---

### Spring AI Maven Setup & Configuration

#### 1. Add BOM (Bill of Materials)
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

#### 2. Add Starter Dependencies
```xml
<!-- For Free Local Development -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-ollama-spring-boot-starter</artifactId>
</dependency>

<!-- For Cloud Production Deployment -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>
```

#### 3. Configure `application.yml`
```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: llama3.2
          temperature: 0.7
    openai:
      api-key: ${OPENAI_API_KEY:dummy-key}
      chat:
        options:
          model: gpt-4o
          temperature: 0.5
```

---

### Building Your First Spring AI Controller

```java
package com.example.genai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
public class FirstAiController {

    private final ChatClient chatClient;

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

---

## 4. Prerequisite & Supporting Concepts

### Prerequisite / Supporting Concept: Virtual Threads and Streaming Concurrency
LLM inference queries are high-latency I/O operations (taking 1 to 30 seconds for complex generations). On traditional platform thread pools (Tomcat's default 200 worker threads), 200 concurrent chat requests would exhaust the entire thread pool, resulting in server-wide HTTP 503 outages.

Java 21 Virtual Threads (`spring.threads.virtual.enabled=true`) decouple active HTTP requests from OS carrier threads. A single JVM can comfortably support 50,000 concurrent streaming LLM connections with minimal memory footprint.

### Prerequisite / Supporting Concept: Model Hyperparameters
- **`temperature` (0.0 – 2.0)**: Controls generation randomness. A temperature of `0.0` produces deterministic, reproducible code and factual responses. A temperature of `0.8+` increases creative variability.
- **`top_p` (0.0 – 1.0)**: Nucleus sampling alternative to temperature. Dynamically cuts off the tail of low-probability vocabulary words.

---

## 5. Advanced Depth (Intermediate → Advanced)

### Multi-Provider Fallback Architecture

In high-availability enterprise environments, relying on a single cloud LLM introduces an external single point of failure (SPOF). Spring AI allows you to register multiple `ChatModel` beans and build resilient failover chains:

```java
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
                    + ") failed: " + ex.getMessage() + ". Failing over to secondary provider...");
            return fallbackModel.call(prompt);
        }
    }
}
```

---

### Hands-On Simulation Code Walkthrough

The companion code repository demonstrates this architecture:
- `Message.java`: Complete message hierarchy implementing `SystemMessage`, `UserMessage`, and `AssistantMessage`.
- `Prompt.java` & `ChatOptions.java`: Encapsulates conversation history and runtime hyperparameters.
- `ChatModel.java`: Low-level SPI implemented by both `OllamaChatModel` and `OpenAiChatModel`.
- `ChatClient.java`: Fluent API wrapper providing fluent prompts and telemetry extraction.
- `SpringAiDemo.java`: 4-scenario test suite validating local Ollama, cloud OpenAI, system personas, and token usage accounting.

```powershell
# Compile Day 32 code
javac -d out Phase_06_Spring_AI/Day_32_Introduction_to_Spring_AI/code/*.java

# Run SpringAiDemo
java -cp out com.genai.springai.core.SpringAiDemo
```

#### Verified Execution Output:
```
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

## 6. Quick Recap

| Concept | Description | Enterprise Rule / Best Practice |
| :--- | :--- | :--- |
| **Spring AI** | Portable AI framework for Spring Boot | Eliminates vendor lock-in across OpenAI, Anthropic, Ollama. |
| **`ChatClient`** | Fluent developer API | Preferred entry point for application business logic. |
| **`ChatModel`** | Low-level provider interface (SPI) | Implemented by specific provider drivers. |
| **Ollama** | Local open-weight LLM execution engine | Free, secure, offline development without cloud costs. |
| **`SystemMessage`**| Guiding persona and safety rules | Immutable boundaries unchangeable by normal user input. |
| **`UserMessage`** | Client question or prompt | Processed by LLM within system guardrail boundaries. |
| **`UsageMetadata`**| Token accounting in `ChatResponse` | Essential for tracking billing and quota enforcement. |

---

## 7. Self-Check Questions & Practice Exercises

### Conceptual & Architectural Questions

#### Q1: Why is Spring AI described as the "JDBC of Artificial Intelligence"?
**Answer**: Just as JDBC abstracted database vendors behind unified Java interfaces (`Connection`, `PreparedStatement`), Spring AI abstracts diverse LLM vendors behind unified interfaces (`ChatModel`, `EmbeddingModel`, `VectorStore`). Developers write code against `ChatClient`, allowing them to swap providers via configuration without changing Java source code.

#### Q2: What is the primary operational advantage of using Ollama during early development of a Spring AI application?
**Answer**: Ollama executes open-weight models (Llama 3.2, Mistral) locally on developer laptops. It incurs zero cloud API billing costs, requires no external API keys, operates entirely offline, and eliminates data privacy risks during development and unit testing.

#### Q3: In the Spring AI `Message` hierarchy, which message type is used to define the AI's immutable behavioral rules, tone, and guardrails?
**Answer**: `SystemMessage` (corresponding to the `system` role in LLMs), which sets operating parameters that the user cannot directly override.

#### Q4: What is the difference between `ChatModel` and `ChatClient` in modern Spring AI?
**Answer**: `ChatModel` is the low-level provider SPI directly communicating with vendor HTTP APIs. `ChatClient` is the ergonomic, fluent developer API that wraps `ChatModel`, providing builders, default system prompts, advisors, and structured output parsing.

#### Q5: How does Spring AI extract token consumption metrics from a generation?
**Answer**: From the `UsageMetadata` object encapsulated inside `ChatResponse.getMetadata().getUsage()`, which reports input prompt tokens, output generation tokens, and total token count.

---

### Hands-On Practice Exercises

#### Exercise 1: Multi-Provider Fallback Service
**Task**: Implement a Spring service `ResilientAiService` that injects two `ChatModel` beans (`ollamaChatModel` and `openAiChatModel`), attempting Ollama first and falling back to OpenAI if an exception occurs.

```java
// Solution:
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
            System.err.println("Primary model failed: " + ex.getMessage() + ". Failing over to secondary...");
            return fallbackModel.call(prompt);
        }
    }
}
```

#### Exercise 2: Token Telemetry & Cost Calculator
**Task**: Build a component `TokenCostTracker` that calculates the financial cost of a `ChatResponse` assuming $0.005 per 1,000 prompt tokens and $0.015 per 1,000 generation tokens.

```java
// Solution:
@Component
public class TokenCostTracker {

    private static final double INPUT_COST_PER_1K = 0.005;
    private static final double OUTPUT_COST_PER_1K = 0.015;

    public record CostReport(long promptTokens, long generationTokens, double totalCostUsd) {}

    public CostReport calculateCost(ChatResponse response) {
        var usage = response.getMetadata().getUsage();
        if (usage == null) return new CostReport(0, 0, 0.0);

        long promptTokens = usage.getPromptTokens();
        long genTokens = usage.getGenerationTokens();

        double inputCost = (promptTokens / 1000.0) * INPUT_COST_PER_1K;
        double outputCost = (genTokens / 1000.0) * OUTPUT_COST_PER_1K;
        double totalCost = inputCost + outputCost;

        return new CostReport(promptTokens, genTokens, Math.round(totalCost * 10000.0) / 10000.0);
    }
}
```

#### Exercise 3: Dynamic Persona Switching in `ChatClient`
**Task**: Build an endpoint `POST /api/v1/ai/persona-chat` that takes a `userPrompt` and `personaType` (`"EXPLAIN_LIKE_IM_5"`, `"SENIOR_ARCHITECT"`, `"PIRATE"`) and configures dynamic system directives using `ChatClient`.

```java
// Solution:
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
            case "EXPLAIN_LIKE_IM_5" -> "Explain this concept as if you are speaking to a five-year-old child. Use simple analogies.";
            case "PIRATE" -> "You are a 17th-century pirate. Answer in authentic nautical pirate slang!";
            case "SENIOR_ARCHITECT" -> "You are a Principal Software Architect. Focus on distributed systems and latency trade-offs.";
            default -> "You are a helpful software engineering assistant.";
        };

        return chatClient.prompt()
            .system(systemInstruction)
            .user(request.userPrompt())
            .call()
            .content();
    }
}
```

---

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [◀ Day 31: Rate Limiting, CORS & API Security](../../Phase_05_Spring_Security/Day_31_Rate_Limiting_CORS_API_Security/Day_31_Rate_Limiting_CORS_API_Security.md) | [All 60 Days Overview](../../README.md) | [Day 33: ChatClient — The Fluent Conversational API ▶](../Day_33_ChatClient_Fluent_Conversational_API/Day_33_ChatClient_Fluent_Conversational_API.md) |
