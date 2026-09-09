# Day 36: Streaming Responses — The ChatGPT Typewriter Effect
## ChatClient.stream(), Project Reactor Flux, Server-Sent Events (SSE) & Virtual Threads

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [◀ Day 35: Structured Output](../Day_35_Structured_Output_Java_Objects/Day_35_Structured_Output_Java_Objects.md) | [All 60 Days Overview](../../README.md) | [Day 37: Embedding Models — Turning Text into Vectors ▶](../Day_37_Embedding_Models_Text_to_Vectors/Day_37_Embedding_Models_Text_to_Vectors.md) |

---

## What Will You Learn Today?

When interacting with an AI chat application, user experience is dominated by a single metric: **Time-To-First-Token (TTFT)**.

If a user asks a complex 500-word question and your server waits 15 seconds for the LLM to finish generation before sending an HTTP response, the user perceives the application as frozen, laggy, or broken. But if words begin appearing on the screen within **200 milliseconds**—streaming live like a typewriter—the user perceives the application as instantaneous and intelligent.

Today, you will master **Real-Time Token Streaming** in Java 21 and Spring AI:
- The autoregressive mechanics of LLM token generation and why streaming is foundational to Gen AI UX.
- The `ChatClient.stream()` API and Project Reactor's reactive `Flux<String>` and `Flux<ChatResponse>`.
- **Server-Sent Events (SSE)**: Why the W3C `text/event-stream` protocol is the industry standard for LLM streaming (and why it outperforms WebSockets for this use case).
- Building streaming endpoints in **Spring WebFlux** vs. **Spring MVC with Java 21 Virtual Threads** (`SseEmitter`).
- Managing **Backpressure**: Protecting your JVM and mobile clients from being overwhelmed by high token throughput.
- The **Dual-Dispatch Problem**: How to stream tokens live to a web client while simultaneously accumulating the full text in memory to save to PostgreSQL when generation finishes.
- Consuming SSE in React / Next.js frontends using modern streaming `fetch()`.

---

## Real-World Analogy: Live Radio Broadcast vs. Next Morning's Newspaper

Imagine an intense World Cup final match:

```
+---------------------------------------------------------------------------------------------------+
|                                  THE DATA TRANSMISSION PARADIGM                                   |
|                                                                                                   |
|  SCENARIO 1: The Next Morning's Printed Newspaper (Buffered Non-Streaming Call)                   |
|  - The game is played at 8:00 PM.                                                                 |
|  - You have to wait until 7:00 AM the next morning to read the full recap printed on paper.       |
|  - Latency: 11 hours. Even though the information is complete, the suspense and immediacy is dead.|
|                                                                                                   |
|  SCENARIO 2: Live Play-by-Play Radio Broadcast (Real-Time Token Streaming)                         |
|  - The commentator shouts: "Messi... passes to Mbappe... shoots... GOAL!"                        |
|  - You hear every word the exact millisecond it happens.                                          |
|  - Latency: 0.1 seconds. The emotional engagement is immediate and exhilarating!                 |
+---------------------------------------------------------------------------------------------------+
```

In Generative AI:
- `chatClient.prompt().call().content()` is the **newspaper**: It buffers the entire response and returns it all at once.
- `chatClient.prompt().stream().content()` is the **live radio broadcast**: It delivers tokens to the user the exact millisecond the GPU generates them!

---

## Autoregressive Token Generation & Time-To-First-Token (TTFT)

To understand streaming, you must understand how transformer-based Large Language Models generate text:

```
                               AUTOREGRESSIVE TOKEN PIPELINE
                               
 Prompt: "Java 21 introduces"
          │
          ▼
 Forward Pass 1 ──► Generates: " Virtual"       (Emitted at t = 180ms)
          │
          ▼
 Prompt + " Virtual"
          │
          ▼
 Forward Pass 2 ──► Generates: " Threads"       (Emitted at t = 210ms)
          │
          ▼
 Prompt + " Virtual Threads"
          │
          ▼
 Forward Pass 3 ──► Generates: " for"           (Emitted at t = 240ms)
          │
          ▼
 Prompt + " Virtual Threads for"
          │
          ▼
 Forward Pass 4 ──► Generates: " concurrency."  (Emitted at t = 270ms)
```

