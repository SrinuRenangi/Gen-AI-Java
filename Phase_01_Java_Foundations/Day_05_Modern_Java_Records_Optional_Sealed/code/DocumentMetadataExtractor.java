package com.javagenai.day05;

import java.util.Map;
import java.util.Optional;

public class DocumentMetadataExtractor {

    public static Optional<String> extractAuthorDomain(Map<String, String> metadata) {
        return Optional.ofNullable(metadata)
            .map(m -> m.get("author_email"))
            .filter(email -> email.contains("@"))
            .map(email -> email.substring(email.indexOf("@") + 1).toLowerCase().trim());
    }
}
