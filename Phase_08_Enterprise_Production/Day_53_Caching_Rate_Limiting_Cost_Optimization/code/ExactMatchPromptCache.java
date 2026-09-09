package com.genai.enterprise.costopt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Exact-match prompt cache using cryptographic SHA-256 hash keys and TTL expiry.
 */
public class ExactMatchPromptCache {

    public record CacheEntry(String response, long expiresAtMs) {}

    private final Map<String, CacheEntry> store = new ConcurrentHashMap<>();
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    private final long defaultTtlMs;

    public ExactMatchPromptCache(long defaultTtlMs) {
        this.defaultTtlMs = defaultTtlMs;
    }

    public String get(String prompt) {
        String key = hashPrompt(prompt);
        CacheEntry entry = store.get(key);
        if (entry != null) {
            if (System.currentTimeMillis() < entry.expiresAtMs()) {
                hits.incrementAndGet();
                return entry.response();
            } else {
                store.remove(key); // expired
            }
        }
        misses.incrementAndGet();
        return null;
    }

    public void put(String prompt, String response) {
        put(prompt, response, defaultTtlMs);
    }

    public void put(String prompt, String response, long ttlMs) {
        String key = hashPrompt(prompt);
        store.put(key, new CacheEntry(response, System.currentTimeMillis() + ttlMs));
    }

    public long getHits() { return hits.get(); }
    public long getMisses() { return misses.get(); }

    private String hashPrompt(String prompt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(prompt.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not supported", e);
        }
    }
}
