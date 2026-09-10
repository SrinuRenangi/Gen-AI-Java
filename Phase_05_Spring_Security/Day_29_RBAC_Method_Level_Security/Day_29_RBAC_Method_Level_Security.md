# Day 29: Role-Based Access Control (RBAC) & Method-Level Security
## Securing AI Business Methods, SpEL Authorization, Role Hierarchies & Multi-Tenant RAG Isolation

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [◀ Day 28: JWT Authentication](../Day_28_JWT_Authentication/Day_28_JWT_Authentication.md) | [All 60 Days Overview](../../README.md) | [Day 30: OAuth2 & Social Login ▶](../Day_30_OAuth2_Social_Login/Day_30_OAuth2_Social_Login.md) |

---

## Friendly Welcome: Locking the Cockpit Door

Hey there, friend! Welcome to Day 29.

Yesterday, we gave our users digital passport wristbands (JWTs). That gets them through the front entrance gate of the airport terminal.

Now ask yourself: Once Alice is inside the airport terminal, can she just stroll onto any random airplane and walk into the cockpit to fly the plane?

Of course not! Getting through the airport gate only proves *who you are*. To get into the cockpit, you need flight credentials and a security badge that says `ROLE_CAPTAIN`.

In our AI applications, we have basic features (like asking a question to a small, free local model) and very expensive, sensitive features (like running GPT-4o on a huge dataset or deleting vector indexes).

Today, we are going to learn **Method-Level Security** and **Role-Based Access Control (RBAC)**. With a single line of Java code like `@PreAuthorize("hasRole('ADMIN')")`, Spring puts a locked cockpit door right in front of your critical Java methods!

---

> 💡 **New Word Alert! Key Concepts for Today**
>
> - **RBAC (Role-Based Access Control)**: A security system where permissions are grouped into roles (such as `ROLE_FREE`, `ROLE_PRO`, `ROLE_ADMIN`), and users are assigned roles based on their subscription tier or company job.
> - **Method-Level Security**: Placing security guards directly on Java service methods rather than only checking HTTP URLs. This ensures that even if internal code or background tasks invoke the method, unauthorized callers are stopped immediately.
> - **`@PreAuthorize`**: A Spring annotation placed on a Java method. It checks if the caller has the required permissions *before* the method executes. If they don't, it immediately throws `AccessDeniedException` (HTTP 403).
> - **SpEL (Spring Expression Language)**: A powerful expression language that lets you write smart dynamic rules right inside annotations, like `@PreAuthorize("hasRole('PRO') and #tokens <= 4096")`.
> - **Role Hierarchy**: A smart inheritance tree that teaches Spring: *"An ADMIN can do everything a PRO user can do, and a PRO user can do everything a FREE user can do."* This saves you from writing tedious duplicate checks everywhere!
> - **Multi-Tenant Isolation**: Ensuring that Company A's AI agents can NEVER see, search, or access Company B's private documents and embeddings stored in the vector database.

---

## What Will You Learn Today?

Yesterday, you mastered how to authenticate users at the perimeter using stateless JSON Web Tokens (JWT) and establish a valid `SecurityContext`. But authentication only answers: *"Who are you?"* It does not answer: *"Are you permitted to invoke GPT-4o with 32,000 tokens?"* or *"Are you allowed to delete this vector index or view another company's embeddings?"*

Today, you will master **Method-Level Security** and **Role-Based Access Control (RBAC)** in Spring Security 6 (Spring Boot 3) using Java 21:
- Why perimeter HTTP URL filtering (`antMatchers` / `requestMatchers`) is dangerously inadequate for AI systems.
- The internal architecture of Spring Security AOP method interception (`AuthorizationManagerBeforeMethodInterceptor`).
- Modern `@EnableMethodSecurity` and the declarative annotations: `@PreAuthorize`, `@PostAuthorize`, `@PreFilter`, and `@PostFilter`.
- Mastering Spring Expression Language (**SpEL**) for contextual access rules, parameter validation, and object-level permissions.
- Designing **Role Hierarchies** (`RoleHierarchyImpl`) so administrative roles cleanly inherit lower-tier permissions without boilerplate.
- Multi-tenant data isolation: Preventing cross-tenant vector database breaches in enterprise RAG pipelines.
- Building custom `PermissionEvaluator` and domain-level security beans.

---

## Real-World Analogy: Airport Security vs. Cockpit Door

