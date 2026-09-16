# 🚀 Java → GenAI Masterclass: Zero to 3-Years-Experienced

> **For developers who want to master Core Java, JVM Internals, Spring Boot, Microservices, and Production GenAI (Spring AI & LangChain4j).**
>
> Every concept explained through **real-world analogies, runnable Java code, ASCII diagrams, Mermaid flowcharts, and memory models.**
> No unexplained annotations. No assumed knowledge. No shortcuts.
>
> Target Bar: **A competent 3-years-experienced Java backend engineer who builds, deploys, and operates production GenAI systems.**

---

## 📋 Course Rules

| Rule | Description |
| :---: | :--- |
| **1** | No concept is introduced without a **real-world analogy FIRST**. |
| **2** | Every concept is shown as **runnable Java code** (no unexplained magic). |
| **3** | Each day builds **ONLY on what previous days taught** — never skip ahead. |
| **4** | **ASCII diagrams, Mermaid flowcharts, and memory perspective maps** for every concept. |
| **5** | Every framework pattern & annotation is explained **WHY it exists**, not just how. |
| **6** | Depth caps strictly at what a **real 3-years-experienced production engineer** needs. |

---

## 🛠️ Prerequisites & Setup

### What You Need

| Tool | Version | Purpose |
| :--- | :--- | :--- |
| **Java JDK** | 21 (LTS) | Core language runtime & Virtual Threads |
| **Maven** | 3.9+ | Build tool & dependency management |
| **IntelliJ IDEA / VS Code** | Latest | Recommended IDE with Java extension pack |
| **Docker Desktop** | Latest | Running PostgreSQL, pgvector, Redis & Ollama |
| **Git** | Latest | Version control & team workflows |
| **Ollama** | Latest | Local open-weight LLMs (100% free & private) |

### Quick Setup

```bash
# 1. Verify Java 21
java --version    # Should show 21.x.x

# 2. Verify Maven
mvn --version     # Should show 3.9+

# 3. Start local infrastructure (PostgreSQL + pgvector + Ollama)
docker-compose up -d

# 4. Pull a local LLM
ollama pull llama3.2
```

---

## 🗺️ 67-Day Roadmap (9 Phases)

### 🟢 Phase_01 — Java Foundations (Days 01–08)
> *From basic syntax to Virtual Threads — building your core programming engine.*

| Day | Topic | Status |
| :---: | :--- | :---: |
| [Day 01](Phase_01_Java_Foundations/Day_01_Java_Ecosystem_and_Setup/Day_01_Java_Ecosystem_and_Setup.md) | Java Ecosystem & Setup — JDK 21, Compilation, Bytecode, Maven | ✅ Complete |
| [Day 02](Phase_01_Java_Foundations/Day_02_OOP_Classes_Objects_Memory/Day_02_OOP_Classes_Objects_Memory.md) | OOP — Classes, Objects & Memory (Stack vs Heap, Scopes, GC Roots) | ✅ Complete |
| [Day 03](Phase_01_Java_Foundations/Day_03_Inheritance_Interfaces_Polymorphism/Day_03_Inheritance_Interfaces_Polymorphism.md) | Inheritance, Interfaces & Polymorphism | 🔄 Next Up |
| [Day 04](Phase_01_Java_Foundations/Day_04_Generics_Collections_DataStructures/Day_04_Generics_Collections_DataStructures.md) | Generics, Collections & Data Structures | 📅 Planned |
| [Day 05](Phase_01_Java_Foundations/Day_05_Modern_Java_Records_Optional_Sealed/Day_05_Modern_Java_Records_Optional_Sealed.md) | Modern Java: Records, Optional & Sealed Types | 📅 Planned |
| [Day 06](Phase_01_Java_Foundations/Day_06_Functional_Programming_Streams/Day_06_Functional_Programming_Streams.md) | Functional Programming & Stream API | 📅 Planned |
| [Day 07](Phase_01_Java_Foundations/Day_07_Concurrency_Virtual_Threads/Day_07_Concurrency_Virtual_Threads.md) | Concurrency & Virtual Threads (Project Loom) | 📅 Planned |
| [Day 08](Phase_01_Java_Foundations/Day_08_IO_HTTP_JSON_Testing/Day_08_IO_HTTP_JSON_Testing.md) | I/O, HTTP Client, JSON & Testing Basics | 📅 Planned |

---

### 🟢 Phase_02 — Advanced Core Java, JVM Internals & Design Foundations (Days 01–09)
> *Write robust, idiomatic Java, master JVM memory and GC, and apply real design patterns.*

