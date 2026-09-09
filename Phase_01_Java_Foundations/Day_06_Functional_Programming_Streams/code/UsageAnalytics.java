package com.javagenai.day06;

import java.util.*;
import java.util.stream.Collectors;

public class UsageAnalytics {

    public static Map<String, Double> calculateCostPerModel(List<LLMRecord> records) {
        return records.stream()
            .collect(Collectors.groupingBy(
                LLMRecord::model,
                Collectors.summingDouble(record -> {
                    double ratePerMillion = "gpt-4o".equalsIgnoreCase(record.model()) ? 2.50 : 0.0;
                    return (record.totalTokens() / 1_000_000.0) * ratePerMillion;
                })
            ));
    }
}