1. LLMs do not write full paragraphs at once; they predict the single next most probable token in a loop.
2. Generating 500 tokens takes approximately `500 * 25ms = 12.5 seconds`.
3. With non-streaming, the user stares at a spinner for **12.5 seconds**.
4. With streaming, the user sees the first token at **0.18 seconds**, reading along comfortably while the GPU continues generating the remainder in the background!

---

## 🧭 The Mid-Level Java Developer Bridge: Streaming AI with `Flux<String>` Demystified

If you haven't used Project Reactor or reactive streams, `Flux<String>` can look intimidating. Here is the secret: **it's just an asynchronous queue that pushes words as they arrive.**

| Streaming Term | What It Actually Is in Java | Plain English Meaning |
| :--- | :--- | :--- |
| **`Flux<String>`** | A reactive publisher that emits 0 to N strings over time. | A conveyor belt that passes individual words to the browser as soon as the GPU computes them. |
| **`.stream().content()`** | Calling Spring AI's streaming method on `ChatClient`. | Instead of waiting for the full paragraph, Spring AI hooks into the model's token stream. |
| **`text/event-stream`** | The HTTP header returned to the browser. | Tells Chrome or React: *"Keep this connection open; words are going to arrive one-by-one."* |
| **SSE vs. WebSockets** | SSE is standard HTTP GET; WebSockets upgrades the protocol. | WebSockets is a 2-way walkie-talkie (heavy setup). SSE is a 1-way FM radio broadcast (simple, passes all firewalls). |
| **Virtual Threads Compatibility** | Java 21 handles SSE streams effortlessly without blocking OS threads. | You can stream AI responses to 50,000 users at the same time without running out of server RAM! |

---

## Why Server-Sent Events (SSE) Beat WebSockets for LLMs

Many developers ask: *"Should I use WebSockets for streaming AI chat?"*

In 99% of Gen AI architectures, **Server-Sent Events (SSE)** is the superior choice:

| Architectural Metric | Server-Sent Events (SSE) | WebSockets |
|:---|:---|:---|
| **Protocol** | Standard HTTP/1.1 or HTTP/2 (`text/event-stream`). | Custom bidirectional TCP handshake (`ws://` or `wss://`). |
| **Directionality** | Unidirectional (Server -> Client). Perfect for AI streaming! | Bidirectional. Overkill when client only sends one prompt. |
| **Firewall & Proxy Friendly** | 100% standard HTTP. Traverses corporate firewalls, cloud proxies, and API gateways effortlessly. | Often blocked or dropped by corporate enterprise proxies and load balancers. |
| **HTTP/2 Multiplexing** | Built-in! Hundreds of SSE streams can share a single TCP connection. | Requires separate TCP connections per socket. |
| **Auto-Reconnection** | Native browser reconnection with `Last-Event-ID`. | Must be manually coded in JavaScript. |

---

## The W3C Server-Sent Events Wire Format

SSE is an incredibly lightweight plain-text protocol. The server sends HTTP headers:

```http
HTTP/1.1 200 OK
Content-Type: text/event-stream
Cache-Control: no-cache
Connection: keep-alive
```

Followed by chunks separated by double newlines (`\n\n`):

```text
id: 1
event: token
data: Java

id: 2
event: token
data: 21

id: 3
event: token
data: Virtual

id: 4
event: token
data: Threads
```

The browser's event parser detects `\n\n`, fires an `onmessage` callback, and appends the token to the DOM immediately.

---

## Spring AI Streaming Architecture: `ChatClient.stream()`

Spring AI provides first-class reactive streaming support powered by **Project Reactor** (`Flux<T>`):

```
┌────────────────────────────────────────────────────────────────────────┐
│ ChatClient.prompt().stream()                                           │
│   ├── .content()       ──► Returns Flux<String>                        │
│   └── .chatResponse()  ──► Returns Flux<ChatResponse> (with metadata)  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ Spring WebFlux Controller / Spring MVC SseEmitter                      │
│   - Dispatches SSE frames over HTTP response stream                    │
│   - Yields thread during token intervals (zero thread starvation)      │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ Client Browser / React Frontend                                        │
│   - Reads stream chunk by chunk using fetch() ReadableStream           │
│   - Renders live typewriter effect                                     │
└────────────────────────────────────────────────────────────────────────┘
```

---

