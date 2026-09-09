package com.genai.langchain4j.advancedrag;

import java.util.List;

/**
 * Functional contract for chunking enterprise text documents into smaller segments.
 * Matches dev.langchain4j.data.document.DocumentSplitter.
 */
public interface DocumentSplitter {
    List<String> split(String text);
}
