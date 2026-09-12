# Day 38: Vector Stores — Semantic Memory for Your App

[← Previous: Day 37 - Embedding Models](../Day_37_Embedding_Models_Text_to_Vectors/Day_37_Embedding_Models_Text_to_Vectors.md) | [Next: Day 39 - RAG Pipeline →](../Day_39_RAG_Retrieval_Augmented_Generation/Day_39_RAG_Retrieval_Augmented_Generation.md)

---

## 1. Topic Overview
A vector store is a specialized database system optimized to persist, index, and query high-dimensional embedding vectors alongside document text and structured metadata. In enterprise Generative AI architectures, vector stores serve as the long-term semantic memory layer, enabling millisecond nearest-neighbor similarity searches across millions of proprietary documents with strict multi-tenant isolation.

---

## 2. Basic Foundations (True Zero)

### What is a Vector Store?
In Day 37, you converted text into float arrays (`float[]`) and searched them in Java memory. But in a real application:
1. If your Spring Boot microservice restarts, all vectors in RAM are permanently lost.
2. If your enterprise indexes 10 million pages, holding all vectors in JVM heap memory causes an immediate `OutOfMemoryError`.

A **vector store** (or vector database) solves this by persisting text chunks, their vector coordinates, and associated metadata to disk, while building specialized indexes (like HNSW graphs) so you can ask: *"Find the 5 closest paragraphs in meaning to this user's question"* across millions of records in under 5 milliseconds.

### Relatable Physical Analogy: The City Expressway vs. Walking Every Street
Imagine you need to find a specific house in a city of 1,000,000 homes:
- **Naive Search (Brute Force / Linear Scan)**: You walk up to every single house one by one and check the address. Checking 1,000,000 houses takes weeks.
- **HNSW Vector Index (The Multi-Level Expressway)**: The city has high-speed elevated highways with exits to major districts, ramps down to local neighborhoods, and final streets. You take the express highway directly to the right district, exit into the correct subdivision, and check only 15 houses. 

That is exactly how modern vector store indexes (like PostgreSQL `pgvector` with HNSW) operate: instead of calculating cosine similarity against 1,000,000 vectors, they navigate a multi-layer graph to find the closest matches in milliseconds.

### Minimal Beginner-Friendly Working Code
Here is how you persist and search documents using Spring AI's universal `VectorStore` interface:

```java
package com.genai.springai.vectorstore;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SimpleVectorStoreRunner implements CommandLineRunner {

    private final VectorStore vectorStore;

    public SimpleVectorStoreRunner(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(String... args) {
        // 1. Create documents with text content and metadata
        Document doc1 = new Document(
            "Virtual Threads in Java 21 enable high-throughput concurrent I/O applications.",
            Map.of("category", "engineering", "year", 2024)
        );
        Document doc2 = new Document(
            "Employees are eligible for 15 days of annual paid leave after 3 months of service.",
            Map.of("category", "hr", "year", 2024)
        );

        // 2. Add documents to vector store (automatically invokes EmbeddingModel and persists)
        vectorStore.add(List.of(doc1, doc2));

        // 3. Perform semantic similarity search
        List<Document> results = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query("How do lightweight threads improve web server concurrency?")
                .topK(1)
                .build()
        );

        // 4. Inspect retrieved match
        Document topMatch = results.get(0);
        System.out.println("Top Match: " + topMatch.getText());
        System.out.println("Category:  " + topMatch.getMetadata().get("category"));
    }
}
```

### Line-by-Line Walkthrough
1. **`private final VectorStore vectorStore;`**: Injects Spring AI's portable abstraction for vector persistence. Whether the backing store is PostgreSQL `pgvector`, Redis, Milvus, or Qdrant, your application logic remains decoupled and identical.
2. **`Document doc1 = new Document(text, metadataMap)`**: Constructs a Spring AI `Document` containing the raw content string and structured key-value attributes for filtering.
3. **`vectorStore.add(List.of(doc1, doc2))`**: Batch-generates embedding vectors for each document chunk via the configured `EmbeddingModel` and inserts both vectors and metadata into the underlying database.
4. **`SearchRequest.builder().query(...).topK(1).build()`**: Constructs a search request specifying the natural-language query and `topK` (the maximum number of most similar documents to retrieve).
5. **`vectorStore.similaritySearch(request)`**: Translates the query into an embedding vector, executes nearest-neighbor search in the database, and returns the top matching `Document` instances.

