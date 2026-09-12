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

![How Java Streams Work: The Factory Conveyor Belt Model](assets/day06_java_streams.jpg)

## 🗺️ Table of Contents
- [1. Topic Overview](#1-topic-overview)
- [2. Basic Foundations (True Zero)](#2-basic-foundations-true-zero)
  - [2.1 What is Functional Programming, a Lambda, and a Stream?](#21-what-is-functional-programming-a-lambda-and-a-stream)
  - [2.2 The Factory Assembly Line Model](#22-the-factory-assembly-line-model)
  - [2.3 Minimal Working Example: Cleaning AI Text Chunks](#23-minimal-working-example-cleaning-ai-text-chunks)
  - [2.4 Line-by-Line Code Breakdown](#24-line-by-line-code-breakdown)
- [3. Core Concept Walkthrough (Basic → Intermediate)](#3-core-concept-walkthrough-basic--intermediate)
  - [3.1 The Big 4 Functional Interfaces](#31-the-big-4-functional-interfaces)
  - [3.2 Method References (`::`): Clean Syntactic Shorthand](#32-method-references--clean-syntactic-shorthand)
  - [3.3 Anatomy of a Stream: Source, Intermediate, and Terminal Stages](#33-anatomy-of-a-stream-source-intermediate-and-terminal-stages)
  - [3.4 Lazy Evaluation in Action: Proof of Zero Work Until Terminal](#34-lazy-evaluation-in-action-proof-of-zero-work-until-terminal)
  - [3.5 Core Intermediate Operations: `filter`, `map`, and `flatMap`](#35-core-intermediate-operations-filter-map-and-flatmap)
  - [3.6 Power Collectors: `toList()`, `groupingBy()`, and `joining()`](#36-power-collectors-tolist-groupingby-and-joining)
  - [3.7 Multi-Core Acceleration with `parallelStream()`](#37-multi-core-acceleration-with-parallelstream)
- [4. Prerequisite & Supporting Concepts](#4-prerequisite--supporting-concepts)
  - [Prerequisite / Supporting Concept: Imperative vs. Declarative Programming](#prerequisite--supporting-concept-imperative-vs-declarative-programming)
  - [Prerequisite / Supporting Concept: Single Abstract Method (SAM) & @FunctionalInterface](#prerequisite--supporting-concept-single-abstract-method-sam--functionalinterface)
  - [Prerequisite / Supporting Concept: Effectively Final Variables in Lambdas](#prerequisite--supporting-concept-effectively-final-variables-in-lambdas)
- [5. Advanced Depth (Intermediate → Advanced)](#5-advanced-depth-intermediate--advanced)
  - [5.1 Senior Deep Dive: `map()` vs. `flatMap()` with Nested Embeddings](#51-senior-deep-dive-map-vs-flatmap-with-nested-embeddings)
  - [5.2 Short-Circuiting Operations: `limit()`, `findFirst()`, and `anyMatch()`](#52-short-circuiting-operations-limit-findfirst-and-anymatch)
  - [5.3 Common Mistakes & Misconceptions (With Bad vs. Good Code)](#53-common-mistakes--misconceptions-with-bad-vs-good-code)
  - [5.4 Architectural Trade-Offs: Stream Pipeline Overhead vs. Primitive Loops](#54-architectural-trade-offs-stream-pipeline-overhead-vs-primitive-loops)
- [6. Quick Recap](#6-quick-recap)
- [7. Self-Check Questions & Practice Exercises](#7-self-check-questions--practice-exercises)
  - [Self-Check Questions (Basic to Advanced)](#self-check-questions-basic-to-advanced)
  - [Hands-On Practice Exercises with Full Solutions](#hands-on-practice-exercises-with-full-solutions)

---

# 1. Topic Overview

**Functional Programming** in Java treats computation as the evaluation of mathematical functions, avoiding mutable state and side-effects. The **Java Stream API (`java.util.stream`)** complements this paradigm by providing declarative, composable pipelines for processing collections of data through transformations such as filtering, mapping, sorting, and aggregation.

### Why This Topic Matters
In Generative AI systems, data preprocessing constitutes over 80% of data engineering pipelines. Before feeding raw documents into vector embeddings or LLM prompts, an application must filter out empty chunks, strip metadata noise, extract text representations, normalize case, and concatenate context passages. Traditional nested `for` loops create brittle, verbose code. The Stream API transforms these operations into clean, declarative, high-throughput pipelines that can be effortlessly parallelized across multi-core CPUs.

> 💡 **New Word Alert — "Lambda (`->`)"**: An anonymous function (a function without a name) that can be passed as a variable or argument to methods.

> 💡 **New Word Alert — "Stream"**: A sequence of elements supporting sequential and parallel aggregate operations, representing a pipeline of computation rather than a data storage container.

> 💡 **New Word Alert — "Lazy Evaluation"**: An execution strategy where intermediate operations are not evaluated until a terminal operation is invoked, optimizing performance by performing only the minimal work needed.

---

# 2. Basic Foundations (True Zero)

Let's start from true zero, assuming you have only written traditional `for` loops in Java.

### 2.1 What is Functional Programming, a Lambda, and a Stream?

- **Imperative Programming (Old Way)**: Telling the computer step-by-step *HOW* to do something (`int i = 0; i < list.size(); i++`).
- **Declarative / Functional Programming (New Way)**: Telling the computer *WHAT* you want done (`list.stream().filter(...).toList()`), letting the runtime optimize the traversal.
- **Lambda (`->`)**: An arrow representing a formula: `(input) -> action`. For example, `s -> s.length() > 10` means *"take string s and check if its length exceeds 10"*.
- **Stream**: A moving conveyor belt. It does not store items in RAM; it moves items from a source through inspection stations and packages them at the end.

---

### 2.2 The Factory Assembly Line Model

```
  1. SOURCE CRATE             2. INTERMEDIATE CONVEYOR BELT           3. TERMINAL PACKAGING
┌──────────────────┐       ┌─────────────────────────────────┐     ┌─────────────────────┐
│ rawList.stream() │  ──►  │ .filter(qualityCheck)           │ ──► │ .toList()           │
│                  │       │ .map(cleanText)                 │     │                     │
└──────────────────┘       └─────────────────────────────────┘     └─────────────────────┘
                               (Lazy: Sets up conveyor belt)         (Eager: Runs the line!)
```

1. **The Source**: Raw materials in a warehouse crate (`ArrayList`).
2. **Intermediate Stations**: Robotic arms on the belt (`filter`, `map`). They reject defectives and transform valid items.
3. **The Terminal Station**: The packaging station (`toList()`). **Nothing moves on the belt until this station is turned on!**

---

### 2.3 Minimal Working Example: Cleaning AI Text Chunks

Let's compare the imperative way vs. the modern functional stream way:

```java
import java.util.List;

public class StreamMinimalDemo {

    public static void main(String[] args) {
        List<String> rawChunks = List.of(
            "Short", 
            "Enterprise Spring AI Architecture Guide", 
            "", 
            "Deep Learning Vector Search Manual"
        );

        // Modern Declarative Stream Pipeline
        List<String> cleanChunks = rawChunks.stream()
            .filter(text -> text.length() > 20)     // Keep only text > 20 chars
            .map(String::toLowerCase)               // Convert to lowercase
            .toList();                              // Collect into an unmodifiable List

        System.out.println(cleanChunks);
    }
}
```

---

### 2.4 Line-by-Line Code Breakdown

1. `rawChunks.stream()`: Pushes the list onto a functional stream conveyor belt.
2. `.filter(text -> text.length() > 20)`: An intermediate operation using a `Predicate`. Discards `"Short"` and `""` because their lengths do not exceed 20.
3. `.map(String::toLowerCase)`: An intermediate operation using a method reference. Transforms `"Enterprise Spring AI..."` to `"enterprise spring ai..."`.
4. `.toList()`: The terminal operation. Flips the switch, runs the pipeline, and collects results into an immutable `List<String>`.
5. Output: `[enterprise spring ai architecture guide, deep learning vector search manual]`. Zero index counters, zero temporary variables!

---

# 3. Core Concept Walkthrough (Basic → Intermediate)

Now let's build the functional toolchain used across enterprise microservices and Spring AI.

### 3.1 The Big 4 Functional Interfaces

Java provides four primary functional interfaces in `java.util.function`:

| Interface Name | Method Signature | Plain English Meaning | Common Stream Method | AI Example |
| :--- | :--- | :--- | :--- | :--- |
| **`Predicate<T>`** | `boolean test(T t)` | The Bouncer: returns `true` or `false`. | `.filter()` | `chunk -> chunk.tokenCount() >= 50` |
| **`Function<T, R>`**| `R apply(T t)` | The Transformer: converts $T$ to $R$. | `.map()` | `doc -> doc.getContent()` |
| **`Consumer<T>`** | `void accept(T t)` | The Worker: performs an action, returns nothing. | `.forEach()` | `prompt -> log.info("Prompt: {}", prompt)` |
| **`Supplier<T>`** | `T get()` | The Factory: takes nothing, creates a new $T$. | `.orElseGet()` | `() -> new OpenAiClient(key)` |

---

### 3.2 Method References (`::`): Clean Syntactic Shorthand

When a lambda does nothing except invoke an existing method on its parameter, replace it with a **Method Reference (`::`)**:

| Verbose Lambda | Method Reference | Meaning |
| :--- | :--- | :--- |
| `s -> s.toLowerCase()` | `String::toLowerCase` | Call instance method on argument |
| `item -> System.out.println(item)` | `System.out::println` | Pass argument to print stream |
| `doc -> doc.getId()` | `Document::getId` | Call getter on object |
| `() -> new ArrayList<>()` | `ArrayList::new` | Call constructor |

---

### 3.3 Anatomy of a Stream: Source, Intermediate, and Terminal Stages

Every stream has 3 distinct stages:
1. **Source**: Collection, array, or I/O channel (`list.stream()`, `Files.lines(path)`).
2. **Intermediate Operations**: Return a new stream and register transformation steps lazily (`filter`, `map`, `flatMap`, `sorted`, `distinct`).
3. **Terminal Operation**: Triggers traversal, produces a final result, and closes the stream (`toList`, `count`, `reduce`, `findFirst`).

---

### 3.4 Lazy Evaluation in Action: Proof of Zero Work Until Terminal

Intermediate operations **never run** until a terminal operation is called:

```java
List<String> prompts = List.of("Short", "A very detailed prompt for LLM", "Hi");

// This pipeline does NOT execute yet!
var stream = prompts.stream()
    .filter(p -> {
        System.out.println("Filtering: " + p);
        return p.length() > 10;
    });

System.out.println("Pipeline created. Triggering terminal operation now...");
String firstMatch = stream.findFirst().orElse("None");
System.out.println("Result: " + firstMatch);
```

**Output:**
```text
Pipeline created. Triggering terminal operation now...
Filtering: Short
Filtering: A very detailed prompt for LLM
Result: A very detailed prompt for LLM
```
Notice: The stream stopped as soon as it found the first match! It **never inspected `"Hi"`**. That is the efficiency of lazy short-circuiting.

---

### 3.5 Core Intermediate Operations: `filter`, `map`, and `flatMap`

#### 1. `filter`: Discarding invalid elements
```java
List<Document> validDocs = documents.stream()
    .filter(d -> d.content() != null && !d.content().isBlank())
    .toList();
```

#### 2. `map`: 1-to-1 field transformation
```java
List<String> contents = validDocs.stream()
    .map(Document::content)
    .map(String::trim)
    .toList();
```

#### 3. `flatMap`: 1-to-Many flattening
If each `Document` has a method `List<Chunk> getChunks()`, using `.map()` would produce `Stream<List<Chunk>>`. Using `.flatMap()` flattens all chunks into a single unified `Stream<Chunk>`:

```
 Document 1 ──► [ Chunk 1A, Chunk 1B ]
                                         ──► flatMap ──► Stream of [ 1A, 1B, 2A, 2B ]
 Document 2 ──► [ Chunk 2A, Chunk 2B ]
```

```java
List<Chunk> allChunks = validDocs.stream()
    .flatMap(doc -> doc.getChunks().stream())
    .toList();
```

---

### 3.6 Power Collectors: `toList()`, `groupingBy()`, and `joining()`

#### 1. Partitioning by Model with `groupingBy`:
```java
public record LLMCallLog(String model, int tokensUsed) {}

// Grouping into Map<String, List<LLMCallLog>>
Map<String, List<LLMCallLog>> logsByModel = logs.stream()
    .collect(Collectors.groupingBy(LLMCallLog::model));

// Aggregating total tokens per model: Map<String, Integer>
Map<String, Integer> tokensPerModel = logs.stream()
    .collect(Collectors.groupingBy(
        LLMCallLog::model,
        Collectors.summingInt(LLMCallLog::tokensUsed)
    ));
```

#### 2. Concatenating Context with `joining`:
In RAG, multiple retrieved passages must be formatted into a single prompt string:

```java
List<String> chunks = List.of(
    "Virtual threads reduce memory footprint.",
    "Spring AI supports PostgreSQL pgvector.",
    "HNSW indexes optimize vector similarity searches."
);

String promptContext = chunks.stream()
    .collect(Collectors.joining("\n---\n", "[CONTEXT START]\n", "\n[CONTEXT END]"));

System.out.println(promptContext);
```

---

### 3.7 Multi-Core Acceleration with `parallelStream()`

For CPU-heavy in-memory transformations (such as computing cosine similarities or vector normalization across 50,000 items), `.parallelStream()` splits the work across all available CPU cores using the **ForkJoinPool**:

```java
// Uses ALL available CPU cores automatically:
List<double[]> normalizedVectors = rawVectors.parallelStream()
    .map(VectorMath::normalize)
    .toList();
```

---

# 4. Prerequisite & Supporting Concepts

### Prerequisite / Supporting Concept: Imperative vs. Declarative Programming

- **Imperative**: The programmer manages state mutations, loop indices, and flow control manually. Prone to off-by-one errors and concurrency bugs.
- **Declarative**: The programmer specifies desired transformations through composable operations, delegating traversal mechanics to the runtime engine.

---

### Prerequisite / Supporting Concept: Single Abstract Method (SAM) & @FunctionalInterface

A functional interface contains **exactly one abstract method**. It can have any number of `default` or `static` methods. The `@FunctionalInterface` annotation instructs the compiler to verify that only one abstract method exists.

---

### Prerequisite / Supporting Concept: Effectively Final Variables in Lambdas

Any local variable defined outside a lambda and referenced inside it must be `final` or **effectively final** (its value is never reassigned). This prevents race conditions and stack frame synchronization issues.

```java
int limit = 50; // Never modified -> Effectively final!
prompts.stream().filter(p -> p.length() > limit); // ✅ Compiles!

int badLimit = 50;
badLimit = 100; // Reassigned!
// prompts.stream().filter(p -> p.length() > badLimit); // ❌ COMPILE ERROR!
```

---

# 5. Advanced Depth (Intermediate → Advanced)

### 5.1 Senior Deep Dive: `map()` vs. `flatMap()` with Nested Embeddings

A classic interview and production problem:
- **`map()`**: Produces a stream where each input element results in exactly one output element ($T \rightarrow R$).
- **`flatMap()`**: Produces a stream where each input element is mapped to a stream of sub-elements, which are then merged into a single continuous stream ($T \rightarrow \text{Stream}<R>$).

```java
List<String> sentences = List.of("hello world", "java twenty one streams");

// 1. map(): List of 2 String[] arrays
List<String[]> nested = sentences.stream()
    .map(s -> s.split(" "))
    .toList();

// 2. flatMap(): Flattened List of 5 individual word strings!
List<String> words = sentences.stream()
    .flatMap(s -> Arrays.stream(s.split(" ")))
    .toList(); // ["hello", "world", "java", "twenty", "one", "streams"]
```

---

### 5.2 Short-Circuiting Operations: `limit()`, `findFirst()`, and `anyMatch()`

Short-circuiting operations terminate processing the instant their condition is fulfilled, allowing streams to operate on infinite sources without hanging:
- `limit(n)`: Stops after $n$ elements.
- `anyMatch(Predicate)`: Returns `true` the moment the first match is encountered.
- `findFirst()` / `findAny()`: Returns an `Optional` containing the first located element.

---

### 5.3 Common Mistakes & Misconceptions (With Bad vs. Good Code)

#### Mistake 1: Reusing a Closed Stream
**Bad Code:**
```java
Stream<String> stream = List.of("a", "b").stream();
stream.forEach(System.out::println); // Terminal operation executed!

// ❌ CRASHES with IllegalStateException: stream has already been operated upon or closed!
long count = stream.count();
```
**Correct Code:**
```java
List<String> list = List.of("a", "b");
list.forEach(System.out::println);
long count = list.stream().count(); // ✅ Obtain a new stream!
```

#### Mistake 2: Mutating External Shared State in Lambdas
**Bad Code:**
```java
// ❌ Thread-unsafe, violates functional purity!
List<String> results = new ArrayList<>();
rawDocs.stream().filter(d -> d.length() > 10).forEach(results::add);
```
**Correct Code:**
```java
// ✅ Clean, thread-safe collection
List<String> results = rawDocs.stream()
    .filter(d -> d.length() > 10)
    .toList();
```

#### Mistake 3: Using `parallelStream()` for Blocking HTTP Calls
The common `ForkJoinPool` has a thread count equal to CPU cores. Blocking worker threads with slow HTTP calls starves all other parallel streams in the entire JVM! Use **Virtual Threads** (Day 07) for I/O concurrency.

---

### 5.4 Architectural Trade-Offs: Stream Pipeline Overhead vs. Primitive Loops

For tiny collections ($N < 20$) of primitive numbers, a traditional `for` loop executes in slightly fewer CPU cycles because it avoids stream object allocations. However, for collections of objects ($N \ge 100$), the readability, immutability, and parallelization advantages of Streams far outweigh microscopic nanosecond differences.

---

# 6. Quick Recap

| Operation | Type | What It Does |
| :--- | :--- | :--- |
| **`filter(Predicate)`** | Intermediate (Lazy) | Keeps elements satisfying condition. |
| **`map(Function)`** | Intermediate (Lazy) | Transforms $T \rightarrow R$ in 1-to-1 mapping. |
| **`flatMap(Function)`**| Intermediate (Lazy) | Transforms $T \rightarrow \text{Stream}<R>$ and flattens results. |
| **`distinct()`** | Intermediate (Lazy) | Filters duplicates via `equals()` / `hashCode()`. |
| **`toList()`** | Terminal (Eager) | Collects into an unmodifiable `List<T>`. |
| **`groupingBy(key)`** | Terminal (Eager) | Groups items into a `Map<K, List<V>>`. |
| **`joining(delim)`** | Terminal (Eager) | Concatenates strings into a formatted block. |
| **`parallelStream()`** | Multi-Core Pipeline | Parallelizes CPU-bound tasks via ForkJoinPool. |

---

# 7. Self-Check Questions & Practice Exercises

### Self-Check Questions (Basic to Advanced)

1. **What is the difference between an intermediate operation and a terminal operation?**
   - *Answer*: Intermediate operations (like `filter`, `map`) are lazy and return a new `Stream` without executing. Terminal operations (like `toList`, `count`) are eager, trigger the traversal, and produce a result or side-effect.
2. **When should you use `flatMap()` instead of `map()`?**
   - *Answer*: Use `map()` for 1-to-1 transformations. Use `flatMap()` when each element maps to a collection or stream (1-to-many), flattening nested streams into a single composite stream.
3. **What occurs if you attempt to invoke a terminal operation on a Stream that has already executed a terminal operation?**
   - *Answer*: The JVM throws an `IllegalStateException: stream has already been operated upon or closed`. Streams are single-use only.
4. **Why is `parallelStream()` unsuitable for blocking network I/O calls to AI APIs?**
   - *Answer*: `parallelStream()` uses the shared common `ForkJoinPool`, which has a small fixed thread pool equal to CPU cores. Blocking these threads with network latency starves the entire JVM.
5. **How does `Collectors.joining()` aid in RAG prompt engineering?**
   - *Answer*: It concatenates multiple retrieved text chunks into a unified context prompt string with custom delimiters, prefixes, and suffixes.

---

### Hands-On Practice Exercises with Full Solutions

#### 🏋️ Exercise 1: Build an AI Document Preprocessing Pipeline
**Objective**: Given raw document texts, build a fluent Stream pipeline that filters null/blank strings, strips whitespace, removes text $< 20$ characters, converts to uppercase, limits to 3 items, and collects into a list.

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

#### 🏋️ Exercise 2: LLM Cost & Usage Analytics with `Collectors.groupingBy()`
**Objective**: Given a list of `LLMRecord(String model, int promptTokens, int completionTokens)`, calculate a `Map<String, Double>` computing total USD cost per model.

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

#### 🏋️ Exercise 3: Finding the Second Highest Number with Streams
**Objective**: Write a stream expression that extracts the second highest unique number from an integer list.

```java
package com.javagenai.day06;

import java.util.Comparator;
import java.util.List;

public class SecondHighestFinder {

    public static int findSecondHighest(List<Integer> numbers) {
        return numbers.stream()
            .distinct()
            .sorted(Comparator.reverseOrder())
            .skip(1)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("List does not contain at least 2 unique numbers"));
    }
}
```

---

<p align="center">
  <b>Day 06 Complete! 🎉</b><br>
  Proceed to <b>Day 07</b>: <b>Concurrency & Virtual Threads (Project Loom)</b>.<br>
  <a href="../Day_07_Concurrency_Virtual_Threads/Day_07_Concurrency_Virtual_Threads.md"><b>Continue to Day 07 →</b></a>
</p>
