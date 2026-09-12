# Day 29: Role-Based Access Control (RBAC) & Method-Level Security
## Securing AI Business Methods, SpEL Authorization, Role Hierarchies & Multi-Tenant RAG Isolation

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [◀ Day 28: JWT Authentication](../Day_28_JWT_Authentication/Day_28_JWT_Authentication.md) | [All 60 Days Overview](../../README.md) | [Day 30: OAuth2 & Social Login ▶](../Day_30_OAuth2_Social_Login/Day_30_OAuth2_Social_Login.md) |

---

## 1. Topic Overview

Method-Level Role-Based Access Control (RBAC) uses Spring AOP proxies and Spring Expression Language (SpEL) to enforce fine-grained authorization directly on business service methods rather than relying solely on perimeter URL filtering. In enterprise Generative AI systems, method security protects expensive model inference tiers, prevents unauthorized vector index mutations, and enforces multi-tenant boundary isolation across RAG vector pipelines.

---

## 2. Basic Foundations (True Zero)

### What is Method-Level Security?
Perimeter HTTP URL security (`requestMatchers("/api/v1/ai/**")`) checks permissions when an HTTP request enters the application. However, URL checks cannot inspect domain objects, evaluate runtime method arguments (such as token counts or model names), or protect internal service methods invoked by asynchronous message consumers, scheduled jobs, or AI agent tool-calling loops.

**Method-Level Security** places authorization guards directly on Java service methods using annotations like `@PreAuthorize`. Before the method's code executes, Spring intercepts the call, validates the caller's granted authorities and role hierarchies, evaluates dynamic parameters via SpEL, and immediately halts unauthorized calls with an `AccessDeniedException` (HTTP 403 Forbidden).

```
+-----------------------------------------------------------------------------------+
|               THE AIRPORT TERMINAL GATE VS. COCKPIT DOOR ANALOGY                  |
|                                                                                   |
|  TIER 1: HTTP URL Security (The Airport Terminal Gate)                            |
|  - Checks: Valid boarding ticket and passport (JWT Token).                        |
|  - Result: You are permitted to enter the departure lounge.                      |
|  - Limitation: Every passenger (Economy, First Class, Pilot) enters here.         |
|                                                                                   |
|  TIER 2: Method-Level Security (The Reinforced Cockpit Door)                      |
|  - Checks: Biometric scanner, pilot license, Captain badge (`@PreAuthorize`).     |
|  - Result: Only certified flight captains may touch the flight controls!          |
|  - Defense: Even if an unauthorized passenger walks up to the door, they cannot   |
|    enter the cockpit or pilot the aircraft!                                       |
+-----------------------------------------------------------------------------------+
```

### Minimal Beginner-Friendly Working Code Example

Below is a self-contained Java demonstration of how a dynamic proxy intercepts a method invocation, evaluates a role requirement, and enforces authorization before execution:

```java
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

public class BasicMethodSecurityExample {

    // 1. Service Interface
    interface AiInferenceService {
        String executeStandardModel(String prompt);
        String executeGpt4o(String prompt);
    }

    // 2. Concrete Implementation
    static class AiInferenceServiceImpl implements AiInferenceService {
        public String executeStandardModel(String prompt) { return "Standard AI response for: " + prompt; }
        public String executeGpt4o(String prompt) { return "GPT-4o response for: " + prompt; }
    }

    // 3. Security Context Simulation
    static class SecurityContext {
        static String currentUser = "alice";
        static Set<String> roles = Set.of("ROLE_FREE_USER");
    }

    // 4. Dynamic Proxy Interceptor (Mirroring Spring AOP)
    static class SecurityInterceptor implements InvocationHandler {
        private final Object target;
        public SecurityInterceptor(Object target) { this.target = target; }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("executeGpt4o")) {
                if (!SecurityContext.roles.contains("ROLE_PRO_USER") && !SecurityContext.roles.contains("ROLE_ADMIN")) {
                    throw new SecurityException("403 Forbidden: User '" + SecurityContext.currentUser + 
                                                "' lacks ROLE_PRO_USER for method " + method.getName());
                }
            }
            return method.invoke(target, args);
        }
    }

    public static void main(String[] args) {
        AiInferenceService realService = new AiInferenceServiceImpl();
        AiInferenceService securedService = (AiInferenceService) Proxy.newProxyInstance(
            AiInferenceService.class.getClassLoader(),
            new Class<?>[]{AiInferenceService.class},
            new SecurityInterceptor(realService)
        );

        System.out.println("Calling Standard Model: " + securedService.executeStandardModel("Hello"));

        try {
            System.out.println("Calling GPT-4o Model: " + securedService.executeGpt4o("Deep analysis"));
        } catch (SecurityException ex) {
            System.out.println("Security Interceptor Triggered: " + ex.getMessage());
        }
    }
}
```