Imagine visiting an international airport:

```
+---------------------------------------------------------------------------------------------------+
|                                       THE TWO-TIER DEFENSE                                       |
|                                                                                                   |
|  TIER 1: HTTP URL Security (The Airport Terminal Gate)                                            |
|  - Checks: Valid ticket and passport (JWT Token).                                                 |
|  - Result: You are allowed into the terminal duty-free lounge.                                    |
|  - Limitation: Every passenger (Economy, Business, First Class, Pilot) enters through this gate. |
|                                                                                                   |
|  TIER 2: Method-Level Security (The Reinforced Cockpit Door)                                      |
|  - Checks: Biometric scanner, flight credentials, keycard (Method Authorization).                 |
|  - Result: Only certified pilots holding `ROLE_CAPTAIN` may touch the throttle controls!          |
|  - Defense: Even if a passenger bypasses the gate, they CANNOT enter the cockpit!                 |
+---------------------------------------------------------------------------------------------------+
```

In an enterprise Generative AI application:
- **URL Security (`/api/v1/ai/**`)** is like the airport terminal gate. It ensures that whoever hits your API has a signed JWT.
- **Method-Level Security (`@PreAuthorize`)** is the locked cockpit door. Inside a single controller endpoint, a user might request a standard lightweight model, a prompt execution against a proprietary vector index, or a $2.00-per-call multimodal reasoning model. Method-level security intercepts the method call inside your service layer, evaluates the caller's roles, quotas, and tenant boundaries, and slams the door shut (`403 Forbidden`) before a single cent of LLM compute is spent!

---

## Under-the-Hood Architecture: Spring AOP Method Interception

How does Spring Security stop a Java method from executing when an unauthorized user calls it?

Spring Security leverages **Aspect-Oriented Programming (AOP)** and **Dynamic Proxies**. When you annotate a Spring service with `@PreAuthorize`, Spring does not expose the raw instance of your service to controllers. Instead, it wraps your service in a CGLIB or JDK dynamic proxy.

```
                           SPRING SECURITY AOP INTERCEPTION PIPELINE
                           
 Caller (e.g. Controller)
           │
           │ 1. Invokes aiModelService.executeGpt4o(request)
           ▼
┌────────────────────────────────────────────────────────────────────────┐
│ Spring AOP Dynamic Proxy (AiModelService$$SpringCGLIB$$0)              │
│                                                                        │
│   2. Intercepts call via AuthorizationManagerBeforeMethodInterceptor   │
│                                                                        │
│   3. Extracts SecurityContext from ThreadLocal                         │
│      Authentication auth = SecurityContextHolder.getContext()          │
│                                                                        │
│   4. Evaluates SpEL Expression:                                        │
│      "@PreAuthorize("hasRole('PRO') and #tokens <= 4096")"              │
│                                                                        │
│   5. RoleHierarchy Check:                                              │
│      Expands direct roles (ROLE_ADMIN -> ROLE_PRO -> ROLE_FREE)        │
│                                                                        │
│   6. Decision:                                                         │
│      ├── AuthorizationDecision(false)                                  │
│      │     └── 💥 Throws AccessDeniedException                         │
│      │           (Method execution aborted immediately)                │
│      │                                                                 │
│      └── AuthorizationDecision(true)                                   │
│            └── ✅ Delegates to Target:                                 │
│                  AiModelServiceImpl.executeGpt4o(request)              │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
                                   ▼
              Target Bean Execution (Actual LLM API Call)
```

### Key Components of the Method Security Pipeline:
1. **`AuthorizationManagerBeforeMethodInterceptor`**: The core AOP advisor registered when `@EnableMethodSecurity` is enabled. It intercepts calls *before* method entry.
2. **`MethodSecurityExpressionHandler`**: Parses and evaluates SpEL expressions in the context of the method call, binding method arguments (`#prompt`, `#tokens`) and the security context (`authentication`, `principal`).
3. **`RoleHierarchy`**: Maps high-level roles to low-level privileges so you don't have to duplicate role checks everywhere.
4. **`AccessDeniedException`**: A runtime exception thrown if authorization fails. Spring MVC's exception handler translates this into an HTTP `403 Forbidden` response.

---

## Modern Spring Security 6 Configuration: `@EnableMethodSecurity`

