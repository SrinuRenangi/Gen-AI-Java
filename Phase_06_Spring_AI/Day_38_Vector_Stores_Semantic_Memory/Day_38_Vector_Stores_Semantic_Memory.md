# Day 38: Vector Stores — Semantic Memory for Your App
## VectorStore Abstraction, Document Metadata, PostgreSQL pgvector (HNSW) & Hybrid Filtering

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [◀ Day 37: Embedding Models](../Day_37_Embedding_Models_Text_to_Vectors/Day_37_Embedding_Models_Text_to_Vectors.md) | [All 60 Days Overview](../../README.md) | [Day 39: RAG — Retrieval-Augmented Generation ▶](../Day_39_RAG_Retrieval_Augmented_Generation/Day_39_RAG_Retrieval_Augmented_Generation.md) |

---

## What Will You Learn Today?

Yesterday in Day 37, you mastered embedding models and in-memory vector math. However, keeping vectors in Java heap memory has severe limits: if your application restarts, all embeddings vanish; if your document corpus grows to millions of paragraphs, your JVM crashes with `OutOfMemoryError`.

To scale AI applications, enterprises require a persistent **Vector Database**.

Today, you will master **Vector Stores** in Spring AI and Java 21:
- The **`VectorStore`** interface: Spring AI's portable abstraction over vector databases.
- The **`Document`** model: Packaging text chunks, unique identifiers, high-dimensional vector arrays, and structured metadata.
- Deep dive into **PostgreSQL `pgvector`**: Deploying enterprise vector search inside the existing PostgreSQL databases your organization already trusts.
- Vector Indexing: **HNSW (Hierarchical Navigable Small World)** graphs vs. **IVFFlat** clusters, tuning `m`, `ef_construction`, and `ef_search`.
- **Hybrid Search & Metadata Filtering**: Combining vector cosine distance with SQL `WHERE` clauses (e.g. searching for policies *only* within a user's department or tenant).
- Building an automated **Document Ingestion Pipeline** in Spring Boot 3.

---

## Real-World Analogy: Library Card Catalog vs. Associative Neural Memory

Imagine searching for information in two different types of libraries:

```
+---------------------------------------------------------------------------------------------------+
|                                  RELATIONAL B-TREE VS. VECTOR STORE                               |
|                                                                                                   |
|  SCENARIO 1: Traditional Relational Database (The Card Catalog B-Tree Index)                     |
|  - Books are indexed alphabetically by exact Title, Author, or ISBN.                             |
|  - Query: "Find books containing the exact string 'microservices resilience'."                   |
|  - Mechanism: B-Tree binary search. Instantaneous if exact keywords match.                        |
|  - Failure: If a book is titled "Fault-Tolerant Distributed Architectures", the card catalog      |
|    misses it completely! Zero semantic understanding.                                            |
|                                                                                                   |
|  SCENARIO 2: Vector Store (The Associative Neural Memory)                                         |
|  - Every paragraph of every book is mapped to a coordinate in conceptual vector space.            |
|  - Query: "How to handle cascading service crashes during peak traffic?"                         |
|  - Mechanism: Finds books whose concept vectors reside within a 5-degree angle in vector space.   |
|  - Result: Instantly retrieves "Fault-Tolerant Distributed Architectures" and "Circuit Breaker     |
|    Pattern in Java" with a 94% relevance score, even though the query words never appeared!      |
+---------------------------------------------------------------------------------------------------+
```

---

## Spring AI `VectorStore` Architecture

Just as Spring Data abstracts relational databases behind `JpaRepository`, Spring AI abstracts vector databases behind the **`VectorStore`** interface:

```
                               SPRING AI VECTORSTORE ECOSYSTEM
                               
                               ┌───────────────────────────────┐
                               │          VectorStore          │
                               └───────────────┬───────────────┘
                                               │
                 ┌─────────────────────────────┼─────────────────────────────┐
                 ▼                             ▼                             ▼
    ┌───────────────────────────┐ ┌───────────────────────────┐ ┌───────────────────────────┐
    │       PgVectorStore       │ │      RedisVectorStore     │ │     MilvusVectorStore     │
    │   (PostgreSQL pgvector)   │ │    (Redis In-Memory VSS)  │ │   (Distributed Scale)     │
    └────────────┬──────────────┘ └────────────┬──────────────┘ └────────────┬──────────────┘
                 │                             │                             │
                 ▼                             ▼                             ▼
          PostgreSQL Server               Redis Cluster                Milvus Cluster
```

Supported vector stores in Spring AI include:
- **PostgreSQL pgvector** (Recommended for 90% of enterprise applications)
- **Redis Vector Search** (Ultra-low latency in-memory vector retrieval)
- **Qdrant**, **Milvus**, **Weaviate**, **Chroma** (Dedicated vector engines)
- **Neo4j Vector** (Graph-augmented vector relationships)
- **Elasticsearch / OpenSearch** (Hybrid BM25 keyword + vector search)

---

## 🧭 The Mid-Level Java Developer Bridge: How Vector Stores Work in Spring AI

If you've spent your career using `JpaRepository` with SQL queries, working with a `VectorStore` in Spring AI is remarkably familiar:

| If You Know In Spring Data JPA... | Spring AI `VectorStore` Equivalent | Plain English Meaning |
| :--- | :--- | :--- |
| **`@Entity User`** | **`org.springframework.ai.document.Document`** | The unit of data: holds the text chunk, metadata (`Map<String, Object>`), and embedding vector. |
| **`repository.saveAll(list)`** | **`vectorStore.add(List<Document>)`** | Automatically embeds the text chunks and saves them into the vector database. |
| **`SELECT * ... ORDER BY ... LIMIT 5`** | **`vectorStore.similaritySearch(SearchRequest.query(...).withTopK(5))`** | Returns the 5 most semantically similar paragraphs to the user's question. |
| **`WHERE tenant_id = 'acme'`** | **`.withFilterExpression("tenant == 'acme'")`** | Metadata filter: filters results by customer/security constraints before vector ranking. |
| **Swappable DB Drivers** | Switch from Postgres to Redis in `pom.xml` | Just like swapping MySQL for Postgres, your Java code calling `VectorStore` never changes! |

---

## The Spring AI `Document` Model

In Spring AI, the atomic unit of semantic memory is the **`Document`**:

```java
package org.springframework.ai.document;

import java.util.Map;

public class Document {
    private final String id;                      // Unique identifier (UUID or custom doc key)
    private final String content;                 // Text chunk to be searched & read by the LLM
    private final Map<String, Object> metadata;   // Key-value attributes for filtering (author, tenant, date)
    private List<Double> embedding;               // The high-dimensional float array
    
    // Constructors and utility methods...
}
```

### Why Metadata is Critical:
Without metadata, vector search is a "black box" that returns raw text chunks.  
With metadata, you can attach:
- `tenantId`: "ACME_CORP" (Enforces multi-tenant data isolation)
- `department`: "LEGAL" (Restricts document access by RBAC role)
- `sourceUrl`: "https://wiki.corp.com/hr/benefits" (Provides citations to the user)
- `ingestionDate`: "2026-09-09" (Enables date-based sorting and freshness filters)

---

## Deep Dive: PostgreSQL `pgvector` Integration

PostgreSQL with the `pgvector` extension is the gold standard for enterprise AI architectures. It eliminates the operational cost, security compliance burden, and synchronizing headaches of maintaining a separate standalone vector database!

### Step 1: Maven Starter
Add the official Spring AI pgvector starter to your `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-pgvector-store-spring-boot-starter</artifactId>
</dependency>
```

### Step 2: Configure `application.yml`
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/genai_db
    username: postgres
    password: postgrespassword
  ai:
    vectorstore:
      pgvector:
        index-type: HNSW                      # Use HNSW graph index for speed
        distance-type: COSINE_DISTANCE        # Use cosine similarity (<=>)
        dimensions: 1536                      # Must match embedding model (e.g. OpenAI 1536, Ollama 768)
        initialize-schema: true               # Auto-create vector_store table on startup
```

### Step 3: The Underlying Database Table Schema
When Spring Boot starts up with `initialize-schema: true`, it executes:

```sql
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS vector_store (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    content TEXT,
    metadata JSONB,
    embedding VECTOR(1536)
);

-- Build the Hierarchical Navigable Small World (HNSW) index
CREATE INDEX IF NOT EXISTS vector_store_hnsw_idx 
ON vector_store USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);
```

---

## Indexing Strategies: HNSW vs. IVFFlat

In Day 26, you explored vector operations. In production, your index choice determines search speed and recall accuracy:

```
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                 HNSW vs. IVFFLAT IN PGVECTOR                                    │
├───────────────────────────────┬─────────────────────────────────┬───────────────────────────────┤
│ Architectural Feature         │ HNSW (Recommended)              │ IVFFlat                       │
├───────────────────────────────┼─────────────────────────────────┼───────────────────────────────┤
│ Mechanism                     │ Multi-layer proximity graphs    │ Inverted clusters (Voronoi)   │
├───────────────────────────────┼─────────────────────────────────┼───────────────────────────────┤
│ Query Latency                 │ **Ultra-fast (<5ms)**           │ Moderate (15–50ms)            │
├───────────────────────────────┼─────────────────────────────────┼───────────────────────────────┤
│ Build Time & RAM              │ Slower build, higher RAM        │ Faster build, lower RAM       │
├───────────────────────────────┼─────────────────────────────────┼───────────────────────────────┤
│ Accuracy (Recall)             │ **98%–99.9%**                   │ 85%–95%                       │
├───────────────────────────────┼─────────────────────────────────┼───────────────────────────────┤
│ Production Verdict            │ **Default for production apps** │ Only for memory-constrained   │
│                               │                                 │ systems with huge datasets    │
└───────────────────────────────┴─────────────────────────────────┴───────────────────────────────┘
```

### Tuning HNSW Parameters:
- **`m` (default: 16)**: Max number of bidirectional links per node. Higher `m` improves recall for high-dimensional data at the cost of build time.
- **`ef_construction` (default: 64)**: Size of the dynamic candidate list evaluated during index construction.
- **`ef_search` (default: 40)**: Size of the candidate list evaluated during query execution. Increase at runtime for higher recall:
  ```sql
  SET hnsw.ef_search = 100;
  ```

---

## Hybrid Search & Metadata Filtering

Pure vector similarity search is often not enough. Consider this query:  
*"What is our bereavement leave policy?"*

If your vector store returns bereavement leave policies from **Company B** (in a multi-tenant system) or the policy for **Executives Only** (when an intern asks), your application has committed a severe security and compliance breach!

### Using Spring AI's `FilterExpressionBuilder`:
Spring AI provides a fluent, SQL-independent Filter Expression API that converts directly to native PostgreSQL `jsonb` queries:

```java
package com.genai.springai.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EnterpriseSearchService {

    private final VectorStore vectorStore;

    public EnterpriseSearchService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public List<Document> searchCompanyPolicies(String query, String tenantId, String userRole) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();

        SearchRequest request = SearchRequest.builder()
            .query(query)
            .topK(4)                                   // Return top 4 most relevant chunks
            .similarityThreshold(0.75)                 // Drop weak matches below 75% similarity
            .filterExpression(b.and(
                b.eq("tenantId", tenantId),             // Enforce tenant boundary
                b.in("confidentiality", "PUBLIC", userRole) // Role-based access control
            ).build())
            .build();

        return vectorStore.similaritySearch(request);
    }
}
```

Behind the scenes, Spring AI converts this into native PostgreSQL SQL:
```sql
SELECT id, content, metadata, 1 - (embedding <=> $queryVector) AS similarity
FROM vector_store
WHERE (metadata->>'tenantId' = 'ACME')
  AND (metadata->>'confidentiality' IN ('PUBLIC', 'ROLE_ENGINEER'))
