package com.javagenai.day08;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class PromptFileManager {

    public static String loadPromptTemplate(Path filePath) throws IOException {
        return Files.readString(filePath);
    }

    public static void saveGeneratedChunk(Path outputPath, String content) throws IOException {
        Files.writeString(outputPath, content, 
                          StandardOpenOption.CREATE, 
                          StandardOpenOption.TRUNCATE_EXISTING);
    }
}
