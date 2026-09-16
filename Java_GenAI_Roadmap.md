# Java → GenAI Roadmap: Zero to 3-Years-Experienced

> **Designed from the perspective of a Senior Curriculum Architect & Technical Hiring Manager.**
>
> This is the definitive, sequenced learning roadmap that takes someone from absolute zero to a solid, practical 3-years-of-experience level as a **Java engineer who builds production GenAI-powered applications**.

---

## How to Read This Roadmap

Each Phase builds **only** on Phases before it — never skip ahead. Within a Phase, each Day is one focused topic. Depth increases gradually across the whole roadmap, but **caps at what a real 3-years-experienced Java/GenAI engineer needs** — not staff-engineer depth, not academic ML research depth.

**The whole arc, in one glance:**
- **Phases 1–2** build rock-solid core Java, JVM mechanics, and design foundations.
- **Phases 3–5** add the data layer, the web/API layer (Spring Boot), and testing discipline — *"you can now build and ship a real backend service."*
- **Phases 6–7** add distributed-systems thinking and deployment literacy — *"you can now operate that service like a real engineer, not just code it."*
- **Phases 8–9** layer in GenAI specifically — *"you can now build, evaluate, and run LLM-powered features in Java,"* ending in a capstone that ties the whole journey together.

---

## Two Tracks, One Complete Engineer

A competent 3-YOE Java + GenAI engineer is not just an AI prompter and not just an enterprise CRUD developer. They fuse two critical disciplines:

| Track | Focus Area | Why It Matters |
|:---:|:---|:---|
| **Track A: Core Java & Backend Engineering** | The language, JVM internals, persistence, REST APIs, testing, microservices, and DevOps | You cannot build resilient AI features in Java if you don't understand how the JVM manages memory, how transactions behave, or how Spring handles requests. |
| **Track B: Applied GenAI Development** | LLMs, prompt engineering, embeddings, vector stores, RAG, tool calling, memory, and agents | You implement AI features *in Java code*, backed *by relational and vector databases*, deployed *as secure containerized services*. |

---

## What Is Explicitly OUT of Scope (and Why)

A 3-YOE engineer is **not** expected to know:
- **Training or fine-tuning ML models from scratch**: That is ML engineering and data science. Backend application engineers consume, orchestrate, evaluate, and integrate models.
- **Deep ML calculus**: Backpropagation math and loss function derivations are unnecessary for software engineers building LLM systems. You need the operational mental model.
- **Kubernetes cluster administration**: You need to understand pods, deployments, and services to ship code, not manage control planes and etcd clusters.
- **Staff/Principal-level distributed systems trivia**: Global CRDTs, Paxos formal proofs, and multi-region active-active database replication.
- **Bytecode rewriting or writing custom JIT compilers**: You understand class loading and memory layouts for debugging, not compiler hacking.

---

## 🗺️ Complete 9-Phase Curriculum Overview

| Phase | Days | Focus Area | Outcome / Bar |
|:---:|:---:|:---|:---|
| **Phase 01** | 8 Days | **Java Foundations** | Write clean idiomatic Java using OOP, Collections, Streams, Virtual Threads, and I/O. |
| **Phase 02** | 9 Days | **Advanced Core Java, JVM Internals & Design Foundations** | Explain JVM memory, GC pauses, class loading, master SOLID, and apply design patterns. |
| **Phase 03** | 7 Days | **Databases & Persistence** | Design relational schemas, write optimized SQL, master JDBC/HikariCP, JPA, and NoSQL basics. |
| **Phase 04** | 9 Days | **Building APIs with Spring Boot** | Build, validate, document, secure, and configure production-ready REST APIs. |
| **Phase 05** | 6 Days | **Testing, Quality & Architecture** | Write rock-solid JUnit 5/Mockito tests, Testcontainers, and practice layered clean architecture. |
| **Phase 06** | 7 Days | **Microservices & Distributed Systems Basics** | Deconstruct monoliths, inter-service messaging, Redis caching, Resilience4j, and observability. |
| **Phase 07** | 6 Days | **DevOps & Deployment Literacy** | Git workflows, Dockerize services, write CI/CD pipelines, understand cloud and K8s basics. |
| **Phase 08** | 8 Days | **GenAI Foundations & Java Integration** | Call LLMs via Java, embeddings, vector databases, RAG, Spring AI / LangChain4j, and tool calling. |
| **Phase 09** | 7 Days | **Building & Operating Production GenAI Systems** | ReAct agents, conversational memory, RAG evaluation, AI safety, cost optimization, and Capstone. |

**Total Duration: 67 Days**

---

## Phase_01 — Java Foundations (Days 01–08)

**Target Outcome**: You can write clean, non-trivial Java programs using modern syntax, strong type safety, collections, stream pipelines, and concurrent virtual threads.

