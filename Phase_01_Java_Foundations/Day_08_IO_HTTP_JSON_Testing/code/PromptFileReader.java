package com.javagenai.day08;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class PromptFileReader {

    public static Optional<String> readValidPrompt(Path path) {
        try {
            if (!Files.exists(path)) return Optional.empty();
            String content = Files.readString(path).trim();
            String[] words = content.split("\\s+");
            if (words.length >= 5) {
                return Optional.of(content);
            }
            return Optional.empty();
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
