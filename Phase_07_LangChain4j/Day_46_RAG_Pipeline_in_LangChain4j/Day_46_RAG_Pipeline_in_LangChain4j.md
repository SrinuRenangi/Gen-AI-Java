# Day 46: RAG Pipeline in LangChain4j

[← Previous: Day 45 - Structured Extraction & Guardrails](../Day_45_Structured_Extraction_Guardrails/Day_45_Structured_Extraction_Guardrails.md) | [Next: Day 47 - Advanced RAG Chunking & Re-Ranking →](../Day_47_Advanced_RAG_Chunking_ReRanking/Day_47_Advanced_RAG_Chunking_ReRanking.md)

---

## 1. Topic Overview
Retrieval-Augmented Generation (RAG) in LangChain4j decouples the ingestion write-path (document parsing, chunking, embedding, vector storage) from the online retrieval read-path (query embedding, similarity scoring, context augmentation) using clean, composable Service Provider Interfaces (SPIs). This architecture allows enterprise Java systems to ground declarative `AiServices` agents in private company knowledge bases with deterministic citations and zero model hallucinations.

---

## 2. Basic Foundations (True Zero)

### What is RAG in LangChain4j?
When an LLM is asked about proprietary internal data (such as enterprise refund policies or Kubernetes cluster configurations), it cannot answer accurately because that information was never part of its public training set.

RAG turns a **closed-book exam** into an **open-book exam**:
1. When a user asks a question, your application searches your private vector store for the exact paragraphs that contain the answer.
2. It injects those retrieved paragraphs directly into the prompt as verified context.
3. The LLM reads the excerpt and synthesizes a 100% factual answer citing the source document.

### Relatable Physical Analogy: The Open-Book University Exam
Imagine taking a high-stakes corporate tax examination:
- **Closed-Book (Vanilla LLM)**: You are locked in a room with no notes, no books, and no internet. When asked: *"What is the equipment depreciation limit under Section 179 for 2026?"*, you cannot remember the exact number and guess: *"Probably $1,000,000"*, causing severe compliance fines.
- **Open-Book with a Research Assistant (RAG Pipeline)**: You sit at a desk accompanied by a fast research assistant. When the question arrives, the assistant navigates directly to the tax code shelf (**Vector Store**), photocopies the relevant paragraph (**Semantic Retrieval**), and places it on your desk (**Context Augmentation**). You read: *"Section 179 limit for 2026 is $1,220,000"*, and answer with perfect accuracy and citations.

### Minimal Beginner-Friendly Working Code
Here is how to set up an in-memory RAG pipeline and connect it to a declarative `AiServices` agent:

```java
package com.genai.langchain4j.rag;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

public class SimpleRagRunner {

    public interface CorporateAssistant {
        @SystemMessage("You are an enterprise support assistant. Answer strictly using the provided context.")
        String ask(@UserMessage String question);
    }

    public static void main(String[] args) {
        // 1. Models & Vector Store
        ChatLanguageModel chatModel = OpenAiChatModel.builder()
            .apiKey(System.getenv("OPENAI_API_KEY"))
            .modelName("gpt-4o")
            .build();

        EmbeddingModel embeddingModel = OpenAiEmbeddingModel.withApiKey(System.getenv("OPENAI_API_KEY"));
        InMemoryEmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        // 2. Ingest verified corporate facts
        TextSegment policyChunk = TextSegment.from(
            "Enterprise refund policy permits customer subscription reversals strictly within 30 calendar days of invoice dispatch."
        );
        embeddingStore.add(embeddingModel.embed(policyChunk).content(), policyChunk);

        // 3. Configure ContentRetriever (top-1 match, minScore 0.70)
        ContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
            .embeddingStore(embeddingStore)
            .embeddingModel(embeddingModel)
            .maxResults(1)
            .minScore(0.70)
            .build();

        // 4. Wire retriever directly into declarative AiServices!
        CorporateAssistant assistant = AiServices.builder(CorporateAssistant.class)
            .chatLanguageModel(chatModel)
            .contentRetriever(retriever)
            .build();

        // The assistant automatically retrieves the policy chunk and answers factually!
        String response = assistant.ask("What is the time window for customer subscription refunds?");
        System.out.println("Grounded Response:\n" + response);
    }
}
```