| Day | Topic | What It Is | Why You Need It | What Problem It Solves | What Happens If You Skip It |
|:---:|:---|:---|:---|:---|:---|
| **01** | **Java Ecosystem & Setup** | JDK vs JRE vs JVM, compilation (`.java` → `.class`), classloaders, memory hierarchy introduction, Maven project layout. | Every Java program runs inside the JVM; understanding the build and execution lifecycle is non-negotiable. | Eliminates platform dependency, memory corruption, and unpredictable runtime failures. | You will be permanently baffled by build tools, classpath errors, and compilation failures. |
| **02** | **OOP: Classes, Objects & Memory** | Heap vs Stack, object allocation, 3 variable scopes, reference vs value, `==` vs `.equals()`, encapsulation, GC roots. | Real-world software models domains with objects; all Spring components and AI DTOs are classes. | Eliminates untestable procedural spaghetti code and manual memory deallocation. | You will write code with memory leaks, reference aliasing bugs, and broken equality semantics. |
| **03** | **Inheritance, Interfaces & Polymorphism** | Abstract classes, interfaces, default methods, runtime polymorphism, Liskov substitution. | Modern frameworks (Spring, Spring AI) are entirely interface-driven to allow pluggable implementations. | Avoids tight coupling and fragile `switch`/`if-else` branching on concrete types. | You cannot understand dependency injection or swap AI providers without rewriting code. |
| **04** | **Generics, Collections & Data Structures** | `List`, `Set`, `Map`, `Queue`, generics type parameters, bounds, wildcards, time complexities (O(1) vs O(n)). | AI data processing relies heavily on collections: token lists, embedding arrays, conversation queues. | Replaces unsafe runtime `Object` casting with compile-time type safety. | You will pick slow O(n²) structures for O(1) lookups and introduce runtime `ClassCastException` bugs. |
| **05** | **Modern Java: Records, Optional, Sealed Types** | Immutability with `record`, handling absence with `Optional`, exhaustive pattern matching with `sealed`. | Modern Java idiomatic standards for DTOs, API responses, and AI pipeline stage modeling. | Eliminates boilerplate getters/hashCodes and prevents rampant `NullPointerException`s. | Your code looks like legacy Java 7; you ship NPEs and write dozens of lines of boilerplate. |
| **06** | **Functional Programming & Stream API** | Lambdas, `Function`/`Predicate`/`Consumer`, `map`, `filter`, `flatMap`, `reduce`, collectors. | Document chunking, token filtering, and AI response transformations are expressed cleanly as streams. | Replaces deeply nested imperative loops and mutable state accumulators. | You write long, fragile, bug-prone loops that cannot be refactored or parallelized cleanly. |
| **07** | **Concurrency & Virtual Threads (Project Loom)** | Threads, `ExecutorService`, `CompletableFuture`, race conditions, Virtual Threads (`Thread.ofVirtual()`). | AI calls are high-latency I/O operations; virtual threads allow scaling to thousands of concurrent requests. | Solves OS thread exhaustion and complex reactive programming callbacks. | Your service bottlenecks at 50 concurrent AI requests, stalling threads and crashing the server. |
| **08** | **I/O, HTTP Client, JSON & Testing Basics** | `java.nio` file I/O, `java.net.http.HttpClient`, Jackson JSON parsing, JUnit 5 assertions and lifecycle. | AI apps read files (RAG data), call HTTP APIs (LLMs), parse JSON, and require verified behavior. | Eliminates brittle custom network code and unverified manual testing. | You cannot ingest documents, communicate with any external AI endpoint, or write testable code. |

---

## Phase_02 — Advanced Core Java, JVM Internals & Design Foundations (Days 01–09)

**Target Outcome**: You can write robust, idiomatic Java, explain what's actually happening inside the JVM, recognize and apply design patterns, and manage builds cleanly.

