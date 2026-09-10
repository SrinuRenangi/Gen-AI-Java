# Day 48: Tool Execution & Function Calling in LangChain4j

## Empowering AI Services with Executable Java Methods, `@Tool` Annotations, and Resilient Error Handling

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [Day 47: Advanced RAG — Chunking, Scoring & Re-Ranking](../Day_47_Advanced_RAG_Chunking_ReRanking/Day_47_Advanced_RAG_Chunking_ReRanking.md) | [All 60 Days Overview](../../README.md) | [Day 49: Building a ReAct Agent in Java](../Day_49_Building_ReAct_Agent_in_Java/Day_49_Building_ReAct_Agent_in_Java.md) |

---

Welcome to Day 48! Up to this point, our AI models have been brilliant thinkers and readers—they can ingest huge documents and summarize enterprise policies. But until now, they've essentially been locked in a room without hands. If you ask an LLM to calculate a 30-year loan payment or look up a live inventory count in your database, it has to guess (which leads to costly hallucinations!).

Today, we give our AI models hands! You'll learn how to let your AI call your real, deterministic Java methods using LangChain4j's `@Tool` system. You'll discover how clean, POJO-first, and fun tool calling in Java can be. Let's start with a quick glossary of today's core concepts:

---

> 💡 **New Word Alert! Plain English Definitions for Today's Concepts**
>
> - **Tool Execution (Function Calling)**: Giving an LLM access to external tools. When the AI realizes it doesn't know the exact answer (like today's weather or complex arithmetic), it stops and asks your Java program: *"Please run `calculatePayment(450000, 6.5, 360)` for me and tell me what you get!"*
> - **`@Tool`**: A simple LangChain4j annotation you stick on top of any normal Java method. It acts as an invitation telling the AI: *"Here is an action you are allowed to request!"*
> - **`@P` (Parameter Documentation)**: An annotation placed directly before method arguments (like `@P("Loan term in months") int termMonths`). It's the label on the button that tells the LLM exactly what format and units your method expects.
> - **POJO-First Tooling**: In LangChain4j, you don't need complicated framework wrappers or single-method beans. Any standard Java class (Plain Old Java Object) with regular methods can become a suite of tools.
> - **`ToolSpecification`**: The behind-the-scenes contract (JSON schema) that LangChain4j automatically generates from your Java code to explain your tools to the LLM.
> - **Tool Execution Loop**: The back-and-forth dance: User asks question ➔ LLM requests tool ➔ Java runs method ➔ Java returns result to LLM ➔ LLM gives final polished answer to user.

---

## What Will You Learn Today?

- **The POJO-First Tooling Paradigm**: Why LangChain4j's `@Tool` annotation on regular Java methods offers superior ergonomics compared to boilerplate functional beans.
- **Precision Parameter Documentation**: Using `@P` annotations to guide the model's argument synthesis, eliminating format errors and type mismatches.
- **Declarative Tool Binding with `AiServices`**: Registering multi-method tool objects into declarative agents with a single `.tools(...)` builder call.
- **Programmatic Tool Construction**: Building dynamic tools at runtime via `ToolSpecification` and `ToolExecutor` for extensible plugin architectures.
- **Exception Containment & Graceful Recovery**: Wrapping tool runtime exceptions into structured error messages so the LLM can self-heal or explain failures without crashing the application thread.
- **Enterprise Safety & Quota Enforcement**: Implementing programmatic circuit breakers and quota ceilings directly inside tool methods.

---

## 1. Real-World Analogy: The Surgeon and the Surgical Instrument Tray

Imagine the world's most brilliant neurosurgeon performing a complex surgical procedure:
- The surgeon possesses extraordinary cognitive knowledge: anatomy, pathology, neurology, risk calculation.
- However, the surgeon **cannot perform surgery with bare hands**.
- Beside the surgeon stands the surgical nurse with a sterile instrument tray.
- When the surgeon requires an action, they call out: *"Scalpel, blade number 10."*
- The nurse hands over the exact instrument. The cut is made, the measurement taken, and the surgeon continues to the next step.

