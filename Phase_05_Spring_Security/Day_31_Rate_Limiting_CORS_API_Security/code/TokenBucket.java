package com.genai.security.apisec;

/**
 * Thread-safe Token Bucket rate limiter implementing the Bucket4j algorithm.
 * Tokens accumulate at refillRatePerSecond up to capacity (burst size).
 * Consuming a token allows an API request through. If bucket is empty, returns false.
 */
public class TokenBucket {

    private final long capacity;
    private final double refillTokensPerNano;
    private double availableTokens;
    private long lastRefillNanos;

    public TokenBucket(long capacity, long refillTokensPerSecond) {
        if (capacity <= 0 || refillTokensPerSecond <= 0) {
            throw new IllegalArgumentException("Capacity and refill rate must be positive");
        }
        this.capacity = capacity;
        this.refillTokensPerNano = (double) refillTokensPerSecond / 1_000_000_000.0;
        this.availableTokens = capacity;
        this.lastRefillNanos = System.nanoTime();
    }

    public synchronized boolean tryConsume(long tokens) {
        refill();
        if (availableTokens >= tokens) {
            availableTokens -= tokens;
            return true;
        }
        return false;
    }

    public synchronized long getAvailableTokens() {
        refill();
        return (long) availableTokens;
    }

    public synchronized long getSecondsUntilNextToken() {
        refill();
        if (availableTokens >= 1.0) {
            return 0;
        }
        double missingTokens = 1.0 - availableTokens;
        double nanosNeeded = missingTokens / refillTokensPerNano;
        return Math.max(1, (long) Math.ceil(nanosNeeded / 1_000_000_000.0));
    }

    private void refill() {
        long now = System.nanoTime();
        long elapsedNanos = now - lastRefillNanos;
        if (elapsedNanos > 0) {
            double newlyGenerated = elapsedNanos * refillTokensPerNano;
            availableTokens = Math.min(capacity, availableTokens + newlyGenerated);
            lastRefillNanos = now;
        }
    }

    public long getCapacity() {
        return capacity;
    }
}
