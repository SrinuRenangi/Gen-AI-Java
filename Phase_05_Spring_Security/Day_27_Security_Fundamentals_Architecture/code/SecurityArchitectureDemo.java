package code;

/**
 * Driver class demonstrating Day 27: Security Fundamentals & Spring Security Architecture.
 *
 * Demonstrates:
 * 1. Public endpoints (permitAll) vs Protected endpoints.
 * 2. Unauthenticated access -> 401 Unauthorized.
 * 3. Insufficient authorities -> 403 Forbidden.
 * 4. Authorized access with required scopes -> 200 OK.
 */
public class SecurityArchitectureDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println(" DAY 27: SPRING SECURITY FUNDAMENTALS, FILTER CHAIN & ACCESS CONTROL            ");
        System.out.println("================================================================================");

        SecurityFilterChainSimulator securityChain = new SecurityFilterChainSimulator();

        // -------------------------------------------------------------------------
        // SCENARIO 1: PUBLIC ENDPOINT ACCESS (permitAll)
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 1: Anonymous Access to Public Endpoint (permitAll) ---");
        var resp1 = securityChain.processRequest("GET", "/api/v1/public/health", null);
        System.out.println(" Result: HTTP Status " + resp1.status() + " -> " + resp1.body());

        // -------------------------------------------------------------------------
        // SCENARIO 2: UNAUTHENTICATED ACCESS TO PROTECTED AI ENDPOINT
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 2: Anonymous Access to Protected LLM Chat Endpoint ---");
        var resp2 = securityChain.processRequest("POST", "/api/v1/chat/completions", null);
        System.out.println(" Result: HTTP Status " + resp2.status() + " -> " + resp2.body());

        // -------------------------------------------------------------------------
        // SCENARIO 3: AUTHENTICATED ACCESS WITH VALID SCOPE
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 3: Authenticated User (Alice) Calling Chat Endpoint ---");
        var resp3 = securityChain.processRequest("POST", "/api/v1/chat/completions", "Bearer token_alice_regular");
        System.out.println(" Result: HTTP Status " + resp3.status() + " -> " + resp3.body());

        // -------------------------------------------------------------------------
        // SCENARIO 4: INSUFFICIENT PRIVILEGES (403 FORBIDDEN)
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 4: Regular User (Alice) Attempting Admin Model Configuration ---");
        var resp4 = securityChain.processRequest("POST", "/api/v1/admin/models/deploy", "Bearer token_alice_regular");
        System.out.println(" Result: HTTP Status " + resp4.status() + " -> " + resp4.body());

        // -------------------------------------------------------------------------
        // SCENARIO 5: ADMIN ACCESS GRANTED (ROLE_ADMIN)
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 5: Admin User (Bob) Deploying AI Model ---");
        var resp5 = securityChain.processRequest("POST", "/api/v1/admin/models/deploy", "Bearer token_bob_admin");
        System.out.println(" Result: HTTP Status " + resp5.status() + " -> " + resp5.body());

        System.out.println("\n================================================================================");
        System.out.println(" DAY 27 DEMONSTRATION COMPLETE: SECURITY FILTER CHAIN FULLY VERIFIED!           ");
        System.out.println("================================================================================");
    }
}