## Implementation 1: Reactive Streaming with Spring WebFlux

If your application uses Spring WebFlux (`spring-boot-starter-webflux`), streaming is as simple as returning `Flux<String>`:

```java
package com.genai.springai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/ai")
public class WebFluxStreamingController {

    private final ChatClient chatClient;

    public WebFluxStreamingController(ChatClient.Builder builder) {
        this.chatClient = builder
            .defaultSystem("You are an expert streaming AI assistant.")
            .build();
    }

    @GetMapping(value = "/stream-flux", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamAiResponse(@RequestParam String prompt) {
        return chatClient.prompt()
            .user(prompt)
            .stream()
            .content(); // Emits each token as it arrives from OpenAI/Ollama
    }
}
```

---

## Implementation 2: Spring MVC Streaming with Java 21 Virtual Threads

If your application uses traditional Spring MVC (`spring-boot-starter-web`), you can achieve identical high-concurrency streaming using `SseEmitter` paired with Java 21 Virtual Threads!

```java
package com.genai.springai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/ai")
public class MvcVirtualThreadStreamingController {

    private final ChatClient chatClient;

    public MvcVirtualThreadStreamingController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping(value = "/stream-mvc", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMvc(@RequestParam String prompt) {
        // 60-second connection timeout
        SseEmitter emitter = new SseEmitter(60_000L);

        // Run streaming inside a lightweight Java 21 Virtual Thread!
        Thread.startVirtualThread(() -> {
            try {
                chatClient.prompt()
                    .user(prompt)
                    .stream()
                    .content()
                    .doOnNext(token -> {
                        try {
                            emitter.send(SseEmitter.event()
                                .name("token")
                                .data(token));
                        } catch (IOException ex) {
                            emitter.completeWithError(ex);
                        }
                    })
                    .doOnComplete(emitter::complete)
                    .doOnError(emitter::completeWithError)
                    .blockLast(); // Blocks only the virtual thread, NOT an OS carrier thread!
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        });

        return emitter;
    }
}
```

---

## The Dual-Dispatch Problem: Streaming While Persisting

In production enterprise chat systems, you have two conflicting requirements:
1. **Live User Feedback**: Stream tokens chunk-by-chunk to the browser.
2. **Conversation History Persistence**: Save the *entire accumulated response string* to the PostgreSQL `chat_messages` table once streaming completes.

If you only stream, you lose the conversation history. If you wait to buffer, you lose the streaming typewriter effect.

### The Solution: `StreamAggregator` & Reactor Hooks

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
            .doOnNext(fullResponseBuffer::append) // Accumulate token in memory
            .doOnComplete(() -> {
                // When stream finishes, persist entire message to DB
                String completeText = fullResponseBuffer.toString();
                ChatMessage assistantMessage = new ChatMessage(sessionId, "ASSISTANT", completeText);
                messageRepository.save(assistantMessage);
                System.out.println("Persisted complete response (" + completeText.length() + " chars) to PostgreSQL.");
            })
            .doOnError(error -> {
                System.err.println("Stream aborted due to error: " + error.getMessage());
            });
    }
}
```

---

## Consuming SSE in React / Next.js Frontends

Standard JavaScript `new EventSource(url)` does **not** support custom HTTP headers (such as `Authorization: Bearer <JWT>`).

In modern frontend engineering, we consume SSE using the native `fetch()` API with `ReadableStream`:

```typescript
// Modern frontend streaming consumer
async function streamChatResponse(prompt: string, jwtToken: string) {
  const response = await fetch('/api/v1/ai/stream-flux?prompt=' + encodeURIComponent(prompt), {
    headers: {
      'Authorization': `Bearer ${jwtToken}`,
      'Accept': 'text/event-stream'
    }
  });

  const reader = response.body?.getReader();
  const decoder = new TextDecoder('utf-8');

  while (true) {
    const { done, value } = await reader!.read();
    if (done) break;

    const chunk = decoder.decode(value);
    // Parse SSE "data: ..." frames and append token to UI
    for (const line of chunk.split('\n')) {
      if (line.startsWith('data:')) {
        const token = line.replace('data:', '');
        appendTokenToChatBubble(token);
      }
    }
  }
}
```

---

## Step-by-Step Production Code Walkthrough

Let's review the companion code written for today's lesson in `Phase_06_Spring_AI/Day_36_Streaming_Responses/code/`:

### 1. `SseFrame.java`
Models standard W3C SSE event specifications:

```java
public record SseFrame(String event, String data, String id) {
    public String toWireFormat() {
        StringBuilder sb = new StringBuilder();
        if (id != null) sb.append("id: ").append(id).append("\n");
        if (event != null) sb.append("event: ").append(event).append("\n");
        sb.append("data: ").append(data.replace("\n", "\\n")).append("\n\n");
        return sb.toString();
    }
}
```

### 2. `OllamaStreamingChatModel.java`
Simulates autoregressive token generation with realistic 25ms generation delay:

```java
public void stream(String prompt, Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError) {
    String[] tokens = generatedContent.split("(?<=\\s)|(?=[.,!?:;])");
    try {
        for (String token : tokens) {
            onToken.accept(token);
            Thread.sleep(25); // Simulates GPU token generation interval
        }
        onComplete.run();
    } catch (Exception ex) {
        onError.accept(ex);
    }
}
```

### 3. `StreamAggregator.java`
Concurrently forwards tokens to the client while compiling the complete text for database storage:

```java
public synchronized void onNextToken(String token) {
    buffer.append(token);
    tokenCount.incrementAndGet();
    downstreamClient.accept(token);
}

