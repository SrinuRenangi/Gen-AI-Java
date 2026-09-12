# Day 47: Advanced RAG — Chunking, Scoring & Re-Ranking

[← Previous: Day 46 - RAG Pipeline in LangChain4j](../Day_46_RAG_Pipeline_in_LangChain4j/Day_46_RAG_Pipeline_in_LangChain4j.md) | [Next: Day 48 - Tool Execution & Function Calling →](../Day_48_Tool_Execution_Function_Calling/Day_48_Tool_Execution_Function_Calling.md)

---

## 1. Topic Overview
Advanced RAG enhances standard semantic search by combining structure-aware recursive document chunking with sliding overlap, two-stage cross-encoder re-ranking (`ScoringModel`), and dynamic query routing. In enterprise Java systems, these architectural patterns eliminate false positives, correct rank inversion, minimize context window token waste, and deliver up to 99% retrieval precision across specialized multi-domain vector stores.

---

## 2. Basic Foundations (True Zero)

### Why Naive RAG Fails in Production
Basic vector search (Bi-Encoder embedding) is fast, but it suffers from two major vulnerabilities:
1. **Naive Slicing Bugs**: Blindly slicing a document every 500 characters cuts sentences and conditional clauses in half. If a policy states: *"Employees may never disclose passwords, except during authorized audits by the VP of Security"*, a naive cut can place the exception into the next chunk, causing the AI to report that exceptions never exist!
2. **Vocabulary & Angle Mismatches**: Vector similarity is coarse. Out of 100,000 documents, it can find the top 20 candidates in 5 milliseconds, but the true #1 answer is frequently ranked down at position #14 or #18 (a problem known as **Rank Inversion**).

### Relatable Physical Analogy: The Olympic Qualifier vs. The Final Medal Judges
Imagine organizing an Olympic Gymnastics Championship with 10,000 global competitors:
- **Stage 1: Automated Qualifier (Bi-Encoder Vector Search)**: You cannot have elite Olympic master judges evaluate all 10,000 athletes for 45 minutes each. Instead, automated electronic timing and balance sensors run a rapid 30-second filter, quickly reducing 10,000 athletes down to the **top 20 finalists** in minutes. It is fast, but coarse.
- **Stage 2: Master Panel Evaluation (Cross-Encoder Re-Ranking)**: The elite human master judges evaluate only those **top 20 finalists**, scrutinizing every micro-second of form, posture, and difficulty. They re-rank the board: Athlete #18 delivers a flawless routine and takes the **Gold Medal**!

### Minimal Beginner-Friendly Working Code
Here is how to set up two-stage re-ranking in LangChain4j using `ReRankingContentRetriever`:

```java
package com.genai.langchain4j.advancedrag;

import dev.langchain4j.model.cohere.CohereScoringModel;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.content.retriever.ReRankingContentRetriever;

public class SimpleReRankingSetup {

    public static ContentRetriever buildTwoStageRetriever(EmbeddingStoreContentRetriever baseRetriever) {
        // 1. Configure the Stage 2 Cross-Encoder Scoring Model (e.g. Cohere Rerank v3)
        ScoringModel scoringModel = CohereScoringModel.builder()
            .apiKey(System.getenv("COHERE_API_KEY"))
            .modelName("rerank-english-v3.0")
            .build();

        // 2. Wrap the Stage 1 retriever: retrieves top 20 candidates,
        // evaluates cross-attention, and returns the top 3 highest quality chunks!
        return ReRankingContentRetriever.builder()
            .contentRetriever(baseRetriever)
            .scoringModel(scoringModel)
            .maxResults(3)           // Final surgical top-3 candidates for the prompt
            .minScore(0.65)          // Discards low-relevance false positives
            .build();
    }
}
```

### Line-by-Line Walkthrough
1. **`CohereScoringModel.builder()`**: Instantiates a Cross-Encoder transformer model that receives both the query and document chunk simultaneously, evaluating deep token-to-token cross-attention.
2. **`ReRankingContentRetriever.builder()`**: LangChain4j's composable wrapper uniting Stage 1 candidate retrieval with Stage 2 re-ranking.
3. **`.contentRetriever(baseRetriever)`**: The Stage 1 Bi-Encoder retriever (e.g., PostgreSQL `pgvector`) that fetches an initial wide net of candidates (e.g., top 20).
4. **`.maxResults(3).minScore(0.65)`**: The cross-encoder re-ranks the 20 candidates, discards anything below the 0.65 relevance score, and passes the 3 absolute best chunks into the LLM prompt.

