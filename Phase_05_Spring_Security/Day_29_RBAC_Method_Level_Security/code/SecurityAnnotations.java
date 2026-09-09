package com.genai.security.rbac;

import java.lang.annotation.*;

public final class SecurityAnnotations {

    private SecurityAnnotations() {}

    /**
     * Equivalent to @PreAuthorize("hasRole('ADMIN')") or @Secured("ROLE_ADMIN")
     */
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RequiresRole {
        String[] value();
    }

    /**
     * Equivalent to @PreAuthorize("hasAuthority('ai:model:execute')")
     */
    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RequiresAuthority {
        String[] value();
    }

    /**
     * Equivalent to @PreAuthorize("#tenantId == principal.tenantId or hasRole('ADMIN')")
     */
    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RequiresTenantAccess {
        int tenantIdParamIndex() default 0;
    }
}
