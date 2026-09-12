# 📦 Day 04: Generics, Collections & Data Structures
## List, Map, Set & PriorityQueue — The Essential Containers for All AI Data

| ⬅️ Previous Day | 📚 Course Hub | ➡️ Next Day |
|:---|:---:|---:|
| [← Day 03: Inheritance, Interfaces & Polymorphism](../Day_03_Inheritance_Interfaces_Polymorphism/Day_03_Inheritance_Interfaces_Polymorphism.md) | [All 60 Days Overview](../../README.md) | [Day 05: Modern Java — Records, Optional & Sealed Types →](../Day_05_Modern_Java_Records_Optional_Sealed/Day_05_Modern_Java_Records_Optional_Sealed.md) |

[![Phase](https://img.shields.io/badge/Phase_01-Java_Foundations-brightgreen.svg?style=for-the-badge)](../../README.md)
[![Day](https://img.shields.io/badge/Day-04_of_60-blue.svg?style=for-the-badge)](../../README.md)
[![Difficulty](https://img.shields.io/badge/Difficulty-Intermediate-blue.svg?style=for-the-badge)](../../README.md)
[![Topic](https://img.shields.io/badge/Core-Collections_and_Generics-red.svg?style=for-the-badge)](../../README.md)

---

## 🗺️ Table of Contents
- [1. Topic Overview](#1-topic-overview)
- [2. Basic Foundations (True Zero)](#2-basic-foundations-true-zero)
  - [2.1 What is a Collection and What are Generics?](#21-what-is-a-collection-and-what-are-generics)
  - [2.2 The Shipping Container Analogy](#22-the-shipping-container-analogy)
  - [2.3 Minimal Working Example: A Type-Safe AI Message History](#23-minimal-working-example-a-type-safe-ai-message-history)
  - [2.4 Line-by-Line Code Breakdown](#24-line-by-line-code-breakdown)
- [3. Core Concept Walkthrough (Basic → Intermediate)](#3-core-concept-walkthrough-basic--intermediate)
  - [3.1 Building Generic Classes: `AIResponse<T>`](#31-building-generic-classes-airesponset)
  - [3.2 Type Erasure: How the JVM Executes Generics](#32-type-erasure-how-the-jvm-executes-generics)
  - [3.3 Bounded Wildcards & the PECS Rule](#33-bounded-wildcards--the-pecs-rule)
  - [3.4 The Java Collections Framework Hierarchy](#34-the-java-collections-framework-hierarchy)
  - [3.5 `List<T>`: `ArrayList` vs. `LinkedList` & Document Chunking](#35-listt-arraylist-vs-linkedlist--document-chunking)
  - [3.6 `Set<T>`: Uniqueness and Deduplicating AI Chunks](#36-sett-uniqueness-and-deduplicating-ai-chunks)
  - [3.7 `Map<K, V>`: `HashMap` Internals & `ConcurrentHashMap`](#37-mapk-v-hashmap-internals--concurrenthashmap)
  - [3.8 `PriorityQueue<T>`: The Top-K Scoring Engine for RAG](#38-priorityqueuet-the-top-k-scoring-engine-for-rag)
  - [3.9 Modern Immutable Collections (`List.of`, `Map.of`)](#39-modern-immutable-collections-listof-mapof)
- [4. Prerequisite & Supporting Concepts](#4-prerequisite--supporting-concepts)
  - [Prerequisite / Supporting Concept: Arrays vs. Dynamic Collections](#prerequisite--supporting-concept-arrays-vs-dynamic-collections)
  - [Prerequisite / Supporting Concept: Autoboxing & Unboxing (Primitives vs. Wrappers)](#prerequisite--supporting-concept-autoboxing--unboxing-primitives-vs-wrappers)
  - [Prerequisite / Supporting Concept: Fail-Fast vs. Fail-Safe Iterators](#prerequisite--supporting-concept-fail-fast-vs-fail-safe-iterators)
- [5. Advanced Depth (Intermediate → Advanced)](#5-advanced-depth-intermediate--advanced)
  - [5.1 Java 8+ HashMap Internals: Treeification & Collisions](#51-java-8-hashmap-internals-treeification--collisions)
  - [5.2 High-Performance Map Operations: `computeIfAbsent()` & `merge()`](#52-high-performance-map-operations-computeifabsent--merge)
  - [5.3 Common Mistakes & Misconceptions (With Bad vs. Good Code)](#53-common-mistakes--misconceptions-with-bad-vs-good-code)
  - [5.4 Architectural Trade-Offs: Contiguous Memory (`ArrayList`) vs. Scattered Pointers (`LinkedList`)](#54-architectural-trade-offs-contiguous-memory-arraylist-vs-scattered-pointers-linkedlist)
- [6. Quick Recap](#6-quick-recap)
- [7. Self-Check Questions & Practice Exercises](#7-self-check-questions--practice-exercises)
  - [Self-Check Questions (Basic to Advanced)](#self-check-questions-basic-to-advanced)
  - [Hands-On Practice Exercises with Full Solutions](#hands-on-practice-exercises-with-full-solutions)

---

# 1. Topic Overview

The **Java Collections Framework (JCF)** provides standardized, high-performance data structures (`List`, `Set`, `Map`, `Queue`) for storing, sorting, filtering, and retrieving groups of objects in memory. In conjunction with collections, **Generics (`<T>`)** enforce strict compile-time type safety, ensuring that containers hold only permitted object types.

### Why This Topic Matters
In modern AI engineering, data rarely exists as single values. AI pipelines ingest batches of floating-point vectors (`List<float[]>`), maintain multi-turn chat dialogues (`List<ChatMessage>`), store prompt variables and metadata filters (`Map<String, Object>`), deduplicate scraped web chunks (`Set<String>`), and select the Top-3 most relevant passages via priority heaps (`PriorityQueue<ScoredChunk>`). Mastering generics and collections ensures your AI pipelines run with maximum speed and zero type-cast errors.

> 💡 **New Word Alert — "Generics (`<T>`)"**: A language feature allowing classes, interfaces, and methods to operate on specified data types while providing compile-time type verification.

> 💡 **New Word Alert — "Top-K"**: An information retrieval technique that returns only the highest-ranking $K$ items (e.g., the 3 best matching document passages for an AI prompt) from a large pool of thousands.

> 💡 **New Word Alert — "Deduplication"**: The process of identifying and removing redundant identical data chunks to reduce memory consumption and LLM token costs.

---

# 2. Basic Foundations (True Zero)

Let's begin with absolute basics, assuming no prior experience with data structures.

### 2.1 What is a Collection and What are Generics?

- **Collection**: A container object that groups multiple elements into a single unit (like a shopping cart holding grocery items).
- **Generics (`<T>`)**: A label placed on that container specifying exactly what kind of items are permitted inside (like labeling a crate *"Apples Only"*).

In dynamically typed languages like Python or JavaScript, a list can hold numbers, strings, and dictionaries simultaneously (`[1, "hello", True]`). While flexible, if a function expects a number and encounters a string, the application crashes at runtime.

Java uses **Generics** to verify types during compilation. If a container is labeled `List<String>`, attempting to insert a number fails before your code ever runs.

---

### 2.2 The Shipping Container Analogy

```
                  UNMARKED WOODEN CRATES (Raw Types / Ancient Java)
                  ┌──────────────────────────────────────────────┐
                  │ Contains: Bananas? Uranium? Dynamite? Glass? │
                  │ You only find out when you open it at home!  │
                  └──────────────────────────────────────────────┘
                                         vs.
                  STANDARDIZED STEEL CONTAINERS (Java Generics)
                  ┌──────────────────────────────────────────────┐
                  │ LABEL: "REFRIGERATED MEDICINE ONLY" <Vaccine>│
                  │ Port cranes reject any attempt to load coal  │
                  │ into it before the ship ever leaves dock!     │
                  └──────────────────────────────────────────────┘
```

Generics act as strict port authority guards: they verify the cargo type at the loading dock (compile-time) so that your ship never sinks at sea (runtime).

---

### 2.3 Minimal Working Example: A Type-Safe AI Message History

Let's write a minimal, fully runnable Java program managing a conversation history with generics:

```java
import java.util.ArrayList;
import java.util.List;

public class MessageHistoryDemo {

    public static void main(String[] args) {
        // 1. Declare a List that ONLY accepts String items
        List<String> chatHistory = new ArrayList<>();

        // 2. Add valid strings
        chatHistory.add("User: What is Java?");
        chatHistory.add("Assistant: Java is a class-based programming language.");

        // 3. The compiler blocks invalid types immediately:
        // chatHistory.add(12345); // COMPILE ERROR! Incompatible types.

        // 4. Retrieve and print items without manual casting
        for (String message : chatHistory) {
            System.out.println(message);
        }
    }
}
```

---

### 2.4 Line-by-Line Code Breakdown

1. `List<String> chatHistory = new ArrayList<>();`:
   - `List<String>`: The interface type, parameterized with `<String>`.
   - `new ArrayList<>()`: Instantiates a resizable array container in Heap memory. The diamond operator `<>` infers the type automatically.
2. `chatHistory.add(...)`: Appends elements to the end of the list in sequential order.
3. `// chatHistory.add(12345);`: Generics prevent invalid types from ever entering the list.
4. `for (String message : chatHistory)`: Iterates through elements directly as `String` with no manual `(String)` cast needed.

---

# 3. Core Concept Walkthrough (Basic → Intermediate)

Now let's build the generic containers and data structures used across production AI engineering.

### 3.1 Building Generic Classes: `AIResponse<T>`

In AI applications, an LLM call might return plain text (`String`), structured JSON parsed into a POJO (`Invoice`), or a numerical embedding vector (`float[]`).

Instead of creating separate classes for each, we create a generic container:

```java
package com.javagenai.day04;

import java.time.Instant;

// T represents an arbitrary payload type
public class AIResponse<T> {
    private final T payload;
    private final int promptTokens;
    private final int completionTokens;
    private final Instant createdAt;

    public AIResponse(T payload, int promptTokens, int completionTokens) {
        this.payload = payload;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.createdAt = Instant.now();
    }

    public T getPayload() {
        return payload;
    }

    public int getTotalTokens() {
        return promptTokens + completionTokens;
    }

    @Override
    public String toString() {
        return String.format("AIResponse[tokens=%d, payload=%s]", getTotalTokens(), payload);
    }
}
```

Using `AIResponse<T>` cleanly for different data types:

```java
// 1. Holding a String text response
AIResponse<String> textResp = new AIResponse<>("Paris is the capital of France.", 10, 8);
String text = textResp.getPayload(); // Pure String, zero casting!

// 2. Holding an Integer count
AIResponse<Integer> countResp = new AIResponse<>(42, 5, 2);
int count = countResp.getPayload(); // Auto-unboxed to primitive int!
```

---

### 3.2 Type Erasure: How the JVM Executes Generics

How does the JVM execute generics without breaking backward compatibility or bloating memory?

Through **Type Erasure**:
1. During compilation, `javac` verifies all types (e.g., ensuring only `String` is added to `List<String>`).
2. Once validated, the compiler **erases** `<String>` from the bytecode, replacing it with `Object` and inserting synthetic casts at call sites.
3. At runtime on the JVM, `List<String>` and `List<Integer>` both execute as raw `ArrayList.class`.

> [!NOTE]
> Because generic types are erased at runtime, you cannot instantiate generic types directly with `new T()` or `new T[10]`.

---

### 3.3 Bounded Wildcards & the PECS Rule

Suppose you want a method that calculates the sum of a list of numbers (integers, floats, or doubles):

```java
// ❌ COMPILE ERROR: List<Integer> is NOT a subtype of List<Number>!
public static double sum(List<Number> list) { ... }
```

To enable polymorphism across generic collections, Java provides **Wildcards (`?`)**:
- **`? extends T` (Upper Bounded)**: Accepts `T` or any subclass of `T`.
- **`? super T` (Lower Bounded)**: Accepts `T` or any superclass of `T`.

#### The PECS Rule: Producer Extends, Consumer Super
- **Producer (`extends`)**: If your method **reads data out** of the collection, use `? extends T`.
- **Consumer (`super`)**: If your method **writes data into** the collection, use `? super T`.

```java
// Reading data OUT of list (Producer -> extends)
public static double calculateMagnitude(List<? extends Number> numbers) {
    double sum = 0.0;
    for (Number n : numbers) {
        sum += n.doubleValue(); // Safe! Guaranteed to be a Number
    }
    return Math.sqrt(sum);
}
```

---

### 3.4 The Java Collections Framework Hierarchy

```
                            ┌────────────────────────┐
                            │ <<interface>> Iterable │
                            └───────────┬────────────┘
                                        │
                            ┌───────────▼────────────┐
                            │<<interface>> Collection│
                            └───────────┬────────────┘
                                        │
         ┌──────────────────────────────┼──────────────────────────────┐
         ▼                              ▼                              ▼
┌──────────────────┐           ┌──────────────────┐           ┌──────────────────┐
│<<interface>> List│           │ <<interface>> Set│           │<<interface>>Queue│
└────────┬─────────┘           └────────┬─────────┘           └────────┬─────────┘
         │                              │                              │
 ┌───────┴───────┐              ┌───────┴───────┐                      ▼
 ▼               ▼              ▼               ▼             ┌──────────────────┐
ArrayList   LinkedList       HashSet         TreeSet          │  PriorityQueue   │
                                                              └──────────────────┘

*Map<K,V> stands in its own separate hierarchy:
┌──────────────────┐
│<<interface>> Map │ ──► HashMap, TreeMap, LinkedHashMap, ConcurrentHashMap
└──────────────────┘
```

---

### 3.5 `List<T>`: `ArrayList` vs. `LinkedList` & Document Chunking

| Feature | `ArrayList<T>` | `LinkedList<T>` |
| :--- | :--- | :--- |
| **Backing Structure** | Contiguous resizable array | Scattered doubly-linked nodes |
| **Random Access (`get(i)`)**| **$O(1)$ Instant** | $O(N)$ Sequential scan |
| **CPU Cache Hits** | **Maximum** (sequential memory) | Poor (pointer chasing) |
| **Memory Overhead** | Lowest | High (24 bytes of pointer overhead per item) |
| **Industry Practice** | **Standard in 99.9% of applications** | Rarely used in modern high-performance Java |

#### Splitting a Document into Chunks with `ArrayList`:
```java
package com.javagenai.day04;

import java.util.ArrayList;
import java.util.List;

public class TextChunker {

    public static List<String> chunkText(String fullText, int chunkSizeChars) {
        List<String> chunks = new ArrayList<>();
        if (fullText == null || fullText.isBlank()) return chunks;

        int start = 0;
        while (start < fullText.length()) {
            int end = Math.min(start + chunkSizeChars, fullText.length());
            chunks.add(fullText.substring(start, end));
            start += chunkSizeChars;
        }
        return chunks;
    }
}
```

---

### 3.6 `Set<T>`: Uniqueness and Deduplicating AI Chunks

A `Set` guarantees that no duplicate elements exist.

```java
List<String> rawChunks = List.of(
    "Java 21 introduces Virtual Threads.",
    "Spring AI supports pgvector.",
    "Java 21 introduces Virtual Threads.", // Duplicate!
    "PostgreSQL pgvector is fast."
);

// LinkedHashSet preserves insertion order while stripping duplicates
Set<String> uniqueChunks = new LinkedHashSet<>(rawChunks);
System.out.println("Unique chunks count: " + uniqueChunks.size()); // 3!
```

---

### 3.7 `Map<K, V>`: `HashMap` Internals & `ConcurrentHashMap`

A `Map` maps unique keys to values.

#### How `HashMap` Retrieves Data in $O(1)$ Time:
1. `map.get(key)` calculates `key.hashCode()`.
2. Hashing math maps the hash code to a bucket index: `index = (n - 1) & hash`.
3. If multiple keys land in the same bucket (collision):
   - Stored in a linked list.
   - When a bucket exceeds 8 items, it converts into a **Red-Black Tree** ($O(\log N)$ search time).

#### Multi-Threaded Services: `ConcurrentHashMap`
Standard `HashMap` is **not thread-safe**. When serving concurrent web requests, use **`ConcurrentHashMap`**, which uses bucket-level locks and atomic Compare-And-Swap (CAS) instructions for high concurrency.

---

### 3.8 `PriorityQueue<T>`: The Top-K Scoring Engine for RAG

In Vector Search and RAG, an application calculates similarity scores for thousands of passages, but only needs the **Top-K most relevant** (e.g., Top-3).

Using a **Min-Heap (`PriorityQueue`) of size $K$** allows Top-K extraction in **$O(N \log K)$** time rather than sorting all items in $O(N \log N)$:

```java
package com.javagenai.day04;

public record ScoredChunk(String text, double similarityScore) 
       implements Comparable<ScoredChunk> {

    @Override
    public int compareTo(ScoredChunk other) {
        return Double.compare(this.similarityScore, other.similarityScore);
    }
}
```

```java
package com.javagenai.day04;

import java.util.List;
import java.util.PriorityQueue;

public class TopKRanker {

    public static PriorityQueue<ScoredChunk> getTopK(List<ScoredChunk> allChunks, int k) {
        PriorityQueue<ScoredChunk> minHeap = new PriorityQueue<>(k);

        for (ScoredChunk chunk : allChunks) {
            if (minHeap.size() < k) {
                minHeap.offer(chunk);
            } else if (chunk.similarityScore() > minHeap.peek().similarityScore()) {
                minHeap.poll(); // Evict the lowest score
                minHeap.offer(chunk);
            }
        }
        return minHeap;
    }
}
```

---

### 3.9 Modern Immutable Collections (`List.of`, `Map.of`)

Java 9+ provides factory methods for creating unmodifiable collections:

```java
// Immutable List
List<String> allowedModels = List.of("gpt-4o", "claude-3-5-sonnet", "llama-3.2");
// allowedModels.add("gemini"); // Throws UnsupportedOperationException!

// Immutable Map
Map<String, Double> modelPrices = Map.of(
    "gpt-4o-mini", 0.15,
    "gpt-4o", 2.50,
    "claude-3-5-sonnet", 3.00
);
```

---

# 4. Prerequisite & Supporting Concepts

### Prerequisite / Supporting Concept: Arrays vs. Dynamic Collections

- **Arrays (`String[]`)**: Fixed capacity determined at allocation. Fast, minimal overhead, but cannot grow dynamically.
- **Dynamic Collections (`ArrayList<String>`)**: Backed by an array that automatically doubles its internal buffer when full, providing a flexible resizable API.

---

### Prerequisite / Supporting Concept: Autoboxing & Unboxing (Primitives vs. Wrappers)

Java collections can only store object references, not raw primitives:
- Primitive `int` $\rightarrow$ Wrapper `Integer`
- Primitive `double` $\rightarrow$ Wrapper `Double`
- **Autoboxing**: Automatic conversion of primitive to wrapper (`list.add(5)` converts to `Integer.valueOf(5)`).
- **Unboxing**: Automatic conversion of wrapper to primitive (`int x = list.get(0)`).

---

### Prerequisite / Supporting Concept: Fail-Fast vs. Fail-Safe Iterators

- **Fail-Fast** (`ArrayList`, `HashMap`): Detects concurrent structural modifications during iteration via an internal `modCount`. If modified, it throws `ConcurrentModificationException` immediately.
- **Fail-Safe / Weakly Consistent** (`ConcurrentHashMap`, `CopyOnWriteArrayList`): Operates on an internal snapshot, permitting concurrent reads and writes without throwing exceptions.

---

# 5. Advanced Depth (Intermediate → Advanced)

### 5.1 Java 8+ HashMap Internals: Treeification & Collisions

When hash collisions occur, `HashMap` manages bucket structures dynamically:
- **`TREEIFY_THRESHOLD = 8`**: When a single bucket exceeds 8 elements AND the total table capacity is $\ge 64$, the bucket transforms from a linked list into a balanced Red-Black Tree.
- **`UNTREEIFY_THRESHOLD = 6`**: When deletions reduce bucket size to 6, it converts back to a linked list to conserve memory.

---

### 5.2 High-Performance Map Operations: `computeIfAbsent()` & `merge()`

#### The Eager Trap: `putIfAbsent` vs. `computeIfAbsent`
```java
Map<String, List<String>> userRoles = new HashMap<>();

// ❌ WASTEFUL: Allocates a new ArrayList() on EVERY call even if the key exists!
userRoles.putIfAbsent("admin", new ArrayList<>());

// ✅ OPTIMIZED: The lambda executes LAZILY only if the key is missing!
userRoles.computeIfAbsent("admin", k -> new ArrayList<>()).add("ROLE_SUPERUSER");
```

#### Counting with `Map.merge()`:
```java
Map<String, Integer> tokenCounts = new HashMap<>();

// Clean atomic increment using Map.merge:
tokenCounts.merge("gpt-4o", 150, Integer::sum);
```

---

### 5.3 Common Mistakes & Misconceptions (With Bad vs. Good Code)

#### Mistake 1: Modifying a Collection During a For-Each Loop
**Bad Code:**
```java
// ❌ CRASHES with ConcurrentModificationException!
for (String token : promptTokens) {
    if (token.isBlank()) {
        promptTokens.remove(token);
    }
}
```
**Correct Code (Java 8+):**
```java
// ✅ Safe, atomic removal using Predicate
promptTokens.removeIf(token -> token == null || token.isBlank());
```

#### Mistake 2: Using Raw Types
**Bad Code:**
```java
// ❌ Raw type loses type safety; throws ClassCastException at runtime
List rawList = new ArrayList();
rawList.add("Text");
rawList.add(100);
```
**Correct Code:**
```java
// ✅ Parameterized type checked at compile-time
List<String> safeList = new ArrayList<>();
```

---

### 5.4 Architectural Trade-Offs: Contiguous Memory (`ArrayList`) vs. Scattered Pointers (`LinkedList`)

Modern CPU hardware uses high-speed L1/L2 caches. When an array element is read, the CPU pre-fetches adjacent memory into the cache lines.
- **`ArrayList`**: Contiguous memory layout guarantees maximum CPU cache hits.
- **`LinkedList`**: Every node is an isolated object in Heap memory. Traversal requires pointer chasing, triggering constant CPU cache misses.
- **Conclusion**: In almost all production scenarios, `ArrayList` significantly outperforms `LinkedList`.

---

# 6. Quick Recap

| Collection / Concept | Key Property | Typical AI Use Case |
| :--- | :--- | :--- |
| **Generics (`<T>`)** | Compile-time type verification. | Ensuring type safety for responses and embeddings. |
| **`ArrayList<T>`** | Contiguous memory, $O(1)$ random access. | Document chunk sequences, token lists. |
| **`HashSet<T>`** | Unordered, $O(1)$ lookup, zero duplicates. | Document ID deduplication, stop-word filtering. |
| **`HashMap<K, V>`** | $O(1)$ key-value associations. | Metadata filters, prompt templates, local caches. |
| **`ConcurrentHashMap`**| Lock-striping thread-safe map. | Multi-threaded AI chat caches and sessions. |
| **`PriorityQueue<T>`**| Min/Max binary heap, $O(\log K)$. | Top-K similarity passage ranking in RAG. |

---

# 7. Self-Check Questions & Practice Exercises

### Self-Check Questions (Basic to Advanced)

1. **Why does `ArrayList` outperform `LinkedList` on modern CPUs?**
   - *Answer*: `ArrayList` elements reside in contiguous memory, enabling CPU cache pre-fetching and high cache-hit rates. `LinkedList` scatters nodes across the Heap, causing CPU cache misses on every node traversal.
2. **What does the PECS mnemonic dictate in Java Generics?**
   - *Answer*: Producer `extends`, Consumer `super`. Use `? extends T` when reading items out of a collection, and `? super T` when writing items into a collection.
3. **What occurs inside a `HashMap` when 9 keys collide in a single bucket in Java 8+?**
   - *Answer*: If table capacity is $\ge 64$, the bucket converts from a singly-linked list into a balanced Red-Black Tree, reducing lookup latency from $O(N)$ to $O(\log N)$.
4. **Why is `computeIfAbsent()` preferred over `putIfAbsent()` for expensive object allocations?**
   - *Answer*: `putIfAbsent()` evaluates its value expression eagerly on every invocation, whereas `computeIfAbsent()` evaluates its lambda lazily only when the key is absent.
5. **What is Type Erasure in Java?**
   - *Answer*: The compile-time process where generic type annotations are verified by the compiler and then removed from the generated bytecode, replaced with raw `Object` or bounds for backward compatibility.

---

### Hands-On Practice Exercises with Full Solutions

#### 🏋️ Exercise 1: Build an In-Memory Document Tag Index
**Objective**: Build a `DocumentTagIndex` mapping string tags (e.g., `"finance"`, `"medical"`) to sets of document IDs using `Map<String, Set<String>>` and lazy initialization.

```java
package com.javagenai.day04;

import java.util.*;

public class DocumentTagIndex {
    private final Map<String, Set<String>> index = new HashMap<>();

    public void addDocument(String docId, List<String> tags) {
        for (String tag : tags) {
            index.computeIfAbsent(tag.toLowerCase(), k -> new HashSet<>()).add(docId);
        }
    }

    public Set<String> findDocumentsByTag(String tag) {
        return index.getOrDefault(tag.toLowerCase(), Collections.emptySet());
    }

    public int getTagCount() {
        return index.size();
    }
}
```

---

#### 🏋️ Exercise 2: Top-K Context Passage Selector
**Objective**: Given candidate document chunks with similarity scores, extract the Top-K highest-ranking passages in descending order using a min-heap.

```java
package com.javagenai.day04;

import java.util.*;

public class ContextPassageSelector {

    public static List<ScoredChunk> selectTopK(List<ScoredChunk> candidates, int k) {
        if (candidates == null || candidates.isEmpty()) return Collections.emptyList();

        PriorityQueue<ScoredChunk> minHeap = new PriorityQueue<>(k);

        for (ScoredChunk candidate : candidates) {
            if (minHeap.size() < k) {
                minHeap.offer(candidate);
            } else if (candidate.similarityScore() > minHeap.peek().similarityScore()) {
                minHeap.poll(); // Evict lowest score
                minHeap.offer(candidate);
            }
        }

        List<ScoredChunk> result = new ArrayList<>(minHeap);
        result.sort((a, b) -> Double.compare(b.similarityScore(), a.similarityScore()));
        return result;
    }
}
```

---

<p align="center">
  <b>Day 04 Complete! 🎉</b><br>
  Proceed to <b>Day 05</b>: <b>Modern Java — Records, Optional & Sealed Types</b>.<br>
  <a href="../Day_05_Modern_Java_Records_Optional_Sealed/Day_05_Modern_Java_Records_Optional_Sealed.md"><b>Continue to Day 05 →</b></a>
</p>
