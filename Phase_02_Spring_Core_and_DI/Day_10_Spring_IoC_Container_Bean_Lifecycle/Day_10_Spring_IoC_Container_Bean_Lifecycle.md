# 🏛️ Day 10: Spring IoC Container & Bean Lifecycle
## Component, Service, Repository, Bean Scopes, and the Lifecycle Pipeline

| ⬅️ Previous Day | 📚 Course Hub | ➡️ Next Day |
|:---|:---:|---:|
| [← Day 09: The Problem Spring Solves — Dependency Hell](../Day_09_Problem_Spring_Solves_Dependency_Hell/Day_09_Problem_Spring_Solves_Dependency_Hell.md) | [All 60 Days Overview](../../README.md) | [Day 11: Dependency Injection In-Depth →](../Day_11_Dependency_Injection_In_Depth/Day_11_Dependency_Injection_In_Depth.md) |

[![Phase](https://img.shields.io/badge/Phase_02-Spring_Core_%26_DI-brightgreen.svg?style=for-the-badge)](../../README.md)
[![Day](https://img.shields.io/badge/Day-10_of_60-blue.svg?style=for-the-badge)](../../README.md)
[![Difficulty](https://img.shields.io/badge/Difficulty-Intermediate-blue.svg?style=for-the-badge)](../../README.md)
[![Topic](https://img.shields.io/badge/Spring_Core-Bean_Lifecycle-purple.svg?style=for-the-badge)](../../README.md)

---

![Spring Framework Bean Lifecycle and IoC Container](assets/day10_bean_lifecycle.jpg)

## 🗺️ Table of Contents
- [1. Topic Overview](#1-topic-overview)
- [2. Basic Foundations (True Zero)](#2-basic-foundations-true-zero)
  - [2.1 What is an ApplicationContext, a Stereotype, and Bean Scope?](#21-what-is-an-applicationcontext-a-stereotype-and-bean-scope)
  - [2.2 The 5-Star Luxury Hotel Analogy](#22-the-5-star-luxury-hotel-analogy)
  - [2.3 Minimal Working Example: Stereotypes & Lifecycle Hooks](#23-minimal-working-example-stereotypes--lifecycle-hooks)
  - [2.4 Line-by-Line Code Breakdown](#24-line-by-line-code-breakdown)
- [3. Core Concept Walkthrough (Basic → Intermediate)](#3-core-concept-walkthrough-basic--intermediate)
  - [3.1 Container Architecture: `BeanFactory` vs. `ApplicationContext`](#31-container-architecture-beanfactory-vs-applicationcontext)
  - [3.2 Stereotype Annotations: `@Component`, `@Service`, `@Repository`](#32-stereotype-annotations-component-service-repository)
  - [3.3 Bean Scopes: Singleton vs. Prototype vs. Web Scopes](#33-bean-scopes-singleton-vs-prototype-vs-web-scopes)
  - [3.4 The 7-Step Bean Lifecycle Pipeline](#34-the-7-step-bean-lifecycle-pipeline)
  - [3.5 Pre-Warming AI Models with `@PostConstruct`](#35-pre-warming-ai-models-with-postconstruct)
  - [3.6 Graceful Resource Teardown with `@PreDestroy`](#36-graceful-resource-teardown-with-predestroy)
  - [3.7 Circular Dependencies: Detection & Resolution](#37-circular-dependencies-detection--resolution)
- [4. Prerequisite & Supporting Concepts](#4-prerequisite--supporting-concepts)
  - [Prerequisite / Supporting Concept: Classpath Scanning (@ComponentScan) Mechanics](#prerequisite--supporting-concept-classpath-scanning-componentscan-mechanics)
  - [Prerequisite / Supporting Concept: Statelessness vs. Stateful Architecture](#prerequisite--supporting-concept-statelessness-vs-stateful-architecture)
  - [Prerequisite / Supporting Concept: BeanPostProcessor & Proxy Wrapping Basics](#prerequisite--supporting-concept-beanpostprocessor--proxy-wrapping-basics)
- [5. Advanced Depth (Intermediate → Advanced)](#5-advanced-depth-intermediate--advanced)
  - [5.1 Senior Deep Dive: The Prototype-in-Singleton Injection Problem](#51-senior-deep-dive-the-prototype-in-singleton-injection-problem)
  - [5.2 Lifecycle Callback Sequencing & Aware Interfaces](#52-lifecycle-callback-sequencing--aware-interfaces)
  - [5.3 Common Mistakes & Misconceptions (With Bad vs. Good Code)](#53-common-mistakes--misconceptions-with-bad-vs-good-code)
  - [5.4 Architectural Trade-Offs: Eager Initialization vs. Lazy Bootstrapping](#54-architectural-trade-offs-eager-initialization-vs-lazy-bootstrapping)
- [6. Quick Recap](#6-quick-recap)
- [7. Self-Check Questions & Practice Exercises](#7-self-check-questions--practice-exercises)
  - [Self-Check Questions (Basic to Advanced)](#self-check-questions-basic-to-advanced)
  - [Hands-On Practice Exercises with Full Solutions](#hands-on-practice-exercises-with-full-solutions)

---

# 1. Topic Overview

The **Spring Inversion of Control (IoC) Container** (`ApplicationContext`) is the runtime engine responsible for discovering, instantiating, wiring, and managing the complete lifecycle of Java objects. Through **Stereotype Annotations** (`@Component`, `@Service`, `@Repository`), **Bean Scopes** (`singleton`, `prototype`), and **Lifecycle Hooks** (`@PostConstruct`, `@PreDestroy`), Spring manages application components with deterministic predictability.

### Why This Topic Matters
In production Generative AI applications, components have vastly different lifecycle and state requirements. Core clients like `OpenAiClient` or `VectorDatabaseConnector` are expensive, thread-safe singletons that should be created once at startup and pre-warmed using `@PostConstruct` to avoid latency spikes on initial user prompts. Conversely, conversation memory buffers (`ChatSessionState`) hold private, multi-turn dialogue histories that must be isolated per user via prototype or session scopes. Understanding the Spring container ensures high performance without cross-user data contamination.

> 💡 **New Word Alert — "ApplicationContext"**: The enterprise Spring IoC container that holds the registry of all active bean instances, coordinates dependency injection, and dispatches lifecycle events.

> 💡 **New Word Alert — "Bean Scope"**: The policy defining how many instances of a bean Spring creates and how long those instances survive in memory (e.g., `singleton` vs. `prototype`).

> 💡 **New Word Alert — "@PostConstruct"**: A lifecycle callback annotation placed on a void method instructing Spring to execute it immediately after all dependency injections have completed.

---

# 2. Basic Foundations (True Zero)

Let's begin with absolute basics, assuming no prior experience with Spring container lifecycles.

### 2.1 What is an ApplicationContext, a Stereotype, and Bean Scope?

- **`ApplicationContext`**: The master registry and central manager of your Spring application. It holds all active objects (beans) in memory.
- **Stereotype Annotations**: Stickers you place on your classes so Spring recognizes what role they play:
  - `@Component`: A general-purpose managed bean.
  - `@Service`: A bean that coordinates business logic and AI prompts.
  - `@Repository`: A bean that talks to databases and translates database errors.
- **Bean Scope**: Dictates who gets to share this object:
  - *Singleton (Default)*: Exactly one shared instance for the entire application.
  - *Prototype*: A brand-new instance created every time someone asks for it.

---

### 2.2 The 5-Star Luxury Hotel Analogy

```
                           THE LUXURY HOTEL (Spring Container)
┌─────────────────────────────────────────────────────────────────────────────┐
│ 1. The Swimming Pool & Fitness Center (Singleton Scope):                    │
│    Built once when the hotel opens. Shared simultaneously by all 500 guests.│
│                                                                             │
│ 2. The Private Room Key & Slippers (Prototype Scope):                       │
│    Issued fresh on demand whenever a new guest checks in. Never shared.     │
│                                                                             │
│ 3. The Welcome Fruit Basket (@PostConstruct):                               │
│    Placed in your room AFTER the furniture is arranged, right before you    │
│    walk through the door.                                                   │
│                                                                             │
│ 4. Housekeeping Room Clean-up (@PreDestroy):                                │
│    Cleans the room and shuts off the air conditioning after you check out.  │
└─────────────────────────────────────────────────────────────────────────────┘
```

In your AI application:
- **`ChatClient`** is the swimming pool (Singleton: shared by all concurrent users).
- **`ConversationHistory`** is the private room key (Prototype: private per conversation).
- **`@PostConstruct`** pre-warms local AI embedding weights before user prompts arrive!

---

### 2.3 Minimal Working Example: Stereotypes & Lifecycle Hooks

Let's write a minimal, fully runnable demonstration of Spring-style stereotypes and lifecycle execution:

```java
package com.javagenai.day10;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

@Service
public class SimpleAIService {

    public SimpleAIService() {
        System.out.println("1. Constructor: Object allocated in RAM.");
    }

    @PostConstruct
    public void init() {
        System.out.println("2. @PostConstruct: Dependencies wired. Pre-warming AI model...");
    }

    public String generateResponse(String prompt) {
        return "Simulated answer to: " + prompt;
    }

    @PreDestroy
    public void cleanup() {
        System.out.println("3. @PreDestroy: Application stopping. Releasing connections.");
    }
}
```

---

### 2.4 Line-by-Line Code Breakdown

1. `@Service`: Registers this class in the `ApplicationContext` as a business layer component.
2. `public SimpleAIService()`: The constructor is invoked first by the container to allocate the object on the Heap.
3. `@PostConstruct public void init()`: Runs automatically after constructor execution and field injection, making it the ideal location for model warmup logic.
4. `generateResponse(...)`: The operational business method.
5. `@PreDestroy public void cleanup()`: Runs automatically when the container closes, cleanly disconnecting resources.

---

# 3. Core Concept Walkthrough (Basic → Intermediate)

Now let's examine container internals and multi-scoped AI architectures.

### 3.1 Container Architecture: `BeanFactory` vs. `ApplicationContext`

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        BeanFactory (Legacy Core)                        │
│  - Basic dependency injection                                           │
│  - Lazy bean initialization (instantiates beans only on getBean())      │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                     ApplicationContext (Enterprise)                     │
│  - Extends BeanFactory                                                  │
│  - Eager pre-instantiation of Singletons at startup (catches bugs early)│
│  - Environment property binding, i18n, and event publication            │
│  - Full Spring Boot integration baseline                                │
└─────────────────────────────────────────────────────────────────────────┘
```

In 100% of modern enterprise applications, you interact with `ApplicationContext`.

---

### 3.2 Stereotype Annotations: `@Component`, `@Service`, `@Repository`

```
                               ┌────────────────┐
                               │  @Component    │  (Generic Spring-managed Bean)
                               └───────┬────────┘
                                       │
               ┌───────────────────────┼───────────────────────┐
               ▼                       ▼                       ▼
      ┌────────────────┐      ┌────────────────┐      ┌────────────────┐
      │    @Service    │      │  @Repository   │      │ @RestController│
      │(Business Logic)│      │(Data Access/DB)│      │  (Web Layer)   │
      └────────────────┘      └────────────────┘      └────────────────┘
```

- **`@Component`**: Generic utility or helper beans (e.g., `TokenCostEstimator`).
- **`@Service`**: Business workflow coordinators. In Spring AI, services construct prompts, call LLMs, and handle business fallback policies.
- **`@Repository`**: Data access gateways. In addition to component registration, Spring wraps database queries and automatically translates native database exceptions (`SQLException`) into Spring's unchecked `DataAccessException` hierarchy.

---

### 3.3 Bean Scopes: Singleton vs. Prototype vs. Web Scopes

#### 1. Singleton Scope (The Default):
- Exactly **one instance** exists in the `ApplicationContext`.
- Shared across all threads.
- **Requirement**: Singletons must be **strictly stateless or thread-safe**. Never store user prompts in instance fields!

```java
@Service // Singleton by default
public class StatelessAiGateway {
    private final ChatModel chatModel;

    public StatelessAiGateway(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String ask(String prompt) {
        return chatModel.call(prompt); // Stateless: prompt passed as parameter!
    }
}
```

#### 2. Prototype Scope:
- Spring creates a **brand-new instance** every time the bean is requested or injected.
- Ideal for mutable, conversation-specific session state:

```java
package com.javagenai.day10;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
@Scope("prototype")
public class ChatConversationSession {
    private final List<String> history = new ArrayList<>();

    public void addMessage(String msg) {
        history.add(msg);
    }

    public List<String> getHistory() {
        return List.copyOf(history);
    }
}
```

#### 3. Web Scopes:
- **`@RequestScope`**: One instance created per HTTP request; destroyed when request completes.
- **`@SessionScope`**: One instance created per HTTP user session.

---

### 3.4 The 7-Step Bean Lifecycle Pipeline

```
 1. Instantiation          ──►  Constructor called: new MyService()
         │
 2. Populate Properties    ──►  Dependencies injected via constructor / fields
         │
 3. Aware Interfaces       ──►  BeanNameAware, ApplicationContextAware notified
         │
 4. BeanPostProcessor      ──►  postProcessBeforeInitialization()
         │
 5. Initialization         ──►  @PostConstruct method executed!
         │
 6. BeanPostProcessor      ──►  postProcessAfterInitialization() (AOP Proxy wrapped)
         │
 ══════════════════════════════════════════════════════════════════════════
    READY FOR SERVICE      ──►  Bean actively handles production traffic!
 ══════════════════════════════════════════════════════════════════════════
         │
 7. Destruction            ──►  @PreDestroy executed on graceful application shutdown!
```

---

### 3.5 Pre-Warming AI Models with `@PostConstruct`

When running local models (via Ollama or ONNX in-memory vectors), the first user request might experience a 5-to-10 second cold-start latency spike. Use `@PostConstruct` to warm up weights during boot:

```java
package com.javagenai.day10;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class LocalEmbeddingEngine {

    @PostConstruct
    public void warmUpEngine() {
        System.out.println("[LocalEmbeddingEngine] Bootstrapping...");
        System.out.println("[LocalEmbeddingEngine] Pre-loading vector embedding model into memory...");
        // Warm-up inference
        System.out.println("[LocalEmbeddingEngine] ✅ Model warmed up! Ready for sub-5ms queries.");
    }
}
```

---

### 3.6 Graceful Resource Teardown with `@PreDestroy`

When an application receives a `SIGTERM` signal (e.g., Kubernetes rolling pod update), `@PreDestroy` executes before the JVM halts:

```java
package com.javagenai.day10;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

@Component
public class VectorDatabaseConnector {

    @PreDestroy
    public void disconnect() {
        System.out.println("[VectorDatabaseConnector] Application shutdown detected.");
        System.out.println("[VectorDatabaseConnector] Flushing memory buffers to pgvector...");
        System.out.println("[VectorDatabaseConnector] ✅ Database connections closed cleanly.");
    }
}
```

---

### 3.7 Circular Dependencies: Detection & Resolution

If `ServiceA` requires `ServiceB` in its constructor, and `ServiceB` requires `ServiceA`:
- Spring cannot determine which bean to instantiate first.
- The boot process halts with `BeanCurrentlyInCreationException`.
- **The Senior Fix**: Extract shared operations into a distinct `ServiceC` so that dependencies flow in one direction without circular cycles.

---

# 4. Prerequisite & Supporting Concepts

### Prerequisite / Supporting Concept: Classpath Scanning (@ComponentScan) Mechanics

When a Spring Boot application starts, `@SpringBootApplication` enables **`@ComponentScan`**.
Spring scans all `.class` files in the package and its sub-packages, reading class-level bytecode metadata using ASM. When it detects `@Component` or its stereotypes, it creates `BeanDefinition` metadata objects in memory.

---

### Prerequisite / Supporting Concept: Statelessness vs. Stateful Architecture

- **Stateless Bean**: Contains no mutable instance fields. All request state is passed via method parameters. Can safely be executed by 10,000 concurrent threads simultaneously (Singleton).
- **Stateful Bean**: Contains instance fields that change during execution. Must be scoped per request, per user, or guarded by synchronization (Prototype / RequestScope).

---

### Prerequisite / Supporting Concept: BeanPostProcessor & Proxy Wrapping Basics

A `BeanPostProcessor` is a Spring internal extension point. In step 6 of the lifecycle, Spring intercepts initialized beans. If a bean has `@Transactional` or security annotations, Spring wraps the bean inside a dynamic CGLIB/JDK **Proxy object** to handle cross-cutting behavior.

---

# 5. Advanced Depth (Intermediate → Advanced)

### 5.1 Senior Deep Dive: The Prototype-in-Singleton Injection Problem

A classic senior interview question:
> *"What happens when you inject a `@Scope("prototype")` bean directly into a `@Scope("singleton")` bean via constructor injection?"*

- **The Trap**: The singleton bean is only instantiated **once** at startup. Its constructor runs only once. Therefore, the prototype bean is injected **once**, behaving like a singleton!
- **The Solution**: Use **`ObjectProvider<T>`** or `@Lookup` to request a fresh prototype instance on demand:

```java
@Service
public class ChatService {
    private final ObjectProvider<ChatConversationSession> sessionProvider;

    public ChatService(ObjectProvider<ChatConversationSession> sessionProvider) {
        this.sessionProvider = sessionProvider;
    }

    public void handleNewUser() {
        ChatConversationSession freshSession = sessionProvider.getObject(); // Mints brand new instance!
    }
}
```

---

### 5.2 Lifecycle Callback Sequencing & Aware Interfaces

1. `BeanNameAware.setBeanName()`
2. `BeanFactoryAware.setBeanFactory()`
3. `ApplicationContextAware.setApplicationContext()`
4. `BeanPostProcessor.postProcessBeforeInitialization()`
5. `@PostConstruct` / `InitializingBean.afterPropertiesSet()`
6. `BeanPostProcessor.postProcessAfterInitialization()` (AOP Proxies created here)

---

### 5.3 Common Mistakes & Misconceptions (With Bad vs. Good Code)

#### Mistake 1: Storing User State in a Singleton Bean
**Bad Code:**
```java
@Service // ❌ Singleton holding user-specific mutable state!
public class BadChatBot {
    private String currentUserPrompt; // Data leaks between concurrent users!
}
```
**Correct Code:**
```java
@Service
public class GoodChatBot {
    // ✅ Stateless: pass prompt as method argument
    public String handle(String userPrompt) { ... }
}
```

#### Mistake 2: Heavy Blocking Logic in `@PostConstruct` Stalling Application Boot
Placing a 60-second blocking download in `@PostConstruct` delays application startup and causes Kubernetes readiness probes to fail. Offload heavy downloads to a background thread or trigger them asynchronously.

---

### 5.4 Architectural Trade-Offs: Eager Initialization vs. Lazy Bootstrapping

- **Eager Initialization (Default)**: All singletons are created at startup. Catches missing dependencies and bad configs immediately, but increases startup time.
- **Lazy Initialization (`@Lazy`)**: Beans are instantiated only when first called. Decreases startup time, but defers configuration and memory errors to runtime.

---

# 6. Quick Recap

| Annotation / Concept | Scope / Phase | Purpose |
| :--- | :--- | :--- |
| **`@Component`** | Class Level | Generic Spring-managed bean. |
| **`@Service`** | Class Level | Business layer stereotype for AI orchestration. |
| **`@Repository`** | Class Level | Persistence stereotype with automatic SQL exception translation. |
| **`singleton`** | Default Scope | Exactly 1 shared instance across entire application. |
| **`prototype`** | Per Request Scope | Brand-new instance created on every injection or call. |
| **`@PostConstruct`**| Lifecycle Stage 5 | Initialization hook to warm up models or test connections. |
| **`@PreDestroy`** | Lifecycle Stage 7 | Teardown hook to flush buffers and close connections. |

---

# 7. Self-Check Questions & Practice Exercises

### Self-Check Questions (Basic to Advanced)

1. **What is the difference between `@Component` and `@Service`?**
   - *Answer*: Technically, `@Service` is a specialized meta-annotation containing `@Component`. Semantically, `@Service` clarifies that the class coordinates business logic, making architectural intent obvious and enabling service-specific tooling.
2. **What is the default scope of a Spring Bean and what constraint does it impose?**
   - *Answer*: `singleton`. It requires that the bean must be strictly stateless or thread-safe, as it will be accessed concurrently across multiple threads.
3. **When does `@PostConstruct` execute in the bean lifecycle?**
   - *Answer*: Immediately after the bean has been instantiated and all dependencies have been injected, but before the bean is made available to the rest of the application.
4. **How do you safely inject a prototype bean into a singleton service so that new instances are generated on each request?**
   - *Answer*: By injecting an `ObjectProvider<T>` and invoking `provider.getObject()`, or by using method injection via `@Lookup`.
5. **What is the purpose of `@PreDestroy` in production AI systems?**
   - *Answer*: It enables graceful resource teardown during application shutdown, ensuring vector buffers are flushed to disk and database connections are closed cleanly.

---

### Hands-On Practice Exercises with Full Solutions

#### 🏋️ Exercise 1: Building a Stateful Chat Session Factory
**Objective**: Build a `@Service` named `ChatSessionManager` that creates and tracks prototype-scoped `ChatConversationSession` instances keyed by a `UUID sessionId`.

```java
package com.javagenai.day10;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class ChatSessionManager {
    private final ObjectProvider<ChatConversationSession> sessionProvider;
    private final Map<UUID, ChatConversationSession> activeSessions = new HashMap<>();

    public ChatSessionManager(ObjectProvider<ChatConversationSession> sessionProvider) {
        this.sessionProvider = sessionProvider;
    }

    public UUID startNewSession() {
        UUID id = UUID.randomUUID();
        ChatConversationSession freshSession = sessionProvider.getObject(); // Mints new prototype instance!
        activeSessions.put(id, freshSession);
        return id;
    }

    public Optional<ChatConversationSession> getSession(UUID id) {
        return Optional.ofNullable(activeSessions.get(id));
    }
}
```

---

#### 🏋️ Exercise 2: Self-Testing Vector Store with `@PostConstruct`
**Objective**: Build a `@Component` named `SelfTestingVectorStore` that automatically runs a connectivity ping during startup inside `@PostConstruct`.

```java
package com.javagenai.day10;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class SelfTestingVectorStore {

    @PostConstruct
    public void runStartupHealthCheck() {
        System.out.println("[Startup] Running Vector Store connectivity self-check...");
        boolean pingSuccess = executePing();
        if (pingSuccess) {
            System.out.println("[Startup] ✅ Vector Store connectivity VERIFIED.");
        } else {
            System.err.println("[Startup] ⚠️ Vector Store ping FAILED. Verify Docker pgvector is running!");
        }
    }

    private boolean executePing() {
        return true; // Simulated ping
    }
}
```

---

<p align="center">
  <b>Day 10 Complete! 🎉</b><br>
  Proceed to <b>Day 11</b>: <b>Dependency Injection In-Depth (@Qualifier, @Primary, and @ConfigurationProperties)</b>.<br>
  <a href="../Day_11_Dependency_Injection_In_Depth/Day_11_Dependency_Injection_In_Depth.md"><b>Continue to Day 11 →</b></a>
</p>
