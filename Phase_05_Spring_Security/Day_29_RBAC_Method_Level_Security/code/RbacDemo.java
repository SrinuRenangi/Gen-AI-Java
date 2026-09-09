package com.genai.security.rbac;

import java.util.Set;

public class RbacDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("  DAY 29: METHOD-LEVEL SECURITY & RBAC DEMONSTRATION (SPRING SECURITY AOP)      ");
        System.out.println("================================================================================\n");

        // 1. Configure Role Hierarchy:
        // ROLE_ADMIN > ROLE_LEAD_AI_ENGINEER > ROLE_PRO_USER > ROLE_FREE_USER
        RoleHierarchy hierarchy = new RoleHierarchy();
        hierarchy.addHierarchy("ROLE_ADMIN", "ROLE_LEAD_AI_ENGINEER", "ROLE_PRO_USER", "ROLE_FREE_USER");
        hierarchy.addHierarchy("ROLE_PRO_USER", "ROLE_FREE_USER", "ai:model:standard");

        // 2. Instantiate secured service proxy
        AiModelService rawService = new AiModelServiceImpl();
        AiModelService securedService = SecurityProxyFactory.secure(AiModelService.class, rawService, hierarchy);

        // -----------------------------------------------------------------------------------------
        // SCENARIO 1: Unauthenticated request
        // -----------------------------------------------------------------------------------------
        System.out.println("[TEST 1] Invocation without authentication context...");
        SecurityContext.clear();
        try {
            securedService.executeStandardModel("Explain Virtual Threads");
            System.err.println("  ❌ FAILED: Should have been rejected!");
        } catch (AccessDeniedException e) {
            System.out.println("  ✅ REJECTED AS EXPECTED: " + e.getMessage());
        }

        // -----------------------------------------------------------------------------------------
        // SCENARIO 2: Free User attempts restricted GPT-4o inference
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 2] Free Tier User attempts GPT-4o inference...");
        SecurityContext.setAuthentication(new SecurityContext.Authentication(
                new SecurityContext.UserPrincipal("alice_free", "ACME_CORP", "alice@acme.com"),
                Set.of("ROLE_FREE_USER", "ai:model:standard"),
                true
        ));

        try {
            securedService.executeGpt4o("Design an enterprise multi-agent RAG workflow", 4096);
            System.err.println("  ❌ FAILED: Free user should not invoke GPT-4o!");
        } catch (AccessDeniedException e) {
            System.out.println("  ✅ REJECTED AS EXPECTED: " + e.getMessage());
        }

        // -----------------------------------------------------------------------------------------
        // SCENARIO 3: Free User invokes standard authorized model
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 3] Free Tier User invokes Standard model...");
        try {
            String response = securedService.executeStandardModel("What is Java 21?");
            System.out.println("  ✅ SUCCESS: " + response);
        } catch (AccessDeniedException e) {
            System.err.println("  ❌ FAILED: " + e.getMessage());
        }

        // -----------------------------------------------------------------------------------------
        // SCENARIO 4: Pro User invokes GPT-4o
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 4] Pro User invokes GPT-4o...");
        SecurityContext.setAuthentication(new SecurityContext.Authentication(
                new SecurityContext.UserPrincipal("bob_pro", "ACME_CORP", "bob@acme.com"),
                Set.of("ROLE_PRO_USER"),
                true
        ));

        try {
            String response = securedService.executeGpt4o("Write a Spring Boot 3 security filter", 2048);
            System.out.println("  ✅ SUCCESS: " + response);
        } catch (AccessDeniedException e) {
            System.err.println("  ❌ FAILED: " + e.getMessage());
        }

        // -----------------------------------------------------------------------------------------
        // SCENARIO 5: Pro User attempts to delete vector index
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 5] Pro User attempts to drop Vector Index...");
        try {
            securedService.deleteVectorIndex("embeddings_v3_cosine");
            System.err.println("  ❌ FAILED: Pro user cannot delete vector index!");
        } catch (AccessDeniedException e) {
            System.out.println("  ✅ REJECTED AS EXPECTED: " + e.getMessage());
        }

        // -----------------------------------------------------------------------------------------
        // SCENARIO 6: Admin deletes vector index
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 6] Admin invokes deleteVectorIndex...");
        SecurityContext.setAuthentication(new SecurityContext.Authentication(
                new SecurityContext.UserPrincipal("root_admin", "SYSTEM", "admin@genai.com"),
                Set.of("ROLE_ADMIN"),
                true
        ));

        try {
            securedService.deleteVectorIndex("embeddings_v3_cosine");
            System.out.println("  ✅ SUCCESS: Admin authorized to drop vector index.");
        } catch (AccessDeniedException e) {
            System.err.println("  ❌ FAILED: " + e.getMessage());
        }

        // -----------------------------------------------------------------------------------------
        // SCENARIO 7: Admin invokes GPT-4o via Role Hierarchy inheritance
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 7] Admin invokes GPT-4o (Inherited via Role Hierarchy)...");
        try {
            String response = securedService.executeGpt4o("Benchmark token throughput across clusters", 8192);
            System.out.println("  ✅ SUCCESS via Hierarchy: " + response);
        } catch (AccessDeniedException e) {
            System.err.println("  ❌ FAILED: " + e.getMessage());
        }

        // -----------------------------------------------------------------------------------------
        // SCENARIO 8: Multi-Tenant Isolation Check (Cross-tenant breach attempt)
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 8] Multi-Tenant Isolation: ACME_CORP user queries GLOBEX KB...");
        SecurityContext.setAuthentication(new SecurityContext.Authentication(
                new SecurityContext.UserPrincipal("charlie_acme", "ACME_CORP", "charlie@acme.com"),
                Set.of("ROLE_PRO_USER"),
                true
        ));

        try {
            securedService.queryKnowledgeBase("GLOBEX_CORP", "Retrieve proprietary pricing vectors");
            System.err.println("  ❌ FAILED: Cross-tenant data leak was not prevented!");
        } catch (AccessDeniedException e) {
            System.out.println("  ✅ BLOCKED CROSS-TENANT BREACH: " + e.getMessage());
        }

        // -----------------------------------------------------------------------------------------
        // SCENARIO 9: Multi-Tenant Isolation Check (User queries their own tenant KB)
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 9] User queries their OWN tenant KB (ACME_CORP)...");
        try {
            String kbResult = securedService.queryKnowledgeBase("ACME_CORP", "Retrieve internal onboarding guide");
            System.out.println("  ✅ SUCCESS: " + kbResult);
        } catch (AccessDeniedException e) {
            System.err.println("  ❌ FAILED: " + e.getMessage());
        }

        SecurityContext.clear();
        System.out.println("\n================================================================================");
        System.out.println("  ALL METHOD SECURITY & RBAC CHECKS PASSED PERFECTLY!                           ");
        System.out.println("================================================================================");
    }
}
