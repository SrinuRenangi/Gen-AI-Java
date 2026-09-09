# Day 27: Security Fundamentals & Spring Security Architecture

> **"In a traditional CRUD app, an unauthenticated endpoint might leak user profiles. In a Generative AI platform, an unauthenticated endpoint allows automated bots to call `gpt-4o` or Claude in tight loops, burning through $50,000 of your company's credit card balance in an afternoon. In AI engineering, security is not a feature—it is a financial survival requirement."**

---

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [Day 26: PostgreSQL pgvector — Your Vector Database](../../Phase_04_Spring_Data_JPA_Database/Day_26_PostgreSQL_pgvector_Vector_Database/Day_26_PostgreSQL_pgvector_Vector_Database.md) | [All 60 Days Overview](../../README.md) | [Day 28: JWT Authentication from Scratch](../Day_28_JWT_Authentication/Day_28_JWT_Authentication.md) |

---

## Table of Contents

1. [Why This Day Matters for a 3-Year Enterprise Gen AI Engineer](#1-why-this-day-matters-for-a-3-year-enterprise-gen-ai-engineer)
2. [Real-World Analogy: Airport Customs & Boarding Gate Clearances](#2-real-world-analogy-airport-customs--boarding-gate-clearances)
3. [Authentication vs Authorization: The Fundamental Distinction](#3-authentication-vs-authorization-the-fundamental-distinction)
4. [Spring Security 6 Architecture & The Filter Chain](#4-spring-security-6-architecture--the-filter-chain)
   - [From `DelegatingFilterProxy` to `SecurityFilterChain`](#from-delegatingfilterproxy-to-securityfilterchain)
   - [The Sequence of Core Security Filters](#the-sequence-of-core-security-filters)
5. [The Core Security Domain Objects](#5-the-core-security-domain-objects)
   - [`SecurityContextHolder`](#securitycontextholder)
   - [`SecurityContext`](#securitycontext)
   - [`Authentication` (Principal, Credentials, Authorities)](#authentication-principal-credentials-authorities)
6. [Modern Spring Boot 3 Security Configuration](#6-modern-spring-boot-3-security-configuration)
   - [Why `WebSecurityConfigurerAdapter` is Dead](#why-websecurityconfigureradapter-is-dead)
   - [Building a Stateless `SecurityFilterChain` Bean](#building-a-stateless-securityfilterchain-bean)
7. [Enterprise Security Exception Handling (401 vs 403 in RFC 7807)](#7-enterprise-security-exception-handling-401-vs-403-in-rfc-7807)
8. [Hands-On Code Walkthrough](#8-hands-on-code-walkthrough)
9. [Step-by-Step Compilation & Execution](#9-step-by-step-compilation--execution)
10. [Hands-On Exercises (With Complete Solutions)](#10-hands-on-exercises-with-complete-solutions)
11. [Self-Check Quiz](#11-self-check-quiz)

---

## 1. Why This Day Matters for a 3-Year Enterprise Gen AI Engineer

Securing AI applications presents distinct challenges that standard web tutorials never cover:

1. **Financial Denial of Service (FDoS)**: Unlike standard microservices where requests cost fractions of a microsecond of CPU time, each LLM inference call incurs direct third-party billing costs ($0.03 to $0.15 per request). If an attacker finds an open endpoint, they can bankrupt an enterprise within hours.
2. **Proprietary Prompt & Model Stealing**: Your system prompts, proprietary few-shot examples, and fine-tuned model endpoints must be protected behind strict authorization boundaries.
3. **Multi-Tenant Data Isolation**: In RAG platforms, legal documents or HR records stored in `pgvector` must be inaccessible to unauthorized users. Even if the embedding search returns a high similarity score, the security filter must prevent cross-tenant exposure.
4. **Spring Security 6 Paradigm Shift**: In Spring Boot 3 / Java 21, the legacy `WebSecurityConfigurerAdapter` class was completely removed. Senior engineers configure security using modern, component-based **`SecurityFilterChain`** beans with lambda DSLs.

---

## 2. Real-World Analogy: Airport Customs & Boarding Gate Clearances

```
                             [ AIRPORT TERMINAL ]
                                      │
                                      ▼
             [ STEP 1: PASSPORT CONTROL (AUTHENTICATION) ]
             "Who are you? Prove your identity with a passport."
             Officer checks passport photo & biometric thumbprint.
                                      │
                                      ├── Pass? ──► Identity verified: "Alice Smith"
                                      └── Fail? ──► 401 UNAUTHORIZED (Turn away at border)
                                      │
                                      ▼
            [ STEP 2: BOARDING GATE ACCESS (AUTHORIZATION) ]
            "What privileges do you hold on Flight #AI-2026?"
                                      │
            ┌─────────────────────────┴─────────────────────────┐
            ▼                                                   ▼
[ Economy Seat 24B ]                                 [ Cockpit / First Class Lounge ]
(Granted: SCOPE_ai:chat)                             (Requires: ROLE_ADMIN / PILOT)
Access Granted to LLM Inference!                     Access Denied: 403 FORBIDDEN!
```

- **Authentication ("Who are you?")**: Validating that the client presenting the request is indeed who they claim to be (via password, JWT token, or API key).
- **Authorization ("What are you allowed to do?")**: Once the identity is verified, determining whether they hold the required role or authority to call this specific AI model or endpoint.

---

## 3. Authentication vs Authorization: The Fundamental Distinction

| Concept | Authentication (`AuthN`) | Authorization (`AuthZ`) |
| :--- | :--- | :--- |
| **Question** | *"Who are you?"* | *"Are you permitted to perform this action?"* |
| **Input** | Credentials (username/password, JWT, API Key, OAuth2 token) | Verified user identity + Granted Authorities (`ROLE_ADMIN`, `SCOPE_ai:write`) |
| **Failure Code** | **`HTTP 401 Unauthorized`** | **`HTTP 403 Forbidden`** |
| **Spring Interface** | `AuthenticationManager`, `AuthenticationProvider` | `AuthorizationManager`, `AccessDecisionManager` |
| **Gen AI Example** | Verifying an incoming `Authorization: Bearer <jwt>` header is cryptographically signed. | Checking if the authenticated user has permission to invoke `gpt-4o` or only cheap local models. |

---

## 🧭 The Mid-Level Java Developer Bridge: Spring Security Demystified

The first time a developer adds `spring-boot-starter-security` to their `pom.xml`, their entire app immediately returns `401 Unauthorized` and prints a random password in the console! Here is why and how it works:

| Spring Security Concept | What It Actually Does | Plain English Meaning |
| :--- | :--- | :--- |
| **Default Lockdown** | Spring Security adopts a "Zero-Trust" posture: all endpoints require login unless explicitly permitted. | A bouncer locks all doors by default until you tell them which door is public. |
| **`SecurityFilterChain`** | A pipeline of 15+ standard Java `Filter` beans that intercept every HTTP request before it reaches `@RestController`. | A series of airport security checkpoints (passport check, metal detector, baggage scan). |
| **`SecurityContextHolder`** | A static wrapper around a `ThreadLocal` variable storing the currently authenticated `UserPrincipal`. | A VIP wristband attached to the current thread while processing this HTTP request. |
| **401 vs. 403** | 401 = Unauthenticated (no valid token/credentials). 403 = Authenticated, but lacking the required role. | 401: *"I don't know who you are."*<br>403: *"I know who you are, but you aren't allowed in this room."* |
| **`csrf.disable()`** | Disables Cross-Site Request Forgery checks. | Needed for browser cookie sessions. For stateless REST APIs with JWT headers in mobile/React apps, CSRF is disabled because requests don't rely on cookies! |

---

## 4. Spring Security 6 Architecture & The Filter Chain

Spring Security is built entirely on standard Java Servlet **Filters**:

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant SC as Servlet Container (Tomcat)
    participant DFP as DelegatingFilterProxy
    participant FCP as FilterChainProxy
    participant SFC as SecurityFilterChain
    participant CTL as RestController

    Client->>SC: POST /api/v1/chat/completions (HTTP Request)
    SC->>DFP: doFilter()
    DFP->>FCP: Delegate to Spring Managed Bean
    FCP->>SFC: Route through SecurityFilterChain
    
    Note over SFC: 1. SecurityContextHolderFilter<br/>2. CorsFilter / CsrfFilter<br/>3. AuthenticationFilter (Validate Bearer Token)<br/>4. AuthorizationFilter (Check Authorities)
    
    alt All Filters Pass
        SFC->>CTL: Dispatch to Controller method
        CTL-->>Client: 200 OK + CompletionResponse
    else Authentication Fails
        SFC-->>Client: 401 Unauthorized (AuthenticationEntryPoint)
    else Authorization Fails
        SFC-->>Client: 403 Forbidden (AccessDeniedHandler)
    end
```

### The Sequence of Core Security Filters
When a request enters the `SecurityFilterChain`, it traverses these ordered filters:
1. **`SecurityContextHolderFilter`**: Loads any existing `SecurityContext` from session or previous state and attaches it to the current thread.
2. **`CorsFilter` / `CsrfFilter`**: Validates Cross-Origin headers and CSRF tokens.
3. **`AuthenticationFilter`** (e.g. `BearerTokenAuthenticationFilter`): Extracts credentials, invokes `AuthenticationManager`, and populates `SecurityContext` with the authenticated `Authentication` token.
4. **`ExceptionTranslationFilter`**: Catches Spring Security exceptions and translates them to HTTP 401 or 403 responses.
5. **`AuthorizationFilter`**: The final gatekeeper. Inspects `@PreAuthorize` rules or URL matchers. If the principal lacks the required authorities, throws `AccessDeniedException`.

---

## 5. The Core Security Domain Objects

```
┌─────────────────────────────────────────────────────────────┐
│                    SecurityContextHolder                    │
│    (Static ThreadLocal accessor: SecurityContextHolder.get) │
└──────────────────────────────┬──────────────────────────────┘
                               │ holds
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                       SecurityContext                       │
│    (Scoped to the current request thread)                   │
└──────────────────────────────┬──────────────────────────────┘
                               │ holds
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                        Authentication                       │
│    - getPrincipal(): Object (e.g. UserDetails / String)     │
│    - getCredentials(): Object (e.g. password / token)       │
│    - getAuthorities(): Collection<GrantedAuthority>         │
│    - isAuthenticated(): boolean                             │
└─────────────────────────────────────────────────────────────┘
```

### Accessing the Authenticated User in Code
```java
// Anywhere in your Spring service or controller:
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String currentUsername = auth.getName();
boolean isAdmin = auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
```

---

## 6. Modern Spring Boot 3 Security Configuration

In Spring Boot 2, developers extended `WebSecurityConfigurerAdapter` and overrode `configure(HttpSecurity http)`. **This approach is completely deprecated and removed in Spring Boot 3.**

### Modern Component-Based `SecurityFilterChain`
In Spring Boot 3 (Java 21), you register a `@Bean` returning `SecurityFilterChain`:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            // 1. Disable CSRF (Cross-Site Request Forgery) for stateless REST APIs
            .csrf(csrf -> csrf.disable())

            // 2. Configure Stateless Session Management (No HTTP Sessions / Cookies)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // 3. Define URL Authorization Rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints accessible without authentication
                .requestMatchers(
                    "/api/v1/public/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()

                // Admin endpoints require ROLE_ADMIN
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                // AI inference endpoints require specific scope
                .requestMatchers("/api/v1/chat/**").hasAuthority("SCOPE_ai:chat")
                .requestMatchers("/api/v1/rag/**").hasAuthority("SCOPE_ai:rag")

                // Any other endpoint must be authenticated
                .anyRequest().authenticated()
            )
            .build();
    }
}
```

---

## 7. Enterprise Security Exception Handling (401 vs 403 in RFC 7807)

When an unauthenticated or unauthorized client calls your AI API, Spring Security should not return raw HTML error pages. It must return **RFC 7807 Problem Details**:

### 1. `AuthenticationEntryPoint` (401 Unauthorized)
Fires when an anonymous or unauthenticated user tries to access a protected endpoint:

```java
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, 
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED.value(),
            "Full authentication credentials are required to access this AI resource"
        );
        pd.setTitle("Unauthorized");
        pd.setType(URI.create("https://api.enterprise-ai.internal/errors/unauthorized"));
        pd.setInstance(URI.create(request.getRequestURI()));

        new ObjectMapper().writeValue(response.getOutputStream(), pd);
    }
}
```

### 2. `AccessDeniedHandler` (403 Forbidden)
Fires when an authenticated user attempts to access an endpoint they lack authorities for:

```java
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, 
                       AccessDeniedException accessDeniedException) throws IOException {

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN.value(),
            "You do not possess the required authorities (e.g. ROLE_ADMIN) to perform this operation"
        );
        pd.setTitle("Forbidden");
        pd.setType(URI.create("https://api.enterprise-ai.internal/errors/forbidden"));
        pd.setInstance(URI.create(request.getRequestURI()));

        new ObjectMapper().writeValue(response.getOutputStream(), pd);
    }
}
```

---

## 8. Hands-On Code Walkthrough

In this day's companion code (`Phase_05_Spring_Security/Day_27_Security_Fundamentals_Architecture/code/`), we built:

1. **`AuthenticationToken.java`**: Implements Spring Security's `Authentication` object holding principal, credentials, and granted authorities (`ROLE_USER`, `ROLE_ADMIN`, `SCOPE_ai:chat`).
2. **`SecurityContext.java`**: Simulates `SecurityContextHolder` with `ThreadLocal` storage and mandatory cleanup.
3. **`SecurityFilterChainSimulator.java`**: Recreates the core filter pipeline:
   - `AuthenticationFilter` checking `Authorization: Bearer <token>`.
   - `AuthorizationFilter` enforcing pattern matching (`permitAll()`, `hasRole()`, `hasAuthority()`).
   - `ExceptionTranslationFilter` mapping to 401 vs 403.
4. **`SecurityArchitectureDemo.java`**: Executable test harness verifying all 5 security scenarios.

---

## 9. Step-by-Step Compilation & Execution

```powershell
# 1. Navigate to workspace
cd "c:\Users\sriva\OneDrive\Desktop\GEN AI COURSE\JAVA"

# 2. Compile Day 27 code
javac Phase_05_Spring_Security/Day_27_Security_Fundamentals_Architecture/code/*.java

# 3. Execute the Security Demo
java -cp Phase_05_Spring_Security/Day_27_Security_Fundamentals_Architecture code.SecurityArchitectureDemo
```

### Verified Output

```
================================================================================
 DAY 27: SPRING SECURITY FUNDAMENTALS, FILTER CHAIN & ACCESS CONTROL            
================================================================================

--- SCENARIO 1: Anonymous Access to Public Endpoint (permitAll) ---
  --> [FilterChainProxy] Intercepting GET /api/v1/public/health
      [AuthFilter] No bearer token. Marked as anonymousUser.
      [AuthzFilter] Public path matched: permitAll() -> Access Granted.
 Result: HTTP Status 200 -> {"status":"public_ok"}

--- SCENARIO 2: Anonymous Access to Protected LLM Chat Endpoint ---
  --> [FilterChainProxy] Intercepting POST /api/v1/chat/completions
      [AuthFilter] No bearer token. Marked as anonymousUser.
      [AuthzFilter] Access Denied: User is not authenticated -> 401 Unauthorized
 Result: HTTP Status 401 -> {"error":"Unauthorized","message":"Full authentication is required to access this resource"}

--- SCENARIO 3: Authenticated User (Alice) Calling Chat Endpoint ---
  --> [FilterChainProxy] Intercepting POST /api/v1/chat/completions
      [AuthFilter] Successfully authenticated: Authentication[principal='alice', authenticated=true, authorities=[SCOPE_ai:chat, ROLE_USER]]
      [AuthzFilter] Chat path matched: SCOPE_ai:chat verified -> Access Granted.
 Result: HTTP Status 200 -> {"id":"cmpl_9821","completion":"Access granted to LLM inference pipeline."}

--- SCENARIO 4: Regular User (Alice) Attempting Admin Model Configuration ---
  --> [FilterChainProxy] Intercepting POST /api/v1/admin/models/deploy
      [AuthFilter] Successfully authenticated: Authentication[principal='alice', authenticated=true, authorities=[SCOPE_ai:chat, ROLE_USER]]
      [AuthzFilter] Access Denied: Missing ROLE_ADMIN -> 403 Forbidden
 Result: HTTP Status 403 -> {"error":"Forbidden","message":"Access denied: Requires ROLE_ADMIN"}

--- SCENARIO 5: Admin User (Bob) Deploying AI Model ---
  --> [FilterChainProxy] Intercepting POST /api/v1/admin/models/deploy
      [AuthFilter] Successfully authenticated: Authentication[principal='bob', authenticated=true, authorities=[ROLE_USER, ROLE_ADMIN, SCOPE_ai:chat, SCOPE_ai:admin]]
      [AuthzFilter] Admin path matched: ROLE_ADMIN verified -> Access Granted.
 Result: HTTP Status 200 -> {"status":"admin_ok","action":"manage_model_deployments"}

================================================================================
 DAY 27 DEMONSTRATION COMPLETE: SECURITY FILTER CHAIN FULLY VERIFIED!           
================================================================================
```

---

## 10. Hands-On Exercises (With Complete Solutions)

### Exercise 1: Custom API Key Header Authentication Filter
**Task**: In enterprise B2B setups, partner microservices call your AI gateway using an API Key header: `X-API-Key: ak_enterprise_secret_99`. Write a custom Spring `OncePerRequestFilter` that inspects this header, verifies it against a service, and populates the `SecurityContext`.

#### Solution:
```java
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-API-Key";
    private final ApiKeyVerificationService apiKeyService;

    public ApiKeyAuthenticationFilter(ApiKeyVerificationService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {

        String apiKey = request.getHeader(API_KEY_HEADER);

        if (apiKey != null && !apiKey.isBlank()) {
            Optional<TenantIdentity> tenant = apiKeyService.verifyApiKey(apiKey);
            if (tenant.isPresent()) {
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT"), new SimpleGrantedAuthority("SCOPE_ai:infer"));
                var authentication = new PreAuthenticatedAuthenticationToken(tenant.get().tenantId(), apiKey, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext(); // Prevent thread leak!
        }
    }
}
```

---

### Exercise 2: Registering Custom Filter in `SecurityFilterChain`
**Task**: How do you register `ApiKeyAuthenticationFilter` to execute **before** `UsernamePasswordAuthenticationFilter` in Spring Boot 3?

#### Solution:
```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http, ApiKeyAuthenticationFilter apiKeyFilter) throws Exception {
    return http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class)
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/public/**").permitAll()
            .requestMatchers("/api/v1/infer/**").hasAuthority("SCOPE_ai:infer")
            .anyRequest().authenticated()
        )
        .build();
}
```

---

### Exercise 3: Role vs Authority Syntax in Spring Security
**Task**: What is the difference between `hasRole("ADMIN")` and `hasAuthority("ROLE_ADMIN")`?

#### Solution:
```java
// In Spring Security:
// hasRole("ADMIN") automatically prepends the prefix "ROLE_" to the string!
// It looks for a GrantedAuthority named "ROLE_ADMIN".

// hasAuthority("ROLE_ADMIN") looks for the EXACT string match "ROLE_ADMIN".
// hasAuthority("SCOPE_ai:chat") looks for the exact permission string.

// Therefore:
.hasRole("ADMIN") == .hasAuthority("ROLE_ADMIN") // True!
```

---

## 11. Self-Check Quiz

### Q1: Why is CSRF disabled (`csrf.disable()`) in stateless REST APIs?
> **Answer**: CSRF attacks rely on the browser automatically attaching saved session cookies (`JSESSIONID`) to cross-origin requests. Modern stateless REST APIs authenticate via `Authorization: Bearer <token>` headers stored in memory (not ambient browser cookies). Because browsers never automatically attach custom Authorization headers to cross-origin requests, CSRF attacks are physically impossible, making CSRF tokens redundant.

### Q2: Why was `WebSecurityConfigurerAdapter` removed in Spring Boot 3?
> **Answer**: `WebSecurityConfigurerAdapter` forced applications into inheritance-based configuration, making it difficult to compose multiple security configurations, apply conditional filters, or maintain clean bean lifecycle boundaries. Spring Security 6 replaced it with component-based `@Bean` declarations (`SecurityFilterChain`), providing superior modularity and lambda-based configuration DSLs.

### Q3: What is the purpose of `DelegatingFilterProxy` in the Servlet Container?
> **Answer**: Standard Servlet Containers (like Tomcat) are created outside the Spring `ApplicationContext` and do not know about Spring beans. `DelegatingFilterProxy` is a standard servlet filter registered in Tomcat that acts as a bridge: it intercepts incoming HTTP requests and delegates them to the Spring-managed `FilterChainProxy` bean.

### Q4: When does an application return `401 Unauthorized` versus `403 Forbidden`?
> **Answer**: An application returns `401 Unauthorized` when the user has **not provided valid authentication credentials** (e.g. missing or expired token). It returns `403 Forbidden` when the user's identity is **verified and authenticated**, but they lack the required permissions or roles to access the requested resource.

### Q5: Why is `SecurityContextHolder.clearContext()` essential in asynchronous or pooled-thread environments?
> **Answer**: `SecurityContextHolder` stores authentication state in a `ThreadLocal`. In web servers using thread pools (or virtual thread dispatchers), threads are recycled. If you fail to clear the security context at the end of the request, the next unrelated client request handled by that recycled thread will inherit the previous user's security context, causing severe privilege escalation security breaches.

---

### What's Next?

We have mastered the Spring Security architecture, filter chain mechanics, and modern stateless configuration. But how do we issue, sign, verify, and rotate secure cryptographically verified tokens for our AI users?

Proceed to **[Day 28: JWT Authentication from Scratch (jjwt, Claims, Signature Verification)](../Day_28_JWT_Authentication/Day_28_JWT_Authentication.md)**!