---

## 3. Core Concept Walkthrough (Basic → Intermediate)

```
+-------------------------------------------------------------------------------+
|                       TWO-STAGE ADVANCED RAG PIPELINE                         |
+-------------------------------------------------------------------------------+
|                                                                               |
|  User Query: "What is the memory limit for HNSW index on 10M vectors?"        |
|         |                                                                     |
|         v                                                                     |
|  [ STAGE 1: Bi-Encoder Vector Search (High Recall, Coarse Ranking) ]          |
|  Query & Chunks embedded independently; cosine search via HNSW in pgvector    |
|  Output: Top 20 Candidates in 5ms:                                            |
|    - Candidate #1  (Score: 0.81) [Spring Boot Virtual Threads]                |
|    - Candidate #2  (Score: 0.79) [Kafka Replication In-Sync]                  |
|    - ...                                                                      |
|    - Candidate #14 (Score: 0.72) [PostgreSQL HNSW 10M vectors: 4GB RAM]       |
|         |                                                                     |
|         v (Top 20 Candidates passed to Stage 2)                               |
|  [ STAGE 2: Cross-Encoder Re-Ranking (High Precision ScoringModel) ]         |
|  Deep Joint Self-Attention: [Query + Candidate] together in Transformer       |
|         |                                                                     |
|         v RANK INVERSION OCCURS!                                              |
|    - Winner #1 (Score: 0.96, was #14): [PostgreSQL HNSW 10M vectors: 4GB RAM] |
|    - Winner #2 (Score: 0.42, was #1):  [Spring Boot Virtual Threads]          |
|         |                                                                     |
|         v                                                                     |
|  Only the Top-2 Surgical Winners injected into LLM Prompt!                    |
+-------------------------------------------------------------------------------+
```

### The Chunking Dilemma: Recursive Splitting with Overlap
Standard character chunking severs sentences. LangChain4j provides `DocumentSplitters.recursive(...)`:
1. **Paragraph First (`\n\n`)**: Splits along natural paragraphs to keep semantic thoughts unified.
2. **Sentence Second (`.`, `!`, `?`)**: If a paragraph exceeds the token target (e.g., 500 tokens), it splits cleanly on sentence boundaries.
3. **Sliding Overlap**: A buffer (e.g., 50 characters or tokens) shared between consecutive chunks ensures boundary conditions are never lost.

```java
package com.genai.langchain4j.advancedrag;

import java.util.ArrayList;
import java.util.List;

public final class RecursiveChunker {

    private RecursiveChunker() {}

    public static List<String> chunkWithOverlap(String text, int chunkSize, int overlap) {
        if (text == null || text.isBlank()) return List.of();
        if (chunkSize <= overlap) throw new IllegalArgumentException("chunkSize must be greater than overlap");

        List<String> chunks = new ArrayList<>();
        int step = chunkSize - overlap;
        int length = text.length();

        for (int start = 0; start < length; start += step) {
            int end = Math.min(start + chunkSize, length);
            chunks.add(text.substring(start, end));
            if (end == length) break;
        }

        return chunks;
    }
}
```

### Bi-Encoders vs. Cross-Encoders: Architectural Comparison

| Architectural Trait | Bi-Encoder (`EmbeddingModel`) | Cross-Encoder (`ScoringModel`) |
|:---|:---|:---|
| **Mechanism** | Encodes query and document independently into vectors. | Encodes `[Query + Document]` together through joint attention layers. |
| **Speed / Scalability** | **Ultra-Fast (< 5ms)**; vectors are pre-computed in database index. | **Slower (30–100ms)**; requires forward pass per candidate pair. |
| **Search Space** | Can search across 10,000,000 documents. | Feasible only for 10–50 candidate chunks. |
| **Precision** | Coarse; susceptible to keyword and semantic overlap bias. | **Surgical precision**; evaluates exact logical and grammatical alignment. |
| **Pipeline Role** | **Stage 1**: Candidate Retrieval (High Recall). | **Stage 2**: Candidate Re-Ranking (High Precision). |

---

## 4. Prerequisite & Supporting Concepts

### Prerequisite / Supporting Concept: Enterprise Dynamic Query Routing
In large enterprises, placing all documents (HR, IT, Legal, Finance) into a single vector store causes cross-domain confusion:
- User asks: *"What is the policy for node evictions?"*
- A monolithic vector store might match an HR document on employee termination rather than Kubernetes pod evictions!
- A **Query Router** inspects the user query and directs it exclusively to the appropriate domain vector store:

```
 User Query ──► [ Query Router / Intent Classifier ]
                     ├── (Intent: Kubernetes / IT) ──► IT Infrastructure Store
                     ├── (Intent: PTO / Benefits)    ──► HR & Benefits Store
                     └── (Intent: Tax / Expense)     ──► Financial Accounting Store
```

---

## 5. Advanced Depth (Intermediate → Advanced)

### Reciprocal Rank Fusion (RRF) Scorer
When merging candidates from multiple searches (e.g., keyword search + vector search), use the standard RRF formula to combine ranks without uncalibrated score distortion:

$$RRF(d) = \sum_{m \in M} \frac{1}{60 + r_m}$$

```java
package com.genai.langchain4j.advancedrag;

public final class ReciprocalRankScorer {

    public static final int K = 60;

    private ReciprocalRankScorer() {}

    public static double computeRrf(int biEncoderRank, int crossEncoderRank) {
        double term1 = 1.0 / (K + biEncoderRank);
        double term2 = 1.0 / (K + crossEncoderRank);
        return term1 + term2;
    }
}
```

### Intent-Based Query Classifier for Routing
```java
package com.genai.langchain4j.advancedrag;

public final class QueryRouter {

    public enum DomainStore { INFRASTRUCTURE_DEVOPS, HR_POLICY, FINANCIAL_ACCOUNTING }

    private QueryRouter() {}

    public static DomainStore route(String query) {
        String lower = query.toLowerCase();
        if (lower.contains("k8s") || lower.contains("kubernetes") || lower.contains("pod") || lower.contains("docker") || lower.contains("kafka")) {
            return DomainStore.INFRASTRUCTURE_DEVOPS;
        }
        if (lower.contains("pto") || lower.contains("vacation") || lower.contains("leave") || lower.contains("insurance") || lower.contains("benefits")) {
            return DomainStore.HR_POLICY;
        }
        return DomainStore.FINANCIAL_ACCOUNTING;
    }
}
```

### Common Anti-Patterns & Production Traps

| Anti-Pattern | Why It Breaks in Production | Correct Architectural Solution |
|:---|:---|:---|
| **Applying Cross-Encoder to 10,000 Chunks** | Joint cross-attention is computationally heavy; running it over thousands of documents locks the server and incurs seconds of latency. | Use Bi-Encoders to retrieve the top 20 candidates, then apply the Cross-Encoder only to those 20 finalists. |
| **Fixed Character Chunking Without Overlap** | Cuts sentences in half, causing queries to miss crucial boundary clauses. | Use recursive paragraph/sentence splitters with a 50–100 character sliding overlap window. |
| **Single Monolithic Vector Store for All Company Data** | General queries retrieve irrelevant documents from unrelated departments (e.g., HR matching IT infrastructure queries). | Segregate data into specialized vector collections and route queries with a `QueryRouter`. |

---

## 6. Quick Recap
- **Naive RAG** suffers from semantic slicing bugs and coarse vector ranking where the true answer is buried in candidate results.
- **Recursive Chunking with Overlap** respects natural paragraph and sentence boundaries, preserving semantic thoughts intact.
- **Two-Stage Retrieval** pairs fast Bi-Encoder vector search (top 20 candidates in 5ms) with surgical Cross-Encoder re-ranking (`ScoringModel`).
- **Rank Inversion** occurs when a cross-encoder evaluates the top 20 candidates and promotes the true answer from rank #18 up to #1.
- **Dynamic Query Routing** directs user inquiries to domain-specific vector stores (DevOps, HR, Finance) to eliminate cross-domain noise.

---

## 7. Self-Check Questions & Practice Exercises

### 5-Question Self-Check Quiz

#### Question 1
What is the fundamental difference between a Bi-Encoder and a Cross-Encoder?
- A) Bi-encoders process images, while cross-encoders process audio.
- B) Bi-encoders embed query and documents independently into vectors for fast search, while cross-encoders compute joint self-attention across the combined query-document pair for high accuracy.
- C) Cross-encoders run only on mobile devices.
- D) Bi-encoders are deprecated in modern AI.

#### Question 2
Why is recursive document chunking with overlap superior to fixed-character chunking?
- A) It doubles the clock speed of the GPU.
- B) It prevents splitting sentences or paragraphs mid-thought and preserves context across chunk boundaries via a sliding overlap window.
- C) It compresses text using GZIP.
- D) It bypasses vector database licensing fees.

