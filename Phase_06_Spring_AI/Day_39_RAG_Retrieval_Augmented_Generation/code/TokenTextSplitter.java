package com.genai.springai.rag;

import com.genai.springai.vectorstore.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simulates Spring AI's TokenTextSplitter.
 * Divides large documents into chunks of chunkSize with overlap to preserve semantic context across boundaries.
 */
public class TokenTextSplitter {

    private final int defaultChunkSize;
    private final int defaultOverlap;

    public TokenTextSplitter(int defaultChunkSize, int defaultOverlap) {
        this.defaultChunkSize = defaultChunkSize;
        this.defaultOverlap = defaultOverlap;
    }

    public TokenTextSplitter() {
        this(400, 50); // 400 chars chunk, 50 chars overlap
    }

    public List<Document> split(List<Document> documents) {
        List<Document> result = new ArrayList<>();

        for (Document doc : documents) {
            String text = doc.content();
            int step = defaultChunkSize - defaultOverlap;

            for (int i = 0; i < text.length(); i += step) {
                int end = Math.min(i + defaultChunkSize, text.length());
                String chunkContent = text.substring(i, end);

                Map<String, Object> chunkMeta = new HashMap<>(doc.metadata());
                chunkMeta.put("parentDocId", doc.id());
                chunkMeta.put("chunkStart", i);
                chunkMeta.put("chunkEnd", end);

                result.add(new Document(doc.id() + "-chunk-" + (i / step), chunkContent, chunkMeta));

                if (end == text.length()) break;
            }
        }
        return result;
    }
}
