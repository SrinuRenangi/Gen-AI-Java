# Day 40: Advanced RAG — Query Transformation & Re-Ranking

[← Previous: Day 39 - RAG Pipeline](../Day_39_RAG_Retrieval_Augmented_Generation/Day_39_RAG_Retrieval_Augmented_Generation.md) | [Next: Day 41 - Tool Calling →](../Day_41_Tool_Calling_LLMs_Execute_Java/Day_41_Tool_Calling_LLMs_Execute_Java.md)

---

## 1. Topic Overview
Advanced RAG enhances standard semantic retrieval pipelines with query transformation techniques (such as Hypothetical Document Embeddings and Multi-Query Expansion) and two-stage cross-encoder re-ranking algorithms. In enterprise production, these patterns overcome real-world user search query imperfections, bridge query-document semantic asymmetry, and achieve up to 99% retrieval precision.

---

## 2. Basic Foundations (True Zero)

### The Production Reality: Naive RAG Fails Real Users
In basic RAG, we assume users ask well-formed, complete questions: *"What are the operational procedures for configuring Nginx 504 gateway timeout thresholds?"*

In real life, users type: *"how fix 504?"* or *"server stuck"*.
- A 3-word query vector looks completely different geometrically from a 400-word declarative engineering document.
- Standard cosine similarity often ranks irrelevant documents higher simply because they contain repeated keywords.
- Standard naive RAG fails to retrieve the correct document up to 40% of the time in production!

### Relatable Physical Analogy: The Detective, The Sketch Artist & The Forensic Expert
Imagine investigating a jewelry store theft:
- **Naive Search (Standard RAG)**: A witness says: *"I saw a tall guy in a dark coat."* The detective types "tall guy dark coat" into the police database. Result: 20,000 irrelevant matches; the thief is never found.
- **Advanced Search (HyDE + Multi-Query + Re-Ranking)**:
  1. **The Sketch Artist (HyDE)**: An artist creates a detailed visual composite portrait based on the description. Police search mugshots using the portrait—because faces match faces!
  2. **Multi-Angle Search (Multi-Query)**: Detectives simultaneously check getaway vehicle registrations, pawn shops, and security footage.
  3. **The Forensic Expert (Cross-Encoder Re-Ranking)**: Out of 20 potential suspects, a forensic expert examines fingerprints and DNA side-by-side, identifying the true culprit at #1 with 99.9% certainty.

### Minimal Beginner-Friendly Working Code: Multi-Query Generation
Here is how to transform a vague user question into multiple search perspectives in Spring AI:

```java
package com.genai.springai.advancedrag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SimpleQueryExpanderRunner implements CommandLineRunner {

    private final ChatClient chatClient;

    public SimpleQueryExpanderRunner(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public void run(String... args) {
        String rawUserQuery = "how fix 504?";

        // Ask the LLM to expand the query into 3 distinct, professional search variations
        List<String> expandedQueries = chatClient.prompt()
            .system("""
                You are an enterprise search query optimizer. Given a user search term,
                generate 3 distinct, detailed search query variations covering synonyms,
                root causes, and technical specifications.
                """)
            .user(rawUserQuery)
            .call()
            .entity(new ParameterizedTypeReference<List<String>>() {});

        System.out.println("Original Query: " + rawUserQuery);
        System.out.println("Expanded Variations for Parallel Vector Search:");
        expandedQueries.forEach(q -> System.out.println(" -> " + q));
    }
}
```

### Line-by-Line Walkthrough
1. **`private final ChatClient chatClient;`**: Injects the fluent Spring AI conversational client.
2. **`chatClient.prompt().system(...)`**: Sets system instructions directing the model to act as a search query optimizer.
3. **`new ParameterizedTypeReference<List<String>>() {}`**: Spring AI automatically prompts the model with a JSON schema and converts the response into a strongly-typed Java `List<String>`.
4. **`expandedQueries.forEach(...)`**: Outputs 3 enriched, professional query strings (e.g., "HTTP 504 Gateway Timeout root causes", "Nginx proxy read timeout configuration", "Downstream microservice response latency") ready for parallel retrieval.

