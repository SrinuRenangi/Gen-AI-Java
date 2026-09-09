package code;

import java.util.Collections;
import java.util.Set;

/**
 * Simulates Spring Security's Authentication interface.
 *
 * Encapsulates:
 * - Principal: The authenticated identity (e.g. username, user ID)
 * - Authorities: Set of granted roles and permissions (e.g. ROLE_USER, SCOPE_ai:infer)
 * - Authenticated flag: Indicates whether credentials have been verified
 */
public class AuthenticationToken {

    private final String principal;
    private final String credentials;
    private final Set<String> authorities;
    private final boolean authenticated;

    public AuthenticationToken(String principal, String credentials, Set<String> authorities, boolean authenticated) {
        this.principal = principal;
        this.credentials = credentials;
        this.authorities = authorities != null ? Collections.unmodifiableSet(authorities) : Set.of();
        this.authenticated = authenticated;
    }

    public static AuthenticationToken authenticated(String principal, Set<String> authorities) {
        return new AuthenticationToken(principal, "[PROTECTED]", authorities, true);
    }

    public static AuthenticationToken anonymous() {
        return new AuthenticationToken("anonymousUser", null, Set.of("ROLE_ANONYMOUS"), false);
    }

    public String getPrincipal() { return principal; }
    public String getCredentials() { return credentials; }
    public Set<String> getAuthorities() { return authorities; }
    public boolean isAuthenticated() { return authenticated; }

    public boolean hasAuthority(String authority) {
        return authorities.contains(authority);
    }

    @Override
    public String toString() {
        return "Authentication[principal='" + principal + "', authenticated=" + authenticated 
            + ", authorities=" + authorities + "]";
    }
}
