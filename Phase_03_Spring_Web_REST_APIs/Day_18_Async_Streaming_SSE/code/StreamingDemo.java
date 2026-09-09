package code;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Driver class demonstrating Day 18: Async APIs, Streaming & Server-Sent Events (SSE).
 *
 * Demonstrates:
 * 1. Full text/event-stream wire format emission.
 * 2. Virtual Thread asynchronous task delegation.
 * 3. Reverse-proxy keep-alive heartbeats (: ping).
 * 4. Early client cancellation and socket disconnect handling.
 */
public class StreamingDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("================================================================================");
        System.out.println(" DAY 18: ASYNC APIS, STREAMING & SERVER-SENT EVENTS (SSE) IN SPRING BOOT        ");
        System.out.println("================================================================================");

        LLMTokenGenerator generator = new LLMTokenGenerator();

        // -------------------------------------------------------------------------
        // SCENARIO 1: Real-Time ChatGPT Typewriter Stream via SSE
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 1: Real-Time ChatGPT Typewriter Stream (text/event-stream) ---");
        ByteArrayOutputStream buffer1 = new ByteArrayOutputStream();
        SseTokenStreamer streamer1 = new SseTokenStreamer(buffer1);

        streamer1.onCompletion(() -> System.out.println(" [CALLBACK] SseEmitter completed successfully."));

        List<String> answerTokens = List.of(
            "Java ", "21 ", "Virtual ", "Threads ", "allow ", "servers ",
            "to ", "handle ", "millions ", "of ", "concurrent ", "SSE ",
            "connections ", "with ", "minimal ", "RAM."
        );

        CountDownLatch latch1 = new CountDownLatch(1);

        // Run on Virtual Thread (Project Loom)
        Thread.startVirtualThread(() -> {
            try {
                generator.streamCompletion(
                    "gpt-4o",
                    "Explain Virtual Threads for SSE",
                    answerTokens,
                    20,    // 20ms per token
                    false, // No pause
                    streamer1
                );
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch1.countDown();
            }
        });

        latch1.await(5, TimeUnit.SECONDS);

        System.out.println(" Wire Format Received by Browser (Content-Type: text/event-stream):");
        System.out.println("-----------------------------------------------------------------");
        System.out.print(buffer1.toString(StandardCharsets.UTF_8));
        System.out.println("-----------------------------------------------------------------");

        // -------------------------------------------------------------------------
        // SCENARIO 2: Deep Reasoning with Keep-Alive Heartbeats (: ping)
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 2: Deep Reasoning Model with Keep-Alive Heartbeats ---");
        ByteArrayOutputStream buffer2 = new ByteArrayOutputStream();
        SseTokenStreamer streamer2 = new SseTokenStreamer(buffer2);

        List<String> reasoningTokens = List.of("Conclusion:", " P", " != ", "NP.");
        CountDownLatch latch2 = new CountDownLatch(1);

        Thread.startVirtualThread(() -> {
            try {
                generator.streamCompletion(
                    "deepseek-r1",
                    "Prove P != NP",
                    reasoningTokens,
                    30,
                    true, // Simulate reasoning pause with heartbeats
                    streamer2
                );
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch2.countDown();
            }
        });

        latch2.await(5, TimeUnit.SECONDS);
        System.out.println(" Wire Output with Heartbeat Comments (Keeps ALB/Cloudflare alive):");
        System.out.println("-----------------------------------------------------------------");
        System.out.print(buffer2.toString(StandardCharsets.UTF_8));
        System.out.println("-----------------------------------------------------------------");

        // -------------------------------------------------------------------------
        // SCENARIO 3: Client Disconnect / Early Abort
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 3: Client Tab Closed Mid-Stream (Early Cancellation) ---");
        ByteArrayOutputStream buffer3 = new ByteArrayOutputStream();
        SseTokenStreamer streamer3 = new SseTokenStreamer(buffer3);

        streamer3.onTimeout(() -> System.out.println(" [CALLBACK] Client disconnected. Freeing LLM worker thread."));

        CountDownLatch latch3 = new CountDownLatch(1);

        Thread.startVirtualThread(() -> {
            try {
                // Simulate client disconnecting after 3 tokens
                Thread.sleep(60);
                System.out.println(" [CLIENT] Browser closed tab. Sending abort signal...");
                streamer3.disconnect();
            } catch (InterruptedException ignored) {
            }
        });

        Thread.startVirtualThread(() -> {
            try {
                generator.streamCompletion(
                    "claude-3-5-sonnet",
                    "Generate a 500-page novel",
                    List.of("Once ", "upon ", "a ", "time ", "in ", "a ", "galaxy ", "far ", "away..."),
                    40,
                    false,
                    streamer3
                );
            } catch (Exception e) {
                System.out.println("  [EXCEPTION CAUGHT] " + e.getMessage());
            } finally {
                latch3.countDown();
            }
        });

        latch3.await(5, TimeUnit.SECONDS);

        System.out.println("\n================================================================================");
        System.out.println(" DAY 18 DEMONSTRATION COMPLETE: ALL SSE STREAMING PATTERNS OPERATIONAL!         ");
        System.out.println("================================================================================");
    }
}
