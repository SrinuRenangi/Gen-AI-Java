package com.javagenai.day08;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class IODemo {

    public static void main(String[] args) throws IOException {
        System.out.println("==================================================");
        System.out.println("   DAY 08: I/O, HTTP CLIENT & TESTING TOOLKIT     ");
        System.out.println("==================================================");

        // 1. File Writing and Reading via NIO.2
        System.out.println("1. Modern Java NIO File I/O:");
        Path tempFile = Path.of("sample-system-prompt.txt");
        String samplePrompt = """
            [SYSTEM RULES]
            1. You are an enterprise AI assistant.
            2. Never hallucinate facts or credentials.
            3. Return output strictly in JSON.
            """;

        PromptFileManager.saveGeneratedChunk(tempFile, samplePrompt);
        System.out.println("   Saved prompt to: " + tempFile.toAbsolutePath());

        String readBack = PromptFileManager.loadPromptTemplate(tempFile);
        System.out.println("   Loaded Prompt back:\n" + readBack.indent(5));

        Optional<String> validated = PromptFileReader.readValidPrompt(tempFile);
        System.out.println("   Prompt validation check (>= 5 words): " + (validated.isPresent() ? "VALID" : "INVALID"));

        // Clean up temp file
        Files.deleteIfExists(tempFile);
        System.out.println();

        // 2. HttpClient Probe
        System.out.println("2. HTTP Client Local Service Probe:");
        boolean isLocalOllamaAlive = ServiceHealthChecker.isServiceAlive("http://localhost:11434");
        System.out.println("   Ollama Local Server Live? " + (isLocalOllamaAlive ? "ONLINE" : "OFFLINE (Start via docker-compose up)"));
        System.out.println();

        // 3. Isolated Mock-Style Summarizer Testing
        System.out.println("3. Simulated Deterministic AI Service Execution:");
        AISummarizerService summarizer = new AISummarizerService(prompt -> {
            // Simulated mock response
            return "Java 21 delivers Virtual Threads and modern syntax.";
        });

        String summary = summarizer.summarize("Java 21 introduced virtual threads for high concurrency.");
        System.out.println("   Summarizer Result: " + summary);
        System.out.println("==================================================");
    }
}
