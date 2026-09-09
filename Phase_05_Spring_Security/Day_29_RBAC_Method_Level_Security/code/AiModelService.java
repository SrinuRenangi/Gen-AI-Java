package com.genai.security.rbac;

import com.genai.security.rbac.SecurityAnnotations.RequiresAuthority;
import com.genai.security.rbac.SecurityAnnotations.RequiresRole;
import com.genai.security.rbac.SecurityAnnotations.RequiresTenantAccess;

/**
 * Service interface exposing AI model inference and vector store management.
 */
public interface AiModelService {

    @RequiresRole({"ROLE_PRO_USER", "ROLE_ADMIN"})
    String executeGpt4o(String prompt, int requestedTokens);

    @RequiresAuthority({"ai:model:standard"})
    String executeStandardModel(String prompt);

    @RequiresRole({"ROLE_ADMIN"})
    void deleteVectorIndex(String indexName);

    @RequiresTenantAccess(tenantIdParamIndex = 0)
    String queryKnowledgeBase(String tenantId, String query);
}