| Day | Topic | What It Is | Why You Need It | What Problem It Solves | What Happens If You Skip It |
|:---:|:---|:---|:---|:---|:---|
| **01** | **Exception Handling Deep Dive** | Checked vs unchecked exceptions, `try-with-resources`, `AutoCloseable`, custom exceptions, exception chaining, stack trace analysis. | Production stability depends on deterministic failure recovery and meaningful error reporting. | Prevents silent failures, swallowed exceptions, and leaked file/network handles. | Resources leak, root causes are lost when re-throwing, and production outages take hours to debug. |
| **02** | **String Internals & Text Processing** | String immutability, the String Constant Pool, `StringBuilder` vs `StringBuffer`, regex pattern matching. <br>🧠 *Memory: JVM Heap & String Pool.* | LLMs process text; understanding how strings occupy memory prevents massive memory bloat during ingestion. | Avoids creating millions of short-lived string objects that overwhelm the garbage collector. | Inefficient string concatenations during document chunking trigger GC thrashing and OutOfMemoryErrors. |
| **03** | **Enums, Annotations & Reflection Basics** | Advanced enums (fields/methods), custom annotations, runtime reflection inspection, framework mechanics. | Understand how frameworks like Spring, Hibernate, and Jackson inspect metadata and inject behaviors dynamically. | Replaces magic configuration with type-safe metadata attached directly to code elements. | Annotations remain mysterious "black magic," leaving you helpless when component scanning fails. |
| **04** | **JVM Internals: Class Loading & Memory Areas** | Bootstrap/Platform/Application ClassLoaders, Metaspace, Heap (Eden, Survivor, Tenured), Thread Stacks, PC Registers. <br>🧠 *Memory: JVM Stack/Heap & Object Lifecycle.* | Essential for diagnosing memory leaks, stack overflows, and Metaspace exhaustion in high-throughput services. | Explains the physical lifecycle of an object from class loading through allocation to deallocation. | You cannot tune JVM memory flags or diagnose `StackOverflowError` vs `OutOfMemoryError`. |
| **05** | **Garbage Collection, Conceptually** | Generational hypothesis, Stop-The-World pauses, GC algorithms (G1, ZGC), monitoring GC pauses, memory profiling awareness. <br>🧠 *Memory: JVM Heap & Generational GC.* | High-throughput AI microservices require predictable latencies; GC pauses directly spike API response times. | Automates memory reclamation while explaining when and why throughput degrades. | You won't know why your service randomly stalls for 2 seconds or how to choose modern collectors like ZGC. |
| **06** | **SOLID Principles & Clean Code** | Single Responsibility, Open/Closed, Liskov Substitution, Interface Segregation, Dependency Inversion with before/after refactoring. | Architectural foundation for maintainable, testable codebases that grow without rotting. | Eliminates rigid, fragile architectures where a change in one class breaks ten others. | You build monolithic, tightly coupled classes that teammates dread reviewing or modifying. |
| **07** | **Core Design Patterns I (Creational)** | Singleton (thread-safe, enum singleton), Factory Method, Builder pattern with real production use cases. | Used universally across Java SDKs, Spring beans, and AI client configuration builders. | Encapsulates complex instantiation logic and protects object immutability. | Code is cluttered with 10-parameter constructors, inconsistent instance states, and duplicated setup logic. |
| **08** | **Core Design Patterns II (Behavioral & Structural)** | Strategy (swappable algorithms), Observer (event handling), Decorator (wrapping functionality). | Directly underpins Spring Security filters, Spring AI advisors, and pluggable model providers. | Allows dynamic runtime extension of behaviors without modifying existing classes. | You duplicate business logic and write massive conditional statements to switch behaviors. |
| **09** | **Build Tools Deep Dive: Maven & Gradle** | Dependency scopes (`compile`, `test`, `provided`), transitive dependencies, conflict resolution, build lifecycle, multi-module setups. | Enterprise services are structured as multi-module Maven/Gradle projects with complex dependencies. | Automates dependency fetching, test execution, packaging, and artifact publishing. | You get stuck on dependency version conflicts (`NoSuchMethodError`), unable to build or modularize projects. |

---

## Phase_03 — Databases & Persistence (Days 01–07)

**Target Outcome**: You can design, query, and connect relational databases to Java, master connection pooling, prevent N+1 query traps, and handle migrations.

| Day | Topic | What It Is | Why You Need It | What Problem It Solves | What Happens If You Skip It |
|:---:|:---|:---|:---|:---|:---|
| **01** | **SQL Fundamentals & Indexing** | DDL/DML, `SELECT`, `JOIN` (INNER/LEFT/RIGHT), `GROUP BY`, aggregation, primary/foreign keys, B-Tree indexes. | Every backend application stores state; understanding indexes explains why queries run in 2ms vs 20 seconds. | Enables structured relational queries and prevents full table scans on growing datasets. | You write naive unindexed queries that bring production databases to their knees under modest load. |
| **02** | **Transactions & ACID** | Atomicity, Consistency, Isolation, Durability, dirty reads, non-repeatable reads, phantom reads, isolation levels. | Guarantees data integrity during concurrent updates (e.g. user quota deductions, payment flows). | Prevents corrupted, half-written data when a mid-flight operation fails. | Concurrent users overwrite each other's data; system crashes leave orphan records and corrupt balances. |
| **03** | **JDBC Fundamentals & Connection Pooling** | `DriverManager`, `Connection`, `PreparedStatement`, `ResultSet`, connection pools (HikariCP). <br>🧠 *Memory: Resource & Connection Buffering.* | Explains how Java physically communicates with a database socket and why opening raw connections kills servers. | PreparedStatement prevents SQL injection; HikariCP buffers and reuses active database sockets. | Vulnerable to SQL injection; exhausting database connections causes server-wide deadlocks. |
| **04** | **ORM Concepts & JPA Basics** | Object-Relational Mapping, `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@Column`, EntityManager, Persistence Context. | Bridges the impedance mismatch between relational tables and object-oriented Java domain models. | Eliminates hundreds of lines of boilerplate ResultSet-to-object mapping code. | You waste days writing manual mapping logic and struggle to keep database schemas in sync with objects. |
| **05** | **Relationships, Fetch Types & The N+1 Problem** | `@OneToMany`, `@ManyToOne`, `@ManyToMany`, `fetch = LAZY` vs `EAGER`, identifying and solving N+1 queries (`JOIN FETCH`, `@EntityGraph`). | Relationships model domain hierarchies (Users → Conversations → Messages); N+1 is the #1 JPA performance killer. | Controls database query volume and ensures associations load efficiently. | An innocent `findAll()` executes 1,001 SQL queries instead of 1, grinding the database to a halt. |
| **06** | **Spring Data JPA** | `JpaRepository`, derived query methods, `@Query` (JPQL & Native SQL), pagination (`Pageable`, `Slice`, `Page`), sorting. | Industry standard for enterprise Java data access; generates queries automatically from method signatures. | Eliminates repetitive CRUD DAO implementations and provides built-in pagination. | You write verbose boilerplate DAO layers and return unpaginated datasets that crash client browsers. |
| **07** | **Migrations (Flyway) & NoSQL Basics** | Flyway schema versioning (`V1__...sql`), migration lifecycle, Redis & MongoDB practical intro, SQL vs NoSQL tradeoffs. | Production database schemas evolve over time; Flyway ensures migrations run deterministically across all environments. | Solves out-of-sync database schemas between local dev, QA, staging, and production. | Schema updates are applied manually via ad-hoc scripts, causing deployment crashes and data loss. |

