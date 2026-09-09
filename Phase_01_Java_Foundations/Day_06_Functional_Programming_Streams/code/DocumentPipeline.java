package com.javagenai.day06;

import java.util.List;

public class DocumentPipeline {

    public static List<String> processDocuments(List<String> rawTexts) {
        return rawTexts.stream()
            .filter(text -> text != null && !text.isBlank())
            .map(String::trim)
            .filter(text -> text.length() >= 20)
            .map(String::toUpperCase)
            .limit(3)
            .toList();
    }
}