> [!IMPORTANT]
> **Spring Boot 2 vs. Spring Boot 3 Migration Notice:**
> In Spring Boot 2 (Spring Security 5), you used `@EnableGlobalMethodSecurity(prePostEnabled = true)`.
> In Spring Boot 3 (Spring Security 6), `@EnableGlobalMethodSecurity` is **deprecated**. You must use `@EnableMethodSecurity`.

By default in Spring Boot 3, `@EnableMethodSecurity` enables `@PreAuthorize` and `@PostAuthorize` out of the box!

```java
package com.genai.security.config;

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
    prePostEnabled = true,   // Enables @PreAuthorize, @PostAuthorize, @PreFilter, @PostFilter (default: true)
    securedEnabled = true,   // Enables legacy @Secured (default: false)
    jsr250Enabled = true     // Enables @RolesAllowed from jakarta.annotation (default: false)
)
public class SecurityMethodConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**", "/actuator/health").permitAll()
                .anyRequest().authenticated() // All endpoints require authentication; methods check granular RBAC
            )
            // JWT filter configured here...
            .build();
    }

    /**
     * Define the Enterprise AI Role Hierarchy:
     * ROLE_ADMIN automatically inherits all capabilities of ROLE_LEAD_ENGINEER,
     * which inherits ROLE_PRO_USER, which inherits ROLE_FREE_USER.
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

## Roles vs. Authorities: The Distinction That Confuses Everyone

Many developers treat Roles and Authorities as identical strings. In Spring Security, they follow a strict convention:

| Feature | Role | Authority (Permission) |
|:---|:---|:---|
| **Naming Prefix** | Must start with `ROLE_` (e.g., `ROLE_ADMIN`, `ROLE_PRO_USER`). | Freeform string (e.g., `ai:model:gpt-4o`, `vector:index:drop`). |
| **Spring Check** | `hasRole('ADMIN')` (automatically prepends `ROLE_`). | `hasAuthority('ai:model:gpt-4o')` (exact literal match). |
| **Granularity** | Coarse-grained job title or subscription tier. | Fine-grained specific action capability. |
| **Best Practice** | Assign Roles to Users. | Assign Authorities to Roles, check Authorities on Methods! |

### Real-World Gen AI Example:
A user subscribes to the **"Pro Tier"** (`ROLE_PRO_USER`). The system assigns authorities:
- `ai:model:standard`
- `ai:model:gpt-4o`
- `rag:document:read`
- `rag:document:upload`

An **Enterprise Admin** (`ROLE_ADMIN`) additionally has:
- `vector:index:drop`
- `ai:finetuning:trigger`
- `audit:logs:export`

---

## Declarative Method Security Annotations

Spring provides four primary method security annotations:

### 1. `@PreAuthorize`: The Ultimate Workhorse
Evaluated **before** the method runs. If the condition evaluates to `false`, the method is never invoked.

```java
// Check role
@PreAuthorize("hasRole('PRO_USER')")
public AiResponse generateReport(ReportRequest request) { ... }

// Check granular authority
@PreAuthorize("hasAuthority('ai:model:gpt-4o')")
public ChatResponse queryGpt4o(Prompt prompt) { ... }

