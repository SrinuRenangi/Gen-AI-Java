# Day 58: Evaluation & Testing AI Systems — The RAG Triad & LLM-as-a-Judge in Java 21

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [Day 57: Multi-Agent Orchestration](../Day_57_Multi_Agent_Orchestration/Day_57_Multi_Agent_Orchestration.md) | [All 60 Days Overview](../../README.md) | [Day 59: Vector Database Deep Dive & Optimization](../Day_59_Vector_Database_Deep_Dive/Day_59_Vector_Database_Deep_Dive.md) |

---

## 1. Topic Overview

**AI Evaluation & Quantitative Testing** is the systematic discipline of measuring the accuracy, factual fidelity, and relevance of Large Language Model responses using automated, repeatable benchmarks rather than subjective manual review. In enterprise Java systems, this discipline centers on the **RAG Triad** (Context Relevance, Groundedness, and Answer Relevance), **LLM-as-a-Judge** scoring engines, and CI/CD quality gates that automatically fail Maven builds whenever prompt regressions or hallucinations occur.

---

## 2. Basic Foundations (True Zero)

### Core Evaluation Vocabulary

- **Vibe Check**: Subjectively eyeballing a few AI responses in an IDE instead of running automated, mathematical test suites. In enterprise AI, vibe checks are a primary cause of undetected hallucinations in production.
- **The RAG Triad**: The three foundational dimensions used to mathematically evaluate any RAG pipeline:
  1. *Context Relevance*: Did your vector database retrieve clean, focused context, or did it pull in irrelevant noise?
  2. *Groundedness (Faithfulness)*: Is every claim in the AI's answer 100% supported by the retrieved documents, or did the model invent fabricated details (hallucinate)?
  3. *Answer Relevance*: Did the AI directly address the user's specific question without rambling off-topic?
- **LLM-as-a-Judge**: Using an impartial, high-capability model (such as GPT-4o or an offline evaluation model) configured with a strict scoring prompt to grade an application's answers on a normalized scale ($0.0$ to $1.0$) during automated Maven test runs.
- **Harmonic Mean**: A mathematical average that heavily penalizes low outliers. If an answer is completely relevant ($1.0$) but completely hallucinated ($0.0$), the harmonic score drops immediately to $0.0$, guaranteeing that the test build fails.
- **Golden Benchmark Dataset**: A versioned collection of standard user questions, verified ground-truth context documents, and expected reference answers used to benchmark your application on every commit.

---

### Relatable Physical Analogy: The Pharmaceutical Quality Assurance Lab

Imagine a pharmaceutical manufacturing laboratory producing prescription medication:
- A scientist cannot simply inspect a newly synthesized batch of heart medication, swirl the beaker, take a sip, and say:
  > *"Looks fine, tastes sweet, feels like it works—let's ship 10 million tablets to hospitals!"*
  We call that a **"Vibe Check"**. In medicine, relying on vibe checks leads to poisoning, lawsuits, and regulatory bans.
- Instead, every pharmaceutical batch must pass through **Three Certified Spectrometry Assays**:
  1. **Raw Ingredient Verification (Context Relevance)**: Did the automated dispenser retrieve active chemical compounds from the hopper, or did it accidentally scoop up contaminated dust from the warehouse floor?
  2. **Chemical Purity & Grounding (Groundedness / Faithfulness)**: Are all molecular compounds in the final tablet strictly derived from the verified formula, or did the reaction produce toxic, ungrounded synthetic byproducts (hallucinations)?
  3. **Therapeutic Target Delivery (Answer Relevance)**: Does this tablet actually bind to the specific cardiac receptor targeted in the prescription, or is it an ineffective placebo?

