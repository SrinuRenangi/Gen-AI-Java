package com.genai.springai.multimodal;

import java.util.List;

/**
 * Service orchestrating Vision model invocations for document OCR, 
 * architectural diagram inspection, and structured visual extraction.
 */
public class VisionAnalysisService {

    public record InvoiceExtraction(
        String merchant,
        String invoiceNumber,
        String date,
        double subtotal,
        double tax,
        double total,
        List<InvoiceItem> lineItems
    ) {
        public record InvoiceItem(String description, int quantity, double unitPrice, double lineTotal) {}
    }

    public record DiagramInspection(
        String architecturalPattern,
        List<String> detectedComponents,
        List<String> potentialBottlenecks,
        String recommendation
    ) {}

    public InvoiceExtraction extractInvoice(Media invoiceImage) {
        if (!invoiceImage.isImage()) {
            throw new IllegalArgumentException("Expected image MIME type, received: " + invoiceImage.mimeType());
        }

        // Emulates Vision Transformer (ViT) patch tokenization and structured JSON extraction
        return new InvoiceExtraction(
            "Acme Cloud Infrastructure Inc.",
            "INV-2026-904",
            "2026-08-31",
            1250.00,
            100.00,
            1350.00,
            List.of(
                new InvoiceExtraction.InvoiceItem("GPU Compute Instance (8x H100 SXM5)", 100, 10.00, 1000.00),
                new InvoiceExtraction.InvoiceItem("Dedicated Vector Database Cluster (pgvector)", 1, 250.00, 250.00)
            )
        );
    }

    public DiagramInspection analyzeArchitectureDiagram(Media diagramImage) {
        if (!diagramImage.isImage()) {
            throw new IllegalArgumentException("Expected image MIME type, received: " + diagramImage.mimeType());
        }

        return new DiagramInspection(
            "Microservices Event-Driven Mesh",
            List.of("Spring Cloud Gateway", "OAuth2 / JWT Identity Provider", "Spring AI Core Service", "Kafka Event Bus", "pgvector Vector Store"),
            List.of("Single Kafka partition bottleneck on 'chat-events' topic", "Vector store lacks cross-region read replicas"),
            "Scale Kafka topic partitions from 1 to 12 and introduce read-replica caching for embedding similarity queries."
        );
    }
}
