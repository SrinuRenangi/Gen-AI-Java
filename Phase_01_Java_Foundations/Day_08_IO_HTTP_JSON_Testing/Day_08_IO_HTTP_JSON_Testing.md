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

## 📌 What Will You Learn Today?

Hey there, my friend! Huge congratulations on reaching **Day 08** — the capstone and graduation day of **Phase 1: Java Foundations**! 🎉

Look how far you've come: from installing the JDK and understanding memory on Day 01, through OOP, polymorphism, data structures, records, streams, and virtual threads. Today, we tie all of these pieces together into the practical, physical skills needed to talk to real AI systems:

1. **How to read and write files** (text files, prompt templates, RAG documents) safely without leaking memory.
2. **How to call real AI APIs** over the internet using Java's built-in `HttpClient` (no external libraries needed!).
3. **How to convert JSON** to and from Java Records (Serialization & Deserialization).
4. **How to write automated tests** with **JUnit 5** and **Mockito** so you can test your AI apps **without spending a single penny on AI API bills**!

By the end of today, you will clearly understand:
- ✅ **Modern Java NIO**: `Path`, `Files.readString()`, and `try-with-resources` for safe file handling.
- ✅ **Built-in `HttpClient`**: Java's modern, zero-dependency tool for making web requests.
- ✅ **Calling Live AI Models**: Sending raw HTTP POST requests to Ollama or OpenAI with JSON payloads.
- ✅ **JSON in Java**: Converting Java records into JSON text and converting AI responses back into Java records.
- ✅ **JUnit 5 Testing**: Writing automated tests with `@Test`, `@BeforeEach`, and assertions.
- ✅ **Mockito Stunt-Doubles**: Mocking external AI calls so your tests run in 5 milliseconds for $0.00.
- ✅ **Phase 1 Capstone Project**: A fully tested, working AI summarization service!

---

## 🗺️ Table of Contents