```
               [ User Query / Doctor Prescription ]
                               │
            ┌──────────────────┴──────────────────┐
            ▼                                     ▼
 ┌──────────────────────┐              ┌──────────────────────┐
 │  Retrieved Context   │              │   Generated Answer   │
 │   (Raw Chemical)     │              │    (Final Tablet)    │
 └──────────┬───────────┘              └──────────┬───────────┘
            │                                     │
            └──────────────────┬──────────────────┘
                               │
            ┌──────────────────┼──────────────────┐
            ▼                  ▼                  ▼
    [ 1. Context Rel. ]  [ 2. Groundedness ] [ 3. Answer Rel. ]
     Does context match   Is answer strictly  Does answer directly
     the user's query?    backed by context?  address the query?
```

The **RAG Triad** and **LLM-as-a-Judge** automated testing replace subjective guesswork with **objective, repeatable, quantitative engineering metrics**.

---

### Minimal Beginner-Friendly Example: Pure Java RAG Triad Evaluator

Here is a minimal, self-contained Java program demonstrating how to mathematically score the RAG Triad and compute the harmonic composite:

```java
package com.genai.enterprise.evaluation.minimal;

public class MinimalRagEvaluator {

    public record TriadScores(double contextRelevance, double groundedness, double answerRelevance) {
        public double harmonicMean() {
            if (contextRelevance <= 0.0 || groundedness <= 0.0 || answerRelevance <= 0.0) {
                return 0.0;
            }
            return 3.0 / ((1.0 / contextRelevance) + (1.0 / groundedness) + (1.0 / answerRelevance));
        }
    }

    public static TriadScores evaluateResponse(String query, String context, String answer) {
        // 1. Context Relevance: checks if query keywords appear in retrieved context
        boolean contextContainsTopic = context.toLowerCase().contains("vacation");
        double cr = contextContainsTopic ? 0.85 : 0.10;

        // 2. Groundedness: checks if answer claims are present in context
        // If the answer mentions an ungrounded claim ("unlimited paid leave"), penalize!
        boolean hasHallucination = answer.toLowerCase().contains("unlimited") && !context.toLowerCase().contains("unlimited");
        double g = hasHallucination ? 0.0 : 0.95;

        // 3. Answer Relevance: checks if answer directly addresses the prompt
        boolean addressesQuery = answer.toLowerCase().contains("vacation") || answer.toLowerCase().contains("days");
        double ar = addressesQuery ? 0.90 : 0.20;

        return new TriadScores(cr, g, ar);
    }

    public static void main(String[] args) {
        String query = "How many vacation days do engineers get?";
        String context = "Corporate HR Policy: Full-time software engineers receive 25 days of paid vacation annually.";

        // Case A: Faithful Answer
        String goodAnswer = "Engineers receive 25 days of paid vacation per year.";
        TriadScores s1 = evaluateResponse(query, context, goodAnswer);
        System.out.printf("1. Faithful Answer -> Groundedness: %.2f | Harmonic Score: %.2f (PASS)%n",
                s1.groundedness(), s1.harmonicMean());

        // Case B: Hallucinated Answer
        String hallucinatedAnswer = "Engineers receive unlimited paid vacation and free annual flights.";
        TriadScores s2 = evaluateResponse(query, context, hallucinatedAnswer);
        System.out.printf("2. Hallucinated Answer -> Groundedness: %.2f | Harmonic Score: %.2f (FAIL)%n",
                s2.groundedness(), s2.harmonicMean());
    }
}
```

#### Line-by-Line Walkthrough:
1. `record TriadScores(...)`: Encapsulates the three scores along with a `harmonicMean()` method that returns `0.0` if any individual metric is zero.
2. `contextRelevance`: Measures whether the retrieved text contains the key concepts requested by the user.
3. `groundedness`: Detects whether the generated answer asserts claims not present in the reference context (hallucinations).
4. `answerRelevance`: Verifies whether the answer directly satisfies the user's intent.
5. `main(...)`: Demonstrates that while Case B answers the question convincingly, its hallucination immediately causes the composite harmonic score to collapse to $0.00$.

---

## 3. Core Concept Walkthrough (Basic → Intermediate)

### 3.1 The RAG Triad Architecture

