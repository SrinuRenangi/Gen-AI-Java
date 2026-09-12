# Day 37: Embedding Models — Converting Text to Vectors

[← Previous: Day 36 - Streaming Responses](../Day_36_Streaming_Responses/Day_36_Streaming_Responses.md) | [Next: Day 38 - Vector Stores →](../Day_38_Vector_Stores_Semantic_Memory/Day_38_Vector_Stores_Semantic_Memory.md)

---

## 1. Topic Overview
Text embeddings transform human language into dense numerical vectors that capture semantic meaning, enabling computers to compute conceptual similarity rather than relying on brittle keyword matches. In enterprise GenAI architectures, embedding models form the mathematical backbone of Retrieval-Augmented Generation (RAG), semantic document search, recommendation engines, and automated clustering pipelines.

---

## 2. Basic Foundations (True Zero)

### What is a Vector Embedding?
Computers do not understand the conceptual meaning of words, emotions, or prose; they only understand numbers. An **embedding model** is a specialized neural network trained to translate any piece of text (a word, sentence, paragraph, or document) into an array of decimal numbers—called a **vector** (in Java, a `float[]`).

Crucially, this vector is not random. It places the text inside a high-dimensional mathematical space where words and sentences with similar meanings end up close together, while unrelated concepts are placed far apart.

```
"King"   -> [ 0.45,  0.82, -0.12,  0.91, ... ]
"Queen"  -> [ 0.43,  0.81, -0.10,  0.88, ... ]  <-- Very close to King
"Banana" -> [-0.62,  0.11,  0.75, -0.34, ... ]  <-- Far away from King & Queen
```

### Relatable Physical Analogy: The Global GPS for Concepts
Imagine a massive 3D library where every book is assigned a coordinate $(X, Y, Z)$ based on its genre:
- $X$ axis represents "Cookbook vs. Tech Manual"
- $Y$ axis represents "Fiction vs. Non-Fiction"
- $Z$ axis represents "Children's vs. Academic"

A book on "Java Multithreading" sits at coordinate $(0.95, 0.90, 0.85)$. A book on "C++ Concurrency" sits right next to it at $(0.93, 0.89, 0.82)$. Meanwhile, "French Baking Techniques" sits way over at $(-0.80, 0.10, -0.40)$.

An embedding model does this exact spatial placement, except instead of 3 coordinates $(X, Y, Z)$, it assigns **768, 1536, or 3072 coordinates** (dimensions) to capture nuanced shades of meaning, tone, domain terminology, and context.

### Minimal Beginner-Friendly Working Code
Here is the absolute simplest way to convert text into an embedding vector in Spring AI:

```java
package com.genai.springai.embeddings;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class SimpleEmbeddingRunner implements CommandLineRunner {

    private final EmbeddingModel embeddingModel;

    // Spring AI automatically injects the configured EmbeddingModel (e.g., Ollama, OpenAI)
    public SimpleEmbeddingRunner(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public void run(String... args) {
        String text = "Spring AI makes building generative AI apps in Java seamless.";

        // Convert the text into a dense vector of floats
        float[] vector = embeddingModel.embed(text);

        System.out.println("Input Text: " + text);
        System.out.println("Vector Dimensions: " + vector.length);
        System.out.println("Sample Coordinates (first 5): " + 
            Arrays.toString(Arrays.copyOfRange(vector, 0, Math.min(5, vector.length))));
    }
}
```

### Line-by-Line Walkthrough
1. **`private final EmbeddingModel embeddingModel;`**: Declares Spring AI's universal interface for embedding generators. Whether backed by OpenAI, Ollama (`nomic-embed-text`), Azure, or Bedrock, client code remains identical.
2. **`public SimpleEmbeddingRunner(EmbeddingModel embeddingModel)`**: Constructor dependency injection supplies the auto-configured model bean.
3. **`float[] vector = embeddingModel.embed(text);`**: Sends the text payload to the embedding model API/runtime, which performs tokenization, feeds tokens through transformer layers, and returns an array of floating-point values representing the semantic coordinate.
4. **`vector.length`**: Prints the dimensionality of the vector space (typically 768 for Ollama `nomic-embed-text` or 1536 for OpenAI `text-embedding-3-small`).
5. **`Arrays.copyOfRange(vector, 0, 5)`**: Displays the first five floating-point coordinates in that high-dimensional space.

