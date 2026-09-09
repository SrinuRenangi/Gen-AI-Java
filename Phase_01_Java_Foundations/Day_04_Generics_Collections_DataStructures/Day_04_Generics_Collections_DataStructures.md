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

## 📌 What Will You Learn Today?

In Generative AI, you almost never process a single piece of data in isolation. 
- You process **batches of embedding vectors** (`List<float[]>`).
- You track **multi-turn conversation histories** (`List<ChatMessage>`).
- You index **thousands of PDF chunks with metadata** (`Map<String, Object>`).
- You maintain a **unique vocabulary of tokens** (`Set<String>`).
- You search a vector database to find the **Top-5 most relevant context passages** (`PriorityQueue<ScoredDocument>`).

In Python, you can throw anything into a `list` or `dict`—integers, strings, booleans, floating-point vectors—and hope your code doesn't crash at runtime. In enterprise Java, **Generics (`<T>`)** and the **Java Collections Framework (JCF)** guarantee **100% compile-time type safety**, high memory efficiency, and predictable concurrency.

By the end of today, you will master:
- ✅ **The Generics Mental Model**: Why `<T>` exists and how it eliminates runtime `ClassCastException`.
- ✅ **Type Erasure**: What the JVM does with generics under the hood.
- ✅ **The PECS Rule**: Producer `extends`, Consumer `super` wildcards (`? extends T`).
- ✅ **The Big Three**: `List<T>`, `Set<T>`, `Map<K,V>`—when to use which.
- ✅ **`ArrayList` vs `LinkedList`**: Why cache locality makes `ArrayList` the king of AI data pipelines.
- ✅ **`HashMap` Internals Under the Hood**: Buckets, collision chaining, red-black trees, and load factors.
- ✅ **`PriorityQueue<T>` for AI RAG**: Implementing a Top-K similarity ranker for vector search.
- ✅ **Modern Immutable Collections**: `List.of()`, `Set.of()`, `Map.of()`.

---

## 🗺️ Table of Contents

