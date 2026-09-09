package com.genai.enterprise.capstone;

import java.util.Set;

/**
 * Enterprise security context representing an authenticated JWT caller with RBAC claims.
 */
public record EnterpriseSecurityContext(
        String userId,
        String tenantId,
        String email,
        Set<String> roles,
        int tokenQuotaRemaining
) {
    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean canExecuteFinancialTransfers() {
        return roles.contains("ROLE_ADMIN") || roles.contains("ROLE_FINANCE_OFFICER");
    }
}
