# 🛡️ Day 24: Transactions, Concurrency & Auditing
## ACID Guarantees, Spring `@Transactional` Internals, Optimistic Locking (`@Version`) & JPA Auditing

| ⬅️ Previous Day | 📚 Course Hub | ➡️ Next Day |
|:---|:---:|---:|
| [← Day 23: Entity Relationships & Fetch Strategies](../Day_23_Entity_Relationships_Fetch_Strategies/Day_23_Entity_Relationships_Fetch_Strategies.md) | [All 60 Days Overview](../../README.md) | [Day 25: Database Migrations (Flyway) & Docker →](../Day_25_Database_Migrations_Docker/Day_25_Database_Migrations_Docker.md) |

[![Phase](https://img.shields.io/badge/Phase_04-Spring_Data_JPA_%26_Databases-blue.svg?style=for-the-badge)](../../README.md)
[![Day](https://img.shields.io/badge/Day-24_of_60-blue.svg?style=for-the-badge)](../../README.md)
[![Difficulty](https://img.shields.io/badge/Difficulty-Intermediate_to_Advanced-orange.svg?style=for-the-badge)](../../README.md)
[![Topic](https://img.shields.io/badge/JPA-Transactions_%26_Concurrency-purple.svg?style=for-the-badge)](../../README.md)

---

## 1. Topic Overview

Transaction management, concurrency control, and auditing enforce mathematical integrity and regulatory compliance across enterprise database systems. In Generative AI platforms where concurrent Virtual Threads deduct pre-paid token quotas, persist multi-turn messages, and update shared prompt templates simultaneously, mastering Spring's `@Transactional` proxy mechanics, `@Version` optimistic locking, and automated JPA auditing guarantees all-or-nothing execution without race conditions or lost updates.

---

## 2. Basic Foundations (True Zero)

### Plain English Definitions
- **Transaction (`@Transactional`)**: An atomic, protective envelope around one or more database operations ensuring that either all statements succeed (Commit), or if any failure occurs, all changes are undone (Rollback) as if they never happened.
- **ACID Guarantees**:
  - **Atomicity**: All-or-nothing execution.
  - **Consistency**: Database invariants and constraints are preserved.
  - **Isolation**: Concurrent transactions cannot observe each other's uncommitted intermediate state.
  - **Durability**: Committed data is safely written to permanent storage (PostgreSQL Write-Ahead Log).
- **Race Condition & Lost Update**: A concurrency bug where two threads read the identical balance (e.g. 100 tokens), perform separate deductions, and write back values—causing one thread's update to be overwritten and lost.
- **Optimistic Locking (`@Version`)**: A concurrency strategy where an entity carries an integer version number. When updating, Hibernate verifies that the database version matches the in-memory version; if another thread updated the row first, an `OptimisticLockException` is raised instead of overwriting data.
- **JPA Auditing**: Automated Spring Data listeners that automatically record `@CreatedDate`, `@LastModifiedDate`, and `@CreatedBy` metadata on database rows without manual code.

### Relatable Physical Analogy: The Bank Vault Notary & Google Docs
```
TRADITIONAL ACCESS (NO TRANSACTION):
[ Clerk hands customer $500 cash ] ──► System crashes before recording debit!
Outcome: Bank lost $500, account ledger corrupted.

ACID TRANSACTION (ALL-OR-NOTHING NOTARIZED ENVELOPE):
[ Certified Notary seals transaction ]
  ├── 1. Verify cash in vault
  ├── 2. Count $500 to customer
  └── 3. Debit customer ledger
  If the pen runs out of ink at Step 2:
  Notary tears up the contract and returns cash to vault! (ATOMIC ROLLBACK)

OPTIMISTIC LOCKING (GOOGLE DOCS VERSIONING):
[ Alice and Bob open Version 4 of a document ]
  ├── Alice saves changes ──► Document becomes Version 5!
  └── Bob attempts to save his changes to Version 4:
      System alerts Bob: "Conflict! Document was modified by Alice. Please reload."
```

Transactions guarantee that multi-step AI workflows—such as token deduction, inference dispatch, and message history storage—either **complete in their entirety** or **leave no partial trace whatsoever**.

### Minimal Beginner-Friendly Working Code Example

Let us examine a minimal `@Transactional` service that transfers tokens atomically between accounts:

```java
package com.javagenai.day24;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MinimalTokenTransferService {

    private final AccountRepository accountRepo;

    public MinimalTokenTransferService(AccountRepository accountRepo) {
        this.accountRepo = accountRepo;
    }

    @Transactional(rollbackFor = Exception.class)
    public void transferTokens(Long fromId, Long toId, int amount) {
        Account sender = accountRepo.findById(fromId).orElseThrow();
        Account recipient = accountRepo.findById(toId).orElseThrow();

        if (sender.getBalance() < amount) {
            throw new IllegalArgumentException("Insufficient token balance!");
        }

        sender.deduct(amount);
        recipient.add(amount);

        // If an unexpected exception occurs here, both sender and recipient
        // modifications are completely rolled back by Spring!
    }
}
```

#### Line-by-Line Walkthrough
1. `@Transactional(rollbackFor = Exception.class)`: Instructs Spring's AOP proxy to begin a database transaction before method execution and commit it after return. If any exception occurs, it rolls back all database modifications.
2. `sender.deduct(amount)` & `recipient.add(amount)`: Mutates both managed entities.
3. Hibernate Dirty Checking flushes both updates simultaneously in a single atomic database transaction.

---

## 3. Core Concept Walkthrough (Basic → Intermediate)

### 3.1 Under the Hood: Spring `@Transactional` Proxy Architecture
Spring implements declarative transaction management via **Spring AOP Dynamic Proxies**:

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant Proxy as Spring AOP Proxy (TransactionInterceptor)
    participant TM as PlatformTransactionManager
    participant DB as PostgreSQL Connection
    participant Target as ChatService (Your Code)

    Client->>Proxy: invoke executePromptPipeline()
    Proxy->>TM: getTransaction()
    TM->>DB: setAutoCommit(false); BEGIN TRANSACTION;
    Proxy->>Target: invoke original method
    
    alt Method executes successfully
        Target-->>Proxy: returns CompletionResponse
        Proxy->>TM: commit()
        TM->>DB: COMMIT;
        Proxy-->>Client: 200 OK
    else RuntimeException thrown
        Target-->>Proxy: throws GenAiException
        Proxy->>TM: rollback()
        TM->>DB: ROLLBACK;
        Proxy-->>Client: 500 / 422 Error (Database clean!)
    end
```

### 3.2 The Self-Invocation Trap
Because transaction boundaries are established by Spring AOP proxies:

```java
@Service
public class PromptService {

    public void processPrompt(String prompt) {
        // ❌ THE SELF-INVOCATION TRAP!
        // Calling this method directly bypasses the Spring AOP proxy!
        // NO TRANSACTION IS STARTED!
        this.saveAuditLog(prompt);
    }

    @Transactional
    public void saveAuditLog(String prompt) {
        // Runs WITHOUT a transaction!
    }
}
```
**Solution**: Always invoke transactional methods from a separate injected `@Service` bean so Spring's proxy can intercept the call.

### 3.3 Rollback Rules: Checked vs. Unchecked Exceptions
> [!IMPORTANT]
> By default, Spring `@Transactional` rolls back **ONLY on unchecked exceptions** (`RuntimeException` and `Error`). If a checked exception (`Exception`, `IOException`) is thrown, Spring **COMMITS** the transaction!

Always declare rollback explicitly:
```java
// ✅ SENIOR PATTERN: Rolls back on ALL checked and unchecked exceptions
@Transactional(rollbackFor = Exception.class)
public void executePipeline() throws Exception { ... }
```

### 3.4 Transaction Propagation: `REQUIRED` vs. `REQUIRES_NEW`
- **`Propagation.REQUIRED` (Default)**: Joins an existing active transaction; creates a new one if none exists.
- **`Propagation.REQUIRES_NEW`**: Always suspends the current transaction and executes in an isolated, brand-new transaction.
  - **AI Use Case**: Security violation auditing. Even if the user's inference pipeline fails and rolls back, the security audit log **must commit permanently**!

```java
@Service
public class SecurityAuditService {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logViolation(String userId, String reason) {
        // Commits independently of caller's transaction status!
        auditRepo.save(new SecurityAuditRecord(userId, reason));
    }
}
```

### 3.5 Concurrency Control: Optimistic Locking with `@Version`
Optimistic locking validates that no concurrent transaction altered the record during computation:

```java
package com.enterprise.ai.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "token_quotas")
public class TokenQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int remainingTokens;

    @Version // 👈 Hibernate checks and increments this on every UPDATE
    private Long version;

    public void deductTokens(int tokens) {
        if (this.remainingTokens < tokens) {
            throw new IllegalStateException("Quota depleted");
        }
        this.remainingTokens -= tokens;
    }
}
```

#### Under the Hood SQL Generation:
```sql
UPDATE token_quotas SET remaining_tokens = 40000, version = 2
WHERE id = 1 AND version = 1;
```
If another thread updated `version` to 2 in the interim, PostgreSQL returns `0 rows updated`. Hibernate detects this and raises an `OptimisticLockException`, preventing silent token balance corruption!

### 3.6 Automated JPA Auditing
Track entity creation and modification automatically:

```java
package com.enterprise.ai.domain;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.Instant;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant lastModifiedAt;

    @CreatedBy
    @Column(nullable = false, updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(nullable = false)
    private String lastModifiedBy;
}
```

Enable auditing in configuration:
```java
@Configuration
@EnableJpaAuditing
public class JpaAuditConfig {
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
            .map(Authentication::getName)
            .or(() -> Optional.of("system"));
    }
}
```

---

## 4. Prerequisite & Supporting Concepts

### Prerequisite / Supporting Concept: Isolation Levels in PostgreSQL

| Isolation Level | Dirty Read | Non-Repeatable Read | Phantom Read |
| :--- | :---: | :---: | :---: |
| `READ_UNCOMMITTED` | ❌ Allowed | ❌ Allowed | ❌ Allowed |
| `READ_COMMITTED` *(Postgres Default)* | ✅ Prevented | ❌ Allowed | ❌ Allowed |
| `REPEATABLE_READ` | ✅ Prevented | ✅ Prevented | ✅ Prevented *(In Postgres MVCC)* |
| `SERIALIZABLE` | ✅ Prevented | ✅ Prevented | ✅ Prevented |

### Prerequisite / Supporting Concept: The Plain English Bridge to Transactions

| Transaction Concept | What Junior Developers Think | What Spring Actually Does | Plain English Advantage |
| :--- | :--- | :--- | :--- |
| **Beginning TX** | Database starts it automatically | Spring AOP proxy sets `autoCommit(false)` | Clear programmatic boundary |
| **Rollback Rule** | Rolls back on any exception | Rolls back ONLY on `RuntimeException` | Must declare `rollbackFor = Exception.class` |
| **Swallowing Errors**| `catch (Exception e) { log.error(); }` | Spring thinks method succeeded and commits! | Hiding exceptions causes partial data corruption |
| **Optimistic Lock** | Needing heavy database locks | Adds integer `@Version` to entity | Google Docs style conflict resolution |

---

## 5. Advanced Depth (Intermediate → Advanced)

### 5.1 Pessimistic Locking with `SELECT ... FOR UPDATE`
When contention is extremely high (e.g. hundreds of concurrent workers hitting a single tenant quota bucket), optimistic locking retries waste CPU. Use **Pessimistic Locking**:

```java
@Repository
public interface TokenQuotaRepository extends JpaRepository<TokenQuota, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM TokenQuota q WHERE q.tenantId = :tenantId")
    Optional<TokenQuota> findByTenantIdForUpdate(@Param("tenantId") String tenantId);
}
```
#### Generated SQL:
```sql
SELECT * FROM token_quotas WHERE tenant_id = 'org_acme' FOR UPDATE;
```
PostgreSQL locks the row at the database level; all other threads queue until the transaction commits.

### 5.2 Common Mistakes & Misconceptions

#### Mistake 1: Swallowing Exceptions inside `@Transactional` Methods
```java
// ❌ CATASTROPHIC BUG: Catches error, hides it from Spring, commits partial data!
@Transactional
public void deductAndGenerate() {
    tokenRepo.deduct(100);
    try {
        callLlm();
    } catch (Exception e) {
        log.error("LLM failed", e); // Spring does NOT know failure occurred!
        // Tokens remain permanently deducted with zero output!
    }
}
```

#### Mistake 2: Calling `@Transactional` Methods Internally (`this.method()`)
Bypasses the Spring AOP proxy, causing the inner method to run with zero transaction management!

---

## 6. Quick Recap

| Mechanism | Annotation / Keyword | Operational Function | Enterprise AI Role |
| :--- | :--- | :--- | :--- |
| **Transaction Boundary**| `@Transactional(rollbackFor = Exception.class)` | Coordinates all-or-nothing commit/rollback | Guarantees atomic token deduction & chat saves |
| **Isolated Sub-Tx** | `propagation = Propagation.REQUIRES_NEW` | Runs in independent transaction | Saves security audits even if AI pipeline aborts |
| **Optimistic Locking** | `@Version` | Detects concurrent row update conflicts | Protects token quotas from lost update anomalies |
| **Pessimistic Locking**| `@Lock(LockModeType.PESSIMISTIC_WRITE)` | Database row-level `FOR UPDATE` lock | High-contention token bucket synchronization |
| **Audit Listener** | `@EntityListeners(AuditingEntityListener.class)`| Automatically records user and timestamps | SOC2 / HIPAA prompt modification governance |

---

## 7. Self-Check Questions & Practice Exercises

### Self-Check Questions

1. **What happens if a checked exception (e.g. `IOException`) is thrown from a `@Transactional` method without configuration?**
   - *Answer*: By default, Spring only rolls back for `RuntimeException` and `Error`. If a checked exception is thrown, Spring will **COMMIT** the transaction. To ensure rollback on all exceptions, configure `@Transactional(rollbackFor = Exception.class)`.
2. **Why does calling a `@Transactional` method from another method inside the same class fail to initiate a transaction?**
   - *Answer*: Spring transactions rely on runtime AOP proxies. Calling `this.method()` executes directly on the target object in JVM memory, bypassing the proxy interceptor entirely.
3. **How does Optimistic Locking with `@Version` prevent lost updates?**
   - *Answer*: Hibernate adds `WHERE id = ? AND version = ?` to the generated SQL `UPDATE`. If another transaction incremented `version` first, the update matches 0 rows, prompting Hibernate to throw an `OptimisticLockException`.
4. **When should you choose Pessimistic Locking over Optimistic Locking?**
   - *Answer*: When write contention on the same database record is extremely high, causing optimistic lock retries to frequently fail and waste CPU cycles. Pessimistic locking queues threads at the database level using `SELECT ... FOR UPDATE`.
5. **What is the purpose of `Propagation.REQUIRES_NEW`?**
   - *Answer*: It suspends any active transaction and starts a completely independent new transaction, ensuring its operations commit even if the calling outer transaction rolls back (ideal for audit logs).

---

### Hands-On Practice Exercises

#### 🏋️ Exercise 1: Resilient Token Deduction with Spring Retry
**Objective**: Construct a token deduction service method that catches `OptimisticLockException` and automatically retries with fresh balance data up to 3 times:

```java
package com.javagenai.day24;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResilientTokenService {

    private final TokenQuotaRepository repository;

    public ResilientTokenService(TokenQuotaRepository repository) {
        this.repository = repository;
    }

    @Retryable(
        retryFor = { ObjectOptimisticLockingFailureException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 50, multiplier = 2.0)
    )
    @Transactional(rollbackFor = Exception.class)
    public void deductTokens(String tenantId, int tokens) {
        TokenQuota quota = repository.findByTenantId(tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));

        quota.deductTokens(tokens);
        // Automatic dirty checking verifies version upon commit!
    }

    @Recover
    public void recover(ObjectOptimisticLockingFailureException ex, String tenantId, int tokens) {
        throw new IllegalStateException("High concurrency collision: failed to deduct tokens after 3 retries.");
    }
}
```

#### 🏋️ Exercise 2: AuditorAware Context Extraction
**Objective**: Build an `AuditorAware<String>` bean extracting the current username from Spring Security's context:

```java
package com.javagenai.day24;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecurityAuditorProvider implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.of("system");
        }
        return Optional.of(auth.getName());
    }
}
```

---

| ⬅️ Previous Day | 📚 Course Hub | ➡️ Next Day |
|:---|:---:|---:|
| [← Day 23: Entity Relationships & Fetch Strategies](../Day_23_Entity_Relationships_Fetch_Strategies/Day_23_Entity_Relationships_Fetch_Strategies.md) | [All 60 Days Overview](../../README.md) | [Day 25: Database Migrations (Flyway) & Docker →](../Day_25_Database_Migrations_Docker/Day_25_Database_Migrations_Docker.md) |
