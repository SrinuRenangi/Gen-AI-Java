# Day 35: Structured Output — LLMs That Return Java Objects
## BeanOutputConverter, Java 21 Records as JSON Schemas, Markdown Sanitization & Self-Correcting Parsers

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [◀ Day 34: Prompt Engineering in Java](../Day_34_Prompt_Engineering_in_Java/Day_34_Prompt_Engineering_in_Java.md) | [All 60 Days Overview](../../README.md) | [Day 36: Streaming Responses — The Typewriter Effect ▶](../Day_36_Streaming_Responses/Day_36_Streaming_Responses.md) |

---

## What Will You Learn Today?

In traditional software systems, microservices communicate with strict, deterministic data protocols (JSON, Protobuf, Avro). But Large Language Models are probabilistic text generators—they output conversational prose, markdown code fences, and unpredictable formatting.

If you build an enterprise backend where an AI service returns raw strings, your application code will constantly crash with `JsonParseException` whenever the model writes: *"Sure! Here is the JSON you asked for: ```json { ... } ```"*.

Today, you will master **Structured Output** in Spring AI:
- Why naive JSON parsing fails in production and how Spring AI's `StructuredOutputConverter` hierarchy solves it.
- Using Java 21 **Records as JSON Schemas**: Automatically generating JSON schema definitions from your Java types via reflection.
- The `BeanOutputConverter<T>`, `MapOutputConverter`, and `ListOutputConverter`.
- Using the fluent `.call().entity(Class<T>)` and `.entity(new ParameterizedTypeReference<List<T>>() {})` API in `ChatClient`.
- Native model JSON Mode (OpenAI `response_format` and Ollama `format: "json"`) vs. prompt-injected schema constraints.
- Building a **Self-Correcting JSON Retry Loop**: Automatically feeding JSON syntax errors back to the LLM so it fixes its own hallucinations!

---

## Real-World Analogy: Customs Declaration Form vs. Casual Conversation

Imagine traveling internationally and landing at an airport customs checkpoint:

```
+---------------------------------------------------------------------------------------------------+
|                                  THE DATA INGESTION PARADIGM                                      |
|                                                                                                   |
|  SCENARIO 1: Casual Freeform Conversation (Raw String Output)                                     |
|  - The customs officer asks: "What are you bringing into the country?"                            |
|  - You reply: "Well, I visited my grandmother, bought some cheese in Paris, also three bottles of |
|    wine for about 40 euros each, and a lovely woolen scarf."                                      |
|  - The officer's computer CANNOT ingest this narrative sentence into its tax database!            |
|                                                                                                   |
|  SCENARIO 2: Standard Customs Declaration Form (Structured Output Schema)                         |
|  - The officer hands you a formal printed grid with predefined boxes:                             |
|    [Item Name]       [Quantity]    [Declared Value USD]                                           |
|    - French Cheese       1               $25.00                                                   |
|    - Red Wine            3              $120.00                                                   |
|  - The barcode scanner reads the exact rows directly into the database without guessing!          |
+---------------------------------------------------------------------------------------------------+
```

In Spring AI:
- Your Java 21 Record is the **Customs Declaration Form**.
- `BeanOutputConverter` gives the LLM the exact schema grid to fill out.
- Spring AI deserializes the output directly into a strongly-typed Java object without fragile manual parsing!

---

## 🧭 The Mid-Level Java Developer Bridge: Structured Output Demystified

If you've ever tried to parse JSON from an LLM by writing manual `String.indexOf("{")` and `substring()` calls, here is why Spring AI's `.entity(Class<T>)` is a game-changer:

| The Hard Way (Manual Parsing) | The Spring AI Way (`.entity(...)`) | Plain English Advantage |
| :--- | :--- | :--- |
| Asking the AI to "return JSON", then doing `objectMapper.readValue(rawText)`. | `chatClient.prompt().user(...).call().entity(MyRecord.class);` | One line of code. No regex, no string trimming! |
| App crashes because the AI added: *"Certainly! Here is the JSON: ```json"*. | Spring AI automatically strips conversational chatter and markdown code blocks. | Immune to LLM chatter and conversational preambles. |
| Writing 50 lines of prompt describing JSON field names and types. | Spring AI inspects your Java Record via reflection and auto-generates the JSON schema. | Your Java Record is the single source of truth for both your Java code and the AI! |
| If LLM omits a required field, you get a mysterious `NullPointerException`. | Pair with Bean Validation (`@NotNull`) to validate fields before passing to business logic. | Guarantees downstream code receives complete, valid domain objects. |

