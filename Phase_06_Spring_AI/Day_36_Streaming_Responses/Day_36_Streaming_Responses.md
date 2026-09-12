# Day 36: Streaming Responses — The ChatGPT Typewriter Effect
## ChatClient.stream(), Project Reactor Flux, Server-Sent Events (SSE) & Virtual Threads

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [◀ Day 35: Structured Output](../Day_35_Structured_Output_Java_Objects/Day_35_Structured_Output_Java_Objects.md) | [All 60 Days Overview](../../README.md) | [Day 37: Embedding Models — Turning Text into Vectors ▶](../Day_37_Embedding_Models_Text_to_Vectors/Day_37_Embedding_Models_Text_to_Vectors.md) |

---

## 1. Topic Overview

Streaming in Spring AI leverages autoregressive token emission via Project Reactor (`Flux<String>`) and Server-Sent Events (SSE) to deliver text chunks to clients the millisecond they are generated. In enterprise Generative AI systems, streaming minimizes Time-To-First-Token (TTFT) from 15 seconds down to under 200 milliseconds, giving users an immediate interactive experience while Java 21 Virtual Threads allow a single backend node to sustain tens of thousands of concurrent open streams without thread exhaustion.

---

## 2. Basic Foundations (True Zero)

### Why Streaming Matters: Time-To-First-Token (TTFT)
Language models generate responses token by token. Generating a 500-word response takes 10 to 15 seconds of GPU compute.

If an application waits for the entire generation to finish before returning an HTTP response, the user stares at a frozen screen or loading spinner for 15 seconds, creating the impression that the system has hung. 

With **Streaming**, the server transmits each token over an open HTTP connection the instant it is computed. The user sees words appearing in under **200 milliseconds**—a metric known as **Time-To-First-Token (TTFT)**.

```
+-----------------------------------------------------------------------------------+
|               THE LIVE RADIO BROADCAST VS. NEWSPAPER ANALOGY                      |
|                                                                                   |
|  SCENARIO 1: The Next Morning's Printed Newspaper (Buffered Non-Streaming):       |
|  - The World Cup final finishes at 10:00 PM.                                      |
|  - You have to wait until 7:00 AM the next morning to read the final score.       |
|  - Latency: 9 hours. Even though the information is complete, the immediacy is    |
|    dead and the user experience is sluggish.                                      |
|                                                                                   |
|  SCENARIO 2: Live Play-by-Play Radio Broadcast (Real-Time Token Streaming):        |
|  - The commentator shouts: "Mbappe... shoots... and it's a GOAL!"                 |
|  - You hear every word the exact millisecond it happens.                          |
|  - Latency: 0.1 seconds. The emotional engagement is instantaneous!               |
|                                                                                   |
|  In Spring AI: `call().content()` is the printed newspaper.                       |
|                `stream().content()` is the live play-by-play radio broadcast!     |
+-----------------------------------------------------------------------------------+
```

### Minimal Beginner-Friendly Working Code Example

Below is a self-contained Java 21 simulation demonstrating how an asynchronous token emitter streams chunks to a consumer with realistic generation delays:

```java
import java.util.function.Consumer;

public class BasicStreamingSimulation {

    // Simulates an autoregressive streaming LLM
    public static void streamTokens(String prompt, Consumer<String> onToken, Runnable onComplete) {
        String generatedResponse = "Java 21 Virtual Threads make streaming AI tokens effortless and scalable.";
        String[] tokens = generatedResponse.split("(?<=\\s)"); // Split preserving spaces

        // Run streaming loop
        for (String token : tokens) {
            onToken.accept(token);
            try {
                Thread.sleep(40); // 40ms simulated GPU token generation interval
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        onComplete.run();
    }

    public static void main(String[] args) {
        System.out.println("--- Starting Real-Time Token Stream ---");
        long startNanos = System.nanoTime();

        streamTokens(
            "Explain Java 21 streaming",
            token -> {
                // Live Typewriter Effect: Print immediately without trailing newline
                System.out.print(token);
                System.out.flush();
            },
            () -> {
                double elapsedSec = (System.nanoTime() - startNanos) / 1_000_000_000.0;
                System.out.printf("%n%n--- Stream Finished in %.2f seconds! ---%n", elapsedSec);
            }
        );
    }
}
```