### Line-by-Line Walkthrough
1. **`TextSegment.from(...)`**: Constructs an atomic text chunk representing a document excerpt.
2. **`embeddingStore.add(embeddingModel.embed(chunk).content(), chunk)`**: Converts the text chunk into a high-dimensional float vector and persists it in the vector store alongside the raw text.
3. **`EmbeddingStoreContentRetriever.builder()`**: Constructs the retrieval SPI. `maxResults(1)` limits context size, while `minScore(0.70)` discards weak matches.
4. **`.contentRetriever(retriever)`**: Attaches the retriever to the `AiServices` builder. LangChain4j automatically intercepts user questions, queries the vector store, injects the retrieved excerpt into the prompt, and invokes the chat model.

---

## 3. Core Concept Walkthrough (Basic → Intermediate)

```
+-------------------------------------------------------------------------------+
|                       LANGCHAIN4J TWO-PATH RAG LIFECYCLE                      |
+-------------------------------------------------------------------------------+
|                                                                               |
|  [ PATH 1: OFFLINE INGESTION PIPELINE (Write Path) ]                          |
|  Enterprise Documents (PDF, DOCX, Markdown)                                   |
|         |                                                                     |
|         v                                                                     |
|  [ DocumentParser (Apache Tika) ] -> Extract plain text                       |
|         |                                                                     |
|         v                                                                     |
|  [ DocumentSplitter ] -> Chunks into TextSegments (e.g. 500 tokens / 50 ov)   |
|         |                                                                     |
|         v                                                                     |
|  [ EmbeddingModel ] -> Generates vector coordinates                           |
|         |                                                                     |
|         v                                                                     |
|  [ EmbeddingStore (pgvector, Qdrant, Milvus) ] -> Indexed storage             |
|                                                                               |
|  ===========================================================================  |
|                                                                               |
|  [ PATH 2: ONLINE RETRIEVAL PIPELINE (Read Path) ]                            |
|  User Query: "What is our refund window?"                                     |
|         |                                                                     |
|         v                                                                     |
|  [ ContentRetriever ] -> EmbeddingStoreContentRetriever                       |
|         |                                                                     |
|         v                                                                     |
|  [ EmbeddingStore.findRelevant(queryVector, maxResults, minScore) ]           |
|         |                                                                     |
|         v                                                                     |
|  [ Context Augmentation ] -> Injects retrieved TextSegments into prompt       |
|         |                                                                     |
|         v                                                                     |
|  [ ChatLanguageModel (AiServices) ] -> Returns grounded answer with citations |
+-------------------------------------------------------------------------------+
```

### The 4 Core RAG Abstractions in LangChain4j
1. **`TextSegment`**: An immutable chunk of text paired with a `Metadata` map (carrying document name, page number, section title, and tenant ID).
2. **`EmbeddingModel`**: Converts strings and `TextSegment` instances into mathematical vector coordinates (`Embedding`).
3. **`EmbeddingStore<TextSegment>`**: The database abstraction for vector persistence and cosine similarity queries (backed by PostgreSQL `pgvector`, Redis, Qdrant, Milvus, etc.).
4. **`ContentRetriever`**: The high-level query SPI that takes an incoming `Query` and returns matching `Content` items for prompt augmentation.

### Easy-RAG: Ingesting Entire Folders in 3 Lines of Code
For rapid prototyping or local desktop search, LangChain4j provides `EmbeddingStoreIngestor`:

```java
package com.genai.langchain4j.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;

import java.util.List;

public class EasyRagSetup {

    public static void ingestFolder(String directoryPath, EmbeddingStore<TextSegment> store, EmbeddingModel model) {
        // 1. Load all PDFs, DOCX, and Markdown files from directory recursively
        List<Document> documents = FileSystemDocumentLoader.loadDocuments(directoryPath);

        // 2. Automates parsing, chunking, embedding generation, and database insertion!
        EmbeddingStoreIngestor.ingest(documents, store);
    }
}
```

---

## 4. Prerequisite & Supporting Concepts

### Prerequisite / Supporting Concept: Tuning `minScore` and `maxResults`
In `EmbeddingStoreContentRetriever`:
- **`maxResults(int k)`**: Defines the maximum number of document chunks returned. Setting $k$ too high (e.g., 20) clutters the prompt with irrelevant noise and triggers the "Lost in the Middle" attention penalty. Standard default: 3 to 5 chunks.
- **`minScore(double score)`**: Sets the minimum cosine similarity threshold (e.g., `0.75`).
  - If a user asks an off-topic question (*"What is the airspeed of an unladen swallow?"* against a banking manual), the vector store returns zero documents.
  - The model then safely refuses to answer (*"I do not have information in the knowledge base"*) rather than hallucinating!

