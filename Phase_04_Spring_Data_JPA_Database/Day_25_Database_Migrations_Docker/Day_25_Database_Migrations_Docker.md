# Day 25: Database Migrations (Flyway) & Docker

> **"Allowing Hibernate to modify your production database schema with `ddl-auto=update` is like letting a robot painter repaint your living room in the dark: it might look acceptable at first glance, but eventually it destroys your furniture. In production, database schemas are version-controlled, immutable code managed via Flyway."**

---

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [Day 24: Transactions, Concurrency & Auditing](../Day_24_Transactions_Concurrency_Auditing/Day_24_Transactions_Concurrency_Auditing.md) | [All 60 Days Overview](../../README.md) | [Day 26: PostgreSQL pgvector — Your Vector Database](../Day_26_PostgreSQL_pgvector_Vector_Database/Day_26_PostgreSQL_pgvector_Vector_Database.md) |

---

## Friendly Welcome: Git for Your Database & Painless Docker

Hey there, friend! Welcome to Day 25.

Have you ever wondered how engineering teams at Netflix, Spotify, or OpenAI upgrade their database schemas across hundreds of servers without losing customer data or causing downtime?

Up until now, we've relied on Hibernate's `ddl-auto=update` setting. While that's convenient for quick afternoon experiments, in a real company, **doing that in production is strictly forbidden**. If Hibernate automatically drops or alters a column in a production table holding 10 million chat records, you can't hit "Undo"!

Today, we are going to learn the professional way to manage databases:
1. **Flyway**: An automated database migration tool that acts like **Git commits for your database**. Every table creation or column addition is written as a numbered, versioned SQL script (`V1__init.sql`, `V2__add_users.sql`).
2. **Docker & Docker Compose**: Instead of spending two days struggling to install PostgreSQL and compile the `pgvector` AI extension natively on your laptop, Docker spins up a complete, isolated database container in 30 seconds with one simple command!

---

> 💡 **New Word Alert! Key Concepts for Today**
>
> - **Database Migration**: A step-by-step, versioned SQL script that safely evolves your database schema from one version to the next.
> - **Flyway**: An open-source database migration tool. When your Spring Boot app starts, Flyway checks a table called `flyway_schema_history`, sees which SQL files haven't run yet, and applies them one by one in exact numerical order.
> - **Checksum (SHA-256)**: A digital fingerprint of each migration file. If a developer secretly edits an old migration script after it has already run, Flyway detects that the fingerprint changed and halts application startup to prevent corrupting your environments!
> - **Docker Container**: An isolated, lightweight runtime environment that packages an application (like PostgreSQL or an AI model) and all its dependencies. It runs identically on your laptop, a teammate's MacBook, and AWS cloud servers.
> - **Docker Compose (`docker-compose.yml`)**: A single configuration file that lets you define and launch multiple services (like PostgreSQL + pgvector + Ollama) all at once with `docker compose up -d`.

---

## Table of Contents

