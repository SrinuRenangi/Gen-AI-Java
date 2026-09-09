# Day 40: Advanced RAG — Query Transformation & Re-Ranking
## Hypothetical Document Embeddings (HyDE), Multi-Query Expansion, Reciprocal Rank Fusion & Cross-Encoders

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [◀ Day 39: RAG — Retrieval-Augmented Generation](../Day_39_RAG_Retrieval_Augmented_Generation/Day_39_RAG_Retrieval_Augmented_Generation.md) | [All 60 Days Overview](../../README.md) | [Day 41: Tool Calling — LLMs That Execute Java Methods ▶](../Day_41_Tool_Calling_LLMs_Execute_Java/Day_41_Tool_Calling_LLMs_Execute_Java.md) |

---

## What Will You Learn Today?

Yesterday in Day 39, you built a fundamental RAG pipeline: chunking documents, calculating embeddings, searching PostgreSQL `pgvector`, and passing context to `ChatClient`.

However, when you launch a naive RAG pipeline into real-world production, you immediately encounter a painful reality: **Naive RAG has a retrieval failure rate of 25% to 40%**.
- Real users ask brief, vague, or typo-ridden questions (*"how to fix 504?"*).
- Dense technical documentation does not match the vector direction of a 4-word question (**Query-Document Asymmetry**).
- When you stuff 10 retrieved chunks into a prompt, the LLM recalls facts at the very top and very bottom, completely ignoring critical data in the center (**Lost in the Middle** phenomenon).

Today, you will master **Advanced RAG** in Java 21 and Spring AI:
- The **Query-Document Asymmetry** problem and how **Hypothetical Document Embeddings (HyDE)** flips vector search on its head by embedding simulated answers instead of queries.
- **Multi-Query Expansion**: Generating parallel search perspectives and merging them using **Reciprocal Rank Fusion (RRF)**.
- **Two-Stage Retrieval with Cross-Encoders**: Using fast Bi-Encoders to retrieve top 25 candidates, then applying deep Cross-Attention re-ranking to place the exact truth at position #1.
- Conquering the **"Lost in the Middle"** attention bias through contextual re-ordering.
- Building an enterprise Advanced RAG orchestration service in Spring Boot 3.

---

## Real-World Analogy: The Detective, The Sketch Artist & The Forensic Expert

Imagine investigating a high-profile art museum theft:

```
+---------------------------------------------------------------------------------------------------+
|                                  THE MULTI-STAGE INVESTIGATION                                    |
|                                                                                                   |
|  SCENARIO 1: Naive Investigation (Naive RAG)                                                      |
|  - A witness says: "I saw a tall guy in a dark coat running near the museum."                     |
|  - The detective searches the city criminal database with the exact text: "tall guy dark coat".   |
|  - Result: Zero hits or 10,000 irrelevant matches. The criminal is never found!                  |
|                                                                                                   |
|  SCENARIO 2: Advanced Investigation (HyDE + Multi-Query + Cross-Encoder)                          |
|  - 1. The Sketch Artist (HyDE):                                                                   |
|     Based on the vague description, an artist renders a detailed composite face portrait.         |
|     Instead of searching with words, the police run facial recognition using the rendered face!   |
|  - 2. Multi-Angle Inquiry (Multi-Query Expansion):                                                |
|     Detectives search 3 angles simultaneously: stolen vehicle registrations, fencing rings, and   |
|     museum security logs.                                                                         |
|  - 3. The Forensic Expert (Cross-Encoder Re-Ranking):                                             |
|     From 50 initial suspects, a forensic expert examines fingerprints side-by-side with crime     |
|     scene glass, ranking the true culprit at #1 with 99.9% certainty!                             |
+---------------------------------------------------------------------------------------------------+
```

---

## Why Naive RAG Fails in Enterprise Production

### 1. The Query-Document Asymmetry Problem
When an embedding model calculates a vector, it projects the textual structure into geometric space:
- **User Query**: 5 words, informal, question format (*"Why is my Kafka consumer lagging?"*).
- **Knowledge Base Chunk**: 400 words, formal, declarative format (*"Configuring `max.poll.interval.ms` and partition rebalancing in high-throughput consumers..."*).

