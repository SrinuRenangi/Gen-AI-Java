# Day 30: OAuth2 & Social Login (OpenID Connect, Resource Server)
## Google/GitHub SSO, Asymmetric RS256 Validation, JWKS & M2M AI Token Security

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [◀ Day 29: Role-Based Access Control (RBAC)](../Day_29_RBAC_Method_Level_Security/Day_29_RBAC_Method_Level_Security.md) | [All 60 Days Overview](../../README.md) | [Day 31: Rate Limiting, CORS & API Security ▶](../Day_31_Rate_Limiting_CORS_API_Security/Day_31_Rate_Limiting_CORS_API_Security.md) |

---

## Friendly Welcome: Sign In with Google & Enterprise Identity

Hey there, friend! Welcome to Day 30.

When you sign up for a new AI tool or web application today, how often do you fill out a 10-field form with a new username, password, and security questions?

Almost never! You look for that big, reassuring button: **"Sign in with Google"** or **"Sign in with GitHub"**.

Why? Because you already trust Google or GitHub to protect your credentials, and you don't want another password to remember. And as developers, we *love* social login because we don't have to store sensitive user passwords in our database!

In modern software engineering, this is powered by **OAuth 2.0** and **OpenID Connect (OIDC)**. Today, you'll learn how your Spring Boot backend can act as an **OAuth2 Resource Server**—validating tokens signed by Google, GitHub, Okta, or Keycloak using public cryptographic keys, completely seamlessly!

---

