package com.genai.springai.multimodal;

/**
 * Executable demonstration of Day 42:
 * Multimodal AI - Vision, Audio, and Image Generation.
 */
public class MultimodalAiDemo {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("  DAY 42: SPRING AI MULTIMODAL AI (VISION, AUDIO & IMAGES) DEMO   ");
        System.out.println("==================================================================");

        VisionAnalysisService visionService = new VisionAnalysisService();
        AudioTranscriptionService audioService = new AudioTranscriptionService();
        ImageGenerationService imageService = new ImageGenerationService();

        // 1. VISION: Document & Receipt OCR with Structured Data Extraction
        System.out.println("\n--- TASK 1: Vision Model Document / Invoice OCR ---");
        byte[] simulatedReceiptBytes = new byte[]{0x12, 0x34, 0x56}; // simulated JPEG payload
        Media invoiceMedia = Media.ofImage("image/jpeg", simulatedReceiptBytes, "datacenter-invoice.jpg");

        var invoice = visionService.extractInvoice(invoiceMedia);
        System.out.println("Merchant:       " + invoice.merchant());
        System.out.println("Invoice Number: " + invoice.invoiceNumber());
        System.out.println("Date:           " + invoice.date());
        System.out.printf("Total Amount:   $%.2f (Subtotal: $%.2f, Tax: $%.2f)\n", invoice.total(), invoice.subtotal(), invoice.tax());
        System.out.println("Line Items Extracted:");
        for (var item : invoice.lineItems()) {
            System.out.printf("   - %s x%d @ $%.2f = $%.2f\n", item.description(), item.quantity(), item.unitPrice(), item.lineTotal());
        }

        // 2. VISION: Technical Architecture Diagram Inspection
        System.out.println("\n--- TASK 2: Vision Model Technical Diagram Inspection ---");
        byte[] simulatedDiagramBytes = new byte[]{0x42, 0x42, 0x42};
        Media diagramMedia = Media.ofImage("image/png", simulatedDiagramBytes, "system-architecture.png");

        var diagramReport = visionService.analyzeArchitectureDiagram(diagramMedia);
        System.out.println("Detected Architecture: " + diagramReport.architecturalPattern());
        System.out.println("Identified Components: " + String.join(", ", diagramReport.detectedComponents()));
        System.out.println("Potential Bottlenecks:");
        for (String b : diagramReport.potentialBottlenecks()) {
            System.out.println("   ⚠️ " + b);
        }
        System.out.println("Recommendation:        " + diagramReport.recommendation());

        // 3. AUDIO: Whisper Speech-to-Text Transcription & Diarization
        System.out.println("\n--- TASK 3: Audio Transcription (Whisper Model) ---");
        byte[] simulatedAudioBytes = new byte[]{0x01, 0x02, 0x03, 0x04};
        Media audioMedia = Media.ofAudio("audio/wav", simulatedAudioBytes, "meeting-standup.wav");

        var transcription = audioService.transcribeAudio(audioMedia);
        System.out.println("Language: " + transcription.language() + " | Duration: " + transcription.durationSeconds() + "s");
        System.out.println("Full Transcript:\n\"" + transcription.fullTranscript() + "\"");
        System.out.println("Timestamped Diarized Segments:");
        for (var seg : transcription.segments()) {
            System.out.printf("   [%4.1fs - %4.1fs] %s: \"%s\" (conf: %.2f)\n",
                seg.startSecond(), seg.endSecond(), seg.speaker(), seg.text(), seg.confidence());
        }

        // 4. IMAGE GENERATION: Spring AI ImageModel Synthesis
        System.out.println("\n--- TASK 4: Image Generation (ImageModel / DALL-E 3) ---");
        var options = new ImageGenerationService.ImageGenerationOptions("dall-e-3", 1792, 1024, "vivid", "url");
        var generatedImage = imageService.generateImage("Spring Boot microservice interacting with vector database", options);

        System.out.println("Revised Prompt:    " + generatedImage.revisedPrompt());
        System.out.println("Generated URL:     " + generatedImage.imageUrl());
        System.out.println("Resolution:        " + generatedImage.width() + "x" + generatedImage.height());
        System.out.println("Creation Time:     " + generatedImage.createdTimestamp());

        System.out.println("\n==================================================================");
        System.out.println("  MULTIMODAL AI DEMO COMPLETED SUCCESSFULLY                      ");
        System.out.println("==================================================================");
    }
}