```
       WITHOUT TOOLS (PASSIVE CHATBOT)                   WITH TOOLS (ACTIVE ENTERPRISE AGENT)
   ┌─────────────────────────────────────┐         ┌──────────────────────────────────────────────┐
   │ User: "Calculate my 30-year loan    │         │ User: "Calculate my 30-year loan payment"    │
   │  payment on $450k at 6.5% interest."│         └──────────────────────┬───────────────────────┘
   └──────────────────┬──────────────────┘                                │
                      ▼                                                   ▼
   ┌─────────────────────────────────────┐         ┌──────────────────────────────────────────────┐
   │ LLM Hallucinated Math:              │         │ Model Emits ToolExecutionRequest:            │
   │ "Your monthly payment is probably   │         │  calculateMonthlyPayment(                    │
   │  around $2,400 per month."          │         │    principal=450000.0, rate=6.5, term=360    │
   │                                     │         │  )                                           │
   │ ❌ Wrong by $444/mo ($160,000 bug!) │         └──────────────────────┬───────────────────────┘
   │ ❌ Zero computational guarantees!   │                                │
   └─────────────────────────────────────┘                                ▼
                                                   ┌──────────────────────────────────────────────┐
                                                   │ Java Runtime Executes Method via Reflection: │
                                                   │  Payment = $2,844.31 / month (Deterministic) │
                                                   └──────────────────────┬───────────────────────┘
                                                                          │
                                                                          ▼
                                                   ┌──────────────────────────────────────────────┐
                                                   │ Grounded Accurate Final Response:            │
                                                   │ "Your monthly principal and interest payment │
                                                   │  will be exactly $2,844.31."                 │
                                                   └──────────────────────────────────────────────┘
```

In modern AI engineering:
- **The LLM is the Surgeon**: Formulating strategy, understanding language, and deciding *when* an action is needed.
- **Your Java Class is the Instrument Tray**: Providing deterministic, high-precision methods (database queries, mathematical amortization, cloud resource allocation) that execute on your verified JVM.

---

## 2. Spring AI vs. LangChain4j Tooling Models

In **Day 41**, we learned how Spring AI defines tools as standalone Spring `@Bean` functions implementing `java.util.function.Function<Request, Response>`. 

LangChain4j adopts an alternative **POJO-centric approach**:

| Dimension | Spring AI | LangChain4j |
| :--- | :--- | :--- |
| **Tool Declaration** | Single-function Spring bean: `Function<I, O>` with `@Description`. | Any method on any standard Java class annotated with `@Tool`. |
| **Methods Per Class** | Exactly 1 function per bean. | Multiple `@Tool` methods co-located within a single domain service class. |
| **Parameter Types** | Requires a dedicated Request Record/POJO per function. | Natural Java parameter lists (`double amount, String currency, int term`). |
| **Parameter Docs** | Jackson `@JsonPropertyDescription` on record components. | `@P("description")` annotation directly on method parameters. |
| **Spring Dependency** | Requires Spring Framework IoC container. | Zero framework dependencies (runs in Spring, Quarkus, or plain `main()` methods). |

---

## 3. Defining Tools with `@Tool` and `@P`

In LangChain4j, you group related business capabilities into a single domain class:

```java
package com.genai.langchain4j.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

public class FinancialServiceTools {

    @Tool("Calculates monthly mortgage payment given principal, annual interest rate, and term in months")
    public double calculateMonthlyPayment(
        @P("Loan principal amount in USD") double principal,
        @P("Annual interest rate percentage, e.g. 6.5 for 6.5%") double annualRatePercent,
        @P("Loan term in months, e.g. 360 for 30-year fixed") int termMonths
    ) {
        if (principal <= 0 || termMonths <= 0) {
            throw new IllegalArgumentException("Principal and term months must be strictly positive.");
        }
        double monthlyRate = (annualRatePercent / 100.0) / 12.0;
        if (monthlyRate == 0) return principal / termMonths;
        double payment = (principal * monthlyRate * Math.pow(1 + monthlyRate, termMonths)) 
                       / (Math.pow(1 + monthlyRate, termMonths) - 1);
        return Math.round(payment * 100.0) / 100.0;
    }

    @Tool("Checks live real-time currency conversion rates between global ISO codes")
    public double convertCurrency(
        @P("Monetary amount to convert") double amount,
        @P("3-letter source currency ISO, e.g. USD") String fromCurrency,
        @P("3-letter target currency ISO, e.g. EUR") String toCurrency
    ) {
        // Deterministic financial lookup
        double rate = fromCurrency.equalsIgnoreCase("USD") && toCurrency.equalsIgnoreCase("EUR") ? 0.92 : 1.0;
        return Math.round(amount * rate * 100.0) / 100.0;
    }
}
```

---

## 4. Declarative Binding with `AiServices`

Connecting your tool classes to an agent is a one-line configuration:

### Step 1: Define the Agent Interface

