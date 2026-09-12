# Day 49: Building a ReAct Agent in Java

[← Previous: Day 48 - Tool Execution](../Day_48_Tool_Execution_Function_Calling/Day_48_Tool_Execution_Function_Calling.md) | [Next: Day 50 - Model Context Protocol (MCP) →](../../Phase_08_Enterprise_Production/Day_50_Model_Context_Protocol_MCP/Day_50_Model_Context_Protocol_MCP.md)

---

## 1. Topic Overview
The ReAct (Reasoning + Acting) architecture enables autonomous AI agents to solve complex, multi-step business objectives by dynamically interleaving verbal thought traces with deterministic Java tool execution and environment observation feedback loops. In enterprise Java systems, ReAct agents transform static question-answering systems into proactive problem solvers equipped with cycle-detection guards, iteration limits, and Human-in-the-Loop authorization gates.

---

## 2. Basic Foundations (True Zero)

### What is an Autonomous ReAct Agent?
In simple tool calling (Day 48), the interaction is typically single-turn: the user asks a question, the LLM calls one tool, and the answer is returned.

However, real-world business problems require multiple connected steps:
*"Customer #881 was overcharged on order #991. Check the tracking delay, calculate the refund, issue the credit to their wallet, and email them a confirmation."*

An AI cannot solve this in one step. It must execute a **ReAct loop**:
1. **Thought**: The model plans its next logical step (*"First, I need to fetch the order details to find the tracking number."*).
2. **Action**: The model requests execution of a local Java tool (*`fetchOrder(orderId="ORD-991")`*).
3. **Observation**: Your Java method runs and returns the live data (*`{trackingNumber: "TRK-7712", customerId: "CUST-881"}`*).
4. **Next Thought**: The model processes the observation and determines the next action (*"The tracking number is TRK-7712. Now I must query carrier telemetry."*).
5. **Repeat**: It continues this cycle until the entire goal is solved, at which point it delivers the final report.

### Relatable Physical Analogy: Sherlock Holmes and the Clue-by-Clue Investigation
Imagine master detective Sherlock Holmes investigating a safe robbery:
- **Blind Action (No Reasoning)**: An inexperienced officer kicks down random doors and arrests bystanders without a plan, wasting resources and failing to find the culprit.
- **Armchair Philosopher (No Action)**: A theorist sits in an armchair speculating about who *might* have cracked the safe, producing plausible-sounding hallucinations without verified evidence.
- **Sherlock Holmes (ReAct: Reasoning + Acting)**:
  1. *Thought*: "The safe was opened without damage. Let's check the midnight badge access logs."
  2. *Action*: Query the electronic security log.
  3. *Observation*: "Only Maintenance Specialist John Doe swiped in between 2 AM and 4 AM."
  4. *Thought*: "John Doe had physical access. Does he have financial motive? Let's check his bank ledger."
  5. *Action*: Query bank records.
  6. *Observation*: "A sudden $50,000 cash deposit arrived yesterday."
  7. *Thought*: "Both access and motive are proven. Indictment ready."
  8. *Final Resolution*: Case closed with 100% verified evidence.

### Minimal Beginner-Friendly Working Code
In Java, an agent is fundamentally a `while` loop that coordinates Thought, Action, and Observation:

```java
package com.genai.langchain4j.react;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public class SimpleReActRunner {

    // 1. Declare autonomous agent interface
    public interface CustomerSupportAgent {
        @SystemMessage("""
            You are an autonomous customer support claims investigator.
            Reason step-by-step. Use your tools to verify facts, inspect shipping delays,
            and issue refunds. Deliver a clear final summary when all actions are complete.
            """)
        String resolveIssue(@UserMessage String customerComplaint);
    }

    public static void main(String[] args) {
        ChatLanguageModel model = OpenAiChatModel.builder()
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .modelName("gpt-4o")
            .temperature(0.1) // Low temperature for deterministic planning
            .build();

        // 2. LangChain4j automatically orchestrates the multi-turn ReAct loop when tools are registered!
        CustomerSupportAgent agent = AiServices.builder(CustomerSupportAgent.class)
            .chatLanguageModel(model)
            .tools(new OrderManagementTools()) // Co-located business tools
            .build();

        // 3. The agent executes multi-step tool calls autonomously until the goal is solved!
        String resolution = agent.resolveIssue(
            "Order ORD-991 arrived late. Please investigate the delay and issue a courtesy credit if delayed over 48 hours."
        );

        System.out.println("Final Agent Resolution:\n" + resolution);
    }
}
```