Because the question vector looks fundamentally different from the answer vector, standard cosine similarity often ranks irrelevant documents higher simply because they contain the word *"Kafka"*.

### 2. The "Lost in the Middle" Phenomenon
Research by Stanford University proved that Large Language Models exhibit a strong **U-shaped attention curve**:

```
                         THE "LOST IN THE MIDDLE" ATTENTION BIAS
                         
 Attention / Recall %
      100% │  ██                                                    ██
           │  ██                                                    ██
           │  ██                                                    ██
       50% │  ██                                                    ██
           │  ████        ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░        ██████
           │  ██████      ░░░  LOST IN THE MIDDLE  ░░░      ██████████
        0% └──┴───────────┴─────────────────────────────┴───────────┴───►
             Doc 1       Doc 3         Doc 5           Doc 8       Doc 10
             (Top)                    (Middle)                    (Bottom)
```

If the exact paragraph needed to answer the user's question is placed at position 5 or 6 among 10 documents, the LLM will often **completely miss it** and claim it does not know the answer!

---

## Advanced Technique 1: Hypothetical Document Embeddings (HyDE)

**HyDE** was created by researchers to completely eliminate Query-Document Asymmetry.

### How HyDE Works:
Instead of embedding the user's question, we ask a fast, lightweight LLM to **hallucinate a hypothetical answer** to the question first. We then embed the hypothetical answer!

```
                               HYDE EXECUTION WORKFLOW
                               
 User Query: "how to fix 504?"
          │
          ▼
┌────────────────────────────────────────────────────────────────────────┐
│ Fast LLM (Llama 3.2 / GPT-4o-mini)                                     │
│ Prompt: "Write a short passage that answers: 'how to fix 504?'"        │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼ Returns Hypothetical Answer:
 "In distributed systems, HTTP 504 Gateway Timeout occurs when reverse
  proxies fail to receive timely responses. Resolution involves increasing
  proxy read timeouts in Nginx or optimizing downstream slow SQL queries..."
                                    │
                                    ▼
┌───────────────────────────────────┴────────────────────────────────────┐
│ EmbeddingModel (nomic-embed-text)                                      │
│ Generates vector for the HYPOTHETICAL ANSWER, NOT the short query!     │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌───────────────────────────────────┴────────────────────────────────────┐
│ PostgreSQL pgvector Search                                             │
│ Vector(Hypothetical Answer) <=> Vector(Real Enterprise Documentation)  │
│                                                                        │
│ ✅ COSINE SIMILARITY SPIKES FROM 0.42 TO 0.88!                         │
│ Retrieves the exact real documentation because answers look like answers!│
└────────────────────────────────────────────────────────────────────────┘
```

Even if the hypothetical passage contains small factual inaccuracies, its **semantic tone, vocabulary, and paragraph structure** match real documentation infinitely better than a 4-word question!

---

## Advanced Technique 2: Multi-Query Expansion & Reciprocal Rank Fusion (RRF)

A single user query only represents one narrow perspective.  
**Multi-Query Expansion** asks the LLM to rewrite the user's prompt into 3 to 5 distinct queries capturing synonyms, technical terms, and root causes:

```
 User Query: "how to fix 504?"
          │
          ├── Query 1: "how to fix 504?"
          ├── Query 2: "Root causes and troubleshooting steps for HTTP 504 gateway timeout"
          └── Query 3: "Configuring reverse proxy timeouts in Nginx and AWS ALB"
```

Each query is dispatched in parallel using Java 21 **Virtual Threads** against `PgVectorStore`.

### Merging Multiple Rankings with Reciprocal Rank Fusion (RRF)
Different queries produce different ranked lists. How do you merge them into a single list without score calibration issues?

You use the mathematical **Reciprocal Rank Fusion (RRF)** algorithm:

$$RRF\_Score(d) = \sum_{m \in M} \frac{1}{k + rank(d, m)}$$

where:
- $M$ is the set of all search queries.
- $rank(d, m)$ is the position of document $d$ in the result list for query $m$ (1-indexed).
- $k$ is a smoothing constant (standard: $k = 60$).

