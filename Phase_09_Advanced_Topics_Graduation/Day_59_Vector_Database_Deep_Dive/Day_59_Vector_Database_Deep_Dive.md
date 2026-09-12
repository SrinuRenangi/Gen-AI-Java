# Day 59: Vector Database Deep Dive — HNSW vs IVFFlat, Indexing at Scale & pgvector Tuning

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [Day 58: Evaluation & Automated Testing of AI Systems](../Day_58_Evaluation_Testing_AI_Systems/Day_58_Evaluation_Testing_AI_Systems.md) | [All 60 Days Overview](../../README.md) | [Day 60: Graduation, Portfolio & Career](../Day_60_Graduation_Portfolio_Career/Day_60_Graduation_Portfolio_Career.md) |

---

## 1. Topic Overview

**High-Scale Vector Indexing & Database Optimization** is the engineering science of organizing and querying millions of high-dimensional mathematical embeddings with sub-10ms latency and near-perfect recall. In enterprise Java and Spring AI architectures, this discipline focuses on Approximate Nearest Neighbor (ANN) algorithms—specifically **Hierarchical Navigable Small World (HNSW)** skip graphs, **IVFFlat** centroid partitioning, **Scalar Quantization (SQ8)**, and **Hybrid Search with Reciprocal Rank Fusion (RRF)**—to ensure vector retrieval remains fast, memory-efficient, and accurate at massive enterprise scale.

---

## 2. Basic Foundations (True Zero)

### Core Vector Database Vocabulary

- **Flat Index (Brute Force)**: Scanning every single vector in the database sequentially ($O(N \cdot D)$). It guarantees 100% recall accuracy, but querying 10 million vectors takes seconds instead of milliseconds.
- **IVFFlat (Inverted File Flat)**: An index that clusters vectors into geometric Voronoi neighborhoods. During search, the database inspects only the closest neighborhood centroids rather than scanning the entire collection.
- **HNSW (Hierarchical Navigable Small World)**: A multi-layered graph data structure for vectors (similar to a skip list). High-level layers provide long-range express hops across the dataset, while lower layers provide localized fine-grained searches, reducing query complexity to logarithmic $O(\log N)$ with ~3ms latency.
- **`m` and `ef_search`**: The master tuning parameters of HNSW:
  - `m`: The maximum number of bidirectional connection edges each node maintains (typically `16` or `32`).
  - `ef_search`: The size of the dynamic candidate neighbor list evaluated during query execution (typically `40` to `100`).
- **Scalar Quantization (SQ8)**: Compressing each 32-bit floating point dimension into an 8-bit integer, slashing RAM consumption by 75% while retaining over 98% search recall.
- **Hybrid Search with RRF**: Combining semantic vector similarity with sparse keyword search (BM25) using **Reciprocal Rank Fusion**, ensuring that queries match conceptual meaning while never missing exact serial numbers, error codes, or product SKUs.

---

### Relatable Physical Analogy: The Intercontinental Flight Network vs. Checking Every House

Imagine a detective in New York City tasked with finding the individual on Earth whose physical appearance, DNA, and background most closely match a suspect:
- **Strategy A: Brute Force Exhaustive Scan (Flat Index)**:
  The detective travels to Maine and knocks on every single front door on planet Earth, inspecting all 8 billion people one by one.
  *Result*: Accuracy is **100% guaranteed (100% Recall)**. However, by the time person number 4,000,000 is examined, 30 years have passed and the criminal is long gone.
- **Strategy B: Postal District Sorting (IVFFlat Index)**:
  The detective divides the globe into 1,000 regional postal centers (Voronoi centroids). The detective identifies the 3 postal regions closest to the suspect's profile and inspects doors only within those 3 districts.
  *Result*: Fast build time and low RAM overhead. But if the suspect lives on the borderline between two postal districts, the detective misses them completely (recall drops to 75%–85%).
