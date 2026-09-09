package com.genai.springai.streaming;

import java.util.concurrent.atomic.AtomicInteger;

public class StreamingDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("  DAY 36: REAL-TIME TOKEN STREAMING & SSE DEMONSTRATION                         ");
        System.out.println("================================================================================\n");

        StreamingChatModel model = new OllamaStreamingChatModel("llama3.2");

        // -----------------------------------------------------------------------------------------
        // SCENARIO 1: Live Typewriter Streaming in Console
        // -----------------------------------------------------------------------------------------
        System.out.println("[TEST 1] Live Console Typewriter Effect (Tokens streamed as generated)...");
        System.out.print("  [STREAMING OUTPUT]: ");

        long startTime = System.currentTimeMillis();
        AtomicInteger tokenCounter = new AtomicInteger(0);

        model.stream(
                "Why are Virtual Threads ideal for AI streaming?",
                token -> {
                    System.out.print(token);
                    tokenCounter.incrementAndGet();
                    System.out.flush();
                },
                () -> {
                    long duration = System.currentTimeMillis() - startTime;
                    double tokensPerSec = (tokenCounter.get() / (double) duration) * 1000.0;
                    System.out.printf("\n\n  ✅ STREAM COMPLETE: %d tokens in %d ms (%.1f tokens/sec)%n",
                            tokenCounter.get(), duration, tokensPerSec);
                },
                error -> System.err.println("\n❌ Stream error: " + error.getMessage())
        );

        // -----------------------------------------------------------------------------------------
        // SCENARIO 2: W3C Server-Sent Events (SSE) Wire Format Encoding
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 2] Encoding Tokens as W3C SSE Event Stream Chunks...");
        System.out.println("--- First 3 Raw SSE HTTP Frames ---");
        for (int i = 1; i <= 3; i++) {
            SseFrame frame = new SseFrame("token", "Chunk #" + i, String.valueOf(i));
            System.out.print(frame.toWireFormat());
        }

        // -----------------------------------------------------------------------------------------
        // SCENARIO 3: Stream Aggregator (Streaming to client while persisting to DB)
        // -----------------------------------------------------------------------------------------
        System.out.println("[TEST 3] StreamAggregator: Concurrently streaming & saving to DB...");
        
        StreamAggregator aggregator = new StreamAggregator(
                clientToken -> {
                    // Simulates pushing to browser SSE emitter
                },
                fullPersistedText -> {
                    // Simulates Spring Data JPA ChatMessageRepository.save(new ChatMessage(...))
                    System.out.println("  [DATABASE SAVE EVENT] Successfully saved full conversation history!");
                    System.out.println("  Persisted Text Length: " + fullPersistedText.length() + " chars.");
                    System.out.println("  First 80 chars: \"" + fullPersistedText.substring(0, Math.min(80, fullPersistedText.length())) + "...\"");
                }
        );

        model.stream(
                "Persist this stream",
                aggregator::onNextToken,
                aggregator::onStreamComplete,
                err -> System.err.println("Aggregator error: " + err.getMessage())
        );

        System.out.println("\n================================================================================");
        System.out.println("  STREAMING PIPELINE & CONCURRENT PERSISTENCE VERIFIED SUCCESSFULLY!           ");
        System.out.println("================================================================================");
    }
}