---

## 3. Core Concept Walkthrough (Basic → Intermediate)

```
+-------------------------------------------------------------------------------+
|                       HOW TEXT EMBEDDING WORKS                                |
+-------------------------------------------------------------------------------+
|                                                                               |
|  Raw Text:                                                                    |
|  "Java Virtual Threads scale concurrent I/O."                                 |
|         |                                                                     |
|         v                                                                     |
|  [ Tokenization & Embedding Neural Network ] (e.g. nomic-embed-text)          |
|         |                                                                     |
|         v                                                                     |
|  Dense Float Array (Vector Coordinates in 768 Dimensions):                    |
|  [ 0.0412, -0.0189, 0.0821, 0.0504, -0.0931, ..., 0.0117 ]                   |
|         |                                                                     |
|         +-------------------------+-------------------------+                 |
|                                   |                         |                 |
|                                   v                         v                 |
|                            Cosine Similarity         Vector Database          |
|                          (Comparing Concepts)     (pgvector, Milvus, Qdrant)  |
+-------------------------------------------------------------------------------+
```

### Vector Dimensions Explained
What does "768 dimensions" or "1536 dimensions" mean?
- A **dimension** is simply an index in the array (`vector[0]`, `vector[1]`, ..., `vector[767]`).
- Individual dimensions do not represent simple labels like "is_animal" or "is_verb". Instead, during deep learning training, dimensions capture latent statistical patterns of human language, associations, and relationships.
- Common model dimensionalities:
  - `nomic-embed-text` (Ollama, local, open-source): **768 dimensions**
  - OpenAI `text-embedding-3-small`: **1536 dimensions**
  - OpenAI `text-embedding-3-large`: **3072 dimensions**
  - Cohere `embed-english-v3.0`: **1024 dimensions**

### Vector Geometry & Cosine Similarity Math
When we have two vectors $\vec{A}$ and $\vec{B}$, how do we determine if their meanings are close?
We calculate the **angle $\theta$** between them using **Cosine Similarity**:

$$\text{Cosine Similarity} = \cos(\theta) = \frac{\vec{A} \cdot \vec{B}}{\|\vec{A}\| \|\vec{B}\|} = \frac{\sum_{i=1}^n A_i B_i}{\sqrt{\sum_{i=1}^n A_i^2} \sqrt{\sum_{i=1}^n B_i^2}}$$

```
                       ^ Dimension 2
                       |
                       |         / Vector A: "Java concurrency"
                       |       / 
                       |     / ) theta (very small angle = high similarity: ~0.85)
                       |   /  
                       | /-------> Vector B: "JVM multithreading"
                       |
                       |
                       |--------------------------------------> Dimension 1
                      /
                     / Vector C: "Chocolate chip cookie recipe" (large angle: ~0.05)
                    v
```

- **Score near 1.0**: The concepts are nearly identical (e.g., "Doctor" vs. "Physician").
- **Score near 0.0**: The concepts are completely unrelated (e.g., "Kubernetes Pod" vs. "Strawberry Cheesecake").
- **Score near -1.0**: The concepts are exact semantic opposites (rare in normalized text embeddings; usually scores range from 0.0 to 1.0).

### Pure Java 21 High-Performance Vector Math Implementation

```java
package com.genai.springai.embeddings;

public final class VectorMath {

    private VectorMath() {}

    /**
     * Computes the dot product of two vectors: sum(A[i] * B[i]).
     */
    public static double dotProduct(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Vector length mismatch: " + a.length + " vs " + b.length);
        }
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    /**
     * Computes the Euclidean L2 norm (magnitude) of a vector: sqrt(sum(A[i]^2)).
     */
    public static double magnitude(float[] a) {
        double sum = 0.0;
        for (float val : a) {
            sum += val * val;
        }
        return Math.sqrt(sum);
    }

    /**
     * Computes the Cosine Similarity between two vectors.
     * Returns a score between -1.0 and 1.0 (typically 0.0 to 1.0 for embeddings).
     */
    public static double cosineSimilarity(float[] a, float[] b) {
        double magA = magnitude(a);
        double magB = magnitude(b);
        if (magA == 0.0 || magB == 0.0) {
            return 0.0;
        }
        return dotProduct(a, b) / (magA * magB);
    }

    /**
     * Normalizes a vector to unit length (L2 norm = 1.0).
     * When vectors are pre-normalized, cosine similarity equals the dot product.
     */
    public static float[] normalize(float[] a) {
        double mag = magnitude(a);
        if (mag == 0.0) return a;
        float[] normalized = new float[a.length];
        for (int i = 0; i < a.length; i++) {
            normalized[i] = (float) (a[i] / mag);
        }
        return normalized;
    }
}
```