// SpEL argument inspection: Free users cannot request more than 512 tokens
@PreAuthorize("hasRole('PRO_USER') or #request.maxTokens <= 512")
public ChatResponse executeInference(InferenceRequest request) { ... }
```

### 2. `@PostAuthorize`: Inspecting the Return Value
Evaluated **after** the method completes successfully, but **before** returning the result to the caller. You have access to the special SpEL variable `returnObject`.

```java
// Ensure an AI agent output does not leak another tenant's confidential document
@PostAuthorize("returnObject.tenantId == authentication.principal.tenantId or hasRole('ADMIN')")
public RagDocument fetchDocumentById(String documentId) {
    return documentRepository.findById(documentId)
        .orElseThrow(() -> new DocumentNotFoundException(documentId));
}
```
> [!CAUTION]
> If `@PostAuthorize` fails, an `AccessDeniedException` is thrown and the client receives `403`. However, the method **did run**! Do NOT use `@PostAuthorize` on methods that perform destructive state mutations (like `deleteDocument` or billing charges). Use it strictly on read operations where the returned object's metadata determines authorization.

### 3. `@PreFilter`: Filtering Input Collections
Filters elements out of an input collection parameter before the method executes. It uses `filterObject` in SpEL.

```java
// Filters out any document chunk that does not belong to the user's tenant
@PreFilter("filterObject.tenantId == authentication.principal.tenantId")
public void indexDocumentChunks(List<DocumentChunk> chunks) {
    // chunks only contains records belonging to the caller's tenant!
    vectorStore.saveAll(chunks);
}
```

### 4. `@PostFilter`: Filtering Output Collections
Filters elements out of a returned collection before handing it back to the caller.

```java
// Returns only the vector documents the user is authorized to read
@PostFilter("hasRole('ADMIN') or filterObject.classificationLevel != 'TOP_SECRET'")
public List<VectorSearchResult> searchKnowledgeBase(String query) {
    return vectorStore.similaritySearch(query);
}
```

---

## Spring Expression Language (SpEL) in Security

SpEL gives you expressive power directly inside annotations. Here is your reference guide:

| SpEL Expression | Description |
|:---|:---|
| `hasRole('ADMIN')` | Returns true if principal has authority `ROLE_ADMIN`. |
| `hasAnyRole('ADMIN', 'PRO')` | Returns true if principal has any of the listed roles. |
| `hasAuthority('ai:model:execute')` | Returns true if principal has the exact authority. |
| `hasAnyAuthority('perm1', 'perm2')`| Returns true if principal has any of the listed authorities. |
| `isAuthenticated()` | Returns true if the user is authenticated (not anonymous). |
| `isAnonymous()` | Returns true if the user is an anonymous guest. |
| `principal` | Direct reference to the authenticated principal object. |
| `authentication` | The current `Authentication` object from `SecurityContext`. |
| `#paramName` | References a method parameter named `paramName`. |
| `#request.tenantId` | Reads property `tenantId` from method parameter `request`. |
| `@mySecurityService.check(...)` | Invokes a method on a registered Spring Bean! |

---

## Multi-Tenant RAG Isolation: Securing the Vector Database

One of the most dangerous vulnerabilities in Generative AI architectures is **Cross-Tenant Vector Leakage**.

When Company A and Company B share a PostgreSQL `pgvector` database, an unauthorized query or flawed prompt must NEVER retrieve vector embeddings belonging to another organization.

```
                               CROSS-TENANT ISOLATION ARCHITECTURE
                               
 User JWT (Tenant: "ACME")
           │
           │ Invokes: vectorService.similaritySearch("ACME", "pricing_2026", 10)
           ▼
┌────────────────────────────────────────────────────────────────────────┐
│ @PreAuthorize("#tenantId == principal.tenantId or hasRole('ADMIN')")   │
│                                                                        │
│ Caller Tenant: "ACME"                                                  │
│ Target Tenant: "GLOBEX"                                                │
│                                                                        │
│ ACME == GLOBEX -> FALSE                                                │
│ is Admin?     -> FALSE                                                 │
│                                                                        │
│ ❌ ACCESS DENIED! 403 Forbidden                                         │
│ (PostgreSQL query is NEVER fired; embeddings remain 100% isolated)    │
└────────────────────────────────────────────────────────────────────────┘
```

Let's look at how to implement this cleanly using a custom Spring Security expression bean:

```java
package com.genai.security.rbac;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Custom Spring Bean referenced in SpEL via:
 * @PreAuthorize("@tenantSecurity.canAccessTenant(#tenantId)")
 */
@Component("tenantSecurity")
public class TenantSecurityExpressionService {

    public boolean canAccessTenant(String requestedTenantId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        // Admins can access all tenant workspaces for maintenance
        boolean isAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return true;
        }

        // Extract tenant ID stored inside custom UserPrincipal
        if (auth.getPrincipal() instanceof CustomUserPrincipal user) {
            return user.tenantId().equalsIgnoreCase(requestedTenantId);
        }

        return false;
    }
}
```

And guard your Vector Knowledge Base service with it:

```java
@Service
public class VectorKnowledgeBaseService {

    @PreAuthorize("@tenantSecurity.canAccessTenant(#tenantId)")
    public List<VectorChunk> queryDocuments(String tenantId, String query, int topK) {
        // Safe to execute; isolation is guaranteed at the AOP layer
        return vectorStoreRepository.searchByTenant(tenantId, query, topK);
    }
}
```

---

## Step-by-Step Production Code Walkthrough

Let's inspect the runnable companion code built for today's lesson in `Phase_05_Spring_Security/Day_29_RBAC_Method_Level_Security/code/`:

### 1. `SecurityContext.java` & `Authentication`
Encapsulates thread-local authentication state with immutable authorities:

```java
public record UserPrincipal(String username, String tenantId, String email) {}

public record Authentication(
        UserPrincipal principal,
        Set<String> authorities,
        boolean authenticated
) {
    public boolean hasAuthority(String authority) {
        return authorities.contains(authority);
    }
}
```

### 2. `RoleHierarchy.java`
Models Spring Security's `RoleHierarchyImpl`, computing the transitive closure of reachable roles:

```java
public class RoleHierarchy {
    private final Map<String, Set<String>> hierarchyMap = new HashMap<>();

    public void addHierarchy(String parentRole, String... childRoles) {
        hierarchyMap.computeIfAbsent(parentRole, k -> new HashSet<>())
                    .addAll(Arrays.asList(childRoles));
    }

    public Set<String> getReachableAuthorities(Collection<String> directAuthorities) {
        Set<String> reachable = new HashSet<>(directAuthorities);
        Queue<String> queue = new LinkedList<>(directAuthorities);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            Set<String> children = hierarchyMap.get(current);
            if (children != null) {
                for (String child : children) {
                    if (reachable.add(child)) {
                        queue.add(child);
                    }
                }
            }
        }
        return Collections.unmodifiableSet(reachable);
    }
}
```

### 3. `SecurityProxyFactory.java`
Implements Spring Security's exact AOP Method Interceptor pattern using `java.lang.reflect.Proxy`:

```java
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

    // 4. Multi-Tenant isolation guard
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

    // Pass -> Invoke concrete implementation
    return method.invoke(target, args);
}
```

### 4. Running the Complete Verification Test Suite
Run the companion code directly from your terminal:

```bash
javac -d out Phase_05_Spring_Security/Day_29_RBAC_Method_Level_Security/code/*.java
java -cp out com.genai.security.rbac.RbacDemo
```

Output:
```text
================================================================================
  DAY 29: METHOD-LEVEL SECURITY & RBAC DEMONSTRATION (SPRING SECURITY AOP)      
================================================================================

[TEST 1] Invocation without authentication context...
  ✅ REJECTED AS EXPECTED: 401 Unauthorized: No active authenticated security context.

[TEST 2] Free Tier User attempts GPT-4o inference...
  ✅ REJECTED AS EXPECTED: 403 Forbidden: User 'alice_free' lacks required role [ROLE_PRO_USER, ROLE_ADMIN] for method 'executeGpt4o'. Effective roles: [ROLE_FREE_USER, ai:model:standard]

[TEST 3] Free Tier User invokes Standard model...
  ✅ SUCCESS: [Standard LLM Response] Completed inference for: "What is Java 21?"

[TEST 4] Pro User invokes GPT-4o...
  ✅ SUCCESS: [GPT-4o Response] Generated 128 tokens for: "Write a Spring Boot 3 security filter" (Max allowed: 2048)

[TEST 5] Pro User attempts to drop Vector Index...
  ✅ REJECTED AS EXPECTED: 403 Forbidden: User 'bob_pro' lacks required role [ROLE_ADMIN] for method 'deleteVectorIndex'. Effective roles: [ROLE_FREE_USER, ROLE_PRO_USER, ai:model:standard]

[TEST 6] Admin invokes deleteVectorIndex...
  [DATABASE AUDIT] Vector index 'embeddings_v3_cosine' permanently dropped by Admin.
  ✅ SUCCESS: Admin authorized to drop vector index.

[TEST 7] Admin invokes GPT-4o (Inherited via Role Hierarchy)...
  ✅ SUCCESS via Hierarchy: [GPT-4o Response] Generated 128 tokens for: "Benchmark token throughput across clusters" (Max allowed: 8192)

[TEST 8] Multi-Tenant Isolation: ACME_CORP user queries GLOBEX KB...
  ✅ BLOCKED CROSS-TENANT BREACH: 403 Forbidden: Multi-tenant boundary violation! User 'charlie_acme' (Tenant: 'ACME_CORP') attempted to access Tenant: 'GLOBEX_CORP'

[TEST 9] User queries their OWN tenant KB (ACME_CORP)...
  ✅ SUCCESS: [KnowledgeBase RAG] Returned 3 vector chunks for Tenant [ACME_CORP] with query: "Retrieve internal onboarding guide"

================================================================================
  ALL METHOD SECURITY & RBAC CHECKS PASSED PERFECTLY!                           
================================================================================
```