> 💡 **New Word Alert! Key Concepts for Today**
>
> - **OAuth 2.0**: The industry standard for *delegated authorization* ("What is this app allowed to do on my behalf?"). Think of it like a valet parking key: it lets the valet start the engine and park the car, but it won't unlock the glove compartment or the trunk.
> - **OpenID Connect (OIDC)**: A friendly identity layer built right on top of OAuth 2.0 for *authentication* ("Who are you?"). It gives your app a verified **ID Token** with the user's name, email, and avatar.
> - **Resource Server**: In OAuth2 terminology, this is **your Spring Boot backend**! It hosts the actual protected resources (your AI endpoints and database).
> - **Identity Provider (IdP) / Authorization Server**: The trusted authority that handles logins and issues tokens (like Google Identity, GitHub, Keycloak, or Okta).
> - **Asymmetric Cryptography (RS256)**: A security system that uses two matching keys:
>   - A **Private Key** (kept top secret by Google to sign tokens).
>   - A **Public Key** (freely shared with the world so your Spring Boot server can verify that the token really came from Google, without needing to know Google's secret!).
> - **JWKS (JSON Web Key Set)**: A public web endpoint (`/.well-known/jwks.json`) where your Spring Boot server automatically downloads and refreshes the public keys to verify incoming JWTs.

---

## What Will You Learn Today?

In Days 27–29, you built perimeter security and method-level access control. But in real-world enterprise applications, you almost never maintain your own plain username/password tables or symmetric HMAC secrets shared between dozens of services.

Enterprises rely on dedicated **Identity Providers (IdP)** such as Google Identity, GitHub, Keycloak, Okta, and Microsoft Entra ID.

Today, you will master **OAuth 2.0** and **OpenID Connect (OIDC)** in Spring Boot 3 (Spring Security 6) using Java 21:
- The fundamental difference between **Authentication (OIDC)** and **Delegated Authorization (OAuth 2.0)**.
- The **Authorization Code Flow with PKCE** for web frontends and the **Client Credentials Flow** for Machine-to-Machine (M2M) AI microservices.
- Asymmetric cryptography (**RS256**) and how Spring Boot Resource Server dynamically downloads and caches public keys via **JWKS** (`/.well-known/jwks.json`).
- Building a modern Spring Boot **OAuth2 Resource Server** using `spring-boot-starter-oauth2-resource-server`.
- Crafting custom `JwtAuthenticationConverter` to map IdP-specific claims (Google, Keycloak, Auth0) into Spring Security `GrantedAuthority` (`ROLE_*` and `SCOPE_*`).
- User Identity Propagation: How autonomous AI agents pass user credentials through microservices without creating security backdoors.

---

## Real-World Analogy: Hotel Keycard vs. Valet Parking Key

To understand OAuth2 and OIDC, picture arriving at a luxury hotel:

```
+----------------------------------------------------------------------------------------------------+
|                                    OIDC vs. OAUTH 2.0 ANALOGY                                      |
|                                                                                                    |
|  SCENARIO 1: OpenID Connect (OIDC) - The Passport / National ID Card                               |
|  - You walk to the hotel check-in desk and present your Passport (ID Token).                       |
|  - It proves WHO YOU ARE: Name, photo, date of birth, nationality.                                 |
|  - Purpose: AUTHENTICATION ("I am Alice, an engineer from TechCorp").                             |
|                                                                                                    |
|  SCENARIO 2: OAuth 2.0 - The Valet Parking Key & Room Keycard                                      |
|  - The concierge hands the valet a physical key that ONLY turns the ignition and opens the driver  |
|    door. It cannot open the glove box or the trunk!                                               |
|  - You get an electronic keycard (Access Token) that ONLY opens Room 402 and the gym between 6am-10pm.|
|  - Purpose: DELEGATED AUTHORIZATION ("Bearer is permitted to access Room 402 with scope:gym").     |
+----------------------------------------------------------------------------------------------------+
```

- **OIDC (OpenID Connect)** gives you an **ID Token**: A signed JWT containing user profile attributes (`sub`, `name`, `email`, `picture`). The client application uses this to display "Welcome, Alice!"
- **OAuth 2.0** gives you an **Access Token**: A token used as an HTTP Bearer credential to make requests against backend API Resource Servers (your Spring Boot AI backend).

---

## OAuth 2.0 Architecture & The 4 Core Actors

In any OAuth 2.0 architecture, there are four distinct parties:

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                 THE 4 OAUTH 2.0 ACTORS                                  │
├───────────────────────┬─────────────────────────────────────────────────────────────────┤
│ 1. Resource Owner     │ The end user (e.g., Alice who owns the Google account).         │
├───────────────────────┼─────────────────────────────────────────────────────────────────┤
│ 2. Client Application │ The frontend application (e.g., Next.js / React AI Chat app).   │
├───────────────────────┼─────────────────────────────────────────────────────────────────┤
│ 3. Authorization      │ The Identity Provider that authenticates Alice and issues tokens │
│    Server (IdP)       │ (e.g., Google OAuth, GitHub, Keycloak, Okta, Microsoft Entra).  │
├───────────────────────┼─────────────────────────────────────────────────────────────────┤
│ 4. Resource Server    │ Your Java 21 Spring Boot Backend hosting LLM APIs and Vector DB. │
└───────────────────────┴─────────────────────────────────────────────────────────────────┘
```

---

## Core OAuth2 Flows for AI Applications

### 1. Authorization Code Flow with PKCE (Interactive Web & Mobile)
Used when a real human logs in using their browser (e.g., "Sign in with Google").

```
 Browser / Next.js SPA             Google IdP                     Spring Boot AI API
         │                              │                                  │
         │ 1. "Sign in with Google"     │                                  │
         │    (Sends code_challenge)    │                                  │
         ├─────────────────────────────>│                                  │
         │                              │                                  │
         │ 2. Prompts user for consent  │                                  │
         │    (User enters credentials) │                                  │
         │<─────────────────────────────┤                                  │
         │                              │                                  │
         │ 3. Redirects with Auth Code  │                                  │
         │<─────────────────────────────┤                                  │
         │                              │                                  │
         │ 4. Exchanges Code + Verifier │                                  │
         ├─────────────────────────────>│                                  │
         │                              │                                  │
         │ 5. Returns ID Token + Access Token                              │
         │<─────────────────────────────┤                                  │
         │                                                                 │
         │ 6. GET /api/v1/ai/generate (Bearer AccessToken)                 │
         ├────────────────────────────────────────────────────────────────>│
         │                                                                 │
         │ 7. Spring verifies RS256 signature via JWKS & executes prompt   │
         │<────────────────────────────────────────────────────────────────┤
```

### 2. Client Credentials Flow (Machine-to-Machine M2M)
Used when backend microservices or autonomous AI workers communicate with each other **without any human present**:
- A scheduled document scraper ingesting PDFs into PostgreSQL `pgvector`.
- A background batch job fine-tuning an LLM on yesterday's audit logs.

```
 Batch Vector Ingestion Service           Keycloak / Auth0 IdP             Spring Boot AI API
               │                                   │                               │
               │ 1. POST /oauth/token              │                               │
               │    grant_type=client_credentials  │                               │
               │    client_id & client_secret      │                               │
               ├──────────────────────────────────>│                               │
               │                                   │                               │
               │ 2. Returns M2M Access Token       │                               │
               │    (Scope: "ai:vector:write")     │                               │
               │<──────────────────────────────────┤                               │
               │                                                                   │
               │ 3. POST /api/v1/vectors/batch (Bearer M2M Token)                  │
               ├──────────────────────────────────────────────────────────────────>│
               │                                                                   │
               │ 4. Spring checks SCOPE_ai:vector:write & processes embeddings    │
               │<──────────────────────────────────────────────────────────────────┤
```

---

## Asymmetric Cryptography (RS256) & The JWKS Endpoint

In Day 28, you used symmetric HMAC (`HS256`), where the secret key is shared between the issuer and validator.

In enterprise architectures, sharing secret keys is a security disaster:
- If you have 50 microservices validating tokens, all 50 services would need the secret key. If one service is compromised, an attacker can forge tokens for all services!

### The Solution: Asymmetric RS256 (Public / Private Key Pair)

```
┌──────────────────────────────────────┐       ┌──────────────────────────────────────┐
│       AUTHORIZATION SERVER           │       │       SPRING BOOT RESOURCE SERVER    │
│       (Google / Keycloak)            │       │       (Your Gen AI Backend)          │
│                                      │       │                                      │
│  [PRIVATE KEY] (Secret)              │       │  [PUBLIC KEY] (Safe to share)        │
│  - Kept strictly in secure HSM       │       │  - Fetched via HTTP from:            │
│  - Used exclusively to SIGN JWTs     │       │    https://idp.com/.well-known/jwks   │
│                                      │       │  - Used exclusively to VERIFY JWTs   │
└──────────────────────────────────────┘       └──────────────────────────────────────┘
```

### What is JWKS (`/.well-known/jwks.json`)?
JWKS (JSON Web Key Set) is a standard endpoint published by identity providers that exposes the public cryptographic keys used to sign tokens.

Example JWKS response from Google or Keycloak:
```json
{
  "keys": [
    {
      "kty": "RSA",
      "use": "sig",
      "alg": "RS256",
      "kid": "google-rsa-key-2026-01",
      "n": "u1P...[Modulus]...",
      "e": "AQAB"
    }
  ]
}
```

When your Spring Boot application boots up:
1. It queries `/.well-known/jwks.json` once and caches the public keys.
2. When a request arrives with a JWT, Spring reads the `kid` (Key ID) header.
3. It finds the matching public key in its local cache and cryptographically verifies the signature without needing any network hop per request!

---

## Configuring a Spring Boot 3 OAuth2 Resource Server

### Step 1: Maven Dependencies
Add the modern OAuth2 Resource Server starter to your `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

### Step 2: Configure `application.yml`
Point Spring Security to your Identity Provider's Issuer URI:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          # Spring automatically discovers the JWKS URI from the issuer:
          # https://accounts.google.com/.well-known/openid-configuration
          issuer-uri: https://accounts.google.com
          # Optional: enforce expected audience
          audiences: genai-cloud-api
```

### Step 3: Security Configuration with Custom Claims Converter

Different IdPs format their roles differently:
- **Google**: Provides `email`, `sub`, and `hd` (hosted domain).
- **Keycloak**: Stores roles in `realm_access.roles` or `resource_access.client.roles`.
- **Auth0**: Stores roles in custom namespaces like `https://genai.io/roles`.

We build a custom `JwtAuthenticationConverter` to normalize all IdP claims into Spring Security `GrantedAuthority`:

```java
package com.genai.security.oauth2;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.*;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class OAuth2ResourceServerConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/ai/m2m/**").hasAuthority("SCOPE_ai:infer")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(customJwtAuthenticationConverter()))
            )
            .build();
    }

    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> customJwtAuthenticationConverter() {
        return jwt -> {
            // 1. Extract standard OAuth2 scopes (e.g. SCOPE_read, SCOPE_profile)
            JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();
            Collection<GrantedAuthority> authorities = new HashSet<>(scopeConverter.convert(jwt));

            // 2. Extract custom Keycloak / IdP roles
            List<String> roles = extractRoles(jwt);
            for (String role : roles) {
                String roleName = role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase();
                authorities.add(new SimpleGrantedAuthority(roleName));
            }

            // 3. Extract enterprise domain for corporate isolation
            String email = jwt.getClaimAsString("email");
            if (email != null && email.endsWith("@mycompany.com")) {
                authorities.add(new SimpleGrantedAuthority("ROLE_INTERNAL_EMPLOYEE"));
            }

            String principalClaimName = jwt.getClaimAsString("email") != null ? "email" : "sub";
            return new JwtAuthenticationToken(jwt, authorities, jwt.getClaimAsString(principalClaimName));
        };
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Jwt jwt) {
        // Support Keycloak realm_access.roles
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof List<?> list) {
            return (List<String>) list;
        }

        // Support direct 'roles' array claim
        List<String> directRoles = jwt.getClaimAsStringList("roles");
        if (directRoles != null) {
            return directRoles;
        }

        return Collections.emptyList();
    }
}
```

---

## Social Login Integration: "Sign in with Google / GitHub"

If your Spring Boot application also serves server-rendered pages (e.g., Thymeleaf) or acts as a Backends-For-Frontends (BFF) gateway, you use `spring-boot-starter-oauth2-client`:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: openid, profile, email
          github:
            client-id: ${GITHUB_CLIENT_ID}
            client-secret: ${GITHUB_CLIENT_SECRET}
            scope: user:email, read:user
```

### Just-In-Time (JIT) User Provisioning
When a user clicks "Sign in with Google", they may not exist in your database yet. You use a custom `OAuth2UserService` to provision them automatically:

```java
@Service
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;

    public CustomOidcUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();
        String googleId = oidcUser.getSubject();

        // JIT User Provisioning: Save user to DB if first login
        userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFullName(name);
            newUser.setAuthProvider("GOOGLE");
            newUser.setProviderId(googleId);
            newUser.setRole("ROLE_FREE_USER");
            newUser.setDailyTokenQuota(10_000); // 10k free tokens
            return userRepository.save(newUser);
        });

        return oidcUser;
    }
}
```

---

## Why It Matters for Gen AI Applications

| Requirement | Traditional Username/Password | OAuth2 & OIDC Resource Server |
|:---|:---|:---|
| **Developer Console Login** | Custom registration, password resets, 2FA headaches. | Instant "Login with GitHub / Google" with zero credential storage risk. |
| **Enterprise SSO (SAML/OIDC)** | Requires custom LDAP connectors. | Seamless integration with Okta / Keycloak / Microsoft Entra ID via standard OIDC. |
| **M2M AI Pipeline Workers** | Hardcoded database passwords or static API keys in config files. | Short-lived RS256 tokens issued via Client Credentials Flow, automatically rotated. |
| **User Identity Propagation** | AI Agent runs queries as a generic database superuser. | AI Agent passes original user's Bearer token; vector database queries execute within user's exact permissions. |

---

## Step-by-Step Production Code Walkthrough

Let's inspect the runnable companion code built for today's lesson in `Phase_05_Spring_Security/Day_30_OAuth2_Social_Login/code/`:

### 1. `AsymmetricKeyManager.java`
Uses standard Java 21 `KeyPairGenerator` to produce genuine RSA 2048-bit key pairs:

```java
KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
keyGen.initialize(2048);
this.keyPair = keyGen.generateKeyPair();
```

### 2. `OidcTokenIssuer.java`
Signs JWTs with `SHA256withRSA` using the IdP's Private Key, including Key ID (`kid`) in the header:

```java
Signature signature = Signature.getInstance("SHA256withRSA");
signature.initSign(privateKey);
signature.update(signingInput.getBytes(StandardCharsets.UTF_8));
byte[] signatureBytes = signature.sign();
```

### 3. `ResourceServerValidator.java`
Simulates Spring Security's `NimbusJwtDecoder` and `JwtAuthenticationConverter`:
- Validates token format (`Header.Payload.Signature`).
- Queries JWKS using `kid` to locate the corresponding Public Key.
- Cryptographically verifies the signature (`initVerify(publicKey)`).
- Validates `iss`, `aud`, and `exp`.
- Translates `roles` into `ROLE_*` and `scope` into `SCOPE_*`.

### 4. Running the Verification Suite
Compile and execute the simulation:

```bash
javac -d out Phase_05_Spring_Security/Day_30_OAuth2_Social_Login/code/*.java
java -cp out com.genai.security.oauth2.OAuth2Demo
```

Output:
```text
================================================================================
  DAY 30: OAUTH2 & OIDC RESOURCE SERVER DEMONSTRATION (RS256 & JWKS)            
================================================================================

[TEST 1] Validating Google Social Login ID Token...
  Generated RS256 JWT: eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6I...[TRUNCATED]
  ✅ AUTHENTICATED SUCCESSFULLY!
     Subject: google-oauth2|1092837465
     Email:   alice.engineer@techcorp.com
     Name:    Alice Engineer
     Roles:   [ROLE_DEVELOPER, ROLE_PRO_USER]

[TEST 2] Validating Machine-to-Machine (M2M) Service Account Token...
  ✅ M2M SERVICE AUTHENTICATED!
     Client ID: svc-rag-indexer-01
     Scopes:    [SCOPE_ai:infer:stream, SCOPE_ai:embed]

[TEST 3] Validating Expired Token...
  ✅ REJECTED AS EXPECTED: Token has expired. Expiration: 1788956301, Current: 1788956361

[TEST 4] Validating Rogue/Counterfeit Token (Spoofed Signature)...
  ✅ BLOCKED SPOOFING ATTACK: Cryptographic RS256 signature verification failed! Token has been tampered with.

[TEST 5] Validating Untrusted Issuer...
  ✅ BLOCKED UNTRUSTED ISSUER: Issuer mismatch. Expected: https://accounts.google.com, Found: https://malicious-idp.evil.com

================================================================================
  ALL OAUTH2 & RESOURCE SERVER TESTS PASSED PERFECTLY!                          
================================================================================
```

---

## Hands-On Exercises (With Complete Solutions)

### Exercise 1: Multi-Tenant Issuer Routing
**Problem Statement:**  
In a multi-tenant enterprise AI platform, Customer A authenticates via Okta (`https://customerA.okta.com`), while Customer B authenticates via Keycloak (`https://auth.customerB.com`). Write a Spring Security `AuthenticationManagerResolver` bean that dynamically routes incoming requests to the correct `JwtDecoder` based on the JWT's `iss` (issuer) claim.

<details>
<summary>👉 View Solution</summary>

```java
package com.genai.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;

@Configuration
public class MultiTenantOAuth2Config {

    @Bean
    public AuthenticationManagerResolver<HttpServletRequest> dynamicIssuerResolver() {
        return JwtIssuerAuthenticationManagerResolver.fromTrustedIssuers(
            "https://customerA.okta.com",
            "https://auth.customerB.com",
            "https://accounts.google.com"
        );
    }
}
```
*Explanation:* Spring Security provides `JwtIssuerAuthenticationManagerResolver` which reads the unverified JWT payload's `iss` claim, validates that the issuer is on the approved whitelist, and delegates validation to the corresponding tenant's `JwtDecoder`.
</details>

---

### Exercise 2: M2M AI Token Scope Guard
**Problem Statement:**  
You are building an endpoint `/api/v1/ai/batch-embeddings` intended exclusively for automated worker microservices. The caller must present an OAuth2 Access Token holding the scope `SCOPE_ai:vector:write`. Configure the HTTP security matcher and method-level annotation.

<details>
<summary>👉 View Solution</summary>

```java
// Controller method:
@RestController
@RequestMapping("/api/v1/ai")
public class BatchEmbeddingController {

    @PostMapping("/batch-embeddings")
    @PreAuthorize("hasAuthority('SCOPE_ai:vector:write')")
    public ResponseEntity<BatchResult> ingestBatch(@RequestBody EmbeddingBatchRequest request) {
        return ResponseEntity.ok(embeddingService.processBatch(request));
    }
}
```
*Explanation:* When Spring Security parses OAuth2 scopes from the `scope` or `scp` claim, it prefixes each scope with `SCOPE_`. Therefore, a scope named `ai:vector:write` becomes the granted authority `SCOPE_ai:vector:write`.
</details>

---

### Exercise 3: User Token Allowance Initialization on First Login
**Problem Statement:**  
Create an `AuthenticationSuccessHandler` for social login that checks if the newly authenticated user has an assigned AI token quota. If they are a new user, grant them an initial welcome balance of 50,000 inference tokens.

<details>
<summary>👉 View Solution</summary>

```java
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserQuotaRepository quotaRepository;

    public OAuth2LoginSuccessHandler(UserQuotaRepository quotaRepository) {
        this.quotaRepository = quotaRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            String email = oidcUser.getEmail();
            quotaRepository.findByEmail(email).ifPresentOrElse(
                quota -> {
                    // Existing user - log login event
                    System.out.println("Existing user logged in: " + email);
                },
                () -> {
                    // First time login - Provision welcome balance
                    UserQuota newQuota = new UserQuota(email, 50_000, "FREE_TIER");
                    quotaRepository.save(newQuota);
                    System.out.println("Granted 50,000 welcome AI tokens to: " + email);
                }
            );
        }
        response.sendRedirect("/dashboard");
    }
}
```
</details>

---

## 5-Question Self-Check Quiz

#### 1. What is the fundamental difference between OpenID Connect (OIDC) and OAuth 2.0?
- A) OAuth 2.0 uses XML, while OIDC uses JSON.
- B) OAuth 2.0 is an authorization framework (Access Tokens); OIDC is an identity layer on top of OAuth 2.0 (ID Tokens for Authentication).
- C) OIDC is only for mobile phones.
- D) OAuth 2.0 is deprecated in favor of SAML.

#### 2. Why is RS256 preferred over HS256 in multi-service enterprise architectures?
- A) HS256 tokens cannot be decoded by web browsers.
- B) RS256 uses asymmetric cryptography, meaning resource servers only need the public key to verify tokens, eliminating the risk of shared secret exposure.
- C) RS256 is 100x faster to compute than HS256.
- D) HS256 does not support expiration dates (`exp`).

#### 3. What is the primary purpose of the JWKS (`/.well-known/jwks.json`) endpoint?
- A) To publish the Authorization Server's public keys so Resource Servers can dynamically verify RS256 JWT signatures.
- B) To store user passwords in encrypted format.
- C) To list all users currently logged in to the application.
- D) To refresh expired access tokens.

#### 4. In Spring Security OAuth2 Resource Server, how does a scope claim named `"ai:infer"` translate to a `GrantedAuthority` by default?
- A) `ROLE_ai:infer`
- B) `SCOPE_ai:infer`
- C) `PERMISSION_ai:infer`
- D) `ai:infer`

#### 5. Which OAuth2 flow is specifically designed for server-to-server (Machine-to-Machine) communication without human interaction?
- A) Authorization Code Flow with PKCE
- B) Implicit Flow
- C) Resource Owner Password Credentials Flow
- D) Client Credentials Flow

---

### Quiz Answers & Explanations

1. **B is correct**: OAuth 2.0 deals with *delegated authorization* (what an app can do), while OIDC adds *authentication* (who the user is) via the ID Token.
2. **B is correct**: With RS256, only the identity provider holds the private signing key. All downstream microservices only need the public key to verify signatures.
3. **A is correct**: JWKS provides a standard JSON payload containing the public cryptographic keys used to sign tokens.
4. **B is correct**: The default `JwtGrantedAuthoritiesConverter` maps items in the `scope` / `scp` claim with the prefix `SCOPE_`.
5. **D is correct**: The Client Credentials Flow is the standard RFC 6749 flow for automated service-to-service integration.

---

## Day 30 Wrap-Up & What's Next

You've mastered how modern tech giants handle identity without maintaining fragile password databases!

Here is what you unlocked today:
- **OIDC vs. OAuth2**: OIDC gives you an ID Token (Authentication: "Who are you?"), while OAuth2 gives you an Access Token (Authorization: "What can you do?").
- **Asymmetric RS256**: Google keeps its private key secret, and your Spring Boot Resource Server uses Google's public keys via JWKS to verify tokens in microseconds.
- **Spring Boot Resource Server**: Clean configuration using `oauth2ResourceServer(oauth2 -> oauth2.jwt(...))` that automatically integrates with Spring Security.
- **Machine-to-Machine (M2M)**: Automated AI agents and ingestion workers use the Client Credentials Flow to talk to your backend safely without human intervention.

### What's Coming Up Next?
Now anyone with a Google or GitHub account can log into your AI platform. 

**But what if a malicious user or bot starts firing 1,000 requests a second, trying to overwhelm your server and exhaust your OpenAI credits?** Or what if a frontend running on another domain tries to steal your API responses?

Tomorrow, in the grand finale of Phase 5: **[Day 31: Rate Limiting, CORS & API Security](../Day_31_Rate_Limiting_CORS_API_Security/Day_31_Rate_Limiting_CORS_API_Security.md)**, we'll build token-bucket rate limiters with Bucket4j, lock down Cross-Origin Resource Sharing (CORS), and protect our AI backend against abuse!