| Day | Topic | Memory Angle |
| :---: | :--- | :---: |
| Day 01 | Exception Handling Deep Dive & Stack Traces | Chained root causes & resource leak prevention |
| Day 02 | String Internals & Text Processing | 🧠 JVM Heap & String Constant Pool |
| Day 03 | Enums, Annotations & Reflection Basics | Metaspace & dynamic bytecode inspection |
| Day 04 | JVM Internals: Class Loading & Memory Areas | 🧠 JVM Stack/Heap & Object Lifecycle |
| Day 05 | Garbage Collection, Conceptually | 🧠 JVM Heap & Generational GC (G1/ZGC) |
| Day 06 | SOLID Principles & Clean Code | Refactoring patterns and clean architecture |
| Day 07 | Core Design Patterns I (Creational) | Singleton, Factory Method, Builder |
| Day 08 | Core Design Patterns II (Behavioral & Structural) | Strategy, Observer, Decorator |
| Day 09 | Build Tools Deep Dive: Maven & Gradle | Dependency management, lifecycles, multi-module |

---

### 🟢 Phase_03 — Databases & Persistence (Days 01–07)
> *Design, query, and persist data; master connection pooling, ORM, and avoid N+1 query traps.*

| Day | Topic | Focus |
| :---: | :--- | :---: |
| Day 01 | SQL Fundamentals & Indexing | Queries, JOINs, aggregations, B-Tree indexes |
| Day 02 | Transactions & ACID | ACID properties, transaction isolation levels |
| Day 03 | JDBC Fundamentals & HikariCP | 🧠 Resource & connection socket buffering |
| Day 04 | ORM Concepts & JPA Basics | Entity mappings, persistence context |
| Day 05 | Relationships, Fetch Types & The N+1 Problem | LAZY vs EAGER, solving N+1 queries |
| Day 06 | Spring Data JPA | Repositories, derived queries, pagination |
| Day 07 | Migrations (Flyway) & NoSQL Basics | Schema versioning, Redis/MongoDB practical intro |

---

### 🟢 Phase_04 — Building APIs with Spring Boot (Days 01–09)
> *Build, secure, validate, and document production REST APIs backed by relational databases.*

| Day | Topic | Focus |
| :---: | :--- | :---: |
| Day 01 | Spring Core: IoC & Dependency Injection | Inversion of Control, beans, constructor DI |
| Day 02 | Spring Boot Fundamentals | Auto-configuration, starters, application.yml |
| Day 03 | Building REST Controllers | Controller mapping, DTO pattern, HTTP semantics |
| Day 04 | Request Validation & Error Handling | Bean Validation, @ControllerAdvice, RFC 7807/9457 |
| Day 05 | Connecting Spring Boot to the Database | Wiring JPA layer into working services |
| Day 06 | API Documentation: OpenAPI & Swagger | Swagger UI, interactive OpenAPI specs |
| Day 07 | Spring Security Basics & JWT | Filter chains, stateless authentication, JWT |
| Day 08 | Configuration & Profiles | Multi-environment config, profiles, secrets |
| Day 09 | Capstone: A Complete CRUD API | Complete enterprise CRUD service |

---

### 🟢 Phase_05 — Testing, Quality & Architecture (Days 01–06)
> *Catch bugs before production with unit, mock, and integration tests; structure code cleanly.*

| Day | Topic | Focus |
| :---: | :--- | :---: |
| Day 01 | Unit Testing with JUnit 5 | Test lifecycle, assertions, parameterized tests |
| Day 02 | Mocking with Mockito | Isolating units under test, verifying interactions |
| Day 03 | Integration Testing & Testcontainers | Testing against real disposable Docker PostgreSQL |
| Day 04 | The TDD Mindset & Pragmatism | Red-Green-Refactor, realistic testing strategies |
| Day 05 | Layered / Clean Architecture | Controller-Service-Repository separation |
| Day 06 | API Design & Code Review Culture | REST conventions, static analysis, team PRs |

---

### 🟢 Phase_06 — Microservices & Distributed Systems Basics (Days 01–07)
> *Understand microservice tradeoffs, inter-service messaging, caching, resilience, and telemetry.*

| Day | Topic | Focus |
| :---: | :--- | :---: |
| Day 01 | Monolith vs. Microservices | Honest tradeoffs, domain boundaries, right calls |
| Day 02 | Inter-Service Communication | Synchronous REST vs async messaging (Kafka/RabbitMQ) |
| Day 03 | API Gateway & Service Discovery | Reverse proxies, edge routing, Spring Cloud Gateway |
| Day 04 | Caching Strategies & Redis | 🧠 Resource buffering (Redis vs JVM memory) |
| Day 05 | Resilience Patterns (Resilience4j) | Circuit breakers, retries, timeouts, bulkheads |
| Day 06 | System Design Basics & Scalability | Horizontal scaling, load balancing, CAP theorem |
| Day 07 | Observability Basics | Structured logging, metrics, Actuator, OpenTelemetry |