---

## 5. Advanced Depth (Intermediate → Advanced)

### Multi-Tenant Metadata Filtering
In enterprise applications, multiple clients or corporate departments share the same vector store. You must prevent User A from retrieving confidential salary or HR records belonging to Department B:

```java
package com.genai.langchain4j.rag;

import dev.langchain4j.data.segment.TextSegment;
import java.util.List;

public final class MultiTenantFilter {

    private MultiTenantFilter() {}

    public static List<TextSegment> filterByTenant(List<TextSegment> segments, String currentTenantId) {
        if (segments == null || segments.isEmpty()) {
            return List.of();
        }

        return segments.stream()
            .filter(segment -> currentTenantId.equals(segment.metadata().getString("tenantId")))
            .toList();
    }
}
```

### Document Source Citation Formatter
For regulatory compliance, users should be presented with clickable citations showing where the AI found its facts:

```java
package com.genai.langchain4j.rag;

import dev.langchain4j.data.segment.TextSegment;
import java.util.List;

public final class CitationFormatter {

    private CitationFormatter() {}

    public static String formatMarkdownCitations(List<TextSegment> retrievedSegments) {
        if (retrievedSegments == null || retrievedSegments.isEmpty()) {
            return "No citations available.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n\n### Verified Sources:\n");
        sb.append("| # | Document | Section | Excerpt |\n");
        sb.append("|:--|:---------|:--------|:--------|\n");

        for (int i = 0; i < retrievedSegments.size(); i++) {
            TextSegment seg = retrievedSegments.get(i);
            String doc = seg.metadata().getString("documentName");
            String section = seg.metadata().getString("sectionTitle");
            String excerpt = seg.text().length() > 60 ? seg.text().substring(0, 60) + "..." : seg.text();

            sb.append(String.format("| %d | %s | %s | %s |\n", i + 1, doc != null ? doc : "Unknown", section != null ? section : "N/A", excerpt));
        }

        return sb.toString();
    }
}
```

### Common Anti-Patterns & Production Traps

| Anti-Pattern | Why It Breaks in Production | Correct Architectural Solution |
|:---|:---|:---|
| **Omitting `minScore`** | When an off-topic question is submitted, the vector store returns the closest 3 chunks even if their similarity is 0.12, causing hallucinations. | Always configure `.minScore(0.75)` on `EmbeddingStoreContentRetriever` to filter weak matches. |
| **Monolithic File Ingestion** | Ingesting 500-page PDF files without chunking causes token truncation and averages diverse topics into an indistinct vector representation. | Use `DocumentSplitters.recursive(...)` with 300–500 tokens and 50-token sliding overlap. |
| **Re-Embedding Unchanged Documents** | Running `EmbeddingModel.embed()` across millions of rows on every server restart wastes thousands of dollars in cloud API spend. | Check document hash checksums in your database and embed only new or modified documents. |

---

## 6. Quick Recap
- **RAG (Retrieval-Augmented Generation)** grounds conversational models in private enterprise data by retrieving verified text chunks and injecting them into the prompt.
- The **Ingestion Path (Write)** parses, chunks, embeds, and stores documents in vector databases; the **Retrieval Path (Read)** queries the vector store, augments the prompt, and generates grounded answers.
- In LangChain4j, RAG is decoupled across **`TextSegment`**, **`EmbeddingModel`**, **`EmbeddingStore`**, and **`ContentRetriever`**.
- Declarative agents connect to semantic search via `.contentRetriever(...)` on `AiServices.builder()`.
- Set **`minScore(0.75)`** to prune weak matches and prevent hallucinations on out-of-domain queries.

---

## 7. Self-Check Questions & Practice Exercises

### 5-Question Self-Check Quiz

#### Question 1
What is the primary role of `ContentRetriever` in LangChain4j?
- A) To convert Java bytecode into WebAssembly.
- B) To act as the standard query SPI that accepts a user query and returns relevant `TextSegment` contents from a vector store or search index.
- C) To bill the user's credit card.
- D) To format JSON schemas.

