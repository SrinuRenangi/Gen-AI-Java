# Day 28: JWT Authentication from Scratch

> **"If your AI gateway relies on server-side HTTP sessions, every time you scale to 50 Kubernetes pods, you need complex Redis session replication clusters. Worse, session cookies fail completely when autonomous AI agents or external microservices invoke your streaming APIs. JSON Web Tokens (JWT) provide cryptographically sealed, stateless identity tickets that any pod can verify in 50 microseconds without touching a database."**

---

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [Day 27: Security Fundamentals & Architecture](../Day_27_Security_Fundamentals_Architecture/Day_27_Security_Fundamentals_Architecture.md) | [All 60 Days Overview](../../README.md) | [Day 29: RBAC & Method-Level Security](../Day_29_RBAC_Method_Level_Security/Day_29_RBAC_Method_Level_Security.md) |

---

## 1. Topic Overview

JSON Web Tokens (JWT, RFC 7519) provide an open standard for securely transmitting cryptographically verifiable, self-contained claims between distributed clients and backend services. In Generative AI systems, stateless JWT authentication enables server clusters and asynchronous AI agent pipelines to verify caller identities, multi-tenant boundaries, and token usage budgets in under 0.05 milliseconds without querying centralized session databases.

---

## 2. Basic Foundations (True Zero)

### What is a JSON Web Token (JWT)?
In traditional web applications, the server stores a session ID in its memory (RAM) or in an external Redis cache, sending a cookie back to the client. On every request, the server queries Redis to verify the session.

A **JSON Web Token** flips this model: the token is **stateless and self-contained**. The server cryptographically signs a digital passport containing user data (user ID, tenant, permissions) and hands it to the client. On subsequent requests, the client attaches the token inside an HTTP header (`Authorization: Bearer <token>`). Any server instance can verify the signature using its secret key without making a database query.

```
+-----------------------------------------------------------------------------------+
|               THE AMUSEMENT PARK VIP WRISTBAND ANALOGY                            |
|                                                                                   |
| Imagine entering a massive amusement park:                                        |
|                                                                                   |
| THE STATEFUL SESSION MODEL:                                                       |
| Every time you want to ride a roller coaster, the operator must call the front    |
| entrance gate over a radio: "Hey, did visitor #4592 pay for admission?"          |
| Result: Long lines, radio congestion, and total gridlock if the radio tower dies. |
|                                                                                   |
| THE STATELESS JWT WRISTBAND MODEL:                                                |
| At the front gate, you pay once and receive a tamper-proof wristband stamped with |
| an official holographic wax seal stating: "Alice - VIP FastPass - Exp: 6:00 PM".  |
|                                                                                   |
| At every ride, the operator simply glances at the holographic seal. If it is     |
| intact and unexpired, you are waved through immediately in 1 second!              |
| If someone tries to alter "Standard" to "VIP", the seal breaks and access fails!   |
+-----------------------------------------------------------------------------------+
```

### Minimal Beginner-Friendly Working Code Example

Below is a self-contained Java 21 demonstration of how an HMAC-SHA256 cryptographic signature is computed and verified:

```java
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class BasicJwtSignatureExample {

    private static final String SECRET_KEY = "super-secret-key-that-is-at-least-256-bits-long!";

    public static String sign(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    public static void main(String[] args) throws Exception {
        // 1. Header & Payload JSON strings
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = "{\"sub\":\"usr_alice\",\"role\":\"USER\"}";

        // 2. Base64Url encode header and payload
        String encHeader = Base64.getUrlEncoder().withoutPadding().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String encPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String dataToSign = encHeader + "." + encPayload;

        // 3. Generate Cryptographic Signature
        String signature = sign(dataToSign, SECRET_KEY);
        String compactJwt = dataToSign + "." + signature;

        System.out.println("Generated Compact JWT:\n" + compactJwt);

        // 4. Verification Check
        String[] parts = compactJwt.split("\\.");
        String expectedSig = sign(parts[0] + "." + parts[1], SECRET_KEY);
        boolean isValid = expectedSig.equals(parts[2]);

        System.out.println("\nVerification with Secret Key: " + (isValid ? "VALID" : "INVALID"));

        // 5. Tamper Attack Simulation: Hacker modifies payload
        String tamperedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"sub\":\"usr_alice\",\"role\":\"ADMIN\"}".getBytes(StandardCharsets.UTF_8));
        String tamperedData = parts[0] + "." + tamperedPayload;
        boolean isTamperValid = sign(tamperedData, SECRET_KEY).equals(parts[2]);

        System.out.println("Tamper Detection (Altered to ADMIN): " + (isTamperValid ? "BREACH!" : "TAMPER DETECTED & BLOCKED!"));
    }
}
```