---

### 🟢 Phase_07 — DevOps & Deployment Literacy (Days 01–06)
> *Containerize, test, ship, and monitor services self-sufficiently.*

| Day | Topic | Focus |
| :---: | :--- | :---: |
| Day 01 | Git Workflows for Teams | Branching, interactive rebase, PR reviews |
| Day 02 | Docker Fundamentals for Java | Multi-stage Dockerfiles, caching, compose |
| Day 03 | CI/CD Basics (GitHub Actions) | Automated build/test/deploy pipelines |
| Day 04 | Cloud Fundamentals (AWS/GCP/Azure) | Compute, storage, RDS, secrets in the cloud |
| Day 05 | Kubernetes: Just Enough to Understand It | Pods, Deployments, Services, ConfigMaps |
| Day 06 | Production Monitoring & Alerting Basics | SLIs/SLOs, Grafana, alerting mindset |

---

### 🟢 Phase_08 — GenAI Foundations & Java Integration (Days 01–08)
> *Understand LLM mechanics, invoke APIs from Java, master embeddings, vector search, and tools.*

| Day | Topic | Focus |
| :---: | :--- | :---: |
| Day 01 | How LLMs Actually Work, Practically | Tokens, context window, autoregressive generation |
| Day 02 | Prompt Engineering Fundamentals | Few-shot, delimiters, role assignment |
| Day 03 | Calling LLM APIs from Java | Raw HttpClient, streaming SSE tokens |
| Day 04 | Embeddings, Explained | Semantic spaces, dot product, cosine similarity |
| Day 05 | Vector Databases & pgvector | 🧠 Vector-store / embedding memory & indexing |
| Day 06 | Retrieval-Augmented Generation (RAG) | RAG triad, private data search, ground truth |
| Day 07 | Spring AI & LangChain4j Introduction | ChatClient, AiServices, vendor portability |
| Day 08 | Function / Tool Calling & Structured Output | @Tool execution, guaranteed JSON objects |

---

### 🟢 Phase_09 — Building & Operating Production GenAI Systems (Days 01–07)
> *ReAct agents, conversation memory, RAG evaluation, safety, and the final Capstone project.*

| Day | Topic | Focus |
| :---: | :--- | :---: |
| Day 01 | AI Agents & the ReAct Pattern | Reasoning + Acting loops, multi-step workflows |
| Day 02 | Conversation Memory Management | 🧠 Context-window limits & database-backed memory |
| Day 03 | Building a Complete RAG Pipeline in Java | End-to-end ingestion, chunking, retrieval, generation |
| Day 04 | Evaluating LLM Outputs & Guardrails | Hallucination detection, evaluation metrics |
| Day 05 | Cost, Latency & Model Selection Tradeoffs | Token counting, response caching, model routing |
| Day 06 | AI Security Basics | Prompt injection defense, PII masking, safety |
| Day 07 | Capstone Project: Production RAG Assistant | Complete end-to-end enterprise Java AI application |

---

## 📂 Repository Layout

```
JAVA GEN AI COURSE/
├── README.md                                          ← Course Hub (You are here)
├── Java_GenAI_Roadmap.md                              ← Complete Detailed Roadmap Document
├── pom.xml                                            ← Root Maven Multi-Module POM
├── docker-compose.yml                                 ← PostgreSQL + pgvector + Redis + Ollama
│
├── Phase_01_Java_Foundations/                         ← Days 01–08
├── Phase_02_Advanced_Core_Java_JVM_Design/            ← Days 01–09
├── Phase_03_Databases_and_Persistence/                ← Days 01–07
├── Phase_04_Building_APIs_Spring_Boot/                ← Days 01–09
├── Phase_05_Testing_Quality_Architecture/             ← Days 01–06
├── Phase_06_Microservices_Distributed_Systems/        ← Days 01–07
├── Phase_07_DevOps_Deployment_Literacy/               ← Days 01–06
├── Phase_08_GenAI_Foundations_Java_Integration/       ← Days 01–08
└── Phase_09_Production_GenAI_Systems_Capstone/        ← Days 01–07
```

---

<p align="center">
  <b>Built for developers ready to master modern enterprise Java and ship real GenAI systems.</b>
</p>