---

## The JSON Dilemma in Generative AI

Why do conventional JSON libraries like Jackson fail when used directly on LLM responses?

```
┌────────────────────────────────────────────────────────────────────────┐
│ WHAT THE DEVELOPER ASKS FOR:                                           │
│   "Return JSON with fields: name, age, and email."                     │
├────────────────────────────────────────────────────────────────────────┤
│ WHAT THE LLM ACTUALLY GENERATES:                                       │
│                                                                        │
│   Certainly! Here is the user profile you requested:                  │
│   ```json                                                              │
│   {                                                                    │
│     "name": "Alice Johnson",                                           │
│     "age": 32,                                                         │
│     "email": "alice@techcorp.com",                                     │
│   }                                                                    │
│   ```                                                                  │
│   Hope this helps! Let me know if you need anything else.              │
└────────────────────────────────────────────────────────────────────────┘
```

If you pass this response into `new ObjectMapper().readValue(rawText, User.class)`:
1. **Preamble Error**: Jackson encounters the letter `'C'` in *"Certainly!"* and immediately throws:  
   `com.fasterxml.jackson.core.JsonParseException: Unrecognized token 'Certainly'`.
2. **Markdown Error**: The backticks ` ```json ` violate RFC 8259 JSON syntax.
3. **Trailing Comma**: The comma after `"email"` violates strict JSON specifications.

### How Spring AI Solves This:
Spring AI solves this through a two-step pipeline:
1. **Schema Generation (`converter.getFormat()`)**: Injects strict schema rules into the prompt.
2. **Resilient Sanitization**: Strips markdown code blocks, locates matching braces `{ ... }`, and deserializes into your strongly-typed Java record.

---

## Spring AI Structured Output Converters

Spring AI provides three core converter implementations in `org.springframework.ai.chat.converter.*`:

```
                           STRUCTURED OUTPUT CONVERTERS
                           
                       ┌───────────────────────────────┐
                       │   StructuredOutputConverter   │
                       └───────────────┬───────────────┘
                                       │
         ┌─────────────────────────────┼─────────────────────────────┐
         ▼                             ▼                             ▼
┌──────────────────┐         ┌───────────────────┐         ┌────────────────────┐
│BeanOutputConverter│        │ MapOutputConverter│         │ListOutputConverter │
│   (Java Record)  │         │(Key-Value Dictionary)│      │  (Collection/Array)│
└──────────────────┘         └───────────────────┘         └────────────────────┘
```

### 1. `BeanOutputConverter<T>`
Extracts the JSON Schema from any Java class or record and deserializes the response:

```java
// 1. Define your domain record
public record SecurityAuditReport(
    String targetHost,
    int openPortsCount,
    List<String> detectedVulnerabilities,
    boolean requiresImmediatePatching,
    String remediationSummary
) {}

// 2. Instantiate converter
BeanOutputConverter<SecurityAuditReport> converter = 
    new BeanOutputConverter<>(SecurityAuditReport.class);

// 3. Inject format rules into prompt
String prompt = """
    Analyze the following network scan log:
    {scanLog}
    {format}
    """;

PromptTemplate template = new PromptTemplate(prompt);
Prompt finalPrompt = template.create(Map.of(
    "scanLog", rawScanOutput,
    "format", converter.getFormat()
));

// 4. Execute and convert
ChatResponse response = chatModel.call(finalPrompt);
SecurityAuditReport report = converter.convert(response.getResult().getOutput().getContent());

System.out.println("Vulnerabilities: " + report.detectedVulnerabilities());
```

---

## Using `ChatClient.entity()`: The Modern High-Level Approach

While using `BeanOutputConverter` directly is powerful, Spring AI's modern `ChatClient` makes it effortless by providing the `.entity(...)` method:

```java
package com.genai.springai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ai/structured")
public class StructuredOutputController {