#### Question 3
What is "Rank Inversion" in a two-stage retrieval pipeline?
- A) When a database crashes and reverses its primary keys.
- B) When a highly relevant document ranked lower in Stage 1 bi-encoder vector search is elevated to Rank 1 by the Stage 2 cross-encoder re-ranking model.
- C) Sorting search results in alphabetical order.
- D) An error caused by negative cosine similarity.

#### Question 4
In LangChain4j, which component wraps a base `ContentRetriever` with a `ScoringModel`?
- A) `MessageWindowChatMemory`
- B) `ReRankingContentRetriever`
- C) `JdbcTemplate`
- D) `OpenAiChatModel`

#### Question 5
Why is Dynamic Query Routing important in multi-domain enterprise applications?
- A) It prevents queries from searching the wrong knowledge bases, reducing noise, preventing cross-domain hallucinations, and enforcing compliance boundaries.
- B) It allows the model to run without internet access.
- C) It encrypts network traffic between microservices.
- D) Query routing is only used for billing calculations.

---

### Quiz Answers & Explanations
1. **B**: Bi-encoders allow pre-computing and caching vector embeddings for millions of chunks, whereas cross-encoders perform full joint attention between the query and candidate text, providing superior precision at higher computational cost.
2. **B**: Fixed-character slicing frequently cuts words and conditional clauses in half. Recursive splitting respects structural grammar (paragraphs, sentences) and uses overlap to maintain semantic continuity.
3. **B**: Bi-encoders use approximate cosine distance and can rank true answers lower due to vocabulary mismatch. The cross-encoder re-evaluates candidates and promotes the true answer to the top.
4. **B**: `ReRankingContentRetriever` accepts an underlying `ContentRetriever` to fetch initial candidates, scores them with a `ScoringModel`, and returns the top-K highest-scoring segments.
5. **A**: Routing queries to specific domain stores (e.g. routing a tax question to finance and a cluster question to DevOps) prevents irrelevant cross-domain matches and guarantees that specialized retrieval policies are applied.

---

### Hands-On Practice Exercises

#### Exercise 1: Sliding Overlap Validator
**Problem Statement**:  
Build a utility method `boolean hasOverlap(String chunk1, String chunk2, int minOverlapLength)` that verifies whether the trailing suffix of `chunk1` is present at the beginning of `chunk2`, ensuring that recursive chunking maintained continuity.

<details>
<summary>👉 View Solution</summary>

```java
package com.genai.langchain4j.exercises;

public class ChunkOverlapValidator {

    public static boolean hasOverlap(String chunk1, String chunk2, int minOverlapLength) {
        if (chunk1 == null || chunk2 == null || chunk1.length() < minOverlapLength || chunk2.length() < minOverlapLength) {
            return false;
        }

        String suffix = chunk1.substring(chunk1.length() - minOverlapLength).trim();
        return chunk2.contains(suffix);
    }
}
```
</details>

#### Exercise 2: Intent-Based Query Classifier
**Problem Statement**:  
Build a regex-based query classifier that categorizes questions into `SQL_QUERY`, `REST_API`, or `GENERAL_JAVA` for routing to distinct code documentation stores.

<details>
<summary>👉 View Solution</summary>

```java
package com.genai.langchain4j.exercises;

public class CodeQueryClassifier {

    public enum CodeDomain { SQL_QUERY, REST_API, GENERAL_JAVA }

    public static CodeDomain classify(String query) {
        if (query == null) return CodeDomain.GENERAL_JAVA;
        String lower = query.toLowerCase();
        if (lower.contains("select") || lower.contains("join") || lower.contains("table") || lower.contains("postgres")) {
            return CodeDomain.SQL_QUERY;
        }
        if (lower.contains("http") || lower.contains("endpoint") || lower.contains("get") || lower.contains("post") || lower.contains("controller")) {
            return CodeDomain.REST_API;
        }
        return CodeDomain.GENERAL_JAVA;
    }
}
```
</details>

---

[← Previous: Day 46 - RAG Pipeline in LangChain4j](../Day_46_RAG_Pipeline_in_LangChain4j/Day_46_RAG_Pipeline_in_LangChain4j.md) | [Next: Day 48 - Tool Execution & Function Calling →](../Day_48_Tool_Execution_Function_Calling/Day_48_Tool_Execution_Function_Calling.md)