---

## 3. Core Concept Walkthrough (Basic → Intermediate)

```
+-------------------------------------------------------------------------------+
|                       ADVANCED RAG PIPELINE WORKFLOW                          |
+-------------------------------------------------------------------------------+
|                                                                               |
|  Raw User Query: "how fix 504?"                                               |
|         |                                                                     |
|         +-----------------------+-----------------------+                     |
|         |                       |                       |                     |
|         v                       v                       v                     |
|    [ HyDE Engine ]     [ Multi-Query Expander ]  [ Direct Query ]             |
|   Generates 2-sentence    Rewrites into 3-5      Raw user string              |
|   hypothetical passage     distinct queries                                   |
|         |                       |                       |                     |
|         v                       v                       v                     |
|  [ Vector Search ]       [ Vector Search ]       [ Vector Search ]            |
|    (HNSW pgvector)        (HNSW pgvector)         (HNSW pgvector)             |
|         \                       |                      /                      |
|          +----------------------+---------------------+                       |
|                                 |                                             |
|                                 v                                             |
|                   [ Reciprocal Rank Fusion (RRF) ]                            |
|                 Combines & deduplicates candidates                            |
|                                 |                                             |
|                                 v (Top 25 Candidates)                         |
|                 [ Cross-Encoder Deep Re-Ranking ]                             |
|               Full transformer attention reranking                            |
|                                 |                                             |
|                                 v (Top 3 Precision Docs)                      |
|               [ Context Re-Ordering (U-Curve Fix) ]                           |
|               Best docs placed at extreme top & bottom                        |
|                                 |                                             |
|                                 v                                             |
|                 [ Grounded Answer Generation ]                                |
+-------------------------------------------------------------------------------+
```

### Technique 1: Hypothetical Document Embeddings (HyDE)
HyDE bridges **Query-Document Asymmetry**:
- Instead of embedding the user's brief question, we instruct a lightweight LLM (such as Llama 3.2 or GPT-4o-mini) to generate a hypothetical answer passage.
- We then embed that hypothetical answer into vector space!
- Even if the hypothetical text contains small inaccuracies, its semantic tone, vocabulary, and paragraph structure match real documentation far better than a 3-word question, boosting cosine similarity dramatically.

```java
package com.genai.springai.advancedrag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class HydeService {

    private final ChatClient chatClient;

    public HydeService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String generateHypotheticalPassage(String userQuery) {
        return chatClient.prompt()
            .system("You are a technical knowledge synthesizer. Write a concise 2-sentence factual passage directly answering the user query. Do not include conversational remarks.")
            .user(userQuery)
            .call()
            .content();
    }
}
```

### Technique 2: Reciprocal Rank Fusion (RRF)
When multiple queries run in parallel, each produces its own ranked document list. How do you merge them into a single fair leaderboard without score calibration issues?

Use the mathematical **Reciprocal Rank Fusion (RRF)** formula:

$$RRF\_Score(d) = \sum_{m \in M} \frac{1}{k + rank(d, m)}$$

- $M$ is the set of search queries.
- $rank(d, m)$ is the 1-indexed position of document $d$ in the result list for query $m$.
- $k$ is a constant (standard default: $k = 60$) that prevents high-ranking documents from totally dominating.