---

## Phase_04 — Building APIs with Spring Boot (Days 01–09)

**Target Outcome**: You can build, validate, secure, document, and deploy production-grade REST APIs backed by a database.

| Day | Topic | What It Is | Why You Need It | What Problem It Solves | What Happens If You Skip It |
|:---:|:---|:---|:---|:---|:---|
| **01** | **Spring Core: IoC & Dependency Injection** | Inversion of Control, `ApplicationContext`, `@Component`, `@Service`, `@Repository`, constructor injection vs field injection. | The architectural backbone of the Spring ecosystem; decouples components for modularity and testability. | Eliminates hardcoded `new` instantiations that make unit testing and refactoring impossible. | Your code is tightly coupled, untestable without booting the whole app, and prone to circular dependencies. |
| **02** | **Spring Boot Fundamentals** | Auto-configuration, starters (`spring-boot-starter-web`), `application.properties`/`yml`, externalized config, banner, main entrypoint. | Eliminates massive XML/Java configuration boilerplate and provides an opinionated runtime container. | Replaces complex manual server configuration (Tomcat setup, servlet mappings) with single-line starters. | You fight the framework rather than leveraging its automatic bean configurations and conventions. |
| **03** | **Building REST Controllers** | `@RestController`, `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PathVariable`, `@RequestParam`, `@RequestBody`, DTOs. | The primary way web and mobile clients communicate with backend services and AI capabilities. | Maps incoming HTTP requests to typed Java method executions and serializes responses to JSON. | You violate REST conventions, mix database entities with API models, and leak sensitive internal data. |
| **04** | **Request Validation & Error Handling** | Jakarta Bean Validation (`@NotNull`, `@Size`, `@Email`), `@Valid`, `@ControllerAdvice`, `@ExceptionHandler`, RFC 7807/9457 Problem Details. | Protects backend services against malicious or malformed input and provides consistent error responses. | Prevents corrupted state from bad inputs and hides internal stack traces from malicious callers. | Unvalidated input crashes services with 500 errors, and raw stack traces leak internal infrastructure details. |
| **05** | **Connecting Spring Boot to the Database** | Wiring Spring Data JPA with Spring Boot, HikariCP configuration, transaction boundaries (`@Transactional`), service layer orchestration. | Connects user requests to durable database persistence through clean transactional boundaries. | Ensures database operations execute atomically within well-defined service transactions. | Data updates fail silently mid-operation or trigger database connection leaks under load. |
| **06** | **API Documentation: OpenAPI & Swagger** | SpringDoc OpenAPI, Swagger UI (`/swagger-ui.html`), `@Operation`, `@ApiResponse`, schema annotations, contract-first mentality. | Frontend developers, mobile teams, and partner APIs need interactive documentation to consume your endpoints. | Eliminates out-of-date PDF/Confluence API documentation by generating docs directly from code. | Frontend teams constantly ping you for endpoint specs; API integrations break due to contract mismatches. |
| **07** | **Spring Security Basics & JWT** | SecurityFilterChain, authentication vs authorization, BCrypt password hashing, stateless JWT issuance and validation filter. | Protects sensitive endpoints and restricts access to authenticated users; critical for paid AI features. | Secures public endpoints and establishes stateless user identity across microservice requests. | Your endpoints are left open to the public; unauthenticated users abuse expensive resources and drain budgets. |
| **08** | **Configuration & Profiles** | `@ConfigurationProperties`, environment-specific profiles (`application-dev.yml`, `application-prod.yml`), secret injection via environment variables. | Applications must behave differently in local development vs staging vs production without code changes. | Decouples environment-specific settings (database URLs, API credentials) from application binaries. | Credentials get committed to Git, and configuration mistakes break production during deployment. |
| **09** | **Capstone: A Complete CRUD API** | End-to-end integration: User/Product management service with JPA, validation, custom exceptions, JWT security, and Swagger docs. | Solidifies the entire Spring Boot backend stack into a production-ready, testable, working service. | Ties all isolated concepts into one cohesive, runnable architecture. | You know individual annotations in isolation but struggle to assemble a full enterprise application. |

