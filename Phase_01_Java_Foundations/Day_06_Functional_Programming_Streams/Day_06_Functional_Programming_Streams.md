# ⚡ Day 06: Functional Programming & Stream API
## Lambdas, Method References & High-Throughput AI Data Pipelines

| ⬅️ Previous Day | 📚 Course Hub | ➡️ Next Day |
|:---|:---:|---:|
| [← Day 05: Modern Java — Records, Optional & Sealed Types](../Day_05_Modern_Java_Records_Optional_Sealed/Day_05_Modern_Java_Records_Optional_Sealed.md) | [All 60 Days Overview](../../README.md) | [Day 07: Concurrency & Virtual Threads →](../Day_07_Concurrency_Virtual_Threads/Day_07_Concurrency_Virtual_Threads.md) |

[![Phase](https://img.shields.io/badge/Phase_01-Java_Foundations-brightgreen.svg?style=for-the-badge)](../../README.md)
[![Day](https://img.shields.io/badge/Day-06_of_60-blue.svg?style=for-the-badge)](../../README.md)
[![Difficulty](https://img.shields.io/badge/Difficulty-Intermediate-blue.svg?style=for-the-badge)](../../README.md)
[![Topic](https://img.shields.io/badge/Core-Stream_API_%26_Lambdas-orange.svg?style=for-the-badge)](../../README.md)

---

## 📌 What Will You Learn Today?

In Generative AI engineering, data preprocessing is 80% of the battle. Before passing text to an LLM or Vector Store, you must:
1. Load 10,000 raw documents.
2. Filter out corrupt, empty, or duplicate files.
3. Clean and normalize whitespace and special characters.
4. Split documents into semantic paragraph chunks.
5. Batch-calculate embeddings across all CPU cores.

In imperative code (traditional loops with nested `for` and `if`), this requires 150 lines of mutable lists and error-prone index tracking.

In **Functional Java**, this entire pipeline is expressed in **one clean, declarative, readable Stream pipeline**.

By the end of today, you will master:
- ✅ **The Functional Mindset**: Declarative ("WHAT") vs Imperative ("HOW").
- ✅ **Lambda Expressions (`->`)**: Treating code as data that can be passed to methods.
- ✅ **Core Functional Interfaces**: `Predicate<T>`, `Function<T,R>`, `Consumer<T>`, `Supplier<T>`.
- ✅ **Method References (`::`)**: Clean shorthand for invoking existing methods.
- ✅ **Stream Anatomy**: Source $\rightarrow$ Intermediate Operations (lazy) $\rightarrow$ Terminal Operation (eager).
- ✅ **Essential Stream Operations**: `filter`, `map`, `flatMap`, `distinct`, `sorted`, `limit`.
- ✅ **Power Collectors**: `toList()`, `groupingBy()`, `joining()`, `summarizingDouble()`.
- ✅ **Parallel Streams (`.parallelStream()`)**: Utilizing all CPU cores for AI batch processing with zero thread boilerplate.

---

## 🗺️ Table of Contents

- [1. Real-World Analogy: The Factory Assembly Line](#1-real-world-analogy-the-factory-assembly-line)
- [2. Lambda Expressions & Functional Interfaces](#2-lambda-expressions--functional-interfaces)
  - [2.1 Syntax of a Lambda (`->`)](#21-syntax-of-a-lambda--)
  - [2.2 The Big 4 Functional Interfaces](#22-the-big-4-functional-interfaces)
  - [2.3 Method References (`::`)](#23-method-references-)
- [3. The Stream API: Architecture & Laziness](#3-the-stream-api-architecture--laziness)
  - [3.1 The 3 Stages of a Stream](#31-the-3-stages-of-a-stream)
  - [3.2 Lazy Evaluation: Why Streams Are Fast](#32-lazy-evaluation-why-streams-are-fast)
- [4. Intermediate Operations: Transforming AI Data](#4-intermediate-operations-transforming-ai-data)
  - [4.1 `filter`: Removing Low-Quality Documents](#41-filter-removing-low-quality-documents)
  - [4.2 `map`: Extracting & Transforming Fields](#42-map-extracting--transforming-fields)
  - [4.3 `flatMap`: Flattening Chunks into a Single Stream](#43-flatmap-flattening-chunks-into-a-single-stream)
- [5. Terminal Operations & Advanced Collectors](#5-terminal-operations--advanced-collectors)
  - [5.1 `collect(Collectors.toList())`](#51-collectcollectorstolist)
  - [5.2 `groupingBy`: Partitioning AI Requests by Model](#52-groupingby-partitioning-ai-requests-by-model)
  - [5.3 `joining`: Building Multi-Chunk LLM Prompts](#53-joining-building-multi-chunk-llm-prompts)
- [6. Parallel Streams: 16-Core Multi-Threaded Ingestion](#6-parallel-streams-16-core-multi-threaded-ingestion)
- [7. Key Takeaways & Summary](#7-key-takeaways--summary)
- [8. Practice Exercises & Full Solutions](#8-practice-exercises--full-solutions)
- [9. Self-Check Quiz](#9-self-check-quiz)

---

# 1. Real-World Analogy: The Factory Assembly Line

Imagine a Tesla battery manufacturing plant.

```
 RAW MATERIAL                  FACTORY CONVEYOR BELT                      FINISHED PACKS
┌─────────────┐       ┌───────────┐     ┌───────────┐     ┌───────────┐   ┌─────────────┐
│ Lithium Ore │ ──►── │ Inspection│ ──► │  Purify   │ ──► │ Assemble  │──►│ Battery Pack│
│ (Raw Data)  │       │ (filter)  │     │   (map)   │     │ (collect) │   │ (Result)    │
└─────────────┘       └───────────┘     └───────────┘     └───────────┘   └─────────────┘
```

1. **The Ore** doesn't sit in workers' desks. It moves continuously along a **conveyor belt** (The Stream).
2. **Each Station** performs one specialized action:
   - Station 1 rejects flawed chunks (`filter`).
   - Station 2 reshapes the metal (`map`).
   - Station 3 packages the finished battery into a crate (`collect`).
3. **Efficiency**: If the final packaging station is turned off, the conveyor belt doesn't even start! (Lazy Evaluation).

---

# 2. Lambda Expressions & Functional Interfaces

A **Functional Interface** is an interface that contains **exactly one abstract method** (marked with `@FunctionalInterface`).

### 2.1 Syntax of a Lambda (`->`)

Instead of writing an anonymous class with 6 lines of code:
```java
// OLD: Anonymous class
Predicate<String> validator = new Predicate<String>() {
    @Override
    public boolean test(String s) {
        return s.length() > 10;
    }
};
```

You write a **Lambda Expression**:
```java
// MODERN: Lambda expression
Predicate<String> validator = s -> s.length() > 10;
```

```
 ( parameters )  ->  { expression_body }
```

---

### 2.2 The Big 4 Functional Interfaces

Java standardizes functional programming in `java.util.function`:

| Interface | Method Signature | Purpose in AI | Example |
| :--- | :--- | :--- | :--- |
| **`Predicate<T>`** | `boolean test(T t)` | Filtering documents or tokens | `doc -> doc.tokenCount() < 4000` |
| **`Function<T, R>`** | `R apply(T t)` | Transforming input to output | `text -> embeddingModel.embed(text)` |
| **`Consumer<T>`** | `void accept(T t)` | Logging, saving, printing (side effects) | `prompt -> logger.info(prompt)` |
| **`Supplier<T>`** | `T get()` | Factory, generating fallback values | `() -> "default-system-prompt"` |

---

### 2.3 Method References (`::`)

When a lambda simply passes its parameter directly into an existing method, you can replace it with a **Method Reference (`::`)**:

| Lambda Syntax | Method Reference Equivalent |
| :--- | :--- |
| `text -> text.toUpperCase()` | `String::toUpperCase` |
| `doc -> doc.getContent()` | `Document::getContent` |
| `item -> System.out.println(item)` | `System.out::println` |
| `() -> new ArrayList<>()` | `ArrayList::new` |

---

# 3. The Stream API: Architecture & Laziness

### 3.1 The 3 Stages of a Stream

Every Java stream pipeline consists of three distinct stages:

```
 1. SOURCE                  2. INTERMEDIATE OPERATIONS              3. TERMINAL OPERATION
┌──────────────────┐       ┌─────────────────────────────────┐     ┌─────────────────────┐
│ list.stream()    │  ──►  │ .filter(...)                    │ ──► │ .toList()           │
│ Set, Map, Array  │       │ .map(...)                       │     │ .count()            │
│ Files.lines(path)│       │ .sorted(...)                    │     │ .forEach(...)       │
└──────────────────┘       └─────────────────────────────────┘     └─────────────────────┘
                                  (Lazy - does nothing yet)          (Eager - triggers execution)
```

---

### 3.2 Lazy Evaluation: Why Streams Are Fast

Intermediate operations are **lazy**. They do not execute when you call `.map()` or `.filter()`. They only execute when a **terminal operation** (like `.collect()` or `.findFirst()`) is triggered.

```java
List<String> rawPrompts = List.of("Short", "A very long detailed prompt for LLM", "Hi");

// This pipeline does NOT execute yet!
Stream<String> stream = rawPrompts.stream()
    .filter(p -> {
        System.out.println("Filtering: " + p);
        return p.length() > 10;
    });

System.out.println("Stream defined. Triggering terminal operation now...");
String firstMatch = stream.findFirst().orElse("None");
System.out.println("Result: " + firstMatch);
```

**Console Output:**
```text
Stream defined. Triggering terminal operation now...
Filtering: Short
Filtering: A very long detailed prompt for LLM
Result: A very long detailed prompt for LLM
```
Notice: The stream stopped processing immediately after finding the first match! It **never even inspected `"Hi"`**. That is the efficiency of lazy evaluation.

---

# 4. Intermediate Operations: Transforming AI Data

### 4.1 `filter`: Removing Low-Quality Documents

```java
List<Document> cleanDocs = rawDocuments.stream()
    .filter(d -> d.content() != null && !d.content().isBlank())
    .filter(d -> d.tokenCount() >= 50) // Discard tiny fragments
    .toList();
```

---

### 4.2 `map`: Extracting & Transforming Fields

`map` transforms each element $T$ into an element $R$:

```java
// Extracting only text strings from documents
List<String> promptTexts = cleanDocs.stream()
    .map(Document::content)
    .map(String::trim)
    .toList();
```

---

### 4.3 `flatMap`: Flattening Chunks into a Single Stream

Suppose each `Document` has a method `List<Chunk> getChunks()`.
- Using `.map(Document::getChunks)` would return `Stream<List<Chunk>>` (a stream of lists).
- Using **`.flatMap(d -> d.getChunks().stream())`** unpacks each list and flattens everything into a single `Stream<Chunk>`!

```
 Document 1 ──► [ Chunk 1A, Chunk 1B ]
                                         ──► flatMap ──► Stream of [ 1A, 1B, 2A, 2B ]
 Document 2 ──► [ Chunk 2A, Chunk 2B ]
```

---

# 5. Terminal Operations & Advanced Collectors

### 5.1 `collect(Collectors.toList())`

In Java 16+, you can write `.toList()` directly!

---

### 5.2 `groupingBy`: Partitioning AI Requests by Model

Imagine your backend processed 10,000 LLM calls today. You want to group them by model name to analyze usage:

```java
public record LLMCallLog(String model, int tokensUsed, double latencyMs) {}

List<LLMCallLog> logs = fetchLogs();

// Grouping by model: Map<String, List<LLMCallLog>>
Map<String, List<LLMCallLog>> logsByModel = logs.stream()
    .collect(Collectors.groupingBy(LLMCallLog::model));

// Calculating total tokens per model: Map<String, Integer>
Map<String, Integer> tokensPerModel = logs.stream()
    .collect(Collectors.groupingBy(
        LLMCallLog::model,
        Collectors.summingInt(LLMCallLog::tokensUsed)
    ));
```

---

### 5.3 `joining`: Building Multi-Chunk LLM Prompts

In RAG, you retrieve 3 document chunks and need to merge them into a single context string separated by dividers:

```java
List<String> retrievedChunks = List.of(
    "Chunk 1: Virtual threads run on carrier threads.",
    "Chunk 2: Spring AI 1.0 supports pgvector.",
    "Chunk 3: PostgreSQL HNSW indexes accelerate vector search."
);

String combinedContext = retrievedChunks.stream()
    .collect(Collectors.joining("\n---\n", "[BEGIN CONTEXT]\n", "\n[END CONTEXT]"));

System.out.println(combinedContext);
```

**Output:**
```text
[BEGIN CONTEXT]
Chunk 1: Virtual threads run on carrier threads.
---
Chunk 2: Spring AI 1.0 supports pgvector.
---
Chunk 3: PostgreSQL HNSW indexes accelerate vector search.
[END CONTEXT]
```

---

# 6. Parallel Streams: 16-Core Multi-Threaded Ingestion

When generating embeddings or normalizing 50,000 document paragraphs, a single CPU core is slow.

By changing `.stream()` to **`.parallelStream()`**, Java automatically splits your collection across all CPU cores using the **ForkJoinPool**:

```java
// Sequential: Uses 1 CPU Core
List<double[]> embeddingsSeq = documents.stream()
    .map(this::heavyVectorComputation)
    .toList();

// Parallel: Uses ALL available CPU cores automatically!
List<double[]> embeddingsPar = documents.parallelStream()
    .map(this::heavyVectorComputation)
    .toList();
```

> [!WARNING]
> Only use `parallelStream()` for **CPU-intensive math or in-memory batch operations**. Do not use `parallelStream()` for blocking network I/O calls to external APIs—we will learn how **Virtual Threads** handle high-concurrency network calls in Day 07!

---

# 7. Key Takeaways & Summary

```
                  ┌─────────────────────────────────┐
                  │       DAY 06 CHEAT SHEET        │
                  └────────────────┬────────────────┘
                                   │
         ┌─────────────────────────┼─────────────────────────┐
         ▼                         ▼                         ▼
  [ Functional Core ]      [ Stream Pipeline ]       [ Collectors & Parallel ]
  • Predicate: test -> bool• Source -> Intermediates • .toList() for immutable
  • Function: apply -> R     -> Terminal               result lists
  • Consumer: accept(v)    • filter: conditional drop• groupingBy: SQL-like
  • Supplier: get() -> V   • map: transform elements   groupings in memory
  • Method Reference (::)  • flatMap: flatten lists  • parallelStream(): 
    for clean readability  • Lazy until terminal       instant multi-core math
```

---

# 8. Practice Exercises & Full Solutions

### 🏋️ Exercise 1: Build an AI Document Preprocessing Pipeline
**Objective**: Given a list of raw document texts, build a single fluent Stream pipeline that:
1. Filters out null or blank strings.
2. Strips leading/trailing whitespace.
3. Removes strings with $< 20$ characters.
4. Converts all text to uppercase.
5. Limits the result to the first 3 documents.
6. Collects into a list.

#### Solution:
```java
package com.javagenai.day06;

import java.util.List;

public class DocumentPipeline {

    public static List<String> processDocuments(List<String> rawTexts) {
        return rawTexts.stream()
            .filter(text -> text != null && !text.isBlank())
            .map(String::trim)
            .filter(text -> text.length() >= 20)
            .map(String::toUpperCase)
            .limit(3)
            .toList();
    }
}
```

---

### 🏋️ Exercise 2: LLM Cost & Usage Analytics with `Collectors`
**Objective**: Given a list of `LLMRecord(String model, int promptTokens, int completionTokens)`, compute a `Map<String, Double>` calculating the total USD cost per model.
(Assume: `gpt-4o`: \$2.50 / 1M tokens, `llama-3.2`: \$0.00 / 1M tokens).

#### Solution:
```java
package com.javagenai.day06;

import java.util.*;
import java.util.stream.Collectors;

public record LLMRecord(String model, int promptTokens, int completionTokens) {
    public int totalTokens() { return promptTokens + completionTokens; }
}

public class UsageAnalytics {

    public static Map<String, Double> calculateCostPerModel(List<LLMRecord> records) {
        return records.stream()
            .collect(Collectors.groupingBy(
                LLMRecord::model,
                Collectors.summingDouble(record -> {
                    double ratePerMillion = "gpt-4o".equalsIgnoreCase(record.model()) ? 2.50 : 0.0;
                    return (record.totalTokens() / 1_000_000.0) * ratePerMillion;
                })
            ));
    }
}
```

---

## 9. Self-Check Quiz

1. **What is the difference between an intermediate operation and a terminal operation in a Stream?**
   - *Answer*: Intermediate operations (like `filter`, `map`) are lazy and return a new `Stream` without processing data immediately. Terminal operations (like `toList`, `count`, `forEach`) are eager, trigger the traversal, and produce a result or side effect.
2. **When should you use `flatMap()` instead of `map()`?**
   - *Answer*: Use `map()` for one-to-one transformations ($T \rightarrow R$). Use `flatMap()` for one-to-many transformations where each element maps to a collection or stream ($T \rightarrow \text{Stream}<R>$), flattening the nested streams into a single stream.
3. **What is a Method Reference (`::`)?**
   - *Answer*: A concise syntactic shorthand for a lambda expression that calls an existing named method without executing it immediately (e.g., `String::toLowerCase` instead of `s -> s.toLowerCase()`).
4. **Why is `parallelStream()` not recommended for blocking HTTP API calls?**
   - *Answer*: `parallelStream()` uses the shared common `ForkJoinPool`, which has a small fixed number of threads equal to your CPU core count. Blocking them with slow HTTP I/O starves the entire JVM of worker threads.
5. **How does `Collectors.joining()` assist in Prompt Engineering?**
   - *Answer*: It concatenates multiple retrieved text chunks into a single formatted context prompt string with custom delimiters, prefixes, and suffixes.

---

<p align="center">
  <b>Congratulations on completing Day 06! 🎉</b><br>
  Tomorrow on <b>Day 07</b>, we conquer <b>Concurrency & Virtual Threads (Project Loom)</b>: The Java 21 superpower that allows a single server to handle 10,000 concurrent LLM streaming connections!
</p>