### Building an In-Memory Semantic Search Engine
With `VectorMath` and `EmbeddingModel`, we can build a working semantic search engine without needing external databases:

```java
package com.genai.springai.embeddings;

import org.springframework.ai.embedding.EmbeddingModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SemanticSearchEngine {

    public record Document(String id, String content, float[] embedding) {}
    public record SearchResult(Document document, double similarityScore) {}

    private final EmbeddingModel embeddingModel;
    private final List<Document> corpus = new ArrayList<>();

    public SemanticSearchEngine(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public void indexDocument(String id, String content) {
        float[] embedding = embeddingModel.embed(content);
        corpus.add(new Document(id, content, embedding));
    }

    public List<SearchResult> search(String query, int topK) {
        float[] queryEmbedding = embeddingModel.embed(query);

        return corpus.stream()
                .map(doc -> new SearchResult(doc, VectorMath.cosineSimilarity(queryEmbedding, doc.embedding())))
                .sorted(Comparator.comparingDouble(SearchResult::similarityScore).reversed())
                .limit(topK)
                .toList();
    }
}
```

### Step-by-Step Explanation of the Search Engine
1. **`record Document(...)`**: Immutable carrier containing an ID, the raw text content, and its precomputed `float[] embedding`.
2. **`indexDocument(id, content)`**: Passes document content to `embeddingModel.embed(content)`. The resulting vector is stored in memory. In real enterprise production, this precomputation happens once at ingestion time.
3. **`search(query, topK)`**:
   - Converts the incoming user query text into a vector coordinate via `embeddingModel.embed(query)`.
   - Compares the query vector against every indexed document vector using `VectorMath.cosineSimilarity()`.
   - Sorts the results in descending order by `similarityScore`.
   - Returns the top $K$ most conceptually relevant documents.

---

## 4. Prerequisite & Supporting Concepts

### Prerequisite / Supporting Concept: Vector Normalization & The Dot Product Shortcut
Notice the denominator in the cosine similarity formula: $\|\vec{A}\| \|\vec{B}\|$.
Computing square roots across 1,536 dimensions for thousands of documents is computationally heavy.

> [!TIP]
> **The Dot Product Shortcut**:
> Most modern embedding models (like OpenAI's `text-embedding-3-*` and Ollama's `nomic-embed-text`) output vectors that are **already normalized to unit length** ($\|\vec{A}\| = 1.0$).
> 
> When $\|\vec{A}\| = 1.0$ and $\|\vec{B}\| = 1.0$, the denominator becomes $1.0 \times 1.0 = 1.0$.
> Therefore:
> $$\text{Cosine Similarity} = \vec{A} \cdot \vec{B} = \sum_{i=1}^n A_i B_i$$
> Computing a simple dot product requires only multiplication and addition—no expensive square roots or divisions!

### Prerequisite / Supporting Concept: Configuring Embedding Models in `application.yml`
Spring AI provides first-class starters for local Ollama and cloud OpenAI embedding models.

#### Local Development with Ollama (`nomic-embed-text`)
```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      embedding:
        options:
          model: nomic-embed-text
```

#### Production with OpenAI
```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      embedding:
        options:
          model: text-embedding-3-small
          dimensions: 1536
```

---

## 5. Advanced Depth (Intermediate → Advanced)

### The 100x Performance Trap: Sequential Loop vs. Batch Embedding
Never embed documents sequentially inside a standard for-loop.

#### Anti-Pattern: Sequential Loop (Disastrous Latency)
```java
// BAD: 5,000 documents = 5,000 HTTP roundtrips!
// At 30ms latency, this takes 150 seconds (2.5 minutes)!
List<float[]> embeddings = new ArrayList<>();
for (String chunk : documentChunks) {
    embeddings.add(embeddingModel.embed(chunk));
}
```