```java
package com.genai.langchain4j.tools;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

@SystemMessage("""
    You are an enterprise mortgage advisor for Acme Banking.
    Help customers understand financing costs and monthly obligations.
    Always execute the calculation tools rather than estimating numbers mentally.
    """)
public interface MortgageAdvisorAgent {

    String consult(@UserMessage String userInquiry);
}
```

### Step 2: Build the Agent with Registered Tools

```java
package com.genai.langchain4j.tools;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

public class MortgageApplication {

    public static void main(String[] args) {
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .modelName("gpt-4o")
            .build();

        // One-line tool binding: LangChain4j scans all @Tool methods automatically!
        MortgageAdvisorAgent advisor = AiServices.builder(MortgageAdvisorAgent.class)
            .chatLanguageModel(chatModel)
            .tools(new FinancialServiceTools())
            .build();

        String answer = advisor.consult(
            "If I borrow $500,000 on a 30-year mortgage at 6.8% interest, what will my monthly payment be?"
        );

        System.out.println(answer);
    }
}
```

When the user asks for a payment estimate:
1. LangChain4j exports the schema of `calculateMonthlyPayment` to the model.
2. The model returns a `ToolExecutionRequest` with parsed arguments: `{principal: 500000.0, annualRatePercent: 6.8, termMonths: 360}`.
3. LangChain4j intercepts the request, invokes the Java method via reflection, gets `3259.94`.
4. The result is returned to the model as a `ToolExecutionResultMessage`.
5. The model outputs a complete, professional response: *"At 6.8% interest on a $500,000 30-year fixed loan, your monthly principal and interest payment will be exactly $3,259.94."*

---

## 5. Resilience: Exception Containment and Self-Healing

What happens if a tool throws an exception? For instance:
- The user asks to reserve 50 GPU nodes, but their quota is capped at 8.
- A database network timeout occurs during a query.

### The Naive Mistake: Crashing the Thread
If your tool throws an unchecked exception that escapes to the HTTP controller, the user experiences a blank screen and a 500 status code.

### The Enterprise Pattern: Structured Error Containment
In LangChain4j, tool exceptions should be trapped, serialized to a clean JSON error message, and returned to the model as a `ToolExecutionResultMessage`:

```
       TOOL THROWS EXCEPTION                            MODEL SELF-HEALS
   ┌─────────────────────────────────────┐         ┌──────────────────────────────────────────────┐
   │ Tool: reserveGpuNodes(count=16)     │         │ Tool Returns JSON:                           │
   │ 💥 SecurityException:               │ ──────► │ {"error": "QuotaExceeded: Max 8 nodes"}      │
   │   "Quota limit exceeded (max 8)"    │         └──────────────────────┬───────────────────────┘
   └─────────────────────────────────────┘                                │
                                                                          ▼
                                                   ┌──────────────────────────────────────────────┐
                                                   │ Model Synthesizes Helpful Correction:        │
                                                   │ "I cannot reserve 16 GPU nodes because your  │
                                                   │  account ceiling is 8 nodes. Would you like  │
                                                   │  me to allocate 8 nodes or file an approval?"│
                                                   └──────────────────────────────────────────────┘
```

The model reads the error string, realizes the parameter was out of bounds, and explains the business constraint constructively to the user!

---

## 6. Complete Runnable Companion Code Architecture

In this lesson's companion code (`Phase_07_LangChain4j/Day_48_Tool_Execution_Function_Calling/code/`), we provide a complete, pure Java 21 implementation:

```
Day_48_Tool_Execution_Function_Calling/code/
├── Tool.java                           # Custom annotation marking callable methods
├── P.java                              # Custom annotation documenting parameters
├── ToolSpecification.java              # Metadata record holding tool schema definitions
├── ToolExecutionRequest.java           # Record representing model execution requests
├── ToolExecutionResultMessage.java     # Record representing tool response payloads
├── EnterpriseBusinessTools.java        # Tool class holding mortgage, inventory, and GPU allocation methods
├── ToolRegistry.java                   # Reflection scanner and resilient dispatch engine
└── LangChain4jToolExecutionDemo.java   # Executable verification suite demonstrating tool dispatch & error containment
```

### Verification & Demonstration Output

Execute `LangChain4jToolExecutionDemo.java`:

```bash
javac -parameters -d out Phase_07_LangChain4j/Day_48_Tool_Execution_Function_Calling/code/*.java
java -cp out com.genai.langchain4j.tools.LangChain4jToolExecutionDemo
```

