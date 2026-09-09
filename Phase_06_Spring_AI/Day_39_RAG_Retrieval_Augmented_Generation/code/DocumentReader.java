package com.genai.springai.rag;

import com.genai.springai.vectorstore.Document;

import java.util.List;

/**
 * Simulates Spring AI's DocumentReader interface.
 * Reads raw text, PDF, or markdown files and converts them into Spring AI Documents.
 */
public interface DocumentReader {
    List<Document> read();
}