#### Line-by-Line Walkthrough:
- **Lines 9–13**: Defines an `AiInferenceService` interface exposing a lightweight model and a costly `executeGpt4o` model.
- **Lines 22–25**: Simulates a `SecurityContext` holding the current user `"alice"` with only `"ROLE_FREE_USER"`.
- **Lines 28–41**: `SecurityInterceptor` implements `InvocationHandler`. When `executeGpt4o` is called, it inspects `SecurityContext.roles`. If the caller lacks `ROLE_PRO_USER` or `ROLE_ADMIN`, it throws `SecurityException` immediately without invoking the target.
- **Lines 44–50**: Instantiates the secured dynamic proxy.
- **Lines 52–57**: Calling `executeStandardModel` succeeds, while calling `executeGpt4o` is intercepted and blocked before execution.

---

## 3. Core Concept Walkthrough (Basic → Intermediate)

### Under-the-Hood Architecture: Spring AOP Method Interception

When you enable `@EnableMethodSecurity`, Spring creates an AOP dynamic proxy (via CGLIB or JDK Dynamic Proxy) around your service bean:

```
                           SPRING SECURITY AOP INTERCEPTION PIPELINE
                           
 Caller (e.g. Controller)
           |
           | 1. Invokes aiModelService.executeGpt4o(request)
           v
+------------------------------------------------------------------------+
| Spring AOP Dynamic Proxy (AiModelService$$SpringCGLIB$$0)              |
|                                                                        |
|   2. Intercepts call via AuthorizationManagerBeforeMethodInterceptor   |
|                                                                        |
|   3. Extracts SecurityContext from ThreadLocal                         |
|      Authentication auth = SecurityContextHolder.getContext()          |
|                                                                        |
|   4. Evaluates SpEL Expression:                                        |
|      @PreAuthorize("hasRole('PRO') and #tokens <= 4096")               |
|                                                                        |
|   5. RoleHierarchy Check:                                              |
|      Expands direct roles (ROLE_ADMIN -> ROLE_PRO -> ROLE_FREE)        |
|                                                                        |
|   6. Decision:                                                         |
|      +-- AuthorizationDecision(false)                                  |
|      |     \--> Throws AccessDeniedException                           |
|      |          (Method execution aborted immediately)                 |
|      |                                                                 |
|      +-- AuthorizationDecision(true)                                   |
|            \--> Delegates to Target:                                   |
|                 AiModelServiceImpl.executeGpt4o(request)               |
+----------------------------------+-------------------------------------+
                                   |
                                   v
              Target Bean Execution (Actual LLM API Call)
```

---

### Modern Spring Security 6 Configuration: `@EnableMethodSecurity`

In Spring Boot 3, legacy `@EnableGlobalMethodSecurity` is deprecated. Use `@EnableMethodSecurity`:

```java
package com.example.genai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
    prePostEnabled = true,   // Enables @PreAuthorize, @PostAuthorize, @PreFilter, @PostFilter
    securedEnabled = true,   // Enables legacy @Secured
    jsr250Enabled = true     // Enables @RolesAllowed from jakarta.annotation
)
public class MethodSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**", "/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .build();
    }

    /**
     * Enterprise AI Role Hierarchy:
     * ADMIN implies LEAD_ENGINEER implies PRO_USER implies FREE_USER
     */
    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
            .role("ADMIN").implies("LEAD_ENGINEER")
            .role("LEAD_ENGINEER").implies("PRO_USER")
            .role("PRO_USER").implies("FREE_USER")
            .build();
    }
}
```

---

### Roles vs. Authorities: The Essential Distinction

```
+-----------------+-----------------------------------+---------------------------------------+
| Characteristic  | Role                              | Authority (Permission)                |
+-----------------+-----------------------------------+---------------------------------------+
| Naming Prefix   | Must start with `ROLE_`           | Arbitrary string (e.g. `ai:chat`)     |
+-----------------+-----------------------------------+---------------------------------------+
| SpEL Check      | `hasRole('ADMIN')` (adds `ROLE_`) | `hasAuthority('ai:model:gpt-4o')`     |
+-----------------+-----------------------------------+---------------------------------------+
| Granularity     | Coarse-grained subscription tier  | Fine-grained operational action       |
+-----------------+-----------------------------------+---------------------------------------+
| Best Practice   | Assign Roles to Users             | Check Authorities on Service Methods  |
+-----------------+-----------------------------------+---------------------------------------+
```

---

### Declarative Method Security Annotations

```java
// 1. @PreAuthorize: Evaluated BEFORE method executes
@PreAuthorize("hasRole('PRO_USER') or #request.maxTokens <= 512")
public ChatResponse executeInference(InferenceRequest request) {
    return llmClient.chat(request);
}

// 2. @PostAuthorize: Evaluated AFTER method executes (inspects returnObject)
@PostAuthorize("returnObject.tenantId == authentication.principal.tenantId or hasRole('ADMIN')")
public RagDocument fetchDocumentById(String documentId) {
    return documentRepository.findById(documentId).orElseThrow();
}

// 3. @PreFilter: Filters input collection parameters before invocation
@PreFilter("filterObject.tenantId == authentication.principal.tenantId")
public void indexDocumentChunks(List<DocumentChunk> chunks) {
    vectorStore.saveAll(chunks);
}

// 4. @PostFilter: Filters output collection elements before returning
@PostFilter("hasRole('ADMIN') or filterObject.classification != 'TOP_SECRET'")
public List<VectorSearchResult> searchKnowledgeBase(String query) {
    return vectorStore.similaritySearch(query);
}
```

---

## 4. Prerequisite & Supporting Concepts

### Prerequisite / Supporting Concept: Spring AOP Dynamic Proxies (CGLIB vs JDK)
Spring method security relies on AOP proxies:
- If a target bean implements an interface, Spring uses **JDK Dynamic Proxies** (`java.lang.reflect.Proxy`).
- If a target bean is a concrete class without interfaces, Spring uses **CGLIB** bytecode subclassing.

**Critical Consequence (Self-Invocation Pitfall)**: If method `A()` in class `AiService` calls method `B()` in the same class, the call does **not** go through the proxy! Therefore, `@PreAuthorize` on method `B()` will be completely bypassed! Internal method calls must be made via an injected self-reference or separated into distinct beans.

### Prerequisite / Supporting Concept: SpEL Security Expression Context
Spring Security evaluates SpEL inside a specialized `MethodSecurityEvaluationContext`. Available root variables include:
- `authentication`: Current `Authentication` object from `SecurityContextHolder`.
- `principal`: The user principal object (`UserDetails` or custom user record).
- `#paramName`: Arguments passed into the intercepted method by parameter name.
- `@beanName`: Any registered Spring bean in the `ApplicationContext`!

