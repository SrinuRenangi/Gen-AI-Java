package com.genai.security.rbac;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Simulates Spring Security's Authentication and SecurityContextHolder.
 */
public final class SecurityContext {

    public record UserPrincipal(String username, String tenantId, String email) {}

    public record Authentication(
            UserPrincipal principal,
            Set<String> authorities,
            boolean authenticated
    ) {
        public Authentication {
            authorities = Collections.unmodifiableSet(new HashSet<>(authorities));
        }

        public boolean hasAuthority(String authority) {
            return authorities.contains(authority);
        }
    }

    private static final ThreadLocal<Authentication> CURRENT_AUTH = new ThreadLocal<>();

    private SecurityContext() {}

    public static void setAuthentication(Authentication auth) {
        CURRENT_AUTH.set(auth);
    }

    public static Authentication getAuthentication() {
        return CURRENT_AUTH.get();
    }

    public static void clear() {
        CURRENT_AUTH.remove();
    }
}
