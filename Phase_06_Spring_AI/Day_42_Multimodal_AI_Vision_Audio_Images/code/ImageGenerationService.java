package com.genai.springai.multimodal;

import java.net.URI;
import java.time.Instant;

/**
 * Service simulating Spring AI's ImageModel (DALL-E 3 / Stability AI).
 */
public class ImageGenerationService {

    public record ImageGenerationOptions(
        String model,
        int width,
        int height,
        String style,
        String responseFormat
    ) {
        public static ImageGenerationOptions defaults() {
            return new ImageGenerationOptions("dall-e-3", 1024, 1024, "vivid", "url");
        }
    }

    public record GeneratedImage(
        String revisedPrompt,
        URI imageUrl,
        Instant createdTimestamp,
        int width,
        int height
    ) {}

    public GeneratedImage generateImage(String userPrompt, ImageGenerationOptions options) {
        String revised = "An ultra-detailed, cinematic isometric diagram of a high-tech Java enterprise datacenter with glowing neural connections, 8k resolution, photorealistic style: " + userPrompt;
        URI generatedUri = URI.create("https://cloud-storage.internal.acme.com/generated-assets/img-2026-ai-" + System.currentTimeMillis() + ".png");

        return new GeneratedImage(
            revised,
            generatedUri,
            Instant.now(),
            options.width(),
            options.height()
        );
    }
}
