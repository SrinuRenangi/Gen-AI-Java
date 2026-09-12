# Day 48: Tool Execution & Function Calling in LangChain4j

[← Previous: Day 47 - Advanced RAG Chunking & Re-Ranking](../Day_47_Advanced_RAG_Chunking_ReRanking/Day_47_Advanced_RAG_Chunking_ReRanking.md) | [Next: Day 49 - Building a ReAct Agent in Java →](../Day_49_Building_ReAct_Agent_in_Java/Day_49_Building_ReAct_Agent_in_Java.md)

---

## 1. Topic Overview
Tool execution (Function Calling) allows Large Language Models to request the deterministic execution of local Java methods to perform real-time database queries, complex mathematical calculations, and external API calls without guessing. In LangChain4j, tools are defined using a clean, POJO-first paradigm with `@Tool` and `@P` annotations, enabling seamless multi-tool co-location and resilient exception containment directly within declarative `AiServices` agents.

---

## 2. Basic Foundations (True Zero)

### What is Tool Execution in LangChain4j?
When an LLM is asked to perform a complex mathematical calculation (such as an exact 30-year fixed loan amortization) or look up live inventory in an ERP database, it cannot do so from memory. Guessing results in costly hallucinations.

**Tool Calling provides the solution:**
- The LLM does not run arbitrary code on your server.
- Instead, you expose ordinary Java methods annotated with `@Tool`.
- When the LLM decides it needs external computation or live data, it halts text generation and returns a structured request: *"Please execute `calculateMonthlyPayment(500000, 6.8, 360)`."*
- LangChain4j invokes your Java method locally via reflection, retrieves the exact return value, and sends it back to the LLM.
- The LLM then synthesizes an authoritative, natural-language response incorporating the verified facts.

### Relatable Physical Analogy: The Surgeon and the Instrument Tray
Imagine a world-class neurosurgeon in an operating theatre:
- **Without Tools**: The surgeon has vast cognitive knowledge (anatomy, pathology, surgical theory), but cannot operate with bare hands.
- **With Tools**: A surgical nurse stands beside the operating table holding a sterile instrument tray. When an incision or measurement is needed, the surgeon calls out: *"Scalpel, blade number 10."* The nurse hands over the exact instrument, the incision is made, and the surgeon proceeds to the next phase of the operation.
- The **LLM is the surgeon** (strategic reasoning, intent detection); your **Java methods are the surgical instruments** (deterministic execution, high-precision calculation, database access).

### Minimal Beginner-Friendly Working Code
Here is how to define a POJO tool and bind it to an `AiServices` agent in LangChain4j:

```java
package com.genai.langchain4j.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public class SimpleToolRunner {

    // 1. Define business tools on a standard Java POJO class
    public static class FinancialCalculator {
        @Tool("Calculates monthly mortgage payment given principal amount, annual interest percentage, and term in months")
        public double calculateMonthlyPayment(
            @P("Loan principal amount in USD") double principal,
            @P("Annual interest rate percentage, e.g. 6.5 for 6.5%") double annualRatePercent,
            @P("Loan term in months, e.g. 360 for 30-year fixed") int termMonths
        ) {
            double monthlyRate = (annualRatePercent / 100.0) / 12.0;
            double payment = (principal * monthlyRate * Math.pow(1 + monthlyRate, termMonths))
                           / (Math.pow(1 + monthlyRate, termMonths) - 1);
            return Math.round(payment * 100.0) / 100.0;
        }
    }

    // 2. Define the declarative agent interface
    public interface MortgageAdvisor {
        @SystemMessage("You are an expert mortgage advisor. Always use calculation tools for financial estimates.")
        String consult(@UserMessage String question);
    }

    public static void main(String[] args) {
        ChatLanguageModel model = OpenAiChatModel.builder()
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .modelName("gpt-4o")
            .build();

        // 3. Register tool POJO with one line of code!
        MortgageAdvisor advisor = AiServices.builder(MortgageAdvisor.class)
            .chatLanguageModel(model)
            .tools(new FinancialCalculator())
            .build();

        // 4. Model detects intent, calls calculateMonthlyPayment, and returns exact answer
        String response = advisor.consult(
            "What will my monthly payment be on a $450,000 30-year fixed mortgage at 6.5% interest?"
        );
        System.out.println("Advisor Answer:\n" + response);
    }
}
```

