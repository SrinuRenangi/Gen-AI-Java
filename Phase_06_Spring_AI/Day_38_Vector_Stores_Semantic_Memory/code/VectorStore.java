package com.genai.springai.vectorstore;

import java.util.List;
import java.util.Optional;

/**
 * Simulates Spring AI's central VectorStore interface.
 * Implemented by PgVectorStore, RedisVectorStore, Milvus, Qdrant, etc.
 */
public interface VectorStore {

    void add(List<Document> documents);

    Optional<Boolean> delete(List<String> idList);

    List<Document> similaritySearch(SearchRequest request);

    default List<Document> similaritySearch(String query) {
        return similaritySearch(SearchRequest.builder().query(query).build());
    }

    String getName();
}