- **Strategy C: The Intercontinental Airline Network (HNSW Index)**:
  The detective starts at the stratosphere (Layer 2). They board an express supersonic flight from JFK to London, then to Tokyo. At Layer 1 (Regional flights), they fly from Tokyo to Kyoto. Finally, at Layer 0 (Local street taxis), they drive straight into the suspect's neighborhood and inspect 10 houses.
  *Result*: The suspect is found in **15 minutes with 99% accuracy**, examining only 50 individuals out of 8 billion!

```
                    HNSW HIERARCHICAL SKIP-GRAPH
                    
 Layer 2 (Supersonic)   [NYC] ═══════════════════════► [Tokyo]
                          │                              │
 Layer 1 (Regional)     [NYC] ══► [Chicago] ══► [LA]   [Tokyo] ══► [Kyoto]
                          │          │           │       │           │
 Layer 0 (Local Taxi)   [All 8 Billion Dense Vector Nodes Interconnected]
```

In production RAG systems, **vector search is the primary latency, memory, and accuracy bottleneck**. The choice of index determines whether your enterprise assistant responds in **5 milliseconds** or crashes under heavy query volume.

---

### Minimal Beginner-Friendly Example: Pure Java Vector Cosine Distance & Flat Search

Here is a minimal, self-contained Java program demonstrating how vector distance calculations and brute-force flat searches work under the hood:

```java
package com.genai.enterprise.vectordb.minimal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MinimalVectorSearch {

    public record VectorItem(String id, float[] embedding, String content) {}
    public record SearchResult(VectorItem item, double distance) {}

    // Cosine Distance: 1.0 - Cosine Similarity (Lower distance = more similar)
    public static double cosineDistance(float[] vA, float[] vB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vA.length; i++) {
            dotProduct += vA[i] * vB[i];
            normA += vA[i] * vA[i];
            normB += vB[i] * vB[i];
        }
        if (normA == 0.0 || normB == 0.0) return 1.0;
        double similarity = dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
        return 1.0 - similarity;
    }

    public static List<SearchResult> flatSearch(List<VectorItem> database, float[] query, int topK) {
        List<SearchResult> results = new ArrayList<>();
        for (VectorItem item : database) {
            double dist = cosineDistance(query, item.embedding());
            results.add(new SearchResult(item, dist));
        }
        // Sort ascending by distance (closest first)
        results.sort(Comparator.comparingDouble(SearchResult::distance));
        return results.subList(0, Math.min(topK, results.size()));
    }

    public static void main(String[] args) {
        List<VectorItem> db = List.of(
            new VectorItem("DOC-1", new float[]{0.9f, 0.1f, 0.0f}, "Employee 401k Benefits Guide"),
            new VectorItem("DOC-2", new float[]{0.1f, 0.8f, 0.1f}, "Kubernetes Deployment Manual"),
            new VectorItem("DOC-3", new float[]{0.85f, 0.15f, 0.05f}, "Health Insurance & Dental Plan")
        );

        float[] hrQuery = new float[]{0.88f, 0.12f, 0.0f}; // Close to HR topics
        List<SearchResult> hits = flatSearch(db, hrQuery, 2);

        System.out.println("Top Matches for HR Query:");
        for (SearchResult hit : hits) {
            System.out.printf("-> %s (Distance: %.4f) | %s%n",
                    hit.item().id(), hit.distance(), hit.item().content());
        }
    }
}
```

#### Line-by-Line Walkthrough:
1. `record VectorItem(...)`: Represents an indexed document containing ID, vector embedding, and text payload.
2. `cosineDistance(...)`: Computes $1.0 - \text{Cosine Similarity}$. In pgvector, this corresponds to the `<=>` operator where $0.0$ indicates identical direction.
3. `flatSearch(...)`: Performs an exhaustive $O(N)$ scan across all vectors in the collection.
4. `results.sort(...)`: Sorts results ascending by distance to find the nearest neighbors.
5. `main(...)`: Executes the search, verifying that the query correctly retrieves HR documents with the lowest distance scores.