---

## 3. Core Concept Walkthrough (Basic → Intermediate)

```
+-------------------------------------------------------------------------------+
|                       SPRING AI VECTORSTORE ECOSYSTEM                         |
+-------------------------------------------------------------------------------+
|                                                                               |
|                             [ VectorStore API ]                               |
|                +---------------------+---------------------+                  |
|                |                     |                     |                  |
|                v                     v                     v                  |
|        [ PgVectorStore ]     [ RedisVectorStore ]  [ MilvusVectorStore ]      |
|         (PostgreSQL +         (Redis In-Memory      (Distributed Cloud        |
|           pgvector)                 VSS)                  Scale)              |
|                |                     |                     |                  |
|                v                     v                     v                  |
|           PostgreSQL            Redis Server          Milvus Pods             |
|                                                                               |
+-------------------------------------------------------------------------------+
```

### The Spring AI `Document` Class
The atomic unit of data stored in any vector store is the `Document`:

```java
package org.springframework.ai.document;

import java.util.Map;
import java.util.List;

public class Document {
    private final String id;                      // Unique ID (UUID or custom identifier)
    private final String text;                    // Content chunk indexed and retrieved
    private final Map<String, Object> metadata;   // Key-value metadata for filtering
    private List<Float> embedding;               // The high-dimensional float coordinates
    // Constructors, builders, and accessors...
}
```

#### Why Metadata is Essential:
Without metadata, vector search is a black box that only returns raw text. Metadata allows you to attach:
- `tenantId`: Enforces multi-tenant data boundaries.
- `department`: Restricts documents to specific corporate departments.
- `confidentiality`: Implements role-based access control (e.g., `PUBLIC`, `INTERNAL`, `RESTRICTED`).
- `sourceUrl`: Enables citations so the user knows where an answer came from.

### PostgreSQL with `pgvector`: The Enterprise Gold Standard
PostgreSQL with the `pgvector` extension allows companies to store embeddings directly inside their existing, ACID-compliant relational databases without needing to manage another distributed database cluster.

#### Maven Dependency:
```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-pgvector-store-spring-boot-starter</artifactId>
</dependency>
```

#### Configuration (`application.yml`):
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/enterprise_ai
    username: postgres
    password: secretpassword
  ai:
    vectorstore:
      pgvector:
        index-type: HNSW                      # Use HNSW for fast graph-based search
        distance-type: COSINE_DISTANCE        # Use cosine distance (<=>)
        dimensions: 1536                      # Must match your embedding model
        initialize-schema: true               # Auto-creates table and index on boot
```

#### Database Schema Generated Automatically:
```sql
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS vector_store (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    content TEXT,
    metadata JSONB,
    embedding VECTOR(1536)
);

CREATE INDEX IF NOT EXISTS vector_store_hnsw_idx 
ON vector_store USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);
```

### HNSW vs. IVFFlat Indexing Strategies
Choosing the right index in `pgvector` is critical for production performance:

| Index Feature | HNSW (Hierarchical Navigable Small World) | IVFFlat (Inverted File Flat) |
|:---|:---|:---|
| **Underlying Structure** | Multi-layer proximity graph | Clustered Voronoi partitions |
| **Search Latency** | **Ultra-low (< 5ms)** | Moderate (15–50ms) |
| **Memory Footprint** | Higher RAM usage | Lower RAM usage |
| **Recall Accuracy** | **98% – 99.9%** | 85% – 95% |
| **Training Required?** | No, builds incrementally on inserts | Yes, requires existing data to train clusters |
| **Enterprise Recommendation** | **Default choice for production** | Only for memory-constrained instances |

---

## 4. Prerequisite & Supporting Concepts

### Prerequisite / Supporting Concept: Relational B-Tree vs. Vector Distance Index
In standard SQL, an index is typically a B-Tree that speeds up exact lookups (`WHERE id = 5`) or range queries (`WHERE age > 21`). A B-Tree cannot find "similar meaning."

In `pgvector`, distance operators calculate proximity:
- `<=>` : **Cosine Distance** ($1 - \text{Cosine Similarity}$). Used when vector angle matters most.
- `<->` : **Euclidean Distance** ($L2$ distance). Used when absolute magnitude matters.
- `<#>` : **Negative Inner Product**. Used for unnormalized dot product searches.