### Line-by-Line Walkthrough
1. **`CustomerSupportAgent`**: Declarative `AiServices` interface with an operational system prompt instructing the model to think step-by-step.
2. **`temperature(0.1)`**: Deterministic temperature prevents the model from diverging into creative or unpredictable tool call loops.
3. **`builder.tools(new OrderManagementTools())`**: Binds multiple tool methods. Under the hood, LangChain4j maintains the multi-turn `Thought -> Action -> Observation` loop until the model outputs a final conversational text response.
4. **`agent.resolveIssue(...)`**: The model calls `fetchOrder`, reads the tracking ID, calls `checkCarrierTracking`, verifies the 72-hour delay, executes `issueWalletCredit`, and formats the final resolution summary.

---

## 3. Core Concept Walkthrough (Basic → Intermediate)

```
+-------------------------------------------------------------------------------+
|                       THE REACT STATE MACHINE IN JAVA                         |
+-------------------------------------------------------------------------------+
|                                                                               |
|  User Goal Submitted: "Investigate delayed order ORD-991 and credit account"  |
|         |                                                                     |
|         v                                                                     |
|  +-------------------------------------------------------------------------+  |
|  | AUTONOMOUS WHILE LOOP (iteration < MAX_ITERATIONS)                      |  |
|  |                                                                         |  |
|  | 1. GENERATE THOUGHT                                                     |  |
|  |    "Need to fetch order details for ORD-991 first."                     |  |
|  |                                                                         |  |
|  | 2. SELECT ACTION (Tool Call)                                            |  |
|  |    execute: fetchOrder(orderId="ORD-991")                               |  |
|  |                                                                         |  |
|  | 3. SAFETY CHECKS                                                        |  |
|  |    • Loop-Trap Detector: Did we just run this identical call?           |  |
|  |    • Quota / HITL Gate: Does this require human approval?               |  |
|  |                                                                         |  |
|  | 4. EXECUTE JAVA METHOD VIA REFLECTION                                   |  |
|  |    Java returns: {"tracking": "TRK-7712", "customerId": "CUST-881"}     |  |
|  |                                                                         |  |
|  | 5. CAPTURE OBSERVATION & APPEND TO CONTEXT                              |  |
|  |    Context augmented with tool return payload. Repeat loop!             |  |
|  +-------------------------------------------------------------------------+  |
|         |                                                                     |
|         v (When model decides no further tools are needed)                    |
|  FINAL ANSWER SYNTHESIS: "Order ORD-991 was delayed 72h; $50 credit issued."  |
+-------------------------------------------------------------------------------+
```

### The 4 Pillars of Every ReAct Iteration
1. **Goal**: The high-level objective assigned to the agent.
2. **Thought**: The explicit reasoning step explaining *why* the agent is choosing an action based on all previous observations.
3. **Action**: The specific Java method name and argument payload to execute.
4. **Observation**: The deterministic data returned by the Java runtime, appended to conversational memory for the next turn.

---

## 4. Prerequisite & Supporting Concepts

### Prerequisite / Supporting Concept: Under the Hood of an Autonomous Agent
Many engineers assume autonomous agents require complex distributed architectures. In pure Java, an agent engine is simply an iterative loop:

```java
package com.genai.langchain4j.react;

import java.util.ArrayList;
import java.util.List;

public class BasicAgentLoopEngine {

    private static final int MAX_ITERATIONS = 6;

    public record Step(String thought, String toolName, String observation) {}

    public String runAgent(String userGoal) {
        List<Step> executionHistory = new ArrayList<>();
        int iterations = 0;

        while (iterations++ < MAX_ITERATIONS) {
            // 1. LLM plans thought and action based on goal + history
            AgentDecision decision = callModel(userGoal, executionHistory);

            if (decision.isFinished()) {
                return decision.finalAnswer(); // Goal reached!
            }

            // 2. Dispatch local Java method
            String observation = executeMethod(decision.toolName(), decision.toolArgs());

            // 3. Record step and repeat
            executionHistory.add(new Step(decision.thought(), decision.toolName(), observation));
        }

        return "Execution halted: Max iterations ceiling reached.";
    }

    private record AgentDecision(boolean isFinished, String thought, String toolName, String toolArgs, String finalAnswer) {}
    private AgentDecision callModel(String goal, List<Step> history) { return null; }
    private String executeMethod(String name, String args) { return ""; }
}
```

---

## 5. Advanced Depth (Intermediate → Advanced)

### The 3 Essential Production Safety Guards

