package com.genai.springai.multimodal;

import java.util.Collections;
import java.util.List;

/**
 * Represents a multimodal user message combining textual instructions with media assets.
 */
public record MultimodalMessage(
    String textPrompt,
    List<Media> mediaList
) {
    public MultimodalMessage {
        mediaList = (mediaList == null) ? List.of() : Collections.unmodifiableList(mediaList);
    }

    public static MultimodalMessage of(String prompt, Media... media) {
        return new MultimodalMessage(prompt, List.of(media));
    }
}