- [1. Modern File I/O with Java NIO.2](#1-modern-file-io-with-java-nio2)
  - [1.1 `try-with-resources`: Never Leak a File Descriptor](#11-try-with-resources-never-leak-a-file-descriptor)
  - [1.2 Reading and Writing Prompts and RAG Documents](#12-reading-and-writing-prompts-and-rag-documents)
- [2. The Modern Java HTTP Client (`java.net.http`)](#2-the-modern-java-http-client-javanethttp)
  - [2.1 Why Not Old `HttpURLConnection`?](#21-why-not-old-httpurlconnection)
  - [2.2 Anatomy of a Request: `HttpClient`, `HttpRequest`, `HttpResponse`](#22-anatomy-of-a-request-httpclient-httprequest-httpresponse)
  - [2.3 Calling a Local Ollama LLM with Zero Dependencies](#23-calling-a-local-ollama-llm-with-zero-dependencies)
- [3. JSON Handling in Enterprise Java](#3-json-handling-in-enterprise-java)
- [4. Automated Testing with JUnit 5](#4-automated-testing-with-junit-5)
  - [4.1 Why Tests Are Mandatory for AI Systems](#41-why-tests-are-mandatory-for-ai-systems)
  - [4.2 Anatomy of a JUnit 5 Test Class](#42-anatomy-of-a-junit-5-test-class)
  - [4.3 Assertions & Testing Exceptions](#43-assertions--testing-exceptions)
- [5. Mocking LLMs with Mockito](#5-mocking-llms-with-mockito)
  - [5.1 Real-World Analogy: The Crash-Test Dummy](#51-real-world-analogy-the-crash-test-dummy)
  - [5.2 Mocking OpenAI API Calls to Save Money](#52-mocking-openai-api-calls-to-save-money)
- [6. Phase 1 Graduation Capstone](#6-phase-1-graduation-capstone)
- [7. Key Takeaways & Summary](#7-key-takeaways--summary)
- [8. Practice Exercises & Full Solutions](#8-practice-exercises--full-solutions)
- [9. Self-Check Quiz](#9-self-check-quiz)

---

# 1. Modern File I/O with Java NIO.2

> [!TIP]
> ### 💡 New Word Alert: Networking & Testing Terms
> - **HTTP Client**: Java's built-in web browser engine without a screen. It lets your Java code send messages across the web to APIs (like asking OpenAI a question) and receive responses.
> - **JSON**: Short for JavaScript Object Notation — the universal data format of the internet. It looks like `{"name": "Alice", "role": "user"}`.
> - **Serialization vs Deserialization**:
>   - *Serialization*: Turning a Java Record into a JSON text string to send over the internet.
>   - *Deserialization*: Taking the JSON text string sent back by an AI model and turning it into a real Java Record in memory.
> - **Mocking (Mockito)**: Creating a fake "stunt double" of an external service. In testing, instead of calling OpenAI and waiting 3 seconds and paying 2 cents, your mock instantly returns a canned answer for free!

---

## 🧭 The Plain English Bridge: Modern I/O, HttpClient & Testing Demystified

If you learned Java using old books with `FileInputStream` or Apache HttpClient, look how clean modern Java 21 is:

| Tool / Concept | Old Way (Legacy Java) | Modern Java 21 Way | Plain English Advantage |
| :--- | :--- | :--- | :--- |
| **Reading Files** | 10 lines of `new FileInputStream()`, `BufferedReader`, and manual `close()`. | `Files.readString(Path.of("file.txt"))` | Reads an entire file into a `String` in a single line. |
| **HTTP Requests** | Clunky `HttpURLConnection` or external Apache libraries. | `java.net.http.HttpClient` (built-in!) | Modern fluent API supporting HTTP/2, async requests, and WebSockets natively. |
| **JSON Parsing** | Manual string chopping or bulky old libraries. | `ObjectMapper.readValue(json, MyRecord.class)` | Automatically populates immutable Java Records directly from JSON! |
| **Resource Cleanup**| `finally { if (r != null) r.close(); }` | `try (var r = ...)` (try-with-resources) | Guaranteed leak-free cleanup even if an error crashes your method. |
| **Testing AI Models**| Calling real OpenAI API during tests (slow & expensive!). | Mockito: `when(model.call(any())).thenReturn(fakeResp)` | **Never spend money during unit tests!** Tests run in 5 milliseconds offline with 0 cloud cost. |

---

In RAG (Retrieval-Augmented Generation), your application continuously reads knowledge-base documents from disk.

### 1.1 `try-with-resources`: Never Leak a File Descriptor

In operating systems, open file handles are a finite resource. If you open a file and forget to close it (or an exception crashes your method before `.close()`), your server eventually runs out of file descriptors and crashes.

Modern Java provides **`try-with-resources`**: Any object implementing `AutoCloseable` is **guaranteed to be closed automatically**, even if an exception occurs!

```java
// SAFE: Guaranteed automatic cleanup
try (BufferedReader reader = Files.newBufferedReader(Path.of("system-prompt.txt"))) {
    String line;
    while ((line = reader.readLine()) != null) {
        System.out.println(line);
    }
} // reader.close() is executed automatically here!
```

---

### 1.2 Reading and Writing Prompts and RAG Documents

Modern Java 11+ makes reading whole files effortless via `java.nio.file.Files`:

```java
package com.javagenai.day08;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class PromptFileManager {

    public static String loadPromptTemplate(Path filePath) throws IOException {
        // Reads an entire file into a String in UTF-8
        return Files.readString(filePath);
    }

    public static void saveGeneratedChunk(Path outputPath, String content) throws IOException {
        // Writes or appends content to disk cleanly
        Files.writeString(outputPath, content, 
                          StandardOpenOption.CREATE, 
                          StandardOpenOption.TRUNCATE_EXISTING);
    }
}
```

---

# 2. The Modern Java HTTP Client (`java.net.http`)

### 2.1 Why Not Old `HttpURLConnection`?

Ancient Java had `HttpURLConnection`, which was clunky, verbose, and lacked HTTP/2 or streaming support.
In Java 11+, Java introduced **`java.net.http.HttpClient`**—a world-class, modern HTTP client built directly into standard library `java.net.http.*`.

### 2.2 Anatomy of a Request

Every HTTP interaction consists of three standard objects:

```
┌────────────────────────┐      ┌────────────────────────┐      ┌────────────────────────┐
│      HttpClient        │ ──►  │      HttpRequest       │ ──►  │      HttpResponse      │
│  - HTTP/2 support      │      │  - URI: /api/generate  │      │  - Status Code: 200    │
│  - Connection pooling  │      │  - Method: POST        │      │  - Headers             │
│  - Timeout configs     │      │  - Body: JSON payload  │      │  - Body: Response text │
└────────────────────────┘      └────────────────────────┘      └────────────────────────┘
```

---

### 2.3 Calling a Local Ollama LLM with Zero Dependencies

Here is a complete, working client that makes an actual REST API call to Ollama (`http://localhost:11434/api/generate`) with **zero third-party dependencies**:

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
        // 1. Build an HTTP/2 client with a 10-second connection timeout
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    public String generate(String model, String prompt) throws IOException, InterruptedException {
        // Simple JSON payload (in production, Jackson serializes this)
        String jsonBody = String.format("""
            {
              "model": "%s",
              "prompt": "%s",
              "stream": false
            }
            """, model, prompt.replace("\"", "\\\""));

        // 2. Build the POST request
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/generate"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .timeout(Duration.ofSeconds(30))
            .build();

        // 3. Dispatch the request and receive the response body as a String
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Ollama API call failed with status: " + response.statusCode());
        }

        return response.body();
    }
}
```

---

# 3. JSON Handling in Enterprise Java

In production Spring Boot and Spring AI applications, we do not manually concatenate JSON strings. We use **Jackson (`com.fasterxml.jackson.databind.ObjectMapper`)**.

```
Java Object / Record  ──(serialize / writeValueAsString)──►   JSON String
JSON String           ──(deserialize / readValue)─────────►   Java Object / Record
```

```java
// Jackson serialization with Java Records:
record ChatRequest(String model, String prompt, boolean stream) {}

ObjectMapper mapper = new ObjectMapper();

// Object -> JSON
ChatRequest req = new ChatRequest("llama-3.2", "Hello AI", false);
String jsonString = mapper.writeValueAsString(req);
// {"model":"llama-3.2","prompt":"Hello AI","stream":false}

// JSON -> Object
ChatRequest parsed = mapper.readValue(jsonString, ChatRequest.class);
System.out.println(parsed.model()); // "llama-3.2"
```

---

# 4. Automated Testing with JUnit 5

### 4.1 Why Tests Are Mandatory for AI Systems

In traditional software, `2 + 2` is always `4`. 
In Generative AI, prompt outputs are **non-deterministic**. If a junior developer modifies a system prompt or changes temperature from `0.2` to `1.8`, your customer support bot might start hallucinating policies.

Automated unit tests ensure that:
1. Input validation guards work.
2. Token estimations stay within limits.
3. Fallback strategies trigger properly when an API fails.
4. Business logic parsing works across hundreds of edge cases.

---

### 4.2 Anatomy of a JUnit 5 Test Class

```java
package com.javagenai.day08;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AI Prompt Validator Test Suite")
class PromptValidatorTest {

    @BeforeEach
    void setUp() {
        // Runs before every individual @Test method
    }

    @Test
    @DisplayName("Should accept valid prompt within token limits")
    void testValidPrompt() {
        String prompt = "Summarize the quarterly financial report.";
        boolean isValid = prompt.length() > 10;
        assertTrue(isValid, "Prompt should be recognized as valid");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException on blank prompt")
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

# 5. Mocking LLMs with Mockito

### 5.1 Real-World Analogy: The Crash-Test Dummy

Car manufacturers do not crash a \$100,000 production luxury vehicle every time they test an airbag sensor. They use **crash-test dummies** and simulation rigs.

In AI engineering:
- **Calling OpenAI in unit tests costs money.** 
- If your CI/CD pipeline runs 500 tests on every git commit, you would burn \$50 every time someone pushes code!
- Real LLMs can be down or slow, making tests flaky.

**Mockito** creates a "stunt double" (Mock) of your LLM client. You instruct the mock:
> *"When someone calls `chatModel.call("Hello")`, do not touch the network. Immediately return `'Mocked Response'`."*

---

### 5.2 Mocking OpenAI API Calls to Save Money

Let's test an `AISummarizerService` that depends on a `ChatModel`:

```java
package com.javagenai.day08;

// The Service to test
public class AISummarizerService {
    private final ChatModel chatModel;

    public AISummarizerService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String summarize(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw new IllegalArgumentException("Cannot summarize empty text");
        }
        String prompt = "Summarize the following text in 1 sentence: " + rawText;
        return this.chatModel.call(prompt);
    }
}
```

Now let's write the **Mockito Unit Test**:

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
    private ChatModel mockChatModel; // 1. Stunt double! No real network calls.

    @InjectMocks
    private AISummarizerService summarizerService; // 2. Injects the mock into our service

    @Test
    void testSuccessfulSummarization() {
        // Arrange: Train the mock on how to respond
        String text = "Java 21 introduced virtual threads which make high concurrency easy.";
        String expectedPrompt = "Summarize the following text in 1 sentence: " + text;
        when(mockChatModel.call(expectedPrompt)).thenReturn("Virtual threads simplify concurrency.");

        // Act: Run our business service
        String result = summarizerService.summarize(text);

        // Assert: Verify the result
        assertEquals("Virtual threads simplify concurrency.", result);

        // Verify: Ensure the mock was called exactly ONCE with the right prompt!
        verify(mockChatModel, times(1)).call(expectedPrompt);
    }
}
```

> **Zero network calls. Zero dollars spent. Executes in 5 milliseconds.**

---

# 6. Phase 1 Graduation Capstone

Let's review the incredible foundation you built across the first 8 days:

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

You now possess the foundational fluency of a strong Java engineer. You understand how memory works, how to design swappable architectures with interfaces, how to process millions of records with Streams, and how to scale to 100,000 concurrent requests with Virtual Threads.

**You are now fully primed for Phase 2: Spring Core & Dependency Injection (Days 09–14)!**

---

# 7. Key Takeaways & Summary

```
                  ┌─────────────────────────────────┐
                  │       DAY 08 CHEAT SHEET        │
                  └────────────────┬────────────────┘
                                   │
         ┌─────────────────────────┼─────────────────────────┐
         ▼                         ▼                         ▼
  [ I/O & HttpClient ]     [ Automated Testing ]     [ Mockito Mocking ]
  • Files.readString() for • JUnit 5: @Test,         • @Mock creates test
    simple file ingestion    @DisplayName, asserts     stunt doubles
  • try-with-resources     • assertThrows validates  • when(...).thenReturn(...)
    prevents handle leaks    domain exceptions         trains mock behavior
  • java.net.http.Client:  • Test business logic     • Zero API bills during
    HTTP/2 zero-dependency   independent of LLM        automated CI/CD runs
```

---

# 8. Practice Exercises & Full Solutions

### 🏋️ Exercise 1: Build a Pure Java HTTP GET Health Checker
**Objective**: Use `java.net.http.HttpClient` to check if a local service (like Ollama on port `11434` or a web service) is alive, returning `true` on HTTP 200 and `false` on connection failure.

#### Solution:
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

### 🏋️ Exercise 2: Safe File Reader with Word Count Metric
**Objective**: Write a method that reads a prompt template from disk, counts total words, and returns an `Optional<String>` containing the prompt if it has at least 5 words.

#### Solution:
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

## 9. Self-Check Quiz

1. **Why is `try-with-resources` superior to a traditional `try-catch-finally` block for closing streams?**
   - *Answer*: `try-with-resources` automatically guarantees that all `AutoCloseable` resources are closed properly even if exceptions are thrown, eliminating resource leaks and messy nested finally blocks.
2. **What are the three main components of Java's built-in HTTP client?**
   - *Answer*: `HttpClient` (the engine/client configuration), `HttpRequest` (the URI, method, headers, and body), and `HttpResponse` (the status code, response headers, and body).
3. **Why should you mock LLM calls in automated unit tests?**
   - *Answer*: Real LLM calls cost money per token, take seconds to run, and are non-deterministic. Mocking provides instant, free, predictable tests for CI/CD pipelines.
4. **What does Mockito's `when(...).thenReturn(...)` do?**
   - *Answer*: It stubs a method call on a mock object, specifying the exact value that should be returned when that method is invoked with specific arguments.
5. **What major milestone did you just achieve?**
   - *Answer*: Completion of Phase 1! You have mastered core Java 21, OOP, memory, polymorphism, data structures, modern records, functional streams, virtual threads, and testing!

---

<p align="center">
  <b>🎉 You Did It! Congratulations on Graduating Phase 1! 🎉</b><br>
  You've gone from the very basics of Java to writing asynchronous HTTP requests, working with JSON, and testing AI models with JUnit 5 & Mockito. You now possess real, production-ready Java foundations!<br>
  Tomorrow, we start <b>Phase 2: Spring Core & Dependency Injection (Days 09–14)</b> — where you will discover why Spring is the #1 framework in enterprise Java, how it manages objects for you automatically, and how it sets the stage for Spring AI. Take a bow, my friend — you earned it!
</p>
