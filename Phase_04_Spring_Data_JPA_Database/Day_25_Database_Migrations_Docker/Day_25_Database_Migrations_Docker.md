# Day 25: Database Migrations with Flyway & Docker Infrastructure

[<- Back to Day 24: Transactions & Auditing](../Day_24_Transactions_Concurrency_Auditing/Day_24_Transactions_Concurrency_Auditing.md) | [Course Index](../../README.md) | [Next: Day 26 - PostgreSQL pgvector ->](../Day_26_PostgreSQL_pgvector_Vector_Database/Day_26_PostgreSQL_pgvector_Vector_Database.md)

---

## 1. Topic Overview

Database schema migration tools like Flyway manage version-controlled, repeatable, and automated database changes alongside your application code, while Docker containerizes databases and local AI services (like pgvector and Ollama) into uniform, reproducible runtime environments. In enterprise AI systems, these tools eliminate schema drift across development, staging, and production clusters, ensuring vector tables, metadata indexes, and chat histories deploy deterministically without catastrophic downtime.

---

## 2. Basic Foundations (True Zero)

### What is a Database Migration?
In early development, developers often rely on Hibernate's `spring.jpa.hibernate.ddl-auto=update` to automatically create and alter database tables. However, in enterprise production environments, `ddl-auto=update` is dangerous:
- It can drop columns unexpectedly.
- It cannot reliably rename columns without dropping and recreating them (destroying data).
- It offers zero audit trail of who modified the schema, when, and why.
- Multiple application instances booting simultaneously can execute conflicting DDL statements.

A **database migration tool** treats your database schema as version-controlled code. Every alteration—creating tables, adding columns, modifying foreign keys, or creating vector indexes—is written as an immutable SQL script checked into Git (e.g., `V1__init_schema.sql`, `V2__add_vector_column.sql`).

```
+-----------------------------------------------------------------------------------+
|                            THE ARCHITECT'S ANALOGY                                |
|                                                                                   |
| Imagine building a skyscraper. You cannot simply remodel structural beams on a   |
| whim while tenants are living inside. Instead, architects produce numbered,       |
| stamped blueprints (Revision 1, Revision 2, Revision 3).                          |
|                                                                                   |
| The city inspector keeps a strict ledger recording every stamped revision that has|
| been applied to the building. If a contractor attempts to secretly alter Revision |
| 1 after it was already inspected and built, the inspector flags a violation and   |
| halts construction immediately!                                                   |
|                                                                                   |
| Flyway is the city inspector; its SQL scripts are the stamped blueprints; and     |
| `flyway_schema_history` is the immutable city ledger.                            |
+-----------------------------------------------------------------------------------+
```

### Minimal Beginner-Friendly Working Code Example

Below is a minimal Java simulation of how Flyway computes an immutable checksum, records migrations in a schema history table, and locks during execution.

```java
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

public class BasicFlywayExample {

    // Simulates an entry in the flyway_schema_history table
    record SchemaHistoryEntry(int rank, String version, String script, String checksum, boolean success) {}

    public static void main(String[] args) throws Exception {
        // 1. Immutable ledger representing the database table
        List<SchemaHistoryEntry> schemaHistory = new ArrayList<>();

        // 2. Migration script file content in src/main/resources/db/migration/
        String scriptName = "V1__init_chat_schema.sql";
        String scriptSql = "CREATE TABLE chat_sessions (id BIGSERIAL PRIMARY KEY, user_id VARCHAR(64) NOT NULL);";

        // 3. Compute SHA-256 Checksum
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(scriptSql.getBytes(StandardCharsets.UTF_8));
        String calculatedChecksum = HexFormat.of().formatHex(hash);

        System.out.println("Applying Migration: " + scriptName);
        System.out.println("Calculated Checksum: " + calculatedChecksum);

        // 4. Record execution in the ledger
        schemaHistory.add(new SchemaHistoryEntry(1, "1", scriptName, calculatedChecksum, true));

        System.out.println("Migration Record Inserted into flyway_schema_history:");
        System.out.println("  Rank: " + schemaHistory.get(0).rank());
        System.out.println("  Version: " + schemaHistory.get(0).version());
        System.out.println("  Checksum: " + schemaHistory.get(0).checksum().substring(0, 16) + "...");
    }
}
```