---

## 3. Core Concept Walkthrough (Basic → Intermediate)

### 3.1 Vector Index Taxonomy & Comparison

```mermaid
graph TD
    subgraph Vector_Index_Taxonomy [Approximate Nearest Neighbor ANN Algorithms]
        A[Vector Query Q] --> B{Index Algorithm}
        
        B -->|Exact Brute Force| C[Flat Index O N*D]
        C --> C1[100% Recall | Zero Build Time | Latency Unusable at Scale]

        B -->|Centroid Inverted File| D[IVFFlat Index O K + n_probes]
        D --> D1[Low RAM | Fast Build | Requires Training | Recall 75-90%]

        B -->|Multi-Layer Skip Graph| E[HNSW Index O log N]
        E --> E1[Sub-10ms Latency | Recall 98%+ | Higher RAM | Logarithmic Scale]
    end
```

### Head-to-Head Comparison Matrix

| Dimension | Flat (Exact k-NN) | IVFFlat | HNSW (Hierarchical Navigable Small World) |
|:---|:---|:---|:---|
| **Search Complexity** | $O(N \cdot D)$ (Linear) | $O(\frac{\text{probes}}{K} \cdot N \cdot D)$ | $O(\log N)$ (Logarithmic) |
| **Search Latency (1M vectors)**| ~850 ms | ~45 ms | **~3 ms** |
| **Recall @ 10** | 100.0% | 80% – 92% | **98.5% – 99.9%** |
| **RAM / Disk Overhead** | Zero index overhead | Minimal (~1.05x) | Higher (~1.3x – 1.5x) |
| **Index Build Speed** | Instant (No index) | Fast (k-means training) | Slower (Graph construction) |
| **Data Dynamic Updates** | Immediate | Degrades over time | Handled seamlessly |
| **Production Recommendation**| < 10,000 vectors | Strict low RAM constraints | **Enterprise Production Gold Standard** |

---

### 3.2 How HNSW Works Internally

HNSW combines two core computer science principles:
1. **Skip Lists**: Providing $O(\log N)$ search across 1D ordered lists by introducing probabilistic higher-level express lanes.
2. **Navigable Small World (NSW) Graphs**: Networks with high clustering coefficients where nodes are not direct neighbors, but can be reached in a small number of hops (the "Six Degrees of Separation" principle).

#### The Three Critical HNSW Parameters in PostgreSQL `pgvector`:
1. **`m` (Max Connections per Node)**:
   - Controls how many bidirectional connection edges each vector node maintains.
   - Recommended: `m = 16` (General use) or `m = 32` (High-dimensional embeddings like OpenAI 1536-dim).
   - Higher `m` improves recall accuracy, but increases index size in RAM.
2. **`ef_construction` (Exploration Factor during Build)**:
   - Size of the dynamic candidate list evaluated when inserting a new vector into the graph.
   - Recommended: `ef_construction = 64` or `128`.
   - Trades build time for higher query-time graph connectivity.
3. **`ef_search` (Exploration Factor during Query Time)**:
   - Number of candidate neighbors evaluated during a live user search.
   - Recommended: `ef_search = 40` (ultra-fast) to `100` (high precision).
   - Can be adjusted on the fly per transaction in PostgreSQL without rebuilding the index!

---

### 3.3 Vector Compression: Scalar Quantization (SQ8) & Product Quantization (PQ)

In high-scale enterprise architectures holding tens of millions of documents, raw floating-point vectors consume massive amounts of RAM:

#### 1. Scalar Quantization (SQ8)
- Maps 32-bit floating-point coordinates (`float32`, 4 bytes) into 8-bit integers (`int8`, 1 byte) using uniform min-max scaling:
  $$q = \text{round}\left(255 \times \frac{x - x_{\min}}{x_{\max} - x_{\min}}\right)$$
