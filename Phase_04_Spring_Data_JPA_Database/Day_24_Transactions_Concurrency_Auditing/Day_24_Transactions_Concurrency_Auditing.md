# Day 24: Transactions, Concurrency & Auditing

> **"In a single-threaded toy application, database updates are easy. In an enterprise Generative AI platform where thousands of concurrent Virtual Threads deduct token quotas, generate chat messages, and edit shared prompts simultaneously, you must guarantee ACID isolation, prevent lost updates with Optimistic Locking, and audit every change."**

---

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [Day 23: Entity Relationships & Fetch Strategies](../Day_23_Entity_Relationships_Fetch_Strategies/Day_23_Entity_Relationships_Fetch_Strategies.md) | [All 60 Days Overview](../../README.md) | [Day 25: Database Migrations (Flyway) & Docker](../Day_25_Database_Migrations_Docker/Day_25_Database_Migrations_Docker.md) |

---

## Friendly Welcome: All-or-Nothing Safety for Your Data

Hey there, friend! Welcome to Day 24.

Imagine you are buying a coffee with your debit card:
- Step 1: The bank takes $5 out of your checking account.
- Step 2: The coffee shop's register prints your receipt and marks the coffee as paid.

Now imagine the power goes out right between Step 1 and Step 2! You lost $5, but the barista says: *"Sorry, our register never got the payment. No coffee for you."* You would be rightfully furious!

In database engineering, we solve this nightmare with a **Transaction**: An all-or-nothing guarantee that either EVERY step succeeds, or if anything goes wrong, EVERYTHING is rolled back as if nothing ever happened.

Today, we'll master Spring's `@Transactional` annotation, prevent race conditions when thousands of users hit your AI models simultaneously using `@Version` (Optimistic Locking), and set up automated auditing so you can always see who created or edited prompt templates and when!

---

> 💡 **New Word Alert! Key Concepts for Today**
>
> - **Transaction (`@Transactional`)**: A protective safety envelope around database operations. All operations inside must either succeed together (Commit), or if an error happens, all changes are wiped out (Rollback).
> - **ACID**: The four golden promises of reliable databases:
>   - **Atomicity**: All-or-nothing. No partial updates!
>   - **Consistency**: Database rules and constraints are never violated.
>   - **Isolation**: Concurrent transactions don't interfere with each other.
>   - **Durability**: Once committed, data is permanently saved on disk.
> - **Race Condition**: A bug where two threads try to update the exact same database row at the exact same millisecond, accidentally overwriting each other's changes.
> - **Optimistic Locking (`@Version`)**: Like collaborating in Google Docs. Every entity has a version number (1, 2, 3...). If two users try to save changes to version 1 at the same time, the second user gets a gentle conflict alert instead of secretly wiping out the first user's work.
> - **JPA Auditing**: An automated feature in Spring Data that automatically timestamps `@CreatedDate`, `@LastModifiedDate`, and records `@CreatedBy` on your entities without manual coding.

---

## Table of Contents

