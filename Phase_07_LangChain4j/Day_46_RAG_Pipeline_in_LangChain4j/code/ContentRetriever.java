package com.genai.langchain4j.rag;

import java.util.List;

/**
 * Standard SPI for retrieving relevant content segments given a natural language query.
 * Matches dev.langchain4j.rag.content.retriever.ContentRetriever.
 */
public interface ContentRetriever {
    List<TextSegment> retrieve(String query);
}
