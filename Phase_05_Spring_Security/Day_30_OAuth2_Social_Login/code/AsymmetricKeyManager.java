package com.genai.security.oauth2;

import java.security.*;
import java.util.Base64;

/**
 * Generates RSA-2048 key pairs simulating an OAuth2 Identity Provider (Google / Keycloak / Okta).
 * The Authorization Server keeps the Private Key to SIGN tokens.
 * The Resource Server (Spring Boot AI app) uses only the Public Key to VERIFY tokens.
 */
public final class AsymmetricKeyManager {

    private final KeyPair keyPair;
    private final String keyId;

    public AsymmetricKeyManager(String keyId) {
        this.keyId = keyId;
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            this.keyPair = keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("RSA algorithm not supported", e);
        }
    }

    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    public String getKeyId() {
        return keyId;
    }

    public String getPublicKeyBase64() {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }
}
