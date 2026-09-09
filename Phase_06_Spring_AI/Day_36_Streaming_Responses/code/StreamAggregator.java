package com.genai.springai.streaming;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Handles the dual responsibility of:
 * 1. Forwarding tokens immediately to the client in real-time.
 * 2. Concurrently accumulating the full string in memory for database persistence upon completion.
 */
public class StreamAggregator {

    private final StringBuilder buffer = new StringBuilder();
    private final AtomicLong tokenCount = new AtomicLong(0);
    private final Consumer<String> downstreamClient;
    private final Consumer<String> onFullTextAccumulated;

    public StreamAggregator(
            Consumer<String> downstreamClient,
            Consumer<String> onFullTextAccumulated
    ) {
        this.downstreamClient = downstreamClient;
        this.onFullTextAccumulated = onFullTextAccumulated;
    }

    public synchronized void onNextToken(String token) {
        buffer.append(token);
        tokenCount.incrementAndGet();
        downstreamClient.accept(token);
    }

    public synchronized void onStreamComplete() {
        String fullText = buffer.toString();
        onFullTextAccumulated.accept(fullText);
    }

    public long getTokenCount() {
        return tokenCount.get();
    }

    public String getAccumulatedText() {
        return buffer.toString();
    }
}
