package com.genai.security.rbac;

import com.genai.security.rbac.SecurityAnnotations.RequiresAuthority;
import com.genai.security.rbac.SecurityAnnotations.RequiresRole;
import com.genai.security.rbac.SecurityAnnotations.RequiresTenantAccess;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Set;

/**
 * Simulates Spring Security's MethodSecurityInterceptor / AuthorizationManagerBeforeMethodInterceptor.
 * Wraps service instances in dynamic proxies that enforce RBAC rules before method execution.
 */
public final class SecurityProxyFactory {

    private SecurityProxyFactory() {}

    @SuppressWarnings("unchecked")
    public static <T> T secure(Class<T> interfaceClass, T target, RoleHierarchy roleHierarchy) {
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                new MethodSecurityInvocationHandler(target, roleHierarchy)
        );
    }

    private static class MethodSecurityInvocationHandler implements InvocationHandler {
        private final Object target;
        private final RoleHierarchy roleHierarchy;

        MethodSecurityInvocationHandler(Object target, RoleHierarchy roleHierarchy) {
            this.target = target;
            this.roleHierarchy = roleHierarchy;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 1. Fetch authentication from context
            SecurityContext.Authentication auth = SecurityContext.getAuthentication();
            if (auth == null || !auth.authenticated()) {
                throw new AccessDeniedException("401 Unauthorized: No active authenticated security context.");
            }

            // 2. Expand authorities via Role Hierarchy
            Set<String> effectiveAuthorities = roleHierarchy != null 
                    ? roleHierarchy.getReachableAuthorities(auth.authorities())
                    : auth.authorities();

            // 3. Inspect @RequiresRole
            RequiresRole roleAnnotation = method.getAnnotation(RequiresRole.class);
            if (roleAnnotation != null) {
                boolean hasAnyRole = Arrays.stream(roleAnnotation.value())
                        .anyMatch(effectiveAuthorities::contains);
                if (!hasAnyRole) {
                    throw new AccessDeniedException(String.format(
                            "403 Forbidden: User '%s' lacks required role %s for method '%s'. Effective roles: %s",
                            auth.principal().username(),
                            Arrays.toString(roleAnnotation.value()),
                            method.getName(),
                            effectiveAuthorities
                    ));
                }
            }

            // 4. Inspect @RequiresAuthority
            RequiresAuthority authAnnotation = method.getAnnotation(RequiresAuthority.class);
            if (authAnnotation != null) {
                boolean hasAnyAuth = Arrays.stream(authAnnotation.value())
                        .anyMatch(effectiveAuthorities::contains);
                if (!hasAnyAuth) {
                    throw new AccessDeniedException(String.format(
                            "403 Forbidden: User '%s' lacks authority %s for method '%s'. Effective authorities: %s",
                            auth.principal().username(),
                            Arrays.toString(authAnnotation.value()),
                            method.getName(),
                            effectiveAuthorities
                    ));
                }
            }

            // 5. Inspect @RequiresTenantAccess (Multi-tenant AI isolation)
            RequiresTenantAccess tenantAnnotation = method.getAnnotation(RequiresTenantAccess.class);
            if (tenantAnnotation != null && args != null && args.length > tenantAnnotation.tenantIdParamIndex()) {
                String requestedTenantId = String.valueOf(args[tenantAnnotation.tenantIdParamIndex()]);
                boolean isAdmin = effectiveAuthorities.contains("ROLE_ADMIN");
                boolean ownsTenant = requestedTenantId.equals(auth.principal().tenantId());

                if (!isAdmin && !ownsTenant) {
                    throw new AccessDeniedException(String.format(
                            "403 Forbidden: Multi-tenant boundary violation! User '%s' (Tenant: '%s') attempted to access Tenant: '%s'",
                            auth.principal().username(),
                            auth.principal().tenantId(),
                            requestedTenantId
                    ));
                }
            }

            // All security guards passed -> Proceed with method invocation
            return method.invoke(target, args);
        }
    }
}
