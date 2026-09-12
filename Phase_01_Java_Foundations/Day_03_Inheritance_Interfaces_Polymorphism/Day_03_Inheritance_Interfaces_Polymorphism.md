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

## 🗺️ Table of Contents
- [1. Topic Overview](#1-topic-overview)
- [2. Basic Foundations (True Zero)](#2-basic-foundations-true-zero)
  - [2.1 What is Inheritance, an Interface, and Polymorphism?](#21-what-is-inheritance-an-interface-and-polymorphism)
  - [2.2 The USB-C Port Analogy](#22-the-usb-c-port-analogy)
  - [2.3 Minimal Working Example: Swappable AI Echo](#23-minimal-working-example-swappable-ai-echo)
  - [2.4 Line-by-Line Code Breakdown](#24-line-by-line-code-breakdown)
- [3. Core Concept Walkthrough (Basic → Intermediate)](#3-core-concept-walkthrough-basic--intermediate)
  - [3.1 Inheritance (`extends`) and the AI Message Hierarchy](#31-inheritance-extends-and-the-ai-message-hierarchy)
  - [3.2 Method Overriding and `@Override`](#32-method-overriding-and-override)
  - [3.3 Abstract Classes: Partial Blueprints with `abstract`](#33-abstract-classes-partial-blueprints-with-abstract)
  - [3.4 Interfaces: Pure Architectural Contracts (`interface` & `implements`)](#34-interfaces-pure-architectural-contracts-interface--implements)
  - [3.5 Modern Interface Features: `default` and `static` Methods](#35-modern-interface-features-default-and-static-methods)
  - [3.6 Polymorphism in Action: Hot-Swapping AI Providers at Runtime](#36-polymorphism-in-action-hot-swapping-ai-providers-at-runtime)
  - [3.7 Modern Java 21: Pattern Matching and Sealed Interfaces](#37-modern-java-21-pattern-matching-and-sealed-interfaces)
- [4. Prerequisite & Supporting Concepts](#4-prerequisite--supporting-concepts)
  - [Prerequisite / Supporting Concept: Overriding vs. Overloading](#prerequisite--supporting-concept-overriding-vs-overloading)
  - [Prerequisite / Supporting Concept: The Diamond Problem & Multiple Inheritance](#prerequisite--supporting-concept-the-diamond-problem--multiple-inheritance)
  - [Prerequisite / Supporting Concept: Composition Over Inheritance ("Is-A" vs. "Has-A")](#prerequisite--supporting-concept-composition-over-inheritance-is-a-vs-has-a)
- [5. Advanced Depth (Intermediate → Advanced)](#5-advanced-depth-intermediate--advanced)
  - [5.1 Dynamic Method Dispatch & JVM VTables (Virtual Method Tables)](#51-dynamic-method-dispatch--jvm-vtables-virtual-method-tables)
  - [5.2 Common Mistakes & Misconceptions (With Bad vs. Good Code)](#52-common-mistakes--misconceptions-with-bad-vs-good-code)
  - [5.3 Architectural Trade-Offs: Interface Decoupling vs. Indirection Cost](#53-architectural-trade-offs-interface-decoupling-vs-indirection-cost)
- [6. Quick Recap](#6-quick-recap)
- [7. Self-Check Questions & Practice Exercises](#7-self-check-questions--practice-exercises)
  - [Self-Check Questions (Basic to Advanced)](#self-check-questions-basic-to-advanced)
  - [Hands-On Practice Exercises with Full Solutions](#hands-on-practice-exercises-with-full-solutions)

---

# 1. Topic Overview

Inheritance, Interfaces, and Polymorphism form the contract and abstraction foundation of modern object-oriented software engineering. **Inheritance** enables code sharing across parent-child class hierarchies; **Interfaces** define strict behavioral contracts without mandating implementation details; and **Polymorphism** allows an application to interact with diverse concrete objects through a unified interface.

### Why This Topic Matters
In enterprise Generative AI engineering, you never couple your core business logic to a single AI vendor (such as OpenAI, Anthropic, or local Ollama instances). By designing around Java interfaces like Spring AI's `ChatModel`, your application can hot-swap between cloud-hosted models and zero-cost local on-premise models, mock AI responses during automated testing, and future-proof enterprise systems against API changes without modifying business code.

> 💡 **New Word Alert — "Polymorphism"**: Derived from Greek meaning *"many forms"*. In Java, it allows a single variable of an interface type (e.g., `ChatModel`) to hold any valid implementation (`OpenAiChatModel`, `OllamaChatModel`) and invoke their specialized behaviors identically.

> 💡 **New Word Alert — "Ollama"**: A free, open-source tool that lets you run modern open-weights Large Language Models (like Meta's Llama 3.2 or Mistral) locally on your own laptop or enterprise server with zero cloud API costs.

> 💡 **New Word Alert — "Streaming"**: Receiving an AI model's response incrementally token-by-token (like a typewriter) rather than waiting several seconds for the full completed paragraph.

---

# 2. Basic Foundations (True Zero)

If you have never built a class hierarchy or interface before, let's establish intuition from absolute scratch.

### 2.1 What is Inheritance, an Interface, and Polymorphism?

- **Inheritance (`extends`)**: When a child class inherits properties and actions from a parent class, just like a child inherits traits from parents. If `Animal` can eat, `Dog` inherits `eat()` automatically without rewriting it.
- **Interface (`implements`)**: A formal contract of promises. An interface declares what actions an object can perform, but contains no state. If a class implements `Flyable`, it promises: *"I guarantee I have a `fly()` method."*
- **Polymorphism**: Interacting with an object based on *what it can do* (its interface), rather than *what exact class it is*.

---

### 2.2 The USB-C Port Analogy

Think about your laptop's **USB-C port**:

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

Your laptop does not know or care who manufactured the cable plugged into its USB-C port (Apple, Samsung, Dell, or Anker). As long as the device adheres to the universal **USB-C standard**, it works seamlessly.

In Java:
- The **USB-C Port** is a Java **Interface** (e.g., `ChatModel`).
- The **Plugged Devices** are concrete **Classes** (e.g., `OpenAiChatModel`, `OllamaChatModel`).
- Your **Laptop** is your **Business Service** (e.g., `CustomerSupportBot`). It depends only on the interface, never a vendor!

---

### 2.3 Minimal Working Example: Swappable AI Echo

Let's write a minimal, fully runnable Java program that demonstrates an interface with two interchangeable implementations:

```java
public class SimplePolymorphismDemo {

    // 1. The Interface (The Contract)
    interface TextGenerator {
        String generate(String prompt);
    }

    // 2. Concrete Implementation A
    static class PoliteGenerator implements TextGenerator {
        @Override
        public String generate(String prompt) {
            return "Certainly! Here is your answer to: " + prompt;
        }
    }

    // 3. Concrete Implementation B
    static class TerseGenerator implements TextGenerator {
        @Override
        public String generate(String prompt) {
            return "Answer: " + prompt;
        }
    }

    // 4. Client method that accepts the INTERFACE
    static void printAIResponse(TextGenerator model, String prompt) {
        // Polymorphic invocation!
        System.out.println(model.generate(prompt));
    }

    public static void main(String[] args) {
        TextGenerator polite = new PoliteGenerator();
        TextGenerator terse = new TerseGenerator();

        printAIResponse(polite, "What is Java?");
        printAIResponse(terse, "What is Java?");
    }
}
```

---

### 2.4 Line-by-Line Code Breakdown

1. `interface TextGenerator`: Declares a contract. Any class claiming to be a `TextGenerator` must implement `String generate(String prompt)`.
2. `class PoliteGenerator implements TextGenerator`: Uses `implements` to sign the contract.
3. `@Override public String generate(...)`: Fulfills the promise by providing the actual method body.
4. `static void printAIResponse(TextGenerator model, String prompt)`:
   - Notice the type of `model`: it is `TextGenerator` (the interface), **not** `PoliteGenerator` or `TerseGenerator`.
   - The method can accept *any* object that implements `TextGenerator`.
5. In `main`:
   - `printAIResponse(polite, ...)` outputs the polite response.
   - `printAIResponse(terse, ...)` outputs the terse response.
   - The method `printAIResponse` never changed; the object passed to it changed! That is polymorphism in its purest form.

---

# 3. Core Concept Walkthrough (Basic → Intermediate)

Now let's build the full architectural structure used across enterprise Java and Spring AI.

### 3.1 Inheritance (`extends`) and the AI Message Hierarchy

In LLMs, all messages share common state: textual `content` and a creation `timestamp`. However:
- A `UserMessage` may contain uploaded image attachment URLs.
- A `SystemMessage` contains system behavioral instructions.
- An `AssistantMessage` contains generated tokens and function execution payloads.

Let's build this hierarchy using `extends`:

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

Now let's create `UserMessage` inheriting from `BaseMessage`:

```java
package com.javagenai.day03;

import java.util.Collections;
import java.util.List;

public class UserMessage extends BaseMessage {
    private final List<String> attachmentUrls;

    // Child constructor calling parent via super(...)
    public UserMessage(String content, List<String> attachmentUrls) {
        super(content); // Must be the FIRST statement in child constructor!
        this.attachmentUrls = (attachmentUrls != null) ? attachmentUrls : Collections.emptyList();
    }

    public UserMessage(String content) {
        this(content, null); // Constructor chaining
    }

    public List<String> getAttachmentUrls() {
        return attachmentUrls;
    }

    // Overriding the parent's getRole() method
    @Override
    public String getRole() {
        return "USER";
    }
}
```

---

### 3.2 Method Overriding and `@Override`

- **Method Overriding**: When a subclass provides a specialized version of a method already declared in its superclass.
- **The `@Override` Annotation**: Informs the compiler that you intend to override a parent method. If you make a typo (e.g., `getrole()` instead of `getRole()`), the compiler flags it as an immediate error rather than silently treating it as a new method.

---

### 3.3 Abstract Classes: Partial Blueprints with `abstract`

Sometimes a parent class represents a concept so generic that directly creating an instance of it is meaningless. You cannot "execute" a generic `AbstractLLMClient`—you can only execute a concrete client like `OpenAiClient` or `OllamaClient`.

```java
package com.javagenai.day03;

public abstract class AbstractLLMClient {
    private final String endpointUrl;
    private final int timeoutSeconds;

    public AbstractLLMClient(String endpointUrl, int timeoutSeconds) {
        this.endpointUrl = endpointUrl;
        this.timeoutSeconds = timeoutSeconds;
    }

    // Concrete method shared by all subclasses
    public void logRequest(String prompt) {
        System.out.printf("[%s] Dispatching prompt to %s (Timeout: %ds)%n", 
                          java.time.Instant.now(), endpointUrl, timeoutSeconds);
    }

    // Abstract method: Every concrete subclass MUST provide this network implementation!
    public abstract String generateResponse(String prompt);
}
```

Key rules of `abstract`:
- Cannot be instantiated with `new AbstractLLMClient(...)`.
- Can contain both concrete methods (with code) and abstract methods (without code).
- Subclasses must implement all abstract methods or be declared abstract themselves.

---

### 3.4 Interfaces: Pure Architectural Contracts (`interface` & `implements`)

While an abstract class is a partial blueprint with instance state, an **Interface** is a 100% pure capability contract.

Here is how Spring AI defines its core `ChatModel` interface:

```java
package com.javagenai.day03;

public interface ChatModel {
    // Every implementing class MUST provide this method
    String call(String prompt);

    // Default method (available in Java 8+)
    default int estimateTokens(String text) {
        return (text == null) ? 0 : (int) Math.ceil(text.length() / 4.0);
    }
}
```

Now let's implement this interface for two completely different AI engines:

#### Implementation 1: OpenAI (Cloud API)
```java
package com.javagenai.day03;

public class OpenAiChatModel implements ChatModel {
    private final String apiKey;

    public OpenAiChatModel(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String call(String prompt) {
        return "[OpenAI GPT-4o Response to: '" + prompt + "']";
    }
}
```

#### Implementation 2: Ollama (Local & Free)
```java
package com.javagenai.day03;

public class OllamaChatModel implements ChatModel {
    private final String localHostUrl;

    public OllamaChatModel(String localHostUrl) {
        this.localHostUrl = localHostUrl;
    }

    @Override
    public String call(String prompt) {
        return "[Ollama Llama-3.2 Local Response to: '" + prompt + "']";
    }
}
```

---

### 3.5 Modern Interface Features: `default` and `static` Methods

- **`default` methods**: Allow interfaces to provide pre-built default logic without breaking existing classes that implement the interface.
- **`static` methods**: Utility helper functions tied directly to the interface namespace (e.g., `ChatModel.builder()`).

---

### 3.6 Polymorphism in Action: Hot-Swapping AI Providers at Runtime

Here is our production `AIAssistantService`:

```java
package com.javagenai.day03;

public class AIAssistantService {

    // Depend on the INTERFACE, not the concrete implementation!
    private ChatModel chatModel;

    public AIAssistantService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    // Dynamic Hot-Swapping
    public void setChatModel(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public void answerUserQuery(String query) {
        System.out.println("Processing user inquiry: " + query);
        // Dynamic Method Dispatch: JVM invokes the correct vendor logic at runtime!
        String answer = this.chatModel.call(query);
        System.out.println("AI Answer: " + answer);
    }
}
```

Executing with zero changes to `AIAssistantService`:

```java
ChatModel cloudModel = new OpenAiChatModel("sk-prod-12345");
ChatModel localModel = new OllamaChatModel("http://localhost:11434");

AIAssistantService bot = new AIAssistantService(cloudModel);
bot.answerUserQuery("Explain Quantum Computing");
// Output: AI Answer: [OpenAI GPT-4o Response to: 'Explain Quantum Computing']

// OpenAI outage or cost exceeded? Hot-swap to local model on the fly!
bot.setChatModel(localModel);
bot.answerUserQuery("Explain Quantum Computing");
// Output: AI Answer: [Ollama Llama-3.2 Local Response to: 'Explain Quantum Computing']
```

---

### 3.7 Modern Java 21: Pattern Matching and Sealed Interfaces

Java 21 introduces pattern matching and sealed types for domain modeling.

#### Pattern Matching with `instanceof`:
```java
// Java 21: Tests and casts in a single clean expression
if (message instanceof UserMessage userMsg) {
    System.out.println("Attachments count: " + userMsg.getAttachmentUrls().size());
}
```

#### Sealed Interfaces (`sealed` & `permits`):
Restricts which classes are permitted to implement an interface, ensuring a strictly bounded type hierarchy:

```java
package com.javagenai.day03;

public sealed interface StreamEvent permits TextToken, ToolCallToken, EndOfStreamToken {}

record TextToken(String text) implements StreamEvent {}
record ToolCallToken(String functionName, String jsonArgs) implements StreamEvent {}
record EndOfStreamToken(int totalTokensUsed) implements StreamEvent {}
```

---

# 4. Prerequisite & Supporting Concepts

### Prerequisite / Supporting Concept: Overriding vs. Overloading

| Dimension | Method Overriding | Method Overloading |
| :--- | :--- | :--- |
| **Location** | Between Parent and Child classes. | Within the same class. |
| **Method Name** | Exactly identical. | Exactly identical. |
| **Parameters** | Exactly identical signature and types. | Must differ in parameter types or count. |
| **Return Type** | Must match (or be a subtype). | Can be anything. |
| **Resolution** | Runtime (Dynamic Polymorphism). | Compile-time (Static Polymorphism). |

---

### Prerequisite / Supporting Concept: The Diamond Problem & Multiple Inheritance

Why does Java reject `class C extends A, B`?
If Class A has `save()` and Class B has `save()`, Class C inherits two conflicting versions. This is the **Diamond Problem**.
Java resolves this by:
- Allowing a class to extend **only one parent class**.
- Allowing a class to implement **unlimited interfaces** (`implements ChatModel, AutoCloseable, Serializable`).

---

### Prerequisite / Supporting Concept: Composition Over Inheritance ("Is-A" vs. "Has-A")

- **Inheritance ("Is-A")**: `UserMessage IS-A BaseMessage`. Creates tight coupling between child and parent.
- **Composition ("Has-A")**: `AIAssistantService HAS-A ChatModel`. Loosely coupled, testable, and swappable at runtime.
- **The Senior Rule**: Always favor composition with interfaces over class inheritance whenever possible.

---

# 5. Advanced Depth (Intermediate → Advanced)

### 5.1 Dynamic Method Dispatch & JVM VTables (Virtual Method Tables)

When `chatModel.call(prompt)` executes, how does the JVM know which vendor method to execute without expensive `if-else` checks?

```
Variable: chatModel (Type: ChatModel) ──► Points to Heap Object: OllamaChatModel
                                                  │
                                                  ▼
                                      [ Object Header: Klass Pointer ]
                                                  │
                                                  ▼
                                          [ VTable in Metaspace ]
                                          Index 0: call() ──► &OllamaChatModel.call
```

1. Each loaded class in the JVM maintains a **vtable (virtual method table)** in Metaspace containing pointers to its executable machine code.
2. At compile-time, the compiler assigns an integer offset index to each method in the vtable (e.g., `call()` = index 0).
3. At runtime, the JVM performs a single pointer dereference: `object->vtable[0]()`. This delivers method dispatch in just a few CPU cycles ($O(1)$).

---

### 5.2 Common Mistakes & Misconceptions (With Bad vs. Good Code)

#### Mistake 1: Coupling to Concrete Classes Instead of Interfaces
**Bad Code:**
```java
public class ChatService {
    // ❌ Tightly coupled: Can never switch to Ollama or a mock for unit testing!
    private OpenAiChatModel model = new OpenAiChatModel("key");
}
```
**Correct Code:**
```java
public class ChatService {
    // ✅ Loosely coupled: Accepts any ChatModel implementation
    private final ChatModel model;

    public ChatService(ChatModel model) {
        this.model = model;
    }
}
```

#### Mistake 2: Forgetting `@Override` Causing Silent Overload Bugs
**Bad Code:**
```java
public class CustomPrompt extends BaseMessage {
    public CustomPrompt(String text) { super(text); }

    // ❌ Bug: Typo in parameter or method name creates an overload, parent method never overridden!
    public String getRole(int version) { 
        return "CUSTOM"; 
    }
}
```
**Correct Code:**
```java
public class CustomPrompt extends BaseMessage {
    public CustomPrompt(String text) { super(text); }

    // ✅ Compiler verifies signature matches parent exactly
    @Override
    public String getRole() { 
        return "CUSTOM"; 
    }
}
```

---

### 5.3 Architectural Trade-Offs: Interface Decoupling vs. Indirection Cost

| Design Approach | Pros | Cons | Best Used When |
| :--- | :--- | :--- | :--- |
| **Direct Concrete Class** | Zero indirection; straightforward navigation. | Impossible to mock or hot-swap without rewriting code. | Data transfer objects (DTOs), utility helper classes. |
| **Interface Abstraction** | Complete modularity, testable with mocks, hot-swappable. | Slight architectural overhead; requires dependency injection. | All enterprise services, AI models, vector stores, repositories. |

---

# 6. Quick Recap

| Concept | Key Takeaway |
| :--- | :--- |
| **Inheritance (`extends`)** | Reuses fields and logic from a single parent; child uses `super()` to initialize parent state. |
| **Interface (`implements`)** | Pure capability contract; a class can implement multiple interfaces. |
| **Abstract Class** | Partial blueprint that can hold instance fields, constructors, and abstract methods. |
| **Polymorphism** | Interacting with objects via interface references; swappable at runtime. |
| **Pattern Matching (Java 21)**| `if (obj instanceof Class c)` checks and binds in one step. |
| **Sealed Types** | `sealed interface ... permits ...` restricts permitted implementations for total domain control. |

---

# 7. Self-Check Questions & Practice Exercises

### Self-Check Questions (Basic to Advanced)

1. **Why does Java prohibit multiple inheritance for classes while permitting multiple interface implementation?**
   - *Answer*: To avoid the Diamond Problem where two parent classes define conflicting method implementations. Interfaces contain contracts without conflicting state, eliminating ambiguity.
2. **Can an abstract class have a constructor? If so, how is it invoked?**
   - *Answer*: Yes. Even though it cannot be instantiated directly via `new`, its constructor runs when a subclass calls `super(...)` during initialization.
3. **What is the purpose of the `@Override` annotation?**
   - *Answer*: It informs the compiler to verify that a method signature exactly matches an inherited method in a superclass or interface, preventing accidental overloading bugs.
4. **How does Polymorphism enable swapping OpenAI with Ollama in Spring AI?**
   - *Answer*: Service classes interact exclusively with the `ChatModel` interface. At runtime, the application injects either `OpenAiChatModel` or `OllamaChatModel` without modifying service code.
5. **What compile-time safety guarantee is provided by Java 21 `sealed` interfaces?**
   - *Answer*: The compiler knows the exhaustive list of permitted subclasses, allowing `switch` expressions on sealed types without requiring a fallback `default` case.

---

### Hands-On Practice Exercises with Full Solutions

#### 🏋️ Exercise 1: Build a Pluggable `SimpleVectorStore` Interface & In-Memory Store
**Objective**: Build a clean interface `SimpleVectorStore` and implement a fast `InMemoryVectorStore` using a Java `Map`.

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
        System.out.println("[InMemoryVectorStore] Stored document id: " + id);
    }

    @Override
    public List<String> similaritySearch(double[] queryEmbedding, int topK) {
        System.out.printf("[InMemoryVectorStore] Searching %d documents in memory...%n", docMap.size());
        return new ArrayList<>(docMap.values()).subList(0, Math.min(topK, docMap.size()));
    }
}
```

---

#### 🏋️ Exercise 2: Modern Java 21 Pattern Matching on AI Stream Events
**Objective**: Model a sealed streaming event hierarchy and write a pattern-matching processor that handles each event exhaustively.

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

<p align="center">
  <b>Day 03 Complete! 🎉</b><br>
  Proceed to <b>Day 04</b>: <b>Generics, Collections & Data Structures</b>.<br>
  <a href="../Day_04_Generics_Collections_DataStructures/Day_04_Generics_Collections_DataStructures.md"><b>Continue to Day 04 →</b></a>
</p>
