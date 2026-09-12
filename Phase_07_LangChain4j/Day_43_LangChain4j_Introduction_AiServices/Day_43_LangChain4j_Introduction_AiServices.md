# Day 43: LangChain4j Introduction & AiServices

[← Previous: Day 42 - Multimodal AI](../../Phase_06_Spring_AI/Day_42_Multimodal_AI_Vision_Audio_Images/Day_42_Multimodal_AI_Vision_Audio_Images.md) | [Next: Day 44 - Memory & Conversation Management →](../Day_44_Memory_Conversation_Management/Day_44_Memory_Conversation_Management.md)

---

## 1. Topic Overview
LangChain4j is a lightweight, framework-agnostic Java library designed to bring idiomatic, type-safe Generative AI and LLM orchestration to standard Java SE, Spring Boot, Quarkus, and Micronaut applications. Its premier architectural pattern—`AiServices`—allows engineers to build intelligent conversational agents declaratively by defining plain Java interfaces decorated with prompt annotations, while the framework dynamically synthesizes model interactions, token accounting, and return-type deserialization.

---

## 2. Basic Foundations (True Zero)

### What is LangChain4j and `AiServices`?
In traditional Java AI programming, calling an LLM requires tedious boilerplate:
- Building strings with manual concatenation.
- Managing low-level JSON payloads.
- Parsing text outputs using Jackson `ObjectMapper`.
- Catching network exceptions and managing message history arrays.

This is the AI equivalent of writing raw JDBC queries in 2002!

LangChain4j introduces **`AiServices`**, which acts like **Spring Data JPA or OpenFeign for Large Language Models**:
- You write a clean, standard Java interface: `public interface SupportAgent { ... }`.
- You add prompt annotations like `@SystemMessage("You are a customer support agent")` and `@UserMessage("Review order {{orderId}}")`.
- LangChain4j generates a working implementation at runtime using standard Java Dynamic Proxies (`java.lang.reflect.Proxy`).
- You invoke plain Java methods with strongly-typed arguments, and receive strongly-typed Java records, enums, or strings back.

### Relatable Physical Analogy: The Universal Translator Headset
Imagine hiring an international business interpreter:
- **Imperative Way (Raw JDBC Style)**: Before every sentence, you walk over to the interpreter, explain the grammar rules, hand them an Italian-to-English dictionary, verify their credentials, format your sentence on an index card, wait for their spoken words, and transcribe their phonetic sounds into an English notebook.
- **Declarative Way (`AiServices` Style)**: You put on a lightweight universal translator headset. You speak your native language into your normal microphone, and the headset seamlessly translates, checks business terminology, and delivers the exact desired words to your overseas partner without any manual coordination.

### Minimal Beginner-Friendly Working Code
Here is how simple it is to declare and invoke an AI service in LangChain4j:

```java
package com.genai.langchain4j.aiservices;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public class SimpleAiServiceRunner {

    // 1. Declare the AI behavior as a plain Java interface
    public interface Assistant {
        @SystemMessage("You are an expert technical writer. Provide concise, clear explanations.")
        @UserMessage("Explain {{concept}} in two simple sentences for a junior developer.")
        String explainConcept(@V("concept") String concept);
    }

    public static void main(String[] args) {
        // 2. Build the underlying model client
        ChatLanguageModel model = OpenAiChatModel.builder()
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .modelName("gpt-4o")
            .build();

        // 3. Let LangChain4j generate the interface implementation via Dynamic Proxy
        Assistant assistant = AiServices.create(Assistant.class, model);

        // 4. Call your interface like any normal Java service!
        String explanation = assistant.explainConcept("Virtual Threads");
        System.out.println("AI Explanation:\n" + explanation);
    }
}
```

### Line-by-Line Walkthrough
1. **`public interface Assistant`**: A pure Java interface without any concrete implementation classes in your codebase.
2. **`@SystemMessage(...)`**: Defines the system directive setting the assistant's persona and constraints.
3. **`@UserMessage("Explain {{concept}}...")`**: A templated prompt. The `{{concept}}` token acts as a placeholder.
4. **`@V("concept") String concept`**: Binds the runtime method argument to the matching template placeholder.
5. **`AiServices.create(Assistant.class, model)`**: Inspects the interface annotations, constructs an `InvocationHandler`, and instantiates a dynamic proxy implementing `Assistant`.
6. **`assistant.explainConcept("Virtual Threads")`**: Intercepts the method invocation, interpolates variables, executes the remote model call, and returns the response string directly.

