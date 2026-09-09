package com.genai.security.oauth2;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Instant;
import java.util.*;

/**
 * Simulates Spring Security's OAuth2 Resource Server:
 * - NimbusJwtDecoder
 * - JwtAuthenticationConverter
 */
public class ResourceServerValidator {

    public record AuthenticatedUser(
            String subject,
            String email,
            String name,
            Set<String> authorities,
            Map<String, Object> claims
    ) {}

    private final String expectedIssuer;
    private final String expectedAudience;
    private final JwksSimulator jwks;

    public ResourceServerValidator(String expectedIssuer, String expectedAudience, JwksSimulator jwks) {
        this.expectedIssuer = expectedIssuer;
        this.expectedAudience = expectedAudience;
        this.jwks = jwks;
    }

    public AuthenticatedUser validateAndAuthenticate(String token) {
        if (token == null || !token.contains(".")) {
            throw new SecurityException("Malformed JWT format");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new SecurityException("JWT must contain Header, Payload, and Signature parts");
        }

        Base64.Decoder decoder = Base64.getUrlDecoder();
        String headerJson = new String(decoder.decode(parts[0]), StandardCharsets.UTF_8);
        String payloadJson = new String(decoder.decode(parts[1]), StandardCharsets.UTF_8);
        byte[] signatureBytes = decoder.decode(parts[2]);

        // 1. Extract kid from header
        String kid = extractJsonField(headerJson, "kid");
        if (kid == null) {
            throw new SecurityException("Missing 'kid' (Key ID) in JWT header");
        }

        // 2. Fetch Public Key from JWKS
        PublicKey publicKey = jwks.getKey(kid);
        if (publicKey == null) {
            throw new SecurityException("Untrusted or unknown Key ID: " + kid);
        }

        // 3. Verify RS256 signature using Public Key
        try {
            String signingInput = parts[0] + "." + parts[1];
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey);
            verifier.update(signingInput.getBytes(StandardCharsets.UTF_8));
            if (!verifier.verify(signatureBytes)) {
                throw new SecurityException("Cryptographic RS256 signature verification failed! Token has been tampered with.");
            }
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new SecurityException("Signature verification error", e);
        }

        // 4. Validate Claims
        String issuer = extractJsonField(payloadJson, "iss");
        if (!expectedIssuer.equals(issuer)) {
            throw new SecurityException("Issuer mismatch. Expected: " + expectedIssuer + ", Found: " + issuer);
        }

        String audience = extractJsonField(payloadJson, "aud");
        if (expectedAudience != null && !expectedAudience.equals(audience)) {
            throw new SecurityException("Audience mismatch. Expected: " + expectedAudience + ", Found: " + audience);
        }

        String expStr = extractJsonField(payloadJson, "exp");
        if (expStr != null) {
            long exp = Long.parseLong(expStr);
            long now = Instant.now().getEpochSecond();
            if (now >= exp) {
                throw new SecurityException("Token has expired. Expiration: " + exp + ", Current: " + now);
            }
        }

        // 5. Build GrantedAuthorities (JwtAuthenticationConverter simulation)
        Set<String> authorities = new HashSet<>();
        String scope = extractJsonField(payloadJson, "scope");
        if (scope != null) {
            for (String s : scope.split(" ")) {
                if (!s.isBlank()) {
                    authorities.add("SCOPE_" + s.trim());
                }
            }
        }

        List<String> roles = extractJsonArray(payloadJson, "roles");
        for (String role : roles) {
            authorities.add(role.startsWith("ROLE_") ? role : "ROLE_" + role);
        }

        String sub = extractJsonField(payloadJson, "sub");
        String email = extractJsonField(payloadJson, "email");
        String name = extractJsonField(payloadJson, "name");

        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", issuer);
        claims.put("sub", sub);
        claims.put("aud", audience);

        return new AuthenticatedUser(sub, email, name, Collections.unmodifiableSet(authorities), claims);
    }

    private static String extractJsonField(String json, String field) {
        String pattern = "\"" + field + "\":\"";
        int start = json.indexOf(pattern);
        if (start != -1) {
            start += pattern.length();
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        }
        // numeric/raw field
        pattern = "\"" + field + "\":";
        start = json.indexOf(pattern);
        if (start != -1) {
            start += pattern.length();
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            return json.substring(start, end).trim();
        }
        return null;
    }

    private static List<String> extractJsonArray(String json, String arrayField) {
        String pattern = "\"" + arrayField + "\":[";
        int start = json.indexOf(pattern);
        if (start == -1) return List.of();
        start += pattern.length();
        int end = json.indexOf("]", start);
        if (end == -1) return List.of();
        String content = json.substring(start, end).trim();
        if (content.isEmpty()) return List.of();
        String[] items = content.split(",");
        List<String> result = new ArrayList<>();
        for (String item : items) {
            result.add(item.replace("\"", "").trim());
        }
        return result;
    }
}