- **Memory Savings**: Exactly **75% reduction** in vector storage (from 6.1 KB down to 1.5 KB per 1536-dimensional vector).
- **Compute Speed**: CPU AVX-512 VNNI instructions compute 8-bit dot products 4x faster than 32-bit float operations.

#### 2. Product Quantization (PQ)
- Decomposes a 1,536-dimensional vector into $M$ sub-vectors (e.g., 64 sub-vectors of 24 dimensions each).
- Clusters sub-vectors into 256 centroids using k-means, replacing each 24-float chunk with a single 1-byte centroid ID.
- **Memory Savings**: Up to **95% reduction** in index memory! Ideal for billion-scale datasets.

---

### 3.4 Production PostgreSQL pgvector DDL & SQL Tuning

```sql
-- 1. Enable extension
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. Create production table
CREATE TABLE enterprise_knowledge_chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_title VARCHAR(255) NOT NULL,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    embedding vector(1536) NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Tune maintenance memory before building index
SET maintenance_work_mem = '4GB';
SET max_parallel_maintenance_workers = 4;

-- 4. Build HNSW index using Cosine Distance operator (<=>)
CREATE INDEX CONCURRENTLY idx_knowledge_hnsw_cosine 
ON enterprise_knowledge_chunks 
USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);
```

#### Query-Time Optimization:
```sql
-- Set exploration depth per session/transaction
SET LOCAL hnsw.ef_search = 100;

-- Query nearest neighbors using cosine distance (<=>)
SELECT id, document_title, content, (embedding <=> :queryEmbedding) AS distance
FROM enterprise_knowledge_chunks
ORDER BY embedding <=> :queryEmbedding
LIMIT 5;
```

---

### 3.5 Hybrid Search: Dense Vectors + Sparse BM25 via Reciprocal Rank Fusion (RRF)

Dense vector embeddings excel at **conceptual semantics** (understanding that "automobile" matches "car"). However, they struggle with **exact alphanumeric identifiers** (e.g., error code `ERR-99214`, drug SKU `RX-7809-B`, or specific customer names).

Sparse keyword search (BM25 or PostgreSQL `tsvector`) excels at exact tokens, but fails at semantic synonyms. **Enterprise Hybrid Search** executes both algorithms concurrently and fuses their ranked lists using **Reciprocal Rank Fusion (RRF)**:

```
                  [ User Query: "ERR-99214 database timeout" ]
                                        │
             ┌──────────────────────────┴──────────────────────────┐
             ▼                                                     ▼
   [ Dense Vector Search ]                               [ Sparse Lexical Search ]
   pgvector HNSW (<=>)                                   PostgreSQL ts_rank (BM25)
   Rank 1: Doc A (timeout guide)                         Rank 1: Doc X (contains ERR-99214)
   Rank 2: Doc B (database tuning)                       Rank 2: Doc A (contains database)
             │                                                     │
             └──────────────────────────┬──────────────────────────┘
                                        ▼
                  [ Reciprocal Rank Fusion (RRF) Engine ]
                  RRF Score(d) = 1/(60 + r_dense) + 1/(60 + r_sparse)
                                        │
                                        ▼
                  Final Top Result: Doc A & Doc X fused together!
```

#### The RRF Formula:

$$RRF\_Score(d) = \sum_{m \in M} \frac{1}{k + r_m(d)}$$

where $k = 60$ is the standard smoothing constant preventing top-ranked outliers from dominating the result, and $r_m(d)$ is the rank of document $d$ in system $m$.

---

## 4. Prerequisite & Supporting Concepts