public synchronized void onStreamComplete() {
    onFullTextAccumulated.accept(buffer.toString());
}
```

### 4. Running the Verification Demonstration
Compile and execute:

```bash
javac -d out Phase_06_Spring_AI/Day_36_Streaming_Responses/code/*.java
java -cp out com.genai.springai.streaming.StreamingDemo
```

Output:
```text
================================================================================
  DAY 36: REAL-TIME TOKEN STREAMING & SSE DEMONSTRATION                         
================================================================================

[TEST 1] Live Console Typewriter Effect (Tokens streamed as generated)...
  [STREAMING OUTPUT]: Java 21 Virtual Threads combined with Spring AI streaming revolutionizes modern LLM applications. Instead of blocking heavy operating system threads while waiting for token generation, lightweight Virtual Threads yield their carrier thread, allowing a single JVM node to effortlessly sustain tens of thousands of concurrent Server-Sent Event (SSE) connections!

  ✅ STREAM COMPLETE: 53 tokens in 1375 ms (38.5 tokens/sec)

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

## Hands-On Exercises (With Complete Solutions)

### Exercise 1: WebFlux SSE Controller with Custom Event Names
**Problem Statement:**  
Create a Spring WebFlux controller endpoint `GET /api/v1/ai/stream-events` that returns `Flux<ServerSentEvent<String>>`.  
The first event must be named `"start"`, subsequent events containing tokens must be named `"token"`, and the final event must be named `"done"` with data `"COMPLETE"`.

<details>
<summary>👉 View Solution</summary>

```java
package com.genai.springai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

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
</details>

---

### Exercise 2: Token-by-Token Backpressure Buffer Guard
**Problem Statement:**  
In high-throughput environments, an aggressive GPU can generate 100 tokens/sec, causing a slow mobile client on 3G to crash if unbounded.  
Configure Project Reactor's `onBackpressureBuffer` with a capacity of 256 tokens and a strategy that drops the oldest tokens if the buffer fills.

<details>
<summary>👉 View Solution</summary>

```java
public Flux<String> streamWithBackpressure(ChatClient client, String prompt) {
    return client.prompt()
        .user(prompt)
        .stream()
        .content()
        .onBackpressureBuffer(
            256, // Buffer up to 256 tokens
            droppedToken -> System.err.println("Dropped token due to slow client: " + droppedToken),
            BufferOverflowStrategy.DROP_OLDEST
        );
}
```
*Explanation:* `onBackpressureBuffer` decouples fast publisher generation from slow subscriber consumption, ensuring the JVM heap does not accumulate unbounded token queues.
</details>

---

### Exercise 3: Virtual-Thread Idle Timeout Guard
**Problem Statement:**  
When streaming over `SseEmitter` in Spring MVC, if the LLM stalls or the connection hangs for more than 15 seconds without a new token, automatically close the connection to reclaim resources.

<details>
<summary>👉 View Solution</summary>

```java
@GetMapping(value = "/stream-safe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter streamSafe(@RequestParam String prompt) {
    SseEmitter emitter = new SseEmitter(15_000L); // 15 second inactivity timeout

    emitter.onTimeout(() -> {
        System.err.println("SSE Stream timed out due to LLM inactivity.");
        emitter.complete();
    });

    emitter.onError(ex -> {
        System.err.println("SSE Stream disconnected: " + ex.getMessage());
        emitter.completeWithError(ex);
    });

    Thread.startVirtualThread(() -> {
        try {
            chatClient.prompt().user(prompt).stream().content().doOnNext(token -> {
                try {
                    emitter.send(token);
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            }).doOnComplete(emitter::complete).blockLast();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    });

    return emitter;
}
```
</details>

---

## 5-Question Self-Check Quiz

#### 1. What is "Time-To-First-Token" (TTFT) and why is it the defining metric for AI chat interfaces?
- A) The time required to install Ollama on Windows.
- B) The latency between the user hitting Send and the very first character appearing on their screen, directly determining the user's perception of responsiveness.
- C) The cost of 1,000 prompt tokens.
- D) The time it takes to compile Java bytecode.

#### 2. Why is Server-Sent Events (SSE) generally preferred over WebSockets for LLM chat streaming?
- A) WebSockets are deprecated in HTTP/2.
- B) SSE is a lightweight, unidirectional HTTP standard that effortlessly traverses corporate firewalls, API gateways, and supports HTTP/2 multiplexing without the overhead of maintaining bidirectional socket handshakes.
- C) WebSockets do not work with React.
- D) SSE can only stream binary data.

#### 3. What reactive type is returned by `ChatClient.prompt().stream().content()` in Spring AI?
- A) `Mono<String>`
- B) `CompletableFuture<String>`
- C) `Flux<String>`
- D) `Stream<String>`

#### 4. How does Java 21's Virtual Threads feature enhance Spring MVC streaming with `SseEmitter`?
- A) It makes the GPU run 2x faster.
- B) Virtual Threads yield their carrier OS thread during I/O blocking intervals, allowing a single Spring MVC server to handle tens of thousands of concurrent open SSE streams without thread pool exhaustion.
- C) Virtual Threads eliminate the need for HTTP headers.
- D) It automatically translates Python code to Java.

#### 5. How do you solve the "Dual-Dispatch" problem (streaming live tokens to the client while saving the complete chat response to a database)?
- A) By calling the LLM twice (once for stream, once for DB).
- B) By using Project Reactor hooks like `doOnNext()` to append each token to a thread-safe string buffer, and `doOnComplete()` to persist the accumulated string to the database when generation finishes.
- C) Databases cannot store streamed text.
- D) By disabling streaming.

---

### Quiz Answers & Explanations

1. **B is correct**: While total response generation may take 15 seconds, a low TTFT (<300ms) creates the perception of instant responsiveness.
2. **B is correct**: LLM interactions are inherently request-and-stream-response. SSE operates over standard HTTP, making it universally compatible with load balancers, caching proxies, and TLS.
3. **C is correct**: Spring AI uses Project Reactor's `Flux<String>` to represent an asynchronous multi-item stream of tokens.
4. **B is correct**: Virtual Threads make synchronous blocking I/O virtually cost-free in memory, allowing Spring MVC to sustain massive concurrent streaming connections.
5. **B is correct**: Intercepting the stream with `doOnNext` avoids paying double API costs while ensuring complete conversation history is preserved in PostgreSQL.

---

## Day 36 Summary & Next Steps

Today you mastered:
1. **The Power of Streaming**: Slashing perceived latency and maximizing conversational engagement with real-time token delivery.
2. **The W3C SSE Protocol**: Why `text/event-stream` is the industry standard for LLM streaming.
3. **Spring AI `ChatClient.stream()`**: Consuming reactive `Flux<String>` and `Flux<ChatResponse>`.
4. **Dual-Dispatch Architecture**: Streaming live to the client while simultaneously aggregating the full response for database persistence.
5. **Modern Frontend Consumption**: Consuming streams in React using native `fetch()` and `ReadableStream`.

👉 **Tomorrow in Day 37: Embedding Models — Turning Text into Vectors** — You will enter the world of Vector AI, learning how embedding models convert human language into multi-dimensional floating-point vectors for semantic similarity search!
