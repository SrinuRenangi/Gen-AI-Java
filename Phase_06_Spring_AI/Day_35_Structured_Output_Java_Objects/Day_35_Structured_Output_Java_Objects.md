# Day 35: Structured Output — LLMs That Return Java Objects
## BeanOutputConverter, Java 21 Records as JSON Schemas, Markdown Sanitization & Self-Correcting Parsers

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [◀ Day 34: Prompt Engineering in Java](../Day_34_Prompt_Engineering_in_Java/Day_34_Prompt_Engineering_in_Java.md) | [All 60 Days Overview](../../README.md) | [Day 36: Streaming Responses — The Typewriter Effect ▶](../Day_36_Streaming_Responses/Day_36_Streaming_Responses.md) |

---

## 1. Topic Overview

Structured output mechanisms in Spring AI constrain language models to produce valid, schema-compliant JSON that deserializes directly into strongly-typed Java 21 Records, domain entities, and collections. In enterprise systems, where microservices require deterministic contracts rather than unpredictable conversational prose, structured converters and self-correcting retry parsers eliminate JSON parse crashes and guarantee reliable integration with downstream databases and business logic.

---

## 2. Basic Foundations (True Zero)

### The JSON Dilemma in Generative AI
Large Language Models are inherently autoregressive text generators: they communicate through natural language, frequently adding conversational pleasantries (e.g., *"Sure, here is your JSON:"*) or wrapping output in markdown code blocks (` ```json ... ``` `).

When traditional JSON parsers like Jackson execute `objectMapper.readValue(rawOutput, MyClass.class)` on this text, they immediately throw syntax exceptions:
- **Preamble Failure**: Jackson hits the letter `'S'` in *"Sure"* and crashes with `JsonParseException: Unrecognized token`.
- **Markdown Fences**: Triple backticks violate RFC 8259 JSON syntax.
- **Trailing Commas**: Models often append invalid trailing commas before closing braces.

**Spring AI Structured Output Converters** solve this by inspecting your Java 21 Records via reflection, automatically appending rigorous JSON Schema instructions to your prompt, sanitizing conversational chatter and markdown fences, and deserializing the clean JSON into your domain objects in a single line of code.

```
+-----------------------------------------------------------------------------------+
|               THE CUSTOMS DECLARATION FORM ANALOGY                                |
|                                                                                   |
|  SCENARIO 1: Casual Freeform Conversation (Raw String Output):                    |
|  - The customs border officer asks: "What are you bringing into the country?"     |
|  - You reply: "Well, I visited my grandmother in Paris, bought some cheese, three |
|    bottles of wine for 40 euros each, and a lovely woolen scarf."                 |
|  - The officer's computer CANNOT ingest this sentence into its tax database!      |
|                                                                                   |
|  SCENARIO 2: Formal Customs Declaration Form (Structured Output Schema):          |
|  - The officer hands you a formal printed grid with predefined columns:           |
|    [Item Description]        [Quantity]    [Declared Value USD]                   |
|    - French Cheese               1               $25.00                           |
|    - Red Wine                    3              $120.00                           |
|  - The barcode scanner reads the exact rows directly into the database!           |
|                                                                                   |
|  In Spring AI: Your Java 21 Record is the printed Customs Declaration Form.       |
|  Spring AI gives the LLM the exact schema grid to populate!                       |
+-----------------------------------------------------------------------------------+
```

### Minimal Beginner-Friendly Working Code Example

Below is a self-contained Java 21 simulation demonstrating how markdown sanitization and JSON brace extraction work under the hood:

```java
public class BasicStructuredSanitizerExample {

    // Simulates Spring AI's MarkdownJsonSanitizer
    public static String sanitizeLlmOutput(String rawText) {
        String cleaned = rawText.trim();
        
        // 1. Strip Markdown Code Fences (```json ... ```)
        if (cleaned.contains("```json")) {
            int start = cleaned.indexOf("```json") + 7;
            int end = cleaned.indexOf("```", start);
            cleaned = (end != -1) ? cleaned.substring(start, end) : cleaned.substring(start);
        } else if (cleaned.contains("```")) {
            int start = cleaned.indexOf("```") + 3;
            int end = cleaned.indexOf("```", start);
            cleaned = (end != -1) ? cleaned.substring(start, end) : cleaned.substring(start);
        }

        // 2. Extract content between first '{' and last '}'
        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            cleaned = cleaned.substring(firstBrace, lastBrace + 1);
        }

        return cleaned.trim();
    }

    public static void main(String[] args) {
        String messyLlmOutput = """
            Certainly! Here is the user profile you requested:
            ```json
            {
              "name": "Alice Johnson",
              "age": 32,
              "role": "SOFTWARE_ARCHITECT"
            }
            ```
            Let me know if you need anything else!
            """;

        System.out.println("--- Raw Unsanitized LLM Output ---");
        System.out.println(messyLlmOutput);

        String cleanJson = sanitizeLlmOutput(messyLlmOutput);
        System.out.println("--- Clean Extracted JSON Ready for Jackson ---");
        System.out.println(cleanJson);
    }
}
```

#### Line-by-Line Walkthrough:
- **Lines 5–8**: Initializes `sanitizeLlmOutput` to scrub conversational wrapper text.
- **Lines 9–17**: Identifies and strips opening markdown fences (` ```json `) and closing triple backticks.
- **Lines 19–24**: Locates the outermost `{` and `}` curly braces, discarding any preceding chatter (*"Certainly!"*) or trailing pleasantries (*"Hope this helps!"*).
- **Lines 28–46**: Passes a typical conversational LLM response through the cleaner, isolating pure, parseable JSON.

---

## 3. Core Concept Walkthrough (Basic → Intermediate)

### Spring AI Structured Output Converters

Spring AI provides three core converter implementations in `org.springframework.ai.chat.converter.*`:

```
                           STRUCTURED OUTPUT CONVERTERS
                           
                       +-------------------------------+
                       |   StructuredOutputConverter   |
                       +---------------+---------------+
                                       |
         +-----------------------------+-----------------------------+
         v                             v                             v
+------------------+         +-------------------+         +--------------------+
|BeanOutputConverter|        | MapOutputConverter|         |ListOutputConverter |
|   (Java Record)  |         |(Key-Value Mapping)|         |  (Collection/Array)|
+------------------+         +-------------------+         +--------------------+
```

#### 1. `BeanOutputConverter<T>` (The Standard Approach)
```java
// 1. Define domain record
public record SecurityAuditReport(
    String targetHost,
    int openPortsCount,
    List<String> detectedVulnerabilities,
    boolean requiresPatching
) {}

// 2. Instantiate converter
BeanOutputConverter<SecurityAuditReport> converter = 
    new BeanOutputConverter<>(SecurityAuditReport.class);

// 3. Inject format schema into prompt
String promptText = """
    Analyze the following network scan log:
    {scanLog}
    {format}
    """;

PromptTemplate template = new PromptTemplate(promptText);
Prompt prompt = template.create(Map.of(
    "scanLog", rawScanOutput,
    "format", converter.getFormat()
));

// 4. Invoke model and convert
ChatResponse response = chatModel.call(prompt);
SecurityAuditReport report = converter.convert(response.getResult().getOutput().getContent());
```

---

### Using `ChatClient.entity()`: The Modern Fluent Approach

In everyday application development, `ChatClient.entity()` combines schema generation, prompt injection, invocation, sanitization, and Jackson deserialization into one fluent call:

```java
package com.example.genai.controller;

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
            .user(u -> u.text("Extract all required programming languages:\n{jd}")
                .param("jd", jobDescription))
            .call()
            .entity(new ParameterizedTypeReference<List<String>>() {});
    }
}
```

---

### Native Model JSON Mode vs. Prompted JSON Schema

Modern inference engines support native JSON grammar constraints:

#### 1. OpenAI Native Strict Mode:
Configured via `application.yml` to lock token sampling directly into JSON:
```yaml
spring:
  ai:
    openai:
      chat:
        options:
          response-format:
            type: json_object
```

#### 2. Ollama Local Engine JSON Mode:
Forces Llama 3.2 or Mistral to restrict token logit sampling exclusively to valid JSON grammar:
```yaml
spring:
  ai:
    ollama:
      chat:
        options:
          format: json
```

---

## 4. Prerequisite & Supporting Concepts

### Prerequisite / Supporting Concept: Java 21 Record Reflection
Spring AI constructs JSON Schemas dynamically by inspecting Java 21 Record components via reflection (`Class.isRecord()` and `Class.getRecordComponents()`). Each component's name and type (`String` -> `"type": "string"`, `int` -> `"type": "integer"`) are mapped into a standardized JSON Schema object injected into the prompt.

### Prerequisite / Supporting Concept: Enum Constrained Sampling
When a record component is a Java `enum`, Spring AI translates the enum values into a JSON Schema `enum` constraint:
```json
"severity": { "type": "string", "enum": ["LOW", "MEDIUM", "HIGH", "CRITICAL"] }
```
This restricts the model from generating random strings, forcing it to output only declared Java enum constants.

