package code;

import java.time.Instant;
import java.util.List;

/**
 * Enterprise AI JWT Claims Data Transfer Record.
 *
 * Encapsulates standard RFC 7519 claims plus custom Gen AI claims:
 * - subject: User ID
 * - tenantId: Multi-tenant organization identifier
 * - roles: List of granted security roles
 * - tokenBudget: Monthly allocated tokens for LLM inference
 * - issuedAt: Token creation timestamp
 * - expiresAt: Token expiration timestamp
 */
public record JwtClaims(
    String subject,
    String tenantId,
    List<String> roles,
    int tokenBudget,
    Instant issuedAt,
    Instant expiresAt
) {
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
