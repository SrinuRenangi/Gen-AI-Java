# Day 39: RAG — Retrieval-Augmented Generation

[← Previous: Day 38 - Vector Stores](../Day_38_Vector_Stores_Semantic_Memory/Day_38_Vector_Stores_Semantic_Memory.md) | [Next: Day 40 - Advanced RAG →](../Day_40_Advanced_RAG_Query_ReRanking/Day_40_Advanced_RAG_Query_ReRanking.md)

---

## 1. Topic Overview
Retrieval-Augmented Generation (RAG) is an architectural pattern that retrieves relevant, authoritative facts from private enterprise vector stores and injects them as dynamic context into a Large Language Model's prompt. This enables LLMs to answer domain-specific questions with 100% factual grounding, audit-grade citations, zero hallucinated claims, and without requiring costly model fine-tuning.

---

## 2. Basic Foundations (True Zero)

### What is Retrieval-Augmented Generation (RAG)?
When you ask a standard commercial LLM (like GPT-4o or Llama 3) a question about your company's proprietary systems or private HR policies, it will either:
1. State that it does not know because the data was never part of its public training set.
2. Make up a convincing, completely false answer (an **AI Hallucination**).

RAG solves this by converting a "closed-book exam" into an **"open-book exam"**:
- When a user asks a question, your application first queries your vector database to find the exact pages or paragraphs containing the facts.
- It places those retrieved excerpts inside the prompt.
- It instructs the LLM: *"Answer the user's question strictly using only the excerpts provided below, and cite your sources."*

### Relatable Physical Analogy: The Open-Book Research Assistant
Imagine sitting for a high-stakes medical licensing exam:
- **Closed-Book (Vanilla LLM)**: You must answer from memory based on what you studied three years ago. If asked about a new drug approved last week or precise dosage formulas, you risk guessing or misremembering.
- **Open-Book with an Assistant (RAG Pipeline)**: Whenever an obscure case comes up, an assistant immediately pulls the latest 2026 medical journal, places the exact 3 relevant clinical trial pages on your desk, and says: *"Base your diagnosis solely on these paragraphs."* You read the verified text, synthesize the conclusion, and write: *"Prescribe Drug X, 50mg [Source: NEJM 2026, Page 412]."*

### Minimal Beginner-Friendly Working Code
Spring AI makes RAG effortless with its built-in `QuestionAnswerAdvisor`:

```java
package com.genai.springai.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class SimpleRagRunner implements CommandLineRunner {

    private final ChatClient chatClient;

    public SimpleRagRunner(ChatClient.Builder builder, VectorStore vectorStore) {
        // Wire QuestionAnswerAdvisor directly into the ChatClient
        this.chatClient = builder
            .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore, SearchRequest.builder().topK(3).build()))
            .build();
    }

    @Override
    public void run(String... args) {
        // The QuestionAnswerAdvisor automatically intercepts this question,
        // performs similarity search against vectorStore, and injects context into the prompt!
        String answer = chatClient.prompt()
            .user("What is our company's P0 outage SLA response time?")
            .call()
            .content();

        System.out.println("Grounded Answer:\n" + answer);
    }
}
```

### Line-by-Line Walkthrough
1. **`public SimpleRagRunner(ChatClient.Builder builder, VectorStore vectorStore)`**: Injects the fluent `ChatClient.Builder` and the configured `VectorStore` (backed by PostgreSQL `pgvector`, Redis, or Milvus).
2. **`new QuestionAnswerAdvisor(vectorStore, SearchRequest.builder().topK(3).build())`**: Instantiates Spring AI's built-in RAG advisor. For every incoming user message, this advisor executes a vector search, retrieves the top 3 closest chunks, and injects them into the prompt.
3. **`builder.defaultAdvisors(...)`**: Binds the advisor to the client instance, guaranteeing every query through this client is automatically grounded in enterprise knowledge.
4. **`chatClient.prompt().user("...").call().content()`**: Sends the prompt. The user does not need to manually write retrieval or augmentation boilerplate—the advisor handles the entire RAG pipeline transparently.