---

## Why It Matters for Gen AI Applications

| Risk / Attack Vector | Without Method-Level RBAC | With Method-Level RBAC |
|:---|:---|:---|
| **Token Budget Exhaustion** | Any authenticated user calls the generic `/chat` endpoint and consumes expensive reasoning tokens (o1, GPT-4o, Claude Opus). | `@PreAuthorize` restricts high-cost models strictly to `ROLE_ENTERPRISE` and checks max token arguments via SpEL. |
| **RAG Multi-Tenant Data Leak** | A malicious or confused user manipulates the `tenantId` query parameter to extract another enterprise's vector embeddings. | `@PreAuthorize("@tenantSecurity.canAccessTenant(#tenantId)")` stops the call at the proxy before the DB query executes. |
| **Accidental Index Deletion** | A standard engineer endpoint triggers vector store re-indexing, destroying production embeddings. | `@PreAuthorize("hasRole('ADMIN')")` protects destructive vector administrative methods. |
| **AI Agent Tool Calling Overreach** | An Autonomous LLM agent with Tool Calling executes sensitive system operations (e.g. `deleteDatabase()`). | When the agent invokes Java tools, each tool method enforces RBAC on behalf of the user who initiated the session. |

---

## Hands-On Exercises (With Complete Solutions)

### Exercise 1: Token Quota SpEL Guard
**Problem Statement:**  
Create a service method `executeBatchEmbeddings(List<String> texts, String modelTier)` where:
- Users with `ROLE_ADMIN` can embed any number of texts with any model tier.
- Users with `ROLE_PRO_USER` can embed up to 100 texts with tier `"advanced"`.
- Users with `ROLE_FREE_USER` can embed up to 10 texts and only with tier `"basic"`.

Write the `@PreAuthorize` SpEL expression.

<details>
<summary>👉 View Solution</summary>

```java
@PreAuthorize("""
    hasRole('ADMIN') or
    (hasRole('PRO_USER') and #texts.size() <= 100) or
    (hasRole('FREE_USER') and #texts.size() <= 10 and #modelTier == 'basic')
""")
public List<float[]> executeBatchEmbeddings(List<String> texts, String modelTier) {
    return embeddingService.calculate(texts, modelTier);
}
```
*Explanation:* SpEL allows boolean logic (`or`, `and`) combined with method argument parameter evaluation (`#texts.size()`) and string comparison (`#modelTier == 'basic'`).
</details>

---

### Exercise 2: Post-Filtering Toxic or Uncensored AI Outputs
**Problem Statement:**  
An enterprise AI application returns candidate chat responses generated by diverse fine-tuned models. Certain responses contain sensitive internal system diagnostics (`isInternalDiagnostic == true`). Only users with authority `ai:diagnostics:read` or `ROLE_ADMIN` should receive these diagnostic responses. Standard users must have them automatically removed from the returned list.

Write the `@PostFilter` annotation on `generateCandidateResponses()`.

<details>
<summary>👉 View Solution</summary>

```java
@PostFilter("""
    hasRole('ADMIN') or
    hasAuthority('ai:diagnostics:read') or
    !filterObject.isInternalDiagnostic()
""")
public List<CandidateResponse> generateCandidateResponses(PromptRequest prompt) {
    return llmCluster.generateCandidates(prompt);
}
```
*Explanation:* `@PostFilter` inspects each item in the returned `List<CandidateResponse>` as `filterObject`. Any item where the SpEL expression evaluates to `false` is automatically removed from the collection before returning it to the caller.
</details>

---

### Exercise 3: Custom Domain Permission Evaluator for Fine-Tuned Models
**Problem Statement:**  
Implement a custom Spring Security `PermissionEvaluator` so that `@PreAuthorize("hasPermission(#modelId, 'AiModel', 'DEPLOY')")` verifies if the user is listed as an owner or collaborator of that specific fine-tuned AI model.

<details>
<summary>👉 View Solution</summary>

```java
package com.genai.security.rbac;

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import java.io.Serializable;

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
        // Direct domain object check
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

        String modelId = targetId.toString();
        AiModelMetadata model = modelRegistry.findById(modelId).orElse(null);
        if (model == null) {
            return false;
        }

        return checkOwnership(auth.getName(), model, (String) permission);
    }

    private boolean checkOwnership(String username, AiModelMetadata model, String action) {
        if ("DEPLOY".equalsIgnoreCase(action)) {
            return model.getOwner().equalsIgnoreCase(username) || model.getCollaborators().contains(username);
        }
        return false;
    }
}
```
</details>