```java
package com.genai.springai.advancedrag;

import com.genai.springai.vectorstore.Document;

import java.util.*;

public final class ReciprocalRankFusion {

    private static final int DEFAULT_K = 60;

    public record RankedDocument(Document document, double rrfScore) {}

    private ReciprocalRankFusion() {}

    public static List<RankedDocument> fuse(List<List<Document>> rankedLists, int topK) {
        Map<String, Document> docLookup = new HashMap<>();
        Map<String, Double> scoreMap = new HashMap<>();

        for (List<Document> list : rankedLists) {
            for (int rank = 0; rank < list.size(); rank++) {
                Document doc = list.get(rank);
                docLookup.put(doc.getId(), doc);

                double currentScore = scoreMap.getOrDefault(doc.getId(), 0.0);
                double contribution = 1.0 / (DEFAULT_K + (rank + 1));
                scoreMap.put(doc.getId(), currentScore + contribution);
            }
        }

        return scoreMap.entrySet().stream()
                .map(e -> new RankedDocument(docLookup.get(e.getKey()), e.getValue()))
                .sorted(Comparator.comparingDouble(RankedDocument::rrfScore).reversed())
                .limit(topK)
                .toList();
    }
}
```

### Technique 3: Two-Stage Retrieval (Bi-Encoder + Cross-Encoder)
1. **Stage 1 (Bi-Encoder / Vector Store)**:
   - Queries and documents are embedded separately into vectors.
   - Calculates fast dot products via HNSW index in PostgreSQL `pgvector` (< 5ms).
   - Retrieves a wide candidate net of the top 25 chunks.
2. **Stage 2 (Cross-Encoder Re-Ranking)**:
   - Feeds `[Query + Document]` together through a deep cross-attention transformer.
   - Every word in the query attends to every word in the document, catching exact logical nuances.
   - Re-ranks the 25 candidates and selects the top 3–5 highest-precision chunks.

---

## 4. Prerequisite & Supporting Concepts

### Prerequisite / Supporting Concept: The "Lost in the Middle" Attention Bias
Empirical research demonstrates that LLMs exhibit a pronounced **U-shaped attention curve**:
- When given 10 retrieved chunks in a prompt, the LLM pays high attention to chunks 1 and 2 (at the beginning) and chunks 9 and 10 (at the end).
- Chunks placed in the middle (positions 4–7) suffer up to a **50% drop in recall accuracy**!

```
 Attention %
      100% │  ██                                                    ██
           │  ██                                                    ██
       50% │  ██                                                    ██
           │  ████        ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░        ██████
           │  ██████      ░░░  LOST IN THE MIDDLE  ░░░      ██████████
        0% └──┴───────────┴─────────────────────────────┴───────────┴───►
             Doc 1       Doc 3         Doc 5           Doc 8       Doc 10
             (Top)                    (Middle)                    (Bottom)
```

### Prerequisite / Supporting Concept: Context Re-Ordering Algorithm
To counteract this bias, we sort documents so the highest-scoring items sit at the **extremes**:
- Position 1: Rank #1 (Top)
- Position Last: Rank #2 (Bottom)
- Position 2: Rank #3
- Position (Last - 1): Rank #4
- Middle: Lowest ranked candidates

---

## 5. Advanced Depth (Intermediate → Advanced)

### Context Reorderer Implementation
Here is the clean Java 21 implementation of the extreme-edge context distributor:

```java
package com.genai.springai.advancedrag;

import com.genai.springai.vectorstore.Document;

import java.util.Arrays;
import java.util.List;

public final class ContextReorderer {

    private ContextReorderer() {}

    public static List<Document> reorderForAttention(List<Document> rankedDocs) {
        if (rankedDocs == null || rankedDocs.size() <= 2) {
            return rankedDocs;
        }

        Document[] result = new Document[rankedDocs.size()];
        int left = 0;
        int right = rankedDocs.size() - 1;
        boolean placeAtLeft = true;

        for (Document doc : rankedDocs) {
            if (placeAtLeft) {
                result[left++] = doc;
            } else {
                result[right--] = doc;
            }
            placeAtLeft = !placeAtLeft;
        }

        return Arrays.asList(result);
    }
}
```

### Parallel Multi-Query Ingestion with Virtual Threads
Using Java 21 Virtual Threads, executing 3–5 multi-query searches concurrently adds virtually zero latency overhead compared to a single query:

```java
package com.genai.springai.advancedrag;

import com.genai.springai.vectorstore.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class ParallelMultiQueryRetriever {

    private final VectorStore vectorStore;

    public ParallelMultiQueryRetriever(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public List<Document> retrieveWithFusion(List<String> queries, int topK) 
            throws InterruptedException, ExecutionException {
        
        List<Future<List<Document>>> futures = new ArrayList<>();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (String query : queries) {
                futures.add(executor.submit(() -> vectorStore.similaritySearch(
                    SearchRequest.builder().query(query).topK(topK).build()
                )));
            }
        } // Executor awaits completion of all virtual thread searches

        List<List<Document>> allRankedLists = new ArrayList<>();
        for (Future<List<Document>> future : futures) {
            allRankedLists.add(future.get());
        }

        return ReciprocalRankFusion.fuse(allRankedLists, topK).stream()
                .map(ReciprocalRankFusion.RankedDocument::document)
                .toList();
    }
}
```

### Common Anti-Patterns & Production Traps

| Anti-Pattern | Why It Fails in Production | Correct Architectural Solution |
|:---|:---|:---|
| **Applying Cross-Encoder to 10,000 Chunks** | Cross-attention has $O(N^2)$ computational complexity; running it across thousands of documents causes seconds of latency and server lockups. | **Two-Stage Funnel**: Use fast Bi-Encoder vector search for the top 20–25 candidates, then apply the Cross-Encoder only to those candidates. |
| **Summing Raw Cosine Scores Across Queries** | Different query embeddings produce non-comparable similarity score distributions; adding them directly distorts rankings. | Use **Reciprocal Rank Fusion (RRF)**, which relies on relative positional ranks rather than raw uncalibrated float scores. |
| **Pasting Retrieved Documents in Default Order** | Documents placed in the middle of long prompts are frequently overlooked by LLM attention mechanisms. | Reorder context chunks using `ContextReorderer` to place top candidates at the start and end of the context envelope. |

---

## 6. Quick Recap
- **Query-Document Asymmetry** occurs because short user queries look fundamentally different in vector space from long, declarative knowledge documents.
- **HyDE** generates a hypothetical answer to the query first, embedding the answer to align with real knowledge base passages.
- **Multi-Query Expansion** explores multiple angles of a user query in parallel using Java 21 Virtual Threads.
- **Reciprocal Rank Fusion (RRF)** combines independent ranked lists fairly based on rank positions: $1 / (60 + \text{rank})$.
- **Two-Stage Retrieval** pairs fast Bi-Encoder vector search (top 25 candidates) with deep Cross-Encoder re-ranking (top 3 winners).
- Counteract the **"Lost in the Middle"** attention bias by placing top-scoring documents at the very top and bottom of the context window.

---

## 7. Self-Check Questions & Practice Exercises

### 5-Question Self-Check Quiz

#### Question 1
What is the "Query-Document Asymmetry" problem in standard RAG?
- A) Database queries take longer than network file downloads.
- B) User questions are short, informal, and sparse, while knowledge base documents are long, dense, and declarative, causing vector cosine similarity between them to be suboptimal.
- C) Documents are stored in JSON, while queries are formatted in SQL.
- D) Queries can only be executed on weekdays.

#### Question 2
How does Hypothetical Document Embeddings (HyDE) solve Query-Document Asymmetry?
- A) By translating the query into French before searching.
- B) By using an LLM to generate a hypothetical answer passage, and embedding the dense hypothetical answer instead of the sparse question.
- C) By encrypting the query vector with AES-256.
- D) By disabling vector search in the database.

#### Question 3
What does the Reciprocal Rank Fusion (RRF) algorithm accomplish?
- A) It deletes duplicate records in PostgreSQL.
- B) It merges multiple independently ranked document lists (from multi-query searches) into a single unified ranking based on document positions rather than raw uncalibrated scores.
- C) It trains a new transformer model.
- D) It formats dates for HTTP responses.