---

## 3. Core Concept Walkthrough (Basic → Intermediate)

```
====================================================================================================
                             PHASE A: OFFLINE INGESTION PIPELINE
====================================================================================================

 ┌───────────────────┐     ┌───────────────────┐     ┌───────────────────┐     ┌───────────────────┐
 │  DocumentReader   │ ──► │ TokenTextSplitter │ ──► │  EmbeddingModel   │ ──► │    VectorStore    │
 │ (PDF / Confluence)│     │(Chunks with 50-ov)│     │(nomic-embed / 768)│     │(PostgreSQL pgvect)│
 └───────────────────┘     └───────────────────┘     └───────────────────┘     └───────────────────┘

====================================================================================================
                        PHASE B: ONLINE RETRIEVAL & GENERATION PIPELINE
====================================================================================================

 User Question: "What is our P0 outage response time?"
         │
         ▼
┌──────────────────┐
│  EmbeddingModel  │ ──► Generates Query Vector: [0.051, 0.021, ...]
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│   VectorStore    │ ──► Executes Cosine Distance (<=>) search in pgvector
└────────┬─────────┘
         │
         ▼ Returns Top-K Relevant Document Chunks
┌────────────────────────────────────────────────────────────────────────┐
│ Context Augmentation Envelope                                          │
│                                                                        │
│ <system_directive>                                                     │
│   Answer strictly using only the <context> below. Cite sources.        │
│ </system_directive>                                                    │
│ <context>                                                              │
│   <document id="DOC-SLA-01">                                           │
│     P0 outages require an initial engineer response under 15 minutes.  │
│   </document>                                                          │
│ </context>                                                             │
│ <question>What is our P0 outage response time?</question>              │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌──────────────────┐
│    ChatClient    │ ──► Calls LLM (GPT-4o / Llama 3.2)
└────────┬─────────┘
         │
         ▼
 Grounded Answer: "According to our SLA, P0 critical outages require an initial
                   response under 15 minutes. [SOURCE: DOC-SLA-01]"
```

### The 5 Stages of the RAG Pipeline
1. **Document Ingestion**: Reading unstructured sources (PDFs, Markdown, Word docs, web pages) into Spring AI `Document` objects.
2. **Text Chunking**: Splitting large documents into smaller semantic fragments (e.g., 500 tokens) with sliding window overlap (e.g., 50 tokens) to ensure ideas spanning chunk borders retain context.
3. **Embedding Generation**: Passing each text chunk to `EmbeddingModel.embed()` to produce high-dimensional coordinates.
4. **Vector Storage**: Inserting document text, metadata, and coordinates into a vector database (e.g., PostgreSQL `pgvector`).
5. **Retrieval & Grounded Generation**: When a user asks a question, calculating cosine similarity, retrieving the top $K$ chunks, inserting them into a prompt envelope, and instructing the LLM to synthesize the final verified answer.

### Ingesting Real Documents (PDFs & Token Splitters)
Spring AI provides first-class readers for common enterprise file formats:

```java
package com.genai.springai.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EnterpriseDocumentIngestionService {

    private final VectorStore vectorStore;

    public EnterpriseDocumentIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void ingestPdf(Resource pdfResource) {
        // 1. Read PDF pages using Spring AI's PagePdfDocumentReader
        PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder()
            .withPageExtractedTextFormatter(new ExtractedTextFormatter.Builder()
                .withNumberOfTopPagesToSkipBeforeHeaderExtraction(0)
                .build())
            .build();

        PagePdfDocumentReader reader = new PagePdfDocumentReader(pdfResource, config);
        List<Document> pages = reader.read();

        // 2. Split pages into 500-token chunks with 50-token overlap
        TokenTextSplitter splitter = new TokenTextSplitter(500, 50, 5, 1000, true);
        List<Document> chunks = splitter.split(pages);

        // 3. Batch persist into vector store (pgvector)
        vectorStore.add(chunks);
        System.out.println("Ingested " + chunks.size() + " chunks from: " + pdfResource.getFilename());
    }
}
```