```
+-------------------------------------------------------------------------------+
|                       ENTERPRISE AGENT SAFETY GUARDS                          |
+-------------------------------------------------------------------------------+
|                                                                               |
|  Incoming Agent Action                                                        |
|         |                                                                     |
|         v                                                                     |
|  [ Guard 1: Maximum Iterations Limit ]                                        |
|  if (iteration >= 10) -> Terminate immediately with MAX_ITERATIONS_EXCEEDED   |
|         |                                                                     |
|         v                                                                     |
|  [ Guard 2: Loop-Trap Repetition Detector ]                                   |
|  Track signature: toolName + arguments. If repeated consecutively 2x with     |
|  the same error -> Terminate with LOOP_TRAP_DETECTED                          |
|         |                                                                     |
|         v                                                                     |
|  [ Guard 3: Human-in-the-Loop (HITL) Gate ]                                   |
|  if (action == 'issueCredit' && amount > $500.0) ->                           |
|  Pause execution; return PENDING_HUMAN_APPROVAL with authorization token      |
|         |                                                                     |
|         v                                                                     |
|  Safe Java Execution                                                          |
+-------------------------------------------------------------------------------+
```

### 1. Loop-Trap / Repetition Cycle Detector
A common failure mode is an agent getting stuck in a repetitive loop (e.g., calling `fetchOrder("ORD-UNKNOWN")`, receiving `{"error": "Not Found"}`, and immediately repeating the identical call on the next turn).

```java
package com.genai.langchain4j.react;

import java.util.Map;

public class LoopTrapDetector {

    private String lastActionSignature = null;
    private int consecutiveRepeatCount = 0;

    public boolean isTrapped(String toolName, Map<String, Object> args) {
        String currentSignature = toolName + ":" + (args != null ? args.toString() : "");

        if (currentSignature.equals(lastActionSignature)) {
            consecutiveRepeatCount++;
            if (consecutiveRepeatCount >= 2) {
                return true; // Loop trap detected!
            }
        } else {
            lastActionSignature = currentSignature;
            consecutiveRepeatCount = 1;
        }

        return false;
    }
}
```

### 2. Audit Trail Generation for Enterprise Compliance
Regulated enterprise environments (banking, healthcare) require complete audit trails explaining why an autonomous agent performed a given action:

```java
package com.genai.langchain4j.react;

import java.util.List;

public final class AuditTrailRenderer {

    private AuditTrailRenderer() {}

    public record AuditStep(int stepNumber, String thought, String action, String observation) {}

    public static String renderMarkdownAuditLog(String goal, List<AuditStep> steps, String finalAnswer) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Autonomous Agent Execution Audit Log\n\n");
        sb.append("**Goal**: ").append(goal).append("\n\n");
        sb.append("| Step | Thought | Action | Observation |\n");
        sb.append("|:-----|:--------|:-------|:------------|\n");

        for (AuditStep s : steps) {
            sb.append(String.format("| %d | *%s* | `%s` | `%s` |\n",
                s.stepNumber(),
                s.thought().replace("\n", " "),
                s.action(),
                s.observation().replace("\n", " ")
            ));
        }

        sb.append("\n**Final Resolution**:\n").append(finalAnswer).append("\n");
        return sb.toString();
    }
}
```

### Common Anti-Patterns & Production Traps

| Anti-Pattern | Why It Breaks in Production | Correct Architectural Solution |
|:---|:---|:---|
| **Unbounded While Loops** | If the model cannot satisfy a sub-goal, it runs indefinitely, draining thousands of dollars in cloud API tokens. | Enforce a strict iteration ceiling (`maxIterations = 6 to 10`). |
| **No Action Signature Tracking** | When a tool returns `404 Not Found`, naive agents repeat the identical request repeatedly in a loop trap. | Use `LoopTrapDetector` to halt and alert when consecutive duplicate calls occur. |
| **Autonomous High-Value Transactions** | Letting an agent autonomously wire money or delete records without human approval creates catastrophic legal risk. | Implement **Human-in-the-Loop (HITL)** gates for state mutations above defined dollar or impact ceilings. |

---

## 6. Quick Recap
- **ReAct (Reasoning + Acting)** alternates between verbal reasoning (*Thoughts*), external Java method execution (*Actions*), and feedback telemetry (*Observations*).
- An autonomous AI agent in Java is fundamentally an **iterative while-loop** with explicit state management and exit conditions.
- LangChain4j's **`AiServices`** automatically implements the multi-step ReAct loop when multiple `@Tool` classes are attached.
- Production systems require **Iteration Caps** (e.g., max 6–10 turns) to prevent runaway billing.
- **Loop-Trap Detectors** identify consecutive identical tool calls and halt runaway cycles.
- High-risk operations must pause execution and request human authorization tokens via **Human-in-the-Loop (HITL)** gateways.

---

## 7. Self-Check Questions & Practice Exercises

### 5-Question Self-Check Quiz