---

## Phase_05 — Testing, Quality & Architecture (Days 01–06)

**Target Outcome**: You write tests that catch bugs before production, isolate dependencies with mocks, test against real databases, and architect clean layered services.

| Day | Topic | What It Is | Why You Need It | What Problem It Solves | What Happens If You Skip It |
|:---:|:---|:---|:---|:---|:---|
| **01** | **Unit Testing with JUnit 5** | `@Test`, lifecycle hooks (`@BeforeEach`), assertions (`assertEquals`, `assertThrows`), parameterized tests (`@ParameterizedTest`, `@CsvSource`). | Unit tests form the base of the testing pyramid; fast automated verification of core business logic. | Catches regressions immediately on every local build and pull request. | Every bug fix risks breaking existing features; testing requires manual clicking through endpoints. |
| **02** | **Mocking with Mockito** | `@Mock`, `@InjectMocks`, `when().thenReturn()`, `verify()`, ArgumentCaptor, isolating units under test. | Allows testing service logic in total isolation without hitting real databases or third-party APIs. | Removes reliance on external network calls and database state during fast unit tests. | Unit tests are slow, flaky, and fail whenever a remote API or database is temporarily offline. |
| **03** | **Integration Testing & Testcontainers** | `@SpringBootTest`, `@WebMvcTest`, Testcontainers for PostgreSQL, testing against real disposable Docker containers. | Proves that your Spring wiring, SQL queries, and database constraints work together in reality. | Eliminates differences between in-memory mock databases (H2) and real production PostgreSQL. | "Works in H2 on my machine" fails in production due to dialect, constraint, or index discrepancies. |
| **04** | **The TDD Mindset & Pragmatism** | Red-Green-Refactor cycle, test-driven development, when TDD genuinely accelerates development vs when it's overkill. | Helps you think through edge cases, boundary conditions, and API design before writing implementation code. | Prevents writing untestable code and ensures comprehensive test coverage by construction. | You write tightly coupled spaghetti code that is nearly impossible to retrofit with unit tests. |
| **05** | **Layered / Clean Architecture** | Controller → Service → Repository separation, domain models vs DTOs, dependency inversion, anti-corruption layers. | Ensures separation of concerns so changes to one layer (e.g. database schema) do not break others (e.g. web controllers). | Prevents business logic from leaking into controllers or raw SQL queries leaking into UI views. | Monolithic "god classes" where database logic, HTTP handling, and business rules are tangled into an unmaintainable knot. |
| **06** | **API Design & Code Review Culture** | RESTful resource conventions, idempotent operations, pagination/filtering standards, SpotBugs, SonarLint, PR review practices. | Professional code quality standards expected in engineering teams; writing readable code for humans. | Prevents technical debt accumulation and unifies coding styles across engineering teams. | Poorly designed endpoints cause breaking changes, unreadable PRs cause team friction, and bugs slip into production. |

---

## Phase_06 — Microservices & Distributed Systems Basics (Days 01–07)

**Target Outcome**: You understand how real production systems operate across services, handle asynchronous events, cache data effectively, and survive network failures.

