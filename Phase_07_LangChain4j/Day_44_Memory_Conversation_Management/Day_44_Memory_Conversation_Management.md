# Day 44: Memory and Conversation Management

[← Previous: Day 43 - LangChain4j Introduction](../Day_43_LangChain4j_Introduction_AiServices/Day_43_LangChain4j_Introduction_AiServices.md) | [Next: Day 45 - Structured Extraction & Guardrails →](../Day_45_Structured_Extraction_Guardrails/Day_45_Structured_Extraction_Guardrails.md)

---

## 1. Topic Overview
Conversational memory management allows stateless Large Language Models to maintain dialogue continuity, conversational state, and personal context across multiple user turns without exceeding model context limits. In enterprise Java systems, LangChain4j provides composable sliding-window memory policies, strict system-prompt preservation, multi-tenant isolation via `@MemoryId`, and persistent database backing stores.

---

## 2. Basic Foundations (True Zero)

### The Statelessness Problem: Why LLMs Have "Goldfish Memory"
By nature, Large Language Models have **zero internal memory**. Every HTTP request sent to OpenAI, Anthropic, or Ollama is completely independent:
- Turn 1: User says: *"Hi, I'm Alex and I live in Seattle."* $\rightarrow$ Model says: *"Hello Alex!"*
- Turn 2: User says: *"What should I wear outside today?"* $\rightarrow$ Model says: *"I don't know where you are! What city do you live in?"*

To simulate memory, our Java application must capture past messages and prepend them into every subsequent request. But if you naively send the entire conversation history:
1. You quickly exceed the model's **Context Window limit** (causing crash errors).
2. Your **API costs skyrocket exponentially** as old messages are re-processed repeatedly.
3. Response latency degrades from 300ms to several seconds.

### Relatable Physical Analogy: The Executive Briefing Folder
Imagine briefing a busy corporate CEO:
- **Naive Approach**: Every morning, you wheel in a 2,000-page file cabinet containing every email, invoice, and note from the last five years. The CEO takes 4 hours just to find yesterday's action items.
- **Smart Sliding Window Approach**: You maintain a clean 5-page **Executive Briefing Folder**:
  - The permanent company mission statement is always pinned to page 1 (**System Message**).
  - The remaining 4 pages hold only the most recent 6 discussion points (**Sliding Window**).
  - Older notes are archived in the office library (**Persistent Database Store**) and reviewed only when needed.

### Minimal Beginner-Friendly Working Code
Here is how to equip an `AiServices` agent with a sliding conversation window in LangChain4j:

```java
package com.genai.langchain4j.memory;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;

public class SimpleMemoryRunner {

    public interface ConversationalAssistant {
        @SystemMessage("You are a helpful customer concierge. Keep your answers brief.")
        String chat(String userMessage);
    }

    public static void main(String[] args) {
        ChatLanguageModel model = OpenAiChatModel.builder()
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .modelName("gpt-4o")
            .build();

        // Configure an AiServices agent with a 10-message sliding window
        ConversationalAssistant assistant = AiServices.builder(ConversationalAssistant.class)
            .chatLanguageModel(model)
            .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
            .build();

        // Turn 1: Introduce name
        System.out.println(assistant.chat("Hello! My name is Alex and I am based in Seattle."));

        // Turn 2: Ask question relying on prior turn
        // The assistant remembers Alex and Seattle because of ChatMemory!
        System.out.println(assistant.chat("What city did I say I live in?"));
    }
}
```

### Line-by-Line Walkthrough
1. **`public interface ConversationalAssistant`**: Declares our declarative AI service interface.
2. **`MessageWindowChatMemory.withMaxMessages(10)`**: Instantiates a memory window that retains the most recent 10 messages (5 user questions + 5 AI responses) while automatically discarding older dialogue turns.
3. **`builder.chatMemory(...)`**: Binds the memory window to the generated dynamic proxy.
4. **`assistant.chat("What city did I say I live in?")`**: LangChain4j automatically reads past turns from memory, merges them into the prompt, dispatches to the model, and saves the new response back into the window.

---

## 3. Core Concept Walkthrough (Basic → Intermediate)

```
+-------------------------------------------------------------------------------+
|                       LANGCHAIN4J MEMORY ARCHITECTURE                         |
+-------------------------------------------------------------------------------+
|                                                                               |
|                            [ ChatMemory API ]                                 |
|                                     |                                         |
|                  +------------------+------------------+                      |
|                  |                                     |                      |
|                  v                                     v                      |
|      [ MessageWindowChatMemory ]           [ TokenWindowChatMemory ]          |
|      Retains last N messages;              Retains last K tokens using        |
|      evicts by count (e.g. 10 turns)       tokenizer (e.g. 4,000 tokens)      |
|                  |                                     |                      |
|                  +------------------+------------------+                      |
|                                     |                                         |
|                                     v                                         |
|                           [ ChatMemoryStore SPI ]                             |
|                        (PostgreSQL, Redis, In-Memory)                         |
+-------------------------------------------------------------------------------+
```