#### Line-by-Line Walkthrough:
- **Lines 7–8**: Defines `SchemaHistoryEntry`, a Java Record mirroring the primary columns of Flyway's `flyway_schema_history` metadata table (`installed_rank`, `version`, `script`, `checksum`, `success`).
- **Line 12**: Creates an in-memory list acting as our database history ledger.
- **Lines 15–16**: Represents a standard Flyway versioned migration file name (`V1__init_chat_schema.sql`) and its raw DDL content.
- **Lines 19–21**: Uses `MessageDigest` to calculate a deterministic SHA-256 hash of the script text. If a single whitespace or character changes later, this hash changes.
- **Line 27**: Inserts a confirmed migration record into `schemaHistory`. On subsequent boots, Flyway reads this row, recalculates the file checksum, and skips re-execution.

---

## 3. Core Concept Walkthrough (Basic → Intermediate)

### Flyway File Naming Conventions

Flyway discovers migration files on the classpath (default: `classpath:db/migration`). Filenames must follow strict formatting rules:

```
    V1__init_chat_schema.sql
    ^ ^  ^
    | |  +--- Description (underscores become spaces: "init chat schema")
    | +------ Double underscore separator (MANDATORY)
    +-------- Type prefix ('V' = Versioned, 'R' = Repeatable, 'U' = Undo, 'B' = Baseline)
```

```
+--------+------------------+-------------------+---------------------------------------------------+
| Prefix | Type             | Example           | Behavior & Use Case                               |
+--------+------------------+-------------------+---------------------------------------------------+
|   V    | Versioned        | V1_1__chunks.sql  | Executed exactly ONCE in strict numerical order.  |
|        |                  |                   | Content is IMMUTABLE once applied.                |
+--------+------------------+-------------------+---------------------------------------------------+
|   R    | Repeatable       | R__rag_views.sql  | Re-executed whenever its SHA-256 checksum changes. |
|        |                  |                   | Ideal for stored procedures, functions, views.    |
+--------+------------------+-------------------+---------------------------------------------------+
|   B    | Baseline         | B1__prod_v1.sql   | Marks an existing production database state.      |
+--------+------------------+-------------------+---------------------------------------------------+
|   U    | Undo (Teams/Pro) | U1__drop_chat.sql | Rollback script to revert the versioned change.   |
+--------+------------------+-------------------+---------------------------------------------------+
```

---

### The `flyway_schema_history` Table

On its first run against an empty database, Flyway automatically creates a metadata table named `flyway_schema_history`:

```
+----------------+---------+-----------------------+------+--------------------------+-----------+--------------+------------------+---------+
| installed_rank | version | description           | type | script                   | checksum  | installed_by | installed_on     | success |
+----------------+---------+-----------------------+------+--------------------------+-----------+--------------+------------------+---------+
| 1              | 1       | init conversations    | SQL  | V1__init_conversations.sql| 125373262 | postgres     | 2026-09-09 14:00 | true    |
| 2              | 2       | enable pgvector       | SQL  | V2__enable_pgvector.sql   | 139557369 | postgres     | 2026-09-09 14:01 | true    |
+----------------+---------+-----------------------+------+--------------------------+-----------+--------------+------------------+---------+
```

When Spring Boot boots:
1. Flyway acquires an exclusive distributed lock on `flyway_schema_history`.
2. It queries all applied migrations from `flyway_schema_history`.
3. It scans `classpath:db/migration` for migration scripts.
4. For all previously applied migrations, it validates that the file checksum matches the stored checksum in the table.
5. If any checksum differs, **startup fails immediately** (`FlywayValidateException`).
6. For any new version scripts greater than the highest applied version, it executes them inside a transaction and inserts a new history row.
7. Flyway releases the lock and hands control over to Hibernate/Spring Data JPA.

---

### Designing Gen AI Migrations with `pgvector`

Here are production-ready Flyway migration scripts used in an enterprise AI platform.

#### Migration 1: `src/main/resources/db/migration/V1__init_chat_schema.sql`
```sql
-- 1. Conversation Sessions
CREATE TABLE conversation_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    title VARCHAR(200) NOT NULL,
    total_tokens_used INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sessions_user_id ON conversation_sessions(user_id);

-- 2. Chat Messages
CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL, -- 'USER', 'ASSISTANT', 'SYSTEM'
    content TEXT NOT NULL,
    token_count INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_messages_session FOREIGN KEY (session_id) 
        REFERENCES conversation_sessions(id) ON DELETE CASCADE
);

CREATE INDEX idx_messages_session_id ON chat_messages(session_id);
```