### Prerequisite / Supporting Concept: Hybrid Search & Metadata Filtering
Pure semantic vector search can inadvertently leak sensitive records across organizational boundaries. If an intern searches for "executive bonuses," a pure vector search might return confidential C-suite compensation files.

Spring AI provides `FilterExpressionBuilder` to combine structured SQL filters with vector similarity:

```java
package com.genai.springai.vectorstore;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SecurePolicySearchService {

    private final VectorStore vectorStore;

    public SecurePolicySearchService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public List<Document> searchCompanyPolicies(String query, String tenantId, String userRole) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();

        SearchRequest request = SearchRequest.builder()
            .query(query)
            .topK(4)
            .similarityThreshold(0.70) // Discards any match below 70% similarity
            .filterExpression(b.and(
                b.eq("tenantId", tenantId),
                b.in("confidentiality", "PUBLIC", userRole)
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
WHERE (metadata->>'tenantId' = 'ACME_CORP')
  AND (metadata->>'confidentiality' IN ('PUBLIC', 'ROLE_ENGINEER'))
ORDER BY embedding <=> $queryVector
LIMIT 4;
```

---

## 5. Advanced Depth (Intermediate → Advanced)

### Complete Production Ingestion Pipeline
In production, documents arrive as raw PDFs, HTML pages, or Markdown documents. An ingestion pipeline splits the text into manageable chunks with overlap, attaches metadata, and batch-persists them into `VectorStore`:

```java
package com.genai.springai.vectorstore;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

        // Batch persist to pgvector in a single efficient transaction
        vectorStore.add(documentsToIngest);
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

### Common Anti-Patterns & Production Traps

| Anti-Pattern | Why It Fails in Production | Correct Architectural Pattern |
|:---|:---|:---|
| **Omitting `similarityThreshold`** | Irrelevant queries (e.g., "What is the speed of light?" asked to a HR bot) will still return the closest 4 HR chunks, causing the LLM to hallucinate answers. | Always configure `.similarityThreshold(0.70)` to discard weak matches before LLM prompting. |
| **Dimension Mismatch** | Setting `dimensions: 1536` in `application.yml` while using an Ollama 768-dimension embedding model causes PostgreSQL vector insert errors. | Ensure database column dimension matches your embedding model exactly. |
| **No Filter Indices on JSONB** | Running metadata filters against unindexed `JSONB` columns on tables with millions of rows forces a slow sequential table scan. | Create a GIN index on `metadata`: `CREATE INDEX idx_vec_metadata ON vector_store USING gin (metadata);`. |

---

## 6. Quick Recap
- A **`VectorStore`** persists document chunks and high-dimensional embeddings to disk, providing sub-second nearest-neighbor similarity search.
- Spring AI abstracts vector databases via the **`VectorStore`** interface and the **`Document`** entity.
- **PostgreSQL `pgvector`** is the recommended enterprise choice, uniting relational business data and vector embeddings in a single database.
- **HNSW indexes** use multi-layer proximity graphs to query millions of vectors in under 5 milliseconds with 99%+ recall.
- **Hybrid Search** combines semantic vector similarity with structured metadata filters (tenant ID, role, date) to ensure security and compliance.
- Always configure a **`similarityThreshold`** to prevent returning irrelevant noise to downstream LLMs.

---

## 7. Self-Check Questions & Practice Exercises

### 5-Question Self-Check Quiz

#### Question 1
What is the fundamental difference between an `EmbeddingModel` and a `VectorStore` in Spring AI?
- A) `EmbeddingModel` is written in Python; `VectorStore` is written in Java.
- B) `EmbeddingModel` converts text into float vectors; `VectorStore` persists those vectors in a database and executes indexed nearest-neighbor similarity searches.
- C) `VectorStore` only stores encrypted passwords.
- D) `EmbeddingModel` is an SQL dialect.

#### Question 2
Why is PostgreSQL with `pgvector` often preferred over standalone vector databases in enterprise architectures?
- A) Standalone vector databases cannot store raw text strings.
- B) `pgvector` allows enterprises to leverage existing PostgreSQL infrastructure, backups, ACID transactions, and security audits without adding another distributed system.
- C) `pgvector` is completely free of CPU and memory usage.
- D) Standalone vector databases do not support cosine distance.

#### Question 3
In PostgreSQL `pgvector`, which operator is used for cosine distance?
- A) `<=>`
- B) `<->`
- C) `<#>`
- D) `==`