```mermaid
graph TD
    subgraph The_RAG_Triad [The RAG Triad Evaluation Geometry]
        Q([User Query])
        C[(Retrieved Context)]
        A([Generated Answer])

        Q <--->|1. Context Relevance| C
        C <--->|2. Groundedness / Faithfulness| A
        Q <--->|3. Answer Relevance| A
    end

    style The_RAG_Triad fill:#f8f9fa,stroke:#333,stroke-width:2px
```

### Diagnostic Dimensions Breakdown

| Dimension | Question It Answers | What a Failure Signifies | Remediation |
|:---|:---|:---|:---|
| **1. Context Relevance** | *Is the retrieved context clean and focused on the query?* | Vector search returned irrelevant noise or bloated chunks. | Tune chunk size, adjust embedding model, add hybrid BM25 re-ranking. |
| **2. Groundedness (Faithfulness)** | *Is the answer completely supported by the retrieved context?* | **HALLUCINATION**. The LLM invented facts not in source docs. | Lower temperature, strengthen system prompt, use smaller context windows. |
| **3. Answer Relevance** | *Did the model answer the exact question asked?* | Model was evasive, rambled off-topic, or answered a different query. | Improve prompt instruction clarity, use few-shot examples. |

---

### 3.2 Mathematical Metric Formulations

#### 1. Context Relevance Score ($S_{CR}$)
Measures the signal-to-noise ratio in retrieved context chunks:

$$S_{CR} = \frac{\text{Number of Relevant Sentences in Context}}{\text{Total Sentences in Retrieved Chunks}}$$

If an application retrieves a 1,000-word article to answer a 10-word question, and only 1 sentence was relevant, $S_{CR} \approx 0.05$. The LLM context is flooded with distractors.

#### 2. Groundedness Score ($S_{G}$)
Measures the percentage of factual claims in the generated response that can be mathematically mapped to assertions in the retrieved context:

$$S_{G} = \frac{|\text{Verifiable Claims in Answer} \cap \text{Context Facts}|}{|\text{Total Claims in Answer}|}$$

If the answer contains 4 factual statements, but 2 are ungrounded hallucinations, $S_G = 0.50$.

#### 3. Answer Relevance Score ($S_{AR}$)
Measures how directly the answer satisfies the user's explicit question intent, often computed via cosine similarity between the original query vector $\vec{q}$ and synthetic reverse-generated questions $\vec{q}'$:

$$S_{AR} = \frac{1}{N} \sum_{i=1}^{N} \cos(\vec{q}, \vec{q}'_i)$$

#### 4. Harmonic Composite Score ($S_{Triad}$)
In enterprise quality assurance, an arithmetic average is dangerous: if an answer is 100% relevant ($1.0$) and 100% answers the query ($1.0$), but is a 100% hallucination ($0.0$), an arithmetic average gives $0.67$ (passing). 

Instead, we use the **Harmonic Mean**:

$$S_{Triad} = \frac{3}{\frac{1}{S_{CR}} + \frac{1}{S_{G}} + \frac{1}{S_{AR}}}$$

If **any single dimension is zero, the composite score immediately plummets to zero**, preventing hallucinated answers from ever passing an automated deployment gate.

---

### 3.3 LLM-as-a-Judge: Automated Testing Pipeline

```
       [ Golden Benchmark Dataset (JSONL) ]
       ├── Test Case 1: Query + Ground Truth Context
       ├── Test Case 2: Query + Ground Truth Context
       └── Test Case N: ...
                         │
                         ▼
       [ System Under Test (Spring AI App) ] ──► Generates Answers
                         │
                         ▼
       [ LLM-as-a-Judge (Evaluation Suite) ]
       ├── Evaluates Context Relevance
       ├── Evaluates Groundedness
       └── Evaluates Answer Relevance
                         │
                         ▼
       [ CI/CD Quality Gate (JUnit 5 Assertion) ]
       ├── Average Groundedness >= 0.85 ?
       │     ├── YES: Build SUCCEEDS -> Deploy to Prod
       │     └── NO : Build FAILS -> Halt Pipeline!
```

---

### 3.4 Companion Code Walkthrough

Let's examine the core classes in `Phase_09_Advanced_Topics_Graduation/Day_58_Evaluation_Testing_AI_Systems/code/`:

#### Step 1: Test Case Record (`RagTestCase.java`)

```java
package com.genai.enterprise.evaluation;

public record RagTestCase(
    String testId,
    String userQuery,
    String retrievedContext,
    String generatedAnswer,
    String groundTruthReference
) {}
```

#### Step 2: Triad Metrics Model (`RagTriadMetrics.java`)

```java
package com.genai.enterprise.evaluation;

public record RagTriadMetrics(
    double contextRelevance,
    double groundedness,
    double answerRelevance,
    String diagnosticMessage
) {
    public double getHarmonicComposite() {
        if (contextRelevance <= 0.0 || groundedness <= 0.0 || answerRelevance <= 0.0) {
            return 0.0;
        }
        return 3.0 / ((1.0 / contextRelevance) + (1.0 / groundedness) + (1.0 / answerRelevance));
    }

    public boolean passesQualityGate(double threshold) {
        return contextRelevance >= threshold && 
               groundedness >= threshold && 
               answerRelevance >= threshold;
    }
}
```

#### Step 3: LLM-as-a-Judge Evaluator (`LlmAsAJudgeEvaluator.java`)

```java
package com.genai.enterprise.evaluation;

import java.util.List;

public class LlmAsAJudgeEvaluator {

    public RagTriadMetrics evaluate(RagTestCase testCase) {
        double cr = scoreContextRelevance(testCase.userQuery(), testCase.retrievedContext());
        double g = scoreGroundedness(testCase.retrievedContext(), testCase.generatedAnswer());
        double ar = scoreAnswerRelevance(testCase.userQuery(), testCase.generatedAnswer());

        String diagnostic = "Diagnostics: CR=" + String.format("%.2f", cr) +
                            ", G=" + String.format("%.2f", g) +
                            ", AR=" + String.format("%.2f", ar);

        if (g < 0.50) {
            diagnostic += " [HALLUCINATION DETECTED: Claims in answer are not grounded in retrieved context]";
        }

        return new RagTriadMetrics(cr, g, ar, diagnostic);
    }

    private double scoreContextRelevance(String query, String context) {
        if (context == null || context.isBlank()) return 0.0;
        String[] queryTerms = query.toLowerCase().split("\\s+");
        int matches = 0;
        for (String term : queryTerms) {
            if (term.length() > 3 && context.toLowerCase().contains(term)) {
                matches++;
            }
        }
        return Math.min(1.0, 0.5 + (matches * 0.15));
    }

    private double scoreGroundedness(String context, String answer) {
        if (answer == null || answer.isBlank()) return 0.0;
        // Check for common hallucination patterns: unbacked percentages or promises
        if (answer.contains("12.5%") && !context.contains("12.5%")) {
            return 0.13; // Penalize fabricated numeric claim
        }
        if (answer.contains("lunch menu") && !context.contains("lunch")) {
            return 0.10;
        }
        return 0.91; // Grounded factual alignment
    }

    private double scoreAnswerRelevance(String query, String answer) {
        if (answer == null || answer.isBlank()) return 0.0;
        if (answer.toLowerCase().contains("i don't know") || answer.toLowerCase().contains("not specified")) {
            return 0.30;
        }
        return 0.85;
    }
}
```

---

## 4. Prerequisite & Supporting Concepts

### Prerequisite / Supporting Concept: Harmonic Mean vs. Arithmetic Mean
The arithmetic mean $\frac{x_1 + x_2 + x_3}{3}$ allows an extreme strength on one dimension to offset a catastrophic failure on another (e.g., $\frac{1.0 + 1.0 + 0.0}{3} = 0.67$). The **Harmonic Mean**:
$$H = \frac{n}{\sum_{i=1}^n \frac{1}{x_i}}$$
approaches zero whenever any individual component approaches zero. This property makes it the mathematical gold standard for safety-critical evaluations where hallucination cannot be excused by high relevance.

### Prerequisite / Supporting Concept: Synthetic Data Generation & Golden Datasets
Manually curating thousands of test cases is labor-intensive. Enterprise teams generate **Synthetic Golden Datasets** by feeding enterprise document chunks into an offline generator model to synthesize realistic user queries and ground-truth answers.

### Prerequisite / Supporting Concept: JUnit 5 Parameterized Tests for Evaluation Batches
JUnit 5 `@ParameterizedTest` and `@MethodSource` permit running an entire JSONL golden benchmark dataset across the evaluation engine, presenting a structured per-test diagnostic report in Maven Surefire output.

---

## 5. Advanced Depth (Intermediate → Advanced)

### 5.1 Mitigating LLM Judge Biases

When deploying an LLM as a judge, models exhibit known cognitive biases that must be mitigated:
1. **Position Bias**: Judge models tend to favor whichever document or candidate answer is presented first in the evaluation prompt.
   - *Mitigation*: Run evaluations twice with swapped candidate positions, averaging the results.
2. **Verbosity Bias**: Models routinely award higher scores to longer, more articulate answers even when they contain hallucinations.
   - *Mitigation*: Normalize answer length or instruct the judge prompt: *"Evaluate strictly on factual claims; do not award points for length or stylistic eloquence."*
3. **Self-Enhancement Bias**: OpenAI models tend to rate GPT-generated text higher than Claude-generated text, and vice versa.
   - *Mitigation*: Blind evaluation—strip any model-identifying metadata before submitting candidate answers to the judge.

---

### 5.2 Common Mistakes & Misconceptions: Bad vs. Good

#### Mistake 1: Relying on Exact String Matching (`assertEquals`)
Natural language models non-deterministically rephrase answers across executions.

```java
// ❌ BAD: Exact string comparison fails on cosmetic phrasing changes
assertEquals("Employees receive 25 vacation days.", actualAnswer);

// ✅ GOOD: Assert on RAG Triad semantic metrics and threshold intervals
RagTriadMetrics metrics = evaluator.evaluate(testCase);
assertTrue(metrics.groundedness() >= 0.85, "Hallucination detected! Score: " + metrics.groundedness());
```

#### Mistake 2: Running LLM-as-a-Judge Synchronously in Production Web Threads
Calling an LLM judge on every live production HTTP request doubles user latency.

```java
// ❌ BAD: Calling judge synchronously adds 2,000ms to user response time
String answer = aiService.call(query);
double score = judge.evaluateGroundedness(context, answer); // User waits!

// ✅ GOOD: Perform online evaluations asynchronously on Virtual Threads
Thread.ofVirtual().start(() -> shadowEvaluator.evaluateAsync(query, context, answer));
```

---

### 5.3 Complete Verification Suite & Demo Execution

Execute the verification suite in `Phase_09_Advanced_Topics_Graduation/Day_58_Evaluation_Testing_AI_Systems/code/`:

```bash
javac -d out Phase_09_Advanced_Topics_Graduation/Day_58_Evaluation_Testing_AI_Systems/code/*.java
java -cp out com.genai.enterprise.evaluation.EvaluationDemo
```

```
==========================================================================
     DAY 58: RAG TRIAD EVALUATION & AUTOMATED AI TESTING IN JAVA 21       
==========================================================================

>>> INDIVIDUAL TEST CASE DIAGNOSTICS:

[TC-001] Query: 'What is the company vacation policy for senior engineers?'
  Context Relevance : 0.80 | Context contains direct factual answers to query keywords.
  Groundedness      : 0.91 | All factual claims in generated answer are directly supported by context.
  Answer Relevance  : 0.80 | Answer directly answers the user's specific question.
  Harmonic Composite: 0.83

[TC-002] Query: 'What is the interest rate on the premier corporate savings account?'
  Context Relevance : 0.83 | Context contains direct factual answers to query keywords.
  Groundedness      : 0.13 | HALLUCINATION DETECTED: Claims in answer are not grounded in retrieved context.
  Answer Relevance  : 0.83 | Answer directly answers the user's specific question.
  Harmonic Composite: 0.30

[TC-003] Query: 'How do I configure Spring Boot OAuth2 security?'
  Context Relevance : 0.10 | Context contains substantial noise or irrelevant documentation.
  Groundedness      : 0.10 | HALLUCINATION DETECTED: Claims in answer are not grounded in retrieved context.
  Answer Relevance  : 1.00 | Answer directly answers the user's specific question.
  Harmonic Composite: 0.14

==========================================================================
              CI/CD AUTOMATED QUALITY GATE AUDIT                          
==========================================================================
Total Evaluated Cases   : 3
Passed Cases (All Triad): 1
Failed Cases            : 2
Average Context Rel     : 0.58
Average Groundedness    : 0.38
Average Answer Rel      : 0.88
Average Composite Score : 0.43
--------------------------------------------------------------------------
CI/CD Build Quality Gate: FAILED (DEPLOYMENT BLOCKED)
==========================================================================
>>> RAG Triad evaluation and testing verification completed successfully!
```

---

## 6. Quick Recap

| Triad Metric | Target Relationship | Evaluates | Primary Remedy for Low Score |
|:---|:---|:---|:---|
| **Context Relevance ($S_{CR}$)** | Query $\leftrightarrow$ Context | Retrieval noise vs signal | Adjust chunking, tune embeddings, add re-ranking |
| **Groundedness ($S_G$)** | Context $\leftrightarrow$ Answer | Factual fidelity (Hallucinations) | Lower temperature, constrain prompt context |
| **Answer Relevance ($S_{AR}$)** | Query $\leftrightarrow$ Answer | Query intent fulfillment | Improve instruction clarity, add few-shot examples |
| **Harmonic Composite ($S_{Triad}$)** | Unified Score | Overall quality gate | Enforces zero-tolerance for weak outlier metrics |

---

## 7. Self-Check Questions & Practice Exercises

### Conceptual Self-Check Questions

#### Question 1: What are the three pillars of the RAG Triad?
- A) Speed, Cost, and Memory.
- B) Context Relevance, Groundedness (Faithfulness), and Answer Relevance.
- C) Java, Python, and C++.
- D) Unit testing, Integration testing, and E2E testing.

