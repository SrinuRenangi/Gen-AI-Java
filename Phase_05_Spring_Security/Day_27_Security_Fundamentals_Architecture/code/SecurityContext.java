package code;

/**
 * Simulates Spring Security's SecurityContextHolder.
 *
 * Backed by ThreadLocal, ensuring each HTTP request thread carries its own
 * authenticated security principal throughout the service execution pipeline.
 */
public final class SecurityContext {

    private static final ThreadLocal<AuthenticationToken> CONTEXT = new ThreadLocal<>();

    private SecurityContext() {}

    public static void setAuthentication(AuthenticationToken token) {
        CONTEXT.set(token);
    }

    public static AuthenticationToken getAuthentication() {
        AuthenticationToken token = CONTEXT.get();
        return token != null ? token : AuthenticationToken.anonymous();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