#### Production Pattern: Batching
```java
// GOOD: Send chunks in batches of 100
// 50 network calls instead of 5,000. Completes in under 2 seconds!
List<List<String>> batches = partitionList(documentChunks, 100);
List<float[]> allEmbeddings = new ArrayList<>();

for (List<String> batch : batches) {
    // EmbeddingResponse containing all vectors for the batch in one request
    List<float[]> batchVectors = embeddingModel.embed(batch);
    allEmbeddings.addAll(batchVectors);
}
```

### Document Chunking with Overlap
Embedding models have strict context window limits (typically 512 to 8,192 tokens). Passing a 100-page document to an embedding model causes:
1. Truncation, dropping 95% of your document.
2. Loss of precision: averaging 100 pages into a single 768-float vector turns specific facts into generic "semantic soup."

#### Chunking with Overlap Strategy
We divide large text into chunks (e.g., 500 characters) with an overlap (e.g., 50 characters) so boundary context is preserved:

```
 Document Text:
 "Java 21 introduces Virtual Threads. Virtual Threads are lightweight threads managed by the JVM."
 
 Chunk 1 (Characters 0..50):
 "Java 21 introduces Virtual Threads. Virtual Threads"
 
 Chunk 2 (Characters 35..90 — 15 character overlap):
 "Virtual Threads are lightweight threads managed by the JVM."
```