#### Line-by-Line Walkthrough:
- **Lines 6–8**: `streamTokens` accepts a prompt and functional callbacks (`Consumer<String>` for token chunks, `Runnable` for completion).
- **Lines 11–19**: Simulates the GPU autoregressive loop: each token is handed to `onToken.accept()` and followed by a 40ms pause.
- **Lines 26–39**: Main method triggers streaming. `System.out.print(token)` and `System.out.flush()` immediately write each token to the console as it arrives, creating the classic typewriter effect.

---

## 3. Core Concept Walkthrough (Basic → Intermediate)

### Autoregressive Generation & Time-To-First-Token (TTFT)

```
 Prompt: "Java 21 introduces"
          │
          ▼
 Forward Pass 1 ──► Generates: " Virtual"       (Emitted at t = 180ms - TTFT!)
          │
          ▼
 Forward Pass 2 ──► Generates: " Threads"       (Emitted at t = 210ms)
          │
          ▼
 Forward Pass 3 ──► Generates: " for"           (Emitted at t = 240ms)
          │
          ▼
 Forward Pass 4 ──► Generates: " concurrency."  (Emitted at t = 270ms)
```

Generating 500 tokens takes ~12 seconds. Without streaming, the client waits 12 seconds in silence. With streaming, reading begins at **180 milliseconds**.

---

### Why Server-Sent Events (SSE) Beat WebSockets for LLMs

```
+-----------------------+-----------------------------------+---------------------------------------+
| Metric                | Server-Sent Events (SSE)          | WebSockets                            |
+-----------------------+-----------------------------------+---------------------------------------+
| Protocol              | Standard HTTP (`text/event-stream`)| Custom TCP handshake (`ws://`, `wss://`)|
+-----------------------+-----------------------------------+---------------------------------------+
| Directionality        | Unidirectional (Server -> Client) | Bidirectional (Full Duplex)           |
+-----------------------+-----------------------------------+---------------------------------------+
| Enterprise Proxies    | 100% compatible with firewalls,   | Frequently blocked by corporate       |
|                       | load balancers, and Cloudflare.   | proxies and corporate firewalls.      |
+-----------------------+-----------------------------------+---------------------------------------+
| HTTP/2 Multiplexing   | Supported out of the box. Multiple| Requires distinct TCP connection      |
|                       | streams share a single socket.    | overhead per active client.           |
+-----------------------+-----------------------------------+---------------------------------------+
| Reconnection          | Built-in browser reconnection.    | Must be manually coded in JavaScript. |
+-----------------------+-----------------------------------+---------------------------------------+
```

---

### The W3C Server-Sent Events Wire Format

SSE transmits text blocks over HTTP separated by double newlines (`\n\n`):

```http
HTTP/1.1 200 OK
Content-Type: text/event-stream
Cache-Control: no-cache
Connection: keep-alive

id: 1
event: token
data: Java

id: 2
event: token
data: 21

id: 3
event: token
data: Virtual
```

The browser's event parser detects `\n\n`, fires an event callback, and renders the chunk on screen immediately.

---

### Implementation 1: Reactive Streaming with Spring WebFlux

In reactive Spring Boot applications (`spring-boot-starter-webflux`), return `Flux<String>` directly:

```java
package com.example.genai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/ai")
public class WebFluxStreamingController {

    private final ChatClient chatClient;