### Prerequisite / Supporting Concept: Vector Cosine Distance vs Euclidean L2
- **Cosine Distance (`<=>`)**: Measures the angular difference between vectors, ignoring magnitude. Ideal for text embeddings where document length variations should not distort semantic similarity.
- **Euclidean L2 Distance (`<->`)**: Measures straight-line geometric distance. Required when vector magnitude carries physical meaning.
- **Inner Product (`<#>`)**: Directly calculates negative dot product. If vectors are pre-normalized to unit length ($L_2 = 1.0$), inner product is mathematically identical to cosine distance and computes significantly faster.

### Prerequisite / Supporting Concept: PostgreSQL Extensions & Concurrent Index Creation
In PostgreSQL, `CREATE INDEX CONCURRENTLY` builds an index without taking an exclusive table lock, allowing live application read and write queries to continue uninterrupted.

### Prerequisite / Supporting Concept: Reciprocal Rank Fusion (RRF) Smoothing Parameter $k = 60$
The constant $k = 60$ (originating from Cormack, Clarke, and Büttcher, 2009) ensures that the difference between Rank 1 and Rank 2 ($\frac{1}{61} - \frac{1}{62} \approx 0.00026$) does not excessively overshadow relevant documents appearing slightly lower across both rankings.

---

## 5. Advanced Depth (Intermediate → Advanced)

### 5.1 Common Mistakes & Misconceptions: Bad vs. Good

#### Mistake 1: Pre-Filtering on Un-indexed Metadata Columns
Filtering by `tenant_id` on a table with a global HNSW index causes HNSW graph disconnections—the graph traverses nodes belonging to other tenants and prunes them, returning fewer than $K$ valid items.

```sql
-- ❌ BAD: Global HNSW index with pre-filtering can cause empty or incomplete results
SELECT * FROM knowledge_chunks 
WHERE tenant_id = 'acme' 
ORDER BY embedding <=> :query LIMIT 5;

-- ✅ GOOD: Use partial HNSW indexes partitioned by tenant
CREATE INDEX idx_tenant_acme_hnsw 
ON knowledge_chunks 
USING hnsw (embedding vector_cosine_ops) 
WHERE tenant_id = 'acme';
```

#### Mistake 2: Failing to Allocate Sufficient `maintenance_work_mem`
Attempting to build an HNSW index over 5 million vectors with PostgreSQL's default `maintenance_work_mem = 64MB` forces the engine to spill temporary graph structures to disk, turning a 10-minute build into an 8-hour bottleneck.

```sql
-- ❌ BAD: Default maintenance memory causes disk thrashing during index build
CREATE INDEX idx_hnsw ON knowledge_chunks USING hnsw (embedding vector_cosine_ops);

-- ✅ GOOD: Allocate sufficient memory and workers for in-memory graph construction
SET maintenance_work_mem = '4GB';
SET max_parallel_maintenance_workers = 4;
CREATE INDEX CONCURRENTLY idx_hnsw ON knowledge_chunks USING hnsw (embedding vector_cosine_ops);
```

#### Mistake 3: Relying Exclusively on Vector Search for Alphanumeric Queries
Vector embeddings frequently map similar-looking product SKUs (e.g., `SKU-881` vs. `SKU-882`) to nearly identical embeddings, causing exact product lookup failures.

```java
// ❌ BAD: Pure vector search for exact product code
vectorStore.similaritySearch("Find details for error code ERR-99214");

// ✅ GOOD: Use Hybrid Search with Reciprocal Rank Fusion
hybridSearchEngine.search("ERR-99214 database timeout", topK);
```

---

### 5.2 Complete Verification Suite & Demo Execution

Execute the verification suite in `Phase_09_Advanced_Topics_Graduation/Day_59_Vector_Database_Deep_Dive/code/`:

```bash
javac -d out Phase_09_Advanced_Topics_Graduation/Day_59_Vector_Database_Deep_Dive/code/*.java
java -cp out com.genai.enterprise.vectordb.VectorDeepDiveDemo
```