```java
package com.genai.springai.embeddings;

import java.util.ArrayList;
import java.util.List;

public final class TextChunker {

    private TextChunker() {}

    public static List<String> chunk(String text, int chunkSize, int overlap) {
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

### Common Architectural Mistakes & Misconceptions

| Mistake | Why It Breaks in Production | Correct Architectural Approach |
|:---|:---|:---|
| **Mixing Embedding Models** | Indexing documents with `nomic-embed-text` (768 dims) and querying with OpenAI `text-embedding-3-small` (1536 dims) yields complete gibberish or runtime dimension mismatch errors. | **Strict Model Symmetry**: Always use the exact same embedding model for indexing documents and querying vectors. |
| **Storing Embeddings as Strings** | Saving vectors as comma-separated strings in SQL causes severe memory and serialization overhead. | Use native vector column types (e.g., `vector(768)` in PostgreSQL `pgvector`). |
| **Embedding Stopwords or Noise** | Embedding massive JSON payloads with repeating keys dilutes conceptual signal. | Clean text or format markdown before generating embeddings. |

---

## 6. Quick Recap
- An **embedding** translates unstructured text into a dense array of floating-point numbers (`float[]`) representing its conceptual meaning.
- **Dimensionality** represents the coordinate axis count in latent semantic space (e.g., 768 for `nomic-embed-text`, 1536 for OpenAI `text-embedding-3-small`).
- **Cosine Similarity** measures the angle between two vectors, returning a score from 0.0 (unrelated) to 1.0 (identical meaning).
- When vectors are pre-normalized to unit length, cosine similarity simplifies to a lightning-fast **dot product**.
- Always use **batching** when embedding multiple documents to avoid $O(N)$ HTTP roundtrip overhead.
- Large documents must be **chunked with overlap** before embedding to prevent truncation and semantic dilution.

---

## 7. Self-Check Questions & Practice Exercises

### 5-Question Self-Check Quiz

#### Question 1
What is an embedding vector in Generative AI?
- A) A primary key stored in an SQL database.
- B) A dense array of floating-point numbers representing the semantic conceptual meaning of text in multi-dimensional space.
- C) A compressed JPEG image representation.
- D) A Java 21 bytecode instruction.

#### Question 2
Why is Cosine Similarity preferred over Euclidean Distance for measuring document similarity?
- A) Cosine similarity only works with integer vectors.
- B) Cosine similarity measures the angle between vectors and is invariant to document length, whereas Euclidean distance is distorted by word count differences.
- C) Cosine similarity requires dedicated GPU hardware.
- D) Euclidean distance cannot exceed 1.0.

#### Question 3
What mathematical optimization occurs when embedding vectors are pre-normalized to unit length ($\|\vec{A}\| = 1.0$)?
- A) Cosine similarity simplifies to a pure Dot Product ($\vec{A} \cdot \vec{B}$), eliminating costly square root and division calculations.
- B) The vector dimensions double.
- C) Negative float values become positive.
- D) Text can be reverse-engineered back to the original string.

#### Question 4
How many dimensions does the popular local Ollama model `nomic-embed-text` generate?
- A) 128
- B) 768
- C) 1536
- D) 4096

#### Question 5
Why is it dangerous to pass an entire 50-page PDF directly to an embedding model without chunking?
- A) The JVM will run out of stack space.
- B) Embedding models have context token limits (e.g., 512–8192 tokens); exceeding them causes truncation and averages all topics into an indistinct "semantic soup."
- C) Large documents corrupt PostgreSQL indexes permanently.
- D) Embedding models only accept single words.

---

### Quiz Answers & Explanations
1. **B**: Embedding models project the semantic relationships of human language into high-dimensional geometric space.
2. **B**: Cosine similarity measures orientation rather than magnitude, preventing longer articles from appearing falsely distant from short queries.
3. **A**: When magnitudes $\|\vec{A}\| = \|\vec{B}\| = 1.0$, the denominator is $1.0$, allowing similarity to be computed with a fast single-pass dot product.
4. **B**: `nomic-embed-text` produces standard 768-dimensional float arrays.
5. **B**: Chunking with overlap ensures every piece of information fits within the model's receptive field while preserving boundary context.

---

### Hands-On Practice Exercises

#### Exercise 1: Parallel Batch Embedding Pipeline with Virtual Threads
**Problem Statement**:  
Write a Spring service `BatchEmbeddingPipeline` that takes a list of text documents, partitions them into batches of `batchSize`, and uses Java 21's `Executors.newVirtualThreadPerTaskExecutor()` to embed all batches concurrently against `EmbeddingModel`.

<details>
<summary>👉 View Solution</summary>

```java
package com.genai.springai.embeddings;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class BatchEmbeddingPipeline {

    private final EmbeddingModel embeddingModel;

    public BatchEmbeddingPipeline(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public List<float[]> embedAllParallel(List<String> allDocuments, int batchSize) 
            throws InterruptedException, ExecutionException {
        
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < allDocuments.size(); i += batchSize) {
            batches.add(allDocuments.subList(i, Math.min(i + batchSize, allDocuments.size())));
        }

        List<Future<List<float[]>>> futures = new ArrayList<>();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (List<String> batch : batches) {
                futures.add(executor.submit(() -> embeddingModel.embed(batch)));
            }
        } // Executor auto-closes, awaiting completion of all virtual threads

        List<float[]> consolidated = new ArrayList<>();
        for (Future<List<float[]>> future : futures) {
            consolidated.addAll(future.get());
        }

        return consolidated;
    }
}
```
</details>

#### Exercise 2: Semantic Duplicate Article Detector
**Problem Statement**:  
Build a component `DuplicateDetector` that accepts a candidate article text, computes its embedding, compares it against an existing corpus of indexed documents, and flags it as a duplicate if the cosine similarity exceeds `0.92`.

<details>
<summary>👉 View Solution</summary>

```java
package com.genai.springai.embeddings;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DuplicateDetector {

    private final EmbeddingModel embeddingModel;
    private static final double DUPLICATE_THRESHOLD = 0.92;

    public DuplicateDetector(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public record DuplicateCheckResult(boolean isDuplicate, double highestSimilarity, String matchedId) {}

    public DuplicateCheckResult checkForDuplicate(String candidateText, List<SemanticSearchEngine.Document> existingCorpus) {
        float[] candidateVector = embeddingModel.embed(candidateText);
        double maxSim = 0.0;
        String matchedDocId = null;

        for (SemanticSearchEngine.Document doc : existingCorpus) {
            double similarity = VectorMath.cosineSimilarity(candidateVector, doc.embedding());
            if (similarity > maxSim) {
                maxSim = similarity;
                matchedDocId = doc.id();
            }
        }

        boolean isDuplicate = maxSim >= DUPLICATE_THRESHOLD;
        return new DuplicateCheckResult(isDuplicate, maxSim, matchedDocId);
    }
}
```
</details>

---

[← Previous: Day 36 - Streaming Responses](../Day_36_Streaming_Responses/Day_36_Streaming_Responses.md) | [Next: Day 38 - Vector Stores →](../Day_38_Vector_Stores_Semantic_Memory/Day_38_Vector_Stores_Semantic_Memory.md)
