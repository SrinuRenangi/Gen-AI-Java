package com.javagenai.day05;

import java.util.*;

public class ModernJavaDemo {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   DAY 05: MODERN JAVA 21 FEATURES FOR GEN AI     ");
        System.out.println("==================================================");

        // 1. Records for Structured AI Extraction
        System.out.println("1. Structured AI Output via Java Records:");
        LineItem item1 = new LineItem("NVIDIA H100 GPU Cloud Hours", 100, 2.50);
        LineItem item2 = new LineItem("OpenAI API Token Batch (10M)", 2, 15.00);
        ExtractedInvoice invoice = new ExtractedInvoice("INV-2026-9901", "Enterprise AI Cloud Inc.", List.of(item1, item2));

        System.out.println("   " + invoice);
        System.out.printf("   Calculated Grand Total: $%.2f USD%n", invoice.grandTotal());
        System.out.println();

        // 2. Safe Metadata Extraction with Optional
        System.out.println("2. Null-Safe Metadata Parsing with Optional<T>:");
        Map<String, String> metaWithEmail = Map.of("author_name", "Dr. John Doe", "author_email", "johndoe@deepmind.com");
        Map<String, String> metaWithoutEmail = Map.of("author_name", "Anonymous Contributor");

        System.out.println("   Domain found: " + DocumentMetadataExtractor.extractAuthorDomain(metaWithEmail).orElse("[NONE]"));
        System.out.println("   Missing domain fallback: " + DocumentMetadataExtractor.extractAuthorDomain(metaWithoutEmail).orElse("[UNKNOWN DOMAIN]"));
        System.out.println();

        // 3. Multi-line Text Blocks
        System.out.println("3. Clean AI Prompt Formatting via Text Blocks (\"\"\"):");
        String promptTemplate = """
            [SYSTEM INSTRUCTION]
            You are an autonomous enterprise agent operating in %s mode.
            Target Service: %s
            Max Retries   : %d
            """;
        String renderedPrompt = promptTemplate.formatted("PRODUCTION", "VectorIndexingService", 3);
        System.out.println(renderedPrompt.indent(3));

        // 4. Sequenced Collections (Java 21)
        System.out.println("4. Sequenced Collections in Action:");
        List<String> conversation = new ArrayList<>(List.of("User: System status?", "AI: All systems nominal.", "User: Run diagnostics."));
        System.out.println("   First Prompt: " + conversation.getFirst());
        System.out.println("   Latest Prompt: " + conversation.getLast());
        System.out.println("   Chronological Reverse: " + conversation.reversed());
        System.out.println("==================================================");
    }
}
