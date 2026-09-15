# Day_06 — Functional Programming, Streams

| ⬅️ Previous Day | 📚 Course Hub | ➡️ Next Day |
|:---|:---:|---:|
| [← Day 05: Modern Java: Records, Optional, Sealed](../Day_05_Modern_Java_Records_Optional_Sealed/Day_05_Modern_Java_Records_Optional_Sealed.md) | [All 60 Days Overview](../../README.md) | [Day 07: Concurrency & Virtual Threads →](../Day_07_Concurrency_Virtual_Threads/Day_07_Concurrency_Virtual_Threads.md) |

---

## 🎯 What You'll Understand By the End
- How **Lambda Expressions** (`->`) and **Method References** (`::`) let you pass logic into methods just like variables.
- The 4 foundational **Functional Interfaces** (`Predicate`, `Function`, `Consumer`, and `Supplier`) that power all modern Java libraries.
- How the **Stream API** transforms tedious, error-prone `for` loops into elegant declarative data pipelines.
- The crucial difference between **Intermediate Operations** (lazy filters and transformations) and **Terminal Operations** (eager execution).
- How to filter, transform, and score large lists of AI document chunks in a few readable lines.

---

## 🧠 The Problem This Solves

Before Java 8 introduced functional programming and the Stream API, manipulating collections required nested, verbose imperative loops:

```java
// Imperative Java (The Old, Painful Way):
List<String> highQualityChunks = new ArrayList<>();
for (DocumentChunk chunk : allChunks) {
    if (chunk.relevanceScore() > 0.85) { // Manual filtering
        String cleanText = chunk.text().trim().toLowerCase(); // Manual transformation
        if (!highQualityChunks.contains(cleanText)) { // Manual deduplication
            highQualityChunks.add(cleanText);
        }
    }
}
```

This traditional approach suffers from three major flaws:
1. **Accidental Complexity**: 80% of the code is boilerplate iteration logic (`for`, `if`, index tracking, temporary lists), drowning out the actual business logic.
2. **High Mutation Risk**: You are constantly mutating temporary lists, making the code fragile and prone to multithreading concurrency bugs.
3. **Hard to Parallelize**: Making an imperative `for` loop run safely across multiple CPU cores requires complex thread synchronization code.

The **Stream API** replaces *how to loop* with *what you want to happen* using declarative pipelines.

---

## 📖 Core Concept, Explained Simply

### The Factory Conveyor Belt Analogy

Think of a Java Stream like an automated factory conveyor belt:

1. **The Source (Loading Dock)**: A pallet of raw items (your initial `List<DocumentChunk>`) is placed onto the belt.
2. **Intermediate Stations (Lazy Filters & Processors)**:
   - *Station 1 (`filter`)*: A robotic arm scans each chunk and knocks off any item with a quality score below 0.85.
   - *Station 2 (`map`)*: A processing arm strips extra whitespace and converts text to lowercase.
   - *Station 3 (`sorted`)*: Items are arranged by relevance.
   *Crucial detail*: The conveyor belt does **not** move an inch until the final shipping box is ready! This is called **lazy evaluation** — nothing executes until a terminal operation is called.
3. **The Terminal Station (Packing & Shipping)**:
   - *`collect(toList())`*: Packs the processed items into a brand-new, clean list and stops the belt.

### The Big Four Functional Interfaces

In Java, functions aren't completely standalone; they are represented by **Functional Interfaces** (interfaces with exactly one abstract method):

| Interface | Input $\rightarrow$ Output | Purpose | Example |
|:---|:---:|:---|:---|
| **`Predicate<T>`** | `T -> boolean` | Tests a condition | `score -> score > 0.85` |
| **`Function<T, R>`** | `T -> R` | Transforms an item into another type | `doc -> doc.getContent()` |
| **`Consumer<T>`** | `T -> void` | Consumes an item, performs an action | `msg -> System.out.println(msg)` |
| **`Supplier<T>`** | `() -> T` | Supplies a new value on demand | `() -> UUID.randomUUID()` |

> 💡 **New Word Alert — "Stream"**: A sequence of elements supporting sequential and parallel aggregate operations. A stream carries values from a data source through a pipeline; it does **not** store data itself.