### Line-by-Line Walkthrough
1. **`@Tool("...")`**: Annotates any public method on a standard Java class, exporting it into the LLM's tool schema registry.
2. **`@P("...") double principal`**: Documents each method parameter with human-readable descriptions embedded into the generated JSON Schema.
3. **`AiServices.builder(...).tools(new FinancialCalculator())`**: Scans the object via reflection, registers the tool schemas, and wires up the dynamic proxy's execution handler.
4. **`advisor.consult("...")`**: The LLM emits a `ToolExecutionRequest`, LangChain4j runs `calculateMonthlyPayment(450000.0, 6.5, 360)`, feeds the `$2844.31` result back, and returns the grounded answer.

---

## 3. Core Concept Walkthrough (Basic → Intermediate)

```
+-------------------------------------------------------------------------------+
|                       THE POJO-FIRST TOOL CALLING CYCLE                       |
+-------------------------------------------------------------------------------+
|                                                                               |
|  User: "What's my monthly payment on a $500,000 loan at 6.8% for 30 years?"   |
|         |                                                                     |
|         v                                                                     |
|  [ 1. Tool Schema Declaration ]                                               |
|  LangChain4j scans @Tool methods on FinancialCalculator,                      |
|  generating JSON Schema: calculateMonthlyPayment(principal, rate, term)       |
|         |                                                                     |
|         v                                                                     |
|  [ 2. ToolExecutionRequest ]                                                  |
|  LLM detects math requirement; halts text generation and returns:             |
|  call: calculateMonthlyPayment(principal=500000.0, rate=6.8, term=360)        |
|         |                                                                     |
|         v                                                                     |
|  [ 3. Local Java Reflection Dispatch ]                                        |
|  LangChain4j invokes FinancialCalculator.calculateMonthlyPayment(...)         |
|  Returns: 3259.94                                                             |
|         |                                                                     |
|         v                                                                     |
|  [ 4. ToolExecutionResultMessage ]                                            |
|  Result passed back to model: {"result": 3259.94}                             |
|         |                                                                     |
|         v                                                                     |
|  [ 5. Natural Language Synthesis ]                                            |
|  LLM responds: "Your monthly principal and interest payment will be $3,259.94."|
+-------------------------------------------------------------------------------+
```

### POJO-First Tooling: LangChain4j vs. Spring AI

| Feature | Spring AI | LangChain4j |
|:---|:---|:---|
| **Declaration Paradigm** | Standalone Spring `@Bean` implementing `Function<Req, Resp>`. | Standard Java class with methods annotated with `@Tool`. |
| **Methods Per Class** | Strictly 1 function per bean. | **Multiple `@Tool` methods co-located** within a single domain class. |
| **Parameter Types** | Requires a dedicated Java Request Record/POJO per function. | Natural Java parameter signatures (`double amount, String sku, int term`). |
| **Parameter Documentation**| Jackson `@JsonPropertyDescription` on record components. | `@P("description")` annotation directly on method parameters. |
| **Container Dependency** | Requires Spring Framework IoC container. | Zero framework dependencies (runs in Spring, Quarkus, or plain `main()` methods). |

---

## 4. Prerequisite & Supporting Concepts

### Prerequisite / Supporting Concept: Precision Parameter Documentation with `@P`
Without `@P`, the model only sees parameter names (`p1`, `p2`, or `arg0` if compiled without `-parameters`).
- **Unclear**: `public double calculate(double a, double b, int c)` $\rightarrow$ Model has no idea which parameter is principal, interest rate, or term months.
- **Explicit**:
  ```java
  public double calculate(
      @P("Loan principal amount in USD") double principal,
      @P("Annual interest rate percentage, e.g. 6.5") double rate,
      @P("Loan term in months, e.g. 360") int termMonths
  )
  ```
- **Compilation Requirement**: Ensure your build configuration includes `-parameters` in `javac` options so reflection retains parameter names.

---

## 5. Advanced Depth (Intermediate → Advanced)

### Exception Containment and Graceful Self-Healing
What happens if a tool throws an exception (e.g., an unauthorized GPU quota request or a database connectivity timeout)?

#### Anti-Pattern: Crashing the Application Thread
```java
// BAD: Unhandled exception crashes the user HTTP request with a 500 error!
@Tool("Allocates GPU nodes")
public String reserveGpu(int count) {
    if (count > 8) throw new RuntimeException("Quota exceeded!");
    return "Allocated " + count;
}
```

