# 💉 Day 11: Dependency Injection In-Depth
## Constructor vs Field Injection, @Qualifier, @Primary, and Multi-Environment Profiles

| ⬅️ Previous Day | 📚 Course Hub | ➡️ Next Day |
|:---|:---:|---:|
| [← Day 10: Spring IoC Container & Bean Lifecycle](../Day_10_Spring_IoC_Container_Bean_Lifecycle/Day_10_Spring_IoC_Container_Bean_Lifecycle.md) | [All 60 Days Overview](../../README.md) | [Day 12: Spring Boot Auto-Configuration Magic →](../Day_12_Spring_Boot_Auto_Configuration/Day_12_Spring_Boot_Auto_Configuration.md) |

[![Phase](https://img.shields.io/badge/Phase_02-Spring_Core_%26_DI-brightgreen.svg?style=for-the-badge)](../../README.md)
[![Day](https://img.shields.io/badge/Day-11_of_60-blue.svg?style=for-the-badge)](../../README.md)
[![Difficulty](https://img.shields.io/badge/Difficulty-Intermediate-blue.svg?style=for-the-badge)](../../README.md)
[![Topic](https://img.shields.io/badge/Spring_Core-Dependency_Injection-purple.svg?style=for-the-badge)](../../README.md)

---

![Spring Dependency Injection Methods and Bean Selection](assets/day11_constructor_vs_field_injection.jpg)

## 🗺️ Table of Contents
- [1. Topic Overview](#1-topic-overview)
- [2. Basic Foundations (True Zero)](#2-basic-foundations-true-zero)
  - [2.1 What is Constructor Injection, Field Injection, and Bean Ambiguity?](#21-what-is-constructor-injection-field-injection-and-bean-ambiguity)
  - [2.2 The Universal Power Strip & Voltage Selector Analogy](#22-the-universal-power-strip--voltage-selector-analogy)
  - [2.3 Minimal Working Example: Constructor Injection in Action](#23-minimal-working-example-constructor-injection-in-action)
  - [2.4 Line-by-Line Code Breakdown](#24-line-by-line-code-breakdown)
- [3. Core Concept Walkthrough (Basic → Intermediate)](#3-core-concept-walkthrough-basic--intermediate)
  - [3.1 The 3 DI Flavors: Constructor vs. Setter vs. Field Injection](#31-the-3-di-flavors-constructor-vs-setter-vs-field-injection)
  - [3.2 Resolving Multiple Beans: `@Primary` vs. `@Qualifier`](#32-resolving-multiple-beans-primary-vs-qualifier)
  - [3.3 Externalizing Configuration with `@Value`](#33-externalizing-configuration-with-value)
  - [3.4 Type-Safe Configuration Properties with Java Records](#34-type-safe-configuration-properties-with-java-records)
  - [3.5 Multi-Environment Deployment with Spring Profiles (`@Profile`)](#35-multi-environment-deployment-with-spring-profiles-profile)
  - [3.6 Building an Intelligent AI Model Router](#36-building-an-intelligent-ai-model-router)
- [4. Prerequisite & Supporting Concepts](#4-prerequisite--supporting-concepts)
  - [Prerequisite / Supporting Concept: YAML Configuration Syntax & Hierarchy](#prerequisite--supporting-concept-yaml-configuration-syntax--hierarchy)
  - [Prerequisite / Supporting Concept: Immutability (final fields) in Dependency Management](#prerequisite--supporting-concept-immutability-final-fields-in-dependency-management)
  - [Prerequisite / Supporting Concept: Spring Expression Language (SpEL) in @Value](#prerequisite--supporting-concept-spring-expression-language-spel-in-value)
- [5. Advanced Depth (Intermediate → Advanced)](#5-advanced-depth-intermediate--advanced)
  - [5.1 Senior Deep Dive: Dynamic Map Injection for Multi-Provider AI Systems](#51-senior-deep-dive-dynamic-map-injection-for-multi-provider-ai-systems)
  - [5.2 Custom Qualifier Annotations for Enterprise Safety](#52-custom-qualifier-annotations-for-enterprise-safety)
  - [5.3 Common Mistakes & Misconceptions (With Bad vs. Good Code)](#53-common-mistakes--misconceptions-with-bad-vs-good-code)
  - [5.4 Architectural Trade-Offs: Static Binding vs. Dynamic Routing](#54-architectural-trade-offs-static-binding-vs-dynamic-routing)
- [6. Quick Recap](#6-quick-recap)
- [7. Self-Check Questions & Practice Exercises](#7-self-check-questions--practice-exercises)
  - [Self-Check Questions (Basic to Advanced)](#self-check-questions-basic-to-advanced)
  - [Hands-On Practice Exercises with Full Solutions](#hands-on-practice-exercises-with-full-solutions)

---

# 1. Topic Overview

Dependency Injection in Spring encompasses multiple injection strategies (**Constructor**, **Setter**, and **Field**), bean disambiguation mechanisms (**`@Primary`** and **`@Qualifier`**), and configuration externalization (**`@Value`**, **`@ConfigurationProperties`**, and **`@Profile`**).

### Why This Topic Matters
Production AI platforms rarely rely on a single model. A cost-effective architecture uses small, free local models (e.g., Ollama running Llama-3.2) for simple tasks and high-reasoning cloud models (e.g., OpenAI GPT-4o) for complex multi-step reasoning. When an application configures multiple implementations of `ChatModel`, Spring requires explicit disambiguation rules. Furthermore, switching between local development on your laptop and cloud production environments must happen cleanly through environment profiles rather than code modifications.

> 💡 **New Word Alert — "Constructor Injection"**: Delivering all required dependencies into a class's constructor, ensuring instance fields can be declared `final` (immutable) and enabling simple unit testing without a running container.

> 💡 **New Word Alert — "Bean Ambiguity"**: A container startup conflict where multiple candidate beans implement the requested interface and Spring cannot infer which one to inject without explicit guidance.

> 💡 **New Word Alert — "@Primary"**: An annotation marking a bean as the default candidate when multiple beans of the same type exist.

> 💡 **New Word Alert — "@Qualifier"**: An annotation specifying the exact bean identifier name to inject, overriding any default `@Primary` designation.

---

# 2. Basic Foundations (True Zero)

Let's start from true zero, assuming you only know basic Java classes.

### 2.1 What is Constructor Injection, Field Injection, and Bean Ambiguity?

- **Constructor Injection**: Passing all required helper objects into a class through its constructor parameters: `public MyService(Helper h) { this.h = h; }`.
- **Field Injection**: Sticking `@Autowired` directly onto a private variable inside a class. While visually concise, it hides dependencies and makes testing painful.
- **Bean Ambiguity**: If you ask Spring for a `ChatModel`, but your project defines both `OpenAiChatModel` and `OllamaChatModel`, Spring stops and throws an error asking: *"Which one do you want?"*

---

### 2.2 The Universal Power Strip & Voltage Selector Analogy

```
                  ┌──────────────────────────────────────────────┐
                  │          UNIVERSAL WALL OUTLET (DI)          │
                  └──────────────────────┬───────────────────────┘
                                         │
                 ┌───────────────────────┴───────────────────────┐
                 ▼                                               ▼
         [ 110V Socket: USA ]                           [ 220V Socket: Europe ]
         (@Qualifier("usGrid"))                         (@Qualifier("euGrid"))
```

- If you plug in a generic device, the room routes power through the **default socket** (`@Primary`).
- If your device requires high voltage, you plug into the explicitly labeled socket (`@Qualifier("euGrid")`).
- When traveling between countries, you don't buy a new laptop—you just toggle the **country profile switch** (`@Profile("dev")` vs. `@Profile("prod")`).

---

### 2.3 Minimal Working Example: Constructor Injection in Action

Let's write a minimal, fully runnable Spring service utilizing clean constructor injection:

```java
package com.javagenai.day11;

import org.springframework.stereotype.Service;

public interface ModelService {
    String query(String prompt);
}

@Service
public class SimpleAssistant {
    private final ModelService model; // Immutable final field!

    // Constructor Injection (Spring 4.3+: @Autowired is 100% optional on single constructor!)
    public SimpleAssistant(ModelService model) {
        this.model = model;
    }

    public String ask(String question) {
        return model.query(question);
    }
}
```

---

### 2.4 Line-by-Line Code Breakdown

1. `private final ModelService model;`: Declares the dependency as an interface and marks it `final` for strict immutability.
2. `public SimpleAssistant(ModelService model)`: Single constructor accepting the dependency.
3. Spring automatically discovers this constructor and supplies the matching bean without requiring `@Autowired`.
4. In unit tests, you can instantiate this class in pure Java using `new SimpleAssistant(mockModel)` without booting the Spring container!

---

# 3. Core Concept Walkthrough (Basic → Intermediate)

Now let's examine injection flavors, disambiguation, and configuration management.

### 3.1 The 3 DI Flavors: Constructor vs. Setter vs. Field Injection

| Injection Style | Code Structure | Pros | Cons | Verdict |
| :--- | :--- | :--- | :--- | :--- |
| **Constructor** | `public Service(Dependency d)` | **`final` fields, easy unit testing, fails fast at startup** | Verbose with 5+ parameters (refactor code!) | **Industry Standard (100% Recommended)** |
| **Setter** | `public void setDep(Dependency d)` | Allows optional dependencies or circular fixes | Mutable fields, object may exist in half-initialized state | **Use only for optional dependencies** |
| **Field** | `@Autowired private Dep d;` | Short, concise code | **Cannot be `final`, hard to test without Spring reflection** | **Discouraged Anti-Pattern** |

---

### 3.2 Resolving Multiple Beans: `@Primary` vs. `@Qualifier`

Suppose your AI platform configures two distinct `ChatModel` beans:

```java
package com.javagenai.day11;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AIModelConfiguration {

    @Bean
    @Primary // Default choice whenever ChatModel is requested without a qualifier
    public ChatModel openAiChatModel() {
        return new OpenAiChatModel("sk-prod-key");
    }

    @Bean
    public ChatModel ollamaChatModel() {
        return new OllamaChatModel("http://localhost:11434");
    }
}
```

#### Scenario A: Using the Default Bean (`@Primary`):
```java
@Service
public class GeneralChatService {
    private final ChatModel chatModel;

    // Receives openAiChatModel because it is marked @Primary!
    public GeneralChatService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }
}
```

#### Scenario B: Explicit Targeting with `@Qualifier`:
```java
@Service
public class BackgroundBatchSummarizer {
    private final ChatModel localModel;

    // Overrides @Primary and explicitly requests the Ollama bean!
    public BackgroundBatchSummarizer(@Qualifier("ollamaChatModel") ChatModel localModel) {
        this.localModel = localModel;
    }

    public void processNightlyBatch() {
        localModel.call("Summarize logs");
    }
}
```

---

### 3.3 Externalizing Configuration with `@Value`

Hardcoding secrets in code is a security vulnerability. Use `@Value` for scalar configuration:

```yaml
# application.yml
ai:
  openai:
    api-key: ${OPENAI_API_KEY:default-dev-key}
    temperature: 0.7
```

```java
@Component
public class OpenAiGateway {

    @Value("${ai.openai.api-key}")
    private String apiKey;

    @Value("${ai.openai.temperature:0.2}") // 0.2 is fallback default
    private double temperature;
}
```

---

### 3.4 Type-Safe Configuration Properties with Java Records

For complex hierarchical configurations, `@Value` becomes unmanageable. Modern Spring Boot allows binding YAML blocks directly into **Java Records**:

```yaml
app:
  ai:
    model-name: gpt-4o
    max-tokens: 4096
    temperature: 0.5
    streaming-enabled: true
```

```java
package com.javagenai.day11;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public record AIModelProperties(
    String modelName,
    int maxTokens,
    double temperature,
    boolean streamingEnabled
) {
    // Compact constructor validation!
    public AIModelProperties {
        if (temperature < 0.0 || temperature > 2.0) {
            throw new IllegalArgumentException("Invalid temperature: " + temperature);
        }
    }
}
```

---

### 3.5 Multi-Environment Deployment with Spring Profiles (`@Profile`)

Switch between free local development and cloud production without touching Java code:

```java
@Configuration
@Profile("dev")
public class DevAIConfig {
    @Bean
    public ChatModel chatModel() {
        System.out.println("[CONFIG] DEV mode active: Connecting to local Ollama (Free)...");
        return new OllamaChatModel("http://localhost:11434");
    }
}
```

```java
@Configuration
@Profile("prod")
public class ProdAIConfig {
    @Bean
    public ChatModel chatModel(@Value("${ai.openai.api-key}") String apiKey) {
        System.out.println("[CONFIG] PROD mode active: Connecting to OpenAI Enterprise...");
        return new OpenAiChatModel(apiKey);
    }
}
```

Activate via `application.yml` (`spring.profiles.active: dev`) or command-line:
`java -jar app.jar --spring.profiles.active=prod`.

---

### 3.6 Building an Intelligent AI Model Router

```java
package com.javagenai.day11;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class AIModelRouter {
    private final ChatModel fastModel;
    private final ChatModel deepModel;

    public AIModelRouter(
        @Qualifier("fastModel") ChatModel fastModel,
        @Qualifier("deepModel") ChatModel deepModel
    ) {
        this.fastModel = fastModel;
        this.deepModel = deepModel;
    }

    public String routeAndExecute(String prompt) {
        if (prompt.length() < 100) {
            System.out.println("[ROUTER]: Routing short query to fast local model.");
            return fastModel.call(prompt);
        } else {
            System.out.println("[ROUTER]: Routing complex query to deep reasoning model.");
            return deepModel.call(prompt);
        }
    }
}
```

---

# 4. Prerequisite & Supporting Concepts

### Prerequisite / Supporting Concept: YAML Configuration Syntax & Hierarchy

YAML uses 2-space indentation to represent nested key-value pairs:
- `app.ai.model-name` maps to nested blocks in YAML.
- Environment variables can be referenced via `${ENV_VAR:defaultValue}` syntax.

---

### Prerequisite / Supporting Concept: Immutability (final fields) in Dependency Management

Marking dependencies `final` guarantees that once Spring injects the reference during construction, it can never be reassigned or set to `null` by any method, preventing subtle multi-threading defects.

---

### Prerequisite / Supporting Concept: Spring Expression Language (SpEL) in @Value

`@Value` supports SpEL expressions enclosed in `#{...}` for runtime arithmetic or method invocations (e.g., `@Value("#{systemProperties['user.home']}")`).

---

# 5. Advanced Depth (Intermediate → Advanced)

### 5.1 Senior Deep Dive: Dynamic Map Injection for Multi-Provider AI Systems

Instead of hardcoding qualifiers for 5 different AI providers, Spring allows injecting a **Map of all beans implementing an interface**:

```java
@Service
public class MultiProviderRouter {
    // Spring automatically injects ALL ChatModel beans keyed by their bean names!
    private final Map<String, ChatModel> modelsByName;

    public MultiProviderRouter(Map<String, ChatModel> modelsByName) {
        this.modelsByName = modelsByName;
    }

    public String executeOnProvider(String providerName, String prompt) {
        ChatModel model = modelsByName.get(providerName.toLowerCase() + "ChatModel");
        if (model == null) {
            throw new IllegalArgumentException("Unknown AI provider: " + providerName);
        }
        return model.call(prompt);
    }
}
```

---

### 5.2 Custom Qualifier Annotations for Enterprise Safety

Instead of string-based qualifiers prone to typos (`@Qualifier("openAiChatModel")`), senior architects create custom meta-annotations:

```java
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Qualifier
public @interface CloudAI {}
```

Usage: `@CloudAI` placed directly on both the `@Bean` definition and the constructor parameter!

---

### 5.3 Common Mistakes & Misconceptions (With Bad vs. Good Code)

#### Mistake 1: Multiple Beans Without `@Primary` or `@Qualifier`
**Result**: Application terminates at startup with `NoUniqueBeanDefinitionException`.
**Fix**: Mark one bean `@Primary` or specify `@Qualifier("exactName")` at injection points.

#### Mistake 2: Storing Sensitive API Keys in Git
**Bad Practice**: Hardcoding `api-key: sk-proj-12345` inside `application.yml`.
**Correct Practice**: Use environment variable substitution: `api-key: ${OPENAI_API_KEY}`.

---

### 5.4 Architectural Trade-Offs: Static Binding vs. Dynamic Routing

- **Static Qualifier Binding**: Compile-time clear, visible in IDE dependency graphs.
- **Dynamic Map Injection**: Highly extensible (new model beans are added without altering router code), but requires runtime validation.

---

# 6. Quick Recap

| Annotation / Concept | Purpose |
| :--- | :--- |
| **Constructor Injection**| Enterprise gold standard: guarantees immutability and easy testing. |
| **Field Injection** | Discouraged anti-pattern using `@Autowired` on private fields. |
| **`@Primary`** | Marks a bean as the default when multiple matching types exist. |
| **`@Qualifier("name")`** | Selects a specific bean by name, overriding `@Primary`. |
| **`@Value("${prop}")`** | Injects scalar configuration values from YAML or environment. |
| **`@ConfigurationProperties`**| Binds structured YAML hierarchies into immutable Java Records. |
| **`@Profile("dev/prod")`** | Selectively activates beans based on active runtime environment. |

---

# 7. Self-Check Questions & Practice Exercises

### Self-Check Questions (Basic to Advanced)

1. **Why is Constructor Injection superior to Field Injection?**
   - *Answer*: It enables field immutability (`final`), makes dependencies explicit, prevents incomplete object construction, and allows straightforward unit testing without Spring or reflection.
2. **What exception does Spring throw if two beans match a dependency type and neither is marked `@Primary`?**
   - *Answer*: `NoUniqueBeanDefinitionException`.
3. **What is the difference between `@Primary` and `@Qualifier`?**
   - *Answer*: `@Primary` is placed on a bean definition to establish it as the default choice when no qualifier is specified. `@Qualifier("name")` is placed at the injection point to explicitly select a specific named bean, overriding `@Primary`.
4. **How do you bind configuration properties to a Java Record?**
   - *Answer*: Place `@ConfigurationProperties(prefix = "...")` on the record declaration and enable it via `@EnableConfigurationProperties(MyRecord.class)` or `@ConfigurationPropertiesScan`.
5. **How does `@Profile("prod")` protect company costs in development?**
   - *Answer*: It prevents expensive cloud API beans (like paid OpenAI/Anthropic models) from being instantiated during local development or CI/CD pipelines, substituting free local mocks or Ollama instances instead.

---

### Hands-On Practice Exercises with Full Solutions

#### 🏋️ Exercise 1: Build an Intelligent AI Model Router
**Objective**: Build an `AIModelRouter` service that accepts both a `@Qualifier("fastModel")` and a `@Qualifier("deepModel")`, routing short prompts ($< 100$ characters) to `fastModel` and long prompts to `deepModel`.

```java
package com.javagenai.day11;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class AIModelRouter {
    private final ChatModel fastModel;
    private final ChatModel deepModel;

    public AIModelRouter(
        @Qualifier("fastModel") ChatModel fastModel,
        @Qualifier("deepModel") ChatModel deepModel
    ) {
        this.fastModel = fastModel;
        this.deepModel = deepModel;
    }

    public String routeAndExecute(String prompt) {
        if (prompt.length() < 100) {
            System.out.println("[ROUTER]: Prompt is short. Routing to fast lightweight model.");
            return fastModel.call(prompt);
        } else {
            System.out.println("[ROUTER]: Prompt is long/complex. Routing to deep reasoning model.");
            return deepModel.call(prompt);
        }
    }
}
```

---

#### 🏋️ Exercise 2: Type-Safe Vector Store Configuration Record
**Objective**: Create a record `VectorStoreProperties` bound to prefix `app.vectorstore` containing: `String host`, `int port`, `String indexName`, `int vectorDimensions`, validating that `vectorDimensions` is positive.

```java
package com.javagenai.day11;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.vectorstore")
public record VectorStoreProperties(
    String host,
    int port,
    String indexName,
    int vectorDimensions
) {
    public VectorStoreProperties {
        if (vectorDimensions <= 0) {
            throw new IllegalArgumentException("Vector dimensions must be > 0. Received: " + vectorDimensions);
        }
        if (host == null || host.isBlank()) {
            host = "localhost";
        }
    }
}
```

---

<p align="center">
  <b>Day 11 Complete! 🎉</b><br>
  Proceed to <b>Day 12</b>: <b>Spring Boot Auto-Configuration Magic</b>.<br>
  <a href="../Day_12_Spring_Boot_Auto_Configuration/Day_12_Spring_Boot_Auto_Configuration.md"><b>Continue to Day 12 →</b></a>
</p>