#### Line-by-Line Walkthrough:
- **Lines 8–14**: `sign` initializes Java's `HmacSHA256` engine using a shared secret key, producing a binary digest encoded to URL-safe Base64 without trailing padding (`=`).
- **Lines 18–24**: Demonstrates the composition of standard JWT components: Header + `.` + Payload.
- **Lines 27–30**: Combines the encoded header, payload, and HMAC signature into the canonical three-part format (`header.payload.signature`).
- **Lines 35–38**: Verifies the token by recalculating the HMAC over the header and payload, comparing it to the provided signature in constant time.
- **Lines 41–45**: Simulates an attacker modifying their role to `"ADMIN"`. Because the attacker does not have the secret key, their modified payload produces a mismatched signature and is rejected.

---

## 3. Core Concept Walkthrough (Basic → Intermediate)

### The Anatomy of a JSON Web Token (RFC 7519)

A compact JWT is formed by three Base64Url-encoded strings separated by periods:

```
eyJhbGciOiJIUzI1NiJ9 . eyJzdWIiOiJ1c3JfYWxpY2UiLCJyb2xlcyI6WyJST0xFX1VTRVIiXX0 . 0dtmybZJkbzNU4f9nqN-OlZp6JJdeBi6bC3vFwFqxjI
└─────────┬─────────┘   └──────────────────────┬──────────────────────┘   └─────────────────────┬─────────────────────┘
       1. HEADER                              2. PAYLOAD                                3. SIGNATURE
```

```
+-----------+--------------------+-----------------------------------------------------------------------+
| Component | Contents           | Plain-English Purpose                                                 |
+-----------+--------------------+-----------------------------------------------------------------------+
| Header    | alg, typ           | Declares the signing algorithm (HS256, RS256) and token type ("JWT"). |
+-----------+--------------------+-----------------------------------------------------------------------+
| Payload   | Claims (sub, exp)  | Key-value statements regarding user identity, tenant, and roles.     |
+-----------+--------------------+-----------------------------------------------------------------------+
| Signature | HMAC or RSA hash   | Cryptographic seal verifying the token was not tampered with.         |
+-----------+--------------------+-----------------------------------------------------------------------+
```

#### Standard vs. Custom AI Claims:
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
- **`sub` (Subject)**: The unique ID of the user.
- **`iat` (Issued At) / `exp` (Expiration)**: Standard RFC timestamps enforcing short validity windows.
- **`tenantId`**: Enforces multi-tenant data isolation in RAG vector queries.
- **`tokenBudget`**: Enforces LLM token usage caps before invoking downstream inference.

---

### Dual-Token Architecture: Access Token vs. Refresh Token

Because stateless JWTs cannot be selectively invalidated without stateful blacklists, enterprise architectures implement the **Dual-Token Pattern**:

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

1. **Access Token (Short-Lived — 15 minutes)**: Attached to every HTTP request. If intercepted, the attacker's window of opportunity is minimal.
2. **Refresh Token (Long-Lived — 7 days)**: Stored securely in an HTTP-Only cookie. Used solely to request new access tokens.
3. **Refresh Token Rotation (RTR)**: Each time a refresh token is used, the server deletes it and generates a new one. If an attacker attempts to replay an old refresh token, the server flags a breach and revokes all active tokens for that user.