#### Migration 2: `src/main/resources/db/migration/V2__enable_pgvector_and_chunks.sql`
```sql
-- Enable PostgreSQL vector extension (provided by pgvector)
CREATE EXTENSION IF NOT EXISTS vector;

-- Document chunks for RAG semantic search
CREATE TABLE document_chunks (
    id BIGSERIAL PRIMARY KEY,
    document_id VARCHAR(120) NOT NULL,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    -- 1536 dimensions matches OpenAI text-embedding-3-small
    embedding vector(1536) NOT NULL,
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Build HNSW index for ultra-fast approximate nearest neighbor (ANN) cosine search
CREATE INDEX idx_chunks_embedding_hnsw 
ON document_chunks 
USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

CREATE INDEX idx_chunks_doc_id ON document_chunks(document_id);
```

---

### Containerizing the AI Platform: Multi-Stage Docker Build

A production Docker container for Spring Boot AI services must be lightweight, secure, and tuned for container cgroups:

```dockerfile
# ==============================================================================
# STAGE 1: Build Application with Maven & Adoptium JDK 21
# ==============================================================================
FROM eclipse-adoptium:21-jdk-alpine AS builder
WORKDIR /workspace

# Cache dependencies layer
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN ./mvnw dependency:go-offline -B

# Build executable JAR without running tests during image packaging
COPY src src
RUN ./mvnw clean package -DskipTests

# ==============================================================================
# STAGE 2: Lightweight Distroless / Alpine JRE Runner
# ==============================================================================
FROM eclipse-adoptium:21-jre-alpine
WORKDIR /app

# Security: Create and switch to an unprivileged user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy built JAR from builder stage
COPY --from=builder /workspace/target/*.jar app.jar

# JVM Container Flags: Respect container memory limits and use G1GC
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

---

### Multi-Service Docker Compose: PostgreSQL + pgvector + Ollama

To develop and test full-stack AI workflows locally without host installations:

```yaml
version: '3.8'

services:
  # ----------------------------------------------------------------------------
  # PostgreSQL with pgvector extension pre-installed
  # ----------------------------------------------------------------------------
  postgres:
    image: pgvector/pgvector:pg16
    container_name: genai-postgres
    environment:
      POSTGRES_DB: genai_db
      POSTGRES_USER: ai_user
      POSTGRES_PASSWORD: ai_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ai_user -d genai_db"]
      interval: 5s
      timeout: 5s
      retries: 5
    networks:
      - ai-network

  # ----------------------------------------------------------------------------
  # Ollama: Local Open-Weights LLM & Embedding Server
  # ----------------------------------------------------------------------------
  ollama:
    image: ollama/ollama:latest
    container_name: genai-ollama
    ports:
      - "11434:11434"
    volumes:
      - ollama_data:/root/.ollama
    networks:
      - ai-network

  # ----------------------------------------------------------------------------
  # Spring Boot AI Application
  # ----------------------------------------------------------------------------
  app:
    build: .
    container_name: genai-spring-app
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/genai_db
      SPRING_DATASOURCE_USERNAME: ai_user
      SPRING_DATASOURCE_PASSWORD: ai_password
      SPRING_FLYWAY_ENABLED: "true"
      OLLAMA_BASE_URL: http://ollama:11434
    depends_on:
      postgres:
        condition: service_healthy
    networks:
      - ai-network

volumes:
  postgres_data:
  ollama_data:

networks:
  ai-network:
    driver: bridge
