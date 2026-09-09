package com.genai.enterprise.costopt;

/**
 * Enterprise Token Bucket Rate Limiter supporting dual constraints:
 * 1. Requests Per Minute (RPM)
 * 2. Tokens Per Minute (TPM)
 */
public class TokenBucketRateLimiter {

    private final long maxRpm;
    private final long maxTpm;

    private double availableRequests;
    private double availableTokens;
    private long lastRefillEpochMs;

    public record RateLimitResult(boolean allowed, String reason, long retryAfterMs) {}

    public TokenBucketRateLimiter(long maxRpm, long maxTpm) {
        this.maxRpm = maxRpm;
        this.maxTpm = maxTpm;
        this.availableRequests = maxRpm;
        this.availableTokens = maxTpm;
        this.lastRefillEpochMs = System.currentTimeMillis();
    }

    public synchronized RateLimitResult tryAcquire(int estimatedTokens) {
        refill();

        if (availableRequests < 1.0) {
            long waitMs = (long) ((1.0 - availableRequests) / (maxRpm / 60000.0));
            return new RateLimitResult(false, "RPM_EXCEEDED: Maximum " + maxRpm + " requests/min reached", Math.max(waitMs, 100));
        }

        if (availableTokens < estimatedTokens) {
            long waitMs = (long) ((estimatedTokens - availableTokens) / (maxTpm / 60000.0));
            return new RateLimitResult(false, "TPM_EXCEEDED: Requested " + estimatedTokens + " tokens exceeds remaining budget", Math.max(waitMs, 100));
        }

        availableRequests -= 1.0;
        availableTokens -= estimatedTokens;
        return new RateLimitResult(true, "ALLOWED", 0);
    }

    private void refill() {
        long now = System.currentTimeMillis();
        long elapsedMs = now - lastRefillEpochMs;
        if (elapsedMs <= 0) return;

        // Refill requests per millisecond: maxRpm / 60,000
        double requestsToAdd = elapsedMs * (maxRpm / 60000.0);
        availableRequests = Math.min(maxRpm, availableRequests + requestsToAdd);

        // Refill tokens per millisecond: maxTpm / 60,000
        double tokensToAdd = elapsedMs * (maxTpm / 60000.0);
        availableTokens = Math.min(maxTpm, availableTokens + tokensToAdd);

        lastRefillEpochMs = now;
    }

    public synchronized double getAvailableRequests() { return availableRequests; }
    public synchronized double getAvailableTokens() { return availableTokens; }
}