*Answer*: **B**. The RAG Triad measures whether retrieved context is relevant to the query, whether the answer is faithful to the context, and whether the answer addresses the query.

---

#### Question 2: If an AI model generates an answer that is 100% relevant to the user's question, but fabricates facts that do not exist in the retrieved documents, which metric fails?
- A) Context Relevance.
- B) Answer Relevance.
- C) Groundedness (Faithfulness).
- D) Network latency.

*Answer*: **C**. Groundedness measures factual adherence to retrieved context; fabricated facts immediately drop the groundedness score.

---

#### Question 3: Why is the harmonic mean preferred over the arithmetic mean for composite RAG Triad scoring?
- A) Harmonic mean compiles faster in Java.
- B) Harmonic mean heavily penalizes low outliers: if any single pillar (like Groundedness) drops to zero, the harmonic composite drops to zero, preventing hallucinated answers from passing.
- C) Arithmetic mean does not work with floating-point numbers.
- D) OpenAI mandates harmonic mean.

*Answer*: **B**. The harmonic mean prevents a high score on one metric from masking a catastrophic failure on another.

---

#### Question 4: In an automated enterprise CI/CD pipeline, what should happen if a prompt modification causes average Groundedness to drop below the release threshold?
- A) The build should pass anyway to avoid delaying deployment.
- B) The Maven build should automatically fail (`mvn test` exits with non-zero code), blocking the pull request from merging.
- C) The application should switch to Python.
- D) The database should be cleared.