1. [Why This Day Matters for a 3-Year Enterprise Gen AI Engineer](#1-why-this-day-matters-for-a-3-year-enterprise-gen-ai-engineer)
2. [Real-World Analogy: The Bank Vault & Certified Notary](#2-real-world-analogy-the-bank-vault--certified-notary)
3. [ACID Guarantees in Enterprise AI Platforms](#3-acid-guarantees-in-enterprise-ai-platforms)
4. [Under the Hood: Spring `@Transactional` Mechanics](#4-under-the-hood-spring-transactional-mechanics)
   - [The AOP Proxy Architecture](#the-aop-proxy-architecture)
   - [The Self-Invocation Trap](#the-self-invocation-trap)
   - [Rollback Rules: Checked vs Unchecked Exceptions](#rollback-rules-checked-vs-unchecked-exceptions)
   - [Propagation Behaviors (`REQUIRED` vs `REQUIRES_NEW`)](#propagation-behaviors-required-vs-requires_new)
5. [Isolation Levels & Read Phenomena](#5-isolation-levels--read-phenomena)
6. [Concurrency Control: Optimistic vs Pessimistic Locking](#6-concurrency-control-optimistic-vs-pessimistic-locking)
   - [Optimistic Locking with `@Version`](#optimistic-locking-with-version)
   - [Pessimistic Locking with `@Lock(LockModeType.PESSIMISTIC_WRITE)`](#pessimistic-locking-with-locklockmodetypepessimistic_write)
7. [Automated JPA Auditing: Compliance & Governance](#7-automated-jpa-auditing-compliance--governance)
8. [Hands-On Code Walkthrough](#8-hands-on-code-walkthrough)
9. [Step-by-Step Compilation & Execution](#9-step-by-step-compilation--execution)
10. [Hands-On Exercises (With Complete Solutions)](#10-hands-on-exercises-with-complete-solutions)
11. [Self-Check Quiz](#11-self-check-quiz)
12. [Day 24 Wrap-Up & What's Next](#12-day-24-wrap-up--whats-next)

---

## 1. Why This Day Matters for a 3-Year Enterprise Gen AI Engineer

When you connect Spring Boot to PostgreSQL in a high-concurrency Generative AI environment, multiple asynchronous threads compete for the same database records:

### The Real-World Disaster Scenarios
1. **The Token Quota Overdraft (Race Condition)**: An enterprise customer has a pre-paid monthly quota of 100,000 tokens. Two users from the organization send complex prompts simultaneously.
   - Virtual Thread 1 reads the balance: 100,000.
   - Virtual Thread 2 reads the balance: 100,000.
   - Thread 1 deducts 60,000 tokens and writes: `balance = 40,000`.
   - Thread 2 deducts 70,000 tokens and writes: `balance = 30,000`.
   - **The Disaster**: The customer consumed 130,000 tokens, but your database recorded a remaining balance of 30,000 tokens! Your company just lost money on GPU compute costs due to a **Lost Update** anomaly.
2. **Partial State Corruption (Atomicity Failure)**: Your workflow consists of:
   - Step A: Deduct 1,500 tokens from the tenant ledger.
   - Step B: Call OpenAI inference API.
   - Step C: Insert the assistant message into `chat_messages`.
   - If Step B throws a `504 Gateway Timeout` and you don't manage transactions properly, Step A stays committed while Step C never happens. The user was billed for a message they never received!
3. **Prompt Template Collision**: Two prompt engineers edit an active system prompt simultaneously. The second engineer saves their changes 5 milliseconds after the first, silently wiping out the first engineer's modifications.
4. **Compliance Audit Failures (SOC2 / HIPAA)**: Security auditors demand proof of who changed an AI system prompt, what model parameters were altered, and the exact UTC timestamp.

---

## 2. Real-World Analogy: The Bank Vault & Certified Notary

```
TRADITIONAL DANGEROUS ACCESS (NO TRANSACTION):
[ Clerk hands customer $500 cash ] ──► System crashes before recording debit!
Outcome: Bank lost $500, accounts desynchronized.

ACID TRANSACTION (ALL-OR-NOTHING NOTARIZED ENVELOPE):
[ Certified Notary seals transaction ]
  ├── 1. Verify cash in vault
  ├── 2. Count $500 to customer
  └── 3. Debit account ledger
  If the pen runs out of ink at Step 2:
  Notary tears up the contract and returns cash to vault! (ATOMIC ROLLBACK)

OPTIMISTIC LOCKING (GOOGLE DOCS VERSIONING):
[ Alice and Bob open Version 4 of document ]
  ├── Alice saves changes ──► Document becomes Version 5!
  └── Bob tries to save changes to Version 4:
      System alerts Bob: "Conflict! Document was modified by Alice. Please reload."
```

Transactions provide a mathematical guarantee that a multi-step workflow either **completes in its entirety** or **leaves no trace whatsoever**.

---

## 🧭 The Mid-Level Java Developer Bridge: How `@Transactional` Actually Works

Almost every Java developer puts `@Transactional` on their services, but subtle misunderstandings cause silent data corruption:

| `@Transactional` Concept | What Junior/Mid Developers Think | What Spring Actually Does | Plain English Rule |
| :--- | :--- | :--- | :--- |
| **How It Starts** | "The database handles it magically." | Spring creates an AOP proxy that calls `conn.setAutoCommit(false)`. | An invisible wrapper starts the transaction before your code runs and calls `commit()` after it returns. |
| **When It Rolls Back** | "It rolls back on ANY exception!" | **False!** By default, Spring rolls back ONLY on `RuntimeException` and `Error`. Checked exceptions (`IOException`, `Exception`) are **COMMITTED** unless you specify `rollbackFor = Exception.class`! | Always write `@Transactional(rollbackFor = Exception.class)`! |
| **The `try-catch` Trap** | Swallowing the error with `catch (Exception e) { log.error(e); }`. | Because you hid the exception from Spring, Spring assumes everything succeeded and **commits partial data**! | If you catch an exception, either rethrow it or call `TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()`. |
| **The Self-Invocation Trap** | Method `public void a()` calls `this.b()` (where `b` has `@Transactional`). | **Bypasses the proxy!** Method `b()` runs with **NO transaction** at all! | Put transactional methods in a separate `@Service` bean so Spring's proxy can intercept the call. |
| **Optimistic Locking (`@Version`)** | Needing heavy database table locks (`SELECT FOR UPDATE`). | Adds an integer `@Version` field to the entity. Automatically checks version on update. | Like Google Docs: if someone else edited the document while you were typing, Spring alerts you instead of overwriting their work! |

---

## 3. ACID Guarantees in Enterprise AI Platforms

| Property | Definition | Gen AI Platform Application |
| :--- | :--- | :--- |
| **Atomicity** | All operations in the transaction succeed, or all are rolled back. | If token deduction succeeds but LLM response persistence fails, the token deduction is automatically refunded. |
| **Consistency** | The database transitions from one valid state to another, satisfying all constraints. | Token balances can never violate `CHECK (remaining_tokens >= 0)`. Foreign keys cannot point to nonexistent sessions. |
| **Isolation** | Concurrent transactions cannot see each other's uncommitted intermediate modifications. | Thread A deducting tokens cannot be read by Thread B until Thread A commits. |
| **Durability** | Once committed, changes are permanent and survive system crashes. | Completed chat messages are safely flushed to PostgreSQL's Write-Ahead Log (WAL) on disk. |

---

## 4. Under the Hood: Spring `@Transactional` Mechanics

Spring does not alter the bytecode of your database driver. Instead, it uses **Spring AOP (Aspect-Oriented Programming)** to intercept method calls:

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
        Target-->>Proxy: throws GenAIException
        Proxy->>TM: rollback()
        TM->>DB: ROLLBACK;
        Proxy-->>Client: 500 / 422 Error (Database clean!)
    end
```

### The Self-Invocation Trap
Because Spring's transaction management relies on dynamic proxies:

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

**The Fix**: Call the transactional method from a separate injected `@Service` bean, or inject `ApplicationContext` to obtain the proxied bean instance.

---

### Rollback Rules: Checked vs Unchecked Exceptions

> **CRITICAL SPRING RULE**: By default, Spring `@Transactional` only triggers rollback for **Unchecked Exceptions** (`RuntimeException` and `Error`). It **COMMITS** if a checked exception (`java.lang.Exception`, `java.io.IOException`) is thrown!

```java
// ❌ DANGEROUS: If IOException occurs, tokens remain deducted!
@Transactional
public void processBatch() throws IOException { ... }

// ✅ SENIOR PATTERN: Explicitly rollback on all exceptions
@Transactional(rollbackFor = Exception.class)
public void processBatch() throws Exception { ... }
```

---

### Propagation Behaviors (`REQUIRED` vs `REQUIRES_NEW`)

- **`Propagation.REQUIRED` (Default)**: If an active transaction exists, join it. If none exists, create a new one.
- **`Propagation.REQUIRES_NEW`**: Always suspend any existing transaction and start a brand-new independent transaction.
  - **Gen AI Use Case**: Writing an audit log or security failure record. Even if the main AI request fails and rolls back, you **still want the audit log record committed**!

```java
@Service
public class AuditService {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSecurityViolation(String userId, String reason) {
        // Commits independently of whether the caller rolls back!
        auditRepository.save(new SecurityAudit(userId, reason));
    }
}
```

---

## 5. Isolation Levels & Read Phenomena

PostgreSQL uses **MVCC (Multi-Version Concurrency Control)**.

| Isolation Level | Dirty Read | Non-Repeatable Read | Phantom Read |
| :--- | :---: | :---: | :---: |
| `READ_UNCOMMITTED` | ❌ Allowed | ❌ Allowed | ❌ Allowed |
| `READ_COMMITTED` *(Postgres Default)* | ✅ Prevented | ❌ Allowed | ❌ Allowed |
| `REPEATABLE_READ` | ✅ Prevented | ✅ Prevented | ✅ Prevented *(In Postgres MVCC)* |
| `SERIALIZABLE` | ✅ Prevented | ✅ Prevented | ✅ Prevented |

- **Dirty Read**: Transaction A reads row modifications made by Transaction B before B has committed.
- **Non-Repeatable Read**: Transaction A reads a row, Transaction B updates that row and commits, Transaction A reads the row again and observes different values.
- **Phantom Read**: Transaction A queries rows matching a condition (`WHERE balance > 1000`), Transaction B inserts a new matching row and commits, Transaction A re-executes query and sees a new "phantom" row.

In 99% of enterprise AI applications, PostgreSQL's default `READ_COMMITTED` with **Optimistic Locking** is the optimal balance of throughput and safety.

---

## 6. Concurrency Control: Optimistic vs Pessimistic Locking

### Optimistic Locking with `@Version`

Optimistic locking assumes conflicts are rare. It validates that no other transaction modified the record while it was being processed:

```java
@Entity
@Table(name = "token_quotas")
public class TokenQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int remainingTokens;

    @Version // 👈 Hibernate automatically increments this on every UPDATE
    private Long version;

    // ...
}
```

#### What Hibernate Generates Under the Hood
When Thread 1 and Thread 2 both read `version = 1`:
1. Thread 1 commits:
   ```sql
   UPDATE token_quotas SET remaining_tokens = 85000, version = 2
   WHERE id = 1 AND version = 1;
   ```
   PostgreSQL returns: `1 row updated`. Success!
2. Thread 2 attempts to commit:
   ```sql
   UPDATE token_quotas SET remaining_tokens = 70000, version = 2
   WHERE id = 1 AND version = 1;
   ```
   PostgreSQL returns: `0 rows updated` (because `version` is now 2!).
3. Hibernate observes that 0 rows were updated and immediately throws:
   `org.hibernate.OptimisticLockException`

Your application catches this exception and can cleanly retry the operation using fresh data.

---

### Pessimistic Locking with `@Lock(LockModeType.PESSIMISTIC_WRITE)`

When contention is extremely high (e.g. 100 parallel Virtual Threads executing prompt tasks against the same customer account), optimistic lock retries waste CPU. Use **Pessimistic Locking**:

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
PostgreSQL locks the row at the database level. Any other transaction attempting to read or update this row **blocks and waits** until the first transaction commits.

---

## 7. Automated JPA Auditing: Compliance & Governance

Enterprise platforms require automated tracking of who created or modified records and when.

### Step 1: Enable Auditing in Configuration
```java
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
            .map(Authentication::getName)
            .or(() -> Optional.of("system"));
    }
}
```

### Step 2: Annotate Entity or Base Class
```java
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

Whenever an entity inheriting `AuditableEntity` is persisted or modified, Hibernate automatically populates the timestamps and current authenticated username!

---

## 8. Hands-On Code Walkthrough

In this day's companion code (`Phase_04_Spring_Data_JPA_Database/Day_24_Transactions_Concurrency_Auditing/code/`), we built:

1. **`TokenQuotaEntity.java`**: Domain model featuring `@Version` optimistic locking, atomic token deduction, and audit metadata.
2. **`TransactionManagerSimulator.java`**: Simulates Spring's `PlatformTransactionManager`, AOP proxy interception, transactional rollback snapshots, and optimistic lock collision detection.
3. **`ConcurrencyDemo.java`**: Executable test harness verifying:
   - Scenario 1: Clean transactional commit.
   - Scenario 2: Automatic rollback on `RuntimeException` (atomicity verified).
   - Scenario 3: Optimistic locking collision between concurrent Virtual Threads.

---

## 9. Step-by-Step Compilation & Execution

```powershell
# 1. Navigate to course root
cd "c:\Users\sriva\OneDrive\Desktop\GEN AI COURSE\JAVA"

# 2. Compile Day 24 code
javac Phase_04_Spring_Data_JPA_Database/Day_24_Transactions_Concurrency_Auditing/code/*.java

# 3. Execute ConcurrencyDemo
java -cp Phase_04_Spring_Data_JPA_Database/Day_24_Transactions_Concurrency_Auditing code.ConcurrencyDemo
```

### Verified Output

```
================================================================================
 DAY 24: TRANSACTIONS, CONCURRENCY & AUDITING (@Transactional, @Version)       
================================================================================

--- SCENARIO 1: Successful @Transactional Execution ---
  [TX BEGIN] Starting transaction: deduct-tokens-prompt-1
 Loaded quota: TokenQuota[tenant='org_acme_corp', balance=100000, version=1, modifiedBy='system']
  [SQL UPDATE] UPDATE token_quotas SET balance = 85000, version = 2 WHERE id = 1 AND version = 1
  [TX COMMIT] Successfully committed transaction: deduct-tokens-prompt-1
 Verified balance after commit: TokenQuota[tenant='org_acme_corp', balance=85000, version=2, modifiedBy='user_alice']

--- SCENARIO 2: Transaction Rollback on RuntimeException ---
  [TX BEGIN] Starting transaction: failing-ai-pipeline
  [SQL UPDATE] UPDATE token_quotas SET balance = 65000, version = 3 WHERE id = 1 AND version = 2
 In-flight deducted balance: 65000
  [TX ROLLBACK] Exception caught: Upstream OpenAI returned HTTP 500. Aborting transaction!. Rolling back transaction: failing-ai-pipeline
 Handled caught exception: Upstream OpenAI returned HTTP 500. Aborting transaction!
 Verified balance after rollback (should remain 85000): 85000

--- SCENARIO 3: Optimistic Locking Collision Detection ---
 Simulating two concurrent Virtual Threads updating the same quota:
 [Thread 1] Read quota: version = 2, balance = 85000
 [Thread 2] Read quota: version = 2, balance = 85000

 [Thread 1] Deducting 5000 tokens and committing...
  [SQL UPDATE] UPDATE token_quotas SET balance = 80000, version = 3 WHERE id = 1 AND version = 2

 [Thread 2] Deducting 10000 tokens and attempting to commit with stale version 2...
 [COLLISION DETECTED] OptimisticLockException caught successfully!
 Detail: Row was updated or deleted by another transaction (expected version: 2, actual database version: 3)
 Resolution: Application will reload fresh entity and retry safely.

 Final Quota State in Database: TokenQuota[tenant='org_acme_corp', balance=80000, version=3, modifiedBy='virtual_thread_1']

================================================================================
 DAY 24 DEMONSTRATION COMPLETE: TRANSACTIONS & LOCKING FULLY VERIFIED!          
================================================================================
```

---

## 10. Hands-On Exercises (With Complete Solutions)

### Exercise 1: Resilient Token Deduction with Spring Retry
**Task**: When an `OptimisticLockException` occurs, the operation should automatically reload the fresh balance and retry up to 3 times before failing. Implement this using Spring's `@Retryable`.

#### Solution:
```java
@Service
public class TokenQuotaService {

    private final TokenQuotaRepository repository;

    public TokenQuotaService(TokenQuotaRepository repository) {
        this.repository = repository;
    }

    @Retryable(
        retryFor = { ObjectOptimisticLockingFailureException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 50, multiplier = 2.0)
    )
    @Transactional(rollbackFor = Exception.class)
    public void deductQuota(String tenantId, int tokensToDeduct, String modifiedBy) {
        TokenQuota quota = repository.findByTenantId(tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Tenant not found: " + tenantId));

        quota.deductTokens(tokensToDeduct, modifiedBy);
        // Automatic dirty checking updates with version check!
    }

    @Recover
    public void recover(ObjectOptimisticLockingFailureException ex, String tenantId, int tokens, String user) {
        log.error("Failed to deduct {} tokens for tenant {} after 3 retries due to high concurrency contention", tokens, tenantId);
        throw new ConcurrencyConflictException("Token reservation failed due to concurrent activity. Please retry.");
    }
}
```

---

### Exercise 2: AuditorAware with JWT Bearer Token Context
**Task**: Implement `AuditorAware<String>` in a Spring Boot microservice where user identities are stored in Spring Security's `JwtAuthenticationToken`.

#### Solution:
```java
@Component
public class SecurityAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.of("system_batch");
        }

        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            // Extract preferred_username or email from JWT claims
            String username = jwtAuth.getToken().getClaimAsString("preferred_username");
            return Optional.ofNullable(username != null ? username : jwtAuth.getName());
        }

        return Optional.of(auth.getName());
    }
}
```

---

### Exercise 3: Pessimistic Locking Repository for Token Buckets
**Task**: Implement a repository query that locks a token bucket row using `SELECT ... FOR UPDATE` with a 3-second lock timeout in PostgreSQL.

#### Solution:
```java
@Repository
public interface TokenBucketRepository extends JpaRepository<TokenBucket, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
        @QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000") // 3000ms timeout
    })
    @Query("SELECT b FROM TokenBucket b WHERE b.tenantId = :tenantId")
    Optional<TokenBucket> findByTenantIdForUpdate(@Param("tenantId") String tenantId);
}
```

---

## 11. Self-Check Quiz

### Q1: What happens if a checked exception (e.g. `java.io.IOException`) is thrown from a `@Transactional` method?
> **Answer**: By default, Spring `@Transactional` **only rolls back on unchecked exceptions** (`RuntimeException` and `Error`). If a checked exception is thrown, Spring will **COMMIT** the transaction! To ensure rollback on all exceptions, you must explicitly declare `@Transactional(rollbackFor = Exception.class)`.

### Q2: Why does calling a `@Transactional` method from another method inside the SAME service class fail to start a transaction?
> **Answer**: Spring's transaction management is implemented via **AOP proxies**. When an external client calls a method on an injected `@Service` bean, the call passes through the proxy interceptor which initiates the transaction. When a method calls another method within the same instance (`this.method()`), it invokes the target object directly, completely bypassing the Spring proxy and transaction interceptor.

### Q3: How does Optimistic Locking with `@Version` prevent lost updates?
> **Answer**: The entity includes an integer or timestamp field annotated with `@Version`. When generating the SQL `UPDATE` statement, Hibernate adds `WHERE id = ? AND version = ?` to the query. If another concurrent transaction modified the row in the meantime, the version in the database is higher, causing the update to match 0 rows. Hibernate detects that 0 rows were updated and throws an `OptimisticLockException`.

### Q4: When should you use Pessimistic Locking instead of Optimistic Locking?
> **Answer**: Use Pessimistic Locking (`PESSIMISTIC_WRITE`) when write contention on the same database record is extremely high (e.g. dozens of concurrent threads updating the same account balance or token quota every second). With optimistic locking, high contention causes almost all threads to collide and fail, wasting CPU on retry loops. Pessimistic locking queues threads at the database level using `SELECT ... FOR UPDATE`.

### Q5: What is the purpose of `Propagation.REQUIRES_NEW`?
> **Answer**: `Propagation.REQUIRES_NEW` suspends any currently active transaction and begins an independent, brand-new transaction. Even if the outer calling transaction ultimately throws an exception and rolls back, the inner `REQUIRES_NEW` transaction remains committed. This is essential for audit logging, token consumption ledgers, and security tracking.

---

## 12. Day 24 Wrap-Up & What's Next

You've just learned how enterprise banking, healthcare, and AI systems keep their data pristine under immense concurrent traffic!

Key takeaways for your toolkit:
- **`@Transactional(rollbackFor = Exception.class)`**: Protects your multi-step operations so you never have partial commits or orphaned billing records.
- **Beware the Self-Invocation Trap**: Calling a `@Transactional` method from inside the same class bypasses Spring's proxy—keep transactional methods in injected beans!
- **`@Version` for Optimistic Locking**: Protects against lost updates when multiple users edit prompt templates or deduct token balances concurrently.
- **JPA Auditing**: Automatically keeps track of who created or modified entities and when.

### What's Coming Up Next?
So far, Hibernate has been automatically creating and updating our database tables using `spring.jpa.hibernate.ddl-auto=update`. In local development, that feels convenient. 

**But in production, doing that is completely forbidden!** If Hibernate renames or drops a table in a live banking or AI database with millions of user records, it's game over.

Tomorrow in **[Day 25: Database Migrations (Flyway) & Docker](../Day_25_Database_Migrations_Docker/Day_25_Database_Migrations_Docker.md)**, we'll learn **Flyway**: how real engineering teams write versioned, automated SQL migration scripts (like Git for your database!) and run PostgreSQL seamlessly with Docker. Get ready for an awesome session!