| Day | Topic | What It Is | Why You Need It | What Problem It Solves | What Happens If You Skip It |
|:---:|:---|:---|:---|:---|:---|
| **01** | **Monolith vs. Microservices** | Modular monoliths, distributed microservices, domain boundaries, organizational tradeoffs, Conway's Law. | Real-world architectures balance development speed with deployment independence; knowing when to split matters. | Prevents building premature distributed complexity when a clean modular monolith is superior. | You jump into microservices blindly, suffering network latency and distributed debugging nightmares without reason. |
| **02** | **Inter-Service Communication** | Synchronous REST (`RestClient`, `WebClient`) vs Asynchronous event-driven messaging (Kafka/RabbitMQ concepts). | Services must exchange data and trigger background tasks without blocking user requests. | Decouples services so a failure or slow processing in Service B does not freeze Service A. | Cascading latency bottlenecks; an outage in an auxiliary service crashes your entire user-facing API. |
| **03** | **API Gateway & Service Discovery** | Reverse proxies, routing, rate limiting at edge, centralized authentication, Spring Cloud Gateway concepts. | Provides a single entry point for clients, shielding internal microservice topography and centralizing cross-cutting policies. | Prevents clients from having to know and authenticate against dozens of individual microservice URLs. | Complex client integrations, duplicated authentication logic across services, and difficult service migration. |
| **04** | **Caching Strategies & Redis** | Cache-aside pattern, read-through, write-through, cache eviction (TTL, LRU), Redis data structures. <br>🧠 *Memory: Resource Buffering (Redis vs JVM Heap).* | Relieves database load and slashes API response times from 200ms to 2ms for repeated reads. | Prevents repeated expensive database calculations and third-party API queries. | Database collapses under traffic spikes because every identical read hits disk storage. |
| **05** | **Resilience Patterns (Resilience4j)** | Circuit Breakers, Retry with exponential backoff, Timeouts, Bulkheads, Fallback methods. | Distributed systems fail constantly; resilient services anticipate failure and degrade gracefully. | Prevents thread pool exhaustion and cascading failures when a downstream service becomes slow or unresponsive. | A single slow dependency causes threads to hang, cascading into a total system-wide outage. |
| **06** | **System Design Basics & Scalability** | Horizontal vs vertical scaling, stateless services, load balancers, database read replicas, CAP theorem in plain English. | Fundamental principles for designing systems that scale from 100 to 100,000 concurrent users. | Prevents single-point-of-failure bottlenecks and unscalable server-side state. | Your application architecture cannot scale beyond a single physical server; traffic spikes crash the business. |
| **07** | **Observability Basics** | Structured logging (Logback/JSON), metrics (Micrometer/Prometheus), distributed tracing (OpenTelemetry), Spring Boot Actuator. | You cannot fix what you cannot see; observability provides telemetry into system health and request lifecycles. | Pinpoints the exact line of code or slow network call causing errors in production. | Production outages become blind guessing games; you only discover issues when frustrated users complain. |

---

## Phase_07 — DevOps & Deployment Literacy (Days 01–06)

**Target Outcome**: You can containerize, ship, configure, and monitor your own Java services without relying entirely on a dedicated DevOps engineer.

| Day | Topic | What It Is | Why You Need It | What Problem It Solves | What Happens If You Skip It |
|:---:|:---|:---|:---|:---|:---|
| **01** | **Git Workflows for Teams** | Trunk-based vs GitFlow, branching strategies, interactive rebasing, writing clear PR descriptions, resolving merge conflicts. | Software is built by teams; disciplined version control prevents lost work and enables fast collaboration. | Prevents messy Git histories, overwritten code, and blocked deployment pipelines. | You produce merge conflicts that break the build, write unreadable commits, and struggle in team environments. |
| **02** | **Docker Fundamentals for Java** | Images vs Containers, multi-stage Dockerfiles for Java 21, layer caching, `.dockerignore`, `docker-compose.yml`. | Packages your Java app with its runtime dependencies into a reproducible, portable container. | Eliminates the classic "works on my machine" problem across different developer machines and servers. | Deployment requires manual Java/environment installations on servers, leading to configuration drift and failure. |
| **03** | **CI/CD Basics (GitHub Actions)** | Continuous Integration / Continuous Deployment, automated test execution on PRs, building Docker images, pipeline secrets. | Automates building, testing, and packaging on every commit so only verified code reaches staging/production. | Replaces manual, error-prone local builds and manual server deployments. | Broken code gets merged into main; releases take hours of manual effort and risk human error. |
| **04** | **Cloud Fundamentals (AWS/GCP/Azure)** | IaaS vs PaaS vs SaaS, compute (EC2/ECS), storage (S3), managed databases (RDS), secrets management. | Modern Java services run in the cloud; engineers must understand the runtime environment their code inhabits. | Eliminates managing physical server hardware, manual OS patching, and raw disk storage arrays. | You are unable to deploy or debug your applications outside of your local laptop. |
| **05** | **Kubernetes: Just Enough to Understand It** | Pods, Deployments, Services, ConfigMaps, Secrets, Horizontal Pod Autoscaler, rolling updates. | Industry standard container orchestration platform; running Java containers at scale in production. | Automatically restarts crashed containers, balances traffic, and performs zero-downtime rolling updates. | You don't understand how your production application is routed, scaled, or health-checked by platform clusters. |
| **06** | **Production Monitoring & Alerting Basics** | Error budgets, SLIs/SLOs, dashboard creation (Grafana), alert rules, on-call mindset and post-mortem culture. | Keeps services reliable by alerting engineers before minor anomalies escalate into full outages. | Prevents alert fatigue and ensures critical failures notify the right engineer immediately. | Alerts go unmonitored; minor memory leaks grow unnoticed until the entire production cluster crashes. |

---

## Phase_08 — GenAI Foundations & Java Integration (Days 01–08)

**Target Outcome**: You understand how LLMs operate mathematically and practically, call them reliably from Java, generate and store embeddings, build RAG pipelines, and invoke Java functions via tool calling.