```

---

## 4. Prerequisite & Supporting Concepts

### Prerequisite / Supporting Concept: Distributed Database Locking
When multiple replicas or Kubernetes pods of a Spring Boot microservice boot up simultaneously, they all attempt to run database migrations at the exact same millisecond. Without synchronization, duplicate tables, deadlocks, and corrupted schemas occur.

Flyway solves this by executing database-level table locks (e.g., `LOCK TABLE flyway_schema_history IN ACCESS EXCLUSIVE MODE` in PostgreSQL). The first pod acquires the lock, applies the migrations, and updates the history table. The remaining pods wait for the lock release, check the updated history table, find zero pending migrations, and start up cleanly without error.

### Prerequisite / Supporting Concept: Multi-Stage Docker Builds
In a single-stage Docker build, the final image contains the JDK, Maven build caches, test tools, and source files—easily bloating the image to 800MB–1.2GB and exposing source code if the image is leaked. 

A multi-stage build cleanly separates the build environment from the runtime environment. Stage 1 compiles the application using a heavy JDK. Stage 2 copies only the compiled executable JAR into a lean JRE Alpine image (~150MB). The final image contains zero build tools and zero source files.

### Prerequisite / Supporting Concept: Docker Compose Healthcheck Coordination
A common pitfall with Docker Compose is using standard `depends_on: [postgres]`. Docker considers the container "started" the instant the Linux process initiates. However, PostgreSQL takes 3 to 8 seconds to initialize storage and start accepting TCP connections. Spring Boot attempts to connect immediately, encounters `Connection refused`, and crashes.

Using a healthcheck with `pg_isready -U ai_user -d genai_db` paired with `condition: service_healthy` guarantees that Spring Boot will not attempt to boot until PostgreSQL is actively responding to database queries.

---

## 5. Advanced Depth (Intermediate → Advanced)

### The Checksum Immutability Rule & Tamper Detection

Once a versioned migration script (`V1__...sql`) is executed against any database (including local development), **its text content becomes strictly immutable**.

Flyway calculates a deterministic CRC32 or SHA-256 checksum across the script's lines:
- Changing a column type in an existing file from `VARCHAR(64)` to `VARCHAR(128)` will alter the checksum.
- Adding a single trailing space or comment line will alter the checksum.
- When an application boots, Flyway detects that the stored checksum in `flyway_schema_history` does not match the file on disk, throwing:

```
org.flywaydb.core.api.exception.FlywayValidateException: 
Validate failed: Migrations have failed validation
Migration checksum mismatch for migration version 1
-> Applied to database : 125373262
-> Resolved locally    : -2002032864
```

```
+-----------------------------------------------------------------------------------+
| BAD PRACTICE: Tampering with Applied Migrations                                   |
|                                                                                   |
| // Editing V1__init_schema.sql after it was applied to staging:                  |
| ALTER TABLE chat_messages ADD COLUMN user_feedback VARCHAR(30); -- CRASH ON BOOT! |
+-----------------------------------------------------------------------------------+
| GOOD PRACTICE: Append New Version Script                                          |
|                                                                                   |
| // Leave V1 intact. Create a new file:                                            |
| V3__add_user_feedback_to_chat_messages.sql                                        |
| ALTER TABLE chat_messages ADD COLUMN user_feedback VARCHAR(30);                   |
+-----------------------------------------------------------------------------------+
```

---

### Zero-Downtime Migration Patterns

In 24/7 high-availability AI systems, database migrations cannot lock tables for minutes or drop columns that running application pods still query.

```
                    ZERO-DOWNTIME EXPAND & CONTRACT PATTERN
                    
    Step 1: EXPAND                       Step 2: DUAL WRITE / BACKFILL
    +------------------------------+     +------------------------------+
    | Add new column (nullable or  | --> | App writes to both columns.  |
    | with default). Old app runs. |     | Background task backfills.   |
    +------------------------------+     +------------------------------+
                                                        |
                                                        v
    Step 4: CLEANUP / CONTRACT           Step 3: SWITCH READS
    +------------------------------+     +------------------------------+
    | Migration V3 drops old column| <-- | New app version reads only   |
    | once zero old pods remain.   |     | from the new column.         |
    +------------------------------+     +------------------------------+