> 💡 **New Word Alert — "Lazy Evaluation"**: Computation that is deferred until the final result is explicitly required by a terminal operation.

---

## 🗺️ Visual Overview

```mermaid
flowchart LR
    A["<b>Data Source</b><br>List of Chunks"] -->|stream()| B["Stream Opened"]
    
    subgraph Pipeline ["Intermediate Operations (Lazy Processing)"]
        direction LR
        B --> C["<b>filter()</b><br>score &gt; 0.85"]
        C --> D["<b>map()</b><br>Extract & Clean Text"]
        D --> E["<b>distinct()</b><br>Remove Duplicates"]
    end
    
    subgraph Terminal ["Terminal Operation (Eager Execution)"]
        E --> F["<b>collect(toList())</b>"]
    end
    
    F --> G["<b>Final Output</b><br>New Clean List"]
```

*This diagram illustrates the Stream lifecycle. Data flows from a source collection into intermediate operations (`filter`, `map`, `distinct`). These steps only execute when triggered by the terminal operation (`collect`), producing a new result without altering the original list.*

---

## 💻 Code Walkthrough

Here is a runnable Java 17+ program demonstrating a real-world AI document-retrieval pipeline using Streams:

```java
import java.util.List;
import java.util.stream.Collectors;

record DocumentChunk(String id, String text, double similarityScore) {}

public class AiStreamPipeline {
    public static void main(String[] args) {
        List<DocumentChunk> rawChunks = List.of(
            new DocumentChunk("c1", "  Introduction to Vector Databases.  ", 0.92),
            new DocumentChunk("c2", "Irrelevant user comment.", 0.45),
            new DocumentChunk("c3", "  INTRODUCTION TO VECTOR DATABASES.  ", 0.94),
            new DocumentChunk("c4", "How to configure pgvector in PostgreSQL. ", 0.88),
            new DocumentChunk("c5", "System heartbeat check.", 0.12)
        );

        // Process chunks using a declarative Stream pipeline
        List<String> topContextSnippets = rawChunks.stream()
            // 1. Filter: Keep only high-similarity chunks
            .filter(chunk -> chunk.similarityScore() >= 0.85)
            // 2. Map: Clean text and convert to lowercase
            .map(chunk -> chunk.text().trim().toLowerCase())
            // 3. Deduplicate identical text
            .distinct()
            // 4. Sort alphabetically
            .sorted()
            // 5. Terminal: Collect into a final list
            .collect(Collectors.toList());

        System.out.println("--- Top AI Context Snippets ---");
        topContextSnippets.forEach(System.out::println);
    }
}
```

### Line-by-Line Breakdown

| Code Pipeline Stage | Plain-English Explanation |
|:---|:---|
| `rawChunks.stream()` | Opens a stream from the source list. The original `rawChunks` list is never modified. |
| `.filter(chunk -> chunk.similarityScore() >= 0.85)` | Uses a `Predicate` lambda. Drops `c2` and `c5` because their scores fall below the threshold. |
| `.map(chunk -> chunk.text().trim().toLowerCase())` | Uses a `Function` lambda. Transforms each `DocumentChunk` object into a cleaned `String`. |
| `.distinct()` | Removes duplicate cleaned strings (combining the normalized versions of `c1` and `c3`). |
| `.sorted()` | Arranges remaining strings in natural alphabetical order. |
| `.collect(Collectors.toList())` | The **terminal operation**. Triggers the pipeline and gathers the resulting strings into a new `List<String>`. |
| `topContextSnippets.forEach(System.out::println)` | Uses a **method reference** (`System.out::println`) instead of a lambda (`s -> System.out.println(s)`) to print each item. |

---

## 🔑 Key Terminology

| Term | Plain-English Meaning |
|:---|:---|
| **Lambda Expression (`->`)** | An anonymous function that can be created and passed around as a value (e.g., `x -> x * 2`). |
| **Method Reference (`::`)** | A compact, readable shorthand syntax for calling an existing method by name (e.g., `String::toUpperCase`). |
| **Functional Interface** | An interface that contains exactly one abstract method (annotated with `@FunctionalInterface`). |
| **Intermediate Operation** | A stream operation (like `filter` or `map`) that returns a new stream and executes lazily. |
| **Terminal Operation** | A stream operation (like `collect`, `count`, or `forEach`) that triggers execution and produces a result. |
| **Lazy Evaluation** | Delaying execution until the exact moment a terminal operation requests the final data. |

