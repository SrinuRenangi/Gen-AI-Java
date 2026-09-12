# 🚀 Day 05: Modern Java — Records, Optional & Text Blocks
## Java 21 Superpowers That Make Generative AI Code Clean, Safe, and NPE-Proof

| ⬅️ Previous Day | 📚 Course Hub | ➡️ Next Day |
|:---|:---:|---:|
| [← Day 04: Generics, Collections & Data Structures](../Day_04_Generics_Collections_DataStructures/Day_04_Generics_Collections_DataStructures.md) | [All 60 Days Overview](../../README.md) | [Day 06: Functional Programming & Stream API →](../Day_06_Functional_Programming_Streams/Day_06_Functional_Programming_Streams.md) |

[![Phase](https://img.shields.io/badge/Phase_01-Java_Foundations-brightgreen.svg?style=for-the-badge)](../../README.md)
[![Day](https://img.shields.io/badge/Day-05_of_60-blue.svg?style=for-the-badge)](../../README.md)
[![Difficulty](https://img.shields.io/badge/Difficulty-Intermediate-blue.svg?style=for-the-badge)](../../README.md)
[![Java Feature](https://img.shields.io/badge/Java_21-Records_%26_Optional-purple.svg?style=for-the-badge)](../../README.md)

---

![Modern Java 21 Records vs Traditional JavaBeans and Optional](assets/day05_records_optional.jpg)

## 🗺️ Table of Contents
- [1. Topic Overview](#1-topic-overview)
- [2. Basic Foundations (True Zero)](#2-basic-foundations-true-zero)
  - [2.1 What is a Record, an Optional, and a Text Block?](#21-what-is-a-record-an-optional-and-a-text-block)
  - [2.2 The Sealed Passport vs. Editable Notebook Analogy](#22-the-sealed-passport-vs-editable-notebook-analogy)
  - [2.3 Minimal Working Example: Modern AI Prompt & Response](#23-minimal-working-example-modern-ai-prompt--response)
  - [2.4 Line-by-Line Code Breakdown](#24-line-by-line-code-breakdown)
- [3. Core Concept Walkthrough (Basic → Intermediate)](#3-core-concept-walkthrough-basic--intermediate)
  - [3.1 Java Records: The Death of Boilerplate](#31-java-records-the-death-of-boilerplate)
  - [3.2 Compact Constructors & Validation](#32-compact-constructors--validation)
  - [3.3 Spring AI's Secret Weapon: Structured Outputs](#33-spring-ais-secret-weapon-structured-outputs)
  - [3.4 `Optional<T>`: Idiomatic Handling of Missing Values](#34-optionalt-idiomatic-handling-of-missing-values)
  - [3.5 Text Blocks (`"""`): Clean Multi-Line Prompt Engineering](#35-text-blocks--clean-multi-line-prompt-engineering)
  - [3.6 Java 21 Pattern Matching with `switch`](#36-java-21-pattern-matching-with-switch)
  - [3.7 Sequenced Collections (Java 21)](#37-sequenced-collections-java-21)
- [4. Prerequisite & Supporting Concepts](#4-prerequisite--supporting-concepts)
  - [Prerequisite / Supporting Concept: The Billion-Dollar Mistake (NullPointerException)](#prerequisite--supporting-concept-the-billion-dollar-mistake-nullpointerexception)
  - [Prerequisite / Supporting Concept: DTOs (Data Transfer Objects) and Immutability](#prerequisite--supporting-concept-dtos-data-transfer-objects-and-immutability)
  - [Prerequisite / Supporting Concept: String Concatenation vs. Multi-Line Text Blocks](#prerequisite--supporting-concept-string-concatenation-vs-multi-line-text-blocks)
- [5. Advanced Depth (Intermediate → Advanced)](#5-advanced-depth-intermediate--advanced)
  - [5.1 Senior Deep Dive: `Optional.of` vs. `Optional.ofNullable`](#51-senior-deep-dive-optionalof-vs-optionalofnullable)
  - [5.2 Why `Optional` Should NEVER Be a Class Field or Parameter](#52-why-optional-should-never-be-a-class-field-or-parameter)
  - [5.3 Transforming Optionals: `map()` vs. `flatMap()`](#53-transforming-optionals-map-vs-flatmap)
  - [5.4 Common Mistakes & Misconceptions (With Bad vs. Good Code)](#54-common-mistakes--misconceptions-with-bad-vs-good-code)
  - [5.5 Architectural Trade-Offs: Record Immutability vs. Mutable JavaBeans](#55-architectural-trade-offs-record-immutability-vs-mutable-javabeans)
- [6. Quick Recap](#6-quick-recap)
- [7. Self-Check Questions & Practice Exercises](#7-self-check-questions--practice-exercises)
  - [Self-Check Questions (Basic to Advanced)](#self-check-questions-basic-to-advanced)
  - [Hands-On Practice Exercises with Full Solutions](#hands-on-practice-exercises-with-full-solutions)

---

# 1. Topic Overview

Modern Java (Java 16 through Java 21 LTS) introduces powerful language capabilities designed to eliminate boilerplate, guarantee data immutability, and banish `NullPointerException`s. **Java Records** provide single-line immutable data carriers; **`Optional<T>`** enforces explicit compile-time handling of potentially absent values; and **Text Blocks (`"""`)** provide native multi-line string authoring.

### Why This Topic Matters
Modern Generative AI pipelines heavily rely on exchanging structured data: prompts, JSON payloads, model hyperparameters, token consumption counts, and optional metadata fields. Traditional Java required 50+ lines of verbose getters, setters, constructors, and null checks for every data container. In Java 21 and Spring AI, records act as native schema-aware DTOs for LLM structured outputs, while `Optional` and text blocks make prompt templates elegant and immune to runtime crashes.

> 💡 **New Word Alert — "Record"**: A concise, immutable class declaration in Java that automatically generates private final fields, a canonical constructor, accessors, `equals()`, `hashCode()`, and `toString()`.

> 💡 **New Word Alert — "DTO (Data Transfer Object)"**: A lightweight object used purely to carry data across boundaries (e.g., from an AI API response to your web controller) without containing business logic.

> 💡 **New Word Alert — "Text Block (`"""`)"**: A multi-line string literal enclosed in triple double-quotes that avoids the need for manual escape sequences (`\n`, `\"`) and string concatenations.

---

# 2. Basic Foundations (True Zero)

Let's start from absolute true zero, understanding why these modern Java features exist.

### 2.1 What is a Record, an Optional, and a Text Block?

- **Java Record**: A quick way to define an unchangeable data container in a single line: `public record User(String name, int age) {}`.
- **`Optional<T>`**: A protective box that either holds a value or is empty, forcing you to handle the empty case so your code never crashes on `null`.
- **Text Block (`"""`)**: A string format that lets you paste paragraphs or JSON directly across multiple lines without messy `+` signs or `\n`.

---

### 2.2 The Sealed Passport vs. Editable Notebook Analogy

```
        THE TRADITIONAL JAVABEAN (Editable Notebook)
        ┌─────────────────────────────────────────────────────────────┐
        │ Anyone with a pencil can erase the name, change the photo,  │
        │ or change the birthday at any time. It is mutable & fragile.│
        └─────────────────────────────────────────────────────────────┘
                                      vs.
        THE JAVA RECORD (Government Sealed Passport)
        ┌─────────────────────────────────────────────────────────────┐
        │ Printed and laminated at birth. Unalterable. Guaranteed by   │
        │ the system to contain genuine, tamper-proof identity data.  │
        └─────────────────────────────────────────────────────────────┘
```

When an AI model extracts an invoice, calculates an embedding vector, or processes a payment, you do not want another thread accidentally modifying that data. A **Record** is a tamper-proof sealed passport.

---

### 2.3 Minimal Working Example: Modern AI Prompt & Response

Let's write a minimal, fully runnable Java program utilizing Records, Text Blocks, and Optionals:

```java
import java.util.Optional;

public class ModernJavaDemo {

    // 1. A Record: single-line immutable data carrier
    public record AIResult(String text, int tokensUsed) {}

    // 2. A method returning an Optional
    public static Optional<AIResult> executePrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return Optional.empty(); // Safely return empty if input invalid
        }
        return Optional.of(new AIResult("Response to: " + prompt, 42));
    }

    public static void main(String[] args) {
        // 3. A multi-line Text Block
        String prompt = """
            Explain Java 21 Records
            and Optional in plain English.
            """;

        // 4. Safe consumption without null checks
        executePrompt(prompt).ifPresentOrElse(
            result -> System.out.printf("Output: %s (Tokens: %d)%n", result.text(), result.tokensUsed()),
            () -> System.out.println("Prompt was empty!")
        );
    }
}
```

---

### 2.4 Line-by-Line Code Breakdown

1. `public record AIResult(String text, int tokensUsed) {}`:
   - Declares an immutable record with components `text` and `tokensUsed`.
   - Accessors are `result.text()` and `result.tokensUsed()` (no `get` prefix).
2. `public static Optional<AIResult> executePrompt(...)`:
   - Returns `Optional<AIResult>` to explicitly notify callers that a valid result is not guaranteed.
3. `return Optional.empty();`: Returns an empty container when invalid, avoiding dangerous `null` returns.
4. `String prompt = """ ... """;`: Multi-line text block preserving formatting and indentation naturally.
5. `executePrompt(prompt).ifPresentOrElse(...)`: Functional branch handling both present and absent outcomes safely.

---

# 3. Core Concept Walkthrough (Basic → Intermediate)

Now let's examine how these tools assemble modern enterprise AI applications.

### 3.1 Java Records: The Death of Boilerplate

In legacy Java, a simple data carrier with 2 fields required over 50 lines of boilerplate:
```java
// LEGACY JAVA (Over 50 lines of boilerplate!)
public final class TokenUsage {
    private final int promptTokens;
    private final int completionTokens;

    public TokenUsage(int promptTokens, int completionTokens) {
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
    }

    public int getPromptTokens() { return promptTokens; }
    public int getCompletionTokens() { return completionTokens; }

    @Override public boolean equals(Object o) { ... }
    @Override public int hashCode() { ... }
    @Override public String toString() { ... }
}
```

In **Modern Java 21**, it is written in a single line:

```java
public record TokenUsage(int promptTokens, int completionTokens) {
    public int totalTokens() {
        return promptTokens + completionTokens;
    }
}
```

The compiler automatically provides:
- `private final` fields for all components.
- A canonical constructor: `new TokenUsage(promptTokens, completionTokens)`.
- Public accessors: `usage.promptTokens()` and `usage.completionTokens()`.
- Consistent `equals()` and `hashCode()` implementations.
- Clear `toString()` representation: `TokenUsage[promptTokens=150, completionTokens=35]`.

---

### 3.2 Compact Constructors & Validation

When data arrives from an external LLM, you need domain validation. Records provide **Compact Constructors**:

```java
package com.javagenai.day05;

public record PromptConfig(String model, double temperature, int maxTokens) {

    // Compact constructor (no parameter list needed!)
    public PromptConfig {
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("Model name cannot be empty");
        }
        if (temperature < 0.0 || temperature > 2.0) {
            throw new IllegalArgumentException("Temperature must be between 0.0 and 2.0. Received: " + temperature);
        }
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("maxTokens must be > 0. Received: " + maxTokens);
        }
        // Components are automatically assigned to fields at the end of the block!
    }
}
```

---

### 3.3 Spring AI's Secret Weapon: Structured Outputs

When calling an LLM, you often need structured JSON parsed into Java objects rather than unformatted chat text:

```
LLM Output (JSON) ──► Spring AI (BeanOutputConverter) ──► Java Record
```

In Spring AI (Phase 6), you define output schemas using simple Records:

```java
public record MovieRecommendation(
    String title,
    int releaseYear,
    String director,
    double imdbRating,
    List<String> genres,
    String rationale
) {}
```
Spring AI inspects the record via reflection, builds a JSON Schema for the LLM prompt, and deserializes the model's response into the record automatically.

---

### 3.4 `Optional<T>`: Idiomatic Handling of Missing Values

An `Optional<T>` is a container that is either present or empty:

```
                ┌───────────────────────────────────┐
                │          Optional<String>         │
                ├─────────────────┬─────────────────┤
                │  Present (Some) │  Empty (None)   │
                │  ┌───────────┐  │                 │
                │  │ "OpenAI"  │  │    [ EMPTY ]    │
                │  └───────────┘  │                 │
                └─────────────────┴─────────────────┘
```

#### Modern Functional Optional Methods:
```java
package com.javagenai.day05;

import java.util.Optional;

public class OptionalUsageDemo {

    public static Optional<String> findApiKey() {
        return Optional.ofNullable(System.getenv("OPENAI_API_KEY"));
    }

    public static void main(String[] args) {
        // 1. orElse: Fallback value if absent
        String key = findApiKey().orElse("default-mock-key");

        // 2. map & filter: Safely transform and filter without null checks
        Optional<String> maskedKey = findApiKey()
            .filter(k -> k.startsWith("sk-"))
            .map(k -> k.substring(0, 7) + "..." + k.substring(k.length() - 4));

        System.out.println("Masked Key: " + maskedKey.orElse("[NO VALID KEY]"));

        // 3. ifPresentOrElse: Clean branching
        findApiKey().ifPresentOrElse(
            k -> System.out.println("API key active!"),
            () -> System.err.println("Warning: No API key found.")
        );
    }
}
```

---

### 3.5 Text Blocks (`"""`): Clean Multi-Line Prompt Engineering

Modern Java text blocks eliminate messy string concatenation and manual `\n` escaping:

```java
String systemPrompt = """
    You are an expert enterprise Java AI architect.
    Follow these strict guidelines:
      1. Always respond in valid JSON matching the requested schema.
      2. Never include conversational filler like "Here is your output:".
      3. Target Java 21 LTS syntax (Records, Pattern Matching).
    """;
```

#### String Interpolation with `.formatted(...)`:
```java
String template = """
    Translate the following text into %s.
    Context: %s
    Text: "%s"
    """;

String prompt = template.formatted("Spanish", "Medical", "Patient has fever.");
```

---

### 3.6 Java 21 Pattern Matching with `switch`

Java 21 allows `switch` to evaluate complex object types and guarded conditions directly:

```java
package com.javagenai.day05;

public class PromptDispatcher {

    public sealed interface AIRequest permits TextPrompt, ImagePrompt, ToolResponsePrompt {}
    public record TextPrompt(String userText) implements AIRequest {}
    public record ImagePrompt(String caption, byte[] imageData) implements AIRequest {}
    public record ToolResponsePrompt(String toolName, String resultJson) implements AIRequest {}

    public static String routeRequest(AIRequest request) {
        return switch (request) {
            case TextPrompt t -> 
                "Dispatching text prompt to LLM: " + t.userText();
            case ImagePrompt img when img.imageData().length > 1_000_000 -> 
                "Compressing large image (" + img.caption() + ") before vision inference";
            case ImagePrompt img -> 
                "Sending standard image (" + img.caption() + ") to GPT-4o Vision";
            case ToolResponsePrompt tool -> 
                "Feeding tool output [" + tool.toolName() + "] back to LLM";
        };
    }
}
```

---

### 3.7 Sequenced Collections (Java 21)

Java 21 introduced uniform methods for ordered collections:
- `getFirst()` / `getLast()`
- `addFirst()` / `addLast()`
- `reversed()`

```java
List<String> conversation = new ArrayList<>(List.of("User: Hi", "Bot: Hello!", "User: Help."));

System.out.println("First Turn: " + conversation.getFirst()); // "User: Hi"
System.out.println("Last Turn : " + conversation.getLast());  // "User: Help."

List<String> newestFirst = conversation.reversed();
```

---

# 4. Prerequisite & Supporting Concepts

### Prerequisite / Supporting Concept: The Billion-Dollar Mistake (NullPointerException)

Invented by Sir Tony Hoare in 1965, the `null` pointer reference was introduced simply because it was easy to implement. However, unhandled `null` references result in `NullPointerException` (NPE) crashes in production. Modern Java uses `Optional<T>` to force callers to explicitly address absence at compile time.

---

### Prerequisite / Supporting Concept: DTOs (Data Transfer Objects) and Immutability

A **DTO** is an object designed solely to transport data between application layers or over the network.
- **Mutable DTOs**: Risky because any method receiving the object can tamper with its fields.
- **Immutable Records**: Thread-safe by design, free from race conditions, and completely reliable across concurrent pipelines.

---

### Prerequisite / Supporting Concept: String Concatenation vs. Multi-Line Text Blocks

- Prior to text blocks, multi-line strings required `+` concatenation and `\n` characters, which created visual clutter and made formatting prompts prone to escaping mistakes.
- Text blocks strip incidental indentation automatically based on the position of the closing `"""`, preserving exact layout cleanly.

---

# 5. Advanced Depth (Intermediate → Advanced)

### 5.1 Senior Deep Dive: `Optional.of` vs. `Optional.ofNullable`

- **`Optional.of(value)`**: Requires that `value` is strictly non-null. Passing `null` immediately throws a `NullPointerException`.
- **`Optional.ofNullable(value)`**: Safe factory. If `value` is non-null, returns `Optional.of(value)`; if `null`, returns `Optional.empty()`.

```java
String key = null;
Optional.of(key);         // 💥 Throws NullPointerException!
Optional.ofNullable(key); // ✅ Returns Optional.empty() safely.
```

---

### 5.2 Why `Optional` Should NEVER Be a Class Field or Parameter

Interviewers frequently test this architectural constraint:
1. **Not Serializable**: `Optional` does not implement `java.io.Serializable`. Using it for fields breaks serialization frameworks (Jackson, JPA entities, Redis caches).
2. **Memory Footprint**: `Optional` is an extra object allocation on the Heap containing a reference pointer, doubling GC overhead for fields.
3. **Clumsy API Signatures**: Forcing callers to pass `Optional.of(val)` as method arguments degrades developer experience. Use method overloading instead.

---

### 5.3 Transforming Optionals: `map()` vs. `flatMap()`

- **`map()`**: Used when the mapping function returns a standard object. `map()` wraps the result in an `Optional`.
- **`flatMap()`**: Used when the mapping function **already returns an `Optional`**. Prevents nested `Optional<Optional<T>>` structures:

```java
record User(String name, Optional<String> email) {}

Optional<User> user = Optional.of(new User("Alice", Optional.of("alice@corp.com")));

// Using map(): Results in Optional<Optional<String>> (Nested!)
Optional<Optional<String>> nested = user.map(User::email);

// Using flatMap(): Results in Optional<String> (Cleanly flattened!)
Optional<String> clean = user.flatMap(User::email);
```

---

### 5.4 Common Mistakes & Misconceptions (With Bad vs. Good Code)

#### Mistake 1: Calling `optional.get()` Directly
**Bad Code:**
```java
// ❌ Throws NoSuchElementException if empty — exact same crash risk as NPE!
String model = config.getRecommendedModel().get();
```
**Correct Code:**
```java
// ✅ Safe fallback with orElse or orElseGet
String model = config.getRecommendedModel().orElse("gpt-4o-mini");
```

#### Mistake 2: Using `Optional` for Class Fields
**Bad Code:**
```java
public class UserProfile {
    private Optional<String> bio; // ❌ Breaks serialization and adds memory overhead!
}
```
**Correct Code:**
```java
public class UserProfile {
    private String bio; // Can be null internally

    public Optional<String> getBio() { // ✅ Return Optional from accessor!
        return Optional.ofNullable(this.bio);
    }
}
```

---

### 5.5 Architectural Trade-Offs: Record Immutability vs. Mutable JavaBeans

| Dimension | Java Records (Modern) | Traditional JavaBeans |
| :--- | :--- | :--- |
| **Boilerplate** | 1 line | 40–80 lines |
| **Immutability** | 100% Guaranteed (`final` fields) | Requires manual defensive coding |
| **Thread Safety** | Inherently thread-safe | Requires synchronization or locks |
| **Framework Support**| First-class in Spring Boot 3 & Jackson | Legacy standard |
| **Best For** | DTOs, API responses, Value Objects | JPA entities requiring lazy-loading |

---

# 6. Quick Recap

| Feature | Syntax | Best Use Case |
| :--- | :--- | :--- |
| **Record** | `record Token(int count) {}` | Immutable DTOs, AI response carriers. |
| **Compact Constructor**| `public RecordName { validate(); }`| Parameter domain validation for records. |
| **`Optional<T>`** | `Optional.ofNullable(val)` | Safe method return types; eliminates NPEs. |
| **Text Blocks** | `""" multi-line text """` | Multi-line AI system prompts and JSON schemas. |
| **Guarded Switch** | `case ImagePrompt img when cond ->`| Pattern-matching dispatch on domain events. |
| **Sequenced Collections**| `list.getFirst()`, `list.getLast()`| Quick access to conversation boundary messages. |

---

# 7. Self-Check Questions & Practice Exercises

### Self-Check Questions (Basic to Advanced)

1. **What is a Java Record and what components does the compiler generate automatically?**
   - *Answer*: An immutable data carrier class. The compiler generates `private final` fields, canonical constructor, component accessors (without `get`), `equals()`, `hashCode()`, and `toString()`.
2. **Why is calling `optional.get()` directly considered an anti-pattern?**
   - *Answer*: If the `Optional` is empty, calling `.get()` throws `NoSuchElementException`, recreating the exact same crash risk as a `NullPointerException`.
3. **Why should `Optional` never be used as a field inside a class?**
   - *Answer*: `Optional` is not `Serializable` (breaking caching and JSON serialization) and introduces redundant object allocation overhead on the Heap.
4. **How do Text Blocks (`"""`) improve prompt engineering in Java?**
   - *Answer*: They enable multi-line string authoring with preserved layout and embedded quotes without messy concatenation (`+`) or escaping (`\n`, `\"`).
5. **What is the difference between `Optional.map()` and `Optional.flatMap()`?**
   - *Answer*: `map()` automatically wraps the return value in an `Optional`. `flatMap()` expects the mapping function to return an `Optional` itself and flattens the result, preventing `Optional<Optional<T>>`.

---

### Hands-On Practice Exercises with Full Solutions

#### 🏋️ Exercise 1: Structured AI Invoice Extraction Record
**Objective**: Build a nested Record structure representing an invoice extracted by an LLM, including validation in compact constructors and defensive copying.

```java
package com.javagenai.day05;

import java.util.List;

public record LineItem(String description, int quantity, double unitPrice) {
    public LineItem {
        if (description == null || description.isBlank()) throw new IllegalArgumentException("Description required");
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be > 0");
        if (unitPrice < 0) throw new IllegalArgumentException("Price cannot be negative");
    }

    public double total() {
        return quantity * unitPrice;
    }
}
```

```java
package com.javagenai.day05;

import java.util.List;

public record ExtractedInvoice(String invoiceId, String vendorName, List<LineItem> items) {
    public ExtractedInvoice {
        if (invoiceId == null || invoiceId.isBlank()) throw new IllegalArgumentException("invoiceId required");
        if (vendorName == null || vendorName.isBlank()) throw new IllegalArgumentException("vendorName required");
        items = (items != null) ? List.copyOf(items) : List.of(); // Defensive unmodifiable copy!
    }

    public double grandTotal() {
        double sum = 0.0;
        for (LineItem item : items) {
            sum += item.total();
        }
        return sum;
    }
}
```

---

#### 🏋️ Exercise 2: Safe Document Metadata Extraction with `Optional`
**Objective**: Build `DocumentMetadataExtractor` to safely extract an author's email domain from a metadata map without any danger of `NullPointerException`.

```java
package com.javagenai.day05;

import java.util.Map;
import java.util.Optional;

public class DocumentMetadataExtractor {

    public static Optional<String> extractAuthorDomain(Map<String, String> metadata) {
        return Optional.ofNullable(metadata)
            .map(m -> m.get("author_email"))
            .filter(email -> email.contains("@"))
            .map(email -> email.substring(email.indexOf("@") + 1).toLowerCase().trim());
    }
}
```

---

<p align="center">
  <b>Day 05 Complete! 🎉</b><br>
  Proceed to <b>Day 06</b>: <b>Functional Programming & Stream API</b>.<br>
  <a href="../Day_06_Functional_Programming_Streams/Day_06_Functional_Programming_Streams.md"><b>Continue to Day 06 →</b></a>
</p>
