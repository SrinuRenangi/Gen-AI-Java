package com.genai.security.apisec;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Thread-safe registry maintaining rate limiting buckets per client key (IP, User ID, or API Key).
 */
public class RateLimiterRegistry {

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final Supplier<TokenBucket> bucketFactory;

    public RateLimiterRegistry(Supplier<TokenBucket> bucketFactory) {
        this.bucketFactory = bucketFactory;
    }

    public TokenBucket resolveBucket(String key) {
        return buckets.computeIfAbsent(key, k -> bucketFactory.get());
    }

    public void clear() {
        buckets.clear();
    }
}
