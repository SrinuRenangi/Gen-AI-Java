package code;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Enterprise JWT Token Service built on pure Java 21 crypto.
 *
 * Implements:
 * 1. RFC 7519 JSON Web Token specification.
 * 2. HMAC-SHA256 (HS256) cryptographic signature signing and verification.
 * 3. Base64Url encoding without padding.
 * 4. Claims extraction and expiration validation.
 */
public class JwtTokenService {

    private final byte[] secretKey;

    public JwtTokenService(String secretKeyString) {
        if (secretKeyString.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT Secret key must be at least 256 bits (32 bytes) long for HS256");
        }
        this.secretKey = secretKeyString.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Generates a signed compact JWT string: header.payload.signature
     */
    public String generateToken(JwtClaims claims) {
        // 1. Header
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String encodedHeader = base64UrlEncode(headerJson);

        // 2. Payload
        StringBuilder rolesJson = new StringBuilder("[");
        for (int i = 0; i < claims.roles().size(); i++) {
            rolesJson.append("\"").append(claims.roles().get(i)).append("\"");
            if (i < claims.roles().size() - 1) rolesJson.append(",");
        }
        rolesJson.append("]");

        String payloadJson = String.format(
            "{\"sub\":\"%s\",\"tenantId\":\"%s\",\"roles\":%s,\"tokenBudget\":%d,\"iat\":%d,\"exp\":%d}",
            claims.subject(),
            claims.tenantId(),
            rolesJson,
            claims.tokenBudget(),
            claims.issuedAt().getEpochSecond(),
            claims.expiresAt().getEpochSecond()
        );
        String encodedPayload = base64UrlEncode(payloadJson);

        // 3. Signature
        String contentToSign = encodedHeader + "." + encodedPayload;
        String encodedSignature = sign(contentToSign);

        return contentToSign + "." + encodedSignature;
    }

    /**
     * Verifies the cryptographic signature and expiration of a JWT.
     */
    public JwtClaims validateAndParseClaims(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Malformed JWT: Must contain exactly 3 parts separated by dots");
        }

        String encodedHeader = parts[0];
        String encodedPayload = parts[1];
        String providedSignature = parts[2];

        // 1. Verify Signature Integrity
        String contentToSign = encodedHeader + "." + encodedPayload;
        String expectedSignature = sign(contentToSign);

        if (!expectedSignature.equals(providedSignature)) {
            throw new SecurityException("JWT Signature Verification Failed: Token has been altered or secret key is invalid!");
        }

        // 2. Parse Payload Claims
        String payloadJson = base64UrlDecode(encodedPayload);
        String sub = extractJsonString(payloadJson, "sub");
        String tenantId = extractJsonString(payloadJson, "tenantId");
        long exp = extractJsonLong(payloadJson, "exp");
        long iat = extractJsonLong(payloadJson, "iat");
        int tokenBudget = (int) extractJsonLong(payloadJson, "tokenBudget");
        List<String> roles = extractJsonStringList(payloadJson, "roles");

        JwtClaims claims = new JwtClaims(
            sub,
            tenantId,
            roles,
            tokenBudget,
            Instant.ofEpochSecond(iat),
            Instant.ofEpochSecond(exp)
        );

        // 3. Verify Expiration
        if (claims.isExpired()) {
            throw new SecurityException("JWT Token Expired: Token expired at " + claims.expiresAt());
        }

        return claims;
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secretKey, "HmacSHA256");
            mac.init(keySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("HMAC signing error", e);
        }
    }

    private String base64UrlEncode(String raw) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String base64UrlDecode(String encoded) {
        byte[] decoded = Base64.getUrlDecoder().decode(encoded);
        return new String(decoded, StandardCharsets.UTF_8);
    }

    private String extractJsonString(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) return matcher.group(1);
        throw new IllegalArgumentException("Missing JSON claim: " + key);
    }

    private long extractJsonLong(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) return Long.parseLong(matcher.group(1));
        throw new IllegalArgumentException("Missing numeric claim: " + key);
    }

    private List<String> extractJsonStringList(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\\[([^\\]]*)\\]");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            String rawList = matcher.group(1);
            if (rawList.isBlank()) return List.of();
            return Arrays.stream(rawList.split(","))
                .map(s -> s.trim().replace("\"", ""))
                .toList();
        }
        return List.of();
    }
}