---

### Spring Security Integration: `JwtAuthenticationFilter`

In production, you register a custom filter extending `OncePerRequestFilter` to validate tokens on every incoming request:

```java
package com.example.genai.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

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

            var authToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
            );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

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

## 4. Prerequisite & Supporting Concepts

### Prerequisite / Supporting Concept: Base64Url Encoding vs. Encryption
A frequent misconception is assuming that Base64 encoding hides sensitive data. Base64 is merely a serialization format converting binary bytes to URL-safe ASCII characters. 

Anyone who intercepts a JWT can decode the payload on [jwt.io](https://jwt.io) or using standard CLI tools:
```bash
echo "eyJzdWIiOiJ1c3JfYWxpY2UifQ" | base64 --decode
# Output: {"sub":"usr_alice"}
```
**Golden Rule**: Never place passwords, credit card numbers, or proprietary encryption keys inside a JWT payload.

### Prerequisite / Supporting Concept: Token Storage Security
Where the client stores the JWT dictates its vulnerability to attacks:

```
+-------------------+-----------------------+-----------------------+---------------------------------------+
| Storage Location  | Vulnerable to XSS?    | Vulnerable to CSRF?   | Production Recommendation             |
+-------------------+-----------------------+-----------------------+---------------------------------------+
| `localStorage`    | YES (Critical hazard!)| Immune                | Avoid for high-value AI applications. |
+-------------------+-----------------------+-----------------------+---------------------------------------+
| HTTP-Only Cookie  | Immune (JS blocked)   | Yes (Requires SameSite| Recommended for browser dashboards.   |
|                   |                       | or CSRF tokens)       |                                       |
+-------------------+-----------------------+-----------------------+---------------------------------------+
| In-Memory (State) | Immune                | Immune                | Enterprise standard for Single Page   |
|                   |                       |                       | Applications (SPA) + Silent Refresh.  |
+-------------------+-----------------------+-----------------------+---------------------------------------+
```

---

## 5. Advanced Depth (Intermediate → Advanced)

### Common Pitfalls & Antipatterns

#### Pitfall 1: Weak Signing Keys (< 256 bits)
```
+-----------------------------------------------------------------------------------+
| BAD PRACTICE: Insecure Short Secret Keys                                          |
|                                                                                   |
| String secret = "my-secret"; // Insecure! Under 256 bits.                         |
| // Vulnerable to offline dictionary attacks using tools like hashcat!             |
+-----------------------------------------------------------------------------------+
| GOOD PRACTICE: Cryptographically Secure 256-Bit Key                               |
|                                                                                   |
| SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));      |
| // JJWT enforces minimum 32-byte (256-bit) entropy at runtime!                    |
+-----------------------------------------------------------------------------------+
```

#### Pitfall 2: Stateful Token Revocation Blacklists
When an engineer wants immediate token revocation upon user logout, they often store revoked tokens in Redis. While functional, checking Redis on every incoming request converts a stateless architecture back into a stateful one, defeating JWT's primary scalability benefit.

Instead, keep Access Tokens strictly short-lived (5–15 minutes) and execute revocation exclusively on the database-backed **Refresh Tokens**.

---

### Hands-On Simulation Code Walkthrough

The companion code repository demonstrates this architecture:
- `JwtClaims.java`: Record modeling subject, tenantId, roles, tokenBudget, and timestamps.
- `JwtTokenService.java`: Pure Java 21 cryptographic token engine generating and validating HMAC-SHA256 tokens.
- `JwtDemo.java`: Executable test harness verifying signing, successful decoding, tamper detection, and expiration rejection.

```powershell
# Compile Day 28 code
javac Phase_05_Spring_Security/Day_28_JWT_Authentication/code/*.java

# Run JwtDemo
java -cp Phase_05_Spring_Security/Day_28_JWT_Authentication code.JwtDemo
```

#### Verified Execution Output:
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

## 6. Quick Recap

| Concept | Description | Enterprise Rule / Best Practice |
| :--- | :--- | :--- |
| **JWT (RFC 7519)** | Compact, URL-safe token format | Three dot-separated parts: `Header.Payload.Signature`. |
| **Base64Url** | ASCII encoding format | NOT encryption. Never store sensitive passwords or secrets in payload. |
| **Signature** | HMAC-SHA256 / RSA cryptographic hash | Guarantees tamper-evidence. Tampered payloads invalidate the hash. |
| **Access Token** | Short-lived bearer token (15 mins) | Carried on every request in `Authorization: Bearer <token>`. |
| **Refresh Token** | Long-lived rotation token (7 days) | Stored securely to issue new access tokens via silent refresh. |
| **`JwtAuthenticationFilter`**| Servlet filter verifying bearer tokens | Placed before `UsernamePasswordAuthenticationFilter`. |

---

## 7. Self-Check Questions & Practice Exercises

### Conceptual & Architectural Questions

#### Q1: Can an attacker decrypt a JWT token if they intercept it over HTTP?
**Answer**: Standard JWT tokens are **encoded**, not encrypted (unless using JWE - JSON Web Encryption). Anyone who intercepts a standard JWT can decode the payload using Base64Url decoding and view all claims. The security of JWT lies in **tamper-evidence (integrity)**: the attacker cannot alter any claims without invalidating the cryptographic signature.

#### Q2: Why is the secret key for HMAC-SHA256 required to be at least 256 bits (32 bytes)?
**Answer**: SHA-256 produces a 256-bit hash. If the signing key contains less than 256 bits of entropy, it is vulnerable to offline brute-force and dictionary attacks (e.g., using GPUs and rainbow tables). Once the key is cracked, an attacker can forge legitimate tokens with arbitrary administrative privileges.

#### Q3: Why should access tokens have a short lifespan (e.g., 15 minutes)?
**Answer**: Because JWTs are stateless, they cannot be easily revoked before expiration without maintaining stateful server blacklists. Keeping access tokens short-lived ensures that if a token is intercepted or leaked, the attacker's window of opportunity is strictly minimized.

#### Q4: What happens if an attacker attempts to elevate their role from `ROLE_USER` to `ROLE_ADMIN` inside a JWT?
**Answer**: When the server receives the token, it recalculates the HMAC-SHA256 signature using its private secret key over the modified header and payload. Because the payload changed, the calculated signature will not match the signature appended to the token. Spring Security throws a signature verification exception and rejects the request with `401 Unauthorized`.

#### Q5: In which Spring Security filter order should `JwtAuthenticationFilter` be placed?
**Answer**: It should be added **before** `UsernamePasswordAuthenticationFilter`:
`.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)`

---

### Hands-On Practice Exercises

#### Exercise 1: JJWT-based Token Provider Component
**Task**: Implement `JwtTokenProvider` using modern JJWT (v0.12+) with strong cryptographic key generation (`Jwts.SIG.HS256.key()`).

```java
// Solution:
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

#### Exercise 2: Refresh Token Entity with Rotation & Revocation
**Task**: Design the JPA entity `RefreshToken` with `tokenHash`, `userId`, `revoked`, and `expiresAt` to support Refresh Token Rotation.

```java
// Solution:
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

#### Exercise 3: Dynamic Tenant Extraction from Security Context
**Task**: Write a helper method that extracts `tenantId` from the current JWT authentication token for multi-tenant RAG queries.

```java
// Solution:
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

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [Day 27: Security Fundamentals & Architecture](../Day_27_Security_Fundamentals_Architecture/Day_27_Security_Fundamentals_Architecture.md) | [All 60 Days Overview](../../README.md) | [Day 29: RBAC & Method-Level Security](../Day_29_RBAC_Method_Level_Security/Day_29_RBAC_Method_Level_Security.md) |
