package code;

import java.util.Map;
import java.util.Set;

/**
 * Enterprise Spring Security Filter Chain Simulator.
 *
 * Recreates the execution sequence of:
 * 1. BearerTokenAuthenticationFilter: Inspects Authorization header and sets SecurityContext.
 * 2. AuthorizationFilter: Verifies required roles/authorities for matched URL patterns.
 * 3. ExceptionTranslationFilter: Translates security exceptions to 401 Unauthorized or 403 Forbidden.
 */
public class SecurityFilterChainSimulator {

    public record HttpResponse(int status, String body) {}

    // Simulated user database
    private static final Map<String, UserCredentials> TOKEN_DATABASE = Map.of(
        "token_alice_regular", new UserCredentials("alice", Set.of("ROLE_USER", "SCOPE_ai:chat")),
        "token_bob_admin",     new UserCredentials("bob",   Set.of("ROLE_USER", "ROLE_ADMIN", "SCOPE_ai:chat", "SCOPE_ai:admin"))
    );

    public record UserCredentials(String username, Set<String> authorities) {}

    public HttpResponse processRequest(String method, String path, String authHeader) {
        System.out.printf("  --> [FilterChainProxy] Intercepting %s %s%n", method, path);

        try {
            // FILTER 1: AuthenticationFilter
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7).trim();
                if (TOKEN_DATABASE.containsKey(token)) {
                    UserCredentials user = TOKEN_DATABASE.get(token);
                    AuthenticationToken auth = AuthenticationToken.authenticated(user.username(), user.authorities());
                    SecurityContext.setAuthentication(auth);
                    System.out.println("      [AuthFilter] Successfully authenticated: " + auth);
                } else {
                    System.out.println("      [AuthFilter] Invalid bearer token: " + token);
                    SecurityContext.setAuthentication(AuthenticationToken.anonymous());
                }
            } else {
                SecurityContext.setAuthentication(AuthenticationToken.anonymous());
                System.out.println("      [AuthFilter] No bearer token. Marked as anonymousUser.");
            }

            // FILTER 2: AuthorizationFilter (URL Pattern matching)
            AuthenticationToken currentAuth = SecurityContext.getAuthentication();

            // Rule 1: Public endpoints permitAll
            if (path.startsWith("/api/v1/public") || path.startsWith("/swagger-ui")) {
                System.out.println("      [AuthzFilter] Public path matched: permitAll() -> Access Granted.");
                return new HttpResponse(200, "{\"status\":\"public_ok\"}");
            }

            // Rule 2: If not authenticated -> 401 Unauthorized
            if (!currentAuth.isAuthenticated()) {
                System.out.println("      [AuthzFilter] Access Denied: User is not authenticated -> 401 Unauthorized");
                return new HttpResponse(401, "{\"error\":\"Unauthorized\",\"message\":\"Full authentication is required to access this resource\"}");
            }

            // Rule 3: Admin endpoints require ROLE_ADMIN
            if (path.startsWith("/api/v1/admin")) {
                if (!currentAuth.hasAuthority("ROLE_ADMIN")) {
                    System.out.println("      [AuthzFilter] Access Denied: Missing ROLE_ADMIN -> 403 Forbidden");
                    return new HttpResponse(403, "{\"error\":\"Forbidden\",\"message\":\"Access denied: Requires ROLE_ADMIN\"}");
                }
                System.out.println("      [AuthzFilter] Admin path matched: ROLE_ADMIN verified -> Access Granted.");
                return new HttpResponse(200, "{\"status\":\"admin_ok\",\"action\":\"manage_model_deployments\"}");
            }

            // Rule 4: Chat inference endpoints require SCOPE_ai:chat
            if (path.startsWith("/api/v1/chat")) {
                if (!currentAuth.hasAuthority("SCOPE_ai:chat")) {
                    System.out.println("      [AuthzFilter] Access Denied: Missing SCOPE_ai:chat -> 403 Forbidden");
                    return new HttpResponse(403, "{\"error\":\"Forbidden\",\"message\":\"Access denied: Missing SCOPE_ai:chat\"}");
                }
                System.out.println("      [AuthzFilter] Chat path matched: SCOPE_ai:chat verified -> Access Granted.");
                return new HttpResponse(200, "{\"id\":\"cmpl_9821\",\"completion\":\"Access granted to LLM inference pipeline.\"}");
            }

            // Default fallback
            return new HttpResponse(200, "{\"status\":\"authenticated_ok\"}");

        } finally {
            SecurityContext.clear(); // Always clean up ThreadLocal!
        }
    }
}
