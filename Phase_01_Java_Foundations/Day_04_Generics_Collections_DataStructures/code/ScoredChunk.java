package com.javagenai.day04;

public record ScoredChunk(String text, double similarityScore) implements Comparable<ScoredChunk> {
    @Override
    public int compareTo(ScoredChunk other) {
        return Double.compare(this.similarityScore, other.similarityScore);
    }
}