### 1. `MessageWindowChatMemory` vs. `TokenWindowChatMemory`
- **`MessageWindowChatMemory`**: Prunes based on message count (e.g., `maxMessages(10)`). Simple and predictable for chat apps with short conversational turns.
- **`TokenWindowChatMemory`**: Prunes based on total token budget (e.g., `maxTokens(4000, tokenizer)`). Essential for technical support or code assistants where a single user turn might contain 10,000 tokens of pasted stack traces.

```java
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiTokenizer;

ChatMemory memory = TokenWindowChatMemory.builder()
    .id("session-102")
    .maxTokens(4000, new OpenAiTokenizer("gpt-4o"))
    .build();
```

### 2. The Golden Invariant: Never Evict the System Message!
A severe bug in naive sliding window implementations is dropping the oldest message when memory overflows. If the oldest message is your `@SystemMessage`, **evicting it strips away all persona instructions, guardrails, and security policies!**

```
+-------------------------------------------------------------------------------+
|                       SYSTEM PROMPT RETENTION INVARIANT                       |
+-------------------------------------------------------------------------------+
|                                                                               |
|  Initial State (Window size: 3):                                              |
|  [0] SYSTEM: "You are a healthcare advisor. Never give medication doses."    |
|  [1] USER:   "I have a headache."                                             |
|  [2] AI:     "Drink water and rest."                                          |
|                                                                               |
|  User adds Turn 3: "Can I take 800mg Ibuprofen?"                              |
|                                                                               |
|  NAIVE PRUNING (WRONG):             LANGCHAIN4J PRUNING (CORRECT):            |
|  ❌ Evicts [0] SYSTEM               ✅ PINS [0] SYSTEM (NEVER DROPPED)        |
|  [1] USER: "I have a headache."     [0] SYSTEM: "You are a healthcare..."     |
|  [2] AI:   "Drink water..."         [2] AI:     "Drink water..."              |
|  [3] USER: "Can I take 800mg..."    [3] USER:   "Can I take 800mg..."         |
|  Model forgets medical guardrail!   Medical guardrail stays 100% active!      |
+-------------------------------------------------------------------------------+
```

LangChain4j guarantees that if a `SystemMessage` is present at index 0, it is **never pruned**; only older dialogue turns are discarded.

### 3. Multi-Tenant Per-User Isolation with `@MemoryId`
In web microservices, hundreds of users chat concurrently. Sharing a single `ChatMemory` instance causes critical security leaks where User B sees User A's private banking or health data.

LangChain4j solves this using `@MemoryId` and `ChatMemoryProvider`:

```java
package com.genai.langchain4j.memory;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public class MultiTenantConcierge {

    public interface ConciergeService {
        @SystemMessage("You are an enterprise concierge. Respect user privacy.")
        String chat(@MemoryId String userId, @UserMessage String message);
    }

    public static ConciergeService create(ChatLanguageModel model) {
        return AiServices.builder(ConciergeService.class)
            .chatLanguageModel(model)
            // Dynamically provisions an isolated memory window per distinct userId!
            .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(10)
                .build()
            )
            .build();
    }
}
```

Now, when Alice chats with `chat("user-101", ...)`, her memory is stored strictly under key `user-101`. When Bob calls `chat("user-202", ...)`, he accesses an isolated memory sandbox with zero visibility into Alice's session.

---

## 4. Prerequisite & Supporting Concepts

### Prerequisite / Supporting Concept: The `ChatMemoryStore` SPI
By default, LangChain4j stores chat histories in JVM memory. If your Spring Boot pod restarts or a Kubernetes autoscaler spins up a new pod, user conversations are lost.

The `ChatMemoryStore` SPI provides pluggable database persistence:

```java
package dev.langchain4j.store.memory.chat;

import dev.langchain4j.data.message.ChatMessage;
import java.util.List;

public interface ChatMemoryStore {
    List<ChatMessage> getMessages(Object memoryId);
    void updateMessages(Object memoryId, List<ChatMessage> messages);
    void deleteMessages(Object memoryId);
}
```

---

## 5. Advanced Depth (Intermediate → Advanced)

### Production PostgreSQL `ChatMemoryStore` Implementation
Here is how to persist conversational turns directly into PostgreSQL using `jsonb` and LangChain4j's built-in message serializers:

```java
package com.genai.langchain4j.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PostgresChatMemoryStore implements ChatMemoryStore {

    private final JdbcTemplate jdbcTemplate;

    public PostgresChatMemoryStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String sql = "SELECT messages_json FROM chat_memory WHERE memory_id = ?";
        List<String> rows = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("messages_json"), memoryId.toString());

        if (rows.isEmpty()) {
            return List.of();
        }

        // LangChain4j provides built-in JSON deserializers for ChatMessage!
        return ChatMessageDeserializer.messagesFromJson(rows.get(0));
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String json = ChatMessageSerializer.messagesToJson(messages);
        String sql = """
            INSERT INTO chat_memory (memory_id, messages_json, updated_at)
            VALUES (?, ?::jsonb, NOW())
            ON CONFLICT (memory_id) DO UPDATE
            SET messages_json = EXCLUDED.messages_json, updated_at = NOW()
            """;
        jdbcTemplate.update(sql, memoryId.toString(), json);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        String sql = "DELETE FROM chat_memory WHERE memory_id = ?";
        jdbcTemplate.update(sql, memoryId.toString());
    }
}
```

### Automated Session Expiration (TTL Memory Store)
In production customer-facing applications, conversations should expire after an inactivity threshold (e.g., 30 minutes of idle time):

```java
package com.genai.langchain4j.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class TtlChatMemoryStore implements ChatMemoryStore {

    private final ChatMemoryStore delegate;
    private final Duration sessionTtl;
    private final Map<Object, Instant> lastAccessMap = new HashMap<>();

    public TtlChatMemoryStore(ChatMemoryStore delegate, Duration sessionTtl) {
        this.delegate = delegate;
        this.sessionTtl = sessionTtl;
    }

    @Override
    public synchronized List<ChatMessage> getMessages(Object memoryId) {
        Instant lastAccess = lastAccessMap.get(memoryId);
        if (lastAccess != null && Duration.between(lastAccess, Instant.now()).compareTo(sessionTtl) > 0) {
            deleteMessages(memoryId);
            return List.of();
        }
        lastAccessMap.put(memoryId, Instant.now());
        return delegate.getMessages(memoryId);
    }

    @Override
    public synchronized void updateMessages(Object memoryId, List<ChatMessage> messages) {
        lastAccessMap.put(memoryId, Instant.now());
        delegate.updateMessages(memoryId, messages);
    }

    @Override
    public synchronized void deleteMessages(Object memoryId) {
        lastAccessMap.remove(memoryId);
        delegate.deleteMessages(memoryId);
    }
}
```

### Common Anti-Patterns & Production Traps

| Anti-Pattern | Why It Breaks in Production | Correct Architectural Solution |
|:---|:---|:---|
| **Sharing a Single `ChatMemory` Singleton** | Causes conversation cross-talk and data leaks across users in multi-user web services. | Always use `@MemoryId` on interface methods paired with `ChatMemoryProvider`. |
| **Using `MessageWindowChatMemory` for Code Reviews** | Users pasting 2,000-line stack traces or JSON payloads will exhaust the model's token context window. | Use `TokenWindowChatMemory` calibrated with `OpenAiTokenizer` to enforce a strict token ceiling. |
| **Storing Memory Exclusively in Heap RAM** | Kubernetes pod restarts or blue-green deployments wipe all user conversation histories. | Implement `ChatMemoryStore` backed by PostgreSQL `jsonb` or Redis. |

---

## 6. Quick Recap
- LLMs are **stateless by default**; conversational memory is simulated by prepending past turns into subsequent prompt requests.
- **Unbounded chat history** leads to context window overflow, exploding token costs, and high latency.
- **`MessageWindowChatMemory`** retains the last $N$ turns; **`TokenWindowChatMemory`** enforces a strict tokenizer-calculated token ceiling.
- The **System Prompt Invariant** ensures that foundational instructions and security boundaries are never evicted during memory pruning.
- Use **`@MemoryId`** and **`ChatMemoryProvider`** to isolate conversation state per user or session in multi-tenant systems.
- Use the **`ChatMemoryStore`** SPI to persist dialogue turns durably in PostgreSQL or Redis.

---

## 7. Self-Check Questions & Practice Exercises

### 5-Question Self-Check Quiz

#### Question 1
Why is an unbounded conversation history dangerous in production LLM applications?
- A) Large Language Models automatically crash if they receive more than 10 messages.
- B) It causes context window overflow errors, drives token costs exponentially higher with every turn, and introduces severe latency spikes.
- C) Relational databases cannot store more than 100 strings.
- D) It violates the HTTP/1.1 protocol specification.