---

## 5-Question Self-Check Quiz

#### 1. In Spring Security 6 / Spring Boot 3, which annotation must you place on a `@Configuration` class to enable `@PreAuthorize`?
- A) `@EnableGlobalMethodSecurity(prePostEnabled = true)`
- B) `@EnableMethodSecurity`
- C) `@EnableAspectJAutoProxy`
- D) `@SecuredConfiguration`

#### 2. What is the fundamental difference between `hasRole('ADMIN')` and `hasAuthority('ROLE_ADMIN')`?
- A) `hasRole` searches LDAP, while `hasAuthority` searches a local database.
- B) `hasRole('ADMIN')` automatically checks for the authority `ROLE_ADMIN`, whereas `hasAuthority` requires the exact string match.
- C) `hasRole` only works with XML configurations.
- D) There is no difference; they are exact aliases.

#### 3. Why is `@PostAuthorize` dangerous to use on a method that bills a user's credit card or deletes a vector table?
- A) `@PostAuthorize` does not work with Spring Data repositories.
- B) The method executes *before* `@PostAuthorize` evaluates. If authorization fails, the billing or deletion has already happened!
- C) `@PostAuthorize` only supports boolean return types.
- D) It causes a deadlock in PostgreSQL transactions.

#### 4. In the SpEL expression `@PreAuthorize("#request.tokens <= 4096")`, what does `#request` represent?
- A) The HTTP Servlet Request (`HttpServletRequest`).
- B) The name of the method parameter named `request`.
- C) A session-scoped Spring Bean named `request`.
- D) An HTTP Header named `request`.

#### 5. If `RoleHierarchyImpl` is configured with `role("ADMIN").implies("PRO")` and a service method requires `@PreAuthorize("hasRole('PRO')")`:
- A) Users with `ROLE_ADMIN` are rejected because they do not have `ROLE_PRO`.
- B) Spring throws an `AmbiguousRoleException`.
- C) Users with `ROLE_ADMIN` are successfully authorized because `ADMIN` transitively implies `PRO`.
- D) The application fails to start.

---

### Quiz Answers & Explanations

1. **B is correct**: In Spring Boot 3 / Spring Security 6, `@EnableMethodSecurity` is the modern standard; `@EnableGlobalMethodSecurity` is deprecated.
2. **B is correct**: Spring Security's `hasRole('X')` automatically adds the `ROLE_` prefix and looks for `ROLE_X` in the user's `GrantedAuthority` set. `hasAuthority('X')` matches the string literally.
3. **B is correct**: `@PostAuthorize` is evaluated *after* target method invocation. While it prevents the caller from seeing the return value, the side effects in the database or payment gateway have already completed!
4. **B is correct**: `#variableName` in SpEL accesses the method's input parameters by name.
5. **C is correct**: `RoleHierarchy` automatically maps reachable authorities so higher-level roles inherit lower-level permissions seamlessly.

---

## Day 29 Wrap-Up & What's Next

You've just added military-grade authorization to your AI platform! 

Let's review the big milestones from today:
- **Cockpit Door Protection**: URL security gets users into the terminal, but `@PreAuthorize` keeps unauthorized users out of sensitive, expensive Java methods.
- **Dynamic Rules with SpEL**: You can write rules that inspect incoming tokens, parameters, and quotas right inside annotations (`#tokens <= 4096`).
- **Role Hierarchies Save Time**: An `ADMIN` inherits permissions from `PRO` and `FREE` tiers automatically.
- **Multi-Tenant Safety**: Never allow one tenant's queries to accidentally search another company's private vector documents!

### What's Coming Up Next?
We have built custom username/password login and JWT token checks. But in real life, users hate creating a new password for every website. They expect a clean button: **"Sign in with Google"** or **"Sign in with GitHub"**!

Tomorrow in **[Day 30: OAuth2 & Social Login (OpenID Connect, Resource Server)](../Day_30_OAuth2_Social_Login/Day_30_OAuth2_Social_Login.md)**, you will learn how OAuth2 and OpenID Connect work under the hood. You'll allow users to sign in with their existing Google or GitHub accounts while your Spring Boot backend acts as a secure, modern Resource Server. You're doing amazing!