### Prerequisite / Supporting Concept: Multi-Tenant Vector Database Isolation
In multi-tenant RAG systems, storing documents for multiple organizations in a single PostgreSQL `pgvector` database introduces data leakage hazards. Method-level security intercepts queries before execution:

```java
@Component("tenantSecurity")
public class TenantSecurityExpressionService {
    public boolean canAccessTenant(String requestedTenantId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return true;

        if (auth.getPrincipal() instanceof CustomUserPrincipal user) {
            return user.tenantId().equalsIgnoreCase(requestedTenantId);
        }
        return false;
    }
}
```

Guarding the vector query method:
```java
@PreAuthorize("@tenantSecurity.canAccessTenant(#tenantId)")
public List<VectorChunk> queryDocuments(String tenantId, String query, int topK) {
    return vectorStoreRepository.searchByTenant(tenantId, query, topK);
}
```

---

## 5. Advanced Depth (Intermediate → Advanced)

### Common Pitfalls & Antipatterns

#### Pitfall 1: Destructive Methods Guarded by `@PostAuthorize`
```
+-----------------------------------------------------------------------------------+
| BAD PRACTICE: Using @PostAuthorize on Mutating Methods                            |
|                                                                                   |
| @PostAuthorize("returnObject.owner == authentication.name")                       |
| public Invoice deleteAndRefundVectorIndex(String indexId) {                       |
|     // Table is permanently dropped BEFORE @PostAuthorize evaluates!              |
| }                                                                                 |
+-----------------------------------------------------------------------------------+
| GOOD PRACTICE: Guard State Changes with @PreAuthorize                             |
|                                                                                   |
| @PreAuthorize("@indexSecurity.isOwner(#indexId) or hasRole('ADMIN')")             |
| public Invoice deleteAndRefundVectorIndex(String indexId) {                       |
|     // Authorization check occurs BEFORE table deletion!                          |
| }                                                                                 |
+-----------------------------------------------------------------------------------+
```

---

### Hands-On Simulation Code Walkthrough

The companion code repository demonstrates this architecture:
- `SecurityContext.java`: ThreadLocal authentication state with user principals and granted authorities.
- `RoleHierarchy.java`: Transitive closure role hierarchy computation engine.
- `SecurityProxyFactory.java`: Full AOP dynamic proxy interceptor evaluating `@RequiresRole` and multi-tenant constraints.
- `RbacDemo.java`: 9-scenario verification test suite validating roles, hierarchies, and multi-tenant isolation.

```powershell
# Compile Day 29 code
javac -d out Phase_05_Spring_Security/Day_29_RBAC_Method_Level_Security/code/*.java

# Run RbacDemo
java -cp out com.genai.security.rbac.RbacDemo
```

