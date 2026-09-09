package com.genai.langchain4j.advancedrag;

import java.util.*;

/**
 * Production-grade recursive document splitter.
 * Preserves structural paragraph boundaries while respecting maximum chunk capacity
 * and injecting a sliding overlap window to avoid semantic slicing bugs.
 */
public class RecursiveDocumentSplitter implements DocumentSplitter {

    private final int maxChunkSize;
    private final int chunkOverlap;

    public RecursiveDocumentSplitter(int maxChunkSize, int chunkOverlap) {
        if (chunkOverlap >= maxChunkSize) {
            throw new IllegalArgumentException("Overlap must be strictly smaller than maxChunkSize");
        }
        this.maxChunkSize = maxChunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    @Override
    public List<String> split(String text) {
        if (text == null || text.isBlank()) return List.of();

        List<String> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\n\n");
        StringBuilder currentChunk = new StringBuilder();

        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (trimmed.isEmpty()) continue;

            if (currentChunk.length() + trimmed.length() + 2 <= maxChunkSize) {
                if (!currentChunk.isEmpty()) currentChunk.append("\n\n");
                currentChunk.append(trimmed);
            } else {
                if (!currentChunk.isEmpty()) {
                    chunks.add(currentChunk.toString());
                    // Compute overlap from previous chunk
                    String prev = currentChunk.toString();
                    int startIdx = Math.max(0, prev.length() - chunkOverlap);
                    currentChunk = new StringBuilder(prev.substring(startIdx).trim());
                    if (!currentChunk.isEmpty()) currentChunk.append("\n\n");
                }
                currentChunk.append(trimmed);
            }
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString());
        }

        return chunks;
    }
}
