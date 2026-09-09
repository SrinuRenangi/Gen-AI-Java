# 🧱 Day 02: OOP — Classes, Objects & Memory
## Stack vs Heap, Constructors, and the Sacred equals() & hashCode() Contract

| ⬅️ Previous Day | 📚 Course Hub | ➡️ Next Day |
|:---|:---:|---:|
| [← Day 01: Java Ecosystem & Setup](../Day_01_Java_Ecosystem_and_Setup/Day_01_Java_Ecosystem_and_Setup.md) | [All 60 Days Overview](../../README.md) | [Day 03: Inheritance, Interfaces & Polymorphism →](../Day_03_Inheritance_Interfaces_Polymorphism/Day_03_Inheritance_Interfaces_Polymorphism.md) |

[![Phase](https://img.shields.io/badge/Phase_01-Java_Foundations-brightgreen.svg?style=for-the-badge)](../../README.md)
[![Day](https://img.shields.io/badge/Day-02_of_60-blue.svg?style=for-the-badge)](../../README.md)
[![Difficulty](https://img.shields.io/badge/Difficulty-Beginner_to_Intermediate-green.svg?style=for-the-badge)](../../README.md)
[![Topic](https://img.shields.io/badge/Core-Object_Oriented_Programming-orange.svg?style=for-the-badge)](../../README.md)

---

## 📌 What Will You Learn Today?

Yesterday, you ran your first Java program and learned how `javac` compiles code into portable bytecode executed by the JVM. Today, we dive into the beating heart of Java: **Object-Oriented Programming (OOP) and Memory Management**.

If you come from Python or JavaScript, you might view OOP as just "putting functions inside classes." In enterprise Java, OOP is much deeper: it defines **how memory is allocated, how objects maintain integrity, and how the JVM prevents data corruption**.

By the end of today, you will master:
- ✅ **Classes vs Objects**: The mental model of Blueprints vs Physical Instances.
- ✅ **Stack vs Heap Memory**: Where variables and objects actually live inside your computer's RAM.
- ✅ **Pass-by-Value in Java**: The classic myth debunked once and for all.
- ✅ **Encapsulation & Access Modifiers**: Guarding the internal state of AI prompts, parameters, and tokens.
- ✅ **The Sacred Contract**: Why `==` fails on objects, and how `equals()` and `hashCode()` make HashMaps, Vector Stores, and Caching layers work.
- ✅ **Garbage Collection (GC)**: How the JVM automatically recycles gigabytes of embedding vectors and prompt strings.
- ✅ **The Spring Bean Connection**: Understanding that a "Spring Bean" is simply a normal Java object managed by Spring.

---

## 🗺️ Table of Contents

- [1. The OOP Mental Model in AI Engineering](#1-the-oop-mental-model-in-ai-engineering)
- [2. Anatomy of a Java Class](#2-anatomy-of-a-java-class)
  - [2.1 Fields, Methods & Constructors](#21-fields-methods--constructors)
  - [2.2 The `this` Keyword](#22-the-this-keyword)
- [3. Memory Deep Dive: Stack vs Heap](#3-memory-deep-dive-stack-vs-heap)
  - [3.1 The Desk vs Warehouse Analogy](#31-the-desk-vs-warehouse-analogy)
  - [3.2 Visualizing Memory Allocation](#32-visualizing-memory-allocation)
  - [3.3 Pass-by-Value: The Truth About Java Parameters](#33-pass-by-value-the-truth-about-java-parameters)
- [4. Encapsulation & Access Modifiers](#4-encapsulation--access-modifiers)
  - [4.1 The 4 Access Modifiers](#41-the-4-access-modifiers)
  - [4.2 Why Getters/Setters Matter for AI Safety](#42-why-getterssetters-matter-for-ai-safety)
- [5. The Sacred Contract: `equals()` & `hashCode()`](#5-the-sacred-contract-equals--hashcode)
  - [5.1 Reference Equality (`==`) vs Logical Equality (`equals`)](#51-reference-equality--vs-logical-equality-equals)
  - [5.2 The `hashCode()` Contract](#52-the-hashcode-contract)
  - [5.3 The Catastrophic HashMap Bug](#53-the-catastrophic-hashmap-bug)
- [6. The `toString()` Method & Debugging](#6-the-tostring-method--debugging)
- [7. Garbage Collection (GC) Fundamentals](#7-garbage-collection-gc-fundamentals)
- [8. What is a "Spring Bean" Really?](#8-what-is-a-spring-bean-really)
- [9. Key Takeaways & Summary](#9-key-takeaways--summary)
- [10. Practice Exercises & Full Solutions](#10-practice-exercises--full-solutions)
- [11. Self-Check Quiz](#11-self-check-quiz)

---

# 1. The OOP Mental Model in AI Engineering

In Generative AI, everything you manipulate is a real-world concept with **state** (data) and **behavior** (actions):

| AI Concept | State (Fields / Data) | Behavior (Methods / Actions) |
| :--- | :--- | :--- |
| **`ChatMessage`** | `role` (user/system), `content`, `timestamp`, `tokenCount` | `calculateTokens()`, `isSystemPrompt()`, `toFormattedText()` |
| **`EmbeddingVector`** | `float[] values`, `dimensions` (e.g. 1536), `modelName` | `cosineSimilarity(other)`, `magnitude()`, `normalize()` |
| **`DocumentChunk`** | `id`, `text`, `metadata` (author, source, page), `embedding` | `matchesFilter(query)`, `wordCount()`, `truncate(maxTokens)` |
| **`LLMClient`** | `apiKey`, `endpointUrl`, `timeoutMs`, `defaultModel` | `chat(prompt)`, `generateStream(prompt)`, `countTokens(text)` |

Without OOP, your program would consist of loose strings and detached arrays floating around with zero structure. OOP binds the **data** and the **operations on that data** into cohesive, self-protecting units called **Objects**.

---

# 2. Anatomy of a Java Class

A **Class** is the blueprint. An **Object** is the physical entity built from that blueprint.

### Real-World Analogy: Cookie Cutter vs. Cookies
- **Class**: The steel cookie cutter. You cannot eat it; it has no calories. It simply defines the shape, dimensions, and outline of what a cookie *will* be.
- **Object (Instance)**: The actual baked cookie made of dough, sugar, and chocolate chips sitting on your plate. You can bake 1,000 cookies from a single cutter. Each cookie has its own sprinkles and weight, but they all share the same structural pattern.

```
                  ┌───────────────────────────────┐
                  │      CLASS: ChatMessage       │  ◄── (The Blueprint)
                  │  - role: String               │
                  │  - content: String            │
                  │  - tokenCount: int            │
                  └──────────────┬────────────────┘
                                 │
                 ┌───────────────┴───────────────┐
      new ChatMessage(...)              new ChatMessage(...)
                 ▼                               ▼
  ┌─────────────────────────────┐ ┌─────────────────────────────┐
  │      OBJECT INSTANCE 1      │ │      OBJECT INSTANCE 2      │
  │  role: "user"               │ │  role: "assistant"          │
  │  content: "What is RAG?"    │ │  content: "RAG stands for.."│
  │  tokenCount: 6              │ │  tokenCount: 42             │
  └─────────────────────────────┘ └─────────────────────────────┘
```

---

### 2.1 Fields, Methods & Constructors

Let's look at a complete, production-grade Java class modeling an AI chat message:

```java
package com.javagenai.day02;

public class ChatMessage {

    // 1. Fields (State / Attributes)
    private String role;
    private String content;
    private int tokenCount;

    // 2. Constructor (Initialization logic)
    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
        this.tokenCount = estimateTokens(content);
    }

    // 3. Methods (Behavior / Capabilities)
    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public int getTokenCount() {
        return tokenCount;
    }

    // Business logic method
    public boolean isSystemMessage() {
        return "system".equalsIgnoreCase(this.role);
    }

    // Helper method (private because internal detail)
    private int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        // Approximate heuristic: ~4 characters per token in English
        return (int) Math.ceil(text.length() / 4.0);
    }
}
```

---

### 2.2 The `this` Keyword

Notice line 12: `this.role = role;`. Why is `this` needed?
- In the constructor parameter list: `(String role, String content)`, the variable names `role` and `content` shadow the class field names.
- `role = role;` would assign the parameter to itself (doing nothing to the object!).
- `this.role` explicitly means: **"The `role` field belonging to THIS current object instance in memory."**

```java
public ChatMessage(String role, String content) {
    this.role = role;       // this.role = class field; role = incoming argument
    this.content = content; // this.content = class field; content = incoming argument
}
```

---

# 3. Memory Deep Dive: Stack vs Heap

When your Java application runs, the JVM partitions your operating system RAM into distinct memory zones. The two most critical are the **Stack** and the **Heap**.

### 3.1 The Desk vs Warehouse Analogy

```
┌──────────────────────────────────────┐  ┌──────────────────────────────────────┐
│             THE STACK                │  │               THE HEAP               │
│          (Your Office Desk)          │  │         (The Central Warehouse)      │
├──────────────────────────────────────┤  ├──────────────────────────────────────┤
│ • Small, ultra-fast workspace        │  │ • Massive, shared storage facility   │
│ • Holds items for the immediate task │  │ • Holds large objects and payloads    │
│ • Cleared instantly when task ends   │  │ • Managed by warehouse staff (GC)    │
│ • Each thread has its OWN desk       │  │ • Shared by ALL threads in the app  │
└──────────────────────────────────────┘  └──────────────────────────────────────┘
```

- **Stack**: Stores **primitive values** (`int`, `double`, `boolean`, `char`) and **object reference pointers** (memory addresses pointing to where the object actually lives). Every method call creates a new **Stack Frame**. When the method finishes (`return`), its frame is immediately popped off and erased.
- **Heap**: Stores **all actual Objects** created with `new` (e.g., `new ChatMessage()`, `new String()`, `new ArrayList()`). Objects live in the Heap as long as someone has a reference to them.

---

### 3.2 Visualizing Memory Allocation

Let's trace this code snippet:

```java
public void processAIRequest() {
    int maxTokens = 500;
    ChatMessage msg = new ChatMessage("user", "Hello LLM");
}
```

Here is exactly what happens in RAM:

```
          STACK MEMORY (Thread 1)                          HEAP MEMORY (Shared)
┌──────────────────────────────────────────┐    ┌─────────────────────────────────────────┐
│ [Stack Frame: processAIRequest()]        │    │                                         │
│                                          │    │  Address: 0x7A4F                        │
│  maxTokens: 500  (primitive int value)   │    │  ┌───────────────────────────────────┐  │
│                                          │    │  │ Object: ChatMessage               │  │
│  msg: 0x7A4F ────────────────────────────┼───►│  │   role: 0x9B10 ──► "user"         │  │
│  (Reference Pointer)                     │    │  │   content: 0x3C81 ──► "Hello LLM" │  │
│                                          │    │  │   tokenCount: 3                   │  │
└──────────────────────────────────────────┘    │  └───────────────────────────────────┘  │
                                                └─────────────────────────────────────────┘
```

1. `int maxTokens = 500`: Primitive value stored **directly inside the Stack frame**.
2. `ChatMessage msg`: A variable created on the Stack to hold a **reference address** (`0x7A4F`).
3. `new ChatMessage(...)`: The `new` keyword instructs the JVM: *"Allocate a chunk of memory on the HEAP, run the constructor to populate fields, and return its memory address."*
4. When `processAIRequest()` completes:
   - The Stack Frame is destroyed. `maxTokens` and `msg` are gone.
   - The `ChatMessage` object on the Heap is now **unreachable** (no one points to `0x7A4F`).
   - The **Garbage Collector** will soon sweep through and reclaim its memory!

---

### 3.3 Pass-by-Value: The Truth About Java Parameters

> [!IMPORTANT]
> **Java is STRICTLY Pass-by-Value. Always. Without exception.**

Many developers mistakenly believe "objects are passed by reference in Java." **They are not.** 
In Java:
- Primitives pass a copy of their **bits**.
- Objects pass a copy of their **reference address bits**.

Let's prove this with an AI example:

```java
public class ParameterTest {

    public static void modifyTokens(int tokens) {
        tokens = 9999; // Modifies the LOCAL stack copy only
    }

    public static void updateContent(ChatMessage message) {
        // message points to the same heap object as caller
        message.setContent("Updated Prompt"); 
    }

    public static void reassignMessage(ChatMessage message) {
        // Reassigns the LOCAL reference copy to a NEW heap object
        message = new ChatMessage("system", "Brand New Prompt");
    }

    public static void main(String[] args) {
        int tokenLimit = 100;
        modifyTokens(tokenLimit);
        System.out.println(tokenLimit); // Prints 100! (Original was NOT changed)

        ChatMessage msg = new ChatMessage("user", "Original Prompt");
        updateContent(msg);
        System.out.println(msg.getContent()); // Prints "Updated Prompt"! (Field changed)

        reassignMessage(msg);
        System.out.println(msg.getContent()); // STILL prints "Updated Prompt"! (Caller pointer did NOT change)
    }
}
```

```
Caller msg (0x7A4F) ───► [ Heap Object ] ◄─── Callee message (copy of 0x7A4F)
```
When `reassignMessage` executes:
`message = new ChatMessage(...)` changes the callee's copy to point to `0x9999`. The caller's `msg` still points to `0x7A4F`!

---

# 4. Encapsulation & Access Modifiers

**Encapsulation** means hiding internal implementation details and exposing only safe, validated entry points.

### Real-World Analogy: The ATM Machine
Imagine a bank with no ATM or tellers—just an open vault with stacks of cash. Anyone could walk in, take money, and write whatever balance they wanted on a whiteboard. Chaos!

An **ATM** encapsulates the bank's vault:
- The cash inside is `private`.
- The keypad is `public`.
- When you request \$100 via `withdraw(100)`, the ATM verifies your PIN, checks if you have sufficient funds, ensures the vault has cash, and *then* updates your balance.

---

### 4.1 The 4 Access Modifiers

| Modifier | Keyword | Visible To Same Class | Visible To Same Package | Visible To Subclasses | Visible To Whole World |
| :--- | :--- | :---: | :---: | :---: | :---: |
| **Public** | `public` | ✅ | ✅ | ✅ | ✅ |
| **Protected** | `protected` | ✅ | ✅ | ✅ | ❌ |
| **Package-Private** | *(default / none)* | ✅ | ✅ | ❌ | ❌ |
| **Private** | `private` | ✅ | ❌ | ❌ | ❌ |

---

### 4.2 Why Getters/Setters Matter for AI Safety

Consider model temperature in an LLM call. Temperature must strictly range between `0.0` (deterministic) and `2.0` (creative):

```java
// BAD: Unprotected public field
public class ModelConfig {
    public double temperature; // Anyone can set this to -50.0 or 999.0, crashing the API!
}

// GOOD: Encapsulated with validation
public class ModelConfig {
    private double temperature = 0.7; // Sensible default

    public double getTemperature() {
        return this.temperature;
    }

    public void setTemperature(double temperature) {
        if (temperature < 0.0 || temperature > 2.0) {
            throw new IllegalArgumentException("Temperature must be between 0.0 and 2.0. Received: " + temperature);
        }
        this.temperature = temperature;
    }
}
```

---

# 5. The Sacred Contract: `equals()` & `hashCode()`

This is one of the **most tested interview questions** and the source of subtle production bugs in Spring Boot and Spring AI applications.

### 5.1 Reference Equality (`==`) vs Logical Equality (`equals`)

In Java:
- `==` checks **memory address identity** (Does variable A point to the exact same memory location as variable B?).
- `equals()` checks **content value equality** (Do these two distinct objects have the same data?).

```java
String prompt1 = new String("Summarize this document");
String prompt2 = new String("Summarize this document");

System.out.println(prompt1 == prompt2);      // FALSE! (Two different heap memory addresses)
System.out.println(prompt1.equals(prompt2));  // TRUE! (Both hold identical characters)
```

By default, the root `java.lang.Object` class implements `equals()` using `==`:
```java
// Default Object implementation:
public boolean equals(Object obj) {
    return (this == obj);
}
```
If you do NOT override `equals()` in your custom classes, two separate `Document` objects with identical text will be treated as completely different!

---

### 5.2 The `hashCode()` Contract

A **hash code** is an integer produced by a mathematical hashing formula applied to an object's fields. Think of it as a **fast fingerprint** of the object.

Hash-based data structures (like `HashMap`, `HashSet`, and in-memory Vector Indexes) use this integer to instantly locate the bucket where an object belongs in $O(1)$ constant time.

```
                         ┌──────────────────────────────────────────────────┐
                         │              THE SACRED CONTRACT                 │
                         ├──────────────────────────────────────────────────┤
                         │ 1. If a.equals(b) is TRUE,                       │
                         │    then a.hashCode() MUST EQUAL b.hashCode().    │
                         │                                                  │
                         │ 2. If a.hashCode() == b.hashCode(),              │
                         │    a.equals(b) is NOT necessarily true (Hash     │
                         │    collision).                                   │
                         │                                                  │
                         │ 3. If you override equals(), you MUST override   │
                         │    hashCode()!                                   │
                         └──────────────────────────────────────────────────┘
```

### Real-World Analogy: Airport Luggage Sorting
- **`hashCode()`**: The flight destination code on your luggage tag (`JFK`). The conveyor belt quickly routes your bag to the JFK carousel with thousands of other JFK bags.
- **`equals()`**: When you pick up a black suitcase from the JFK carousel, you check the name tag and passport ID (`equals`) to verify it is *your* exact suitcase, not someone else's identical-looking black bag.

If two bags belong to the same passenger (`equals() == true`) but receive different airport codes (`hashCode() != hashCode()`), your bag will end up in Tokyo while you are in New York!

---

### 5.3 The Catastrophic HashMap Bug

Let's see what happens in an AI semantic caching system when `hashCode()` is forgotten:

```java
package com.javagenai.day02;

import java.util.Objects;

public class PromptKey {
    private String model;
    private String promptText;

    public PromptKey(String model, String promptText) {
        this.model = model;
        this.promptText = promptText;
    }

    // Overrode equals, but FORGOT to override hashCode!
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PromptKey promptKey = (PromptKey) o;
        return Objects.equals(model, promptKey.model) &&
               Objects.equals(promptText, promptKey.promptText);
    }
}
```

Now let's use it as a cache key in a `HashMap`:

```java
Map<PromptKey, String> aiCache = new HashMap<>();

PromptKey key1 = new PromptKey("gpt-4o", "What is Java?");
aiCache.put(key1, "Java is a class-based programming language...");

// Later, another request arrives with the identical model and prompt:
PromptKey key2 = new PromptKey("gpt-4o", "What is Java?");

System.out.println("Are keys equal? " + key1.equals(key2)); // TRUE!
System.out.println("Cache result  : " + aiCache.get(key2));   // NULL! 🚨 BUG!
```

**Why did `aiCache.get(key2)` return `null` even though `key1.equals(key2)` is `true`?**
1. Because `hashCode()` was not overridden, the JVM used `System.identityHashCode()` based on raw memory addresses.
2. `key1` had hash code `142857` (placed in Bucket 5).
3. `key2` had hash code `857142` (looked for in Bucket 12).
4. Bucket 12 was empty! The HashMap never even called `equals()`. Your cache failed, and you wasted money making a redundant LLM call.

#### The Fix: Always Override Both!

```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PromptKey that = (PromptKey) o;
    return Objects.equals(model, that.model) && 
           Objects.equals(promptText, that.promptText);
}

@Override
public int hashCode() {
    return Objects.hash(model, promptText);
}
```
Now, `aiCache.get(key2)` returns the cached response instantly in $O(1)$ time!

---

# 6. The `toString()` Method & Debugging

When you print an object in Java:
```java
ChatMessage msg = new ChatMessage("user", "Hello");
System.out.println(msg);
```
Without overriding `toString()`, Java outputs:
`com.javagenai.day02.ChatMessage@4f3f5b24` (Class name + `@` + hexadecimal hash code).

In production AI microservices, you need informative logs for OpenTelemetry and debugging:

```java
@Override
public String toString() {
    return String.format("ChatMessage[role='%s', tokens=%d, content='%s']", 
                         this.role, this.tokenCount, this.content);
}
```
Output:
`ChatMessage[role='user', tokens=2, content='Hello']`

---

# 7. Garbage Collection (GC) Fundamentals

Unlike C or C++, Java does not have `malloc()` or `free()`. The JVM handles memory deallocation automatically via the **Garbage Collector (GC)**.

### How Does the GC Know What to Delete?
The GC uses **Reachability Analysis**. It starts from a set of known active roots (called **GC Roots**):
1. Variables in active Stack frames.
2. Static variables loaded in classes.
3. Active Thread objects.

The GC traces all pointers. Any object on the Heap that cannot be reached from any GC Root is deemed **garbage** and swept away.

```
 [Stack: activeThread] ──► [Prompt Object A] ──► [Embedding Vector B]  (REACHABLE - KEPT)

                           [Old Response C] (NO POINTERS REACH HERE)   (UNREACHABLE - COLLECTED)
```

> [!TIP]
> **AI Performance Tip**: In RAG and document chunking, avoid holding references to large PDF documents in `static` lists. Static fields live for the entire life of the JVM; holding references there prevents the GC from freeing megabytes of raw text!

---

# 8. What is a "Spring Bean" Really?

Throughout upcoming phases, you will hear the term **Spring Bean** hundreds of times. Beginners assume a Bean is a mysterious, magical construct.

**Demystifying the Spring Bean**:
> A Spring Bean is **just a normal Java Object (POJO)**. The only difference is that instead of YOU creating it with `new ChatService()`, the **Spring IoC Container** calls `new ChatService()` on your behalf, configures its fields, and stores it in memory for your app to use.

If you understand Java classes, objects, and constructors today, you already understand 80% of what a Spring Bean is!

---

# 9. Key Takeaways & Summary

```
                  ┌─────────────────────────────────┐
                  │       DAY 02 CHEAT SHEET        │
                  └────────────────┬────────────────┘
                                   │
         ┌─────────────────────────┼─────────────────────────┐
         ▼                         ▼                         ▼
  [ Memory Model ]         [ Object Design ]         [ Sacred Contract ]
  • Stack: Local primitives  • Class = Blueprint       • == checks memory address
    & reference pointers     • Object = Instance       • equals() checks content
  • Heap: All objects via    • this refers to current  • If a.equals(b), then
    new keyword                instance fields           a.hashCode() MUST equal
  • Strictly Pass-by-Value   • Encapsulate with          b.hashCode()
  • GC cleans unreachable      private fields and      • Always override both
    heap objects               public validated APIs     for Map/Set keys
```

---

# 10. Practice Exercises & Full Solutions

### 🏋️ Exercise 1: Build a Production-Grade `AIModelSpecification` Class
**Objective**: Create a class named `AIModelSpecification` that represents an LLM engine (e.g., `gpt-4o`, `claude-3-5-sonnet`, `llama-3.2`).

**Requirements**:
1. Fields: `modelId` (String), `contextWindowTokens` (int), `costPerMillionInputTokens` (double).
2. Constructor with validation: `modelId` cannot be null/empty, `contextWindowTokens` must be $> 0$, cost must be $\ge 0$.
3. Overridden `equals()` and `hashCode()` based on `modelId`.
4. Overridden `toString()`.
5. Helper method `calculateInferenceCost(int inputTokens)` returning USD cost.

#### Solution:
```java
package com.javagenai.day02;

import java.util.Objects;

public class AIModelSpecification {
    private final String modelId;
    private final int contextWindowTokens;
    private final double costPerMillionInputTokens;

    public AIModelSpecification(String modelId, int contextWindowTokens, double costPerMillionInputTokens) {
        if (modelId == null || modelId.isBlank()) {
            throw new IllegalArgumentException("modelId cannot be null or empty");
        }
        if (contextWindowTokens <= 0) {
            throw new IllegalArgumentException("contextWindowTokens must be positive. Received: " + contextWindowTokens);
        }
        if (costPerMillionInputTokens < 0) {
            throw new IllegalArgumentException("Cost cannot be negative. Received: " + costPerMillionInputTokens);
        }

        this.modelId = modelId;
        this.contextWindowTokens = contextWindowTokens;
        this.costPerMillionInputTokens = costPerMillionInputTokens;
    }

    public String getModelId() {
        return modelId;
    }

    public int getContextWindowTokens() {
        return contextWindowTokens;
    }

    public double getCostPerMillionInputTokens() {
        return costPerMillionInputTokens;
    }

    public double calculateInferenceCost(int inputTokens) {
        if (inputTokens < 0) {
            throw new IllegalArgumentException("Input tokens cannot be negative");
        }
        return (inputTokens / 1_000_000.0) * this.costPerMillionInputTokens;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AIModelSpecification that = (AIModelSpecification) o;
        return Objects.equals(modelId.toLowerCase(), that.modelId.toLowerCase());
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelId.toLowerCase());
    }

    @Override
    public String toString() {
        return String.format("AIModelSpecification[id='%s', contextWindow=%,d tokens, cost=$%.2f/1M]", 
                             modelId, contextWindowTokens, costPerMillionInputTokens);
    }
}
```

---

### 🏋️ Exercise 2: Vector Distance Calculator
**Objective**: Build a `Vector3D` class representing a 3-dimensional embedding coordinate $(x, y, z)$. Implement Euclidean distance and Dot Product methods.

#### Solution:
```java
package com.javagenai.day02;

public class Vector3D {
    private final double x;
    private final double y;
    private final double z;

    public Vector3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    // Dot product: (x1*x2) + (y1*y2) + (z1*z2)
    public double dotProduct(Vector3D other) {
        if (other == null) throw new IllegalArgumentException("Target vector cannot be null");
        return (this.x * other.x) + (this.y * other.y) + (this.z * other.z);
    }

    // Magnitude / Norm: sqrt(x^2 + y^2 + z^2)
    public double magnitude() {
        return Math.sqrt((x * x) + (y * y) + (z * z));
    }

    // Cosine similarity: (A . B) / (||A|| * ||B||)
    public double cosineSimilarity(Vector3D other) {
        double dot = this.dotProduct(other);
        double denom = this.magnitude() * other.magnitude();
        if (denom == 0.0) return 0.0;
        return dot / denom;
    }

    @Override
    public String toString() {
        return String.format("Vector3D(%.4f, %.4f, %.4f)", x, y, z);
    }
}
```

---

## 11. Self-Check Quiz

1. **Where does a `new ChatMessage(...)` object live in memory? Where does its variable reference live?**
   - *Answer*: The object itself lives on the **Heap**. The local reference variable pointing to that object lives in the **Stack frame** of the calling method.
2. **If Class A overrides `equals()` but does not override `hashCode()`, what specific data structures will misbehave?**
   - *Answer*: All hash-based collections: `HashMap`, `HashSet`, `Hashtable`, and `LinkedHashMap`. Objects that are logically equal will end up in different hash buckets and cannot be retrieved.
3. **What is the difference between `public` and `private`?**
   - *Answer*: `public` members can be accessed by any class anywhere in the application. `private` members can only be accessed from within the exact class in which they are declared.
4. **Is Java pass-by-reference or pass-by-value?**
   - *Answer*: Strictly **pass-by-value**. For objects, Java passes a copy of the memory address reference bits.
5. **What happens to objects on the Heap when no Stack variables or static fields point to them?**
   - *Answer*: They become unreachable and are automatically reclaimed by the Garbage Collector (GC).

---

<p align="center">
  <b>Congratulations on completing Day 02! 🎉</b><br>
  Tomorrow on <b>Day 03</b>, we conquer <b>Inheritance, Interfaces & Polymorphism</b>: The Contract System that powers all of Spring Boot, Spring AI's <code>ChatModel</code>, and interchangeable LLM providers!
</p>