- [1. Real-World Analogy: Labeled Cargo Shipping Containers](#1-real-world-analogy-labeled-cargo-shipping-containers)
- [2. Generics (`<T>`): Compile-Time Type Safety](#2-generics-t-compile-time-type-safety)
  - [2.1 The Danger of Raw Types](#21-the-danger-of-raw-types)
  - [2.2 Writing Your Own Generic Class](#22-writing-your-own-generic-class)
  - [2.3 Type Erasure: The Compiler's Secret](#23-type-erasure-the-compilers-secret)
  - [2.4 Bounded Wildcards: The PECS Rule](#24-bounded-wildcards-the-pecs-rule)
- [3. The Java Collections Framework (JCF) Hierarchy](#3-the-java-collections-framework-jcf-hierarchy)
- [4. `List<T>`: Ordered Sequences for AI Pipelines](#4-listt-ordered-sequences-for-ai-pipelines)
  - [4.1 `ArrayList` vs. `LinkedList`](#41-arraylist-vs-linkedlist)
  - [4.2 Chunking a Document with `List`](#42-chunking-a-document-with-list)
- [5. `Set<T>`: Uniqueness and Deduplication](#5-sett-uniqueness-and-deduplication)
  - [5.1 `HashSet` vs `TreeSet` vs `LinkedHashSet`](#51-hashset-vs-treeset-vs-linkedhashset)
  - [5.2 Deduplicating Document Chunks](#52-deduplicating-document-chunks)
- [6. `Map<K, V>`: Key-Value Associations](#6-mapk-v-key-value-associations)
  - [6.1 Under the Hood of `HashMap`](#61-under-the-hood-of-hashmap)
  - [6.2 `ConcurrentHashMap` for Multi-Threaded AI](#62-concurrenthashmap-for-multi-threaded-ai)
- [7. `PriorityQueue<T>`: The Top-K Engine for RAG](#7-priorityqueuet-the-top-k-engine-for-rag)
- [8. Modern Immutable Collections (`List.of`, `Map.of`)](#8-modern-immutable-collections-listof-mapof)
- [9. Key Takeaways & Summary](#9-key-takeaways--summary)
- [10. Practice Exercises & Full Solutions](#10-practice-exercises--full-solutions)
- [11. Self-Check Quiz](#11-self-check-quiz)

---

# 1. Real-World Analogy: Labeled Cargo Shipping Containers

Imagine an international shipping port.

```
                      UNMARKED WOODEN CRATES (Raw Types / Python Lists)
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

Without generics, a collection is an unmarked crate holding `Object`. Anyone can put a `String` into a list intended for `Double` embedding vectors, and the program will compile silently—only to explode with a `ClassCastException` in production at 3:00 AM.

With generics (`List<Double>`), the Java compiler acts as a strict port authority: **It refuses to compile if anything other than a `Double` attempts to enter the container.**

---

## 🧭 The Mid-Level Java Developer Bridge: Generics & Collections Demystified

Most mid-level Java developers use `List<String>` and `Map<String, Object>` every day, but wildcards and type erasure often feel confusing:

| Concept | The Academic Definition | What It Actually Means in Plain English |
| :--- | :--- | :--- |
| **Generics (`<T>`)** | Parameterized type polymorphism. | Putting a label on a box: *"Only items of type T allowed inside."* |
| **Type Erasure** | Generic type metadata removed at bytecode compilation. | Generics exist **only** to protect you while writing code. Once compiled into `.class` bytecode, Java erases `<String>` back to plain `Object` for backward compatibility. |
| **`<? extends Number>`** | Covariant wildcard (PECS: Producer Extends). | Read-only access! You can read items out as `Number`, but Java won't let you `.add()` anything into it because it doesn't know if the list is `Integer` or `Double`. |
| **`<? super Integer>`** | Contravariant wildcard (PECS: Consumer Super). | Write-safe access! You can safely `.add(42)` because the list is guaranteed to hold `Integer` or its ancestors (`Number`, `Object`). |
| **`List.of("A", "B")`** | Immutable unmodifiable list (Java 9+). | Cannot `.add()` or `.set()`. Fast, lightweight, and thread-safe. Throws `UnsupportedOperationException` if mutated. |
| **`ConcurrentHashMap`** | Lock-striping thread-safe map. | Unlike `HashMap` which corrupts or loops infinitely when 2 threads write simultaneously, `ConcurrentHashMap` allows 10,000 threads to read and write safely without locking the whole map. |

---

# 2. Generics (`<T>`): Compile-Time Type Safety

### 2.1 The Danger of Raw Types

In ancient Java (prior to Java 5), collections held raw `Object` references:

```java
// DANGEROUS: Raw Type
List vector = new ArrayList();
vector.add(0.245);
vector.add(0.891);
vector.add("corrupted_text"); // Accidental bug! Compiles without error!

// Later in the math engine:
for (int i = 0; i < vector.size(); i++) {
    Double val = (Double) vector.get(i); // BOOM! Crashes on index 2 with ClassCastException!
}
```

With Generics:
```java
// SAFE: Strictly parameterized
List<Double> vector = new ArrayList<>();
vector.add(0.245);
vector.add(0.891);
// vector.add("corrupted_text"); // COMPILE ERROR! Compiler prevents the bug immediately!
```

---

### 2.2 Writing Your Own Generic Class

Let's build a generic **`AIResponse<T>`** container. In AI, a model response might contain raw text (`String`), structured JSON parsed into a record (`Invoice`), or embedding vectors (`float[]`):

```java
package com.javagenai.day04;

import java.time.Instant;

// T is a generic type parameter placeholder
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

Look at how cleanly this adapts to any data type:

```java
// 1. Text response
AIResponse<String> textResp = new AIResponse<>("The capital of France is Paris.", 12, 8);
String text = textResp.getPayload(); // No casting required!

// 2. Structured response
AIResponse<Integer> countResp = new AIResponse<>(42, 5, 2);
int count = countResp.getPayload(); // Auto-unboxing directly to primitive int!
```

---

### 2.3 Type Erasure: The Compiler's Secret

How does the JVM execute generics without bloating memory?

Through **Type Erasure**:
1. During compilation, the compiler inspects all types (e.g., verifying you only put `Double` in `List<Double>`).
2. Once validated, the compiler **erases** `<Double>` from the bytecode and replaces it with raw `Object` (plus automatic synthetic casts).
3. **At runtime on the JVM, `List<String>` and `List<Double>` are the exact same class (`ArrayList.class`)!**

> [!NOTE]
> Because of Type Erasure, you cannot do `new T()` or `new T[10]` directly inside a generic class.

---

### 2.4 Bounded Wildcards: The PECS Rule

What if you want a method that accepts a list of *any numbers* (integers, floats, doubles) to calculate embedding magnitudes?

```java
// Fails! A List<Integer> is NOT a subclass of List<Number> in Java generics!
public static double calculateMagnitude(List<Number> numbers) { ... }
```

To solve this, Java provides **Wildcards (`?`)**:
- **`? extends T` (Upper Bounded)**: Accepts `T` or any subclass of `T`.
- **`? super T` (Lower Bounded)**: Accepts `T` or any superclass of `T`.

#### The Senior Rule: PECS (Producer Extends, Consumer Super)
- If your method **reads data out** of the collection (it *produces* data for you to use), use **`? extends T`**.
- If your method **writes data into** the collection (it *consumes* data from you), use **`? super T`**.

```java
// Reading data OUT of list (Producer -> extends)
public static double sumVectors(List<? extends Number> numbers) {
    double sum = 0.0;
    for (Number n : numbers) {
        sum += n.doubleValue(); // Safe! Everything inside is guaranteed to be a Number
    }
    return sum;
}
```

---

# 3. The Java Collections Framework (JCF) Hierarchy

All major collection interfaces inherit from the root `java.lang.Iterable` and `java.util.Collection`:

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

*Note: Map<K,V> is so fundamental that it stands in its own separate hierarchy!
┌──────────────────┐
│<<interface>> Map │ ──► HashMap, TreeMap, LinkedHashMap, ConcurrentHashMap
└──────────────────┘
```

---

# 4. `List<T>`: Ordered Sequences for AI Pipelines

A `List` is an ordered collection that allows duplicates and provides indexed access (`get(i)`).

### 4.1 `ArrayList` vs. `LinkedList`

| Feature | `ArrayList<T>` | `LinkedList<T>` |
| :--- | :--- | :--- |
| **Internal Structure** | Resizable array in contiguous memory | Doubly-linked nodes scattered across Heap |
| **Random Access (`get(i)`)** | **$O(1)$ Instant** | $O(N)$ Traverses from head |
| **Append (`add(item)`)** | $O(1)$ amortized | $O(1)$ |
| **CPU Cache Locality** | **Phenomenal** (elements sit adjacent in RAM) | Terrible (CPU cache misses on every node pointer) |
| **Memory Overhead** | Minimal (plain array buffer) | High (24 bytes of pointer overhead per element) |
| **Verdict** | **Use in 99.9% of AI applications** | Almost never used in modern high-performance Java |

---

### 4.2 Chunking a Document with `List`

In RAG, a 50-page document must be split into smaller 500-token chunks. Here is how clean document chunking looks with `ArrayList`:

```java
package com.javagenai.day04;

import java.util.ArrayList;
import java.util.List;

public class TextChunker {

    public static List<String> chunkText(String fullText, int chunkSizeChars) {
        List<String> chunks = new ArrayList<>();
        if (fullText == null || fullText.isBlank()) {
            return chunks;
        }

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

# 5. `Set<T>`: Uniqueness and Deduplication

A `Set` is a collection that guarantees **no duplicate elements**.

### 5.1 `HashSet` vs `TreeSet` vs `LinkedHashSet`

| Implementation | Ordering | Lookup Time | Under The Hood |
| :--- | :--- | :---: | :--- |
| **`HashSet`** | None (random bucket order) | **$O(1)$** | Backed by a `HashMap` table |
| **`LinkedHashSet`** | Preserves insertion order | $O(1)$ | Hash table + doubly-linked list |
| **`TreeSet`** | Natural sorted order (e.g. A-Z) | $O(\log N)$ | Red-Black self-balancing binary search tree |

---

### 5.2 Deduplicating Document Chunks

When scraping web data or ingesting PDFs for RAG, the same paragraph often appears multiple times (e.g., disclaimers, headers). A `Set` removes duplicates in $O(N)$ linear time:

```java
List<String> rawChunks = List.of(
    "Java 21 introduces Virtual Threads.",
    "Spring AI supports pgvector.",
    "Java 21 introduces Virtual Threads.", // Duplicate!
    "PostgreSQL pgvector is fast."
);

Set<String> uniqueChunks = new LinkedHashSet<>(rawChunks);
System.out.println("Unique chunks count: " + uniqueChunks.size()); // 3!
```

---

# 6. `Map<K, V>`: Key-Value Associations

A `Map` associates a unique key with a value. In AI, maps store:
- HTTP headers for LLM API calls (`Authorization: Bearer sk-...`)
- Prompt template parameters (`{ "username": "Alice", "topic": "Java" }`)
- Metadata filters in vector search (`{ "author": "John", "year": 2025 }`)

### 6.1 Under the Hood of `HashMap`

How does `map.get("user")` find its value in instant $O(1)$ time among 1,000,000 entries?

```
Key ("user") ──► hashCode() ──► Hashing Math ──► Bucket Index (e.g. Bucket 4)
                                                       │
                                                       ▼
                                        Bucket 4: [Node: key="user", val="Alice"]
```

1. **Bucket Array**: A `HashMap` maintains an internal array of `Node<K,V>[]` (default initial size: 16).
2. **Hash Function**: The key's `hashCode()` is mapped to an array index via `(n - 1) & hash`.
3. **Collision Handling**: If two different keys land in the same bucket:
   - In Java 7: Stored as a singly-linked list ($O(N)$ scan).
   - In Java 8+: When a bucket accumulates $> 8$ items, it dynamically converts into a **Red-Black Tree**! Search time drops from $O(N)$ to **$O(\log N)$**, preventing Denial-of-Service (HashDoS) attacks!
4. **Load Factor**: Default is `0.75`. When 75% of buckets are occupied, the HashMap automatically doubles its array size and rehashes all elements.

---

### 6.2 `ConcurrentHashMap` for Multi-Threaded AI

In a web application serving 1,000 simultaneous users, standard `HashMap` is **not thread-safe**. Modifying it concurrently from multiple threads will cause corrupted state and infinite loops.

**`ConcurrentHashMap`** solves this without locking the entire map! It uses fine-grained **bucket-level locking** (via Compare-And-Swap / CAS), allowing hundreds of threads to read and write simultaneously with near-zero contention.

---

# 7. `PriorityQueue<T>`: The Top-K Engine for RAG

In Vector Search and RAG, after calculating cosine similarity scores between a query and 10,000 document chunks, you only want the **Top-K most relevant chunks** (e.g., Top-3).

Sorting all 10,000 items takes $O(N \log N)$ time.
Using a **Min-Heap (`PriorityQueue`) of size $K$** takes only **$O(N \log K)$** time!

```java
package com.javagenai.day04;

import java.util.PriorityQueue;

public record ScoredChunk(String text, double similarityScore) 
       implements Comparable<ScoredChunk> {

    // Smallest score has highest priority (Min-Heap)
    @Override
    public int compareTo(ScoredChunk other) {
        return Double.compare(this.similarityScore, other.similarityScore);
    }
}
```

```java
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

# 8. Modern Immutable Collections (`List.of`, `Map.of`)

In AI applications, configuration parameters and system prompts should be **immutable** (cannot be altered after creation):

```java
// Immutable List (read-only)
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

# 9. Key Takeaways & Summary

```
                  ┌─────────────────────────────────┐
                  │       DAY 04 CHEAT SHEET        │
                  └────────────────┬────────────────┘
                                   │
         ┌─────────────────────────┼─────────────────────────┐
         ▼                         ▼                         ▼
  [ Generics & Types ]      [ JCF Data Structures ]   [ Modern Best Practice ]
  • <T> ensures compile-    • ArrayList: $O(1)$ fast  • Use List.of() and
    time type safety          random access             Map.of() for immutability
  • Type Erasure: erased    • HashSet: $O(1)$ unique  • ConcurrentHashMap for
    to Object at runtime    • HashMap: $O(1)$ lookup    multi-threaded services
  • PECS: Producer Extends, • PriorityQueue: Top-K    • Prefer ArrayList over
    Consumer Super            heap ranking for RAG      LinkedList always
```

---

# 10. Practice Exercises & Full Solutions

### 🏋️ Exercise 1: Build an In-Memory Document Tag Index
**Objective**: Build an index `DocumentTagIndex` that maps tags (e.g., `"finance"`, `"medical"`, `"legal"`) to sets of document IDs using `Map<String, Set<String>>`.

#### Solution:
```java
package com.javagenai.day04;

import java.util.*;

public class DocumentTagIndex {
    private final Map<String, Set<String>> index = new HashMap<>();

    public void addDocument(String docId, List<String> tags) {
        for (String tag : tags) {
            // computeIfAbsent creates the HashSet if the tag doesn't exist yet!
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

### 🏋️ Exercise 2: Top-3 Context Passage Selector
**Objective**: Given a list of passages with cosine similarity scores ranging from `0.0` to `1.0`, return the Top-3 passages in descending order of similarity.

#### Solution:
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
                minHeap.poll();
                minHeap.offer(candidate);
            }
        }

        List<ScoredChunk> result = new ArrayList<>(minHeap);
        // Sort descending so highest score appears first
        result.sort((a, b) -> Double.compare(b.similarityScore(), a.similarityScore()));
        return result;
    }
}
```

---

## 11. Self-Check Quiz

1. **Why does `ArrayList` outperform `LinkedList` in modern CPU architectures?**
   - *Answer*: `ArrayList` stores elements in contiguous memory blocks, maximizing CPU L1/L2 cache hits. `LinkedList` scatters node objects across the heap, incurring cache misses on every node pointer traversal.
2. **What does the PECS acronym stand for in Java Generics?**
   - *Answer*: Producer `extends`, Consumer `super`. Use `? extends T` when reading data from a collection, and `? super T` when writing data into a collection.
3. **What happens inside a `HashMap` when 9 keys collide in the same bucket in Java 8+?**
   - *Answer*: The linked list converts into a balanced Red-Black Tree, reducing search time from $O(N)$ to $O(\log N)$.
4. **Why should you use `ConcurrentHashMap` instead of `HashMap` in a Spring Boot web service?**
   - *Answer*: Spring Boot handles requests across multiple threads concurrently. `HashMap` is not thread-safe and can become corrupted, while `ConcurrentHashMap` uses bucket-level lock-free CAS operations for thread safety.
5. **What is Type Erasure?**
   - *Answer*: The process where the Java compiler enforces type safety during compilation and then strips out generic type parameters, replacing them with `Object` in the generated bytecode for backward compatibility.

---

<p align="center">
  <b>Congratulations on completing Day 04! 🎉</b><br>
  Tomorrow on <b>Day 05</b>, we explore <b>Modern Java: Records, Optional & Sealed Types</b> — The Java 21 superpowers that make Generative AI pipelines clean, expressive, and immune to NullPointerExceptions!
</p>