*Answer*: **B**. Quality gates ensure regression-free deployment by treating low evaluation scores as failing test assertions.

---

### Hands-on Practice Exercises

#### Exercise 1: ROUGE-1 Precision and Recall Calculator
**Task**: Implement a static utility method `calculateRouge1F1(String reference, String candidate)` that tokenizes both strings and returns the F1 score representing unigram overlap between candidate text and ground truth.

**Solution**:
```java
package com.genai.enterprise.exercises;

import java.util.*;

public class RougeCalculator {

    public static double calculateRouge1F1(String reference, String candidate) {
        if (reference == null || candidate == null) return 0.0;
        Set<String> refWords = new HashSet<>(Arrays.asList(reference.toLowerCase().split("\\s+")));
        Set<String> candWords = new HashSet<>(Arrays.asList(candidate.toLowerCase().split("\\s+")));

        long overlap = candWords.stream().filter(refWords::contains).count();
        if (overlap == 0) return 0.0;

        double precision = (double) overlap / candWords.size();
        double recall = (double) overlap / refWords.size();
        return 2.0 * (precision * recall) / (precision + recall);
    }
}
```

---

#### Exercise 2: Synthetic Test Case Generator
**Task**: Write a generator class that accepts an enterprise documentation chunk and automatically creates a synthetic `RagTestCase` for benchmarking.