---

## 4. Prerequisite & Supporting Concepts

### Prerequisite / Supporting Concept: Context Stuffing & Context Windows
Every LLM has a finite **context window** (the maximum number of tokens it can process in a single request).
- "Context Stuffing" refers to taking retrieved documents and pasting them directly into the prompt string.
- If you retrieve too many documents (e.g., `topK = 50`), you risk:
  1. Exceeding token limits, triggering request errors.
  2. The **"Lost in the Middle" phenomenon**: research shows LLMs pay closest attention to the beginning and end of long prompts, frequently ignoring facts buried in the middle.
  3. Inflating API latency and cost.
- **Rule of Thumb**: Set `topK` to between 3 and 5 chunks of 300–500 tokens for optimal reasoning.

### Prerequisite / Supporting Concept: Fine-Tuning vs. RAG

| Architectural Dimension | Model Fine-Tuning | Retrieval-Augmented Generation (RAG) |
|:---|:---|:---|
| **Data Freshness** | Stale; frozen at training time. Updating requires retraining. | Real-time; update a database row and the AI knows it immediately. |
| **Hallucination Rate** | Moderate to High; model generates from statistical memory. | Near Zero; model is constrained to retrieved text context. |
| **Auditability & Citations** | Black box; cannot prove why the model said something. | 100% auditable; every claim cites `[SOURCE: docId]`. |
| **Implementation Cost** | High ($1,000s–$100,000s in GPU clusters and ML talent). | Low; uses standard Java, Spring Boot, and PostgreSQL. |
| **Access Control & RBAC** | Impossible; all data baked into universal model weights. | Trivial; metadata filters enforce user tenant and role boundaries. |

---

## 5. Advanced Depth (Intermediate → Advanced)

### Anti-Hallucination Guardrails & Source Attribution
The single greatest operational hazard in enterprise GenAI is a confident hallucination when the database contains no relevant documents.

To enforce strict adherence, design a robust XML prompt envelope with **explicit negative constraints**:

```java
package com.genai.springai.rag;

import org.springframework.ai.document.Document;

import java.util.List;

public final class RagContextAugmenter {

    private RagContextAugmenter() {}

    public static String buildAugmentedPrompt(String userQuestion, List<Document> retrievedDocs) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("""
            <system_directive>
            You are an authoritative enterprise knowledge assistant. Answer the user's question
            using ONLY the factual information provided inside the <context> block below.

            STRICT RULES:
            1. Cite the document ID for every factual claim using [SOURCE: docId].
            2. If the answer cannot be found in <context>, respond EXACTLY with:
               "I do not have sufficient information in the knowledge base to answer this question."
            3. Do NOT extrapolate, speculate, or utilize outside world knowledge not contained in <context>.
            4. If different documents in <context> conflict, explicitly highlight the discrepancy.
            </system_directive>

            <context>
            """);

        if (retrievedDocs == null || retrievedDocs.isEmpty()) {
            sb.append("  [NO RELEVANT DOCUMENTS FOUND IN KNOWLEDGE BASE]\n");
        } else {
            for (Document doc : retrievedDocs) {
                sb.append("  <document id=\"").append(doc.getId()).append("\">\n")
                  .append("    ").append(doc.getText().trim()).append("\n")
                  .append("  </document>\n");
            }
        }

        sb.append("</context>\n\n<question>\n")
          .append(userQuestion)
          .append("\n</question>");

        return sb.toString();
    }
}
```

### Complete End-to-End Orchestration Service
Here is how you orchestrate retrieval, prompt augmentation, and generation into a clean production service:

```java
package com.genai.springai.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EnterpriseRagService {

    public record RagAnswer(String text, List<Document> citedSources) {}

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public EnterpriseRagService(ChatClient.Builder builder, VectorStore vectorStore) {
        this.chatClient = builder.build();
        this.vectorStore = vectorStore;
    }

    public RagAnswer answerQuestion(String question, int topK, double threshold) {
        // 1. Retrieve top matching documents above similarity threshold
        SearchRequest request = SearchRequest.builder()
            .query(question)
            .topK(topK)
            .similarityThreshold(threshold)
            .build();

        List<Document> retrievedDocs = vectorStore.similaritySearch(request);

        // 2. Build anti-hallucination XML prompt envelope
        String augmentedPrompt = RagContextAugmenter.buildAugmentedPrompt(question, retrievedDocs);

        // 3. Call LLM with grounded prompt
        String answer = chatClient.prompt()
            .user(augmentedPrompt)
            .call()
            .content();

        return new RagAnswer(answer, retrievedDocs);
    }
}
```

### Common Anti-Patterns & Production Traps

| Anti-Pattern | Why It Fails in Production | Correct Architectural Solution |
|:---|:---|:---|
| **No Similarity Threshold** | When a user asks an out-of-domain question ("What is the capital of Mars?"), `similaritySearch` returns the closest 3 documents regardless of relevance score (e.g. 0.12 similarity), confusing the LLM. | Always configure `.similarityThreshold(0.70)` to prune weak matches before prompting. |
| **Naive String Truncation** | Splitting text on arbitrary character lengths cuts words and sentences in half, losing semantic meaning. | Use `TokenTextSplitter` with token-aware sliding window overlap. |
| **Ignoring Chunk Metadata** | Storing text without document IDs, titles, or source URLs prevents audit citation extraction in client UI. | Attach `sourceUrl`, `parentDocId`, and `pageNumber` to every chunk's metadata map. |

---

## 6. Quick Recap
- **RAG (Retrieval-Augmented Generation)** grounds LLM responses in verified enterprise data by retrieving relevant chunks and injecting them into the prompt.
- RAG eliminates **hallucinations**, bypasses static **knowledge cutoff dates**, and preserves **data privacy** without costly model retraining.
- The **5-Stage Pipeline**: Read documents $\rightarrow$ split into overlapping token chunks $\rightarrow$ generate embeddings $\rightarrow$ store in vector database $\rightarrow$ retrieve & generate.
- Spring AI's **`QuestionAnswerAdvisor`** encapsulates the entire RAG retrieval-and-stuffing lifecycle into a single line of Java configuration.
- Always enforce **strict negative prompt guardrails** requiring citations and instructing the LLM to admit when information is missing.

---

## 7. Self-Check Questions & Practice Exercises

### 5-Question Self-Check Quiz

#### Question 1
What does the acronym RAG stand for in Artificial Intelligence?
- A) Relational Access Gateway
- B) Retrieval-Augmented Generation
- C) Recursive Algorithm Generator
- D) Random Array Grouping

#### Question 2
Why is RAG generally preferred over Model Fine-Tuning for enterprise documentation and internal policies?
- A) Fine-tuning is free, while RAG requires millions of dollars in GPU cloud spend.
- B) RAG grounds the model with real-time, verified documents without expensive GPU training, keeps confidential data inside your database, and provides audit citations for every fact.
- C) Fine-tuning only works on macOS.
- D) RAG eliminates the need for Java.

#### Question 3
What is the role of Spring AI's `QuestionAnswerAdvisor`?
- A) It deletes inactive user accounts from the database.
- B) It automatically intercepts `ChatClient` prompts, queries the configured `VectorStore`, injects retrieved document snippets into the prompt context, and forwards the grounded request to the LLM.
- C) It converts SQL databases to MongoDB.
- D) It formats Java source code.

#### Question 4
How does an Anti-Hallucination Guardrail in a RAG prompt ensure user trust?
- A) By encrypting the network response.
- B) By explicitly instructing the model to reply *"I do not have sufficient information"* if the answer is absent from the provided `<context>`, preventing the model from inventing plausible lies.
- C) By forcing the model to speak Latin.
- D) By disabling temperature.