| Day | Topic | What It Is | Why You Need It | What Problem It Solves | What Happens If You Skip It |
|:---:|:---|:---|:---|:---|:---|
| **01** | **How LLMs Actually Work, Practically** | Autoregressive token prediction, tokenization, context window limits, temperature, top-p, prompt-to-response generation loop. | Demystifies LLMs from "magic" into probabilistic next-token predictors, forming realistic engineering expectations. | Clarifies why LLMs hallucinate, lose context, or generate nondeterministic outputs. | You treat the model as an omniscient database, leading to naive prompt design and unhandled hallucinations. |
| **02** | **Prompt Engineering Fundamentals** | Zero-shot, few-shot with examples, system prompts vs user prompts, chain-of-thought, delimiter framing, defensive formatting. | The quality of an LLM's response is directly constrained by the structure and clarity of the prompt. | Reduces hallucination, forces desired output structure, and primes the model with required context. | LLMs produce unpredictable, rambly, or inaccurate outputs that break downstream parsers. |
| **03** | **Calling LLM APIs from Java** | Direct HTTP integration with LLM providers (OpenAI/Anthropic/Ollama) using `java.net.http.HttpClient`, JSON payloads, streaming SSE tokens. | Teaches the underlying raw HTTP wire protocol for LLMs before introducing framework abstractions. | Removes framework mystique by showing that LLM APIs are just JSON-over-HTTP endpoints. | You are completely helpless if a framework bug occurs, unable to understand what is sent over the wire. |
| **04** | **Embeddings, Explained** | Vector representations of text, high-dimensional semantic spaces, dot product, cosine similarity from geometric intuition. | The foundational mathematical mechanism that enables semantic search, recommendation, and RAG. | Solves the limitation of keyword search (finding concepts by meaning rather than exact word matches). | You cannot understand how semantic retrieval works, treating vector embeddings as arbitrary numbers. |
| **05** | **Vector Databases & pgvector** | Vector indexing (HNSW vs IVFFlat), storing embeddings in PostgreSQL via `pgvector`, similarity search SQL queries. <br>🧠 *Memory: Vector-Store & Embedding Memory.* | Persists and indexes vector embeddings alongside relational business data in PostgreSQL. | Enables fast similarity search across millions of vector records without brute-force comparisons. | Computing similarity requires scanning entire datasets in memory, which crashes the JVM at scale. |
| **06** | **Retrieval-Augmented Generation (RAG)** | The RAG triad: Ingestion (chunking/embedding), Retrieval (vector query), Generation (prompt augmentation with retrieved context). | Solves the LLM's training cutoff and lack of access to private company/proprietary documents. | Eliminates hallucinations on private domain knowledge without expensive model fine-tuning. | Your AI application can only answer general public questions and invents false answers about your private data. |
| **07** | **Spring AI & LangChain4j Introduction** | Java-native GenAI frameworks: `ChatClient`, `AiServices`, prompt templates, model abstractions, vendor portability. | Standardizes AI interactions in Java; switch between OpenAI, Anthropic, or Ollama without rewriting code. | Eliminates low-level HTTP boilerplate and manual JSON parsing for AI completions. | You write fragile custom client code and get locked into a single AI provider's proprietary API format. |
| **08** | **Function / Tool Calling & Structured Output** | Giving LLMs Java tools via `@Tool`, function call schemas, forcing guaranteed JSON schemas via bean output converters. | Transforms LLMs from passive text generators into agents that can query databases and trigger Java business logic. | Solves the problem of extracting reliable, type-safe Java objects from unstructured LLM responses. | You rely on fragile regex strings to parse LLM outputs, which fail whenever the model changes formatting. |

---

## Phase_09 — Building & Operating Production GenAI Systems (Days 01–07)

**Target Outcome**: You can design, implement, evaluate, secure, and ship an end-to-end, production-ready GenAI application in Java that operates reliably and cost-effectively.