1. [Why This Day Matters for a 3-Year Enterprise Gen AI Engineer](#1-why-this-day-matters-for-a-3-year-enterprise-gen-ai-engineer)
2. [Real-World Analogy: Git Version Control for Relational Databases](#2-real-world-analogy-git-version-control-for-relational-databases)
3. [The Dangers of `ddl-auto=update` in Production](#3-the-dangers-of-ddl-autoupdate-in-production)
4. [Flyway Architecture & Naming Conventions](#4-flyway-architecture--naming-conventions)
   - [Versioned Migrations (`V`)](#versioned-migrations-v)
   - [Repeatable Migrations (`R`)](#repeatable-migrations-r)
   - [The `flyway_schema_history` Table](#the-flyway_schema_history-table)
   - [Checksums & Immutability Rules](#checksums--immutability-rules)
5. [Designing Real Gen AI Schema Migrations with `pgvector`](#5-designing-real-gen-ai-schema-migrations-with-pgvector)
6. [Containerizing the AI Platform: Docker & Multi-Stage Builds](#6-containerizing-the-ai-platform-docker--multi-stage-builds)
7. [Enterprise `docker-compose.yml`: PostgreSQL + pgvector + Ollama](#7-enterprise-docker-composeyml-postgresql--pgvector--ollama)
8. [Hands-On Code Walkthrough](#8-hands-on-code-walkthrough)
9. [Step-by-Step Compilation & Execution](#9-step-by-step-compilation--execution)
10. [Hands-On Exercises (With Complete Solutions)](#10-hands-on-exercises-with-complete-solutions)
11. [Self-Check Quiz](#11-self-check-quiz)
12. [Day 25 Wrap-Up & What's Next](#12-day-25-wrap-up--whats-next)

---

## 1. Why This Day Matters for a 3-Year Enterprise Gen AI Engineer

When building enterprise AI backends, your database requirements go far beyond standard tables:
- You must enable specialized PostgreSQL extensions like **`vector` (pgvector)**.
- You must build high-dimensional **HNSW (Hierarchical Navigable Small World)** vector indexes.
- You must create `JSONB` columns with GIN indexes for unstructured document metadata.
- You must coordinate database upgrades across multiple Kubernetes pods without causing race conditions or downtime.

### The Production Traps
1. **Hibernate Cannot Create Extensions or HNSW Indexes**: Hibernate's `@Table` and `@Column` annotations have no concept of `CREATE EXTENSION IF NOT EXISTS vector;` or `USING hnsw (embedding vector_cosine_ops)`. You **must** write native SQL DDL.
2. **The Multi-Pod Startup Race Condition**: When you deploy a new version to Kubernetes with 10 replicas, all 10 pods boot simultaneously. If each pod attempts to alter tables, they lock each other out or corrupt the database catalog. Flyway uses a **distributed table lock** on `flyway_schema_history` so exactly one pod executes the migration while the other 9 wait safely.
3. **Local Development Friction**: Requiring new team members to install PostgreSQL, build `pgvector` from C source code, and install Ollama locally takes 2 days of onboarding. With **Docker Compose**, running `docker-compose up -d` brings up the entire infrastructure in 30 seconds!

---

## 2. Real-World Analogy: Git Version Control for Relational Databases

```
GIT CODE REPOSITORY:                         FLYWAY SCHEMA MIGRATIONS:
Commit 1: git commit -m "init app"           V1__init_conversations.sql
Commit 2: git commit -m "add auth"           V2__add_users_and_tokens.sql
Commit 3: git commit -m "add vector"         V3__enable_pgvector_and_chunks.sql
                │                                            │
                ▼                                            ▼
Commit SHA: 7a8b9c...                        Checksum: 125373262 (SHA-256)
If you rewrite history with git rebase:      If you edit an old migration file:
Teammates' branches break!                   Flyway halts startup: CHECKSUM MISMATCH!
```

Flyway brings the discipline of **Git commits** to relational database schemas:
- Every schema change is recorded as an immutable, timestamped SQL script.
- The database knows its exact version number.
- Every developer, CI test runner, staging server, and production cluster runs through the identical sequence of SQL scripts.

---

## 3. The Dangers of `ddl-auto=update` in Production

In beginner tutorials, Spring Boot sets:
```properties
# ❌ NEVER USE IN PRODUCTION!
spring.jpa.hibernate.ddl-auto=update
```

### Why Senior Engineers Ban `ddl-auto=update`:
1. **It Never Drops Columns**: If you rename a field from `modelName` to `modelFamily`, Hibernate does not rename the column. It creates a brand-new column `model_family` and leaves `model_name` full of stale data.
2. **No Rollback Capability**: If an update fails midway, there is no migration history or rollback script.
3. **Destructive on Refactoring**: A subtle typo in an entity class can alter column types or drop foreign key constraints silently.
4. **No Version Tracking**: You cannot tell if your staging database matches your production database schema.

### The Production Standard:
```properties
# Disable Hibernate schema manipulation
spring.jpa.hibernate.ddl-auto=validate

# Enable Flyway
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration
```

---

## 4. Flyway Architecture & Naming Conventions

Flyway scans the `src/main/resources/db/migration/` directory for SQL scripts matching strict naming rules:

```
    V1_2__add_vector_embeddings.sql
    │ ──┘ └───────────────────────┘
  Type Version      Description
```

### 1. Versioned Migrations (`V`)
- Prefix: **`V`**
- Version: Numbers separated by dots or underscores (`V1__`, `V1_1__`, `V2026_09_09__`).
- Separator: **Two underscores `__`** (Single underscore causes syntax errors!).
- Description: Words separated by underscores (e.g. `init_chat_schema`).
- Extension: `.sql`.
- **Rule**: Executed **exactly once** in strict version order. **Immutable forever**.

### 2. Repeatable Migrations (`R`)
- Prefix: **`R__`**
- No version number (e.g. `R__recreate_prompt_stats_view.sql`).
- Re-executed **whenever their SHA-256 checksum changes**.
- Ideal for database views, stored procedures, and triggers.

---

### The `flyway_schema_history` Table

On its first run, Flyway automatically creates a metadata table in PostgreSQL:

| installed_rank | version | description | type | script | checksum | installed_by | installed_on | success |
| :---: | :---: | :--- | :---: | :--- | :---: | :--- | :--- | :---: |
| 1 | 1 | init_conversations | SQL | V1__init_conversations.sql | 125373262 | postgres | 2026-09-09 14:00 | true |
| 2 | 2 | enable_pgvector | SQL | V2__enable_pgvector.sql | 139557369 | postgres | 2026-09-09 14:01 | true |

---

### Checksums & Immutability Rules

Flyway calculates a **SHA-256 checksum** for each migration file when it is applied.
- If a developer later edits `V1__init_conversations.sql` to change `VARCHAR(64)` to `VARCHAR(128)`, the new file's checksum will not match the checksum stored in `flyway_schema_history`.
- On application boot, Flyway detects the mismatch and **immediately aborts startup with an exception**:
  `FlywayValidateException: Migration checksum mismatch for migration version 1`
- **The Golden Rule**: Never edit an already applied migration script. Always create a **new version** (`V3__increase_user_id_length.sql`).

---

## 5. Designing Real Gen AI Schema Migrations with `pgvector`

Here is what production Flyway scripts look like in an enterprise AI platform:

### `src/main/resources/db/migration/V1__init_chat_schema.sql`
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

### `src/main/resources/db/migration/V2__enable_pgvector_and_chunks.sql`
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

## 6. Containerizing the AI Platform: Docker & Multi-Stage Builds

A production Docker container must be:
1. **Lightweight**: Exclude Maven, source code, and build tools from the final image.
2. **Secure**: Run as an unprivileged user, not `root`.
3. **JVM Optimized**: Tuned for container memory limits and Java 21 Virtual Threads.

### Production Multi-Stage `Dockerfile`

```dockerfile
# ==============================================================================
# STAGE 1: Build Application with Maven & Adoptium JDK 21
# ==============================================================================
FROM eclipse-adoptium:21-jdk-alpine AS builder
WORKDIR /workspace

# Cache dependencies
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN ./mvnw dependency:go-offline -B

# Build executable JAR
COPY src src
RUN ./mvnw clean package -DskipTests

# ==============================================================================
# STAGE 2: Lightweight Distroless / Alpine JRE Runner
# ==============================================================================
FROM eclipse-adoptium:21-jre-alpine
WORKDIR /app

# Security: Create unprivileged user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy JAR from builder stage
COPY --from=builder /workspace/target/*.jar app.jar

# JVM Container Flags: MaxRAMPercentage ensures JVM respects Docker memory limits
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

---

## 7. Enterprise `docker-compose.yml`: PostgreSQL + pgvector + Ollama

To develop, test, and run locally without installing anything on your host machine:

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
  # Ollama: Run open-weight LLMs locally (llama3.2, nomic-embed-text)
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

## 8. Hands-On Code Walkthrough

In this day's companion code (`Phase_04_Spring_Data_JPA_Database/Day_25_Database_Migrations_Docker/code/`), we built:

1. **`FlywayMigrationSimulator.java`**: Simulates Flyway's core migration engine:
   - Manages `flyway_schema_history` table.
   - Calculates SHA-256 script checksums.
   - Applies versioned scripts (`V1`, `V2`).
   - Detects script tampering and throws exceptions if history was altered.
   - Implements distributed migration table locks.
2. **`MigrationDemo.java`**: Driver executing 3 critical scenarios:
   - Scenario 1: Initial cold boot applying `V1` and `V2` (with pgvector).
   - Scenario 2: Idempotent warm reboot (0 migrations applied).
   - Scenario 3: Tamper detection when a developer modifies an old migration file.

---

## 9. Step-by-Step Compilation & Execution

```powershell
# 1. Navigate to course workspace
cd "c:\Users\sriva\OneDrive\Desktop\GEN AI COURSE\JAVA"

# 2. Compile Day 25 code
javac Phase_04_Spring_Data_JPA_Database/Day_25_Database_Migrations_Docker/code/*.java

# 3. Run the migration demo
java -cp Phase_04_Spring_Data_JPA_Database/Day_25_Database_Migrations_Docker code.MigrationDemo
```

### Verified Output

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

## 10. Hands-On Exercises (With Complete Solutions)

### Exercise 1: Writing a Non-Destructive Column Migration Script
**Task**: In production, you need to add a `model_version` column to `prompt_templates`. Write the Flyway SQL script `V4__add_model_version_to_prompts.sql` with a non-null constraint and default value without locking the entire table for hours.

#### Solution:
```sql
-- V4__add_model_version_to_prompts.sql
-- In PostgreSQL 11+, adding a column with DEFAULT value does NOT rewrite the table!
ALTER TABLE prompt_templates 
ADD COLUMN model_version VARCHAR(32) NOT NULL DEFAULT 'v1.0';

-- Add index concurrently (Postgres best practice for zero-downtime)
CREATE INDEX CONCURRENTLY idx_prompts_model_ver ON prompt_templates(model_version);
```

---

### Exercise 2: Docker Compose Healthcheck Coordination
**Task**: Why does `depends_on: [postgres]` fail in production if healthchecks are not used? How does `condition: service_healthy` fix it?

#### Solution:
```yaml
# When using standard depends_on:
# Docker starts the postgres container and IMMEDIATELY starts the Spring Boot app.
# However, PostgreSQL takes 3-5 seconds to initialize its database clusters and accept sockets.
# Spring Boot tries to connect on port 5432, gets "Connection refused", and crashes!

# THE SOLUTION:
services:
  postgres:
    image: pgvector/pgvector:pg16
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ai_user -d genai_db"]
      interval: 3s
      timeout: 3s
      retries: 5

  app:
    depends_on:
      postgres:
        condition: service_healthy # Waits until pg_isready returns 0 before booting Spring!
```

---

### Exercise 3: Baseline Migrations on an Existing Production Database
**Task**: What happens if you introduce Flyway into an existing production system that already has database tables created? How do you prevent Flyway from failing?

#### Solution:
```properties
# If Flyway connects to an existing database without flyway_schema_history,
# it throws an error: "Found non-empty schema(s) without schema history table!".

# Tell Flyway to create the history table and mark existing schema as baseline V1:
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=1
spring.flyway.baseline-description=Existing_Production_Baseline

# Then, your first new Flyway migration script is named:
# V2__first_new_change.sql
```

---

## 11. Self-Check Quiz

### Q1: Why must migration scripts in Flyway be treated as strictly immutable?
> **Answer**: Because Flyway stores the SHA-256 checksum of each executed script in `flyway_schema_history`. If you alter an already executed script, other environments (staging, production, other developers' laptops) will have different checksums, resulting in a `FlywayValidateException` that halts the application.

### Q2: How does Flyway prevent two Kubernetes pods from running migrations simultaneously?
> **Answer**: Flyway creates and acquires an exclusive table lock on the `flyway_schema_history` table in PostgreSQL before reading or executing any migration scripts. The second pod blocks waiting for the lock and finds all migrations already applied when the lock is released.

### Q3: What is the difference between `V` (Versioned) and `R` (Repeatable) migration scripts in Flyway?
> **Answer**: Versioned migrations (`V1__...sql`, `V2__...sql`) are executed in strict numerical sequence exactly once and must never be modified. Repeatable migrations (`R__...sql`) do not have version numbers; they are re-executed whenever their file checksum changes, making them ideal for views, functions, and stored procedures.

### Q4: Why is `pgvector/pgvector:pg16` required as the Docker image instead of standard `postgres:16`?
> **Answer**: Standard PostgreSQL does not include the C-based vector data types, distance operators (`<=>`, `<->`), or HNSW indexing libraries. The `pgvector/pgvector` image contains the pre-compiled `pgvector` extension ready to be activated via `CREATE EXTENSION IF NOT EXISTS vector;`.

### Q5: In a Dockerfile, why should you use `-XX:MaxRAMPercentage=75.0` instead of hardcoding `-Xmx2g`?
> **Answer**: Hardcoding `-Xmx2g` makes the container brittle; if Docker memory limits are adjusted (e.g. from 4GB to 8GB on Kubernetes), the JVM will not adapt. `-XX:MaxRAMPercentage=75.0` instructs the JVM to dynamically calculate its max heap as 75% of the container's cgroup memory limit, leaving 25% for Metaspace, threads, and off-heap memory.

---

## 12. Day 25 Wrap-Up & What's Next

You've just taken a massive step from writing "student code" to writing real **enterprise-grade infrastructure**!

Remember these golden rules:
- **Never use `ddl-auto=update` in production**: Rely on Flyway for deterministic, version-controlled schema evolution.
- **Migrations are immutable**: Once a versioned SQL script (`V1__...`) has run, never edit its contents; create a `V2__...` script for new changes.
- **Docker eliminates "It works on my machine"**: A clean `docker-compose.yml` gives your entire team the exact same PostgreSQL database with `pgvector` pre-installed and ready in seconds.

### What's Coming Up Next?
We have Docker running PostgreSQL. We have Flyway ready to run SQL migrations. 

Now comes the grand finale of Phase 4: **[Day 26: PostgreSQL pgvector — Your Vector Database](../Day_26_PostgreSQL_pgvector_Vector_Database/Day_26_PostgreSQL_pgvector_Vector_Database.md)**!
Tomorrow, you'll learn how to turn PostgreSQL into a high-speed AI Vector Database. You'll store high-dimensional embeddings directly in database rows, search for documents by semantic meaning using cosine distance (`<=>`), and build lightning-fast HNSW vector indexes. You're going to love it!

