package com.genai.springai.streaming;

import java.util.function.Consumer;

/**
 * Simulates Spring AI's streaming chat model (e.g. Ollama or OpenAI streaming).
 * Streams tokens at realistic generation speeds (30 tokens/sec).
 */
public class OllamaStreamingChatModel implements StreamingChatModel {

    private final String modelName;

    public OllamaStreamingChatModel(String modelName) {
        this.modelName = modelName;
    }

    public OllamaStreamingChatModel() {
        this("llama3.2");
    }

    @Override
    public void stream(
            String prompt,
            Consumer<String> onToken,
            Runnable onComplete,
            Consumer<Throwable> onError
    ) {
        String generatedContent = getSampleGenerationForPrompt(prompt);
        String[] tokens = generatedContent.split("(?<=\\s)|(?=[.,!?:;])");

        try {
            for (String token : tokens) {
                onToken.accept(token);
                // Simulate autoregressive token generation latency (~25ms per token)
                Thread.sleep(25);
            }
            onComplete.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            onError.accept(new RuntimeException("Streaming was interrupted", e));
        } catch (Exception ex) {
            onError.accept(ex);
        }
    }

    private String getSampleGenerationForPrompt(String prompt) {
        return "Java 21 Virtual Threads combined with Spring AI streaming revolutionizes modern LLM applications. "
                + "Instead of blocking heavy operating system threads while waiting for token generation, "
                + "lightweight Virtual Threads yield their carrier thread, allowing a single JVM node "
                + "to effortlessly sustain tens of thousands of concurrent Server-Sent Event (SSE) connections!";
    }
}
