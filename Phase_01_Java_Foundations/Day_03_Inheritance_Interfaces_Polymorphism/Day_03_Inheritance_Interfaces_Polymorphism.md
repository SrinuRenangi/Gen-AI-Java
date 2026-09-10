# 🧩 Day 03: Inheritance, Interfaces & Polymorphism
## The Contract System That Powers All of Spring & Spring AI

| ⬅️ Previous Day | 📚 Course Hub | ➡️ Next Day |
|:---|:---:|---:|
| [← Day 02: OOP — Classes, Objects & Memory](../Day_02_OOP_Classes_Objects_Memory/Day_02_OOP_Classes_Objects_Memory.md) | [All 60 Days Overview](../../README.md) | [Day 04: Generics, Collections & Data Structures →](../Day_04_Generics_Collections_DataStructures/Day_04_Generics_Collections_DataStructures.md) |

[![Phase](https://img.shields.io/badge/Phase_01-Java_Foundations-brightgreen.svg?style=for-the-badge)](../../README.md)
[![Day](https://img.shields.io/badge/Day-03_of_60-blue.svg?style=for-the-badge)](../../README.md)
[![Difficulty](https://img.shields.io/badge/Difficulty-Intermediate-blue.svg?style=for-the-badge)](../../README.md)
[![Design Pattern](https://img.shields.io/badge/Core-Polymorphism_%26_Contracts-purple.svg?style=for-the-badge)](../../README.md)

---

## 📌 What Will You Learn Today?

Welcome to Day 03, my friend! Today we're exploring one of the most liberating and superpower-granting ideas in software engineering: **Polymorphism and Interface-Driven Design**.

Don't let the 12-letter Greek word scare you. "Polymorphism" just means *"many forms"* — the ability for different objects to respond to the same command in their own unique way. 

Think about your TV remote: it has a `Power` button. You press `Power`, and the TV turns on. You don't care whether the TV's internal circuit board was made by Sony, LG, or Samsung. As long as it responds to the `Power` button, you're happy!

In modern AI engineering with Spring AI, you almost **never** tie your code directly to one company like `OpenAiClient`. Why? What if OpenAI has an outage? Or what if your boss says, *"OpenAI is getting too expensive, let's switch to a free local AI model running on our own servers"*? 
With interfaces and polymorphism, you can swap out OpenAI for a local AI model **without changing a single line of your business code**!

By the end of today, you will clearly understand:
- ✅ **The Power of Polymorphism**: How writing code against *contracts* instead of *vendors* makes your apps future-proof.
- ✅ **Inheritance (`extends`)**: Reusing code with parent and child classes, using `super`, and knowing when to stop ("Composition over Inheritance").
- ✅ **Interfaces (`implements`)**: The universal plug (like a USB-C port) of Java software architecture.
- ✅ **The Diamond Problem**: Why Java lets you implement multiple interfaces, but only inherit from one class.
- ✅ **Abstract Classes vs. Interfaces**: When you want a partial template vs. a 100% pure capability contract.
- ✅ **Modern Java Interface Features**: `default` methods (adding new features safely) and `static` utility methods.
- ✅ **Pattern Matching with `instanceof` (Java 21)**: The clean modern way to check and cast types in one easy step.
- ✅ **Sealed Types (`sealed`, `permits`)**: Keeping your AI event streams strictly controlled and bug-free.
- ✅ **The Spring AI Architecture Connection**: Seeing how Spring AI's `ChatModel` interface lets you swap AI providers with zero stress.

---

## 🗺️ Table of Contents

- [1. Real-World Analogy: The Universal USB-C Port](#1-real-world-analogy-the-universal-usb-c-port)
- [2. Inheritance (`extends`): Hierarchies and Code Reuse](#2-inheritance-extends-hierarchies-and-code-reuse)
  - [2.1 Building an AI Message Hierarchy](#21-building-an-ai-message-hierarchy)
  - [2.2 Method Overriding and `@Override`](#22-method-overriding-and-override)
  - [2.3 The `super` Keyword](#23-the-super-keyword)
- [3. Abstract Classes: Partial Blueprints](#3-abstract-classes-partial-blueprints)
- [4. Interfaces: Pure Architectural Contracts](#4-interfaces-pure-architectural-contracts)
  - [4.1 Why Interfaces Rule the Enterprise](#41-why-interfaces-rule-the-enterprise)
  - [4.2 The Diamond Problem Solved](#42-the-diamond-problem-solved)
  - [4.3 Default and Static Methods in Modern Java](#43-default-and-static-methods-in-modern-java)
- [5. Polymorphism in Action: Swapping LLMs at Runtime](#5-polymorphism-in-action-swapping-llms-at-runtime)
- [6. Modern Java: Pattern Matching & Sealed Types](#6-modern-java-pattern-matching--sealed-types)
  - [6.1 Pattern Matching with `instanceof`](#61-pattern-matching-with-instanceof)
  - [6.2 Sealed Interfaces (`sealed` & `permits`)](#62-sealed-interfaces-sealed--permits)
- [7. Composition Over Inheritance: The Senior Rule](#7-composition-over-inheritance-the-senior-rule)
- [8. The Spring AI Architecture Connection](#8-the-spring-ai-architecture-connection)
- [9. Key Takeaways & Summary](#9-key-takeaways--summary)
- [10. Practice Exercises & Full Solutions](#10-practice-exercises--full-solutions)
- [11. Self-Check Quiz](#11-self-check-quiz)

---

# 1. Real-World Analogy: The Universal USB-C Port

> [!TIP]
> ### 💡 New Word Alert: AI Terms You'll See Today
> - **Ollama**: A free, open-source application that lets you run powerful AI models (like Meta's Llama 3) directly on your own computer or laptop! No credit card, no internet connection needed, completely private and free.
> - **Streaming**: When you chat with ChatGPT, notice how words appear one by one like a typewriter? That's called "streaming"! Instead of waiting 10 seconds for the full paragraph, the AI sends tiny pieces (stream tokens) in real time.

Imagine your laptop. On the side is a **USB-C port**.

```
                           ┌────────────────────────┐
                           │      USB-C PORT        │
                           │      (Interface)       │
                           │  - deliverPower()      │
                           │  - transferData()      │
                           └───────────┬────────────┘
                                       │
         ┌─────────────────────────────┼─────────────────────────────┐
         ▼                             ▼                             ▼
  ┌───────────────┐             ┌───────────────┐             ┌───────────────┐
  │ Monitor Cable │             │ Fast Charger  │             │ External SSD  │
  │ (Transmits 4K)│             │ (Pumps 65W)   │             │ (Reads 2GB/s) │
  └───────────────┘             └───────────────┘             └───────────────┘
```

Your laptop doesn't care who manufactured the device plugged into that port. It could be Apple, Samsung, Dell, or Anker. As long as the device follows the **USB-C Interface Standard**, it works seamlessly.

In software:
- **The USB-C Port** is a Java **Interface** (e.g., `ChatModel`).
- **The Devices** are concrete **Classes** (e.g., `OpenAiChatModel`, `OllamaChatModel`, `ClaudeChatModel`).
- **Your Laptop** is your **Business Service** (e.g., `CustomerSupportBot`). It only connects to the interface, not the specific vendor!

---

## 🧭 The Plain English Bridge: Abstract Class vs. Interface Demystified

Every Java interview asks: *"What is the difference between an Abstract Class and an Interface?"* Here is how to keep them crystal clear in your mind:

| Dimension | Abstract Class (`abstract class`) | Interface (`interface`) | Plain English Rule |
| :--- | :--- | :--- | :--- |
| **Relationship** | **"IS-A"** identity (`Dog is an Animal`). | **"CAN-DO"** capability (`Dog can Run`, `Document can BePrinted`). | Inheritance is who you are; Interfaces are what you can do. |
| **Instance Fields** | Can have regular variables (`protected int tokenCount;`). | **Cannot hold data.** Only fixed constants (`public static final`). | If you need fields to hold state, use an Abstract Class. |
| **Inheritance Limit**| A class can extend **only ONE** abstract class. | A class can implement **as many interfaces as you want** (`implements A, B, C`). | Avoids confusion: no clashing parent variables. |
| **Default Methods (Java 8+)** | Regular method with code inside. | Can also have `default` methods with code! | Lets framework creators add new methods without breaking your code. |
| **Polymorphism in Spring** | Rarely used as the injected variable type. | **The industry standard.** Inject `ChatModel`, not `OpenAiChatModel`. | Write your code against the contract, never the vendor. |
| **Java 21 Pattern Matching** | `if (obj instanceof String s)` | Auto-casts `obj` into `s` on the fly! | No more clunky old manual casting like `String s = (String) obj;`! |

---

# 2. Inheritance (`extends`): Hierarchies and Code Reuse

Inheritance allows a child class (subclass) to inherit the state (fields) and behavior (methods) of a parent class (superclass).

### 2.1 Building an AI Message Hierarchy

In LLMs, chat messages share common attributes: all messages have text `content` and a creation `timestamp`. But:
- A `UserMessage` may contain uploaded image attachments.
- A `SystemMessage` contains system instructions and safety rules.
- An `AssistantMessage` contains generated tokens and tool execution requests.

Let's model this with inheritance:

```java
package com.javagenai.day03;

import java.time.Instant;

// Base Parent Class
public class BaseMessage {
    private final String content;
    private final Instant timestamp;

    public BaseMessage(String content) {
        this.content = content;
        this.timestamp = Instant.now();
    }

    public String getContent() {
        return content;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getRole() {
        return "UNKNOWN";
    }
}
```

Now let's create `UserMessage` that inherits from `BaseMessage` using the `extends` keyword:

```java
package com.javagenai.day03;

import java.util.Collections;
import java.util.List;

public class UserMessage extends BaseMessage {
    private final List<String> attachmentUrls;

    // 1. Calling the parent constructor using super(...)
    public UserMessage(String content, List<String> attachmentUrls) {
        super(content); // Must be the VERY FIRST statement in constructor!
        this.attachmentUrls = (attachmentUrls != null) ? attachmentUrls : Collections.emptyList();
    }

    public UserMessage(String content) {
        this(content, null); // Constructor chaining
    }

    public List<String> getAttachmentUrls() {
        return attachmentUrls;
    }

    // 2. Overriding the parent's getRole() method
    @Override
    public String getRole() {
        return "USER";
    }
}
```

---

### 2.2 Method Overriding and `@Override`

Notice the `@Override` annotation above `getRole()`.
- **Method Overriding**: When a child class provides its own specific implementation of a method that is already defined in its parent class.
- **Why `@Override` is critical**: It asks the compiler to double-check your method signature. If you accidentally misspelled `getRole` as `getrole()`, the compiler catches the typo immediately and stops the build!

---

### 2.3 The `super` Keyword

- `super(...)`: Invokes the constructor of the immediate parent class.
- `super.someMethod()`: Invokes the parent's version of an overridden method.

---

# 3. Abstract Classes: Partial Blueprints

Sometimes a base class represents a concept so generic that creating a direct object of it makes no sense.

For example, what does a generic `BaseModel` look like? You cannot run inference on a "BaseModel"—you can only run inference on a specific model like `OpenAiModel` or `OllamaModel`.

In Java, we mark such classes with the keyword **`abstract`**:
- **Cannot be instantiated**: Calling `new AbstractLLMClient(...)` causes a compile-time error.
- **Can contain abstract methods**: Methods with no body (no curly braces `{}`) that child classes **must** implement.
- **Can contain concrete methods and state**: Common helper methods (like HTTP timeout handlers and retry logic) can be shared across all subclasses.

```java
package com.javagenai.day03;

public abstract class AbstractLLMClient {
    private final String endpointUrl;
    private final int timeoutSeconds;

    public AbstractLLMClient(String endpointUrl, int timeoutSeconds) {
        this.endpointUrl = endpointUrl;
        this.timeoutSeconds = timeoutSeconds;
    }

    // Concrete method shared by all LLM clients
    public void logRequest(String prompt) {
        System.out.printf("[%s] Dispatching prompt to %s (Timeout: %ds)%n", 
                          java.time.Instant.now(), endpointUrl, timeoutSeconds);
    }

    // Abstract method: Every specific client MUST write its own network logic!
    public abstract String generateResponse(String prompt);
}
```

---

# 4. Interfaces: Pure Architectural Contracts

While an abstract class is a **partial blueprint**, an **Interface** is a **100% pure contract**. 

An interface says:
> *"I do not care HOW you do it. I only care THAT you can do it. If you sign this contract (implement this interface), you must deliver these capabilities."*

### 4.1 Why Interfaces Rule the Enterprise

Let's look at how Spring AI defines its core `ChatModel` interface:

```java
package com.javagenai.day03;

public interface ChatModel {

    // Any class implementing ChatModel MUST provide this method
    String call(String prompt);

    // Optional: token calculation
    default int estimateTokens(String text) {
        return (text == null) ? 0 : (int) Math.ceil(text.length() / 4.0);
    }
}
```

Now let's implement this interface for **two completely different LLM providers**:

#### Implementation 1: OpenAI (Cloud)
```java
package com.javagenai.day03;

public class OpenAiChatModel implements ChatModel {
    private final String apiKey;

    public OpenAiChatModel(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String call(String prompt) {
        // In real life: makes an HTTPS POST request to https://api.openai.com/v1/chat/completions
        return "[OpenAI GPT-4o Response to: '" + prompt + "']";
    }
}
```

#### Implementation 2: Ollama (Local & Free!)
```java
package com.javagenai.day03;

public class OllamaChatModel implements ChatModel {
    private final String localHostUrl;

    public OllamaChatModel(String localHostUrl) {
        this.localHostUrl = localHostUrl;
    }

    @Override
    public String call(String prompt) {
        // In real life: makes a local HTTP call to http://localhost:11434/api/generate
        return "[Ollama Llama-3.2 Local Response to: '" + prompt + "']";
    }
}
```

---

### 4.2 The Diamond Problem Solved

Why doesn't Java allow a class to `extend` multiple classes (e.g., `class C extends A, B`)?

Imagine Class A has a method `save()` and Class B has a method `save()`. If Class C inherits from both, which `save()` should it call? This ambiguity is known as the **Diamond Problem**.

```
          Class A (save)       Class B (save)
                 \                  /
                  \                /
                   ▼              ▼
                     Class C (???)
```

**Java's Solution**:
- A class can only **extend ONE class** (Single Class Inheritance).
- But a class can **implement MULTIPLE interfaces**!
  `public class RAGService implements ChatModel, Searchable, AutoCloseable`
Since interfaces only declare method signatures without conflicting state, there is zero ambiguity.

---

### 4.3 Default and Static Methods in Modern Java

Prior to Java 8, interfaces could *only* declare method signatures. Modern Java allows two powerful additions:

1. **`default` methods**: Methods with a default body. Implementing classes inherit this method automatically unless they choose to override it. This allows library designers (like the Spring team) to add new features to interfaces without breaking existing code!
2. **`static` methods**: Utility helper functions tied directly to the interface namespace (e.g., `ChatModel.getDefaultClient()`).

---

# 5. Polymorphism in Action: Swapping LLMs at Runtime

Now witness the true power of polymorphism. Here is our enterprise `AIAssistantService`:

```java
package com.javagenai.day03;

public class AIAssistantService {

    // Notice: We hold a reference to the INTERFACE, not the concrete class!
    private ChatModel chatModel;

    public AIAssistantService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    // Dynamic Hot-Swapping!
    public void setChatModel(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public void answerUserQuery(String query) {
        System.out.println("Processing user inquiry: " + query);
        // Dynamic Method Dispatch: The JVM figures out WHICH model to call at runtime!
        String answer = this.chatModel.call(query);
        System.out.println("AI Answer: " + answer);
    }
}
```

Look at how this behaves in `main`:

```java
ChatModel cloudModel = new OpenAiChatModel("sk-prod-12345");
ChatModel localModel = new OllamaChatModel("http://localhost:11434");

AIAssistantService bot = new AIAssistantService(cloudModel);
bot.answerUserQuery("Explain Quantum Computing");
// Output: AI Answer: [OpenAI GPT-4o Response to: 'Explain Quantum Computing']

// OpenAI goes down? Or cost budget exceeded? Hot-swap to local Ollama on the fly!
bot.setChatModel(localModel);
bot.answerUserQuery("Explain Quantum Computing");
// Output: AI Answer: [Ollama Llama-3.2 Local Response to: 'Explain Quantum Computing']
```

> **The `AIAssistantService` did not change by a single line of code.** That is the beauty of Polymorphism.

---

# 6. Modern Java: Pattern Matching & Sealed Types

Java 21 introduced modern language features that eliminate old-school casting and make domain modeling elegant.

### 6.1 Pattern Matching with `instanceof`

In old Java, checking and casting types required two verbose steps:
```java
// OLD Java (pre-Java 16)
if (message instanceof UserMessage) {
    UserMessage userMsg = (UserMessage) message; // Manual ugly cast!
    System.out.println("Attachments: " + userMsg.getAttachmentUrls());
}
```

In **Modern Java (Java 21)**, you test and bind in a single elegant step:
```java
// MODERN Java 21 Pattern Matching:
if (message instanceof UserMessage userMsg) {
    // userMsg is already cast and ready to use!
    System.out.println("Attachments: " + userMsg.getAttachmentUrls());
}
```

---

### 6.2 Sealed Interfaces (`sealed` & `permits`)

In mission-critical AI systems, you often want a closed hierarchy. For example: an LLM streaming token can **ONLY** be one of three things:
1. A `TextToken` (regular word chunk)
2. A `ToolCallToken` (model requested a database search)
3. An `EndOfStreamToken` (stream finished)

Before Java 17, any developer in any package could inherit from your interface and introduce rogue event types.

**Sealed Types** allow you to restrict which classes are permitted to implement your interface:

```java
package com.javagenai.day03;

// Only these three records are permitted to implement StreamEvent!
public sealed interface StreamEvent permits TextToken, ToolCallToken, EndOfStreamToken {
}

record TextToken(String text) implements StreamEvent {}
record ToolCallToken(String functionName, String jsonArgs) implements StreamEvent {}
record EndOfStreamToken(int totalTokensUsed) implements StreamEvent {}
```

Now, the Java compiler guarantees that **no other class in the world can implement `StreamEvent`**. When handling events in a modern `switch` statement, you don't even need a `default` case because the compiler knows all possible subclasses exhaustively!

---

# 7. Composition Over Inheritance: The Senior Rule

A famous principle in enterprise Java is:
> **"Favor Composition over Inheritance."**

- **Inheritance** creates an **"IS-A"** relationship (`Dog IS-A Animal`, `UserMessage IS-A BaseMessage`). It tightly couples the child to every single field and quirk of the parent.
- **Composition** creates a **"HAS-A"** relationship (`Car HAS-A Engine`, `AIAssistant HAS-A ChatModel`). It is loosely coupled, easily mocked in tests, and swappable at runtime.

Whenever you want to reuse functionality, ask yourself: *"Does my class truly have an 'is-a' biological identity relationship, or does it simply need to USE a tool?"* If it just needs to use a tool, **use composition with an interface!**

---

# 8. The Spring AI Architecture Connection

Let's look at the actual Spring AI dependency graph. Notice how everything we studied today forms its architectural backbone:

```
                          ┌─────────────────────────────┐
                          │    <<interface>> Model      │
                          └──────────────┬──────────────┘
                                         │
                 ┌───────────────────────┴───────────────────────┐
                 ▼                                               ▼
   ┌───────────────────────────┐                   ┌───────────────────────────┐
   │  <<interface>> ChatModel  │                   │<<interface>>EmbeddingModel│
   └─────────────┬─────────────┘                   └─────────────┬─────────────┘
                 │                                               │
     ┌───────────┴───────────┐                       ┌───────────┴───────────┐
     ▼                       ▼                       ▼                       ▼
┌──────────────┐      ┌──────────────┐        ┌──────────────┐        ┌──────────────┐
│OpenAiChat    │      │OllamaChat    │        │OpenAi        │        │Ollama        │
│Model         │      │Model         │        │EmbeddingModel│        │EmbeddingModel│
└──────────────┘      └──────────────┘        └──────────────┘        └──────────────┘
```

When you write a Spring Boot AI application in Phase 6, you will write:
```java
@Autowired
private ChatModel chatModel; // Injected automatically by Spring via Polymorphism!
```
Spring Boot looks at your `application.yml` configuration. If you configured OpenAI, it injects `OpenAiChatModel`. If you configured Ollama, it injects `OllamaChatModel`. **Your code never changes.**

---

# 9. Key Takeaways & Summary

```
                  ┌─────────────────────────────────┐
                  │       DAY 03 CHEAT SHEET        │
                  └────────────────┬────────────────┘
                                   │
         ┌─────────────────────────┼─────────────────────────┐
         ▼                         ▼                         ▼
  [ Inheritance ]          [ Interfaces ]            [ Polymorphism ]
  • 'extends' single class • 'implements' multiple   • Program to Interfaces,
  • 'super()' calls parent • Pure API contract         not implementations
  • Abstract classes for   • 'default' methods add   • Swappable AI engines
    partial blueprints       backward compatibility  • Pattern matching:
  • Favor Composition      • 'sealed' restricts        'if (obj instanceof T t)'
    over Inheritance         permitted subclasses
```

---

# 10. Practice Exercises & Full Solutions

### 🏋️ Exercise 1: Build a Pluggable `VectorStore` Interface

> [!TIP]
> ### 💡 New Word Alert: Vector Store (Vector Database)
> When you build AI applications that read your documents (like employee handbooks or product manuals), you convert paragraphs into numbers (embeddings) and store them in a database.
> 
> A database designed to store numbers and search by meaning is called a **Vector Store** (or Vector DB). In production, companies use systems like PostgreSQL (with `pgvector`), Milvus, or Pinecone.
> 
> But here's the beauty of Polymorphism: by defining an interface `SimpleVectorStore`, our code doesn't care whether the vectors are stored in real PostgreSQL or just inside a simple Java `HashMap` in RAM!

**The Task**: Create a pluggable `SimpleVectorStore` interface and implement a fast `InMemoryVectorStore` using a Java `Map`.

#### Solution:
```java
package com.javagenai.day03;

import java.util.*;

public interface SimpleVectorStore {
    void addDocument(String id, String text, double[] embedding);
    List<String> similaritySearch(double[] queryEmbedding, int topK);
}
```

```java
package com.javagenai.day03;

import java.util.*;

public class InMemoryVectorStore implements SimpleVectorStore {
    private final Map<String, String> docMap = new HashMap<>();

    @Override
    public void addDocument(String id, String text, double[] embedding) {
        docMap.put(id, text);
        System.out.println("[InMemoryVectorStore] Cached document id: " + id);
    }

    @Override
    public List<String> similaritySearch(double[] queryEmbedding, int topK) {
        System.out.printf("[InMemoryVectorStore] Searching %d documents in RAM...%n", docMap.size());
        // Returns first topK matches for demo
        return new ArrayList<>(docMap.values()).subList(0, Math.min(topK, docMap.size()));
    }
}
```

---

### 🏋️ Exercise 2: Modern Java 21 Pattern Matching on AI Stream Events

> [!TIP]
> ### 💡 The Story Behind Streaming Events
> When you talk to an AI model, it sends tokens back to your screen in real time (streaming). Along the way, different things can happen:
> 1. `ChunkEvent`: A new word or sentence chunk arrived to display.
> 2. `ErrorEvent`: Something failed (rate limit reached, API key expired).
> 3. `FinishedEvent`: The AI is done answering, along with how many milliseconds it took.
> 
> By using a `sealed interface`, we guarantee that only these 3 specific events exist. The Java compiler will force you to handle all 3 — leaving zero room for unexpected surprises!

#### Solution:
```java
package com.javagenai.day03;

public sealed interface LLMEvent permits ChunkEvent, ErrorEvent, FinishedEvent {}

record ChunkEvent(String text) implements LLMEvent {}
record ErrorEvent(String errorMsg, int code) implements LLMEvent {}
record FinishedEvent(long durationMs) implements LLMEvent {}
```

```java
package com.javagenai.day03;

public class EventProcessor {

    public static void handleEvent(LLMEvent event) {
        // Modern Java 21 exhaustive pattern matching switch
        switch (event) {
            case ChunkEvent chunk -> 
                System.out.print(chunk.text());
            case ErrorEvent err -> 
                System.err.printf("%n[ERROR %d]: %s%n", err.code(), err.errorMsg());
            case FinishedEvent done -> 
                System.out.printf("%n[STREAM COMPLETED]: Execution took %d ms%n", done.durationMs());
        }
    }
}
```

---

## 11. Self-Check Quiz

1. **Why does Java not support multiple inheritance for classes?**
   - *Answer*: To prevent the Diamond Problem — confusion and ambiguity when two parent classes define the same method with different code bodies.
2. **Can an abstract class have a constructor?**
   - *Answer*: Yes! Even though you can't instantiate it directly with `new`, its constructor runs when a child class calls `super(...)` to set up shared fields.
3. **What is the difference between a `default` method in an interface and an abstract method?**
   - *Answer*: An abstract method has no code body and must be written by the child class. A `default` method has a pre-written code body that child classes can use directly or choose to customize.
4. **How does Polymorphism enable swapping OpenAI with Ollama in Spring AI?**
   - *Answer*: Your service code only talks to the `ChatModel` interface. At runtime, Spring supplies either `OpenAiChatModel` or `OllamaChatModel` without changing a single line of your code!
5. **What does the `sealed` keyword on an interface achieve in Java 21?**
   - *Answer*: It explicitly controls which classes or records are allowed to implement the interface, giving you a closed, safe set of types.

---

<p align="center">
  <b>Awesome job finishing Day 03! 🎉</b><br>
  You've mastered Polymorphism, Interfaces, and Abstract Classes — the architectural backbone of Spring Boot and Spring AI.<br>
  Tomorrow on <b>Day 04</b>, we'll dive into <b>Generics, Collections & Data Structures</b>: Lists, Sets, and HashMaps — the exact containers that store your AI prompts, tokens, and document chunks in memory! Keep up the momentum!
</p>