    private final ChatClient chatClient;

    public StructuredOutputController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public record CandidateProfile(
        String fullName,
        int yearsOfExperience,
        List<String> primarySkills,
        String seniorityLevel
    ) {}

    // 1. Single Record Extraction
    @PostMapping("/extract-candidate")
    public CandidateProfile extractProfile(@RequestBody String resumeText) {
        return chatClient.prompt()
            .user(u -> u.text("Extract candidate details from resume:\n{resume}")
                .param("resume", resumeText))
            .call()
            .entity(CandidateProfile.class);
    }

    // 2. Generic List Extraction using ParameterizedTypeReference
    @PostMapping("/extract-skills")
    public List<String> extractSkillsList(@RequestBody String jobDescription) {
        return chatClient.prompt()
            .user(u -> u.text("Extract all required programming languages and frameworks:\n{jd}")
                .param("jd", jobDescription))
            .call()
            .entity(new ParameterizedTypeReference<List<String>>() {});
    }
}
```

Behind the scenes, `ChatClient`:
1. Constructs the JSON Schema for `CandidateProfile`.
2. Automatically appends formatting instructions to the prompt.
3. Invokes the model.
4. Strips markdown fences.
5. Jackson-deserializes the JSON into a new `CandidateProfile` record instance!

---

## Native Model JSON Mode vs. Prompted JSON Schema

Modern model providers support native JSON modes:

### 1. OpenAI Strict Structured Outputs:
OpenAI models (GPT-4o, GPT-4o-mini) support native structured outputs via JSON Schema:

```yaml
spring:
  ai:
    openai:
      chat:
        options:
          response-format:
            type: json_object
```

### 2. Ollama JSON Mode:
Ollama models (Llama 3.2, Mistral) can be forced to output pure JSON at the engine level:

```yaml
spring:
  ai:
    ollama:
      chat:
        options:
          format: json
```

When native JSON mode is enabled, the model's logits are constrained during sampling so that it is mathematically impossible for the model to output characters that violate JSON grammar!

---

## Resilient Self-Correcting JSON Retry Loop

What happens if a complex model call produces malformed JSON due to token truncation or hallucinated commas?

In enterprise Java, we implement a **Self-Correction Retry Loop**:

```
                              SELF-CORRECTING JSON RETRY PIPELINE
                              
 Prompt Sent to LLM
         │
         ▼
 LLM Returns Response
         │
         ▼
 Attempt Deserialization (Jackson / BeanOutputConverter)
         │
         ├── ✅ SUCCESS ──► Return Java Record to Caller
         │
         └── ❌ JsonParseException Caught!
               │
               ▼
 Build Feedback Prompt:
 "Your previous response was NOT valid JSON.
  Error: Unexpected character ',' at line 4 column 12.
  Fix the error and return only valid JSON matching this schema: ..."
               │
               ▼
 Retry Call (Attempt 2)
         │
         ├── ✅ SUCCESS ──► Return Java Record
         └── ❌ Failed again ──► Throw Clean Enterprise Exception
```

Here is how you implement this pattern in Java:

```java
package com.genai.springai.service;