---

## 3. Core Concept Walkthrough (Basic → Intermediate)

```
+-------------------------------------------------------------------------------+
|                       HOW AISERVICES EXECUTES UNDER THE HOOD                  |
+-------------------------------------------------------------------------------+
|                                                                               |
|  Java Application Code calls:                                                 |
|  assistant.explainConcept("Virtual Threads");                                 |
|         |                                                                     |
|         v                                                                     |
|  [ java.lang.reflect.Proxy (Dynamic InvocationHandler) ]                      |
|  Intercepts method call; inspects @SystemMessage and @UserMessage             |
|         |                                                                     |
|         v                                                                     |
|  [ Prompt Template Interpolator ]                                             |
|  Binds @V("concept") = "Virtual Threads" into template:                       |
|  "Explain Virtual Threads in two simple sentences for a junior developer."     |
|         |                                                                     |
|         v                                                                     |
|  [ ChatLanguageModel.generate() ]                                             |
|  Dispatches messages to LLM provider (OpenAI, Anthropic, Ollama)              |
|         |                                                                     |
|         v                                                                     |
|  [ Return Type Unmarshaller ]                                                 |
|  Inspects method return type: String, Enum, Record, Boolean, or List<T>       |
|         |                                                                     |
|         v                                                                     |
|  Returns strongly-typed Java object back to caller!                           |
+-------------------------------------------------------------------------------+
```

### Core Model Abstractions: `ChatLanguageModel` and `ChatMessage`
LangChain4j structures all conversational communication around clean interfaces:
- **`ChatLanguageModel`**: The root interface for all text-generation models.
- **`ChatMessage` Hierarchy**:
  - `SystemMessage`: Persona instructions and boundaries.
  - `UserMessage`: The human query (with optional multimodal `ImageContent`).
  - `AiMessage`: The model's response (containing text or `ToolExecutionRequest` objects).
  - `ToolExecutionResultMessage`: Observations returned from executing local Java tools.

### Automatic Return-Type Conversions
`AiServices` is not limited to returning plain text. It automatically translates LLM outputs into strongly-typed Java primitives, enums, and records.

#### 1. Direct Conversion to Java Enums
When a method returns an `enum`, LangChain4j instructs the model to select exclusively from the declared enum values and deserializes the response via `Enum.valueOf()`:

```java
package com.genai.langchain4j.aiservices;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface SentimentClassifier {

    enum Sentiment { POSITIVE, NEGATIVE, NEUTRAL }

    @SystemMessage("You are an automated sentiment analyzer. Classify the customer review.")
    @UserMessage("Analyze this review: {{reviewText}}")
    Sentiment classify(@V("reviewText") String reviewText);
}
```

#### 2. Direct Conversion to Java 21 Records
When returning a custom `record`, LangChain4j generates a JSON Schema prompt, instructs the model to return raw JSON, and uses Jackson to map it into an instance:

```java
package com.genai.langchain4j.aiservices;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.List;

public interface ResumeParser {

    record CandidateProfile(
        String fullName,
        String email,
        int yearsOfExperience,
        List<String> skills,
        boolean openToRelocation
    ) {}

    @SystemMessage("Extract structured candidate profile information from the supplied text.")
    @UserMessage("Resume text: {{resume}}")
    CandidateProfile parseResume(@V("resume") String resume);
}
```

---

## 4. Prerequisite & Supporting Concepts

### Prerequisite / Supporting Concept: Java Dynamic Proxies (`java.lang.reflect.Proxy`)
How can Java execute an interface that has no implementation class?
- The Java Virtual Machine has built-in support for **Dynamic Proxies**.
- When you invoke `Proxy.newProxyInstance(loader, new Class<?>[]{MyInterface.class}, invocationHandler)`, the JVM generates bytecode in memory that implements `MyInterface`.
- Every method call on that proxy is routed to a single method: `invocationHandler.invoke(Object proxy, Method method, Object[] args)`.
- This is the identical mechanism that powers Spring Data JPA repositories, Spring `@Transactional` aspects, and Retrofit/Feign HTTP clients!

### Prerequisite / Supporting Concept: Token Telemetry with `Response<T>`
If your enterprise application requires auditing API usage costs and token consumption, wrap your interface return type in `Response<T>`:

```java
import dev.langchain4j.model.output.Response;

public interface AuditedAssistant {
    @UserMessage("Draft an announcement: {{topic}}")
    Response<String> draftAnnouncement(@V("topic") String topic);
}
```

When called:
```java
Response<String> response = assistant.draftAnnouncement("New Data Center Opening");
String text = response.content();
int inputTokens = response.tokenUsage().inputTokenCount();
int outputTokens = response.tokenUsage().outputTokenCount();
System.out.println("Tokens used: " + response.tokenUsage().totalTokenCount());
```

---

## 5. Advanced Depth (Intermediate → Advanced)

### Architectural Comparison: LangChain4j vs. Spring AI

| Feature | Spring AI | LangChain4j |
|:---|:---|:---|
| **Primary Philosophy** | Native alignment with Spring Boot conventions (`application.yml`, starters). | Framework-agnostic (runs on plain Java SE, Spring Boot, Quarkus, Micronaut, CLI). |
| **API Style** | Fluent Builder API (`ChatClient.prompt().user(...).call().content()`). | Declarative Interface API (`AiServices.create(MyInterface.class, model)`). |
| **Model Ecosystem** | OpenAI, Azure, Bedrock, Ollama, Vertex AI, Anthropic, Mistral. | Broad community support: OpenAI, Azure, Anthropic, Gemini, Ollama, HuggingFace, Cohere, Jina, etc. |
| **Conversational Memory** | `ChatMemory` repository abstraction. | Rich built-in policies: `MessageWindowChatMemory`, `TokenWindowChatMemory`, custom SPI. |
| **Testing Ergonomics** | Mocking `ChatClient` builder chains requires mock setup. | Pure Java interface; trivial to mock with Mockito in standard JUnit tests. |

### Effortless Unit Testing with Mockito
Because `AiServices` uses standard Java interfaces, you can test business services without making live LLM network calls:

```java
package com.genai.langchain4j.aiservices;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class BusinessServiceTest {

    @Test
    void shouldProcessSentimentCorrectly() {
        // Mock the declarative interface directly!
        SentimentClassifier mockClassifier = mock(SentimentClassifier.class);
        when(mockClassifier.classify("Great product!"))
            .thenReturn(SentimentClassifier.Sentiment.POSITIVE);

        // Execute business logic with zero API token spend
        SentimentClassifier.Sentiment result = mockClassifier.classify("Great product!");
        assertEquals(SentimentClassifier.Sentiment.POSITIVE, result);
    }
}
```

### Common Anti-Patterns & Production Traps

| Anti-Pattern | Why It Breaks in Production | Correct Architectural Solution |
|:---|:---|:---|
| **Hardcoding API Keys in Source Code** | Leaks credentials into version control systems. | Read keys from environment variables: `System.getenv("OPENAI_API_KEY")` or Spring secret vaults. |
| **Re-creating `AiServices` Proxies on Every Request** | Dynamic proxy creation and annotation reflection on every HTTP request adds unnecessary CPU overhead. | Instantiate `AiServices` proxies as singleton Spring beans and reuse them across requests. |
| **Ignoring Token Limits in Return Types** | Asking an LLM to parse a 20-page document into a complex record can exceed output token limits, producing truncated, malformed JSON. | Use token-aware chunking or summarize long documents before passing them to structured extraction services. |

---

## 6. Quick Recap
- **LangChain4j** provides a lightweight, framework-agnostic Java library for building Generative AI and agentic applications.
- **`AiServices`** enables declarative AI engineering: you define a plain Java interface with `@SystemMessage`, `@UserMessage`, and `@V`, and the framework generates the implementation at runtime.
- Methods can return typed **enums**, **records**, **primitives**, or **`Response<T>`** with automatic JSON schema generation and parsing.
- Under the hood, `AiServices` relies on standard Java **Dynamic Proxies (`java.lang.reflect.Proxy`)** to intercept invocations, compile prompt templates, and dispatch model calls.
- Pure Java interfaces make unit testing with **Mockito** fast, deterministic, and free of API token expenses.

---

## 7. Self-Check Questions & Practice Exercises

### 5-Question Self-Check Quiz

