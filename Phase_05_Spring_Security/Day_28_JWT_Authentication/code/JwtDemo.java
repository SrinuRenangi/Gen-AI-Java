package code;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Driver class demonstrating Day 28: JWT Authentication from Scratch.
 *
 * Demonstrates:
 * 1. Generating cryptographically signed JWTs (HMAC-SHA256).
 * 2. Parsing and verifying claims on valid tokens.
 * 3. Detecting payload tampering (signature mismatch).
 * 4. Rejecting expired tokens.
 */
public class JwtDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println(" DAY 28: JWT AUTHENTICATION FROM SCRATCH — CREATION, VALIDATION & INTEGRITY     ");
        System.out.println("================================================================================");

        // 256-bit secret key for HMAC-SHA256
        String secret = "enterprise-ai-platform-super-secret-key-32bytes-minimum!";
        JwtTokenService jwtService = new JwtTokenService(secret);

        Instant now = Instant.now();

        // -------------------------------------------------------------------------
        // SCENARIO 1: TOKEN GENERATION
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 1: Generating Signed Enterprise AI JWT ---");
        JwtClaims userClaims = new JwtClaims(
            "usr_alice_123",
            "org_deepmind_ai",
            List.of("ROLE_USER", "SCOPE_ai:chat"),
            500000, // 500k token quota
            now,
            now.plus(1, ChronoUnit.HOURS)
        );

        String compactJwt = jwtService.generateToken(userClaims);
        System.out.println(" Generated Compact JWT (Header.Payload.Signature):");
        System.out.println(" " + compactJwt);

        String[] parts = compactJwt.split("\\.");
        System.out.println("\n Token Breakdown:");
        System.out.println("   [1] Encoded Header:    " + parts[0]);
        System.out.println("   [2] Encoded Payload:   " + parts[1]);
        System.out.println("   [3] HMAC-SHA256 Sig:   " + parts[2]);

        // -------------------------------------------------------------------------
        // SCENARIO 2: TOKEN VALIDATION & CLAIMS EXTRACTION
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 2: Validating Authentic Token ---");
        JwtClaims parsed = jwtService.validateAndParseClaims(compactJwt);
        System.out.println(" [VALIDATION SUCCESS] Cryptographic signature verified!");
        System.out.println("   Subject:       " + parsed.subject());
        System.out.println("   Tenant ID:     " + parsed.tenantId());
        System.out.println("   Roles:         " + parsed.roles());
        System.out.println("   Token Budget:  " + parsed.tokenBudget() + " tokens");
        System.out.println("   Expires At:    " + parsed.expiresAt());

        // -------------------------------------------------------------------------
        // SCENARIO 3: TAMPER DETECTION (ATTACK SIMULATION)
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 3: Tampering Attack Simulation ---");
        // An attacker modifies the payload to grant themselves ROLE_ADMIN and 99,000,000 tokens
        String fakePayloadJson = String.format(
            "{\"sub\":\"usr_alice_123\",\"tenantId\":\"org_deepmind_ai\",\"roles\":[\"ROLE_ADMIN\"],\"tokenBudget\":99000000,\"iat\":%d,\"exp\":%d}",
            now.getEpochSecond(),
            now.plus(1, ChronoUnit.HOURS).getEpochSecond()
        );
        String fakeEncodedPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(fakePayloadJson.getBytes());
        String tamperedToken = parts[0] + "." + fakeEncodedPayload + "." + parts[2]; // Keeping original signature!

        System.out.println(" Attacker substituted payload with elevated admin privileges:");
        try {
            jwtService.validateAndParseClaims(tamperedToken);
            System.out.println(" ERROR: Tampered token should have been rejected!");
        } catch (SecurityException se) {
            System.out.println(" [DEFENSE TRIGGERED] Tampered token rejected: " + se.getMessage());
        }

        // -------------------------------------------------------------------------
        // SCENARIO 4: EXPIRED TOKEN REJECTION
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 4: Expired Token Rejection ---");
        JwtClaims expiredClaims = new JwtClaims(
            "usr_alice_123",
            "org_deepmind_ai",
            List.of("ROLE_USER"),
            10000,
            now.minus(2, ChronoUnit.HOURS),
            now.minus(1, ChronoUnit.HOURS) // Expired 1 hour ago
        );
        String expiredToken = jwtService.generateToken(expiredClaims);
        System.out.println(" Attempting to authenticate with expired token...");
        try {
            jwtService.validateAndParseClaims(expiredToken);
            System.out.println(" ERROR: Expired token should have been rejected!");
        } catch (SecurityException se) {
            System.out.println(" [DEFENSE TRIGGERED] Expired token rejected: " + se.getMessage());
        }

        System.out.println("\n================================================================================");
        System.out.println(" DAY 28 DEMONSTRATION COMPLETE: JWT CRYPTOGRAPHIC DEFENSES VERIFIED!            ");
        System.out.println("================================================================================");
    }
}
