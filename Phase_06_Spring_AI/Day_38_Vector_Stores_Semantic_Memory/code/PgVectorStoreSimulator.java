package com.genai.springai.vectorstore;

import com.genai.springai.embeddings.EmbeddingModel;
import com.genai.springai.embeddings.VectorMath;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simulates Spring AI's PgVectorStore backed by PostgreSQL and pgvector extension:
 * - HNSW (Hierarchical Navigable Small World) index simulation
 * - Cosine Distance (<=>) operator
 * - Metadata Filtering (WHERE metadata->>'department' = 'FINANCE')
 */
public class PgVectorStoreSimulator implements VectorStore {

    private final EmbeddingModel embeddingModel;
    private final Map<String, Document> store = new ConcurrentHashMap<>();
    private boolean hnswIndexBuilt = false;

    public PgVectorStoreSimulator(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public void add(List<Document> documents) {
        for (Document doc : documents) {
            float[] vector = doc.embedding();
            if (vector == null) {
                // Auto-embed document content if vector not precomputed
                vector = embeddingModel.embed(doc.content());
            }
            Document storedDoc = doc.withEmbedding(vector);
            store.put(storedDoc.id(), storedDoc);
        }
        buildHnswIndex();
    }

    @Override
    public Optional<Boolean> delete(List<String> idList) {
        boolean anyRemoved = false;
        for (String id : idList) {
            if (store.remove(id) != null) {
                anyRemoved = true;
            }
        }
        return Optional.of(anyRemoved);
    }

    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        float[] queryVector = embeddingModel.embed(request.query());

        record ScoredDoc(Document doc, double score) {}

        return store.values().stream()
                // 1. Metadata Filtering (SQL WHERE clause)
                .filter(doc -> request.filterExpression().test(doc.metadata()))
                // 2. Vector Cosine Similarity (<=> distance)
                .map(doc -> new ScoredDoc(doc, VectorMath.cosineSimilarity(queryVector, doc.embedding())))
                // 3. Similarity Threshold Filter
                .filter(scored -> scored.score() >= request.similarityThreshold())
                // 4. Order by score descending
                .sorted(Comparator.comparingDouble(ScoredDoc::score).reversed())
                // 5. Limit (LIMIT topK)
                .limit(request.topK())
                .map(ScoredDoc::doc)
                .toList();
    }

    private void buildHnswIndex() {
        // Simulates: CREATE INDEX ON vector_store USING hnsw (embedding vector_cosine_ops)
        this.hnswIndexBuilt = true;
    }

    public boolean isHnswIndexBuilt() {
        return hnswIndexBuilt;
    }

    public int size() {
        return store.size();
    }

    @Override
    public String getName() {
        return "PostgreSQL pgvector (HNSW Cosine Index)";
    }
}
