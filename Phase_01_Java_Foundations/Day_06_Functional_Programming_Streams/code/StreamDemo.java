package com.javagenai.day06;

import java.util.*;
import java.util.stream.Collectors;

public class StreamDemo {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   DAY 06: FUNCTIONAL STREAMS FOR AI PIPELINES    ");
        System.out.println("==================================================");

        // 1. Text Ingestion Pipeline
        System.out.println("1. Document Ingestion & Cleaning Pipeline:");
        List<String> rawTexts = List.of(
            "   Short   ",
            "   Enterprise RAG pipelines with Spring AI are robust.   ",
            "   ",
            "PostgreSQL vector extensions offer HNSW index support.",
            "Tiny",
            "Modern Java 21 streams enable expressive data filtering."
        );

        List<String> cleanDocs = DocumentPipeline.processDocuments(rawTexts);
        cleanDocs.forEach(d -> System.out.println("   Cleaned: " + d));
        System.out.println();

        // 2. Joining Chunks for LLM Context
        System.out.println("2. Context Assembly via Collectors.joining():");
        String context = cleanDocs.stream()
            .collect(Collectors.joining("\n---\n", "[START CONTEXT]\n", "\n[END CONTEXT]"));
        System.out.println(context);
        System.out.println();

        // 3. Analytics via groupingBy
        System.out.println("3. LLM Usage Analytics via GroupingBy:");
        List<LLMRecord> records = List.of(
            new LLMRecord("gpt-4o", 1200, 300),
            new LLMRecord("gpt-4o", 2500, 800),
            new LLMRecord("llama-3.2", 4000, 1500),
            new LLMRecord("llama-3.2", 1000, 500)
        );

        Map<String, Double> costs = UsageAnalytics.calculateCostPerModel(records);
        costs.forEach((model, cost) -> System.out.printf("   Model: %-10s Total Incurred Cost: $%.6f USD%n", model, cost));
        System.out.println();

        // 4. Parallel Stream Multi-Core Processing
        System.out.println("4. Parallel Stream Batch Vector Normalization:");
        List<Double> rawValues = new ArrayList<>(100_000);
        for (int i = 0; i < 100_000; i++) rawValues.add((double) i);

        long start = System.currentTimeMillis();
        double sum = rawValues.parallelStream()
            .map(x -> Math.sin(x) * Math.cos(x))
            .reduce(0.0, Double::sum);
        long elapsed = System.currentTimeMillis() - start;

        System.out.printf("   Processed 100,000 vector elements across multi-cores in: %d ms (Sum: %.4f)%n", elapsed, sum);
        System.out.println("==================================================");
    }
}
