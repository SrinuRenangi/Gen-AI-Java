# Day 30: OAuth2 & Social Login (OpenID Connect, Resource Server)
## Google/GitHub SSO, Asymmetric RS256 Validation, JWKS & M2M AI Token Security

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [◀ Day 29: Role-Based Access Control (RBAC)](../Day_29_RBAC_Method_Level_Security/Day_29_RBAC_Method_Level_Security.md) | [All 60 Days Overview](../../README.md) | [Day 31: Rate Limiting, CORS & API Security ▶](../Day_31_Rate_Limiting_CORS_API_Security/Day_31_Rate_Limiting_CORS_API_Security.md) |

---

## 1. Topic Overview

OAuth 2.0 provides an industry-standard framework for delegated authorization, while OpenID Connect (OIDC) adds a standardized identity layer for user authentication using asymmetric JSON Web Tokens (RS256). In enterprise Generative AI systems, configuring Spring Boot as an OAuth2 Resource Server allows AI endpoints to validate Google/GitHub social logins and Machine-to-Machine (M2M) microservice tokens against dynamic JWKS endpoints without storing sensitive passwords or sharing private cryptographic keys.

---

## 2. Basic Foundations (True Zero)

### What is OAuth 2.0 vs. OpenID Connect (OIDC)?
When building modern AI web platforms, requiring users to create a unique username and password introduces friction, security risks, and credential management burdens. Instead, applications provide **"Sign in with Google"** or **"Sign in with GitHub"**:

1. **OAuth 2.0 (Delegated Authorization — "What can you do?")**: A framework that lets a third-party application access resources on a user's behalf without learning their password. (Analogous to a valet key that starts the engine but cannot open the trunk).
2. **OpenID Connect / OIDC (Authentication — "Who are you?")**: An identity protocol built on top of OAuth 2.0. It delivers a cryptographically signed **ID Token** containing user profile information (`sub`, `name`, `email`).
3. **Resource Server**: In OAuth2 terminology, this is your Spring Boot backend hosting protected REST endpoints, vector tables, and LLM services.
4. **Identity Provider (IdP) / Authorization Server**: The trusted entity (Google, GitHub, Keycloak, Okta) that authenticates users and issues signed tokens.

```
+-----------------------------------------------------------------------------------+
|               THE HOTEL PASSPORT VS. VALET KEY ANALOGY                            |
|                                                                                   |
|  SCENARIO 1: OpenID Connect (OIDC) - The National Passport                        |
|  - You walk up to the hotel check-in desk and present your passport (ID Token).   |
|  - It proves WHO YOU ARE: Name, date of birth, photo, nationality.                |
|  - Purpose: AUTHENTICATION ("I am Alice, an engineer from Acme Corp").           |
|                                                                                   |
|  SCENARIO 2: OAuth 2.0 - The Valet Parking Key & Electronic Room Keycard          |
|  - The hotel concierge gives the valet driver a key that turns the ignition       |
|    and unlocks the driver door, but CANNOT unlock the glove box or trunk!         |
|  - You receive an electronic room keycard (Access Token) that opens Room 402      |
|    and the gym between 6:00 AM and 10:00 PM.                                      |
|  - Purpose: DELEGATED AUTHORIZATION ("Bearer is permitted access to Room 402").   |
+-----------------------------------------------------------------------------------+
```

### Minimal Beginner-Friendly Working Code Example

Below is a self-contained Java 21 demonstration of how Asymmetric RS256 cryptography works: an Authorization Server signs a token using a **Private Key**, and a Resource Server verifies it using only the matching **Public Key**:

