# 🧪 Day 08: I/O, HTTP Client, JSON & Testing
## The Production Toolkit — Calling Real LLM APIs and Testing with JUnit 5 & Mockito

| ⬅️ Previous Day | 📚 Course Hub | ➡️ Next Day |
|:---|:---:|---:|
| [← Day 07: Concurrency & Virtual Threads](../Day_07_Concurrency_Virtual_Threads/Day_07_Concurrency_Virtual_Threads.md) | [All 60 Days Overview](../../README.md) | [Day 09: The Problem Spring Solves — Dependency Hell →](../../Phase_02_Spring_Core_and_DI/Day_09_Problem_Spring_Solves_Dependency_Hell/Day_09_Problem_Spring_Solves_Dependency_Hell.md) |

[![Phase](https://img.shields.io/badge/Phase_01-Java_Foundations-brightgreen.svg?style=for-the-badge)](../../README.md)
[![Day](https://img.shields.io/badge/Day-08_of_60-blue.svg?style=for-the-badge)](../../README.md)
[![Difficulty](https://img.shields.io/badge/Difficulty-Intermediate-blue.svg?style=for-the-badge)](../../README.md)
[![Milestone](https://img.shields.io/badge/Milestone-Phase_1_Graduation!-brightgreen.svg?style=for-the-badge)](../../README.md)

---

## 🗺️ Table of Contents
- [1. Topic Overview](#1-topic-overview)
- [2. Basic Foundations (True Zero)](#2-basic-foundations-true-zero)
  - [2.1 What is File I/O, an HTTP Client, JSON, and Unit Testing?](#21-what-is-file-io-an-http-client-json-and-unit-testing)
  - [2.2 The Crash-Test Dummy Analogy for Mocking](#22-the-crash-test-dummy-analogy-for-mocking)
  - [2.3 Minimal Working Example: Safe File Reading and Validation](#23-minimal-working-example-safe-file-reading-and-validation)
  - [2.4 Line-by-Line Code Breakdown](#24-line-by-line-code-breakdown)
- [3. Core Concept Walkthrough (Basic → Intermediate)](#3-core-concept-walkthrough-basic--intermediate)
  - [3.1 Modern Java NIO.2 & `try-with-resources`](#31-modern-java-nio2--try-with-resources)
  - [3.2 The Modern Built-in `HttpClient` (`java.net.http`)](#32-the-modern-built-in-httpclient-javanethttp)
  - [3.3 Calling a Local Ollama LLM with Zero Dependencies](#33-calling-a-local-ollama-llm-with-zero-dependencies)
  - [3.4 JSON Serialization & Deserialization with Jackson](#34-json-serialization--deserialization-with-jackson)
  - [3.5 Automated Testing with JUnit 5](#35-automated-testing-with-junit-5)
  - [3.6 Mocking LLM API Calls with Mockito](#36-mocking-llm-api-calls-with-mockito)
  - [3.7 Phase 1 Graduation Capstone](#37-phase-1-graduation-capstone)
- [4. Prerequisite & Supporting Concepts](#4-prerequisite--supporting-concepts)
  - [Prerequisite / Supporting Concept: Operating System File Descriptors & Resource Leaks](#prerequisite--supporting-concept-operating-system-file-descriptors--resource-leaks)
  - [Prerequisite / Supporting Concept: The HTTP Protocol (Methods, Status Codes, Headers)](#prerequisite--supporting-concept-the-http-protocol-methods-status-codes-headers)
  - [Prerequisite / Supporting Concept: Test-Driven Development (TDD) & The Test Pyramid](#prerequisite--supporting-concept-test-driven-development-tdd--the-test-pyramid)
- [5. Advanced Depth (Intermediate → Advanced)](#5-advanced-depth-intermediate--advanced)
  - [5.1 Senior Deep Dive: `HttpClient.sendAsync()` with Virtual Threads](#51-senior-deep-dive-httpclientsendasync-with-virtual-threads)
  - [5.2 Advanced Mockito: Argument Matchers & Verification](#52-advanced-mockito-argument-matchers--verification)
  - [5.3 Common Mistakes & Misconceptions (With Bad vs. Good Code)](#53-common-mistakes--misconceptions-with-bad-vs-good-code)
  - [5.4 Architectural Trade-Offs: Real API Integration Tests vs. Mocked Unit Tests](#54-architectural-trade-offs-real-api-integration-tests-vs-mocked-unit-tests)
- [6. Quick Recap](#6-quick-recap)
- [7. Self-Check Questions & Practice Exercises](#7-self-check-questions--practice-exercises)
  - [Self-Check Questions (Basic to Advanced)](#self-check-questions-basic-to-advanced)
  - [Hands-On Practice Exercises with Full Solutions](#hands-on-practice-exercises-with-full-solutions)

---

# 1. Topic Overview

Modern enterprise Java provides standard, production-ready toolkits for **File I/O (NIO.2)**, **HTTP Client communication (`java.net.http`)**, **JSON processing (Jackson)**, and **automated testing (JUnit 5 & Mockito)**. Together, these tools form the bridge between in-memory Java code and external networks, file systems, and AI cloud services.

### Why This Topic Matters
Real-world Generative AI applications do not operate in a vacuum. They read system prompts and PDF documents from disk, dispatch HTTP POST requests to OpenAI, Anthropic, or Ollama endpoints, parse returned JSON strings into immutable Java records, and validate business logic using automated unit tests. Without mocking tools like Mockito, testing AI applications would incur exorbitant cloud API bills and produce flaky CI/CD pipelines.

> 💡 **New Word Alert — "HTTP Client"**: A software component that acts as a headless web browser, sending HTTP requests (GET, POST) over network sockets to external APIs and receiving responses.

> 💡 **New Word Alert — "Serialization / Deserialization"**:
> - *Serialization*: Converting an in-memory Java object/record into a JSON string to transmit over the network.
> - *Deserialization*: Parsing an incoming JSON string into an in-memory Java object/record.

> 💡 **New Word Alert — "Mocking (Mockito)"**: Creating a simulated "stunt double" of an external dependency (like an LLM API) during automated testing so tests execute in milliseconds at zero monetary cost.

---

# 2. Basic Foundations (True Zero)

Let's begin with absolute basics, assuming no prior networking or testing background.

### 2.1 What is File I/O, an HTTP Client, JSON, and Unit Testing?

- **File I/O (Input/Output)**: Reading text or binary data from your computer's hard drive into RAM, or saving data from RAM back to disk.
- **HTTP Client**: A built-in Java tool that connects to web addresses (like `https://api.openai.com/v1/chat/completions`) to send and receive text payloads.
- **JSON (JavaScript Object Notation)**: A lightweight, universal plain-text data interchange format that computers across different programming languages understand (e.g., `{"model": "gpt-4o", "temperature": 0.7}`).
- **Unit Testing**: Writing small, automated code snippets that verify individual methods in your application to catch bugs before deploying to production.

---

### 2.2 The Crash-Test Dummy Analogy for Mocking

```
                     REAL LUXURY CAR CRASH (Calling Live OpenAI in Tests)
┌─────────────────────────────────────────────────────────────────────────────┐
│ You buy a brand new $100,000 car and smash it into a concrete wall to test  │
│ whether the airbag sensor triggers. If you run 500 tests a day, you go      │
│ bankrupt and wait hours for replacement cars to arrive!                     │
└─────────────────────────────────────────────────────────────────────────────┘
                                       vs.
                     CRASH-TEST SIMULATOR (Mockito Stunt-Double)
┌─────────────────────────────────────────────────────────────────────────────┐
│ You build a lightweight simulator that replicates the exact electrical wire │
│ signal of a crash. The airbag tests run in 5 milliseconds, cost $0.00, and  │
│ never destroy a physical car!                                               │
└─────────────────────────────────────────────────────────────────────────────┘
```

When testing an AI service, you **never** call OpenAI or Claude directly. You instruct Mockito: *"When someone calls `chatModel.call(...)`, immediately return `'Mocked Response'` without touching the internet."*

---

### 2.3 Minimal Working Example: Safe File Reading and Validation

Let's write a minimal, fully runnable program that reads a prompt template from disk using modern Java NIO.2:

```java
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SimpleFileIODemo {

    public static void main(String[] args) {
        Path tempFile = null;
        try {
            // 1. Create a temporary prompt file
            tempFile = Files.createTempFile("system-prompt", ".txt");
            Files.writeString(tempFile, "You are a helpful AI assistant.");

            // 2. Read the file into a String in one line
            String content = Files.readString(tempFile);
            System.out.println("Prompt Loaded: " + content);

        } catch (IOException e) {
            System.err.println("File operation failed: " + e.getMessage());
        } finally {
            // 3. Clean up temporary file
            if (tempFile != null) {
                try { Files.deleteIfExists(tempFile); } catch (IOException ignored) {}
            }
        }
    }
}
```

---

### 2.4 Line-by-Line Code Breakdown

1. `Files.createTempFile(...)`: Safely creates a unique temporary file on disk managed by the OS.
2. `Files.writeString(...)`: Modern Java 11 method that writes a string to disk using UTF-8 encoding.
3. `Files.readString(...)`: Reads the entire file into memory as a `String` in a single line, eliminating ancient boilerplate loops.
4. `try-catch`: Catches potential `IOException` events (such as missing files or permission errors).

---

# 3. Core Concept Walkthrough (Basic → Intermediate)

Now let's examine the production components used to connect to real LLM backends.

### 3.1 Modern Java NIO.2 & `try-with-resources`

In operating systems, open file streams are tracked as **file descriptors**. If an application opens files without closing them, the server runs out of descriptors and crashes.

Modern Java uses **`try-with-resources`**: Any resource implementing `AutoCloseable` is guaranteed to close automatically upon block exit:

```java
package com.javagenai.day08;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SafeFileReader {

    public static void printLines(Path filePath) throws IOException {
        // Guaranteed automatic closure of reader upon exiting block
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }
    }
}
```

---

### 3.2 The Modern Built-in `HttpClient` (`java.net.http`)

Introduced in Java 11, `java.net.http.HttpClient` provides a native, asynchronous, HTTP/2-enabled client with zero third-party dependencies.

```
┌────────────────────────┐      ┌────────────────────────┐      ┌────────────────────────┐
│      HttpClient        │ ──►  │      HttpRequest       │ ──►  │      HttpResponse      │
│  - HTTP/2 support      │      │  - URI: /api/generate  │      │  - Status Code: 200    │
│  - Connection pooling  │      │  - Method: POST        │      │  - Headers             │
│  - Timeout configs     │      │  - Body: JSON payload  │      │  - Body: Response text │
└────────────────────────┘      └────────────────────────┘      └────────────────────────┘
```

---

### 3.3 Calling a Local Ollama LLM with Zero Dependencies

Here is a complete, working client that makes an actual REST API call to Ollama (`http://localhost:11434/api/generate`):

```java
package com.javagenai.day08;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class RawOllamaClient {
    private final HttpClient httpClient;
    private final String baseUrl;

    public RawOllamaClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    public String generate(String model, String prompt) throws IOException, InterruptedException {
        String jsonBody = String.format("""
            {
              "model": "%s",
              "prompt": "%s",
              "stream": false
            }
            """, model, prompt.replace("\"", "\\\""));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/generate"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .timeout(Duration.ofSeconds(30))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Ollama API failed with HTTP status: " + response.statusCode());
        }

        return response.body();
    }
}
```

---

### 3.4 JSON Serialization & Deserialization with Jackson

In enterprise applications, we use **Jackson (`ObjectMapper`)** to map JSON payloads directly into Java Records:

```java
package com.javagenai.day08;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonMappingDemo {

    public record ChatRequest(String model, String prompt, boolean stream) {}

    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // 1. Serialize: Java Record -> JSON String
        ChatRequest req = new ChatRequest("llama-3.2", "Explain Java", false);
        String jsonText = mapper.writeValueAsString(req);
        System.out.println("JSON Output: " + jsonText);

        // 2. Deserialize: JSON String -> Java Record
        ChatRequest parsed = mapper.readValue(jsonText, ChatRequest.class);
        System.out.println("Parsed Model: " + parsed.model());
    }
}
```

---

### 3.5 Automated Testing with JUnit 5

Automated tests prevent regressions when developers modify prompt templates or model settings:

```java
package com.javagenai.day08;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AI Prompt Validator Suite")
class PromptValidatorTest {

    @Test
    @DisplayName("Should accept valid prompt with content")
    void testValidPrompt() {
        String prompt = "Summarize the quarterly financial report.";
        assertTrue(prompt.length() > 10, "Prompt must exceed minimum length threshold");
    }

    @Test
    @DisplayName("Should throw exception when prompt is blank")
    void testBlankPromptThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            validatePrompt("   ");
        });
    }

    private void validatePrompt(String p) {
        if (p == null || p.isBlank()) {
            throw new IllegalArgumentException("Prompt cannot be empty");
        }
    }
}
```

---

### 3.6 Mocking LLM API Calls with Mockito

Mockito allows you to test business logic that depends on an LLM without making real network requests or spending money on tokens:

```java
package com.javagenai.day08;

// The business service under test
public class AISummarizerService {
    private final ChatModel chatModel;

    public AISummarizerService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String summarize(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw new IllegalArgumentException("Cannot summarize empty text");
        }
        String prompt = "Summarize in 1 sentence: " + rawText;
        return this.chatModel.call(prompt);
    }
}
```

```java
package com.javagenai.day08;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AISummarizerServiceTest {

    @Mock
    private ChatModel mockChatModel; // Stunt double! Zero network calls

    @InjectMocks
    private AISummarizerService summarizerService;

    @Test
    void testSuccessfulSummarization() {
        // Arrange
        String text = "Java 21 introduced virtual threads for high concurrency.";
        String expectedPrompt = "Summarize in 1 sentence: " + text;
        when(mockChatModel.call(expectedPrompt)).thenReturn("Virtual threads simplify concurrency.");

        // Act
        String result = summarizerService.summarize(text);

        // Assert
        assertEquals("Virtual threads simplify concurrency.", result);
        verify(mockChatModel, times(1)).call(expectedPrompt);
    }
}
```

---

### 3.7 Phase 1 Graduation Capstone

```
┌────────────────────────────────────────────────────────────────────────┐
│             CONGRATULATIONS: PHASE 1 COMPLETED! 🎓                     │
├────────────────────────────────────────────────────────────────────────┤
│ Day 01: Java Ecosystem, JDK 21 LTS, Bytecode & Maven                   │
│ Day 02: OOP, Classes, Objects, Stack vs Heap & equals/hashCode         │
│ Day 03: Inheritance, Interfaces, Polymorphism & Sealed Types           │
│ Day 04: Generics (<T>), Collections (List, Map, Set, PriorityQueue)   │
│ Day 05: Modern Java (Records, Optional<T>, Text Blocks, Switch)       │
│ Day 06: Functional Programming, Lambdas & Parallel Stream Pipelines    │
│ Day 07: Concurrency, Virtual Threads (Project Loom) & CompletableFuture│
│ Day 08: Modern I/O, HttpClient, JSON & Testing (JUnit 5 + Mockito)     │
└────────────────────────────────────────────────────────────────────────┘
```

You now possess the foundational mastery of a modern Java engineer. You understand memory layout, interface contracts, stream processing, virtual threads, and automated testing.

---

# 4. Prerequisite & Supporting Concepts

### Prerequisite / Supporting Concept: Operating System File Descriptors & Resource Leaks

Every time a program opens a file or network socket, the operating system assigns an integer identifier called a **file descriptor**. Operating systems set hard limits on maximum concurrent open descriptors (typically 1,024 to 65,535). Failing to close streams exhausts descriptors, causing subsequent socket or file operations to fail with `Too many open files`.

---

### Prerequisite / Supporting Concept: The HTTP Protocol (Methods, Status Codes, Headers)

- **Methods**: `GET` (retrieve data), `POST` (submit data/prompts), `DELETE` (remove resource).
- **Status Codes**: `200 OK` (success), `400 Bad Request` (client validation error), `401 Unauthorized` (bad API key), `500 Internal Server Error` (backend failure).
- **Headers**: Key-value metadata accompanying the request (e.g., `Content-Type: application/json`, `Authorization: Bearer sk-...`).

---

### Prerequisite / Supporting Concept: Test-Driven Development (TDD) & The Test Pyramid

- **Unit Tests (Base)**: Fast, isolated tests executing in milliseconds with mocked dependencies.
- **Integration Tests (Middle)**: Verify real database or network interactions.
- **E2E Tests (Peak)**: Full system simulation across all microservices.

---

# 5. Advanced Depth (Intermediate → Advanced)

### 5.1 Senior Deep Dive: `HttpClient.sendAsync()` with Virtual Threads

The modern `HttpClient` supports both synchronous blocking calls (`send`) and non-blocking asynchronous calls (`sendAsync`):

```java
CompletableFuture<HttpResponse<String>> future = httpClient.sendAsync(
    request, 
    HttpResponse.BodyHandlers.ofString()
);

future.thenApply(HttpResponse::body)
      .thenAccept(System.out::println);
```

When combined with Java 21 **Virtual Threads**, calling synchronous `httpClient.send(...)` is already non-blocking at the operating system level, eliminating the need to write complex asynchronous callback pipelines!

---

### 5.2 Advanced Mockito: Argument Matchers & Verification

- **`any()` / `anyString()`**: Matches any string parameter.
- **`eq(value)`**: Matches exact value.
- **`verify(mock, times(n))`**: Asserts that a method was called exactly $n$ times.
- **`verifyNoMoreInteractions(mock)`**: Guarantees no unexpected background calls occurred.

```java
when(mockChatModel.call(anyString())).thenReturn("Default mock answer");
verify(mockChatModel, never()).call("forbidden prompt");
```

---

### 5.3 Common Mistakes & Misconceptions (With Bad vs. Good Code)

#### Mistake 1: Forgetting `try-with-resources`
**Bad Code:**
```java
// ❌ If an exception is thrown, stream is NEVER closed!
FileInputStream fis = new FileInputStream("prompt.txt");
byte[] data = fis.readAllBytes();
fis.close();
```
**Correct Code:**
```java
// ✅ Guaranteed closure under all circumstances
try (InputStream fis = Files.newInputStream(Path.of("prompt.txt"))) {
    byte[] data = fis.readAllBytes();
}
```

#### Mistake 2: Hardcoding Live API Calls in Unit Tests
**Bad Practice:** Calling real OpenAI endpoints inside `@Test` methods. Tests fail when offline, run slowly, and consume real money on every commit.
**Correct Practice:** Use Mockito `@Mock` to stub AI responses deterministically.

---

### 5.4 Architectural Trade-Offs: Real API Integration Tests vs. Mocked Unit Tests

| Dimension | Mocked Unit Tests (Mockito) | Real Live API Integration Tests |
| :--- | :--- | :--- |
| **Execution Speed** | < 10 milliseconds | 2,000 to 10,000 milliseconds |
| **Monetary Cost** | **$0.00** | Costs tokens per test run |
| **Determinism** | 100% predictable | Non-deterministic output |
| **Network Dependency**| 100% Offline | Requires active internet & API keys |
| **Purpose** | Validate business logic, edge cases | Validate API connectivity & authentication |

---

# 6. Quick Recap

| Component | Library | Primary Function |
| :--- | :--- | :--- |
| **`Files.readString(path)`** | `java.nio.file` | High-level, single-line UTF-8 file reading. |
| **`try-with-resources`** | Core Language | Guaranteed closure of `AutoCloseable` streams. |
| **`HttpClient`** | `java.net.http` | Modern HTTP/2 zero-dependency web client. |
| **`ObjectMapper`** | Jackson | Serializes/Deserializes JSON to/from Java records. |
| **JUnit 5** | `org.junit.jupiter` | Standard automated test runner (`@Test`, asserts). |
| **Mockito** | `org.mockito` | Mocks external AI models for fast, zero-cost tests. |

---

# 7. Self-Check Questions & Practice Exercises

### Self-Check Questions (Basic to Advanced)

1. **Why is `try-with-resources` superior to manual `close()` calls in a `finally` block?**
   - *Answer*: `try-with-resources` automatically guarantees that all `AutoCloseable` resources are closed even when exceptions are thrown, eliminating resource leaks and preventing suppressed exception issues.
2. **What are the three core classes of the Java 11+ HTTP client?**
   - *Answer*: `HttpClient` (engine configuration and connection manager), `HttpRequest` (URL, method, headers, and request body), and `HttpResponse` (status code, response headers, and body).
3. **Why should you mock LLM calls in automated unit tests?**
   - *Answer*: Real LLM API calls incur token billing costs, require active internet connections, exhibit network latency, and return non-deterministic text that causes test flakiness.
4. **What does Mockito's `when(...).thenReturn(...)` construct do?**
   - *Answer*: It stubs a method on a mock object, dictating the exact canned value that should be returned when the method is invoked with specified arguments.
5. **What major milestone did you just achieve?**
   - *Answer*: Graduation from Phase 1! You have mastered core Java 21, memory management, OOP, polymorphism, collections, records, streams, virtual threads, and automated testing.

---

### Hands-On Practice Exercises with Full Solutions

#### 🏋️ Exercise 1: Build a Pure Java HTTP GET Health Checker
**Objective**: Use `java.net.http.HttpClient` to check if a local service (such as Ollama on port 11434) is reachable, returning `true` on HTTP 200 and `false` on failure.

```java
package com.javagenai.day08;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;

public class ServiceHealthChecker {

    public static boolean isServiceAlive(String url) {
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .timeout(Duration.ofSeconds(2))
            .build();

        try {
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
```

---

#### 🏋️ Exercise 2: Safe File Reader with Word Count Metric
**Objective**: Write a method that reads a prompt template from disk, counts total words, and returns an `Optional<String>` containing the prompt if it has at least 5 words.

```java
package com.javagenai.day08;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class PromptFileReader {

    public static Optional<String> readValidPrompt(Path path) {
        try {
            if (!Files.exists(path)) return Optional.empty();
            String content = Files.readString(path).trim();
            String[] words = content.split("\\s+");
            if (words.length >= 5) {
                return Optional.of(content);
            }
            return Optional.empty();
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
```

---

<p align="center">
  <b>🎉 Congratulations on Graduating Phase 1: Java Foundations! 🎉</b><br>
  You have mastered Core Java 21, OOP, memory, polymorphism, collections, records, streams, virtual threads, and testing.<br>
  Proceed to <b>Phase 2: Spring Core & Dependency Injection</b> starting with <b>Day 09: The Problem Spring Solves — Dependency Hell</b>.<br>
  <a href="../../Phase_02_Spring_Core_and_DI/Day_09_Problem_Spring_Solves_Dependency_Hell/Day_09_Problem_Spring_Solves_Dependency_Hell.md"><b>Continue to Day 09 →</b></a>
</p>