    public WebFluxStreamingController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping(value = "/stream-flux", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamAiResponse(@RequestParam String prompt) {
        return chatClient.prompt()
            .user(prompt)
            .stream()
            .content(); // Emits each token as it arrives from the model
    }
}
```

---

### Implementation 2: Spring MVC Streaming with Java 21 Virtual Threads

In standard Spring MVC applications (`spring-boot-starter-web`), use `SseEmitter` within a Virtual Thread:

```java
package com.example.genai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;

@RestController
@RequestMapping("/api/v1/ai")
public class MvcStreamingController {

    private final ChatClient chatClient;

    public MvcStreamingController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping(value = "/stream-mvc", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMvc(@RequestParam String prompt) {
        SseEmitter emitter = new SseEmitter(60_000L); // 60-second timeout

        Thread.startVirtualThread(() -> {
            try {
                chatClient.prompt()
                    .user(prompt)
                    .stream()
                    .content()
                    .doOnNext(token -> {
                        try {
                            emitter.send(SseEmitter.event().name("token").data(token));
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    })
                    .doOnComplete(emitter::complete)
                    .doOnError(emitter::completeWithError)
                    .blockLast(); // Blocks virtual thread only, yielding OS carrier thread
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        });

        return emitter;
    }
}
```

---

## 4. Prerequisite & Supporting Concepts

### Prerequisite / Supporting Concept: Project Reactor `Flux`
`Flux<T>` represents an asynchronous sequence of 0 to N items. Unlike a Java `List<T>` (which requires all elements in memory before returning), a `Flux` pushes items lazily over time as they become available. Calling `.stream().content()` returns a `Flux<String>` publisher where each emitted string represents an AI token.

### Prerequisite / Supporting Concept: Virtual Thread Carrier Unpinning
When streaming over `SseEmitter`, the connection remains open for 10 to 30 seconds. On traditional platform threads, 200 concurrent users would tie up all 200 Tomcat worker threads. 

With Java 21 Virtual Threads, waiting for the next token parks the virtual thread and frees the underlying OS carrier thread to serve other requests, allowing a single JVM to sustain tens of thousands of active streams.

### Prerequisite / Supporting Concept: Frontend SSE Consumption via `fetch()`
Standard browser `new EventSource(url)` does not support custom HTTP headers like `Authorization: Bearer <jwt>`. In modern React/Next.js frontends, SSE is consumed via the standard `fetch()` API reading `response.body.getReader()` as a `ReadableStream`.

---

## 5. Advanced Depth (Intermediate → Advanced)

### The Dual-Dispatch Problem: Streaming While Persisting

In enterprise AI applications, you must fulfill two simultaneous requirements:
1. Stream tokens live to the user interface.
2. Save the **entire completed response** to the PostgreSQL `chat_messages` table once finished.

Calling the model twice (once for stream, once for persistence) doubles billing costs and latency. Instead, use Reactor lifecycle hooks:

```java
@Service
public class StreamingChatService {

    private final ChatClient chatClient;
    private final ChatMessageRepository messageRepository;

    public StreamingChatService(ChatClient.Builder builder, ChatMessageRepository messageRepository) {
        this.chatClient = builder.build();
        this.messageRepository = messageRepository;
    }

    public Flux<String> streamAndPersist(String sessionId, String userPrompt) {
        StringBuilder fullResponseBuffer = new StringBuilder();

        return chatClient.prompt()
            .user(userPrompt)
            .stream()
            .content()
            .doOnNext(fullResponseBuffer::append) // 1. Accumulate token in memory
            .doOnComplete(() -> {
                // 2. When stream finishes, persist entire message to database
                String completeText = fullResponseBuffer.toString();
                ChatMessage assistantMessage = new ChatMessage(sessionId, "ASSISTANT", completeText);
                messageRepository.save(assistantMessage);
            })
            .doOnError(error -> {
                System.err.println("Stream failed: " + error.getMessage());
            });
    }
}
```

---

### Hands-On Simulation Code Walkthrough

The companion code repository demonstrates this architecture:
- `SseFrame.java`: Formats standard W3C SSE wire frames (`id`, `event`, `data\n\n`).
- `OllamaStreamingChatModel.java`: Simulates autoregressive token generation with realistic 25ms GPU intervals.
- `StreamAggregator.java`: Concurrently forwards tokens while aggregating the full text for database storage.
- `StreamingDemo.java`: 3-scenario verification test suite validating live console typewriter output, W3C SSE formatting, and concurrent database persistence.

```powershell
# Compile Day 36 code
javac -d out Phase_06_Spring_AI/Day_36_Streaming_Responses/code/*.java

# Run StreamingDemo
java -cp out com.genai.springai.streaming.StreamingDemo
```

#### Verified Execution Output:
```
================================================================================
  DAY 36: REAL-TIME TOKEN STREAMING & SSE DEMONSTRATION                         
================================================================================

[TEST 1] Live Console Typewriter Effect (Tokens streamed as generated)...
  [STREAMING OUTPUT]: Java 21 Virtual Threads combined with Spring AI streaming revolutionizes modern LLM applications. Instead of blocking heavy operating system threads while waiting for token generation, lightweight Virtual Threads yield their carrier thread, allowing a single JVM node to effortlessly sustain tens of thousands of concurrent Server-Sent Event (SSE) connections!

  [OK] STREAM COMPLETE: 53 tokens in 1375 ms (38.5 tokens/sec)

[TEST 2] Encoding Tokens as W3C SSE Event Stream Chunks...
--- First 3 Raw SSE HTTP Frames ---
id: 1
event: token
data: Chunk #1

id: 2
event: token
data: Chunk #2

id: 3
event: token
data: Chunk #3

[TEST 3] StreamAggregator: Concurrently streaming & saving to DB...
  [DATABASE SAVE EVENT] Successfully saved full conversation history!
  Persisted Text Length: 360 chars.
  First 80 chars: "Java 21 Virtual Threads combined with Spring AI streaming revolutionizes modern ..."

================================================================================
  STREAMING PIPELINE & CONCURRENT PERSISTENCE VERIFIED SUCCESSFULLY!           
================================================================================
```

---

## 6. Quick Recap

| Concept | Description | Enterprise Rule / Best Practice |
| :--- | :--- | :--- |
| **TTFT** | Time-To-First-Token latency | Target < 300ms for responsive user perception. |
| **`.stream()`** | Fluent streaming entry point | Returns reactive `Flux` publishers instead of strings. |
| **SSE (`text/event-stream`)**| Unidirectional HTTP streaming | Preferred over WebSockets for AI chat streaming. |
| **Virtual Threads** | Lightweight user-mode threads | Use with `SseEmitter` in Spring MVC for massive scale. |
| **Dual-Dispatch** | Streaming live while buffering to DB | Use `.doOnNext()` to buffer and `.doOnComplete()` to save. |
| **Backpressure** | Handling fast producer / slow consumer | Configure Reactor `onBackpressureBuffer` to prevent OOM. |

---

## 7. Self-Check Questions & Practice Exercises

### Conceptual & Architectural Questions

#### Q1: What is "Time-To-First-Token" (TTFT) and why is it the defining metric for AI chat interfaces?
**Answer**: TTFT measures the latency between when a user submits a prompt and when the very first token renders on their screen. While total generation might require 15 seconds, a low TTFT (< 300ms) creates the immediate psychological perception of real-time responsiveness.

#### Q2: Why is Server-Sent Events (SSE) generally preferred over WebSockets for LLM chat streaming?
**Answer**: LLM chat interactions are inherently unidirectional during generation (server sending text to client). SSE operates over standard HTTP (`text/event-stream`), effortlessly traversing corporate firewalls, cloud load balancers, and caching proxies while natively supporting HTTP/2 multiplexing without complex socket handshakes.

#### Q3: What reactive type is returned by `ChatClient.prompt().stream().content()` in Spring AI?
**Answer**: **`Flux<String>`**, which emits tokens asynchronously as they arrive from the model provider.

#### Q4: How does Java 21's Virtual Threads feature enhance Spring MVC streaming with `SseEmitter`?
**Answer**: Streaming connections remain open for tens of seconds. Traditional platform threads tie up dedicated OS threads, causing thread pool exhaustion at ~200 users. Virtual Threads yield their underlying OS carrier thread during I/O wait periods, enabling tens of thousands of concurrent open SSE streams on a single JVM.

#### Q5: How do you solve the "Dual-Dispatch" problem (streaming live tokens to the client while saving the complete chat response to a database)?
**Answer**: By utilizing Project Reactor hooks: use `doOnNext(tokenBuffer::append)` to assemble the full response in memory while tokens are transmitted to the user, and trigger the database insert inside `doOnComplete()`.

---

### Hands-On Practice Exercises

#### Exercise 1: WebFlux SSE Controller with Custom Event Names
**Task**: Build a Spring WebFlux controller endpoint `GET /api/v1/ai/stream-events` returning `Flux<ServerSentEvent<String>>` emitting a `"start"` event, `"token"` events, and a final `"done"` event.

```java
// Solution:
@RestController
@RequestMapping("/api/v1/ai")
public class SseEventController {

    private final ChatClient chatClient;

    public SseEventController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping("/stream-events")
    public Flux<ServerSentEvent<String>> streamEvents(@RequestParam String prompt) {
        Flux<ServerSentEvent<String>> start = Flux.just(
            ServerSentEvent.<String>builder().event("start").data("STREAM_INIT").build()
        );

        Flux<ServerSentEvent<String>> tokens = chatClient.prompt()
            .user(prompt)
            .stream()
            .content()
            .map(token -> ServerSentEvent.<String>builder().event("token").data(token).build());

        Flux<ServerSentEvent<String>> done = Flux.just(
            ServerSentEvent.<String>builder().event("done").data("COMPLETE").build()
        );

        return Flux.concat(start, tokens, done);
    }
}
```

#### Exercise 2: Token Backpressure Buffer Guard
**Task**: Configure Project Reactor's `onBackpressureBuffer` with a capacity of 256 tokens and a strategy that drops the oldest tokens if a slow client lags behind.

```java
// Solution:
public Flux<String> streamWithBackpressure(ChatClient client, String prompt) {
    return client.prompt()
        .user(prompt)
        .stream()
        .content()
        .onBackpressureBuffer(
            256,
            droppedToken -> System.err.println("Dropped token due to slow consumer: " + droppedToken),
            BufferOverflowStrategy.DROP_OLDEST
        );
}
```

#### Exercise 3: Virtual-Thread Idle Timeout Guard
**Task**: Configure an `SseEmitter` with an explicit 15-second inactivity timeout that completes the connection if the LLM stalls.

```java
// Solution:
@GetMapping(value = "/stream-safe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter streamSafe(@RequestParam String prompt) {
    SseEmitter emitter = new SseEmitter(15_000L);

    emitter.onTimeout(() -> {
        System.err.println("SSE Stream timed out due to inactivity.");
        emitter.complete();
    });

    emitter.onError(ex -> emitter.completeWithError(ex));

    Thread.startVirtualThread(() -> {
        try {
            chatClient.prompt()
                .user(prompt)
                .stream()
                .content()
                .doOnNext(token -> {
                    try { emitter.send(token); } catch (Exception e) { emitter.completeWithError(e); }
                })
                .doOnComplete(emitter::complete)
                .blockLast();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    });

    return emitter;
}
```

---

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [◀ Day 35: Structured Output](../Day_35_Structured_Output_Java_Objects/Day_35_Structured_Output_Java_Objects.md) | [All 60 Days Overview](../../README.md) | [Day 37: Embedding Models — Turning Text into Vectors ▶](../Day_37_Embedding_Models_Text_to_Vectors/Day_37_Embedding_Models_Text_to_Vectors.md) |
