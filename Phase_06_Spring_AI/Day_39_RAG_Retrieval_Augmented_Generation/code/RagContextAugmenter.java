package com.genai.springai.rag;

import com.genai.springai.vectorstore.Document;

import java.util.List;

/**
 * Builds standard RAG augmented prompts containing context snippets, document citations,
 * and strict negative constraints preventing model hallucinations.
 */
public class RagContextAugmenter {

    public static String buildAugmentedPrompt(String userQuestion, List<Document> retrievedContext) {
        StringBuilder sb = new StringBuilder();

        sb.append("<system_directive>\n");
        sb.append("You are an authoritative enterprise knowledge assistant. Answer the user's question\n");
        sb.append("using ONLY the factual information provided inside the <context> block below.\n");
        sb.append("RULES:\n");
        sb.append("1. Cite the document ID for every claim using [SOURCE: docId].\n");
        sb.append("2. If the answer cannot be found in <context>, respond EXACTLY with:\n");
        sb.append("   \"I do not have sufficient information in the knowledge base to answer this question.\"\n");
        sb.append("3. Do NOT make up facts or extrapolate beyond the provided text.\n");
        sb.append("</system_directive>\n\n");

        sb.append("<context>\n");
        if (retrievedContext == null || retrievedContext.isEmpty()) {
            sb.append("  [NO RELEVANT DOCUMENTS FOUND IN KNOWLEDGE BASE]\n");
        } else {
            for (Document doc : retrievedContext) {
                sb.append("  <document id=\"").append(doc.id()).append("\">\n");
                sb.append("    ").append(doc.content().trim()).append("\n");
                sb.append("  </document>\n\n");
            }
        }
        sb.append("</context>\n\n");

        sb.append("<question>\n");
        sb.append(userQuestion.trim()).append("\n");
        sb.append("</question>");

        return sb.toString();
    }
}
