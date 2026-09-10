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

Hey there, friend! Welcome to Day 02. Yesterday, you ran your very first Java program and learned how `javac` compiles code into bytecode that runs anywhere on the JVM. That was your initiation — today is where things get truly exciting!

Today, we're diving into the heart of Java: **Object-Oriented Programming (OOP) and Memory Management**.

If you've played with Python or JavaScript before, you might think OOP is just "putting functions inside a class." But in Java, OOP is your best friend: it determines **how your computer's RAM stores your data, how objects protect themselves from bad input, and how Java prevents sneaky bugs from crashing your app in production**.

By the end of today, you will clearly understand:
- ✅ **Classes vs Objects**: The mental model of Blueprints vs Physical Cookies.
- ✅ **Stack vs Heap Memory**: Where variables and objects actually live inside your computer's RAM (Desk vs Warehouse analogy).
- ✅ **Pass-by-Value in Java**: The classic myth debunked once and for all with clear diagrams.
- ✅ **Encapsulation & Access Modifiers**: Guarding internal data (like model temperature and API keys).
- ✅ **The Sacred Contract**: Why `==` fails on objects, and how `equals()` and `hashCode()` make HashMaps and AI caches work.
- ✅ **Garbage Collection (GC)**: How Java automatically cleans up old AI prompts and vectors so your memory never runs out.
- ✅ **The Spring Bean Connection**: Demystifying what a "Spring Bean" actually is (spoiler: it's just a normal Java object managed by Spring!).

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

Before we write code, let's talk about what "Object-Oriented" actually means in the world of Generative AI.

When building AI applications with Java, you aren't just doing math. You are dealing with real concepts: messages sent by users, responses from the AI, document chunks loaded from a PDF, and connections to AI services like OpenAI or Claude.

> [!TIP]
> ### 💡 New Word Alert: Everyday AI Vocabulary
> Before we look at any code, let's get comfortable with terms you'll see in modern AI:
> 
> - **Prompt**: The message, instruction, or question you send to an AI model (e.g. *"Explain Java memory like I am 5"*).
> - **Token**: Think of a token as a bite-sized piece of a word (usually 3 to 4 characters in English). AI models don't read full sentences the way humans do; they break your sentence into tokens. For instance, the phrase *"Hello world"* is 2 tokens. AI providers charge you per token!
> - **LLM (Large Language Model)**: The AI engine itself (like GPT-4, Google Gemini, or Claude) that reads your prompt and generates intelligent text responses.
> - **Embedding / Vector**: Don't let this fancy math term scare you! An "embedding" is simply turning words or sentences into a list of numbers (e.g., `[0.12, -0.98, 0.45, ...]`). Why? Because computers can't understand meaning directly, but they can easily compare numbers! Sentences with similar meanings have numbers that are close to each other. In Java, an embedding vector is just a standard array of numbers: `float[]` or `double[]`.
> - **Document Chunk**: When an AI app searches through a 100-page PDF, you don't feed the entire PDF in one prompt. You chop it into small paragraphs called "chunks", convert them to numbers, and find the most relevant chunk.

In Java, we group the **data** (state) and the **actions** (behavior) into one neat, self-contained box called an **Object**:

| AI Concept in Java | State (Data / Fields) | Behavior (Actions / Methods) |
| :--- | :--- | :--- |
| **`ChatMessage`** | `role` (user/system), `content` (text), `tokenCount` (number) | `calculateTokens()`, `isSystemPrompt()`, `toFormattedText()` |
| **`EmbeddingVector`** | `float[] values` (the list of numbers), `modelName` | `cosineSimilarity(other)` (compares similarity), `magnitude()` |
| **`DocumentChunk`** | `id`, `text` (the paragraph), `pageNumber` | `wordCount()`, `truncate(maxTokens)` |
| **`LLMClient`** | `apiKey`, `endpointUrl`, `timeoutMs` | `chat(prompt)` (calls the AI API), `countTokens(text)` |

Without OOP, your program would have loose strings, disconnected integers, and floating arrays everywhere. OOP wraps them together so your code is clean, safe, and organized!

---

## 🧭 The Plain English Bridge: OOP & JVM Memory Demystified

Here's how to think about core Java memory concepts without getting overwhelmed by computer science jargon:

| Concept | What Most Beginners Think | What the JVM Actually Does | Plain English Analogy |
| :--- | :--- | :--- | :--- |
| **`Stack` Memory** | "Where Java runs everything." | Fast memory allocated per thread. Holds primitive numbers (`int`, `boolean`) and reference addresses (`0x7A4F`). | **Your Office Desk**: Small, super fast, cleared completely the second you finish your current task. |
| **`Heap` Memory** | "Where everything else goes." | Big shared storage area where all objects created with `new` live. | **The Amazon Warehouse**: Huge storage where boxes (objects) stay until the cleaning crew (Garbage Collector) throws out boxes nobody uses anymore. |
| **Pass-by-Value** | "Java passes objects by reference!" | **Java is strictly pass-by-value.** When passing an object to a method, Java copies the *memory address pointer*, not the object itself. | Giving your friend a **photocopy of your house address**, not photocopying the physical house. |
| **`==` vs `.equals()`** | "Both check if things are equal." | `==` checks if both sides point to the **exact same memory address**. `.equals()` checks if the **contents** inside match. | `==` asks: *"Are these the exact same physical $1 coin?"*<br>`.equals()` asks: *"Do these two different coins both have a value of $1?"* |
| **The `hashCode()` Contract** | "Something Eclipse or IntelliJ generates." | A quick integer number used by `HashMap` and `HashSet` to find items instantly. | **A Postal Zip Code**: Two envelopes sent to the exact same house MUST have the exact same zip code! |

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

> [!TIP]
> ### 💡 New Word Alert: LLM Temperature
> In Generative AI, **temperature** controls how creative or wild the AI model is allowed to be!
> - `temperature = 0.0`: Super strict, deterministic, and factual. Great for math, code generation, and financial reports.
> - `temperature = 0.7`: Balanced and natural (most chat assistants use this).
> - `temperature = 1.5+`: Wild, creative, and unpredictable. Fun for poems or brainstorming, but prone to hallucinating (making things up).
> 
> Most AI APIs (like OpenAI) strictly reject values outside `0.0` to `2.0`. If you send a negative number or `999.0`, your request immediately crashes!

Here's why encapsulation saves the day in real code:

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

This is one of the **most famous interview questions in Java** — and forgetting it causes sneaky bugs in Spring Boot and AI apps!

### 5.1 Reference Equality (`==`) vs Logical Equality (`equals`)

In Java:
- `==` checks **memory address identity** (Does variable A point to the exact same spot in RAM as variable B?).
- `equals()` checks **content value equality** (Do these two distinct objects have the same characters or data?).

```java
String prompt1 = new String("Summarize this document");
String prompt2 = new String("Summarize this document");

System.out.println(prompt1 == prompt2);      // FALSE! (Two different heap memory addresses)
System.out.println(prompt1.equals(prompt2));  // TRUE! (Both hold identical characters)
```

By default, Java's root `Object` class implements `equals()` using `==`:
```java
// Default Object implementation:
public boolean equals(Object obj) {
    return (this == obj);
}
```
If you do NOT override `equals()` in your custom classes, two separate `Document` objects with identical text will be treated as completely different objects!

---

### 5.2 The `hashCode()` Contract

A **hash code** is a simple integer number produced by running a mathematical formula on an object's fields. Think of it as a **quick fingerprint** of the object.

Hash-based collections (like `HashMap`, `HashSet`, and caches) use this integer to instantly jump to the exact "bucket" where an object is stored, without searching through thousands of items one by one ($O(1)$ time).

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
- **`hashCode()`**: The airport destination code stamped on your luggage tag (`JFK`). The conveyor belt quickly routes your bag to the JFK cart with other JFK bags.
- **`equals()`**: When you pick up a black suitcase from the JFK carousel, you check the name tag and passport ID (`equals`) to verify it's *your* exact suitcase, not someone else's identical-looking black bag.

If two bags belong to the same passenger (`equals() == true`) but receive different airport codes (`hashCode() != hashCode()`), your bag ends up in Tokyo while you are in New York!

---

### 5.3 The Catastrophic HashMap Bug

> [!TIP]
> ### 💡 Why do we need a Cache in AI apps?
> Calling an AI model (like GPT-4) takes 2-5 seconds and costs money for every single question. If 1,000 customers ask your bot *"What are your store hours?"*, you definitely don't want to call OpenAI 1,000 times!
> 
> Instead, you check a fast in-memory Java `HashMap` cache. If the answer is already there, you return it instantly (0.1 milliseconds) for $0.00!
> 
> But what happens if your cache key forgets `hashCode()`? Let's see:

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

**The Story**: In any enterprise AI system, you connect to multiple AI engines (like OpenAI's `gpt-4o`, Anthropic's `claude-3-5-sonnet`, or Meta's `llama-3.2`). Each model has different token limits and different pricing. Today, you'll build the class that keeps track of this data safely!

**Requirements**:
1. Fields: `modelId` (String), `contextWindowTokens` (int), `costPerMillionInputTokens` (double).
2. Constructor with validation: `modelId` cannot be null/empty, `contextWindowTokens` must be $> 0$, cost must be $\ge 0$.
3. Overridden `equals()` and `hashCode()` based on `modelId`.
4. Overridden `toString()` for clean logging.
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

**The Story**: Remember how we said AI models turn sentences into lists of numbers (embeddings) so they can compare meanings? 
In OpenAI or Spring AI, an embedding might have 1,536 numbers! But guess what? The math for 1,536 numbers is the exact same math as for 3 numbers: $(x, y, z)$.

- **Dot Product**: Multiplies matching coordinates together and adds them up.
- **Magnitude**: Calculates how far the vector arrow stretches from $(0, 0, 0)$.
- **Cosine Similarity**: Measures the angle between two vectors:
  - `1.0` = Exact same direction (identical meaning).
  - `0.0` = Completely unrelated (perpendicular).
  - `-1.0` = Exact opposite meaning.

Let's build a simple 3D Vector to see how it works in clean Java code!

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
  <b>Awesome job finishing Day 02! 🎉</b><br>
  You've got the foundation down: classes, memory layout, pass-by-value, and the famous <code>equals()</code>/<code>hashCode()</code> contract.<br>
  Tomorrow on <b>Day 03</b>, we'll explore <b>Inheritance, Interfaces & Polymorphism</b> — the exact design system that powers Spring Boot and Spring AI's plug-and-play AI model architecture! Keep up the great momentum!
</p>