#### Question 1
What is the primary conceptual advantage of LangChain4j's `AiServices`?
- A) It accelerates GPU matrix multiplication on OpenAI's remote infrastructure.
- B) It allows developers to define AI behavior declaratively using plain Java interfaces and annotations, eliminating imperative prompt-building boilerplate.
- C) It replaces the Java Virtual Machine with a Python runtime.
- D) It bypasses commercial API billing mechanisms.

#### Question 2
In LangChain4j, which annotation binds a method parameter to a `{{placeholder}}` in `@UserMessage`?
- A) `@PathVariable`
- B) `@Param`
- C) `@V`
- D) `@Value`

#### Question 3
How does `AiServices` handle interface methods that return a Java `enum`?
- A) It throws an `UnsupportedOperationException`.
- B) It prompts the model with the allowed enum constants and converts the model's text response directly into the matching enum value via `Enum.valueOf()`.
- C) It only works if the method returns `String`.
- D) It compiles a new `.java` file to disk at runtime.

#### Question 4
How is `AiServices` implemented under the hood in pure Java?
- A) Through bytecode manipulation using CGLIB.
- B) Using standard Java Dynamic Proxies (`java.lang.reflect.Proxy`) and reflection to intercept method calls at runtime.
- C) By invoking external Python scripts.
- D) By compiling C++ native libraries via JNI.

#### Question 5
Why is an interface-based AI design advantageous for enterprise unit testing?
- A) Interfaces do not require unit testing.
- B) It allows developers to mock the AI service using standard tools like Mockito, enabling fast and free unit testing without sending network requests to an LLM provider.
- C) It forces JUnit tests to run on virtual threads.
- D) It encrypts unit test results.

---

### Quiz Answers & Explanations
1. **B**: `AiServices` operates like Spring Data repositories: you define an interface with annotations, and the framework generates the proxy implementation that manages prompt templates, parameter binding, model invocation, and return-type conversion.
2. **C**: LangChain4j uses the `@V("variableName")` annotation to link method arguments to matching `{{variableName}}` placeholders within `@UserMessage` templates.
3. **B**: LangChain4j inspects the enum return type, supplies the list of valid enum constants to the model, and parses the response directly into the typed enum constant.
4. **B**: LangChain4j uses `Proxy.newProxyInstance()` to construct an `InvocationHandler` that intercepts calls to your interface methods, resolves annotations, builds prompts, calls the model, and formats the output.
5. **B**: Because your application depends on a plain Java interface rather than concrete LLM client code, you can easily mock the interface in test suites, simulating AI responses without latency, network dependencies, or API costs.

---

### Hands-On Practice Exercises

#### Exercise 1: Declarative Language Translator Service
**Problem Statement**:  
Define a declarative LangChain4j interface `LanguageTranslator` with a method `translate(@V("text") String text, @V("targetLanguage") String lang)`. Decorate it with `@SystemMessage` instructing the model to act as a professional linguist, and `@UserMessage` with variable placeholders.

<details>
<summary>👉 View Solution</summary>

```java
package com.genai.langchain4j.exercises;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface LanguageTranslator {

    @SystemMessage("You are an expert diplomatic translator. Preserve cultural nuances and technical terminology.")
    @UserMessage("Translate the following text into {{targetLanguage}}: {{text}}")
    String translate(@V("text") String text, @V("targetLanguage") String targetLanguage);
}
```
</details>

#### Exercise 2: Boolean Verification Service
**Problem Statement**:  
Define an interface `SecurityPolicyAuditor` that returns a `boolean` indicating whether a given pull request commit message adheres to Conventional Commits standards.

<details>
<summary>👉 View Solution</summary>

```java
package com.genai.langchain4j.exercises;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface SecurityPolicyAuditor {

    @SystemMessage("""
        You are a strict Git CI/CD compliance bot.
        Evaluate if the commit message conforms to Conventional Commits (e.g. feat:, fix:, chore:, docs:).
        Respond strictly with true or false.
        """)
    @UserMessage("Commit message: {{commitMsg}}")
    boolean isCompliant(@V("commitMsg") String commitMessage);
}
```
</details>

---

[← Previous: Day 42 - Multimodal AI](../../Phase_06_Spring_AI/Day_42_Multimodal_AI_Vision_Audio_Images/Day_42_Multimodal_AI_Vision_Audio_Images.md) | [Next: Day 44 - Memory & Conversation Management →](../Day_44_Memory_Conversation_Management/Day_44_Memory_Conversation_Management.md)