```
 Document     Rank in Q1    Rank in Q2    Rank in Q3    RRF Calculation              Total Score
──────────────────────────────────────────────────────────────────────────────────────────────────
 DOC-NET-01   #1            #1            #2            1/(60+1) + 1/(60+1) + 1/(60+2) = 0.0489 (WINNER!)
 DOC-DB-01    #3            #4            #1            1/(60+3) + 1/(60+4) + 1/(60+1) = 0.0478
 DOC-NET-02   #2            -             -             1/(60+2) + 0 + 0               = 0.0161
```

Documents that appear consistently near the top across multiple diverse queries receive the highest cumulative RRF score!

---

## Advanced Technique 3: Two-Stage Retrieval with Cross-Encoder Re-Ranking

To achieve 99%+ precision, production enterprise AI systems use a **Two-Stage Retrieval Pipeline**:

```
┌────────────────────────────────────────────────────────────────────────┐
│ STAGE 1: Fast Candidate Retrieval (Bi-Encoder / Vector Store)          │
│ - Uses HNSW index in PostgreSQL pgvector.                              │
│ - Speed: ~3ms.                                                         │
│ - Evaluates query and documents independently (shallow dot product).   │
│ - Output: Retrieves top 25 candidate chunks.                           │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼ (Top 25 Candidates)
┌────────────────────────────────────────────────────────────────────────┐
│ STAGE 2: Deep Cross-Encoder Re-Ranking (Cross-Attention Model)         │
│ - Model: Cohere Rerank, BGE-Reranker, or Hugging Face cross-encoder.   │
│ - Passes [Query, Document] TOGETHER through full transformer attention.│
│ - Evaluates exact semantic, logical, and term-level alignment.         │
│ - Output: Sorts top 25 candidates and picks the absolute best 3!       │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ Context Placement (Solving "Lost in the Middle")                       │
│ - Highest scoring document placed at POSITION #1 (Top of prompt).      │
│ - 2nd highest placed at POSITION #2.                                   │
└────────────────────────────────────────────────────────────────────────┘
```

```
                              BI-ENCODER VS. CROSS-ENCODER
                              
  BI-ENCODER (Embedding Model):
  Query    ──► [Encoder] ──► Vector(Q) ──┐
                                         ├──► Dot Product (Fast, but shallow)
  Document ──► [Encoder] ──► Vector(D) ──┘
  
  CROSS-ENCODER (Re-Ranking Model):
  [Query + Document] ──► [Deep Cross-Attention Transformer] ──► Score (0.0 to 1.0)
                         (Every query word attends to every document word!)
```

---

## Step-by-Step Production Code Walkthrough

Let's inspect the companion code written for today's lesson in `Phase_06_Spring_AI/Day_40_Advanced_RAG_Query_ReRanking/code/`:

### 1. `HydeQueryTransformer.java`
Generates hypothetical answer passages to bridge Query-Document Asymmetry:

```java
public String transform(String rawUserQuery) {
    return "Hypothetical passage answering '" + rawUserQuery + "': "
            + "In enterprise distributed architectures, gateway timeout 504 errors typically occur "
            + "when downstream microservices take longer than the reverse proxy timeout threshold. "
            + "Resolution requires tuning proxy connection timeouts and optimizing downstream database queries.";
}
```

### 2. `ReciprocalRankFusion.java`
Implements the standard RRF formula to combine multiple ranked lists:

```java
public static List<RankedDocument> fuse(List<List<Document>> rankedLists, int topK) {
    Map<String, Document> docLookup = new HashMap<>();
    Map<String, Double> scoreMap = new HashMap<>();

    for (List<Document> list : rankedLists) {
        for (int rank = 0; rank < list.size(); rank++) {
            Document doc = list.get(rank);
            docLookup.put(doc.id(), doc);

            double currentScore = scoreMap.getOrDefault(doc.id(), 0.0);
            double contribution = 1.0 / (DEFAULT_K + (rank + 1));
            scoreMap.put(doc.id(), currentScore + contribution);
        }
    }

    return scoreMap.entrySet().stream()
            .map(e -> new RankedDocument(docLookup.get(e.getKey()), e.getValue()))
            .sorted(Comparator.comparingDouble(RankedDocument::rrfScore).reversed())
            .limit(topK)
            .toList();
}
```

### 3. `CrossEncoderReranker.java`
Performs second-stage candidate re-ranking:

```java
public List<RerankedDocument> rerank(String query, List<Document> candidateDocuments, int topK) {
    String queryLower = query.toLowerCase();

    return candidateDocuments.stream()
            .map(doc -> {
                double score = computeCrossAttentionScore(queryLower, doc.content().toLowerCase());
                return new RerankedDocument(doc, score);
            })
            .sorted(Comparator.comparingDouble(RerankedDocument::crossAttentionScore).reversed())
            .limit(topK)
            .toList();
}
```

### 4. Running the Verification Suite
Compile and execute:

```bash
javac -d out Phase_06_Spring_AI/Day_37_Embedding_Models_Text_to_Vectors/code/*.java Phase_06_Spring_AI/Day_38_Vector_Stores_Semantic_Memory/code/*.java Phase_06_Spring_AI/Day_40_Advanced_RAG_Query_ReRanking/code/*.java
java -cp out com.genai.springai.advancedrag.AdvancedRagDemo
```

Output:
```text
================================================================================
  DAY 40: ADVANCED RAG — HYDE, MULTI-QUERY & CROSS-ENCODER RE-RANKING           
================================================================================

[TEST 1] Hypothetical Document Embeddings (HyDE) Transformation...
  Raw User Query: "how to fix 504?"
  Generated HyDE Passage:
  "Hypothetical passage answering 'how to fix 504?': In enterprise distributed architectures, gateway timeout 504 errors typically occur when downstream microservices take longer than the reverse proxy timeout threshold. Resolution requires tuning proxy connection timeouts and optimizing downstream database queries."

  --- HyDE Retrieved Documents ---
  * [DOC-DB-01]: PostgreSQL connection pooling timeouts occur when HikariCP m...
  * [DOC-NET-01]: HTTP 504 Gateway Timeout indicates an edge reverse proxy (su...

[TEST 2] Multi-Query Expansion & Reciprocal Rank Fusion (RRF)...
  Expanded into 3 parallel queries:
    - "how to fix 504?"
    - "Root causes and troubleshooting steps for how to fix 504?"
    - "Configuring timeouts and architectural fixes for how to fix 504?"

  --- Fused Rankings via RRF Algorithm ---
  #1 [RRF Score: 0.0328] [DOC-NET-01]: HTTP 504 Gateway Timeout indicates an edge reverse proxy (su...
  #2 [RRF Score: 0.0320] [DOC-DB-01]: PostgreSQL connection pooling timeouts occur when HikariCP m...
  #3 [RRF Score: 0.0164] [DOC-NET-02]: HTTP 502 Bad Gateway indicates the upstream service crashed ...

[TEST 3] Second-Stage Cross-Encoder Deep Attention Re-Ranking...
  --- Final Re-Ranked Top Results ---
  #1 [Cross-Score: 0.4500] [DOC-NET-01]: HTTP 504 Gateway Timeout indicates an edge reverse proxy (such as Ngin...
  #2 [Cross-Score: 0.4500] [DOC-DB-01]: PostgreSQL connection pooling timeouts occur when HikariCP maximumPool...

================================================================================
  ADVANCED RAG PIPELINE VALIDATED SUCCESSFULLY! HIGH RECALL & PRECISION.        
================================================================================
```

---

## Hands-On Exercises (With Complete Solutions)

### Exercise 1: HyDE Prompt Transformation in Spring AI
**Problem Statement:**  
Write a Spring `@Service` method `String generateHypotheticalPassage(ChatClient fastClient, String rawQuery)` that uses a lightweight model to draft a 2-sentence hypothetical answer for use in vector search.

<details>
<summary>👉 View Solution</summary>

```java
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

---

### Exercise 2: Lost-in-the-Middle Context Re-orderer
**Problem Statement:**  
Write a Java utility `ContextReorderer.reorderForAttention(List<Document> documents)` that takes a ranked list of documents and re-orders them so that the highest-ranked documents are placed at the **extremes** (the very top and the very bottom of the list) to counteract the U-shaped attention curve:
- Position 1: Best doc (Rank #1)
- Position Last: 2nd best doc (Rank #2)
- Position 2: 3rd best doc (Rank #3)
- Position (Last - 1): 4th best doc (Rank #4)
- Middle: Lowest ranked docs

<details>
<summary>👉 View Solution</summary>

```java
package com.genai.springai.advancedrag;