#### Question 5
In a RAG pipeline, why is text chunking with overlap (e.g. 500 tokens with 50-token overlap) necessary?
- A) To make document files larger on disk.
- B) To fit within the embedding model's context window and ensure sentences cut across boundary edges maintain contextual and semantic continuity.
- C) Overlap is an anti-pattern and should never be used.
- D) It enables multithreading in Python.

---

### Quiz Answers & Explanations
1. **B**: RAG stands for Retrieval-Augmented Generation.
2. **B**: RAG separates reasoning capabilities (the foundation model) from dynamic knowledge retrieval (PostgreSQL `pgvector`), allowing instant updates without retraining.
3. **B**: `QuestionAnswerAdvisor` encapsulates the full online retrieval-and-stuffing pipeline into a reusable Spring AI advisor.
4. **B**: Strict negative refusal instructions force the model to admit lack of knowledge rather than hallucinating false facts.
5. **B**: Sliding window overlap ensures that ideas or sentences split across chunk boundaries do not lose their semantic context during embedding.

---

### Hands-On Practice Exercises

#### Exercise 1: Multi-Document Citation Extractor
**Problem Statement**:  
When an LLM responds to a RAG query, it embeds `[SOURCE: docId]` markers in its text.  
Write a Java utility `CitationExtractor.extractCitations(String responseText)` that uses regular expressions to find all unique cited document IDs and returns them as a `Set<String>`.

<details>
<summary>👉 View Solution</summary>

```java
package com.genai.springai.rag;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CitationExtractor {

    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[SOURCE:\\s*([^\\]]+)\\]");

    private CitationExtractor() {}

    public static Set<String> extractCitations(String answerText) {
        if (answerText == null || answerText.isBlank()) return Set.of();

        Set<String> citedDocIds = new LinkedHashSet<>();
        Matcher matcher = CITATION_PATTERN.matcher(answerText);

        while (matcher.find()) {
            citedDocIds.add(matcher.group(1).trim());
        }

        return Collections.unmodifiableSet(citedDocIds);
    }
}
```
</details>

#### Exercise 2: Adaptive Threshold Fallback Advisor
**Problem Statement**:  
Write a custom Spring AI Advisor `AdaptiveThresholdRagAdvisor` that first attempts retrieval with a strict similarity threshold (`0.80`). If zero documents are retrieved, it automatically relaxes the threshold to `0.65` and tries one more time before proceeding to prompt generation.

<details>
<summary>👉 View Solution</summary>

```java
package com.genai.springai.rag;

import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

public class AdaptiveThresholdRagAdvisor implements CallAroundAdvisor {

    private final VectorStore vectorStore;

    public AdaptiveThresholdRagAdvisor(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest request, CallAroundAdvisorChain chain) {
        String query = request.userText();

        // 1. First attempt: Strict 0.80 threshold
        List<Document> docs = vectorStore.similaritySearch(
            SearchRequest.builder().query(query).topK(3).similarityThreshold(0.80).build()
        );

        // 2. Fallback attempt: Relaxed 0.65 threshold
        if (docs.isEmpty()) {
            docs = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(3).similarityThreshold(0.65).build()
            );
        }

        // 3. Inject context into prompt
        String augmented = RagContextAugmenter.buildAugmentedPrompt(query, docs);
        AdvisedRequest augmentedRequest = AdvisedRequest.from(request)
            .withUserText(augmented)
            .build();

        return chain.nextAroundCall(augmentedRequest);
    }

    @Override
    public String getName() {
        return "AdaptiveThresholdRagAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
```
</details>

---

[← Previous: Day 38 - Vector Stores](../Day_38_Vector_Stores_Semantic_Memory/Day_38_Vector_Stores_Semantic_Memory.md) | [Next: Day 40 - Advanced RAG →](../Day_40_Advanced_RAG_Query_ReRanking/Day_40_Advanced_RAG_Query_ReRanking.md)