#### Production Pattern: Structured Error Containment
In production, catch domain exceptions, serialize them to a clean JSON error response, and return them in `ToolExecutionResultMessage`. The LLM reads the error and explains the business constraint constructively:

```java
package com.genai.langchain4j.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

public class CloudResourceTools {

    private static final int MAX_AUTONOMOUS_GPUS = 8;

    @Tool("Provisions dedicated GPU cloud instances for enterprise training clusters")
    public String reserveGpuInstances(
        @P("Cluster identifier") String clusterId,
        @P("Number of GPU nodes to allocate") int nodeCount
    ) {
        if (nodeCount <= 0) {
            return "{\"error\": \"InvalidRequest: Node count must be strictly greater than zero.\"}";
        }

        if (nodeCount > MAX_AUTONOMOUS_GPUS) {
            return String.format(
                "{\"error\": \"QuotaExceeded: Requested %d GPUs exceeds autonomous ceiling (%d). Requires VP approval.\"}",
                nodeCount, MAX_AUTONOMOUS_GPUS
            );
        }

        return String.format("{\"status\": \"SUCCESS\", \"cluster\": \"%s\", \"allocatedGpus\": %d}", clusterId, nodeCount);
    }
}
```

When the user asks: *"Please spin up 16 GPU nodes on cluster prod-1"*, the model reads the error string and replies:
> *"I cannot allocate 16 GPU nodes because your autonomous ceiling is capped at 8 nodes. Would you like me to allocate 8 nodes now, or submit an approval ticket to the VP of Engineering?"*

### Safe Database Query Tool with Whitelist Protection
Never allow an LLM tool to execute arbitrary SQL statements. Implement strict table whitelisting:

```java
package com.genai.langchain4j.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import java.util.Set;

public class SafeDatabaseTools {

    private static final Set<String> ALLOWED_TABLES = Set.of("products", "orders", "promotions");

    @Tool("Retrieves active schema records for approved public business tables")
    public String inspectTableData(@P("Table name to inspect") String tableName) {
        if (!ALLOWED_TABLES.contains(tableName.toLowerCase())) {
            return String.format("{\"error\": \"Access Denied: Table '%s' is not in the approved whitelist.\"}", tableName);
        }
        return String.format("{\"table\": \"%s\", \"status\": \"ACTIVE\", \"rowCount\": 150}", tableName);
    }
}
```

### Common Anti-Patterns & Production Traps

| Anti-Pattern | Why It Breaks in Production | Correct Architectural Solution |
|:---|:---|:---|
| **Omitting `@P` Parameter Annotations** | Model guesses argument types and units, often passing percentages as decimals (`0.065` instead of `6.5`). | Annotate all tool parameters with clear `@P("...")` documentation strings. |
| **Letting Exceptions Escape to the Caller** | Unchecked exceptions terminate the conversational loop and trigger 500 server errors. | Trap exceptions inside tool methods and return structured JSON error payloads. |
| **Mutating State on Read-Only Prompts** | Giving an agent a tool named `deleteUserRecord` without authorization gates allows prompt injection attacks to delete production records. | Enforce authorization gates and Human-in-the-Loop approval workflows for state-altering operations. |

---

## 6. Quick Recap
- **Tool Calling** enables LLMs to request execution of real, deterministic Java methods on your backend server.
- LangChain4j uses a **POJO-first** approach: any standard Java method annotated with **`@Tool`** and **`@P`** becomes an executable tool.
- Multiple related tool methods can be co-located within a single cohesive Java service class.
- Declarative agents register tools with a single line: `.tools(new MyServiceTools())` on `AiServices.builder()`.
- Resilient systems implement **Exception Containment**, returning structured error JSON so the LLM can explain constraints or self-heal rather than crashing the application thread.

---

## 7. Self-Check Questions & Practice Exercises

### 5-Question Self-Check Quiz

#### Question 1
How does LangChain4j discover and register tools on a Java object?
- A) It parses XML configuration files located in `src/main/resources`.
- B) It scans the object at runtime via reflection, identifying methods annotated with `@Tool` and extracting parameter documentation from `@P` annotations.
- C) It requires all tools to extend an abstract C++ class.
- D) It only works if the class implements `java.io.Serializable`.

#### Question 2
What is the primary ergonomic advantage of LangChain4j's `@Tool` over Spring AI's functional beans?
- A) LangChain4j tools run faster on the CPU.
- B) Multiple tool methods can be co-located inside a single POJO class with normal method parameter lists, rather than requiring one `@Bean Function<I, O>` per tool.
- C) Spring AI is deprecated.
- D) LangChain4j does not require Java JDK 21.