#### Verified Execution Output:
```
================================================================================
  DAY 29: METHOD-LEVEL SECURITY & RBAC DEMONSTRATION (SPRING SECURITY AOP)      
================================================================================

[TEST 1] Invocation without authentication context...
  [OK] REJECTED AS EXPECTED: 401 Unauthorized: No active authenticated security context.

[TEST 2] Free Tier User attempts GPT-4o inference...
  [OK] REJECTED AS EXPECTED: 403 Forbidden: User 'alice_free' lacks required role [ROLE_PRO_USER, ROLE_ADMIN] for method 'executeGpt4o'. Effective roles: [ROLE_FREE_USER, ai:model:standard]

[TEST 3] Free Tier User invokes Standard model...
  [OK] SUCCESS: [Standard LLM Response] Completed inference for: "What is Java 21?"

[TEST 4] Pro User invokes GPT-4o...
  [OK] SUCCESS: [GPT-4o Response] Generated 128 tokens for: "Write a Spring Boot 3 security filter" (Max allowed: 2048)

[TEST 5] Pro User attempts to drop Vector Index...
  [OK] REJECTED AS EXPECTED: 403 Forbidden: User 'bob_pro' lacks required role [ROLE_ADMIN] for method 'deleteVectorIndex'. Effective roles: [ROLE_FREE_USER, ROLE_PRO_USER, ai:model:standard]

[TEST 6] Admin invokes deleteVectorIndex...
  [DATABASE AUDIT] Vector index 'embeddings_v3_cosine' permanently dropped by Admin.
  [OK] SUCCESS: Admin authorized to drop vector index.

[TEST 7] Admin invokes GPT-4o (Inherited via Role Hierarchy)...
  [OK] SUCCESS via Hierarchy: [GPT-4o Response] Generated 128 tokens for: "Benchmark token throughput across clusters" (Max allowed: 8192)

[TEST 8] Multi-Tenant Isolation: ACME_CORP user queries GLOBEX KB...
  [OK] BLOCKED CROSS-TENANT BREACH: 403 Forbidden: Multi-tenant boundary violation! User 'charlie_acme' (Tenant: 'ACME_CORP') attempted to access Tenant: 'GLOBEX_CORP'

[TEST 9] User queries their OWN tenant KB (ACME_CORP)...
  [OK] SUCCESS: [KnowledgeBase RAG] Returned 3 vector chunks for Tenant [ACME_CORP] with query: "Retrieve internal onboarding guide"

================================================================================
  ALL METHOD SECURITY & RBAC CHECKS PASSED PERFECTLY!                           
================================================================================
```

---

## 6. Quick Recap

| Concept | Description | Enterprise Rule / Best Practice |
| :--- | :--- | :--- |
| **`@EnableMethodSecurity`** | Enables method security in Spring Boot 3 | Replaces deprecated `@EnableGlobalMethodSecurity`. |
| **`@PreAuthorize`** | Checks conditions before method execution | Primary tool for RBAC and parameter validation. |
| **`@PostAuthorize`** | Checks conditions after return value is produced| Use strictly on non-mutating read operations. |
| **`@PreFilter` / `@PostFilter`** | Filters elements in input/output collections | Evaluates each item via `filterObject`. |
| **Role Hierarchy** | Maps parent roles to child privileges | Eliminates repetitive role checks (`ADMIN implies PRO`). |
| **SpEL Bean Reference** | `@beanName.method(#arg)` | Delegates complex domain authorization to dedicated beans. |
| **Self-Invocation Trap** | Calling `this.methodB()` bypasses proxies | Internal calls bypass AOP proxies and security checks. |

---

## 7. Self-Check Questions & Practice Exercises

### Conceptual & Architectural Questions

#### Q1: In Spring Boot 3 / Spring Security 6, which annotation enables `@PreAuthorize`?
**Answer**: `@EnableMethodSecurity` (with `prePostEnabled = true` by default). The legacy annotation `@EnableGlobalMethodSecurity` is deprecated and removed from modern Spring Security baselines.

#### Q2: What is the fundamental difference between `hasRole('ADMIN')` and `hasAuthority('ROLE_ADMIN')`?
**Answer**: `hasRole('ADMIN')` automatically prepends the prefix `ROLE_` and evaluates whether the principal has the granted authority `ROLE_ADMIN`. `hasAuthority('ROLE_ADMIN')` performs an exact literal string comparison against the authority name without prepending any prefix.

#### Q3: Why is `@PostAuthorize` dangerous to use on a method that executes database deletions or billing charges?
**Answer**: `@PostAuthorize` is evaluated *after* the target method completes its execution. While an authorization failure prevents the caller from receiving the returned object, any database mutations, deletions, or external API side effects have already occurred. Mutating methods must always be protected using `@PreAuthorize`.

#### Q4: In the SpEL expression `@PreAuthorize("#request.tokens <= 4096")`, what does `#request` represent?
**Answer**: `#request` references the method input parameter named `request`. Spring's `MethodSecurityEvaluationContext` binds method parameters by name to enable runtime introspection of argument values.

