package com.genai.springai.streaming;

/**
 * Encapsulates a W3C compliant Server-Sent Events (SSE) data frame.
 */
public record SseFrame(
        String event,
        String data,
        String id
) {
    public SseFrame(String data) {
        this("message", data, null);
    }

    public String toWireFormat() {
        StringBuilder sb = new StringBuilder();
        if (id != null) {
            sb.append("id: ").append(id).append("\n");
        }
        if (event != null) {
            sb.append("event: ").append(event).append("\n");
        }
        sb.append("data: ").append(data.replace("\n", "\\n")).append("\n\n");
        return sb.toString();
    }
}
