package com.genai.foundations.day08;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Day 08: I/O, Modern HTTP, JSON Serialization, and Testing in Memory.
 * Demonstrates NIO file operations, hermetic mock client decoupling, and resource cleanup.
 */

// 1. Immutable Domain Record for JSON payload mapping
record AiCompletionResponse(String id, String summary, int tokensUsed) {}

// 2. Service interface for abstraction
interface AiGatewayClient {
    String sendPrompt(String prompt);
}

// 3. Application service consuming files and AI clients
class DocumentSummarizer {
    private final AiGatewayClient client;

    public DocumentSummarizer(AiGatewayClient client) {
        this.client = Objects.requireNonNull(client, "client cannot be null");
    }

    public String summarizeDocument(Path filePath) throws IOException {
        // High-performance Java NIO file read:
        String content = Files.readString(filePath);
        return client.sendPrompt("Summarize: " + content);
    }
}

// 4. Standalone Runner demonstrating execution and file I/O
public class IoTestingDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("   DAY 08: I/O, STREAMING & TESTING MEMORY TRACE  ");
        System.out.println("==================================================");

        // Create temporary document using Java NIO
        Path tempFile = Files.createTempFile("ai_doc_", ".txt");
        Files.writeString(tempFile, "Java 21 Virtual Threads and NIO zero-copy channels.");
        System.out.println("1. Temporary file created at: " + tempFile.toAbsolutePath());

        // Hermetic mock implementation (stunt double)
        AiGatewayClient mockClient = prompt -> {
            System.out.println("   [Mock Gateway] Intercepted prompt without network calls!");
            return "Summary: High-performance Java I/O and Concurrency.";
        };

        // Service execution
        DocumentSummarizer summarizer = new DocumentSummarizer(mockClient);
        String summary = summarizer.summarizeDocument(tempFile);

        System.out.println("2. Summary Result: " + summary);

        // Clean up native file descriptors
        Files.deleteIfExists(tempFile);
        System.out.println("3. Temporary file cleanly deleted (OS descriptor released).");
        System.out.println("==================================================");
    }
}