#### Question 4
What is the primary operational difference between a Bi-Encoder and a Cross-Encoder?
- A) Cross-Encoders are 100x faster than Bi-Encoders.
- B) Bi-Encoders compute query and document vectors independently ($O(1)$ indexed lookup); Cross-Encoders evaluate the query and document simultaneously with deep cross-attention, offering superior accuracy at the cost of higher latency.
- C) Bi-Encoders only run on Windows.
- D) There is no difference.

#### Question 5
What causes the "Lost in the Middle" phenomenon in Large Language Models?
- A) Network packet drops during HTTP streaming.
- B) Transformer self-attention mechanisms assign higher attention weights to tokens at the beginning and end of long prompts, frequently overlooking facts placed in the center of the context.
- C) Database deadlocks in PostgreSQL.
- D) OutOfMemoryError in the JVM heap.

---

### Quiz Answers & Explanations
1. **B**: Short questions and dense technical paragraphs reside in different regions of high-dimensional vector space.
2. **B**: Embedding an answer passage ensures vector proximity to real answers in the knowledge base.
3. **B**: RRF combines rankings without needing to normalize arbitrary cosine score distributions across different queries.
4. **B**: Bi-Encoders enable fast million-scale vector retrieval; Cross-Encoders provide deep pairwise semantic re-ranking for the top 20 candidates.
5. **B**: Transformer attention exhibits a U-shaped curve, making intelligent context placement critical.

---

### Hands-On Practice Exercises

#### Exercise 1: HyDE Prompt Transformation in Spring AI
**Problem Statement**:  
Write a Spring `@Service` method `String generateHypotheticalPassage(ChatClient fastClient, String rawQuery)` that uses a lightweight model to draft a 2-sentence hypothetical answer for use in vector search.

<details>
<summary>👉 View Solution</summary>

```java
package com.genai.springai.advancedrag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class HydeGenerationService {

    private final ChatClient fastClient;

    public HydeGenerationService(ChatClient.Builder builder) {
        this.fastClient = builder
            .defaultSystem("You are an expert technical passage writer. Write a factual, dense paragraph answering the query.")
            .build();
    }

    public String generateHypotheticalPassage(String userQuery) {
        return fastClient.prompt()
            .user(u -> u.text("""
                Write a concise 2-sentence factual paragraph that directly answers the question:
                "{query}"
                Do not include conversational preamble. Output only the informative passage.
            """).param("query", userQuery))
            .call()
            .content();
    }
}
```
</details>

#### Exercise 2: Parallel Multi-Query Runner using Virtual Threads
**Problem Statement**:  
Given a `List<String> queries` and a `VectorStore`, execute all searches concurrently using Java 21's `Executors.newVirtualThreadPerTaskExecutor()` and fuse the results with `ReciprocalRankFusion`.

<details>
<summary>👉 View Solution</summary>

```java
package com.genai.springai.advancedrag;

import com.genai.springai.vectorstore.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class ParallelMultiQuerySearcher {

    private final VectorStore vectorStore;

    public ParallelMultiQuerySearcher(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public List<Document> searchParallelAndFuse(List<String> queries, int topK) throws Exception {
        List<Future<List<Document>>> futures = new ArrayList<>();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (String query : queries) {
                futures.add(executor.submit(() -> vectorStore.similaritySearch(
                    SearchRequest.builder().query(query).topK(topK).build()
                )));
            }
        }

        List<List<Document>> allResults = new ArrayList<>();
        for (Future<List<Document>> f : futures) {
            allResults.add(f.get());
        }

        return ReciprocalRankFusion.fuse(allResults, topK).stream()
            .map(ReciprocalRankFusion.RankedDocument::document)
            .toList();
    }
}
```
</details>

---

[← Previous: Day 39 - RAG Pipeline](../Day_39_RAG_Retrieval_Augmented_Generation/Day_39_RAG_Retrieval_Augmented_Generation.md) | [Next: Day 41 - Tool Calling →](../Day_41_Tool_Calling_LLMs_Execute_Java/Day_41_Tool_Calling_LLMs_Execute_Java.md)