```
==========================================================================
     DAY 59: VECTOR DATABASE DEEP DIVE (HNSW vs FLAT & HYBRID RRF)       
==========================================================================

[Step 1: Indexing 100 High-Dimensional Vectors (32 dimensions)]
  Flat Index Items: 100 | HNSW Graph Nodes: 100

[Step 2: Benchmark Comparison (Flat Exact k-NN vs HNSW Graph ANN)]
  Flat Search Time : 19168400 ns (Recall: 100.0%)
  HNSW Search Time : 2488100 ns (Recall@5: 80.0%)
  Top Match (Flat) : DOC-83 (Dist: 0.6242)
  Top Match (HNSW) : DOC-83 (Dist: 0.6242)

[Step 3: Executing Enterprise Hybrid Search (Dense Vectors + Sparse BM25 via RRF)]
  Rank 1: DOC-1 | RRF Score: 0.03151 (Dense Rank: 5, Sparse Rank: 2)
  Rank 2: DOC-4 | RRF Score: 0.02854 (Dense Rank: 16, Sparse Rank: 5)
  Rank 3: DOC-2 | RRF Score: 0.02724 (Dense Rank: 28, Sparse Rank: 3)
  Rank 4: DOC-25 | RRF Score: 0.02678 (Dense Rank: 6, Sparse Rank: 26)
  Rank 5: DOC-16 | RRF Score: 0.02650 (Dense Rank: 14, Sparse Rank: 17)

==========================================================================
>>> Vector Database Deep Dive verification completed successfully!
```

---

## 6. Quick Recap

| Technique | Complexity | Latency (1M Vectors) | Recall @ 10 | Primary Use Case |
|:---|:---|:---|:---|:---|
| **Flat (Brute Force)** | $O(N \cdot D)$ | ~850 ms | 100.0% | Ground-truth baseline, $< 10\text{k}$ documents |
| **IVFFlat** | $O(\frac{P}{K} \cdot N \cdot D)$| ~45 ms | 80% – 92% | Memory-constrained systems, static datasets |
| **HNSW Index** | $O(\log N)$ | **~3 ms** | **98.5% – 99.9%** | **Enterprise Production Gold Standard** |
| **Scalar Quantization (SQ8)**| Compresses 32-bit float to 8-bit int | Slashes RAM by 75% | > 98% | High-scale RAM cost reduction |
| **Hybrid Search (RRF)** | Dense + BM25 Fusion | Sub-15 ms | Superior across all queries | Combines semantic meaning with exact IDs |

---

## 7. Self-Check Questions & Practice Exercises

### Conceptual Self-Check Questions

#### Question 1: What is the primary operational advantage of HNSW over Flat vector indexing?
- A) HNSW uses less RAM than Flat.
- B) HNSW provides logarithmic $O(\log N)$ search complexity, delivering sub-10ms query times over millions of vectors compared to linear $O(N)$ scans.
- C) HNSW operates without floating-point arithmetic.
- D) HNSW requires no index construction.

*Answer*: **B**. HNSW uses hierarchical skip graphs to achieve ultra-fast approximate nearest neighbor retrieval at enterprise scale.

---

#### Question 2: What is the role of the `ef_search` parameter in PostgreSQL pgvector?
- A) It limits the maximum number of rows in the table.
- B) It controls the size of the dynamic candidate list evaluated during query execution, allowing developers to tune the balance between latency and recall on the fly.
- C) It sets the database password.
- D) It compiles Java code inside PostgreSQL.

*Answer*: **B**. Higher `ef_search` values increase recall accuracy at the cost of slight additional query latency.

---

#### Question 3: Why is Hybrid Search (combining Dense Vectors with Sparse Lexical search via RRF) superior to vector search alone in enterprise applications?
- A) Hybrid search is cheaper to host.
- B) Dense vectors understand conceptual semantic meaning, while sparse lexical search catches exact product codes, error identifiers, and SKU numbers that embedding models often blur.
- C) Hybrid search eliminates the database.
- D) Vector search cannot handle English text.