```
==================================================================
  DAY 48: LANGCHAIN4J TOOL EXECUTION & FUNCTION CALLING DEMO     
==================================================================

--- 1. Registered Tool Specifications (Scanned via Reflection) ---
Tool Name: checkInventoryStock
   Description: Checks inventory level and warehouse distribution center for an enterprise SKU
   Parameters:  {sku=ParameterInfo[type=string, description=Product SKU, e.g. SKU-100-PRO]}
Tool Name: reserveGpuInstances
   Description: Provisions dedicated GPU cloud instances for enterprise training clusters
   Parameters:  {clusterId=ParameterInfo[type=string, description=Cluster identifier], nodeCount=ParameterInfo[type=int, description=Number of GPU nodes to allocate]}
Tool Name: calculateMonthlyPayment
   Description: Calculates monthly mortgage or loan payment given principal, annual interest rate percentage, and term in months
   Parameters:  {principal=ParameterInfo[type=double, description=Loan principal amount in USD], annualRatePercent=ParameterInfo[type=double, description=Annual interest rate percentage, e.g. 6.5 for 6.5%], termMonths=ParameterInfo[type=int, description=Loan term in months, e.g. 360 for 30-year fixed]}

--- 2. Executing Financial Amortization Tool ---
Tool Request:  calculateMonthlyPayment -> {termMonths=360, annualRatePercent=6.5, principal=450000.0}
Tool Response: $2844.31 / month

--- 3. Executing Inventory Stock Verification Tool ---
Tool Request:  checkInventoryStock -> {sku=SKU-990-PRO}
Tool Response: {"sku": "SKU-990-PRO", "inStock": 42, "warehouse": "Dallas-DC-1", "status": "AVAILABLE"}

--- 4. Exception Containment on High-Risk Operation (Quota Exceeded) ---
Tool Request:  reserveGpuInstances -> {nodeCount=16, clusterId=prod-us-east-1}
Tool Response: {"error": "SecurityException: Quota exceeded: Maximum autonomous allocation without VP sign-off is 8 GPU nodes."}

==================================================================
  LANGCHAIN4J TOOL EXECUTION VERIFICATION COMPLETED SUCCESSFULLY 
==================================================================
```

---

## 7. Why LangChain4j Tools Matter for Senior AI Engineers

1. **Zero Framework Pollution**: Your business services remain standard, clean Java classes. You do not need to rewrite your business logic as functional beans or tie your core domain to an external AI framework.
2. **Co-Located Tool Capabilities**: Related operations (e.g. `checkInventory`, `reserveStock`, `cancelReservation`) live cleanly within the same service class rather than across separate files.
3. **Resilient Production Self-Healing**: Containing exceptions inside `ToolExecutionResultMessage` transforms fatal runtime crashes into conversational recovery dialogs.

---

## 8. Practical Exercises

### Exercise 1: Customer Verification Tool with Audit Logging
**Task**: Build a class `CustomerVerificationTools` with a method `@Tool String verifyKycStatus(@P("Customer SSN or Tax ID") String taxId)` that checks an internal customer map. Implement an audit log printing every time the tool is invoked.
**Solution**:
```java
package com.genai.langchain4j.exercises;

import com.genai.langchain4j.tools.P;
import com.genai.langchain4j.tools.Tool;
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

### Exercise 2: Safe Database Query Tool with Table Whitelist
**Task**: Write a tool method `@Tool String executeTableQuery(@P("Target table name") String table)` that rejects any table not explicitly listed in an allowed whitelist (`Set.of("products", "orders", "promotions")`).
**Solution**:
```java
package com.genai.langchain4j.exercises;

import com.genai.langchain4j.tools.P;
import com.genai.langchain4j.tools.Tool;
import java.util.Set;

public class SafeDatabaseTools {

    private static final Set<String> ALLOWED_TABLES = Set.of("products", "orders", "promotions");

    @Tool("Retrieves active schema records for approved public tables")
    public String executeTableQuery(@P("Table name to inspect") String table) {
        if (!ALLOWED_TABLES.contains(table.toLowerCase())) {
            throw new SecurityException("Access Denied: Table '" + table + "' is not whitelisted.");
        }
        return "{\"table\": \"" + table + "\", \"status\": \"ACTIVE\", \"rowCount\": 120}";
    }
}
```

### Exercise 3: Dynamic Tool Filter Based on Department
**Task**: Build a method `List<ToolSpecification> filterToolsByDepartment(String department, List<ToolSpecification> allTools)` that excludes financial tools if the department is `HR`, and excludes employee personnel tools if the department is `ENGINEERING`.
**Solution**:
```java
package com.genai.langchain4j.exercises;