```

1. **Non-destructive column additions**: In PostgreSQL 11+, `ALTER TABLE ... ADD COLUMN ... DEFAULT 'val'` is instant because the default value is stored in catalog metadata rather than rewriting every existing row.
2. **Concurrent Indexing**: Standard `CREATE INDEX` locks the entire table against writes. In production with millions of vector embeddings, always use:
   ```sql
   CREATE INDEX CONCURRENTLY idx_chunks_doc_id ON document_chunks(document_id);
   ```
   *(Note: Concurrent indexing requires setting `spring.flyway.mixed=true` or running outside a transactional migration block).*

---

### Hands-On Simulation Code Walkthrough

The companion code repository demonstrates this architecture:
- `FlywayMigrationSimulator.java`: Implements a distributed lock, schema history ledger, checksum validator, and migration executor.
- `MigrationDemo.java`: Tests cold boots, idempotent restarts, and tampered script detection.

```powershell
# Compile Day 25 code
javac Phase_04_Spring_Data_JPA_Database/Day_25_Database_Migrations_Docker/code/*.java

# Run the migration engine simulation
java -cp Phase_04_Spring_Data_JPA_Database/Day_25_Database_Migrations_Docker code.MigrationDemo
```

#### Verified Execution Output:
```
================================================================================
 DAY 25: DATABASE MIGRATIONS (FLYWAY) & DOCKER INFRASTRUCTURE SETUP             
================================================================================

--- SCENARIO 1: First Application Boot (Applying V1 & V2) ---
  [Flyway Lock] Acquired migration table lock on 'flyway_schema_history'
  [Flyway Apply] Migrating to version 1 - init_conversation_sessions ...
  [Flyway Success] Successfully applied version 1 (Checksum: 125373262)
  [Flyway Apply] Migrating to version 2 - enable_pgvector_and_chunks ...
  [Flyway Success] Successfully applied version 2 (Checksum: 139557369)
  [Flyway Lock] Released migration table lock on 'flyway_schema_history'
 Migrations applied: 2

 flyway_schema_history Table State:
   [Rank 1] Version: 1 | Description: init_conversation_sessions   | Checksum: 125373262    | Success: true
   [Rank 2] Version: 2 | Description: enable_pgvector_and_chunks   | Checksum: 139557369    | Success: true

--- SCENARIO 2: Second Application Boot (Idempotent Check) ---
  [Flyway Lock] Acquired migration table lock on 'flyway_schema_history'
  [Flyway Verify] Version 1 already applied with valid checksum 125373262. Skipping.
  [Flyway Verify] Version 2 already applied with valid checksum 139557369. Skipping.
  [Flyway Lock] Released migration table lock on 'flyway_schema_history'
 Migrations applied on reboot: 0 (Expected: 0)

--- SCENARIO 3: Detecting Altered Historical Migration Script ---
 Attempting to boot application with tampered V1 migration script...
  [Flyway Lock] Acquired migration table lock on 'flyway_schema_history'
  [Flyway Lock] Released migration table lock on 'flyway_schema_history'
 [SAFETY GUARD ACTIVATED] Checksum mismatch caught cleanly!
 Message: CRITICAL FLYWAY ERROR: Checksum mismatch for version 1 (init_conversation_sessions)! Previously installed with checksum 125373262, but current script has checksum -2002032864. Migration scripts in git MUST BE IMMUTABLE!

================================================================================
 DAY 25 DEMONSTRATION COMPLETE: FLYWAY & DOCKER ENGINE VERIFIED!                
================================================================================
```

---

## 6. Quick Recap

| Concept | Description | Production Rule / Best Practice |
| :--- | :--- | :--- |
| **`ddl-auto`** | Hibernate automatic schema tool | Always set to `validate` or `none` in production. Never `update`. |
| **Flyway** | Version-controlled SQL migration tool | Place scripts in `src/main/resources/db/migration`. |
| **Versioned (`V`)** | Scripts named `V<Num>__<Description>.sql` | Immutable. Applied once in ascending order. |
| **Repeatable (`R`)** | Scripts named `R__<Description>.sql` | Re-run when checksum changes. Ideal for views and procedures. |
| **Checksums** | SHA-256 hash of script text | Any change to an applied script aborts application startup. |
| **Multi-Stage Docker** | Separate build vs runtime images | Excludes Maven and source files. Runs as unprivileged `appuser`. |
| **Docker Compose** | Multi-container local orchestration | Coordinates PostgreSQL, `pgvector`, and Ollama with healthchecks. |

---

## 7. Self-Check Questions & Practice Exercises

### Conceptual & Architectural Questions

#### Q1: Why must migration scripts in Flyway be treated as strictly immutable?
**Answer**: Flyway records the SHA-256 checksum of every executed script in the `flyway_schema_history` table. If an engineer alters an already applied script, other environments (staging, production, or teammates' local environments) will detect a checksum mismatch during validation. Flyway immediately halts application boot with a `FlywayValidateException` to protect database integrity.

#### Q2: How does Flyway prevent multiple application pods in Kubernetes from running migrations simultaneously?
**Answer**: Flyway obtains an exclusive database-level table lock on the `flyway_schema_history` table before scanning or executing any pending scripts. Secondary instances block waiting for the lock. Once released, the secondary instances inspect the history table, see all new versions recorded as successful, and continue booting without reapplying changes.

#### Q3: What is the difference between Versioned (`V`) and Repeatable (`R`) migrations?
**Answer**: Versioned migrations (`V1__...`, `V2__...`) have explicit ascending version numbers, execute once, and can never be modified. Repeatable migrations (`R__...`) lack version numbers and execute after all versioned scripts have finished, but only when their file checksum has changed. They are primarily used for stateless database definitions such as views, stored functions, and triggers.

#### Q4: Why is `pgvector/pgvector:pg16` required as the Docker image instead of standard `postgres:16`?
**Answer**: Standard PostgreSQL distributions do not compile or ship the C-language extension `pgvector`. Attempting to run `CREATE EXTENSION vector;` on standard Postgres results in a missing shared library error. The `pgvector/pgvector` image contains the compiled binaries, vector data types, index operators (`vector_cosine_ops`), and HNSW algorithms.

#### Q5: In a production Dockerfile, why should you use `-XX:MaxRAMPercentage=75.0` instead of hardcoding `-Xmx2g`?
**Answer**: Hardcoding `-Xmx` values couples the image to a specific hardware configuration. If Kubernetes limits the container to 1GB or scales it to 16GB, a hardcoded `-Xmx2g` will cause Linux OOM (Out-Of-Memory) killer crashes or underutilize available memory. `-XX:MaxRAMPercentage=75.0` dynamically calculates maximum heap size based on container cgroups, leaving 25% for thread stacks, Metaspace, and native JVM memory.

---

### Hands-On Practice Exercises

#### Exercise 1: Non-Destructive Column Migration Script
**Task**: In production, you must add an `embedding_model` column to an existing `document_chunks` table with a non-null constraint and default value `'text-embedding-3-small'`. Write the Flyway script `V3__add_embedding_model.sql`.

```sql
-- Solution: V3__add_embedding_model.sql
-- In PostgreSQL 11+, adding a column with a DEFAULT value is a zero-downtime metadata operation.
ALTER TABLE document_chunks 
ADD COLUMN embedding_model VARCHAR(64) NOT NULL DEFAULT 'text-embedding-3-small';

-- Add index concurrently without locking existing write transactions
CREATE INDEX CONCURRENTLY idx_chunks_model ON document_chunks(embedding_model);
```

#### Exercise 2: Coordinating Services with Docker Compose Healthchecks
**Task**: Explain why standard `depends_on: [postgres]` causes Spring Boot to fail on startup, and write the YAML configuration that guarantees database readiness.

```yaml
# Solution:
services:
  postgres:
    image: pgvector/pgvector:pg16
    environment:
      POSTGRES_DB: genai_db
      POSTGRES_USER: ai_user
      POSTGRES_PASSWORD: ai_password
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ai_user -d genai_db"]
      interval: 3s
      timeout: 3s
      retries: 5

  app:
    build: .
    depends_on:
      postgres:
        condition: service_healthy # Blocks container boot until pg_isready returns code 0!
```

#### Exercise 3: Baselining an Existing Production Database
**Task**: How do you introduce Flyway into an existing production system with hundreds of thousands of live rows where tables were created manually or via legacy scripts?

```properties
# Solution: Configure application.properties
# Prevents Flyway from failing with "Found non-empty schema(s) without schema history table!"
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=1
spring.flyway.baseline-description=Existing_Production_Schema

# Result: Flyway generates flyway_schema_history and records version 1 as completed.
# Your first custom migration script will be named:
# V2__new_feature_migration.sql
```

---

[<- Back to Day 24: Transactions & Auditing](../Day_24_Transactions_Concurrency_Auditing/Day_24_Transactions_Concurrency_Auditing.md) | [Course Index](../../README.md) | [Next: Day 26 - PostgreSQL pgvector ->](../Day_26_PostgreSQL_pgvector_Vector_Database/Day_26_PostgreSQL_pgvector_Vector_Database.md)