```java
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

public class BasicAsymmetricSignatureExample {

    public static void main(String[] args) throws Exception {
        // 1. Generate RSA 2048-bit KeyPair (Simulating Google / Keycloak IdP)
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        PrivateKey privateSigningKey = keyPair.getPrivate();
        PublicKey publicVerificationKey = keyPair.getPublic();

        // 2. Token payload representing user identity
        String tokenPayload = "{\"sub\":\"google-uid-10293\",\"email\":\"alice@techcorp.com\",\"role\":\"PRO_USER\"}";
        String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenPayload.getBytes(StandardCharsets.UTF_8));

        // 3. Authorization Server signs the payload using the PRIVATE KEY
        Signature rsaSigner = Signature.getInstance("SHA256withRSA");
        rsaSigner.initSign(privateSigningKey);
        rsaSigner.update(encodedPayload.getBytes(StandardCharsets.UTF_8));
        byte[] signatureBytes = rsaSigner.sign();
        String signatureString = Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);

        System.out.println("Token signed with Private Key successfully.");
        System.out.println("Signature: " + signatureString.substring(0, 32) + "...");

        // 4. Spring Boot Resource Server verifies the signature using ONLY the PUBLIC KEY
        Signature rsaVerifier = Signature.getInstance("SHA256withRSA");
        rsaVerifier.initVerify(publicVerificationKey);
        rsaVerifier.update(encodedPayload.getBytes(StandardCharsets.UTF_8));
        boolean isSignatureValid = rsaVerifier.verify(signatureBytes);

        System.out.println("\nVerification using Public Key: " + (isSignatureValid ? "SUCCESS (AUTHENTIC)" : "FAILED"));

        // 5. Tampering Attempt: Attacker alters payload
        String tamperedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"google-uid-10293\",\"role\":\"ADMIN\"}".getBytes(StandardCharsets.UTF_8));
        rsaVerifier.initVerify(publicVerificationKey);
        rsaVerifier.update(tamperedPayload.getBytes(StandardCharsets.UTF_8));
        boolean isTamperValid = rsaVerifier.verify(signatureBytes);

        System.out.println("Tamper Detection (Altered to ADMIN): " + (isTamperValid ? "BREACH!" : "BLOCKED! Signature Invalid"));
    }
}
```

#### Line-by-Line Walkthrough:
- **Lines 10–14**: Generates an asymmetric RSA 2048-bit keypair. In production, the Identity Provider (Google, Okta) protects the private key inside a Hardware Security Module (HSM) and exposes the public key over HTTP.
- **Lines 17–18**: Encodes the token payload containing the user's Google ID, email, and subscription role.
- **Lines 21–25**: The IdP uses `SHA256withRSA` and the **Private Key** to generate a digital cryptographic signature.
- **Lines 31–35**: Your Spring Boot backend (Resource Server) uses `initVerify` with the **Public Key** to verify the signature. At no point does Spring Boot need to know or store Google's private key!
- **Lines 39–44**: If an attacker intercepts the token and changes their role from `"PRO_USER"` to `"ADMIN"`, public key verification fails instantly.

---

## 3. Core Concept Walkthrough (Basic → Intermediate)

### The 4 Core OAuth 2.0 Actors

```
+-----------------------------------------------------------------------------------------+
|                                 THE 4 OAUTH 2.0 ACTORS                                  |
+-----------------------+-----------------------------------------------------------------+
| 1. Resource Owner     | The end user (e.g., Alice who owns the Google account).         |
+-----------------------+-----------------------------------------------------------------+
| 2. Client Application | The frontend application (e.g., Next.js / React AI Chat app).   |
+-----------------------+-----------------------------------------------------------------+
| 3. Authorization      | The Identity Provider that authenticates Alice and issues tokens|
|    Server (IdP)       | (e.g., Google OAuth, GitHub, Keycloak, Okta, Microsoft Entra).  |
+-----------------------+-----------------------------------------------------------------+
| 4. Resource Server    | Your Java 21 Spring Boot Backend hosting LLM APIs and Vector DB.|
+-----------------------+-----------------------------------------------------------------+
```

---

### Core OAuth2 Flows for AI Applications

#### 1. Authorization Code Flow with PKCE (Interactive Web & Mobile)
Used when a real human user logs in via a web or mobile browser:

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

#### 2. Client Credentials Flow (Machine-to-Machine M2M)
Used when backend microservices or autonomous AI workers communicate with each other **without human interaction** (e.g., background document scrapers ingesting PDFs into PostgreSQL `pgvector`):

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

### Asymmetric RS256 & The JWKS Endpoint

In symmetric HMAC (`HS256`), every microservice needs the shared secret key. If one microservice is compromised, all tokens across the enterprise can be forged.

In asymmetric **RS256**, Google, Keycloak, or Okta signs tokens using a private key and publishes public keys at a standard web endpoint known as **JWKS (JSON Web Key Set)** (`/.well-known/jwks.json`):

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

When Spring Boot starts up:
1. It queries `/.well-known/jwks.json` and caches the public keys.
2. When a request arrives with a JWT, Spring reads the `kid` (Key ID) header.
3. It finds the matching public key in memory and verifies the signature locally in microseconds.

---

### Spring Boot 3 OAuth2 Resource Server Configuration

#### Maven Dependency:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

#### `application.yml`:
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://accounts.google.com
          audiences: genai-cloud-api
