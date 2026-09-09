package com.genai.springai.multimodal;

import java.net.URI;
import java.util.Objects;

/**
 * Encapsulates multimodal media (images, audio, documents) passed to models.
 * Corresponds to org.springframework.ai.model.Media in Spring AI.
 */
public record Media(
    String mimeType,
    byte[] data,
    URI uri,
    String filename
) {
    public static Media ofImage(String mimeType, byte[] data, String filename) {
        return new Media(mimeType, Objects.requireNonNull(data), null, filename);
    }

    public static Media ofAudio(String mimeType, byte[] data, String filename) {
        return new Media(mimeType, Objects.requireNonNull(data), null, filename);
    }

    public static Media ofRemoteUri(String mimeType, URI uri) {
        return new Media(mimeType, new byte[0], Objects.requireNonNull(uri), uri.getPath());
    }

    public boolean isImage() {
        return mimeType != null && mimeType.startsWith("image/");
    }

    public boolean isAudio() {
        return mimeType != null && mimeType.startsWith("audio/");
    }
}
