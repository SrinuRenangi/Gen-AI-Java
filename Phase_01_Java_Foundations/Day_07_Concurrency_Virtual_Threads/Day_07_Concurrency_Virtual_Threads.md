# 🧵 Day 07: Concurrency & Virtual Threads
## Project Loom — 10,000 Concurrent LLM Calls on a Single Server

| ⬅️ Previous Day | 📚 Course Hub | ➡️ Next Day |
|:---|:---:|---:|
| [← Day 06: Functional Programming & Stream API](../Day_06_Functional_Programming_Streams/Day_06_Functional_Programming_Streams.md) | [All 60 Days Overview](../../README.md) | [Day 08: I/O, HTTP Client, JSON & Testing →](../Day_08_IO_HTTP_JSON_Testing/Day_08_IO_HTTP_JSON_Testing.md) |

[![Phase](https://img.shields.io/badge/Phase_01-Java_Foundations-brightgreen.svg?style=for-the-badge)](../../README.md)
[![Day](https://img.shields.io/badge/Day-07_of_60-blue.svg?style=for-the-badge)](../../README.md)
[![Difficulty](https://img.shields.io/badge/Difficulty-Advanced-red.svg?style=for-the-badge)](../../README.md)
[![Java 21 Feature](https://img.shields.io/badge/Java_21-Virtual_Threads_(Project_Loom)-purple.svg?style=for-the-badge)](../../README.md)

---

![Traditional Platform Threads vs Java 21 Virtual Threads](assets/day07_virtual_threads.jpg)

## 🗺️ Table of Contents
- [1. Topic Overview](#1-topic-overview)
- [2. Basic Foundations (True Zero)](#2-basic-foundations-true-zero)
  - [2.1 What is Concurrency, a Thread, and I/O-Bound Work?](#21-what-is-concurrency-a-thread-and-io-bound-work)
  - [2.2 The Restaurant Waiter vs. Dedicated Butler Analogy](#22-the-restaurant-waiter-vs-dedicated-butler-analogy)
  - [2.3 Minimal Working Example: Launching a Virtual Thread](#23-minimal-working-example-launching-a-virtual-thread)
  - [2.4 Line-by-Line Code Breakdown](#24-line-by-line-code-breakdown)
- [3. Core Concept Walkthrough (Basic → Intermediate)](#3-core-concept-walkthrough-basic--intermediate)
  - [3.1 The Concurrency Crisis in Generative AI](#31-the-concurrency-crisis-in-generative-ai)
  - [3.2 Platform Threads vs. Virtual Threads (Project Loom)](#32-platform-threads-vs-virtual-threads-project-loom)
  - [3.3 How Virtual Threads Work: Mounting & Unmounting on Carrier Threads](#33-how-virtual-threads-work-mounting--unmounting-on-carrier-threads)
  - [3.4 Spawning 10,000 Virtual Threads in 3 Lines](#34-spawning-10000-virtual-threads-in-3-lines)
  - [3.5 Asynchronous Composition with `CompletableFuture`](#35-asynchronous-composition-with-completablefuture)
  - [3.6 Thread Safety & Atomic Operations (`AtomicLong`)](#36-thread-safety--atomic-operations-atomiclong)
  - [3.7 Python GIL vs. Java Virtual Threads: The Production Truth](#37-python-gil-vs-java-virtual-threads-the-production-truth)
- [4. Prerequisite & Supporting Concepts](#4-prerequisite--supporting-concepts)
  - [Prerequisite / Supporting Concept: OS Processes vs. Threads](#prerequisite--supporting-concept-os-processes-vs-threads)
  - [Prerequisite / Supporting Concept: Race Conditions and Critical Sections](#prerequisite--supporting-concept-race-conditions-and-critical-sections)
  - [Prerequisite / Supporting Concept: The Java Memory Model & Volatile Keyword](#prerequisite--supporting-concept-the-java-memory-model--volatile-keyword)
- [5. Advanced Depth (Intermediate → Advanced)](#5-advanced-depth-intermediate--advanced)
  - [5.1 Senior Deep Dive: `CompletableFuture` Composition (`thenApply`, `thenCompose`, `thenCombine`)](#51-senior-deep-dive-completablefuture-composition-thenapply-thencompose-thencombine)
  - [5.2 Thread Starvation: The Default `ForkJoinPool.commonPool()` Hazard](#52-thread-starvation-the-default-forkjoinpoolcommonpool-hazard)
  - [5.3 Virtual Thread Pinning: `synchronized` vs. `ReentrantLock`](#53-virtual-thread-pinning-synchronized-vs-reentrantlock)
  - [5.4 Common Mistakes & Misconceptions (With Bad vs. Good Code)](#54-common-mistakes--misconceptions-with-bad-vs-good-code)
  - [5.5 Architectural Trade-Offs: Virtual Threads vs. Reactive WebFlux](#55-architectural-trade-offs-virtual-threads-vs-reactive-webflux)
- [6. Quick Recap](#6-quick-recap)
- [7. Self-Check Questions & Practice Exercises](#7-self-check-questions--practice-exercises)
  - [Self-Check Questions (Basic to Advanced)](#self-check-questions-basic-to-advanced)
  - [Hands-On Practice Exercises with Full Solutions](#hands-on-practice-exercises-with-full-solutions)

---

# 1. Topic Overview

Concurrency is the art of coordinating multiple independent tasks simultaneously. In Java 21, **Virtual Threads (Project Loom)** introduce ultra-lightweight, user-space threads that decouple execution logic from scarce operating system kernel threads, while **`CompletableFuture`** and **atomic primitives** enable expressive asynchronous composition and lock-free thread safety.

### Why This Topic Matters
Generative AI web applications are overwhelmingly **I/O-bound**. When a user requests an LLM inference, the server's CPU does virtually zero work while waiting 2 to 5 seconds for network packets to return from OpenAI, Claude, or local Ollama servers. With traditional operating system threads, handling 5,000 concurrent user requests consumes 5 to 10 GB of RAM and triggers server crashes. With Java 21 Virtual Threads, a single standard server can easily manage 100,000+ simultaneous connections with minimal memory and plain synchronous code.

> 💡 **New Word Alert — "Virtual Thread"**: An ultra-lightweight thread managed entirely by the JVM rather than the OS kernel. It occupies only ~250 bytes of heap memory at creation and unmounts automatically during blocking network operations.

> 💡 **New Word Alert — "I/O-Bound"**: A computing workload whose throughput is limited by data transfer speeds across network sockets or disk drives rather than raw CPU math.

> 💡 **New Word Alert — "Race Condition"**: A concurrency defect occurring when multiple concurrent threads attempt to read and mutate the same shared state without synchronization, corrupting data.

---

# 2. Basic Foundations (True Zero)

Let's start from true zero, assuming no prior experience with multithreading.

### 2.1 What is Concurrency, a Thread, and I/O-Bound Work?

- **Concurrency**: Doing multiple things at the same time (like cooking rice, baking chicken, and setting the table concurrently).
- **Thread**: A single worker following a list of step-by-step instructions.
- **CPU-Bound**: Heavy mathematics (e.g., training an AI model or rendering 3D graphics) where the CPU runs at 100%.
- **I/O-Bound**: Waiting for external data (e.g., waiting 3 seconds for OpenAI's API to respond over the internet). The CPU is completely idle.

---

### 2.2 The Restaurant Waiter vs. Dedicated Butler Analogy

```
                     PLATFORM THREADS (The Dedicated Butler)
┌─────────────────────────────────────────────────────────────────────────────┐
│ You hire 1 personal butler for each customer. When a customer orders steak, │
│ the butler walks to the kitchen and STANDS FROZEN for 30 minutes waiting for│
│ the oven. You need 1,000 butlers for 1,000 customers! Payroll goes bankrupt.│
└─────────────────────────────────────────────────────────────────────────────┘
                                      vs.
                     VIRTUAL THREADS (The Master Waiter)
┌─────────────────────────────────────────────────────────────────────────────┐
│ You hire 8 master waiters (Carrier Threads). When Table 1 orders steak, the │
│ waiter submits the ticket to the kitchen, immediately walks to Table 2 to   │
│ take an order, pours water for Table 3, and returns to Table 1 only when    │
│ the kitchen bell rings that the steak is ready!                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

In Java 21:
- The **Customers** are incoming user requests.
- The **Kitchen** is the external AI model generating tokens.
- The **Waiters** are physical CPU cores (Carrier Threads).
- **Virtual Threads** allow millions of orders to proceed concurrently without hiring millions of expensive physical butlers.

---

### 2.3 Minimal Working Example: Launching a Virtual Thread

Let's write a minimal, fully runnable Java program launching a virtual thread:

```java
public class SimpleVirtualThreadDemo {

    public static void main(String[] args) throws InterruptedException {
        // Launch a lightweight virtual thread
        Thread vThread = Thread.ofVirtual().start(() -> {
            System.out.println("Running on Virtual Thread: " + Thread.currentThread());
        });

        // Wait for the virtual thread to complete
        vThread.join();
        System.out.println("Main thread completed.");
    }
}
```

---

### 2.4 Line-by-Line Code Breakdown

1. `Thread.ofVirtual().start(...)`: Instructs the JVM to create a user-space virtual thread in the Heap and begin running the lambda task immediately.
2. `Thread.currentThread()`: Prints metadata revealing that the task is executing on a `VirtualThread` mounted on a `ForkJoinPool` carrier worker.
3. `vThread.join()`: Pauses the calling main thread until the virtual thread finishes execution.

---

# 3. Core Concept Walkthrough (Basic → Intermediate)

Now let's examine how Virtual Threads transform enterprise AI engineering.

### 3.1 The Concurrency Crisis in Generative AI

| Request Type | Typical Latency | What Thread Does During Latency |
| :--- | :---: | :--- |
| **Database Query** | 2 ms – 10 ms | Brief wait for indexed records |
| **Cache Lookup** | 0.5 ms – 1 ms | Near instant |
| **LLM Generation (GPT-4o)** | **2,000 ms – 10,000 ms** | **Stuck completely idle doing zero CPU work!** |

Because LLM generation takes seconds instead of milliseconds, waiting requests pile up rapidly. With traditional threads, 1,000 concurrent users tie up 1,000 operating system threads, crashing servers with `OutOfMemoryError: unable to create native thread`.

---

### 3.2 Platform Threads vs. Virtual Threads (Project Loom)

| Dimension | Platform Thread (Legacy) | Virtual Thread (Java 21) |
| :--- | :--- | :--- |
| **Management** | OS Kernel | JVM User-Space |
| **Memory Stack Size** | 1 MB to 2 MB fixed | ~250 bytes dynamically growing on Heap |
| **Creation Cost** | Heavy system call (~1ms) | Fast Java object allocation (nanoseconds) |
| **Safe Concurrency Limit**| ~2,000 to 5,000 per JVM | **1,000,000+ per JVM** |
| **Context Switching** | Expensive OS kernel switch | Lightweight Java continuation swap |
| **Pooling Rule** | **Must pool** (`FixedThreadPool`) | **NEVER pool** (create per task!) |

---

### 3.3 How Virtual Threads Work: Mounting & Unmounting on Carrier Threads

```
┌────────────────────────────────────────────────────────────────────────┐
│                        JVM USER-SPACE MEMORY                           │
│   [VT 1]   [VT 2]   [VT 3]   [VT 4]   ...   [VT 100,000]               │
│   (Tiny: ~250 bytes stack per virtual thread, stored in Java Heap)     │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Mounts & Unmounts dynamically
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                   CARRIER THREADS (ForkJoinPool)                       │
│                   Pool size = Number of CPU Cores (e.g. 16 cores)      │
│   [Core 1]  [Core 2]  [Core 3]  ...  [Core 16]                         │
└────────────────────────────────────────────────────────────────────────┘
```

1. **Mounting**: When a virtual thread has CPU code to run, the JVM mounts it onto an available OS **Carrier Thread**.
2. **Unmounting**: When the virtual thread performs a blocking operation (e.g., `HttpClient.send()`, `socket.read()`, `Thread.sleep()`), the JVM **unmounts** it, parks its stack on the Heap, and frees the Carrier Thread immediately to run another user's task!
3. **Resumption**: When the network response arrives, the JVM wakes up the virtual thread and mounts it onto any available carrier thread to resume execution.

---

### 3.4 Spawning 10,000 Virtual Threads in 3 Lines

```java
package com.javagenai.day07;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;

public class VirtualThreadScaleDemo {

    public static void main(String[] args) {
        Instant start = Instant.now();

        // 1. Create a Virtual-Thread-Per-Task Executor
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 1; i <= 10_000; i++) {
                final int taskId = i;
                executor.submit(() -> {
                    // Simulating 1 second of blocking network I/O from an LLM call
                    Thread.sleep(Duration.ofSeconds(1));
                    if (taskId % 2500 == 0) {
                        System.out.printf("Finished simulated LLM call #%,d on %s%n", 
                                          taskId, Thread.currentThread());
                    }
                    return taskId;
                });
            }
        } // try-with-resources automatically awaits all 10,000 tasks!

        Duration elapsed = Duration.between(start, Instant.now());
        System.out.printf("Completed 10,000 concurrent LLM calls in: %d ms!%n", elapsed.toMillis());
    }
}
```

**Result**: 10,000 concurrent tasks finish in $\approx 1.4$ seconds on a regular laptop with negligible RAM usage.

---

### 3.5 Asynchronous Composition with `CompletableFuture`

In AI systems, you frequently need to coordinate multiple models:
- Query **OpenAI** AND **Claude** concurrently.
- Merge the outputs when both complete (Fan-Out / Fan-In).

```java
package com.javagenai.day07;

import java.util.concurrent.CompletableFuture;

public class AsyncModelEnsemble {

    public static CompletableFuture<String> callOpenAI(String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            simulateLatency(500);
            return "[OpenAI Output: 42]";
        });
    }

    public static CompletableFuture<String> callClaude(String prompt) {
        return CompletableFuture.supplyAsync(() -> {
            simulateLatency(800);
            return "[Claude Output: 42]";
        });
    }

    public static void main(String[] args) {
        String prompt = "Find optimal batch size";

        // Fan-Out: Launch both models in parallel
        CompletableFuture<String> openAi = callOpenAI(prompt);
        CompletableFuture<String> claude = callClaude(prompt);

        // Fan-In: Combine results when both complete
        CompletableFuture<String> combined = openAi.thenCombine(
            claude,
            (res1, res2) -> "Consensus Verified: " + res1 + " == " + res2
        );

        System.out.println(combined.join());
    }

    private static void simulateLatency(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
```

---

### 3.6 Thread Safety & Atomic Operations (`AtomicLong`)

When thousands of concurrent requests increment a shared token counter, `count++` creates race conditions because `++` is not atomic (read $\rightarrow$ modify $\rightarrow$ write).

Java provides lock-free atomic variables backed by CPU-level **Compare-And-Swap (CAS)** machine instructions:

```java
package com.javagenai.day07;

import java.util.concurrent.atomic.AtomicLong;

public class AtomicTokenBudget {
    private final AtomicLong tokensConsumed = new AtomicLong(0);
    private final long maxBudget;

    public AtomicTokenBudget(long maxBudget) {
        this.maxBudget = maxBudget;
    }

    public boolean tryConsume(long tokens) {
        while (true) {
            long current = tokensConsumed.get();
            if (current + tokens > maxBudget) {
                return false; // Budget exhausted!
            }
            // Atomically update only if another thread hasn't modified it in between
            if (tokensConsumed.compareAndSet(current, current + tokens)) {
                return true;
            }
        }
    }

    public long getTokensConsumed() {
        return tokensConsumed.get();
    }
}
```

---

### 3.7 Python GIL vs. Java Virtual Threads: The Production Truth

| Feature | Python (FastAPI / asyncio) | Java 21 (Spring Boot + Virtual Threads) |
| :--- | :--- | :--- |
| **Concurrency Engine** | Single-threaded Event Loop + GIL | Multi-Core Virtual Threads (No GIL) |
| **Code Structure** | `async def` / `await` syntax coloring | **Plain synchronous code** that scales automatically |
| **Debugging** | Complex fragmented stack traces | Unified, standard Java stack traces |
| **Multi-Core Usage** | Bound to 1 core without multi-process | Automatically spreads across all 16–128 CPU cores |
| **Throughput** | Degrades after ~5,000 connections | **100,000+ connections per single JVM instance** |

---

# 4. Prerequisite & Supporting Concepts

### Prerequisite / Supporting Concept: OS Processes vs. Threads

- **Process**: An isolated running program with its own dedicated memory space (e.g., the JVM process).
- **Thread**: A lightweight path of execution running inside a process, sharing heap memory with sibling threads.

---

### Prerequisite / Supporting Concept: Race Conditions and Critical Sections

A **critical section** is a block of code accessing shared mutable state. If multiple threads enter this section simultaneously without synchronization, a **race condition** occurs, producing corrupted data.

---

### Prerequisite / Supporting Concept: The Java Memory Model & Volatile Keyword

The **Java Memory Model (JMM)** governs how CPU caches synchronize with main RAM. Marking a variable `volatile` guarantees that reads and writes bypass local CPU registers and flush directly to main memory, ensuring cross-thread visibility.

---

# 5. Advanced Depth (Intermediate → Advanced)

### 5.1 Senior Deep Dive: `CompletableFuture` Composition (`thenApply`, `thenCompose`, `thenCombine`)

| Method | Role | Analogy | Return Type |
| :--- | :--- | :--- | :--- |
| **`thenApply(Function)`** | Transforms result synchronously. | Like Stream `map()` | `CompletableFuture<U>` |
| **`thenCompose(Function)`**| Chains dependent async task. | Like Stream `flatMap()` | `CompletableFuture<U>` (avoids nesting) |
| **`thenCombine(BiFunction)`**| Combines two independent futures.| Fork-Join parallel merge | `CompletableFuture<V>` |

---

### 5.2 Thread Starvation: The Default `ForkJoinPool.commonPool()` Hazard

By default, `CompletableFuture.supplyAsync(supplier)` executes on the shared `ForkJoinPool.commonPool()`, which has a thread pool size equal to `CPU Cores - 1` (e.g., 7 threads on an 8-core machine).
- **Hazard**: Executing blocking LLM network requests on the common pool blocks all 7 threads, starving the entire JVM and freezing background tasks!
- **Best Practice**: Always supply a custom executor or a virtual thread executor:
  ```java
  CompletableFuture.supplyAsync(supplier, Executors.newVirtualThreadPerTaskExecutor());
  ```

---

### 5.3 Virtual Thread Pinning: `synchronized` vs. `ReentrantLock`

When a virtual thread executes inside a `synchronized` block or method, the JVM **pins** the virtual thread to its OS carrier thread:
- **The Issue**: If the virtual thread makes a blocking network call while pinned, the carrier thread is blocked too, defeating virtual thread scalability!
- **The Fix**: In modern high-concurrency Java, replace `synchronized` with **`java.util.concurrent.locks.ReentrantLock`**, which allows virtual threads to unmount freely during locks.

---

### 5.4 Common Mistakes & Misconceptions (With Bad vs. Good Code)

#### Mistake 1: Pooling Virtual Threads
**Bad Code:**
```java
// ❌ ANTI-PATTERN: Never pool virtual threads!
ExecutorService pool = Executors.newFixedThreadPool(100, Thread.ofVirtual().factory());
```
**Why it's wrong**: Virtual threads cost only ~250 bytes. Pooling them adds memory management overhead.
**Correct Code:**
```java
// ✅ Create a new virtual thread per task!
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
```

#### Mistake 2: Non-Atomic Increments in Multi-User Handlers
**Bad Code:**
```java
private long totalTokens = 0;
public void record(long tokens) { totalTokens += tokens; } // ❌ Race condition!
```
**Correct Code:**
```java
private final AtomicLong totalTokens = new AtomicLong(0);
public void record(long tokens) { totalTokens.addAndGet(tokens); } // ✅ Lock-free atomic
```

---

### 5.5 Architectural Trade-Offs: Virtual Threads vs. Reactive WebFlux

| Dimension | Spring WebFlux (Reactive) | Virtual Threads (Java 21) |
| :--- | :--- | :--- |
| **Programming Model** | Complex reactive streams (`Mono`, `Flux`) | **Plain imperative, blocking Java code** |
| **Stack Traces** | Fragmented, hard to debug | Clean, linear, native stack traces |
| **Learning Curve** | Very High | Zero (standard Java code) |
| **Throughput** | High | **Equally High (100k+ concurrent requests)** |
| **Verdict** | Legacy alternative | **The modern industry standard baseline** |

---

# 6. Quick Recap

| Concept | Key Property |
| :--- | :--- |
| **Virtual Threads** | Lightweight user-space threads managed by JVM; unmount during blocking I/O. |
| **Carrier Threads** | OS threads backing virtual threads (sized to available CPU cores). |
| **`newVirtualThreadPerTaskExecutor`** | Standard executor for running concurrent I/O tasks. |
| **`CompletableFuture`** | Orchestrates asynchronous pipelines and multi-model fan-out/fan-in queries. |
| **`AtomicLong` / `AtomicInteger`** | Lock-free thread-safe counters using CPU Compare-And-Swap (CAS). |
| **Thread Pinning** | Avoid `synchronized` around blocking calls; use `ReentrantLock` instead. |

---

# 7. Self-Check Questions & Practice Exercises

### Self-Check Questions (Basic to Advanced)

1. **Why do Virtual Threads prevent memory exhaustion during thousands of concurrent LLM calls?**
   - *Answer*: Traditional platform threads allocate 1MB–2MB of fixed OS stack memory each. Virtual threads start at only ~250 bytes of heap memory, enabling millions to coexist without exhausting RAM.
2. **What occurs when a Virtual Thread executes a blocking socket read?**
   - *Answer*: The JVM unmounts the virtual thread from its OS carrier thread, parking its state on the heap, freeing the carrier thread to immediately execute other virtual threads.
3. **What is the difference between `CompletableFuture.allOf()` and `CompletableFuture.anyOf()`?**
   - *Answer*: `allOf()` waits for all specified futures to complete. `anyOf()` returns as soon as the first future finishes, returning its result.
4. **Why is `counter++` unsafe in multi-threaded code?**
   - *Answer*: It is not an atomic operation; it performs three distinct machine steps (read, modify, write). Multiple threads reading simultaneously will overwrite each other's increments.
5. **What is "Virtual Thread Pinning" and how is it resolved?**
   - *Answer*: When a virtual thread executes inside a native method or `synchronized` block, it is pinned to its carrier thread and cannot unmount during blocking I/O. It is resolved by replacing `synchronized` with `ReentrantLock`.

---

### Hands-On Practice Exercises with Full Solutions

#### 🏋️ Exercise 1: Build a Multi-Model Race Controller
**Objective**: Query two simulated LLM endpoints concurrently and return whichever response arrives first using `CompletableFuture.anyOf()`.

```java
package com.javagenai.day07;

import java.util.concurrent.CompletableFuture;

public class ModelRaceController {

    public static CompletableFuture<String> queryModel(String modelName, int delayMs, String response) {
        return CompletableFuture.supplyAsync(() -> {
            try { Thread.sleep(delayMs); } catch (InterruptedException ignored) {}
            return String.format("[%s]: %s (latency: %dms)", modelName, response, delayMs);
        });
    }

    public static String getFastestResponse(String prompt) {
        CompletableFuture<String> fastModel = queryModel("Llama-3.2-1B", 150, "Quick summary");
        CompletableFuture<String> slowModel = queryModel("GPT-4o-Heavy", 600, "In-depth treatise");

        Object winner = CompletableFuture.anyOf(fastModel, slowModel).join();
        return (String) winner;
    }
}
```

---

#### 🏋️ Exercise 2: Virtual Thread Batch Document Embedding Simulator
**Objective**: Simulate embedding 500 documents concurrently with a 200ms network delay each, measuring total time taken with `Executors.newVirtualThreadPerTaskExecutor()`.

```java
package com.javagenai.day07;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;

public class BatchEmbeddingSimulator {

    public static Duration simulateBatchEmbedding(int documentCount, int delayMs) {
        Instant start = Instant.now();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < documentCount; i++) {
                final int id = i;
                executor.submit(() -> {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ignored) {}
                    return "embedding_vector_" + id;
                });
            }
        }

        return Duration.between(start, Instant.now());
    }
}
```

---

<p align="center">
  <b>Day 07 Complete! 🎉</b><br>
  Proceed to <b>Day 08</b>: <b>I/O, Modern HTTP Client, JSON & Testing (JUnit 5 + Mockito)</b>.<br>
  <a href="../Day_08_IO_HTTP_JSON_Testing/Day_08_IO_HTTP_JSON_Testing.md"><b>Continue to Day 08 →</b></a>
</p>