```

#### Security Configuration with Custom Claims Converter:
```java
package com.example.genai.config;

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
                .requestMatchers("/api/v1/ai/m2m/**").hasAuthority("SCOPE_ai:vector:write")
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

            // 2. Extract IdP custom roles (supporting direct roles array and Keycloak realm_access)
            List<String> roles = extractRoles(jwt);
            for (String role : roles) {
                String roleName = role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase();
                authorities.add(new SimpleGrantedAuthority(roleName));
            }

            // 3. Extract corporate enterprise domain
            String email = jwt.getClaimAsString("email");
            if (email != null && email.endsWith("@mycompany.com")) {
                authorities.add(new SimpleGrantedAuthority("ROLE_INTERNAL_EMPLOYEE"));
            }

            String principalClaimName = email != null ? "email" : "sub";
            return new JwtAuthenticationToken(jwt, authorities, jwt.getClaimAsString(principalClaimName));
        };
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof List<?> list) {
            return (List<String>) list;
        }
        List<String> directRoles = jwt.getClaimAsStringList("roles");
        if (directRoles != null) return directRoles;
        return Collections.emptyList();
    }
}
```

---

## 4. Prerequisite & Supporting Concepts

### Prerequisite / Supporting Concept: PKCE (Proof Key for Code Exchange)
In standard web single-page apps (React/Vue/Next.js), storing a client secret in frontend code is insecure because anyone can inspect the JavaScript bundle. 

**PKCE (RFC 7636)** replaces client secrets with dynamically generated cryptographic challenges:
1. The frontend creates a secret random string called the `code_verifier`.
2. It hashes the verifier with SHA-256 to create a `code_challenge` and sends it to the IdP.
3. When exchanging the authorization code, the frontend sends the original `code_verifier`.
4. The IdP hashes it and verifies that it matches the initial challenge. Even if an attacker intercepts the authorization code, they cannot exchange it without the verifier!

### Prerequisite / Supporting Concept: Just-In-Time (JIT) User Provisioning
When users sign in via Google or GitHub, they do not yet have a record in your local database. You implement a custom `OidcUserService`:

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

        userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFullName(oidcUser.getFullName());
            newUser.setAuthProvider("GOOGLE");
            newUser.setRole("ROLE_FREE_USER");
            newUser.setDailyTokenQuota(10_000);
            return userRepository.save(newUser);
        });

        return oidcUser;
    }
}
```

---

## 5. Advanced Depth (Intermediate → Advanced)

### Multi-Tenant Issuer Routing

In enterprise SaaS where Customer A uses Okta (`https://customerA.okta.com`) and Customer B uses Keycloak (`https://auth.customerB.com`), a single static `issuer-uri` in `application.yml` fails.

Spring Security provides `JwtIssuerAuthenticationManagerResolver` to dynamically inspect the token's `iss` header and delegate verification to the matching tenant's JWKS:

```java
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

---

### Hands-On Simulation Code Walkthrough

The companion code repository demonstrates this architecture:
- `AsymmetricKeyManager.java`: Generates RSA 2048-bit keypairs using standard Java cryptography.
- `OidcTokenIssuer.java`: Signs JWTs using `SHA256withRSA` and embeds the `kid` header.
- `ResourceServerValidator.java`: Validates format, fetches matching public keys from simulated JWKS, verifies cryptographic signatures, and maps roles.
- `OAuth2Demo.java`: 5-scenario verification test suite validating Google tokens, M2M tokens, expired tokens, tampered signatures, and untrusted issuers.

```powershell
# Compile Day 30 code
javac -d out Phase_05_Spring_Security/Day_30_OAuth2_Social_Login/code/*.java

# Run OAuth2Demo
java -cp out com.genai.security.oauth2.OAuth2Demo
```

#### Verified Execution Output:
```
================================================================================
  DAY 30: OAUTH2 & OIDC RESOURCE SERVER DEMONSTRATION (RS256 & JWKS)            
================================================================================

[TEST 1] Validating Google Social Login ID Token...
  Generated RS256 JWT: eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6I...[TRUNCATED]
  [OK] AUTHENTICATED SUCCESSFULLY!
     Subject: google-oauth2|1092837465
     Email:   alice.engineer@techcorp.com
     Name:    Alice Engineer
     Roles:   [ROLE_DEVELOPER, ROLE_PRO_USER]

[TEST 2] Validating Machine-to-Machine (M2M) Service Account Token...
  [OK] M2M SERVICE AUTHENTICATED!
     Client ID: svc-rag-indexer-01
     Scopes:    [SCOPE_ai:infer:stream, SCOPE_ai:embed]

[TEST 3] Validating Expired Token...
  [OK] REJECTED AS EXPECTED: Token has expired. Expiration: 1788956301, Current: 1788956361

[TEST 4] Validating Rogue/Counterfeit Token (Spoofed Signature)...
  [OK] BLOCKED SPOOFING ATTACK: Cryptographic RS256 signature verification failed! Token has been tampered with.

[TEST 5] Validating Untrusted Issuer...
  [OK] BLOCKED UNTRUSTED ISSUER: Issuer mismatch. Expected: https://accounts.google.com, Found: https://malicious-idp.evil.com

================================================================================
  ALL OAUTH2 & RESOURCE SERVER TESTS PASSED PERFECTLY!                          
================================================================================
```

---

## 6. Quick Recap

| Concept | Description | Enterprise Rule / Best Practice |
| :--- | :--- | :--- |
| **OAuth 2.0** | Delegated authorization framework | Issues Access Tokens for API resource access. |
| **OIDC** | Identity layer built on OAuth 2.0 | Issues ID Tokens containing verified user profile data. |
| **Resource Server** | Spring Boot backend hosting APIs | Validates Bearer tokens and protects resources. |
| **RS256** | Asymmetric RSA with SHA-256 | Private key signs; Public key verifies. No shared secrets. |
| **JWKS** | `/.well-known/jwks.json` | Public key distribution endpoint used by Spring Boot. |
| **PKCE** | Proof Key for Code Exchange | Mandatory for public browser and mobile clients. |
| **Client Credentials** | Machine-to-Machine flow | Used for autonomous AI background services without humans. |

---

## 7. Self-Check Questions & Practice Exercises

### Conceptual & Architectural Questions

#### Q1: What is the fundamental difference between OpenID Connect (OIDC) and OAuth 2.0?
**Answer**: OAuth 2.0 is a delegated authorization framework designed to issue Access Tokens granting permission to call specific APIs. OIDC is an identity layer built on top of OAuth 2.0 that issues ID Tokens (JWTs) containing verified user identity attributes (`name`, `email`, `sub`) for authentication.

#### Q2: Why is RS256 preferred over HS256 in multi-service enterprise architectures?
**Answer**: RS256 uses asymmetric cryptography. The Identity Provider signs tokens using a private key, while downstream Resource Servers verify signatures using only the corresponding public key. With HS256, all resource servers would need the shared secret, meaning a compromise of any single microservice exposes the entire system.

#### Q3: What is the primary purpose of the JWKS (`/.well-known/jwks.json`) endpoint?
**Answer**: JWKS exposes the Identity Provider's public cryptographic keys in a standardized JSON format. Resource Servers automatically query this endpoint to download and cache public keys, allowing them to verify token signatures locally in microseconds.

#### Q4: In Spring Security OAuth2 Resource Server, how does a scope claim named `"ai:vector:write"` translate to a `GrantedAuthority`?
**Answer**: By default, Spring's `JwtGrantedAuthoritiesConverter` maps items in the `scope` or `scp` claim by prefixing them with `SCOPE_`, resulting in the authority `SCOPE_ai:vector:write`.

#### Q5: Which OAuth2 flow is specifically designed for server-to-server (Machine-to-Machine) communication without human interaction?
**Answer**: The **Client Credentials Flow** (defined in RFC 6749), where the background worker authenticates directly with the IdP using a `client_id` and `client_secret` to obtain an M2M access token.

---

### Hands-On Practice Exercises

#### Exercise 1: Multi-Tenant Issuer Routing
**Task**: In a multi-tenant platform, Customer A authenticates via Okta (`https://customerA.okta.com`), while Customer B authenticates via Keycloak (`https://auth.customerB.com`). Write a Spring Security `AuthenticationManagerResolver` bean that routes requests based on the JWT's `iss` claim.

```java
// Solution:
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

#### Exercise 2: M2M AI Token Scope Guard
**Task**: Configure a controller endpoint `/api/v1/ai/batch-embeddings` restricted exclusively to automated worker microservices holding the scope `SCOPE_ai:vector:write`.

```java
// Solution:
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

#### Exercise 3: User Token Allowance Initialization on First Login
**Task**: Create an `AuthenticationSuccessHandler` for social login that checks if the user has an assigned AI token quota, granting new users a welcome balance of 50,000 tokens.

```java
// Solution:
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
                quota -> System.out.println("Existing user logged in: " + email),
                () -> {
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

---

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [◀ Day 29: Role-Based Access Control (RBAC)](../Day_29_RBAC_Method_Level_Security/Day_29_RBAC_Method_Level_Security.md) | [All 60 Days Overview](../../README.md) | [Day 31: Rate Limiting, CORS & API Security ▶](../Day_31_Rate_Limiting_CORS_API_Security/Day_31_Rate_Limiting_CORS_API_Security.md) |
