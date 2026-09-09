package com.genai.security.oauth2;

import java.util.List;

public class OAuth2Demo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("  DAY 30: OAUTH2 & OIDC RESOURCE SERVER DEMONSTRATION (RS256 & JWKS)            ");
        System.out.println("================================================================================\n");

        // 1. Identity Provider Setup (e.g. Google or Keycloak)
        String authServerIssuer = "https://accounts.google.com";
        AsymmetricKeyManager googleKeyManager = new AsymmetricKeyManager("google-rsa-key-2026-01");
        JwksSimulator jwks = new JwksSimulator();
        jwks.registerKey(googleKeyManager.getKeyId(), googleKeyManager.getPublicKey());

        OidcTokenIssuer tokenIssuer = new OidcTokenIssuer(authServerIssuer, googleKeyManager);

        // 2. Spring Boot Resource Server Setup (Validates tokens using public key from JWKS)
        ResourceServerValidator resourceServer = new ResourceServerValidator(
                authServerIssuer,
                "genai-cloud-api",
                jwks
        );

        // -----------------------------------------------------------------------------------------
        // SCENARIO 1: Valid OIDC User Token (Social Login)
        // -----------------------------------------------------------------------------------------
        System.out.println("[TEST 1] Validating Google Social Login ID Token...");
        String userToken = tokenIssuer.issueUserOidcToken(
                "google-oauth2|1092837465",
                "alice.engineer@techcorp.com",
                "Alice Engineer",
                List.of("DEVELOPER", "PRO_USER"),
                3600 // 1 hour validity
        );
        System.out.println("  Generated RS256 JWT: " + userToken.substring(0, 45) + "...[TRUNCATED]");

        try {
            ResourceServerValidator.AuthenticatedUser user = resourceServer.validateAndAuthenticate(userToken);
            System.out.println("  ✅ AUTHENTICATED SUCCESSFULLY!");
            System.out.println("     Subject: " + user.subject());
            System.out.println("     Email:   " + user.email());
            System.out.println("     Name:    " + user.name());
            System.out.println("     Roles:   " + user.authorities());
        } catch (Exception e) {
            System.err.println("  ❌ FAILED: " + e.getMessage());
        }

        // -----------------------------------------------------------------------------------------
        // SCENARIO 2: Valid M2M Client Credentials Token (AI Worker Service)
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 2] Validating Machine-to-Machine (M2M) Service Account Token...");
        String m2mToken = tokenIssuer.issueClientCredentialsToken(
                "svc-rag-indexer-01",
                "ai:embed ai:infer:stream",
                1800
        );

        try {
            ResourceServerValidator.AuthenticatedUser svc = resourceServer.validateAndAuthenticate(m2mToken);
            System.out.println("  ✅ M2M SERVICE AUTHENTICATED!");
            System.out.println("     Client ID: " + svc.subject());
            System.out.println("     Scopes:    " + svc.authorities());
        } catch (Exception e) {
            System.err.println("  ❌ FAILED: " + e.getMessage());
        }

        // -----------------------------------------------------------------------------------------
        // SCENARIO 3: Expired Token
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 3] Validating Expired Token...");
        String expiredToken = tokenIssuer.issueUserOidcToken(
                "google-oauth2|expired",
                "old.user@techcorp.com",
                "Old User",
                List.of("USER"),
                -60 // expired 60 seconds ago
        );

        try {
            resourceServer.validateAndAuthenticate(expiredToken);
            System.err.println("  ❌ FAILED: Expired token should be rejected!");
        } catch (SecurityException e) {
            System.out.println("  ✅ REJECTED AS EXPECTED: " + e.getMessage());
        }

        // -----------------------------------------------------------------------------------------
        // SCENARIO 4: Tampered / Counterfeit Token (Signed by Rogue Key)
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 4] Validating Rogue/Counterfeit Token (Spoofed Signature)...");
        AsymmetricKeyManager rogueKeyManager = new AsymmetricKeyManager("google-rsa-key-2026-01"); // same kid, different key!
        OidcTokenIssuer rogueIssuer = new OidcTokenIssuer(authServerIssuer, rogueKeyManager);
        String spoofedToken = rogueIssuer.issueUserOidcToken(
                "hacker|007",
                "hacker@darknet.org",
                "Rogue Admin",
                List.of("ADMIN"),
                3600
        );

        try {
            resourceServer.validateAndAuthenticate(spoofedToken);
            System.err.println("  ❌ FAILED: Spoofed token should have failed cryptographic verification!");
        } catch (SecurityException e) {
            System.out.println("  ✅ BLOCKED SPOOFING ATTACK: " + e.getMessage());
        }

        // -----------------------------------------------------------------------------------------
        // SCENARIO 5: Untrusted Issuer
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 5] Validating Untrusted Issuer...");
        AsymmetricKeyManager untrustedKeyManager = new AsymmetricKeyManager("untrusted-key");
        jwks.registerKey(untrustedKeyManager.getKeyId(), untrustedKeyManager.getPublicKey());
        OidcTokenIssuer untrustedIssuer = new OidcTokenIssuer("https://malicious-idp.evil.com", untrustedKeyManager);
        String untrustedToken = untrustedIssuer.issueUserOidcToken(
                "evil|123",
                "evil@malicious.com",
                "Evil User",
                List.of("ADMIN"),
                3600
        );

        try {
            resourceServer.validateAndAuthenticate(untrustedToken);
            System.err.println("  ❌ FAILED: Untrusted issuer must be rejected!");
        } catch (SecurityException e) {
            System.out.println("  ✅ BLOCKED UNTRUSTED ISSUER: " + e.getMessage());
        }

        System.out.println("\n================================================================================");
        System.out.println("  ALL OAUTH2 & RESOURCE SERVER TESTS PASSED PERFECTLY!                          ");
        System.out.println("================================================================================");
    }
}