### Prerequisite / Supporting Concept: `ParameterizedTypeReference`
Due to Java's type erasure, passing `List.class` to Jackson loses the generic type parameter `T`. Spring's `ParameterizedTypeReference<List<CandidateProfile>>` captures generic type information at runtime, allowing Jackson to deserialize nested collections without type-cast exceptions.

---

## 5. Advanced Depth (Intermediate → Advanced)

### Resilient Self-Correcting JSON Retry Loop

When handling complex schemas with lower-tier models, an LLM may occasionally generate a malformed JSON payload (such as an unescaped quote or missing bracket). In enterprise Java, we implement a **Self-Correction Retry Loop** that hands the specific Jackson error back to the model:

```
                            SELF-CORRECTING RETRY PIPELINE
                            
 Prompt Sent to Model
         |
         v
 Model Returns Completion
         |
         v
 Attempt Deserialization (Jackson / BeanOutputConverter)
         |
         +-- [SUCCESS] --------------> Return Java Record to Caller
         |
         \-- [JsonParseException]
                   |
                   v
 Build Error-Feedback Prompt:
 "Your previous response was NOT valid JSON.
  Parser Error: Unexpected character ',' at line 4 column 12.
  Fix the error and return strictly valid JSON matching schema: ..."
                   |
                   v
 Model Retries (Corrects its own syntax error!)
```

#### Java Implementation:
```java
@Service
public class ResilientStructuredService {

    private final ChatClient chatClient;

    public ResilientStructuredService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public <T> T callWithSelfCorrection(String userPrompt, Class<T> targetClass, int maxRetries) {
        String currentPrompt = userPrompt;
        String lastError = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            if (lastError != null) {
                currentPrompt = userPrompt + "\n\nCRITICAL FIX REQUIRED: Your previous attempt failed JSON parsing: [" 
                        + lastError + "]. Fix the syntax error and return strictly valid JSON.";
            }

            try {
                return chatClient.prompt()
                    .user(currentPrompt)
                    .call()
                    .entity(targetClass);
            } catch (Exception ex) {
                lastError = ex.getMessage();
                System.err.println("Attempt " + attempt + " failed parsing JSON: " + lastError);
            }
        }

        throw new IllegalStateException("Failed to extract valid " + targetClass.getSimpleName() + " after " + maxRetries + " attempts.");
    }
}
```

---

### Hands-On Simulation Code Walkthrough

