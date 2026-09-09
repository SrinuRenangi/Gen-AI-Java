package code;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Enterprise Server-Sent Events (SSE) Stream Emitter.
 *
 * Implements the official W3C / WHATWG text/event-stream wire protocol:
 * - Format:
 *     id: <eventId>\n
 *     event: <eventType>\n
 *     data: <jsonPayload>\n\n
 * - Comments/Heartbeats:
 *     : <commentText>\n\n
 *
 * Thread-safety: Uses AtomicBoolean to prevent writes to closed/broken client sockets.
 */
public class SseTokenStreamer {

    private final OutputStream outputStream;
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private Runnable onCompletionCallback;
    private Runnable onTimeoutCallback;

    public SseTokenStreamer(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void onCompletion(Runnable callback) {
        this.onCompletionCallback = callback;
    }

    public void onTimeout(Runnable callback) {
        this.onTimeoutCallback = callback;
    }

    /**
     * Sends a named event with an ID and data payload.
     */
    public synchronized void send(String eventName, String eventId, String data) throws IOException {
        if (isClosed.get()) {
            throw new IOException("Cannot send event: SSE stream is already closed or disconnected");
        }

        StringBuilder frame = new StringBuilder();
        if (eventId != null) {
            frame.append("id: ").append(eventId).append("\n");
        }
        if (eventName != null) {
            frame.append("event: ").append(eventName).append("\n");
        }
        frame.append("data: ").append(data).append("\n\n");

        byte[] bytes = frame.toString().getBytes(StandardCharsets.UTF_8);
        outputStream.write(bytes);
        outputStream.flush(); // Crucial: Flush immediately so browser receives token in real-time!
    }

    /**
     * Sends an SSE comment / heartbeat ping.
     * Keeps AWS ALB, NGINX, and Cloudflare reverse proxies from timing out during slow LLM reasoning.
     */
    public synchronized void sendHeartbeat(String comment) throws IOException {
        if (isClosed.get()) return;

        String frame = ": " + (comment != null ? comment : "ping") + "\n\n";
        outputStream.write(frame.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }

    /**
     * Gracefully completes the stream.
     */
    public void complete() {
        if (isClosed.compareAndSet(false, true)) {
            if (onCompletionCallback != null) {
                onCompletionCallback.run();
            }
        }
    }

    /**
     * Triggers client disconnection / cancellation.
     */
    public void disconnect() {
        if (isClosed.compareAndSet(false, true)) {
            if (onTimeoutCallback != null) {
                onTimeoutCallback.run();
            }
        }
    }

    public boolean isClosed() {
        return isClosed.get();
    }
}
