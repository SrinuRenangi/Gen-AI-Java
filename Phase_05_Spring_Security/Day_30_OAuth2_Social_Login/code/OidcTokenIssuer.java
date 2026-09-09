package com.genai.security.oauth2;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

/**
 * Simulates an OAuth2 / OIDC Authorization Server issuing RS256 signed JWTs.
 */
public class OidcTokenIssuer {

    private final String issuerUri;
    private final AsymmetricKeyManager keyManager;

    public OidcTokenIssuer(String issuerUri, AsymmetricKeyManager keyManager) {
        this.issuerUri = issuerUri;
        this.keyManager = keyManager;
    }

    public String issueUserOidcToken(String subject, String email, String name, List<String> roles, long ttlSeconds) {
        long now = Instant.now().getEpochSecond();
        long exp = now + ttlSeconds;

        String headerJson = String.format(
                "{\"alg\":\"RS256\",\"typ\":\"JWT\",\"kid\":\"%s\"}",
                keyManager.getKeyId()
        );

        String rolesJson = "[" + String.join(",", roles.stream().map(r -> "\"" + r + "\"").toList()) + "]";

        String payloadJson = String.format(
                "{\"iss\":\"%s\",\"sub\":\"%s\",\"aud\":\"genai-cloud-api\",\"email\":\"%s\",\"name\":\"%s\",\"iat\":%d,\"exp\":%d,\"roles\":%s}",
                issuerUri, subject, email, name, now, exp, rolesJson
        );

        return signJwt(headerJson, payloadJson, keyManager.getPrivateKey());
    }

    public String issueClientCredentialsToken(String clientId, String scope, long ttlSeconds) {
        long now = Instant.now().getEpochSecond();
        long exp = now + ttlSeconds;

        String headerJson = String.format(
                "{\"alg\":\"RS256\",\"typ\":\"JWT\",\"kid\":\"%s\"}",
                keyManager.getKeyId()
        );

        String payloadJson = String.format(
                "{\"iss\":\"%s\",\"sub\":\"%s\",\"aud\":\"genai-cloud-api\",\"client_id\":\"%s\",\"scope\":\"%s\",\"iat\":%d,\"exp\":%d}",
                issuerUri, clientId, clientId, scope, now, exp
        );

        return signJwt(headerJson, payloadJson, keyManager.getPrivateKey());
    }

    private String signJwt(String headerJson, String payloadJson, PrivateKey privateKey) {
        try {
            Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
            String encodedHeader = encoder.encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
            String encodedPayload = encoder.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            String signingInput = encodedHeader + "." + encodedPayload;

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(signingInput.getBytes(StandardCharsets.UTF_8));

            byte[] signatureBytes = signature.sign();
            String encodedSignature = encoder.encodeToString(signatureBytes);

            return signingInput + "." + encodedSignature;
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign RS256 JWT", e);
        }
    }
}