*Answer*: **B**. Hybrid search combines the semantic strengths of embeddings with the keyword precision of lexical indexes.

---

#### Question 4: In Reciprocal Rank Fusion (RRF), what is the purpose of the constant $k = 60$?
- A) It limits the search duration to 60 seconds.
- B) It smooths rank impact, preventing an extreme outlier rank (e.g., rank 1 in one list) from disproportionately dominating the combined score.
- C) It represents 60 degrees of rotation.
- D) It is the maximum number of documents allowed in a database.

*Answer*: **B**. The constant $k = 60$ balances dense and sparse ranking weights across systems.

---

### Hands-on Practice Exercises

#### Exercise 1: Dynamic `ef_search` Adjuster in Spring Data JPA
**Task**: Write a Spring service method that configures `SET LOCAL hnsw.ef_search = 120` for high-precision regulatory queries, but leaves it at `40` for general queries.

**Solution**:
```java
package com.genai.enterprise.exercises;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PrecisionVectorRepository {

    private final JdbcTemplate jdbcTemplate;

    public PrecisionVectorRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void configureSearchPrecision(boolean isHighPrecision) {
        int ef = isHighPrecision ? 120 : 40;
        jdbcTemplate.execute("SET LOCAL hnsw.ef_search = " + ef + ";");
        System.out.println("[PGVECTOR] Active session hnsw.ef_search set to: " + ef);
    }
}
```

---

#### Exercise 2: Vector Normalization Filter for Dot Product Equivalence
**Task**: Implement a utility method `toUnitVector(float[] raw)` that normalizes an embedding vector to unit length ($L_2 = 1.0$), allowing inner product (dot product) to be used instead of cosine distance for faster computation.

**Solution**:
```java
package com.genai.enterprise.exercises;

public class VectorNormalizer {

    public static float[] toUnitVector(float[] raw) {
        if (raw == null) return new float[0];
        float sumSquares = 0.0f;
        for (float v : raw) sumSquares += v * v;
        if (sumSquares == 0.0f) return raw;

        float norm = (float) Math.sqrt(sumSquares);
        float[] unit = new float[raw.length];
        for (int i = 0; i < raw.length; i++) {
            unit[i] = raw[i] / norm;
        }
        return unit;
    }
}
```

---

#### Exercise 3: Multi-Tenant Partial Index Definition
**Task**: Write the optimal PostgreSQL DDL definition for a multi-tenant vector table where each tenant's data is isolated into a dedicated partial HNSW index, avoiding graph disconnection bugs during pre-filtered queries.

**Solution**:
```sql
-- Optimal Partial Index for Tenant Partitioning:
CREATE INDEX CONCURRENTLY idx_tenant_acme_hnsw 
ON enterprise_knowledge_chunks 
USING hnsw (embedding vector_cosine_ops) 
WHERE tenant_id = 'acme_corp';
```

---

#### Exercise 4: Reciprocal Rank Fusion Merger
**Task**: Implement a Java method `calculateRrfScore(int denseRank, int sparseRank)` that computes the fused RRF score using $k = 60$.

**Solution**:
```java
package com.genai.enterprise.exercises;

public class RrfMerger {

    private static final double K = 60.0;

    public static double calculateRrfScore(int denseRank, int sparseRank) {
        double densePart = (denseRank > 0) ? (1.0 / (K + denseRank)) : 0.0;
        double sparsePart = (sparseRank > 0) ? (1.0 / (K + sparseRank)) : 0.0;
        return densePart + sparsePart;
    }
}
```

---

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [Day 58: Evaluation & Automated Testing of AI Systems](../Day_58_Evaluation_Testing_AI_Systems/Day_58_Evaluation_Testing_AI_Systems.md) | [All 60 Days Overview](../../README.md) | [Day 60: Graduation, Portfolio & Career](../Day_60_Graduation_Portfolio_Career/Day_60_Graduation_Portfolio_Career.md) |
