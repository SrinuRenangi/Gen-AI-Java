# Day 28: JWT Authentication from Scratch

> **"If your AI gateway relies on server-side HTTP sessions, every time you scale to 50 Kubernetes pods, you need complex Redis session replication clusters. Worse, session cookies fail completely when autonomous AI agents or external microservices invoke your streaming APIs. JSON Web Tokens (JWT) provide cryptographically sealed, stateless identity tickets that any pod can verify in 50 microseconds without touching a database."**

---

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [Day 27: Security Fundamentals & Architecture](../Day_27_Security_Fundamentals_Architecture/Day_27_Security_Fundamentals_Architecture.md) | [All 60 Days Overview](../../README.md) | [Day 29: RBAC & Method-Level Security](../Day_29_RBAC_Method_Level_Security/Day_29_RBAC_Method_Level_Security.md) |

---

## Table of Contents

1. [Why This Day Matters for a 3-Year Enterprise Gen AI Engineer](#1-why-this-day-matters-for-a-3-year-enterprise-gen-ai-engineer)
2. [Real-World Analogy: The Wax-Sealed Royal Passport & Embassy Courier](#2-real-world-analogy-the-wax-sealed-royal-passport--embassy-courier)
3. [The Anatomy of a JSON Web Token (RFC 7519)](#3-the-anatomy-of-a-json-web-token-rfc-7519)
   - [Header](#header)
   - [Payload (Standard & Custom Claims)](#payload-standard--custom-claims)
   - [Cryptographic Signature (HS256 vs RS256)](#cryptographic-signature-hs256-vs-rs256)
4. [The Access Token vs Refresh Token Architecture](#4-the-access-token-vs-refresh-token-architecture)
   - [Why Short-Lived Access Tokens (15 Minutes)?](#why-short-lived-access-tokens-15-minutes)
   - [Refresh Token Rotation (RTR) & Breach Detection](#refresh-token-rotation-rtr--breach-detection)
5. [Integrating JWT with Spring Security: `JwtAuthenticationFilter`](#5-integrating-jwt-with-spring-security-jwtauthenticationfilter)
6. [Security Warning: LocalStorage vs HTTP-Only Cookies](#6-security-warning-localstorage-vs-http-only-cookies)
7. [Hands-On Code Walkthrough](#7-hands-on-code-walkthrough)
8. [Step-by-Step Compilation & Execution](#8-step-by-step-compilation--execution)
9. [Hands-On Exercises (With Complete Solutions)](#9-hands-on-exercises-with-complete-solutions)
10. [Self-Check Quiz](#10-self-check-quiz)

---

## 1. Why This Day Matters for a 3-Year Enterprise Gen AI Engineer

In modern generative AI platforms, your Spring Boot backend serves three distinct clients:
1. **Frontend Web & Mobile Apps**: React/Vue chat dashboards streaming tokens via SSE.
2. **Autonomous AI Agents & Pipelines**: Python/LangChain workers executing multi-step retrieval and evaluation loops.
3. **Enterprise B2B Partner Integrations**: External corporate systems calling your AI completions.

### Why Traditional Stateful Sessions Fail:
- **Session State Overhead**: If 20,000 active users maintain concurrent chat sessions, storing session state in memory consumes gigabytes of heap, while externalizing to Redis introduces network round-trip latency on every single request.
- **Server-Sent Events (SSE) Streaming**: SSE connections hold open HTTP connections for minutes; cookie-based session expiration frequently interrupts long-running reasoning model generations.
- **Microservice Portability**: A request to `/api/v1/chat` might be routed by a Kubernetes load balancer to Pod A, while the subsequent call to `/api/v1/documents/embed` is routed to Pod B.

**JSON Web Tokens (JWT)** solve this by making the token **self-contained**:
- The token itself carries the user's `userId`, `tenantId`, `roles`, and `tokenQuota`.
- Any pod in your cluster verifies the cryptographic signature locally in **0.05 milliseconds** without querying PostgreSQL or Redis.

---

## 2. Real-World Analogy: The Wax-Sealed Royal Passport & Embassy Courier

```
THE STATEFUL SESSION MODEL (CHECKING THE CENTRAL CITADEL REGISTRY):
[ Traveler arrives at remote mountain fortress ]
Guard: "Wait here. I must send a messenger on horseback 500 miles back 
       to the Royal Citadel to verify if your name is written in the King's ledger."
Outcome: 3-day delay, citadel gates overcrowded with messengers.

THE STATELESS JWT MODEL (THE WAX-SEALED ROYAL PASSPORT):
[ Traveler arrives at remote mountain fortress ]
Traveler presents parchment: "I am Duke Alice of House Acme. Permitted to carry 500k tokens."
Guard inspects the King's red wax seal on the document with a magnifying glass:
"The royal seal is unbroken and matches the King's signet ring! Enter immediately."
Outcome: Zero delay. Verification happens locally at the gate in 2 seconds!
```

If an impostor attempts to forge the passport by crossing out "Baron" and handwriting "King", **the wax seal breaks**. The frontier guard rejects the document instantly without ever consulting the central castle.

---

## 3. The Anatomy of a JSON Web Token (RFC 7519)

A compact JWT string consists of **three Base64Url-encoded parts separated by periods (`.`)**:

```
eyJhbGciOiJIUzI1NiJ9 . eyJzdWIiOiJ1c3JfYWxpY2UiLCJyb2xlcyI6WyJST0xFX1VTRVIiXX0 . 0dtmybZJkbzNU4f9nqN-OlZp6JJdeBi6bC3vFwFqxjI
└─────────┬─────────┘   └──────────────────────┬──────────────────────┘   └─────────────────────┬─────────────────────┘
       1. HEADER                              2. PAYLOAD                                3. SIGNATURE
```

### 1. The Header
Specifies the signature algorithm and token type:
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

### 2. The Payload (Claims)
The payload contains the **claims**—statements about an entity (typically, the user) and additional metadata:

```json
{
  "sub": "usr_alice_123",
  "tenantId": "org_deepmind_ai",
  "roles": ["ROLE_USER", "SCOPE_ai:chat"],
  "tokenBudget": 500000,
  "iat": 1788953027,
  "exp": 1788956627
}
```

- **Registered Claims (RFC 7519 standard)**:
  - `sub` (Subject): The unique user ID.
  - `iat` (Issued At): Epoch timestamp when generated.
  - `exp` (Expiration Time): Epoch timestamp when token becomes invalid.
- **Custom AI Claims**:
  - `tenantId`: Used by database queries to enforce multi-tenant isolation.
  - `tokenBudget`: Max tokens the client is allowed to consume.
  - `roles`: Granted security permissions.

> **CRITICAL SECURITY NOTE**: The payload is **Base64Url-encoded, NOT encrypted!** Anyone who intercepts the token can decode and read the payload using `jwt.io` or `atob()`. **Never store secrets, passwords, or API keys inside a JWT payload!**

---

### 3. The Cryptographic Signature

The signature guarantees that the token has not been altered:

$$\text{Signature} = \text{HMAC-SHA256}(\text{base64Url}(\text{Header}) + "." + \text{base64Url}(\text{Payload}), \text{SecretKey})$$

- If an attacker tampers with a single character in the payload (e.g. changing `"tokenBudget": 500` to `500000`), the calculated signature will not match the token's signature, and Spring Security will reject it immediately.

#### Symmetric (HS256) vs Asymmetric (RS256 / ES256)
- **HS256 (HMAC-SHA256)**: Single shared secret key used for both signing and verification. Ideal for monolithic Spring Boot apps or internal microservices within a trusted VPC.
- **RS256 (RSA-SHA256)**: Private key signs the token (Auth Server); Public key verifies the token (Resource Servers). Ideal when frontend or third-party gateways verify tokens without knowing the private signing key.

---

## 4. The Access Token vs Refresh Token Architecture

Because JWTs are stateless, **a signed token cannot be revoked before its expiration date** without maintaining a centralized blacklist database (which defeats the purpose of statelessness).

To solve this, enterprise AI platforms use the **Dual-Token Pattern**:

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant Auth as Auth Server (/api/v1/auth/login)
    participant API as Spring Boot AI Gateway (/api/v1/chat)

    Client->>Auth: POST /login (username, password)
    Auth-->>Client: Returns Access Token (15m exp) + Refresh Token (7d exp)
    
    loop For 15 minutes
        Client->>API: POST /chat (Header: Authorization: Bearer <Access Token>)
        API->>API: Verifies signature locally in 0.05ms
        API-->>Client: 200 OK + AI Completion
    end

    Note over Client,API: After 15 minutes: Access Token Expires!
    Client->>API: POST /chat (Expired Access Token)
    API-->>Client: 401 Unauthorized (Token Expired)

    Client->>Auth: POST /refresh (Refresh Token)
    Auth->>Auth: Validates refresh token in DB & checks revocation
    Auth-->>Client: Returns NEW Access Token (15m) + NEW Rotated Refresh Token
```

### Why This Design is Secure
1. If an attacker intercepts the **Access Token**, it becomes completely useless after **15 minutes**.
2. The **Refresh Token** is stored securely (e.g. HTTP-Only, Secure, SameSite Cookie).
3. **Refresh Token Rotation (RTR)**: Every time a refresh token is used to obtain a new access token, the old refresh token is invalidated in the database and a new one is issued. If an attacker tries to reuse an old refresh token, the server detects a breach and revokes all tokens for that user immediately!

---

## 5. Integrating JWT with Spring Security: `JwtAuthenticationFilter`

In production, you register a custom filter that intercepts incoming HTTP requests:

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, UserDetailsService userDetailsService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {

        String jwt = extractBearerToken(request);

        if (jwt != null && tokenProvider.validateToken(jwt)) {
            String username = tokenProvider.extractUsername(jwt);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Create Spring Security Authentication token
            var authToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
            );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Attach to current thread's SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7).trim();
        }
        return null;
    }
}
```

---

## 6. Security Warning: LocalStorage vs HTTP-Only Cookies

Where should client applications store JWT tokens?

| Storage Option | Vulnerable to XSS? | Vulnerable to CSRF? | Senior Recommendation |
| :--- | :---: | :---: | :--- |
| **`localStorage`** | ❌ **YES!** Any injected JavaScript can execute `localStorage.getItem('token')` and exfiltrate your token. | ✅ Immune | Avoid for high-value AI applications. |
| **`HTTP-Only Cookie`** | ✅ **Immune!** JavaScript running in the browser cannot read HTTP-Only cookies. | ❌ Requires CSRF mitigation or `SameSite=Strict` | **Recommended for Web Dashboards**. |
| **In-Memory Variable** | ✅ Immune (disappears on page refresh; refreshed via silent refresh) | ✅ Immune | **Enterprise Gold Standard**. |

---

## 7. Hands-On Code Walkthrough

In this day's companion code (`Phase_05_Spring_Security/Day_28_JWT_Authentication/code/`), we built a pure Java 21 cryptographic token engine without external dependencies:

1. **`JwtClaims.java`**: Record modeling subject, tenantId, roles, tokenBudget, and timestamps.
2. **`JwtTokenService.java`**:
   - `generateToken(claims)`: Formats JSON header and payload, signs with HMAC-SHA256 (`javax.crypto.Mac`), and returns `header.payload.signature`.
   - `validateAndParseClaims(token)`: Verifies signature integrity, validates expiration, and extracts claims.
3. **`JwtDemo.java`**: Executable driver demonstrating:
   - Scenario 1: Generating a signed JWT.
   - Scenario 2: Validating an authentic token and decoding claims.
   - Scenario 3: Simulating a tampering attack (modifying payload breaks signature).
   - Scenario 4: Rejecting expired tokens.

---

## 8. Step-by-Step Compilation & Execution

```powershell
# 1. Navigate to course workspace
cd "c:\Users\sriva\OneDrive\Desktop\GEN AI COURSE\JAVA"

# 2. Compile Day 28 code
javac Phase_05_Spring_Security/Day_28_JWT_Authentication/code/*.java

# 3. Run JwtDemo
java -cp Phase_05_Spring_Security/Day_28_JWT_Authentication code.JwtDemo
```

### Verified Output

```
================================================================================
 DAY 28: JWT AUTHENTICATION FROM SCRATCH — CREATION, VALIDATION & INTEGRITY     
================================================================================

--- SCENARIO 1: Generating Signed Enterprise AI JWT ---
 Generated Compact JWT (Header.Payload.Signature):
 eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c3JfYWxpY2VfMTIzIiwidGVuYW50SWQiOiJvcmdfZGVlcG1pbmRfYWkiLCJyb2xlcyI6WyJST0xFX1VTRVIiLCJTQ09QRV9haTpjaGF0Il0sInRva2VuQnVkZ2V0Ijo1MDAwMDAsImlhdCI6MTc4ODk1MzAyNywiZXhwIjoxNzg4OTU2NjI3fQ.0dtmybZJkbzNU4f9nqN-OlZp6JJdeBi6bC3vFwFqxjI

 Token Breakdown:
   [1] Encoded Header:    eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9
   [2] Encoded Payload:   eyJzdWIiOiJ1c3JfYWxpY2VfMTIzIiwidGVuYW50SWQiOiJvcmdfZGVlcG1pbmRfYWkiLCJyb2xlcyI6WyJST0xFX1VTRVIiLCJTQ09QRV9haTpjaGF0Il0sInRva2VuQnVkZ2V0Ijo1MDAwMDAsImlhdCI6MTc4ODk1MzAyNywiZXhwIjoxNzg4OTU2NjI3fQ
   [3] HMAC-SHA256 Sig:   0dtmybZJkbzNU4f9nqN-OlZp6JJdeBi6bC3vFwFqxjI

--- SCENARIO 2: Validating Authentic Token ---
 [VALIDATION SUCCESS] Cryptographic signature verified!
   Subject:       usr_alice_123
   Tenant ID:     org_deepmind_ai
   Roles:         [ROLE_USER, SCOPE_ai:chat]
   Token Budget:  500000 tokens
   Expires At:    2026-09-09T12:23:47Z

--- SCENARIO 3: Tampering Attack Simulation ---
 Attacker substituted payload with elevated admin privileges:
 [DEFENSE TRIGGERED] Tampered token rejected: JWT Signature Verification Failed: Token has been altered or secret key is invalid!

--- SCENARIO 4: Expired Token Rejection ---
 Attempting to authenticate with expired token...
 [DEFENSE TRIGGERED] Expired token rejected: JWT Token Expired: Token expired at 2026-09-09T10:23:47Z

================================================================================
 DAY 28 DEMONSTRATION COMPLETE: JWT CRYPTOGRAPHIC DEFENSES VERIFIED!            
================================================================================
```

---

## 9. Hands-On Exercises (With Complete Solutions)

### Exercise 1: JJWT-based Token Provider Component
**Task**: Implement `JwtTokenProvider` using modern JJWT (v0.12+) with strong cryptographic key generation (`Jwts.SIG.HS256.key()`).

#### Solution:
```java
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessValidityMs = 15 * 60 * 1000; // 15 mins

    public JwtTokenProvider(@Value("${app.jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(String username, String tenantId, List<String> roles) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + accessValidityMs);

        return Jwts.builder()
            .subject(username)
            .claim("tenantId", tenantId)
            .claim("roles", roles)
            .issuedAt(now)
            .expiration(validity)
            .signWith(secretKey, Jwts.SIG.HS256)
            .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
```

---

### Exercise 2: Refresh Token Entity with Rotation & Revocation
**Task**: Design the JPA entity `RefreshToken` with `tokenHash`, `userId`, `revoked`, and `expiresAt` to support Refresh Token Rotation.

#### Solution:
```java
@Entity
@Table(name = "refresh_tokens", indexes = @Index(name = "idx_token_hash", columnList = "token_hash", unique = true))
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash; // SHA-256 hash of the refresh token secret

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public boolean isValid() {
        return !revoked && Instant.now().isBefore(expiresAt);
    }

    public void revoke() {
        this.revoked = true;
    }
}
```

---

### Exercise 3: Dynamic Tenant Extraction in Security Context
**Task**: Write a helper method that extracts `tenantId` from the current JWT authentication token for RAG queries.

#### Solution:
```java
public class SecurityUtils {

    public static String getCurrentTenantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtToken) {
            return (String) jwtToken.getTokenAttributes().get("tenantId");
        }
        throw new IllegalStateException("Current request is not authenticated via enterprise JWT");
    }
}
```

---

## 10. Self-Check Quiz

### Q1: Can an attacker decrypt a JWT token if they intercept it over HTTP?
> **Answer**: JWT tokens are **encoded**, not encrypted (unless using JWE - JSON Web Encryption). Anyone who intercepts a standard JWT can decode the payload using Base64Url decoding and view all claims. The security of JWT lies in **tamper-evidence (integrity)**: the attacker cannot modify any claim without invalidating the cryptographic signature.

### Q2: Why is the secret key for HMAC-SHA256 required to be at least 256 bits (32 bytes)?
> **Answer**: The SHA-256 algorithm produces a 256-bit hash. If the signing key is shorter than 256 bits, it is vulnerable to brute-force and dictionary attacks where an attacker can compute possible keys offline until a signature matches, allowing them to forge valid tokens.

### Q3: Why should access tokens have a short lifespan (e.g. 15 minutes)?
> **Answer**: Because JWTs are stateless, they cannot be easily revoked before they expire without adding stateful server blacklists. Keeping access tokens short-lived ensures that if a token is intercepted or leaked, the attacker's window of access is strictly limited. The legitimate client uses a refresh token to seamlessly obtain new access tokens.

### Q4: What happens if an attacker attempts to elevate their role from `ROLE_USER` to `ROLE_ADMIN` inside a JWT?
> **Answer**: When the server receives the token, it recalculates the HMAC-SHA256 signature using the secret key over the modified header and payload. Because the payload changed, the calculated signature will not match the signature appended to the token. Spring Security throws a signature verification exception and rejects the request with `401 Unauthorized`.

### Q5: In which Spring Security filter order should `JwtAuthenticationFilter` be placed?
> **Answer**: It should be added **before** `UsernamePasswordAuthenticationFilter`:
> `.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)`

---

### What's Next?

We can now authenticate users and verify cryptographic tokens. But once a user is authenticated, how do we restrict access to specific AI models, prompt actions, and administrative operations at the Java method level?

Proceed to **[Day 29: Role-Based Access Control (RBAC) & Method-Level Security (`@PreAuthorize`, `@Secured`, SpEL)](../Day_29_RBAC_Method_Level_Security/Day_29_RBAC_Method_Level_Security.md)**!