#### Question 2
In `MessageWindowChatMemory`, what happens to the leading `SystemMessage` when the window overflows?
- A) It is evicted first because it is the oldest message.
- B) It is retained permanently; the window evicts the oldest non-system dialogue turns to preserve agent instructions and guardrails.
- C) It throws a `BufferOverflowException`.
- D) It is overwritten by the latest user message.

#### Question 3
How does `AiServices` isolate chat history for different concurrent users?
- A) By spawning a separate JVM process for each user.
- B) Using the `@MemoryId` annotation on an interface method parameter combined with a `ChatMemoryProvider` that provisions dedicated memory instances per ID.
- C) By prefixing all user names to the system prompt string.
- D) LangChain4j can only handle one user at a time.

#### Question 4
When should you prefer `TokenWindowChatMemory` over `MessageWindowChatMemory`?
- A) Always, because message counts are deprecated.
- B) When users submit large, highly variable payloads (e.g., code snippets, CSV files, log dumps) where counting turns does not protect against token budget exhaustion.
- C) Only when running on mobile Android devices.
- D) When using local Ollama models instead of OpenAI.

#### Question 5
What is the purpose of the `ChatMemoryStore` interface in LangChain4j?
- A) To cache generated embedding vectors in memory.
- B) To provide a pluggable persistence SPI for saving and restoring conversational turns to external databases (PostgreSQL, Redis, MongoDB) across application restarts.
- C) To serialize Java bytecode into native assemblies.
- D) To charge the user's credit card for API usage.

---

### Quiz Answers & Explanations
1. **B**: Every turn in an unbounded history retransmits all past tokens, leading to context length violations ($128\text{k}$ tokens), massive token bills, and degrading response times.
2. **B**: LangChain4j guarantees that the leading `SystemMessage` is never discarded, ensuring that core system guardrails and persona constraints remain active regardless of conversation length.
3. **B**: Tagging a user ID parameter with `@MemoryId` instructs `AiServices` to look up or construct an isolated `ChatMemory` for that specific identifier via `ChatMemoryProvider`.
4. **B**: A single user turn can contain 15,000 tokens of error logs. A count-based window cannot detect this, whereas `TokenWindowChatMemory` measures exact token weight and enforces your token budget.
5. **B**: `ChatMemoryStore` is the storage SPI decoupling memory window logic from the database layer, allowing chat histories to persist in PostgreSQL, Redis, or DynamoDB across server restarts and cluster scaling.

---

### Hands-On Practice Exercises

#### Exercise 1: Conversation Export to Markdown Auditor
**Problem Statement**:  
Write a utility method `String exportTranscriptToMarkdown(ChatMemory memory)` that formats all messages in a `ChatMemory` into an executive markdown audit log with roles and message counts.

<details>
<summary>👉 View Solution</summary>

```java
package com.genai.langchain4j.exercises;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;

public class TranscriptAuditExporter {

    public static String exportTranscriptToMarkdown(ChatMemory memory) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Conversational Audit Log (Session: ").append(memory.id()).append(")\n\n");
        sb.append("| Turn | Role | Content |\n");
        sb.append("| :--- | :--- | :--- |\n");

        int turn = 1;
        for (ChatMessage msg : memory.messages()) {
            sb.append(String.format("| %d | **%s** | %s |\n", turn++, msg.type(), msg.text().replace("\n", " ")));
        }
        return sb.toString();
    }
}
```
</details>

#### Exercise 2: PII Scrubbing Memory Interceptor
**Problem Statement**:  
Build a utility method `ChatMessage sanitize(ChatMessage message)` that scrubs credit card numbers (matching 13 to 16 digits) from user messages before they are added to `ChatMemory`.

<details>
<summary>👉 View Solution</summary>

```java
package com.genai.langchain4j.exercises;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.UserMessage;

public class PiiScrubbingMemoryFilter {

    private static final String CC_REGEX = "\\b(?:\\d[ -]*?){13,16}\\b";

    public static ChatMessage sanitize(ChatMessage original) {
        if (original.type() != ChatMessageType.USER) {
            return original;
        }

        String scrubbed = original.text().replaceAll(CC_REGEX, "[REDACTED_CREDIT_CARD]");
        return UserMessage.from(scrubbed);
    }
}
```
</details>

---

[← Previous: Day 43 - LangChain4j Introduction](../Day_43_LangChain4j_Introduction_AiServices/Day_43_LangChain4j_Introduction_AiServices.md) | [Next: Day 45 - Structured Extraction & Guardrails →](../Day_45_Structured_Extraction_Guardrails/Day_45_Structured_Extraction_Guardrails.md)