---

## ⚠️ Common Beginner Mistakes

### 1. Reusing a Stream After Calling a Terminal Operation
A Stream can only be traversed **once**. Once a terminal operation executes, the stream is consumed and closed.

❌ **Wrong Way**:
```java
Stream<String> stream = List.of("gpt-4o", "claude-3-5").stream();
long count = stream.count(); // Terminal operation 1: Consumes stream!

// Reusing the same stream:
List<String> list = stream.collect(Collectors.toList()); 
// CRASH! IllegalStateException: stream has already been operated upon or closed
```

✅ **Right Way**:
```java
List<String> models = List.of("gpt-4o", "claude-3-5");
long count = models.stream().count();
List<String> list = models.stream().collect(Collectors.toList()); // Open a fresh stream!
```
*Why it is wrong*: Streams represent transient pipelines of data, not permanent storage structures.

---

### 2. Forgetting the Terminal Operation
Because intermediate operations are lazy, if you don't call a terminal operation, **nothing will execute**.

❌ **Wrong Way**:
```java
List<String> prompts = List.of("Prompt 1", "Prompt 2");
prompts.stream().map(p -> {
    System.out.println("Processing: " + p); // NEVER RUNS!
    return p.toUpperCase();
});
// The map method was never triggered because there was no terminal operation!
```

✅ **Right Way**:
```java
prompts.stream()
    .map(String::toUpperCase)
    .forEach(p -> System.out.println("Processing: " + p)); // forEach is a terminal operation!
```

---

### 3. Mutating External State Inside a Stream
Streams are designed for pure functional programming. Modifying shared variables outside the stream causes unpredictable bugs, especially if parallelized.

❌ **Wrong Way**:
```java
List<String> results = new ArrayList<>();
models.stream().filter(m -> m.startsWith("gpt")).forEach(results::add); // Anti-pattern!
```

✅ **Right Way**:
```java
List<String> results = models.stream()
    .filter(m -> m.startsWith("gpt"))
    .collect(Collectors.toList()); // Pure functional collection!
```

---

## ✅ Best Practices

1. **Keep Lambdas Short and Focused**: If a lambda exceeds 2-3 lines of code, extract it into a separate named private method and use a **method reference** (`this::myMethod`).
2. **Never Mutate Data in Streams**: Treat elements passing through a stream as read-only. Always use `map()` to create new transformed objects.
3. **Prefer Method References Where Possible**: Writing `String::toLowerCase` is cleaner and easier to read than `s -> s.toLowerCase()`.

---

## 🔭 Looking Ahead
In **Day_07**, we will tackle **Concurrency & Virtual Threads**, discovering how Java 21's lightweight virtual threads can process thousands of streaming AI network requests simultaneously without choking system memory.

---

## 📝 Quick Recap
- **Lambdas (`->`)** let you treat units of executable logic as first-class citizens.
- **`Predicate`** tests conditions; **`Function`** transforms data; **`Consumer`** performs side effects; **`Supplier`** provides values.
- **Streams** process collections declaratively without mutating the underlying data source.
- Intermediate operations (`filter`, `map`, `sorted`) are **lazy**; they do not run until a terminal operation (`collect`, `count`, `forEach`) pulls data through.
- A stream cannot be reused after a terminal operation has consumed it.

---

## 🧪 Try It Yourself

1. **Filter AI Models by Cost**: Given a list of model records with `name` and `costPerToken`, write a stream pipeline that filters for models costing less than $0.01 and collects their names in uppercase.
2. **Token Counter**: Given a list of sentences, write a stream pipeline that uses `.mapToInt(...)` and `.sum()` to calculate the total number of words across all sentences.
3. **Predicate Composition**: Create two predicates: `isSafe` and `isHighConfidence`. Combine them using `.and()` (`isSafe.and(isHighConfidence)`) and use the combined predicate to filter a stream of incoming prompts.
