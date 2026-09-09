package code;

import java.io.IOException;
import java.util.List;

/**
 * Simulates an Auto-Regressive Large Language Model inference stream.
 *
 * Emits tokens one-by-one with realistic latencies (30ms - 80ms per token),
 * simulating GPU autoregressive decoding, plus heartbeat pings during deep reasoning.
 */
public class LLMTokenGenerator {

    /**
     * Streams simulated completion tokens to the provided SSE emitter.
     */
    public void streamCompletion(
        String modelName,
        String prompt,
        List<String> tokens,
        long tokenDelayMs,
        boolean simulateReasoningPause,
        SseTokenStreamer streamer
    ) throws IOException, InterruptedException {

        // 1. Initial event: Handshake & metadata
        streamer.send("start", "evt_000", "{\"model\":\"" + modelName + "\",\"status\":\"generating\"}");

        // 2. Simulated deep reasoning pause (e.g., DeepSeek-R1 / OpenAI o1)
        if (simulateReasoningPause) {
            // Send heartbeat pings while thinking so reverse proxies don't terminate the socket
            for (int p = 1; p <= 2; p++) {
                Thread.sleep(100);
                streamer.sendHeartbeat("thinking-phase-heartbeat-" + p);
            }
        }

        // 3. Auto-regressive token streaming
        int tokenIndex = 0;
        for (String token : tokens) {
            if (streamer.isClosed()) {
                System.out.println("  [LLM STREAM] Streamer closed by client. Aborting token generation.");
                return;
            }

            tokenIndex++;
            String eventId = "tok_" + String.format("%03d", tokenIndex);
            String payload = "{\"token\":\"" + escapeJson(token) + "\",\"index\":" + tokenIndex + "}";

            streamer.send("token", eventId, payload);

            if (tokenDelayMs > 0) {
                Thread.sleep(tokenDelayMs);
            }
        }

        // 4. Final completion event: Token usage and finishReason
        String donePayload = "{\"finishReason\":\"stop\",\"totalTokens\":" + tokenIndex + "}";
        streamer.send("done", "evt_done", donePayload);

        streamer.complete();
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