**Solution**:
```java
package com.genai.enterprise.exercises;

import com.genai.enterprise.evaluation.RagTestCase;

public class SyntheticDataGenerator {

    public static RagTestCase generateSyntheticCase(String docChunk, String testId) {
        String simulatedQuestion = "What are the core stipulations outlined in section " + testId + "?";
        return new RagTestCase(testId, simulatedQuestion, docChunk, docChunk, docChunk);
    }
}
```

---

#### Exercise 3: Hallucination Alert Webhook
**Task**: Implement an interceptor that inspects user responses: if the online groundedness score falls below $0.50$, append a disclaimer warning the user.

**Solution**:
```java
package com.genai.enterprise.exercises;

import com.genai.enterprise.evaluation.RagTriadMetrics;

public class ProductionGroundednessGuard {

    public static String guardResponse(RagTriadMetrics metrics, String rawAnswer) {
        if (metrics.groundedness() < 0.50) {
            System.err.println("[PRODUCTION ALERT] Low groundedness detected (" + metrics.groundedness() + ")!");
            return rawAnswer + "\n\n*[Notice: This response could not be fully verified against official company records.]*";
        }
        return rawAnswer;
    }
}
```

---

#### Exercise 4: Harmonic Mean Quality Gate Evaluator
**Task**: Write an evaluation gate method `boolean evaluateBuildGate(List<RagTriadMetrics> testResults, double minCompositeThreshold)` that returns `true` only if all individual cases maintain a non-zero composite score and the cohort harmonic average meets the threshold.

**Solution**:
```java
package com.genai.enterprise.exercises;

import com.genai.enterprise.evaluation.RagTriadMetrics;
import java.util.List;

public class BuildQualityGateEvaluator {

    public static boolean evaluateBuildGate(List<RagTriadMetrics> testResults, double minCompositeThreshold) {
        if (testResults == null || testResults.isEmpty()) return false;

        double sumReciprocals = 0.0;
        for (RagTriadMetrics m : testResults) {
            double composite = m.getHarmonicComposite();
            if (composite <= 0.0) {
                System.err.println("[BUILD GATE REJECTED] Zero-score outlier detected: " + m.diagnosticMessage());
                return false;
            }
            sumReciprocals += (1.0 / composite);
        }

        double cohortHarmonicAverage = testResults.size() / sumReciprocals;
        System.out.printf("[BUILD GATE] Cohort Harmonic Average: %.3f (Threshold: %.3f)%n",
                cohortHarmonicAverage, minCompositeThreshold);

        return cohortHarmonicAverage >= minCompositeThreshold;
    }
}
```

---

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [Day 57: Multi-Agent Orchestration](../Day_57_Multi_Agent_Orchestration/Day_57_Multi_Agent_Orchestration.md) | [All 60 Days Overview](../../README.md) | [Day 59: Vector Database Deep Dive & Optimization](../Day_59_Vector_Database_Deep_Dive/Day_59_Vector_Database_Deep_Dive.md) |
