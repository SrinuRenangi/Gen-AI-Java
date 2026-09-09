package com.genai.springai.embeddings;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * In-memory semantic search engine demonstrating vector similarity retrieval.
 */
public class SemanticSearchEngine {

    public record Document(String id, String content, float[] embedding) {}

    public record SearchResult(Document document, double similarityScore) {}

    private final EmbeddingModel embeddingModel;
    private final List<Document> documents = new ArrayList<>();

    public SemanticSearchEngine(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public void addDocument(String id, String content) {
        float[] embedding = embeddingModel.embed(content);
        documents.add(new Document(id, content, embedding));
    }

    public List<SearchResult> search(String query, int topK) {
        float[] queryEmbedding = embeddingModel.embed(query);

        return documents.stream()
                .map(doc -> new SearchResult(doc, VectorMath.cosineSimilarity(queryEmbedding, doc.embedding())))
                .sorted(Comparator.comparingDouble(SearchResult::similarityScore).reversed())
                .limit(topK)
                .toList();
    }

    public int documentCount() {
        return documents.size();
    }
}