The companion code repository demonstrates this architecture:
- `JsonSchemaGenerator.java`: Uses Java 21 `RecordComponent` reflection to generate JSON schemas dynamically.
- `MarkdownJsonSanitizer.java`: Strips markdown fences (` ```json `) and extracts outer JSON braces `{ ... }`.
- `BeanOutputConverter.java`: Binds schema formatting instructions to prompts and extracts values into domain records.
- `StructuredOutputDemo.java`: 3-scenario verification test suite validating schema generation, markdown extraction, and complex financial report deserialization.

```powershell
# Compile Day 35 code
javac -d out Phase_06_Spring_AI/Day_35_Structured_Output_Java_Objects/code/*.java

# Run StructuredOutputDemo
java -cp out com.genai.springai.structured.StructuredOutputDemo
```

#### Verified Execution Output:
```
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

  [OK] SUCCESSFULLY DESERIALIZED INTO JAVA RECORD!
     Fraudulent:     true
     Risk Score:     0.89
     Anomaly Flags:  [IP_GEOLOCATION_MISMATCH, UNUSUAL_MIDNIGHT_AMOUNT, VELOCITY_SPIKE]
     Recommendation: BLOCK_TRANSACTION_AND_ALERT_CARDHOLDER

[TEST 3] Converting Complex FinancialReport Record...
  [OK] SUCCESSFULLY DESERIALIZED FINANCIAL REPORT!
     Company:       Acme Cloud AI Inc.
     Revenue ($M):  $348.5
     Fiscal Year:   2026
     Risk Factors:  [GPU_SUPPLY_CHAIN, CURRENCY_FLUCTUATION, REGULATORY_COMPLIANCE]

================================================================================
  STRUCTURED OUTPUT CONVERSION VALIDATED WITH ZERO RUNTIME ERRORS!              
================================================================================
```

---

## 6. Quick Recap

| Concept | Description | Enterprise Rule / Best Practice |
| :--- | :--- | :--- |
| **Structured Output** | Forcing models to reply in valid JSON schemas| Eliminates manual regex string slicing. |
| **`BeanOutputConverter`** | Generates JSON Schema and deserializes beans| Automatically derives schema from Java 21 Records. |
| **`.entity(Class<T>)`** | Fluent `ChatClient` conversion method | Preferred ergonomic entry point for domain DTOs. |
| **Sanitization** | Stripping markdown backticks and preambles | Protects Jackson from conversational prose crashes. |
| **Native JSON Mode** | Engine-level logit constraint | Locks Ollama or OpenAI token generation to JSON. |
| **Self-Correction** | Resending parser errors back to the model | Automatically repairs syntax typos and missing commas. |

---

## 7. Self-Check Questions & Practice Exercises

### Conceptual & Architectural Questions

#### Q1: Why does `new ObjectMapper().readValue(...)` frequently fail when executed on raw LLM completions?
**Answer**: Language models frequently prepend conversational filler (*"Certainly! Here is your data:"*) and wrap the JSON body inside markdown backticks (` ```json `). Strict JSON parsers like Jackson reject these characters immediately with `JsonParseException`.

#### Q2: What role does `BeanOutputConverter.getFormat()` play in the Spring AI pipeline?
**Answer**: `getFormat()` reflects upon the target Java class or record, constructs a formal JSON Schema, and generates a formatted prompt instruction telling the model to adhere strictly to that schema without markdown fences or outside text.

#### Q3: In `ChatClient`, which method allows mapping a model's completion directly into a generic collection like `List<Product>`?
**Answer**: **`.call().entity(new ParameterizedTypeReference<List<Product>>() {})`**, which preserves generic type metadata past Java's runtime type erasure.

#### Q4: How does Spring AI handle Java `enum` components inside a record when generating JSON Schema?
**Answer**: Spring AI inspects declared enum constants and generates a JSON Schema `enum` validation array (`"enum": ["ACTIVE", "SUSPENDED"]`), constraining the model to output only declared Java enum values.

#### Q5: In a Self-Correcting JSON Retry Loop, what information is passed back to the model on retry?
**Answer**: The original user prompt combined with the specific Jackson syntax error message (including line and column numbers), instructing the model to fix its mistake and output strictly valid JSON.

---

### Hands-On Practice Exercises

#### Exercise 1: Multi-Item Invoice Line Extraction
**Task**: Create records `InvoiceItem(String description, int quantity, double unitPrice, double lineTotal)` and `Invoice(String invoiceNumber, String vendor, List<InvoiceItem> items, double grandTotal)`. Write a service extracting this structure from document text via `.entity(Invoice.class)`.

```java
// Solution:
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

#### Exercise 2: Enum Deserialization for Strict State Machines
**Task**: Create enum `TicketSeverity { CRITICAL_P0, MAJOR_P1, MINOR_P2, INFORMATIONAL_P3 }` and record `TriageResult(TicketSeverity severity, String justification, String assignedGroup)`. Demonstrate automated enum extraction.

```java
// Solution:
public enum TicketSeverity { CRITICAL_P0, MAJOR_P1, MINOR_P2, INFORMATIONAL_P3 }
public record TriageResult(TicketSeverity severity, String justification, String assignedGroup) {}

@Service
public class IncidentTriageService {

    private final ChatClient chatClient;

    public IncidentTriageService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public TriageResult triageIncident(String incidentSummary) {
        return chatClient.prompt()
            .user(u -> u.text("Triage the following incident report:\n{report}").param("report", incidentSummary))
            .call()
            .entity(TriageResult.class);
    }
}
```

#### Exercise 3: Resilient JSON Fallback Converter
**Task**: Write a utility `safeConvertOrFallback(String rawJson, Class<T> targetClass, T fallbackInstance)` that attempts deserialization via `BeanOutputConverter`, returning `fallbackInstance` upon parsing errors.

```java
// Solution:
public final class ResilientConverterUtils {

    private ResilientConverterUtils() {}

    public static <T> T safeConvertOrFallback(String rawLlmResponse, Class<T> targetClass, T fallbackInstance) {
        try {
            BeanOutputConverter<T> converter = new BeanOutputConverter<>(targetClass);
            return converter.convert(rawLlmResponse);
        } catch (Exception ex) {
            System.err.println("Failed to parse structured output into " + targetClass.getSimpleName() + ". Returning fallback.");
            return fallbackInstance;
        }
    }
}
```

---

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [◀ Day 34: Prompt Engineering in Java](../Day_34_Prompt_Engineering_in_Java/Day_34_Prompt_Engineering_in_Java.md) | [All 60 Days Overview](../../README.md) | [Day 36: Streaming Responses — The Typewriter Effect ▶](../Day_36_Streaming_Responses/Day_36_Streaming_Responses.md) |