#### Question 4
Which vector indexing algorithm provides the highest query throughput (<5ms) and highest recall (99%+) for production vector search?
- A) B-Tree
- B) Hash Index
- C) HNSW (Hierarchical Navigable Small World)
- D) GIN Index

#### Question 5
What is "Metadata Filtering" in a vector search query?
- A) Compressing image and audio files before embedding.
- B) Combining vector semantic similarity with structured criteria (e.g., tenant ID, department, creation date) to restrict search scope before ranking.
- C) Running two LLM models simultaneously.
- D) Encrypting text payloads in memory.

---

### Quiz Answers & Explanations
1. **B**: `EmbeddingModel` translates strings to coordinates; `VectorStore` stores and indexes those coordinates for efficient retrieval.
2. **B**: Keeping vector embeddings in PostgreSQL alongside relational tables eliminates data sync drift and leverages proven enterprise operational infrastructure.
3. **A**: In `pgvector`, `<=>` computes Cosine Distance ($1 - \text{Cosine Similarity}$).
4. **C**: HNSW constructs a multi-layer geometric graph that enables logarithmic-time nearest neighbor exploration.
5. **B**: Hybrid metadata filtering ensures that semantic searches honor strict organizational, security, and tenant boundaries.

---

### Hands-On Practice Exercises

#### Exercise 1: Multi-Tenant Isolated Vector Search
**Problem Statement**:  
Write a service method `searchTenantDocuments(VectorStore vectorStore, String query, String tenantId, int topK)` that uses Spring AI's `SearchRequest` and `FilterExpressionBuilder` to guarantee that documents belonging to other tenants are NEVER returned.

<details>
<summary>👉 View Solution</summary>

```java
package com.genai.springai.vectorstore;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import java.util.List;

public class TenantSearchService {

    public List<Document> searchTenantDocuments(VectorStore vectorStore, String query, String tenantId, int topK) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();

        SearchRequest request = SearchRequest.builder()
            .query(query)
            .topK(topK)
            .filterExpression(b.eq("tenantId", tenantId).build())
            .build();

        return vectorStore.similaritySearch(request);
    }
}
```
</details>

#### Exercise 2: Batch Document Deletion by Metadata Tag
**Problem Statement**:  
When a contract expires, all associated vector chunks must be purged from `VectorStore`. Write a method that queries all document IDs where `metadata.contractId == 'CTR-2026-X'` and deletes them from `VectorStore`.

<details>
<summary>👉 View Solution</summary>

```java
package com.genai.springai.vectorstore;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

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

[← Previous: Day 37 - Embedding Models](../Day_37_Embedding_Models_Text_to_Vectors/Day_37_Embedding_Models_Text_to_Vectors.md) | [Next: Day 39 - RAG Pipeline →](../Day_39_RAG_Retrieval_Augmented_Generation/Day_39_RAG_Retrieval_Augmented_Generation.md)