#### Question 2
In LangChain4j RAG, why is setting a `minScore` threshold on `EmbeddingStoreContentRetriever` critical?
- A) It speeds up the GPU clock rate.
- B) It prevents low-similarity, irrelevant documents from being injected into the prompt, preventing the model from hallucinating or answering off-topic questions.
- C) It is required by the SQL standard.
- D) It reduces the memory size of Java virtual threads.

#### Question 3
What is Easy-RAG in LangChain4j?
- A) A low-cost subscription tier from OpenAI.
- B) A turnkey utility (`EmbeddingStoreIngestor`) that automates document parsing, recursive splitting, embedding generation, and vector indexing in a few lines of code.
- C) A specialized Python script that runs outside the JVM.
- D) An embedded relational database.

#### Question 4
How are retrieved documents injected into a declarative `AiServices` agent?
- A) By passing them manually in every method invocation.
- B) By configuring `.contentRetriever(...)` on `AiServices.builder(...)`, allowing the framework to execute retrieval and prompt augmentation transparently.
- C) By writing the files to the JVM classpath.
- D) `AiServices` does not support RAG.

#### Question 5
What is the purpose of `TextSegment` metadata in enterprise search?
- A) To store the author, document title, section heading, and security tenant ID alongside the text chunk, enabling post-filtering and verifiable citations.
- B) To store Java class reflection bytecodes.
- C) To encrypt the database disk partition.
- D) Metadata is ignored by LangChain4j.

---

### Quiz Answers & Explanations
1. **B**: `ContentRetriever` is the foundational SPI bridging retrieval mechanisms (vector stores, hybrid search, keyword indexes) to the generation layer (`AiServices`).
2. **B**: Without a `minScore` threshold, the vector store will always return the top-K closest vectors, even if their similarity is completely irrelevant to the user's question, causing context contamination.
3. **B**: Easy-RAG provides high-level convenience methods allowing developers to point to a folder of enterprise files and have them automatically parsed, chunked, embedded, and stored with minimal boilerplate.
4. **B**: By attaching a `ContentRetriever` to `AiServices.builder()`, LangChain4j intercepts user messages, queries the retriever, constructs the augmented prompt, and invokes the model automatically.
5. **A**: Metadata carries critical provenance data (document URI, page number, tenant ID) allowing systems to format source citations and filter search results by security context.

---

### Hands-On Practice Exercises

#### Exercise 1: Multi-Tenant Metadata Filtering
**Problem Statement**:  
Write a utility method `filterByTenant(List<TextSegment> retrieved, String expectedTenantId)` that filters matches so that queries from `tenant-A` never retrieve documents belonging to `tenant-B`.

<details>
<summary>👉 View Solution</summary>

```java
package com.genai.langchain4j.exercises;

import dev.langchain4j.data.segment.TextSegment;
import java.util.List;

public class MultiTenantContentRetriever {

    public static List<TextSegment> filterByTenant(List<TextSegment> retrieved, String expectedTenantId) {
        if (retrieved == null) return List.of();
        return retrieved.stream()
            .filter(seg -> expectedTenantId.equals(seg.metadata().getString("tenantId")))
            .toList();
    }
}
```
</details>

#### Exercise 2: Reciprocal Similarity Threshold Calibrator
**Problem Statement**:  
Write a utility method that checks the distribution of scores in `List<Double> similarityScores`. If the top score is below a strict threshold of `0.65`, return `false` to signal that no reliable context was found.

<details>
<summary>👉 View Solution</summary>

```java
package com.genai.langchain4j.exercises;

import java.util.List;

public class RelevanceThresholdCalibrator {

    public static final double MIN_ACCEPTABLE_SIMILARITY = 0.65;

    public static boolean isContextReliable(List<Double> scores) {
        if (scores == null || scores.isEmpty()) return false;
        return scores.get(0) >= MIN_ACCEPTABLE_SIMILARITY;
    }
}
```
</details>

---

[← Previous: Day 45 - Structured Extraction & Guardrails](../Day_45_Structured_Extraction_Guardrails/Day_45_Structured_Extraction_Guardrails.md) | [Next: Day 47 - Advanced RAG Chunking & Re-Ranking →](../Day_47_Advanced_RAG_Chunking_ReRanking/Day_47_Advanced_RAG_Chunking_ReRanking.md)