#### Question 3
What should happen when an enterprise Java tool method encounters a business constraint violation?
- A) Crash the JVM process.
- B) The exception should be captured and returned to the model as a structured error payload, allowing the LLM to self-heal and inform the user gracefully.
- C) Hide the error and return random synthetic numbers.
- D) Delete the database table.

#### Question 4
What is the role of the `@P` annotation?
- A) To declare a method parameter as a primary key.
- B) To provide a natural language description for a tool method parameter, guiding the LLM on what format, units, and values to supply.
- C) To make the parameter private.
- D) To compress the parameter in memory.

#### Question 5
How are tools bound to a declarative `AiServices` agent in LangChain4j?
- A) By specifying `.tools(myToolObject)` on `AiServices.builder(...)`.
- B) By writing raw SQL queries.
- C) By adding an entry to `application.properties`.
- D) Tools cannot be used with `AiServices`.

---

### Quiz Answers & Explanations
1. **B**: LangChain4j inspects the class using reflection, converting methods decorated with `@Tool` and parameters decorated with `@P` into `ToolSpecification` schemas.
2. **B**: LangChain4j allows you to organize multiple related business tools inside a single cohesive class using natural Java method signatures.
3. **B**: Wrapping exceptions inside the tool result allows the LLM to understand why the operation failed (e.g., invalid parameter, quota exceeded) and provide a constructive conversational response.
4. **B**: `@P` provides the description embedded into the tool parameter's JSON Schema, telling the LLM the expected format, units, or constraints for that specific argument.
5. **A**: Calling `.tools(...)` on `AiServices.builder(...)` registers your tool instances, and the dynamic proxy orchestrates the execution loop automatically.

---

### Hands-On Practice Exercises

#### Exercise 1: Customer Verification Tool with Audit Logging
**Problem Statement**:  
Build a class `CustomerVerificationTools` with a method `@Tool String verifyKycStatus(@P("Customer SSN or Tax ID") String taxId)` that checks an internal customer map. Implement an audit log printing every time the tool is invoked.

<details>
<summary>👉 View Solution</summary>

```java
package com.genai.langchain4j.exercises;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import java.util.Map;

public class CustomerVerificationTools {

    private final Map<String, String> kycRecords = Map.of(
        "TAX-101", "{\"status\": \"VERIFIED\", \"tier\": \"VIP\"}",
        "TAX-202", "{\"status\": \"PENDING_DOCUMENTATION\", \"tier\": \"STANDARD\"}"
    );

    @Tool("Verifies customer identity and KYC compliance tier by Tax ID")
    public String verifyKycStatus(@P("Customer unique tax identifier") String taxId) {
        System.out.println("[AUDIT LOG] verifyKycStatus invoked for TaxID: " + taxId);
        return kycRecords.getOrDefault(taxId, "{\"status\": \"NOT_FOUND\"}");
    }
}
```
</details>

#### Exercise 2: Safe Database Query Tool with Table Whitelist
**Problem Statement**:  
Write a tool method `@Tool String executeTableQuery(@P("Target table name") String table)` that rejects any table not explicitly listed in an allowed whitelist (`Set.of("products", "orders", "promotions")`).

<details>
<summary>👉 View Solution</summary>

```java
package com.genai.langchain4j.exercises;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import java.util.Set;

public class SafeDatabaseTools {

    private static final Set<String> ALLOWED_TABLES = Set.of("products", "orders", "promotions");

    @Tool("Retrieves active schema records for approved public tables")
    public String executeTableQuery(@P("Table name to inspect") String table) {
        if (!ALLOWED_TABLES.contains(table.toLowerCase())) {
            return "{\"error\": \"Access Denied: Table '" + table + "' is not whitelisted.\"}";
        }
        return "{\"table\": \"" + table + "\", \"status\": \"ACTIVE\", \"rowCount\": 120}";
    }
}
```
</details>

---

[← Previous: Day 47 - Advanced RAG Chunking & Re-Ranking](../Day_47_Advanced_RAG_Chunking_ReRanking/Day_47_Advanced_RAG_Chunking_ReRanking.md) | [Next: Day 49 - Building a ReAct Agent in Java →](../Day_49_Building_ReAct_Agent_in_Java/Day_49_Building_ReAct_Agent_in_Java.md)