#### Q5: If `RoleHierarchyImpl` is configured with `role("ADMIN").implies("PRO")` and a service method requires `@PreAuthorize("hasRole('PRO')")`, what happens when an Admin invokes it?
**Answer**: The Admin user is successfully authorized. The `RoleHierarchy` engine calculates the transitive closure of reachable authorities for `ROLE_ADMIN`, which includes `ROLE_PRO`.

---

### Hands-On Practice Exercises

#### Exercise 1: Token Quota SpEL Guard
**Task**: Create a service method `executeBatchEmbeddings(List<String> texts, String modelTier)` guarded by `@PreAuthorize`:
- Admins can embed any number of texts with any model tier.
- Pro users can embed up to 100 texts with tier `"advanced"`.
- Free users can embed up to 10 texts and only with tier `"basic"`.

```java
// Solution:
@PreAuthorize("""
    hasRole('ADMIN') or
    (hasRole('PRO_USER') and #texts.size() <= 100) or
    (hasRole('FREE_USER') and #texts.size() <= 10 and #modelTier == 'basic')
""")
public List<float[]> executeBatchEmbeddings(List<String> texts, String modelTier) {
    return embeddingService.calculate(texts, modelTier);
}
```

#### Exercise 2: Post-Filtering Diagnostic Responses
**Task**: Write a `@PostFilter` annotation on `generateCandidateResponses()` so that internal system diagnostics (`isInternalDiagnostic == true`) are filtered out unless the caller holds `ai:diagnostics:read` or `ROLE_ADMIN`.

```java
// Solution:
@PostFilter("""
    hasRole('ADMIN') or
    hasAuthority('ai:diagnostics:read') or
    !filterObject.isInternalDiagnostic()
""")
public List<CandidateResponse> generateCandidateResponses(PromptRequest prompt) {
    return llmCluster.generateCandidates(prompt);
}
```

#### Exercise 3: Custom Domain Permission Evaluator for Fine-Tuned Models
**Task**: Implement a custom Spring Security `PermissionEvaluator` so that `@PreAuthorize("hasPermission(#modelId, 'AiModel', 'DEPLOY')")` verifies if the user is listed as an owner or collaborator of that specific model.

```java
// Solution:
@Component
public class AiModelPermissionEvaluator implements PermissionEvaluator {

    private final ModelRegistryRepository modelRegistry;

    public AiModelPermissionEvaluator(ModelRegistryRepository modelRegistry) {
        this.modelRegistry = modelRegistry;
    }

    @Override
    public boolean hasPermission(Authentication auth, Object targetDomainObject, Object permission) {
        if (auth == null || targetDomainObject == null || !(permission instanceof String)) {
            return false;
        }
        if (targetDomainObject instanceof AiModelMetadata model) {
            return checkOwnership(auth.getName(), model, (String) permission);
        }
        return false;
    }

    @Override
    public boolean hasPermission(Authentication auth, Serializable targetId, String targetType, Object permission) {
        if (auth == null || targetId == null || !"AiModel".equalsIgnoreCase(targetType)) {
            return false;
        }
        AiModelMetadata model = modelRegistry.findById(targetId.toString()).orElse(null);
        return model != null && checkOwnership(auth.getName(), model, (String) permission);
    }

    private boolean checkOwnership(String username, AiModelMetadata model, String action) {
        if ("DEPLOY".equalsIgnoreCase(action)) {
            return model.getOwner().equalsIgnoreCase(username) || model.getCollaborators().contains(username);
        }
        return false;
    }
}
```

---

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [◀ Day 28: JWT Authentication](../Day_28_JWT_Authentication/Day_28_JWT_Authentication.md) | [All 60 Days Overview](../../README.md) | [Day 30: OAuth2 & Social Login ▶](../Day_30_OAuth2_Social_Login/Day_30_OAuth2_Social_Login.md) |
