package com.javagenai.day15;

import java.time.Instant;

public record PromptTemplate(
    String id,
    String title,
    String category,
    String templateText,
    double defaultTemperature,
    Instant createdAt
) {}
