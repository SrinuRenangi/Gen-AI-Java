package com.genai.security.oauth2;

import java.security.PublicKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Simulates the Identity Provider's JWKS (JSON Web Key Set) endpoint:
 * https://idp.example.com/.well-known/jwks.json
 * Spring Security's NimbusJwtDecoder calls this endpoint once on startup to cache public keys.
 */
public class JwksSimulator {

    private final Map<String, PublicKey> publicKeys = new HashMap<>();

    public void registerKey(String keyId, PublicKey publicKey) {
        publicKeys.put(keyId, publicKey);
    }

    public PublicKey getKey(String keyId) {
        return publicKeys.get(keyId);
    }

    public Map<String, PublicKey> getAllKeys() {
        return Collections.unmodifiableMap(publicKeys);
    }
}