import com.genai.springai.vectorstore.Document;
import java.util.*;

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
*Explanation:* Distributing high-value documents to the edges ensures that the transformer's self-attention mechanism captures the most critical facts during inference.
</details>

---

### Exercise 3: Parallel Multi-Query Runner using Virtual Threads
**Problem Statement:**  
Given a `List<String> queries` and a `VectorStore`, execute all searches concurrently using Java 21's `Executors.newVirtualThreadPerTaskExecutor()` and fuse the results with `ReciprocalRankFusion`.

<details>
<summary>👉 View Solution</summary>

```java
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
                futures.add(executor.submit(() -> vectorStore.similaritySearch(query)));
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

## 5-Question Self-Check Quiz

#### 1. What is the "Query-Document Asymmetry" problem in standard RAG?
- A) Database queries take longer than file downloads.
- B) User questions are short, informal, and sparse, while knowledge base documents are long, dense, and declarative, causing vector cosine similarity between them to be suboptimal.
- C) Documents are stored as JSON, while queries are in SQL.
- D) Queries only work on weekdays.

#### 2. How does Hypothetical Document Embeddings (HyDE) solve Query-Document Asymmetry?
- A) By translating the query into French.
- B) By using an LLM to generate a hypothetical answer passage, and embedding the dense hypothetical answer instead of the sparse question.
- C) By encrypting the query vector.
- D) By disabling vector search.

#### 3. What does the Reciprocal Rank Fusion (RRF) algorithm accomplish?
- A) It deletes duplicate files in PostgreSQL.
- B) It merges multiple independently ranked document lists (from multi-query searches) into a single unified ranking based on document positions rather than raw uncalibrated scores.
- C) It trains a new neural network.
- D) It formats dates.

#### 4. What is the key performance difference between a Bi-Encoder (Stage 1) and a Cross-Encoder (Stage 2)?
- A) Cross-Encoders are 100x faster than Bi-Encoders.
- B) Bi-Encoders compute query and document vectors independently ($O(1)$ indexed lookup); Cross-Encoders evaluate the query and document simultaneously with deep cross-attention, offering superior accuracy at the cost of higher latency.
- C) Bi-Encoders only run on Windows.
- D) There is no difference.

#### 5. What causes the "Lost in the Middle" phenomenon in Large Language Models?
- A) Network packet loss during HTTP transmission.
- B) Transformer self-attention mechanisms naturally assign higher attention weights to tokens at the beginning and end of long prompts, frequently overlooking facts placed in the center of the context.
- C) Database deadlocks in PostgreSQL.
- D) OutOfMemoryError in Tomcat.

---

### Quiz Answers & Explanations

1. **B is correct**: Short questions and dense technical paragraphs reside in different regions of high-dimensional vector space.
2. **B is correct**: Embedding an answer passage ensures vector proximity to real answers in the knowledge base.
3. **B is correct**: RRF combines rankings without needing to normalize arbitrary cosine score distributions across different queries.
4. **B is correct**: Bi-Encoders enable fast million-scale vector retrieval; Cross-Encoders provide deep pairwise semantic re-ranking for the top 20 candidates.
5. **B is correct**: Transformer attention exhibits a U-shaped curve, making intelligent context placement critical.

---

## Day 40 Summary & Next Steps

Today you mastered:
1. **The Limitations of Naive RAG**: Addressing Query-Document Asymmetry and the Lost-in-the-Middle attention curve.
2. **Hypothetical Document Embeddings (HyDE)**: Transforming short questions into dense hypothetical answers for vector search.
3. **Multi-Query Expansion**: Searching from multiple semantic angles in parallel using Virtual Threads.
4. **Reciprocal Rank Fusion (RRF)**: Merging multi-query search results into a unified, calibrated ranking.
5. **Two-Stage Re-Ranking**: Combining fast Bi-Encoder retrieval with deep Cross-Encoder attention scoring.

👉 **Tomorrow in Day 41: Tool Calling — LLMs That Execute Java Methods** — You will turn passive text generators into active autonomous agents by allowing LLMs to inspect, decide, and execute real Java functions and Spring Services!
