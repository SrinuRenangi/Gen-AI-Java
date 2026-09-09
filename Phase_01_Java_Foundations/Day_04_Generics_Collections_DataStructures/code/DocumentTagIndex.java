package com.javagenai.day04;

import java.util.*;

public class DocumentTagIndex {
    private final Map<String, Set<String>> index = new HashMap<>();

    public void addDocument(String docId, List<String> tags) {
        for (String tag : tags) {
            index.computeIfAbsent(tag.toLowerCase(), k -> new HashSet<>()).add(docId);
        }
    }

    public Set<String> findDocumentsByTag(String tag) {
        return index.getOrDefault(tag.toLowerCase(), Collections.emptySet());
    }

    public int getTagCount() {
        return index.size();
    }
}
