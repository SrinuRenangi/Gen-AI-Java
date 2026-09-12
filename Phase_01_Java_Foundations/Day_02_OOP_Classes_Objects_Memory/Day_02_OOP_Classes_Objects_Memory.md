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

## 🗺️ Table of Contents
- [1. Topic Overview](#1-topic-overview)
- [2. Basic Foundations (True Zero)](#2-basic-foundations-true-zero)
  - [2.1 What is an Object and What is a Class?](#21-what-is-an-object-and-what-is-a-class)
  - [2.2 What is Computer Memory (RAM)?](#22-what-is-computer-memory-ram)
  - [2.3 Minimal Working Example: A Simple AI Message](#23-minimal-working-example-a-simple-ai-message)
  - [2.4 Line-by-Line Code Breakdown](#24-line-by-line-code-breakdown)
- [3. Core Concept Walkthrough (Basic → Intermediate)](#3-core-concept-walkthrough-basic--intermediate)
  - [3.1 Anatomy of a Java Class: Fields, Methods, and `this`](#31-anatomy-of-a-java-class-fields-methods-and-this)
  - [3.2 Memory Architecture: Stack vs. Heap (The Desk vs. Warehouse)](#32-memory-architecture-stack-vs-heap-the-desk-vs-warehouse)
  - [3.3 Pass-by-Value: The Great Java Parameter Myth](#33-pass-by-value-the-great-java-parameter-myth)
  - [3.4 Encapsulation & Access Modifiers (Guarding AI Parameters)](#34-encapsulation--access-modifiers-guarding-ai-parameters)
  - [3.5 The Sacred Contract: `==` vs. `equals()` and `hashCode()`](#35-the-sacred-contract--vs-equals-and-hashcode)
  - [3.6 Debugging with `toString()`](#36-debugging-with-tostring)
- [4. Prerequisite & Supporting Concepts](#4-prerequisite--supporting-concepts)
  - [Prerequisite / Supporting Concept: Memory Addresses & Pointers](#prerequisite--supporting-concept-memory-addresses--pointers)
  - [Prerequisite / Supporting Concept: Hash Tables & Bucket Lookups](#prerequisite--supporting-concept-hash-tables--bucket-lookups)
  - [Prerequisite / Supporting Concept: What is a Spring Bean Really?](#prerequisite--supporting-concept-what-is-a-spring-bean-really)
- [5. Advanced Depth (Intermediate → Advanced)](#5-advanced-depth-intermediate--advanced)
  - [5.1 Garbage Collection (GC) Reachability & Root Tracing](#51-garbage-collection-gc-reachability--root-tracing)
  - [5.2 Common Mistakes & Misconceptions (With Bad vs. Good Code)](#52-common-mistakes--misconceptions-with-bad-vs-good-code)
  - [5.3 Performance & Architectural Trade-offs: Object Overhead vs. Primitives](#53-performance--architectural-trade-offs-object-overhead-vs-primitives)
- [6. Quick Recap](#6-quick-recap)
- [7. Self-Check Questions & Practice Exercises](#7-self-check-questions--practice-exercises)
  - [Self-Check Questions (Basic to Advanced)](#self-check-questions-basic-to-advanced)
  - [Hands-On Practice Exercises with Full Solutions](#hands-on-practice-exercises-with-full-solutions)

---

# 1. Topic Overview

Object-Oriented Programming (OOP) in Java models real-world concepts by packaging **state** (data variables) and **behavior** (executable functions) into cohesive units called **Objects**. In tandem with OOP, the JVM organizes runtime memory into two distinct regions: the thread-local **Stack** and the shared **Heap**.

### Why This Topic Matters
In production Generative AI, applications manage high volumes of dynamic state: active user chat sessions, system instructions, document chunks, and mathematical vector embeddings. Understanding how Java objects are instantiated, where they reside in RAM, how references behave across method boundaries, and how the `equals()` and `hashCode()` contract controls hash-based caches prevents memory leaks and subtle runtime failures.

> 💡 **New Word Alert — "Prompt"**: The text query or instruction sent to an AI model (e.g., *"Summarize this financial report"*).

> 💡 **New Word Alert — "Embedding / Vector"**: An array of floating-point numbers representing the semantic meaning of a word or paragraph (e.g., `[0.12, -0.98, 0.45, ...]`). Computers compare these numbers to find documents with similar meanings.

> 💡 **New Word Alert — "Document Chunk"**: A segment or paragraph extracted from a large document (like a 100-page PDF) so it fits into an AI model's context window.

---

# 2. Basic Foundations (True Zero)

Let's start from the absolute ground level with no assumed prior knowledge.

### 2.1 What is an Object and What is a Class?

Imagine baking chocolate chip cookies:
- The **Class** is the **Cookie Cutter**. It is an inanimate steel mold. You cannot eat it; it has no calories. It merely defines the outline, dimensions, and shape of any cookie made with it.
- The **Object** (or **Instance**) is the **Baked Cookie**. It is real, made of physical dough and sugar, and sits on your plate. You can bake 500 cookies from one cutter. Each cookie can have slightly different chocolate chip patterns, but all share the same shape.

In software:
- A **Class** is the blueprint you write in a `.java` file.
- An **Object** is the real entity allocated in your computer's RAM when you run the `new` keyword.

---

### 2.2 What is Computer Memory (RAM)?

Your computer has Random Access Memory (RAM). Think of RAM as a giant grid of numbered mailboxes. Each mailbox has a unique numeric address (e.g., `0x7A4F`). When a program runs, it stores numbers, letters, and images inside these mailboxes.

The JVM splits this RAM into two primary areas:
1. **The Stack (Your Office Desk)**: A small, ultra-fast workspace. It holds variables for the exact task you are doing right now. When the task ends, your desk is wiped clean immediately.
2. **The Heap (The Amazon Warehouse)**: A vast storage warehouse. When you create large items (like an AI message or a list of documents), they are stored in the warehouse. You keep a small slip of paper on your desk containing the warehouse aisle number (the memory address).

---

### 2.3 Minimal Working Example: A Simple AI Message

Let's write a minimal, fully runnable Java program that creates and prints a message:

```java
public class SimpleMessageApp {

    // 1. Blueprint for an AI message
    static class SimpleMessage {
        String role;
        String content;

        // Constructor: initializes the object's data
        SimpleMessage(String r, String c) {
            role = r;
            content = c;
        }

        // Method: displays the message
        void display() {
            System.out.println("[" + role + "]: " + content);
        }
    }

    public static void main(String[] args) {
        // Create an object instance in RAM using 'new'
        SimpleMessage userPrompt = new SimpleMessage("user", "What is Java?");
        userPrompt.display();
    }
}
```

---

### 2.4 Line-by-Line Code Breakdown

Let's trace every instruction in the program above:

1. `static class SimpleMessage`: Declares our blueprint named `SimpleMessage`.
2. `String role; String content;`: Fields (state). Every message object will store who sent it (`role`) and what it says (`content`).
3. `SimpleMessage(String r, String c)`: The **Constructor**. When you bake a new cookie, you supply ingredients. Here, we supply the role and text.
4. `role = r; content = c;`: Assigns the incoming parameters to the object's fields.
5. `void display()`: A method (behavior) that prints the state to the terminal.
6. `SimpleMessage userPrompt = new SimpleMessage("user", "What is Java?");`:
   - `new SimpleMessage(...)`: Requests a block of memory in the **Heap**, builds the object, and populates `role` and `content`.
   - `userPrompt`: A variable created on the **Stack** that stores the memory address pointing to the object on the Heap.
7. `userPrompt.display();`: Follows the memory pointer to the object in the Heap and triggers its `display` method.

---

# 3. Core Concept Walkthrough (Basic → Intermediate)

Now let's ramp up our knowledge to production-grade Java OOP.

### 3.1 Anatomy of a Java Class: Fields, Methods, and `this`

Here is an enterprise-grade class representing an AI chat message with encapsulation and token estimation:

```java
package com.javagenai.day02;

public class ChatMessage {

    // 1. Encapsulated Fields (State)
    private String role;
    private String content;
    private int tokenCount;

    // 2. Constructor
    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
        this.tokenCount = estimateTokens(content);
    }

    // 3. Getters (Controlled access)
    public String getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public int getTokenCount() {
        return tokenCount;
    }

    public void setContent(String content) {
        this.content = content;
        this.tokenCount = estimateTokens(content);
    }

    // 4. Business Logic Method
    public boolean isSystemMessage() {
        return "system".equalsIgnoreCase(this.role);
    }

    // 5. Private Helper Method
    private int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        // Approximate rule: ~4 English characters per token
        return (int) Math.ceil(text.length() / 4.0);
    }
}
```

#### The Role of `this`:
In `public ChatMessage(String role, String content)`, the parameter names `role` and `content` match the field names.
- Writing `role = role;` assigns the argument back to itself and leaves the field uninitialized!
- `this.role` explicitly instructs the compiler: *"Store the argument into the `role` field of THIS specific instance in memory."*

---

### 3.2 Memory Architecture: Stack vs. Heap (The Desk vs. Warehouse)

```
          STACK MEMORY (Thread 1)                          HEAP MEMORY (Shared)
┌──────────────────────────────────────────┐    ┌─────────────────────────────────────────┐
│ [Stack Frame: processAIRequest()]        │    │                                         │
│                                          │    │  Address: 0x7A4F                        │
│  maxTokens: 500  (primitive int value)   │    │  ┌───────────────────────────────────┐  │
│                                          │    │  │ Object: ChatMessage               │  │
│  msg: 0x7A4F ────────────────────────────┼───►│  │   role: "user"                    │  │
│  (Reference Pointer)                     │    │  │   content: "Hello LLM"            │  │
│                                          │    │  │   tokenCount: 3                   │  │
└──────────────────────────────────────────┘    │  └───────────────────────────────────┘  │
                                                └─────────────────────────────────────────┘
```

Consider this method:
```java
public void processAIRequest() {
    int maxTokens = 500;
    ChatMessage msg = new ChatMessage("user", "Hello LLM");
}
```

What happens in RAM?
1. **Stack**: An execution frame for `processAIRequest()` is created.
   - The primitive variable `int maxTokens = 500` is stored directly inside the Stack frame.
   - The reference variable `ChatMessage msg` is created on the Stack.
2. **Heap**: The operator `new` allocates storage in the Heap at address `0x7A4F`. The fields `"user"`, `"Hello LLM"`, and `3` are stored there.
3. The address `0x7A4F` is assigned to `msg` on the Stack.
4. When `processAIRequest()` returns, its Stack frame is instantly cleared. The address `0x7A4F` is gone from the desk. The object in the Heap is now unreachable, ready for the Garbage Collector.

---

### 3.3 Pass-by-Value: The Great Java Parameter Myth

> [!IMPORTANT]
> **Java is strictly Pass-by-Value. Always and without exception.**

Many developers say: "Primitives are pass-by-value, but objects are pass-by-reference." **That is false.**
- For primitives, Java copies the **data value**.
- For objects, Java copies the **reference address bits**.

Let's prove this with an experiment:

```java
public class ParameterProof {

    public static void modifyPrimitive(int tokens) {
        tokens = 9999; // Modifies local stack copy only
    }

    public static void modifyObjectField(ChatMessage message) {
        // Copies address pointer. Both caller and callee point to SAME heap object!
        message.setContent("Updated Prompt"); 
    }

    public static void reassignReference(ChatMessage message) {
        // Points callee's local reference to a BRAND NEW object in heap
        message = new ChatMessage("system", "Brand New Prompt");
    }

    public static void main(String[] args) {
        int limit = 100;
        modifyPrimitive(limit);
        System.out.println(limit); // Prints 100! (Primitive copy modified, original untouched)

        ChatMessage chat = new ChatMessage("user", "Original Prompt");
        modifyObjectField(chat);
        System.out.println(chat.getContent()); // Prints "Updated Prompt"! (Field in heap modified)

        reassignReference(chat);
        System.out.println(chat.getContent()); // STILL prints "Updated Prompt"!
        // reassignReference only changed its local pointer; caller's 'chat' still points to original!
    }
}
```

---

### 3.4 Encapsulation & Access Modifiers (Guarding AI Parameters)

Encapsulation ensures that an object maintains control over its internal state.

| Modifier | Keyword | Same Class | Same Package | Subclasses | Everywhere |
| :--- | :--- | :---: | :---: | :---: | :---: |
| **Public** | `public` | ✅ | ✅ | ✅ | ✅ |
| **Protected** | `protected` | ✅ | ✅ | ✅ | ❌ |
| **Package-Private** | *(no keyword)* | ✅ | ✅ | ❌ | ❌ |
| **Private** | `private` | ✅ | ❌ | ❌ | ❌ |

#### Real-World Example: LLM Temperature
In AI, `temperature` controls how creative the model is (valid range: `0.0` to `2.0`). Values outside this range will crash the OpenAI or Anthropic API.

```java
// ❌ BAD: Unprotected public field
public class UnsafeConfig {
    public double temperature; // Anyone can assign -50.0 or 999.0!
}

// ✅ GOOD: Encapsulated with validation
public class SafeModelConfig {
    private double temperature = 0.7; // Safe default

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

### 3.5 The Sacred Contract: `==` vs. `equals()` and `hashCode()`

- **`==` (Reference Equality)**: Tests if two variables point to the **exact same memory address** in the Heap.
- **`equals()` (Logical Equality)**: Tests if two distinct objects contain the **same data values**.

```java
String prompt1 = new String("Explain RAG");
String prompt2 = new String("Explain RAG");

System.out.println(prompt1 == prompt2);      // FALSE! Two different heap allocations.
System.out.println(prompt1.equals(prompt2));  // TRUE! Identical character sequences.
```

#### The `hashCode()` Contract
A **hash code** is an integer computed from an object's fields. Think of it as a **postal zip code**:
1. If `a.equals(b)` is `true`, then `a.hashCode()` **MUST** equal `b.hashCode()`.
2. If `a.hashCode() == b.hashCode()`, `a.equals(b)` may or may not be true (hash collision).
3. If you override `equals()`, you **must** override `hashCode()`.

#### The Catastrophic AI Cache Bug:
If you use a custom object as a key in a `HashMap` (e.g., an LLM response cache) and forget `hashCode()`:

```java
public class CacheKey {
    private String model;
    private String prompt;

    public CacheKey(String model, String prompt) {
        this.model = model;
        this.prompt = prompt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CacheKey that = (CacheKey) o;
        return Objects.equals(model, that.model) && Objects.equals(prompt, that.prompt);
    }
    // ❌ FORGOT hashCode()!
}
```

When you store `key1` and later query with identical `key2`:
- `key1` and `key2` get random, different default memory hash codes.
- The `HashMap` looks in the wrong bucket and returns `null`!
- Your application makes an expensive, duplicate \$0.05 LLM API call instead of reading from cache.

**The Fix:**
```java
@Override
public int hashCode() {
    return Objects.hash(model, prompt);
}
```

---

### 3.6 Debugging with `toString()`

By default, printing an object outputs `ClassName@hexHashcode` (e.g., `ChatMessage@4f3f5b24`), which is useless in logs. Always override `toString()`:

```java
@Override
public String toString() {
    return String.format("ChatMessage[role='%s', tokens=%d, content='%s']", 
                         this.role, this.tokenCount, this.content);
}
```

---

# 4. Prerequisite & Supporting Concepts

### Prerequisite / Supporting Concept: Memory Addresses & Pointers

In lower-level languages like C, developers directly manipulate memory addresses called pointers (`int* ptr = &val`).
In Java:
- Pointers exist internally as **object references**.
- You cannot perform pointer arithmetic (e.g., you cannot do `ptr + 4`).
- This design eliminates entire categories of security vulnerabilities like buffer overflows.

---

### Prerequisite / Supporting Concept: Hash Tables & Bucket Lookups

A `HashMap` provides $O(1)$ constant-time lookup using an array of buckets:
1. When you call `map.put(key, value)`, Java calls `key.hashCode()`.
2. An internal modulo operation determines the bucket index: `index = hashCode % bucketArray.length`.
3. The entry is placed in that bucket.
4. When you call `map.get(queryKey)`, Java jumps directly to `queryKey.hashCode() % length`. It scans only that bucket, using `equals()` to find the exact key.

---

### Prerequisite / Supporting Concept: What is a Spring Bean Really?

Throughout upcoming Spring and Spring AI lessons, you will encounter the term **Spring Bean**.
- A Spring Bean is **simply a standard Java object** created from a standard Java class.
- The only difference: instead of your code calling `new ChatService()`, the **Spring IoC Container** calls `new ChatService()` on your behalf and manages its lifecycle.

---

# 5. Advanced Depth (Intermediate → Advanced)

### 5.1 Garbage Collection (GC) Reachability & Root Tracing

Java eliminates manual memory deallocation (`free()`) through automated Garbage Collection.

```
 [Stack: activeThread] ──► [Prompt Object A] ──► [Embedding Vector B]  (REACHABLE - PRESERVED)

                           [Old Response C] (NO INCOMING POINTERS)     (UNREACHABLE - RECLAIMED)
```

The GC uses **Reachability Analysis**:
1. It begins at **GC Roots** (local Stack frame variables, static class variables, active threads).
2. It follows all object reference pointers.
3. Any object on the Heap that cannot be reached from any GC Root is unreachable and swept from memory.

> [!WARNING]
> **Static Reference Memory Leaks in AI Apps**:
> If you store large PDF documents or vector arrays in a `static List<Document> cache`, they will **never** be collected because static variables remain GC Roots as long as the application runs!

---

### 5.2 Common Mistakes & Misconceptions (With Bad vs. Good Code)

#### Mistake 1: Comparing Strings with `==`
**Bad Code:**
```java
// ❌ FAILS when strings come from user input, JSON payloads, or HTTP APIs
if (message.getRole() == "user") { 
    processUserPrompt();
}
```
**Correct Code:**
```java
// ✅ Compares character content accurately
if ("user".equalsIgnoreCase(message.getRole())) {
    processUserPrompt();
}
```

#### Mistake 2: Mutating Keys in a `HashMap`
If an object is used as a `HashMap` key and you change a field that affects its `hashCode()`, the object becomes permanently lost in the wrong bucket!

**Bad Practice:**
```java
class MutableKey {
    String query;
}
// If you modify key.query AFTER placing it in a HashMap, map.get(key) will return null!
```
**Rule**: Always make Map keys **immutable** using `final` fields or Java `record`s (covered on Day 05).

---

### 5.3 Performance & Architectural Trade-offs: Object Overhead vs. Primitives

Every Java object on the Heap has a hidden 12-to-16-byte **object header** (containing mark word and class metadata pointer).
- An `int` primitive takes **4 bytes**.
- An `Integer` object takes **16 to 24 bytes** (header + int value + padding).
- When storing 1,000,000 AI embeddings of 1,536 dimensions:
  - Using primitive arrays `float[]` takes $\approx 6$ MB of RAM.
  - Using boxed objects `List<Float>` takes $\approx 36$ MB of RAM (6x overhead!).

---

# 6. Quick Recap

| Concept | Key Takeaway |
| :--- | :--- |
| **Class vs. Object** | Class is the blueprint; Object is the allocated instance in RAM. |
| **Stack Memory** | Fast, stores local primitives and reference pointers; destroyed on method exit. |
| **Heap Memory** | Shared storage for all objects created with `new`; managed by Garbage Collection. |
| **Pass-by-Value** | Java always passes copies: primitives copy raw data; objects copy address pointers. |
| **`==` vs. `equals()`** | `==` compares RAM memory addresses; `equals()` compares contents. |
| **`hashCode()` Contract** | Equal objects must return identical hash codes for hash collections to function. |
| **Encapsulation** | Keep fields `private` and provide validated public getters/setters. |

---

# 7. Self-Check Questions & Practice Exercises

### Self-Check Questions (Basic to Advanced)

1. **Where does a `ChatMessage` object live in memory? Where does its local variable reference live?**
   - *Answer*: The object instance itself lives on the **Heap**. The local reference variable pointing to that object lives in the calling method's **Stack frame**.
2. **If Class A overrides `equals()` but does not override `hashCode()`, what specific collection breaks and why?**
   - *Answer*: `HashMap` and `HashSet`. Two logically equal keys will produce different hash codes and land in different buckets, causing `map.get(key)` to fail and return `null`.
3. **What happens in memory when a method parameter reassigns `message = new ChatMessage(...)`?**
   - *Answer*: Only the local copy of the reference address inside the method's Stack frame is updated. The caller's variable outside the method continues pointing to the original Heap object.
4. **Why is it considered dangerous to use mutable objects as keys in a `HashMap`?**
   - *Answer*: If a field used in `hashCode()` calculation is modified after the object is inserted into the map, its hash code changes. The map will search for it in a different bucket and fail to retrieve it.
5. **How does the JVM Garbage Collector determine whether an object can be deleted from the Heap?**
   - *Answer*: Through Reachability Analysis. If no path of reference pointers exists from any GC Root (Stack variables, static fields, active threads) to the object, it is marked as unreachable and collected.

---

### Hands-On Practice Exercises with Full Solutions

#### 🏋️ Exercise 1: Build a Production-Grade `AIModelSpecification` Class
**Objective**: Build an encapsulated class modeling an AI model (like `gpt-4o` or `claude-3-5-sonnet`) with strict validation, token cost calculations, and a complete `equals()` and `hashCode()` implementation.

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

#### 🏋️ Exercise 2: Vector Distance Calculator (Cosine Similarity)
**Objective**: Build a `Vector3D` class representing a 3-dimensional embedding vector, implementing dot product, magnitude, and cosine similarity calculations.

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

<p align="center">
  <b>Day 02 Complete! 🎉</b><br>
  Proceed to <b>Day 03</b>: <b>Inheritance, Interfaces & Polymorphism</b>.<br>
  <a href="../Day_03_Inheritance_Interfaces_Polymorphism/Day_03_Inheritance_Interfaces_Polymorphism.md"><b>Continue to Day 03 →</b></a>
</p>
