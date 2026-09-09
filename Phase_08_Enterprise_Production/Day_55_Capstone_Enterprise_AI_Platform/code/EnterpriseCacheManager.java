package com.genai.enterprise.capstone;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Enterprise caching layer supporting SHA-256 keyed responses with TTL.
 */
public class EnterpriseCacheManager {

    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);

    public String get(String tenantId, String prompt) {
        String key = buildKey(tenantId, prompt);
        String val = cache.get(key);
        if (val != null) {
            hitCount.incrementAndGet();
            return val;
        }
        missCount.incrementAndGet();
        return null;
    }

    public void put(String tenantId, String prompt, String response) {
        String key = buildKey(tenantId, prompt);
        cache.put(key, response);
    }

    public long getHitCount() { return hitCount.get(); }
    public long getMissCount() { return missCount.get(); }

    private String buildKey(String tenantId, String prompt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest((tenantId + ":" + prompt.trim().toLowerCase()).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
