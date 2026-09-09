package com.javagenai.day08;

import java.util.function.Function;

public class AISummarizerService {
    private final Function<String, String> llmFunction;

    public AISummarizerService(Function<String, String> llmFunction) {
        this.llmFunction = llmFunction;
    }

    public String summarize(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw new IllegalArgumentException("Cannot summarize empty text");
        }
        String prompt = "Summarize the following text in 1 sentence: " + rawText;
        return this.llmFunction.apply(prompt);
    }
}