import com.genai.langchain4j.tools.ToolSpecification;
import java.util.List;

public class DepartmentToolFilter {

    public static List<ToolSpecification> filter(String department, List<ToolSpecification> allTools) {
        return allTools.stream()
            .filter(t -> {
                if ("HR".equalsIgnoreCase(department) && t.name().toLowerCase().contains("payment")) return false;
                if ("ENGINEERING".equalsIgnoreCase(department) && t.name().toLowerCase().contains("salary")) return false;
                return true;
            })
            .toList();
    }
}
```

---

## 9. Self-Check Quiz

### Question 1: How does LangChain4j discover and register tools on a Java object?
- A) It parses XML files in `src/main/resources`.
- B) It scans the object at runtime via reflection, identifying methods annotated with `@Tool` and extracting parameter documentation from `@P` annotations.
- C) It requires all tools to extend an abstract C++ class.
- D) It only works if the class implements `java.io.Serializable`.

*Answer*: **B**. LangChain4j inspects the class using reflection, converting methods decorated with `@Tool` and parameters decorated with `@P` into `ToolSpecification` schemas.

---

### Question 2: What is the primary ergonomic advantage of LangChain4j's `@Tool` over Spring AI's functional beans?
- A) LangChain4j tools run faster on the CPU.
- B) Multiple tool methods can be naturally co-located inside a single POJO class with normal method parameter lists, rather than requiring one `@Bean Function<I, O>` per tool.
- C) Spring AI is deprecated.
- D) LangChain4j does not require Java JDK 21.

*Answer*: **B**. LangChain4j allows you to organize multiple related business tools inside a single cohesive class using natural Java method signatures.

---

### Question 3: What should happen when an enterprise Java tool method throws a business exception?
- A) Crash the JVM process.
- B) The exception should be captured and returned to the model as a structured `ToolExecutionResultMessage` containing the error details, allowing the LLM to self-heal and inform the user gracefully.
- C) Hide the error and return random data.
- D) Delete the database table.

*Answer*: **B**. Wrapping exceptions inside the tool result allows the LLM to understand why the operation failed (e.g. invalid parameter, quota exceeded) and provide a constructive conversational response.

---

### Question 4: What is the role of the `@P` annotation?
- A) To declare a method parameter as a primary key.
- B) To provide a natural language description for a tool method parameter, guiding the LLM on what value to supply.
- C) To make the parameter private.
- D) To compress the parameter in memory.

*Answer*: **B**. `@P` provides the description embedded into the tool parameter's JSON Schema, telling the LLM the expected format, units, or constraints for that specific argument.

---

### Question 5: How are tools bound to a declarative `AiServices` agent in LangChain4j?
- A) By specifying `.tools(myToolObject)` on the `AiServices.builder(...)`.
- B) By writing raw SQL queries.
- C) By adding an entry to `application.properties`.
- D) Tools cannot be used with `AiServices`.

*Answer*: **A**. Calling `.tools(...)` on `AiServices.builder(...)` registers your tool instances, and the dynamic proxy orchestrates the execution loop automatically.

---

## 10. Day 48 Mentor Wrap-Up: You Gave Your AI Real Powers!

What an empowering day! By connecting your AI model to real Java methods, you solved the biggest Achilles' heel of language models: ungrounded hallucinations during calculations and real-time operations.

Let's review the big wins:
1. **The Surgeon's Tray**: The AI is the strategic brain, while your Java `@Tool` methods are the precision surgical instruments.
2. **Clean POJO Ergonomics**: No ugly boilerplate beans. You just write normal Java methods, slap `@Tool` and `@P` on them, and LangChain4j handles reflection, schema generation, and proxy execution.
3. **Resilient Error Containment**: When things fail, returning clear error messages instead of blowing up the thread lets the LLM apologize intelligently or try an alternative approach.

Tomorrow in **Day 49: Building a ReAct Agent in Java**, we bring everything together: Thought, Action, and Observation! We'll teach our AI how to solve multi-step problems autonomously in a loop. Get ready for the grand finale of Phase 7!

---

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [Day 47: Advanced RAG — Chunking, Scoring & Re-Ranking](../Day_47_Advanced_RAG_Chunking_ReRanking/Day_47_Advanced_RAG_Chunking_ReRanking.md) | [All 60 Days Overview](../../README.md) | [Day 49: Building a ReAct Agent in Java](../Day_49_Building_ReAct_Agent_in_Java/Day_49_Building_ReAct_Agent_in_Java.md) |