import com.genai.springai.chatclient.ChatClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class ResilientStructuredService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResilientStructuredService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public <T> T callWithSelfCorrection(String userPrompt, Class<T> targetClass, int maxRetries) {
        String currentPrompt = userPrompt;
        String lastError = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            if (lastError != null) {
                currentPrompt = userPrompt + "\n\nCRITICAL FIX REQUIRED: Your previous attempt failed JSON parsing with error: [" 
                        + lastError + "]. Correct the syntax and return strictly valid JSON.";
            }

            try {
                return chatClient.prompt()
                    .user(currentPrompt)
                    .call()
                    .entity(targetClass);
            } catch (Exception ex) {
                lastError = ex.getMessage();
                System.err.println("Attempt " + attempt + " failed to parse JSON: " + lastError);
            }
        }

        throw new IllegalStateException("Failed to extract valid " + targetClass.getSimpleName() + " after " + maxRetries + " attempts.");
    }
}
```

---

## Step-by-Step Production Code Walkthrough

Let's examine the companion code written for today's lesson in `Phase_06_Spring_AI/Day_35_Structured_Output_Java_Objects/code/`:

### 1. `JsonSchemaGenerator.java`
Uses Java 21 reflection (`RecordComponent`) to generate standard JSON schemas dynamically:

```java
public static String generateSchema(Class<?> clazz) {
    StringBuilder sb = new StringBuilder();
    sb.append("{\n  \"type\": \"object\",\n  \"properties\": {\n");

    if (clazz.isRecord()) {
        RecordComponent[] components = clazz.getRecordComponents();
        for (int i = 0; i < components.length; i++) {
            RecordComponent rc = components[i];
            sb.append("    \"").append(rc.getName()).append("\": { \"type\": \"")
              .append(mapJavaTypeToJsonType(rc.getType())).append("\" }");
            if (i < components.length - 1) sb.append(",");
            sb.append("\n");
        }
    }
    // appends required fields...
    return sb.toString();
}
```

### 2. `MarkdownJsonSanitizer.java`
Cleans up code fences and finds matching braces:

```java
public static String clean(String rawLlmOutput) {
    String cleaned = rawLlmOutput.trim();
    if (cleaned.contains("```json")) {
        int start = cleaned.indexOf("```json") + 7;
        int end = cleaned.indexOf("```", start);
        cleaned = (end != -1) ? cleaned.substring(start, end) : cleaned.substring(start);
    }
    int firstBrace = cleaned.indexOf('{');
    int lastBrace = cleaned.lastIndexOf('}');
    if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
        cleaned = cleaned.substring(firstBrace, lastBrace + 1);
    }
    return cleaned.trim();
}
```

### 3. `BeanOutputConverter.java`
Binds schema formatting instructions to prompt generation and extracts fields into records:

```java
public String getFormat() {
    String schema = JsonSchemaGenerator.generateSchema(targetClass);
    return """
        Your response should be in JSON format.
        Do not include any explanations, markdown code fences, or text outside the JSON.
        Adhere strictly to this JSON Schema:
        %s
        """.formatted(schema);
}
```

### 4. Running the Complete Verification Suite
Compile and execute the demonstration:

```bash
javac -d out Phase_06_Spring_AI/Day_35_Structured_Output_Java_Objects/code/*.java
java -cp out com.genai.springai.structured.StructuredOutputDemo
```

Output:
```text
================================================================================
  DAY 35: STRUCTURED OUTPUT — CONVERTING LLM RESPONSES TO JAVA OBJECTS          
================================================================================

[TEST 1] Generating JSON Schema for Domain Record: FraudAssessment...
--- Generated Prompt Directives ---
Your response should be in JSON format.
Do not include any explanations, markdown code fences, or text outside the JSON.
Adhere strictly to this JSON Schema:
{
  "type": "object",
  "properties": {
    "isFraudulent": { "type": "boolean" },
    "riskScore": { "type": "number" },
    "anomalyFlags": { "type": "array" },
    "recommendedAction": { "type": "string" }
  },
  "required": ["isFraudulent", "riskScore", "anomalyFlags", "recommendedAction"]
}

[TEST 2] Parsing Messy Raw LLM Output with Markdown Code Blocks...
--- Raw LLM Text Received ---
Sure! Here is the JSON response you requested for the transaction:
```json
{
  "isFraudulent": true,
  "riskScore": 0.89,
  "anomalyFlags": ["IP_GEOLOCATION_MISMATCH", "UNUSUAL_MIDNIGHT_AMOUNT", "VELOCITY_SPIKE"],
  "recommendedAction": "BLOCK_TRANSACTION_AND_ALERT_CARDHOLDER"
}
```
Let me know if you need any additional compliance analysis!

  ✅ SUCCESSFULLY DESERIALIZED INTO JAVA RECORD!
     Fraudulent:     true
     Risk Score:     0.89
     Anomaly Flags:  [IP_GEOLOCATION_MISMATCH, UNUSUAL_MIDNIGHT_AMOUNT, VELOCITY_SPIKE]
     Recommendation: BLOCK_TRANSACTION_AND_ALERT_CARDHOLDER

[TEST 3] Converting Complex FinancialReport Record...
  ✅ SUCCESSFULLY DESERIALIZED FINANCIAL REPORT!
     Company:       Acme Cloud AI Inc.
     Revenue ($M):  $348.5
     Fiscal Year:   2026
     Risk Factors:  [GPU_SUPPLY_CHAIN, CURRENCY_FLUCTUATION, REGULATORY_COMPLIANCE]

================================================================================
  STRUCTURED OUTPUT CONVERSION VALIDATED WITH ZERO RUNTIME ERRORS!              
================================================================================
```

---

## Why It Matters for Gen AI Applications

| Risk / Challenge | Raw String Handling | Structured Output (`BeanOutputConverter`) |
|:---|:---|:---|
| **Runtime Crash Rate** | High (5%–15% of responses fail JSON parsing due to markdown fences or conversational chatter). | 0% (Cleaned, validated against schema, self-corrected on failure). |
| **Downstream Integration** | Requires fragile custom regex or manual parsing per endpoint. | Direct binding to strongly-typed Java Records and domain models. |
| **API Contract Enforcement** | Undefined; model might output XML, YAML, or prose interchangeably. | JSON Schema enforced in prompt and validated at deserialization. |
| **Enterprise Maintainability** | Adding a field requires rewriting regex. | Adding a record component automatically updates the JSON Schema! |

---

## Hands-On Exercises (With Complete Solutions)

### Exercise 1: Multi-Item Invoice Line Extraction
**Problem Statement:**  
Create a domain record `InvoiceItem(String description, int quantity, double unitPrice, double lineTotal)` and `Invoice(String invoiceNumber, String vendor, List<InvoiceItem> items, double grandTotal)`.  
Write a method `extractInvoice(ChatClient client, String rawInvoiceText)` using `.call().entity(Invoice.class)`.

<details>
<summary>👉 View Solution</summary>

```java
public record InvoiceItem(String description, int quantity, double unitPrice, double lineTotal) {}

public record Invoice(String invoiceNumber, String vendor, List<InvoiceItem> items, double grandTotal) {}

@Service
public class InvoiceProcessingService {

    private final ChatClient chatClient;

    public InvoiceProcessingService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public Invoice extractInvoice(String rawInvoiceText) {
        return chatClient.prompt()
            .user(u -> u.text("""
                Extract invoice details and line items from the following document text:
                {document}
                Calculate line totals and grand total accurately.
            """).param("document", rawInvoiceText))
            .call()
            .entity(Invoice.class);
    }
}
```
</details>

---

### Exercise 2: Enum Deserialization for Strict State Machines
**Problem Statement:**  
Create a Java `enum TicketSeverity { CRITICAL_P0, MAJOR_P1, MINOR_P2, INFORMATIONAL_P3 }` and a record `TriageResult(TicketSeverity severity, String justification, String assignedGroup)`.  
Demonstrate that Spring AI automatically restricts the LLM's output to the defined enum constants.

<details>
<summary>👉 View Solution</summary>

```java
public enum TicketSeverity {
    CRITICAL_P0, MAJOR_P1, MINOR_P2, INFORMATIONAL_P3
}

public record TriageResult(
    TicketSeverity severity,
    String justification,
    String assignedGroup
) {}

@Service
public class IncidentTriageService {

    private final ChatClient chatClient;

    public IncidentTriageService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public TriageResult triageIncident(String incidentSummary) {
        return chatClient.prompt()
            .user(u -> u.text("Triage the following incident report:\n{report}")
                .param("report", incidentSummary))
            .call()
            .entity(TriageResult.class);
    }
}
```
*Explanation:* Spring AI inspects Java `enum` types and outputs a JSON Schema with the `enum: ["CRITICAL_P0", "MAJOR_P1", ...]` constraint. The model is forced to choose exclusively from valid enum names!
</details>

---

### Exercise 3: Resilient JSON Fallback Converter
**Problem Statement:**  
Write a custom Spring utility `safeConvertOrFallback(String rawJson, Class<T> targetClass, T fallbackInstance)` that attempts to deserialize the JSON using `BeanOutputConverter`. If parsing fails due to invalid syntax or missing fields, it catches the exception, logs a warning with the offending text, and returns `fallbackInstance` instead of throwing an error.

<details>
<summary>👉 View Solution</summary>

```java
package com.genai.springai.structured;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ResilientConverterUtils {

    private static final Logger log = LoggerFactory.getLogger(ResilientConverterUtils.class);

    private ResilientConverterUtils() {}

    public static <T> T safeConvertOrFallback(String rawLlmResponse, Class<T> targetClass, T fallbackInstance) {
        try {
            BeanOutputConverter<T> converter = new BeanOutputConverter<>(targetClass);
            return converter.convert(rawLlmResponse);
        } catch (Exception ex) {
            log.warn("Failed to parse structured output into {}. Falling back to default. Raw output: [{}]",
                    targetClass.getSimpleName(), rawLlmResponse, ex);
            return fallbackInstance;
        }
    }
}
```
</details>

---

## 5-Question Self-Check Quiz

#### 1. Why does `new ObjectMapper().readValue(...)` frequently fail when executed on raw LLM completions?
- A) Jackson cannot parse Unicode characters.
- B) LLMs frequently prepend conversational filler ("Here is your JSON:") or wrap content in markdown code fences (` ```json `), which violates RFC 8259 JSON syntax.
- C) Jackson requires an active database connection.
- D) Because Jackson is only compatible with Java 8.

#### 2. What role does `BeanOutputConverter.getFormat()` play in the Spring AI pipeline?
- A) It formats numbers as currency.
- B) It generates a prompt instruction string containing the JSON Schema derived from the target Java class, instructing the model how to structure its output.
- C) It compresses the JSON using GZIP.
- D) It compiles Java records to WebAssembly.

#### 3. In `ChatClient`, which method allows mapping a model's completion directly into a generic collection like `List<Product>`?
- A) `.call().entity(List.class)`
- B) `.call().entity(new ParameterizedTypeReference<List<Product>>() {})`
- C) `.call().collection(Product.class)`
- D) `.call().toList()`

#### 4. How does Spring AI handle Java `enum` components inside a record when generating JSON Schema?
- A) Enums are converted to integers.
- B) Enums are ignored.
- C) Spring AI inspects enum constants and generates a JSON Schema `enum` validation array, constraining the LLM to choose only valid enum names.
- D) Enums require a Python wrapper.

#### 5. In a Self-Correcting JSON Retry Loop, what information is passed back to the model on retry?
- A) A random new prompt.
- B) The original prompt combined with the exact JSON syntax/deserialization error message produced by Jackson.
- C) The server's environment variables.
- D) The source code of the Jackson library.

---

### Quiz Answers & Explanations

1. **B is correct**: LLMs naturally produce conversational text and markdown formatting that cause strict JSON parsers to abort with syntax exceptions.
2. **B is correct**: `getFormat()` translates Java record components and types into standard JSON Schema format directives for prompt inclusion.
3. **B is correct**: Spring's `ParameterizedTypeReference` captures generic type parameters at runtime, allowing Jackson to deserialize complex nested collections like `List<Product>`.
4. **C is correct**: JSON Schema supports `enum` arrays; Spring AI automatically populates this with the declared Java enum values.
5. **B is correct**: Passing the specific parser error message gives the LLM the exact context needed to locate and fix missing braces, trailing commas, or type mismatches.

---

## Day 35 Summary & Next Steps

Today you mastered:
1. **The Structured Output Architecture**: Eliminating runtime JSON parsing exceptions with Spring AI converters.
2. **Records as Schemas**: Generating dynamic JSON schemas directly from Java 21 record components.
3. **The `ChatClient.entity()` API**: Seamlessly converting LLM completions into Java records, DTOs, Enums, and generic collections.
4. **Markdown Sanitization**: Stripping code fences and finding JSON boundary braces.
5. **Self-Correcting Retry Loops**: Building self-healing AI pipelines that automatically repair malformed JSON.

👉 **Tomorrow in Day 36: Streaming Responses — The ChatGPT Typewriter Effect** — You will master how to stream tokens in real-time from the LLM to web clients using Spring WebFlux, Project Reactor, Server-Sent Events (SSE), and Java 21 Virtual Threads!
