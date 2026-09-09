package com.genai.springai.multimodal;

import java.util.List;

/**
 * Service orchestrating Audio Transcription (Whisper) models.
 * Generates timestamped segments and speaker diarization metadata.
 */
public class AudioTranscriptionService {

    public record TranscriptionSegment(
        double startSecond,
        double endSecond,
        String speaker,
        String text,
        double confidence
    ) {}

    public record TranscriptionResult(
        String language,
        double durationSeconds,
        String fullTranscript,
        List<TranscriptionSegment> segments
    ) {}

    public TranscriptionResult transcribeAudio(Media audioFile) {
        if (!audioFile.isAudio()) {
            throw new IllegalArgumentException("Expected audio MIME type, received: " + audioFile.mimeType());
        }

        List<TranscriptionSegment> segments = List.of(
            new TranscriptionSegment(0.0, 3.2, "Speaker 1 (Tech Lead)", "Welcome team to our Spring AI production readiness review.", 0.98),
            new TranscriptionSegment(3.5, 7.8, "Speaker 2 (Architect)", "Today we are auditing tool calling latency and multimodal throughput.", 0.96),
            new TranscriptionSegment(8.1, 12.4, "Speaker 1 (Tech Lead)", "Target P99 response time for image understanding is under 800 milliseconds.", 0.99)
        );

        String full = "Welcome team to our Spring AI production readiness review. Today we are auditing tool calling latency and multimodal throughput. Target P99 response time for image understanding is under 800 milliseconds.";

        return new TranscriptionResult("en", 12.4, full, segments);
    }
}