#### Question 1
What is the core innovation of the ReAct (Reasoning + Acting) architecture?
- A) It replaces GPUs with specialized TPUs.
- B) It interleaves verbal reasoning traces (Thoughts) with discrete tool execution (Actions), using environment observations to condition subsequent reasoning until a goal is achieved.
- C) It requires models to be trained exclusively in Java.
- D) It bypasses token fees by caching text locally.

#### Question 2
In a ReAct agent loop, what is an "Observation"?
- A) A comment written by a human tester in a pull request review.
- B) The return value resulting from executing the selected Java tool method, fed back into the agent's context for the next reasoning step.
- C) An error logged in the JVM console.
- D) A metric sent to Prometheus.

#### Question 3
Why is a "Loop Trap Detector" necessary in autonomous agent execution?
- A) To detect when the server CPU fan is spinning too fast.
- B) To detect when an agent repeatedly executes the exact same tool with the exact same arguments in a futile cycle, preventing infinite runaway loops and API billing surges.
- C) To restart the Spring Boot application container.
- D) Loop trap detectors are deprecated.

#### Question 4
How does LangChain4j support autonomous ReAct agent loops declaratively?
- A) By compiling C++ source files.
- B) Through `AiServices`: when multiple tools are bound to an interface, the dynamic proxy automatically executes tools, feeds observations back, and re-invokes the model until a final text answer is reached.
- C) By requiring developers to write 500 lines of custom socket code.
- D) It only supports single-turn interactions.

#### Question 5
When should a Human-in-the-Loop (HITL) safeguard be triggered in an agent architecture?
- A) On every single keystroke.
- B) Before executing state-mutating, irreversible, or high-risk actions (e.g., wiring funds exceeding a threshold, deleting database records, dispatching unapproved emails).
- C) Only during unit tests.
- D) Never, because autonomous agents should be 100% unsupervised.

---

### Quiz Answers & Explanations
1. **B**: ReAct combines reasoning (planning and goal tracking) with acting (executing external tools), creating an adaptive feedback loop that prevents hallucinations and blind tool calls.
2. **B**: An Observation is the environmental data returned by the Java tool (e.g., database rows, API status, math calculations) that grounds the model's next thought.
3. **B**: Models can become trapped in repetitive cycles when a tool returns an error. A loop trap detector recognizes identical repeated actions and safely terminates execution.
4. **B**: LangChain4j's `AiServices` handles multi-step tool calling transparently, continuing the execution loop until the model decides the goal is achieved.
5. **B**: High-stakes enterprise actions must be intercepted by safety policies, requiring human authorization tokens before destructive mutations can be committed.

---

### Hands-On Practice Exercises

#### Exercise 1: Step Counter and Timeout Guardrail
**Problem Statement**:  
Build a wrapper class `GuardedAgentExecutor` that takes an agent and enforces both a maximum iteration count and a maximum total execution duration in milliseconds using `System.currentTimeMillis()`.

<details>
<summary>👉 View Solution</summary>

```java
package com.genai.langchain4j.exercises;

import java.time.Duration;

public class GuardedAgentExecutor {

    private final int maxIterations;
    private final Duration maxTimeout;

    public GuardedAgentExecutor(int maxIterations, Duration maxTimeout) {
        this.maxIterations = maxIterations;
        this.maxTimeout = maxTimeout;
    }

    public boolean isExecutionAllowed(int currentIteration, long startTimestampMs) {
        if (currentIteration >= maxIterations) return false;
        long elapsed = System.currentTimeMillis() - startTimestampMs;
        return elapsed < maxTimeout.toMillis();
    }
}
```
</details>

#### Exercise 2: Action Signature Serializer for Loop Detection
**Problem Statement**:  
Write a utility method `String computeActionSignature(String toolName, Map<String, Object> arguments)` that produces a normalized, sorted signature string for cycle detection.

<details>
<summary>👉 View Solution</summary>

```java
package com.genai.langchain4j.exercises;

import java.util.*;

public class ActionSignatureUtils {

    public static String computeActionSignature(String toolName, Map<String, Object> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return toolName + "()";
        }
        List<String> keys = new ArrayList<>(arguments.keySet());
        Collections.sort(keys);

        StringBuilder sb = new StringBuilder(toolName).append("(");
        for (int i = 0; i < keys.size(); i++) {
            String k = keys.get(i);
            sb.append(k).append("=").append(arguments.get(k));
            if (i < keys.size() - 1) sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }
}
```
</details>

---

[← Previous: Day 48 - Tool Execution](../Day_48_Tool_Execution_Function_Calling/Day_48_Tool_Execution_Function_Calling.md) | [Next: Day 50 - Model Context Protocol (MCP) →](../../Phase_08_Enterprise_Production/Day_50_Model_Context_Protocol_MCP/Day_50_Model_Context_Protocol_MCP.md)