ORDER BY embedding <=> $queryVector
LIMIT 4;
```

---

## Building an Automated Ingestion Pipeline

Here is a complete production service that reads text documents, splits them into overlapping chunks using token thresholds, and persists them into PostgreSQL `pgvector`:

```java
package com.genai.springai.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DocumentIngestionService {

    private final VectorStore vectorStore;

    public DocumentIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void ingestArticle(String articleId, String title, String fullText, String tenantId) {
        List<String> textChunks = chunkTextWithOverlap(fullText, 500, 50);
        List<Document> documentsToIngest = new ArrayList<>();

        for (int i = 0; i < textChunks.size(); i++) {
            Map<String, Object> metadata = Map.of(
                "articleId", articleId,
                "title", title,
                "chunkIndex", i,
                "totalChunks", textChunks.size(),
                "tenantId", tenantId,
                "ingestedAt", System.currentTimeMillis()
            );

            Document doc = new Document(
                UUID.randomUUID().toString(),
                textChunks.get(i),
                metadata
            );
            documentsToIngest.add(doc);
        }

        // Batch persist to pgvector (generates embeddings and inserts in single transaction)
        vectorStore.add(documentsToIngest);
        System.out.println("Ingested " + documentsToIngest.size() + " chunks for article: " + title);
    }

    private List<String> chunkTextWithOverlap(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int step = chunkSize - overlap;
        for (int i = 0; i < text.length(); i += step) {
            chunks.add(text.substring(i, Math.min(i + chunkSize, text.length())));
            if (i + chunkSize >= text.length()) break;
        }
        return chunks;
    }
}
```

---

## Step-by-Step Production Code Walkthrough

Let's review the companion code written for today's lesson in `Phase_06_Spring_AI/Day_38_Vector_Stores_Semantic_Memory/code/`:

### 1. `Document.java`
Models the immutable semantic memory record:

```java
public record Document(
        String id,
        String content,
        Map<String, Object> metadata,
        float[] embedding
) {
    public Document(String id, String content, Map<String, Object> metadata) {
        this(id, content, Collections.unmodifiableMap(new HashMap<>(metadata)), null);
    }
}
```

### 2. `SearchRequest.java`
Provides a builder for queries, limits, similarity thresholds, and metadata predicate filters:

```java
public record SearchRequest(
        String query,
        int topK,
        double similarityThreshold,
        Predicate<Map<String, Object>> filterExpression
) {
    public static class Builder { ... }
}
```

### 3. `PgVectorStoreSimulator.java`
Simulates PostgreSQL pgvector indexing, HNSW cosine similarity ranking, and metadata filtering:

```java
public List<Document> similaritySearch(SearchRequest request) {
    float[] queryVector = embeddingModel.embed(request.query());

    return store.values().stream()
            .filter(doc -> request.filterExpression().test(doc.metadata()))
            .map(doc -> new ScoredDoc(doc, VectorMath.cosineSimilarity(queryVector, doc.embedding())))
            .filter(scored -> scored.score() >= request.similarityThreshold())
            .sorted(Comparator.comparingDouble(ScoredDoc::score).reversed())
            .limit(request.topK())
            .map(ScoredDoc::doc)
            .toList();
}
```

### 4. Running the Verification Suite
Compile and execute:

```bash
javac -d out Phase_06_Spring_AI/Day_37_Embedding_Models_Text_to_Vectors/code/*.java Phase_06_Spring_AI/Day_38_Vector_Stores_Semantic_Memory/code/*.java
java -cp out com.genai.springai.vectorstore.VectorStoreDemo
```

Output:
```text
================================================================================
  DAY 38: SPRING AI VECTORSTORE & PGVECTOR SIMULATION DEMONSTRATION             
================================================================================

  Active Vector Store: PostgreSQL pgvector (HNSW Cosine Index)

[TEST 1] Ingesting Corporate Policies with Department & Tenant Metadata...
  ✅ Successfully ingested and indexed 5 documents with HNSW vectors.

[TEST 2] Semantic Query: "What is the retirement pension 401k savings policy?"
--- Top 2 Matches (Unfiltered) ---
  [DOC-FIN-01] [FINANCE]: Employees can allocate up to 15% of base salary into company 401k with 50% match.
  [DOC-ENG-02] [ENGINEERING]: PostgreSQL pgvector HNSW index is the standard storage engine for all RAG vector pipelines.

[TEST 3] Hybrid Search with Metadata Filter: department == 'ENGINEERING'
--- Engineering Matches Only ---
  [DOC-ENG-02] [ENGINEERING]: PostgreSQL pgvector HNSW index is the standard storage engine for all RAG vector pipelines.
  [DOC-ENG-01] [ENGINEERING]: All production backend microservices must run on Java 21 LTS using Virtual Threads.

[TEST 4] Deleting Document DOC-FIN-02...
  ✅ Deleted DOC-FIN-02. Vector store now contains 4 documents.

================================================================================
  VECTOR STORE INGESTION & HYBRID SEARCH VERIFIED SUCCESSFULLY!                 
================================================================================
```

---

## Hands-On Exercises (With Complete Solutions)

### Exercise 1: Multi-Tenant Isolated Vector Search
**Problem Statement:**  
Write a method `searchTenantDocs(VectorStore vs, String query, String tenantId, int topK)` that uses `SearchRequest` to guarantee that documents belonging to other tenants are NEVER returned.

<details>
<summary>👉 View Solution</summary>

```java
public List<Document> searchTenantDocs(VectorStore vectorStore, String query, String tenantId, int topK) {
    FilterExpressionBuilder b = new FilterExpressionBuilder();

    SearchRequest request = SearchRequest.builder()
        .query(query)
        .topK(topK)
        .filterExpression(b.eq("tenantId", tenantId).build())
        .build();

    return vectorStore.similaritySearch(request);
}
```
</details>

---

### Exercise 2: Similarity Threshold Relevance Guard
**Problem Statement:**  
If a user asks a nonsensical question like *"What is the airspeed velocity of an unladen swallow?"* against an enterprise banking knowledge base, the vector store will still mathematically return the closest 4 documents, even if they have a low similarity score (e.g. 0.15).  
Configure `SearchRequest` with a minimum similarity threshold of `0.72` so that completely irrelevant queries return an empty list instead of misleading hallucinated matches.

<details>
<summary>👉 View Solution</summary>

```java
public List<Document> safeThresholdSearch(VectorStore vectorStore, String query) {
    SearchRequest request = SearchRequest.builder()
        .query(query)
        .topK(3)
        .similarityThreshold(0.72) // Discards any match below 72% semantic similarity
        .build();

    List<Document> matches = vectorStore.similaritySearch(request);
    if (matches.isEmpty()) {
        System.out.println("No relevant enterprise knowledge found above 72% threshold.");
    }
    return matches;
}
```
</details>

---

### Exercise 3: Batch Document Deletion by Metadata Tag
**Problem Statement:**  
When an enterprise contract expires, all associated vector chunks must be purged from `VectorStore`. Write a method that queries all document IDs where `metadata.contractId == 'CTR-2026-X'` and deletes them from `VectorStore`.

<details>
<summary>👉 View Solution</summary>

```java
@Service
public class DocumentLifecycleService {

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    public DocumentLifecycleService(VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void purgeContractDocuments(String contractId) {
        // Query IDs directly from pgvector JSONB column
        String sql = "SELECT id::text FROM vector_store WHERE metadata->>'contractId' = ?";
        List<String> idsToDelete = jdbcTemplate.queryForList(sql, String.class, contractId);

        if (!idsToDelete.isEmpty()) {
            vectorStore.delete(idsToDelete);
            System.out.println("Purged " + idsToDelete.size() + " vector chunks for expired contract: " + contractId);
        }
    }
}
```
</details>

---

## 5-Question Self-Check Quiz

#### 1. What is the fundamental difference between an `EmbeddingModel` and a `VectorStore`?
- A) EmbeddingModel is written in Python; VectorStore is written in Java.
- B) EmbeddingModel computes float arrays from text; VectorStore persists those vectors in a database and executes indexed nearest-neighbor similarity searches.
- C) VectorStore only stores passwords.
- D) EmbeddingModel is an SQL dialect.

#### 2. Why is PostgreSQL with `pgvector` generally preferred over niche standalone vector databases in enterprise architectures?
- A) Standalone vector databases cannot store text.
- B) pgvector allows enterprises to leverage their existing PostgreSQL infrastructure, backups, ACID transactions, and security audits without adding another distributed database.
- C) pgvector is written in Rust.
- D) PostgreSQL is 100% free of CPU usage.

#### 3. In pgvector, what does the `<=>` operator represent?
- A) Equal or greater than.
- B) Cosine distance between two vectors.
- C) String concatenation.
- D) JSON array search.

#### 4. Which vector index algorithm provides the highest query throughput (<5ms) and highest recall (99%+) for production vector search?
- A) B-Tree
- B) Hash Index
- C) HNSW (Hierarchical Navigable Small World)
- D) Full Text GIN Index

#### 5. What is "Hybrid Search" with metadata filtering?
- A) Searching both images and audio at the same time.
- B) Combining vector semantic similarity with structured SQL `WHERE` clauses (e.g., filtering by tenant ID, department, or date) to restrict search scope before ranking.
- C) Using two different LLMs simultaneously.
- D) Storing half the data in MongoDB.

---

### Quiz Answers & Explanations

1. **B is correct**: `EmbeddingModel` translates strings to coordinates; `VectorStore` stores and indexes those coordinates for efficient retrieval.
2. **B is correct**: Keeping vector embeddings in PostgreSQL alongside relational tables eliminates data sync drift and leverages proven enterprise operational infrastructure.
3. **B is correct**: In pgvector, `<=>` computes Cosine Distance ($1 - \text{Cosine Similarity}$).
4. **C is correct**: HNSW constructs a multi-layer geometric graph that enables logarithmic-time nearest neighbor exploration.
5. **B is correct**: Hybrid metadata filtering ensures that semantic searches honor strict organizational, security, and tenant boundaries.

---

## Day 38 Summary & Next Steps

Today you mastered:
1. **The `VectorStore` Abstraction**: Spring AI's unified interface for semantic vector storage.
2. **The `Document` Model**: Managing text chunks, IDs, vector arrays, and JSON metadata.
3. **PostgreSQL `pgvector` Mastery**: HNSW graph indexing, cosine distance operations, and schema initialization.
4. **Hybrid Search & Metadata Filtering**: Applying fine-grained security and organizational constraints to vector queries.
5. **Automated Ingestion Pipelines**: Chunking, embedding, and persisting enterprise documents at scale.

👉 **Tomorrow in Day 39: RAG — Retrieval-Augmented Generation** — You will combine everything you have built: connecting `ChatClient`, `EmbeddingModel`, and `VectorStore` into an end-to-end RAG system that grounds LLMs with your enterprise data and eliminates hallucinations!