| Day | Topic | What It Is | Why You Need It | What Problem It Solves | What Happens If You Skip It |
|:---:|:---|:---|:---|:---|:---|
| **01** | **AI Agents & the ReAct Pattern** | Reasoning + Acting loop (Think → Act → Observe), iterative multi-step goal execution, knowing when NOT to use an agent. | Enables solving complex, multi-step problems that cannot be answered in a single prompt. | Automates multi-hop workflows (e.g. "Look up user → check invoice → calculate refund → issue email"). | Your AI features remain limited to single-turn Q&A, unable to complete real multi-step workflows. |
| **02** | **Conversation Memory Management** | Short-term sliding window, token-based windowing, message summarization, persisting chat state to PostgreSQL. <br>🧠 *Memory: LLM Context-Window & Conversational Memory.* | Multi-turn chat requires context continuity while preventing expensive context window overflow. | Prevents LLM context limit crashes and controls token costs during long conversations. | Chatbots forget past messages immediately or exceed context limits, crashing with API errors. |
| **03** | **Building a Complete RAG Pipeline in Java** | Document parsing (PDF/TXT), intelligent chunking with overlap, embedding generation, vector storage, reranking, and generation. | The standard enterprise pattern for building knowledge-grounded AI assistants on proprietary documents. | Converts raw unsearchable files into an interactive, grounded question-answering service. | Chunks are split naively mid-sentence, retrieval misses key context, and answers degrade in quality. |
| **04** | **Evaluating LLM Outputs & Guardrails** | Hallucination detection, RAG metrics (faithfulness, answer relevance), input/output guardrails, prompt defense. | AI systems are nondeterministic; automated evaluation ensures updates improve quality rather than degrading it. | Replaces "vibes-based" manual checking with systematic regression testing. | You deploy prompt changes blindly, discovering hallucinations and offensive outputs only when users complain. |
| **05** | **Cost, Latency & Model Selection Tradeoffs** | Token counting, model routing (cheap models for simple tasks, frontier models for complex reasoning), response caching. | LLM calls cost real money per token and take seconds; cost optimization makes AI products economically viable. | Prevents runaway API bills and slashes end-user latency for frequent queries. | Popular features incur thousands of dollars in surprise cloud bills, making the product unsustainable. |
| **06** | **AI Security Basics** | Direct and indirect prompt injection attacks, data exfiltration risks, system prompt leaks, PII masking before sending to LLMs. | Secures AI endpoints against adversarial users who attempt to hijack the model or leak private data. | Prevents attackers from manipulating system instructions or stealing proprietary information via prompts. | Attackers bypass business rules, expose company secrets, or weaponize your AI assistant against users. |
| **07** | **Capstone Project: Production RAG Assistant** | Build and deploy a complete, secure, observable Java GenAI application end-to-end: REST API + pgvector + Spring AI / LangChain4j + Docker. | Unifies every phase of the roadmap into a showcase, production-ready portfolio application. | Proves that you can design, build, test, and deploy a real enterprise Java + GenAI service. | You have fragmented theoretical knowledge but cannot architect and ship a complete real-world system. |

---

## 📌 Critical Gaps & Industry Suggestions Integrated

This updated roadmap addresses the real-world demands of engineering teams:

1. **Debugging & Reading Stack Traces**: Integrated immediately into Phase 02 (Day 01) so engineers can decipher chained exceptions, root causes, and framework stack traces early.
2. **Cost Awareness with Paid LLM APIs**: Covered in Phase 08 and deeply mastered in Phase 09 (Day 05). Token economics, prompt caching, and smaller/cheaper model routing are treated as first-class engineering responsibilities.
3. **Reading Open-Source Codebases**: Practiced across Phase 02, 04, and 08 by studying how Spring Boot, HikariCP, and Spring AI implement their internal abstractions.
4. **Soft Skills for the 3-YOE Bar**: Emphasized in Phase 05 and Phase 07: writing clear Pull Request descriptions, estimating story points, defending architectural tradeoffs, and communicating failure modes.
5. **Single-Provider Focus for Deep Muscle Memory**: The roadmap standardizes on a primary provider setup (OpenAI API or local Ollama) for consistency, demonstrating how polymorphism and frameworks (Spring AI / LangChain4j) make swapping models effortless when needed.

---

## 🧠 Memory Perspective Cross-Reference

Every modern senior engineer needs a crystal-clear mental model of where data resides at every layer of the architecture:

```
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│                                ARCHITECTURAL MEMORY MAP                                     │
├──────────────────────────┬─────────────────────────────────┬────────────────────────────────┤
│ Memory Domain            │ What Lives Here                 │ Course Phase & Day             │
├──────────────────────────┼─────────────────────────────────┼────────────────────────────────┤
│ 1. JVM Stack             │ Primitive values, references,   │ Phase 01 (Day 02),             │
│                          │ stack frames, thread isolation  │ Phase 02 (Day 04)              │
│ 2. JVM Heap & Pool       │ Allocated objects, arrays,      │ Phase 01 (Day 02),             │
│                          │ String Constant Pool            │ Phase 02 (Day 02, 04, 05)      │
│ 3. Connection Buffering  │ Sockets, pooled DB connections  │ Phase 03 (Day 03)              │
│                          │ (HikariCP connection pool)      │                                │
│ 4. External Cache Memory │ Key-value stores (Redis cache), │ Phase 06 (Day 04)              │
│                          │ distinct from JVM heap          │                                │
│ 5. Vector Store Memory   │ High-dimensional float vectors, │ Phase 08 (Day 05)              │
│                          │ HNSW graph indexes (pgvector)   │                                │
│ 6. LLM Context Window    │ In-flight conversation tokens,  │ Phase 08 (Day 01),             │
│                          │ augmented RAG context, system   │ Phase 09 (Day 02)              │
└──────────────────────────┴─────────────────────────────────┴────────────────────────────────┘
```
