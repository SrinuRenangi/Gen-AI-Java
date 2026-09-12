# 🌐 Day 15: HTTP Deep Dive & First REST Controller
## GET, POST, PUT, DELETE — Building a Production Prompt Library CRUD API

| ⬅️ Previous Day | 📚 Course Hub | ➡️ Next Day |
|:---|:---:|---:|
| [← Day 14: Spring Boot Actuator & Production Readiness](../../Phase_02_Spring_Core_and_DI/Day_14_Actuator_Production_Readiness/Day_14_Actuator_Production_Readiness.md) | [All 60 Days Overview](../../README.md) | [Day 16: Request Validation, DTOs & Response Design →](../Day_16_Validation_DTOs_Response_Design/Day_16_Validation_DTOs_Response_Design.md) |

[![Phase](https://img.shields.io/badge/Phase_03-Spring_Web_REST_APIs-yellow.svg?style=for-the-badge)](../../README.md)
[![Day](https://img.shields.io/badge/Day-15_of_60-blue.svg?style=for-the-badge)](../../README.md)
[![Difficulty](https://img.shields.io/badge/Difficulty-Intermediate-blue.svg?style=for-the-badge)](../../README.md)
[![Topic](https://img.shields.io/badge/Spring_Web-REST_Controllers-orange.svg?style=for-the-badge)](../../README.md)

---

## 1. Topic Overview

Spring Web REST Controllers expose HTTP endpoints that enable external frontends, mobile devices, and microservices to communicate with enterprise Java applications using standard JSON payloads over network sockets. In enterprise Generative AI engineering, REST controllers form the primary API boundary where client prompts are received, system prompt templates are dynamically managed via CRUD operations, and downstream model outputs are serialized and returned to users.

---

## 2. Basic Foundations (True Zero)

### Plain English Definitions
- **REST API (Representational State Transfer)**: An architectural standard for computer systems communicating over HTTP, where resources (like prompt templates or chat conversations) are represented as clean URLs and manipulated using standard web actions.
- **HTTP Verb**: The action keyword in an HTTP request indicating what operation to perform: `GET` (read data), `POST` (create new resource/submit prompt), `PUT` (replace existing resource), and `DELETE` (remove resource).
- **Idempotency**: An operation property where executing the identical request multiple times produces the exact same server state as executing it once (e.g., calling `DELETE /prompts/12` ten times leaves item 12 deleted without further state change).
- **`@RestController`**: A Spring Web annotation combining `@Controller` and `@ResponseBody`, instructing Spring to automatically serialize Java return values (Records, Lists, Maps) directly into JSON HTTP response bodies rather than rendering HTML web templates.
- **DispatcherServlet**: The front-controller servlet at the heart of Spring MVC that intercepts all incoming network requests and routes them to the appropriate controller method.

### Relatable Physical Analogy: The Postal Mail Order Desk
```
                     THE REST API POSTAL DESK
┌─────────────────────────────────────────────────────────────────────────────┐
│ 1. The Customer (Web Client / Python Script) sends a letter:               │
│    • Envelope Destination: "https://api.myai.com/v1/prompts/42" (URI)        │
│    • Action Stamp: "GET" (Please read and deliver prompt #42)               │
│    • Headers: "Accept: application/json" (Please write response in JSON)    │
│                                                                             │
│ 2. The Mail Clerk (Spring DispatcherServlet) inspects the letter:           │
│    • Matches URL pattern to PromptController.getPromptById(id)              │
│    • Delivers parsed parameters to the method.                              │
│                                                                             │
│ 3. The Clerk packages a return parcel:                                      │
│    • Status Code: "200 OK" (Success)                                        │
│    • Content-Type: "application/json"                                       │
│    • Parcel Body: { "id": "42", "text": "You are a code reviewer..." }      │
└─────────────────────────────────────────────────────────────────────────────┘
```

A REST API is simply a standardized postal service governing communication over TCP/IP internet sockets.

### Minimal Beginner-Friendly Working Code Example

Let us examine a minimal working REST Controller that returns a static AI greeting in JSON format:

```java
package com.javagenai.day15;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@SpringBootApplication
@RestController
@RequestMapping("/api/v1/hello")
public class MinimalRestControllerApp {

    public static void main(String[] args) {
        SpringApplication.run(MinimalRestControllerApp.class, args);
    }

    @GetMapping
    public Map<String, String> sayHello() {
        return Map.of(
            "service", "Enterprise AI Gateway",
            "status", "ONLINE",
            "message", "Welcome to Spring Web REST APIs!"
        );
    }
}
```

#### Line-by-Line Walkthrough
1. `@RestController`: Marks this class as a REST endpoint handler. Spring will convert the returned `Map` into JSON automatically.
2. `@RequestMapping("/api/v1/hello")`: Sets the base URL path prefix for all endpoints inside this class.
3. `@GetMapping`: Maps HTTP `GET` requests sent to `/api/v1/hello` directly to the `sayHello()` method.
4. `Map.of(...)`: Returns a simple Java key-value map.
5. Jackson (Spring Boot's JSON serializer) converts this map into: `{"service":"Enterprise AI Gateway","status":"ONLINE","message":"Welcome..."}` with HTTP Status `200 OK`.

---

## 3. Core Concept Walkthrough (Basic → Intermediate)

### 3.1 HTTP Protocol Anatomy
An HTTP communication consists of an explicit request and response format:

```
┌────────────────────────────────────────────────────────┐
│                      HTTP REQUEST                      │
├────────────────────────────────────────────────────────┤
│ POST /api/v1/prompts HTTP/1.1                          │  ◄── Method + Path + Protocol
│ Host: api.enterprise-ai.com                            │  ◄── Headers
│ Content-Type: application/json                         │
│ Authorization: Bearer eyJhbGciOi...                    │
│                                                        │
│ {                                                      │  ◄── Request Body (Payload)
│   "title": "Code Reviewer",                            │
│   "template": "Analyze this Java code for bugs: {code}"│
│ }                                                      │
└────────────────────────────────────────────────────────┘
```

### 3.2 HTTP Verbs & Idempotency Matrix

| Verb | Enterprise AI Purpose | Idempotent? | Safe? (Read-Only) |
| :--- | :--- | :---: | :---: |
| **`GET`** | Read prompt templates, fetch embedding status | **YES** | **YES** |
| **`POST`** | Generate inference, ingest documents, create prompts | **NO** (Billed tokens each call) | **NO** |
| **`PUT`** | Completely replace an existing prompt template | **YES** | **NO** |
| **`PATCH`** | Partially update template fields (e.g. temperature) | **NO** | **NO** |
| **`DELETE`** | Remove outdated prompt template or vector record | **YES** | **NO** |

### 3.3 Status Codes in Enterprise AI

| Code | Standard Name | Enterprise AI Application |
| :---: | :--- | :--- |
| **`200 OK`** | Success | Query succeeded; returns requested prompt or model list. |
| **`201 Created`** | Created | Successfully created a new prompt template or document chunk. |
| **`204 No Content`**| No Content | Successfully deleted a prompt resource; response body is empty. |
| **`400 Bad Request`**| Bad Request | Malformed JSON or missing required prompt variables. |
| **`404 Not Found`**| Not Found | Prompt template ID does not exist in the database. |
| **`429 Rate Limited`**| Too Many Requests | User has exceeded their allocated tokens-per-minute quota. |
| **`500 Internal Error`**| Server Error | Unhandled exception in backend Java code. |
| **`503 Unavailable`**| Service Down | External LLM provider (OpenAI / Anthropic) is currently unreachable. |

### 3.4 Request Parameter Extraction Annotations
Spring provides three primary annotations to pull parameters from incoming HTTP requests:

```
1. @PathVariable:  GET /api/v1/prompts/{id}
   Extracts identifier directly from URI path segments.

2. @RequestParam:   GET /api/v1/prompts?category=finance&limit=10
   Extracts query string parameters used for filtering, pagination, or sorting.

3. @RequestBody:    POST /api/v1/prompts (JSON Payload)
   Deserializes HTTP body JSON into a typed Java Record or DTO using Jackson.
```

### 3.5 Complete Hands-On CRUD Project: The Prompt Library API

#### 1. The Model: `PromptTemplate.java`
```java
package com.javagenai.day15.model;

import java.time.Instant;

public record PromptTemplate(
    String id,
    String title,
    String category,
    String templateText,
    double defaultTemperature,
    Instant createdAt
) {}
```

#### 2. The Controller: `PromptController.java`
```java
package com.javagenai.day15.controller;

import com.javagenai.day15.model.PromptTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/prompts")
public class PromptController {

    private final Map<String, PromptTemplate> promptStore = new ConcurrentHashMap<>();

    public PromptController() {
        // Pre-seed a default prompt template
        String id = "prompt-001";
        promptStore.put(id, new PromptTemplate(
            id, "Senior Java Reviewer", "Engineering",
            "Review this Java code for concurrency leaks: {code}",
            0.2, Instant.now()
        ));
    }

    // 1. GET ALL (with optional category filtering)
    @GetMapping
    public List<PromptTemplate> getAllPrompts(@RequestParam(required = false) String category) {
        if (category == null || category.isBlank()) {
            return new ArrayList<>(promptStore.values());
        }
        return promptStore.values().stream()
            .filter(p -> category.equalsIgnoreCase(p.category()))
            .toList();
    }

    // 2. GET BY ID
    @GetMapping("/{id}")
    public ResponseEntity<PromptTemplate> getPromptById(@PathVariable String id) {
        PromptTemplate prompt = promptStore.get(id);
        if (prompt == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 404 Not Found
        }
        return ResponseEntity.ok(prompt); // 200 OK
    }

    // 3. CREATE (POST)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // 201 Created
    public PromptTemplate createPrompt(@RequestBody PromptTemplate request) {
        String newId = "prompt-" + UUID.randomUUID().toString().substring(0, 8);
        PromptTemplate saved = new PromptTemplate(
            newId, request.title(), request.category(),
            request.templateText(), request.defaultTemperature(), Instant.now()
        );
        promptStore.put(newId, saved);
        return saved;
    }

    // 4. UPDATE (PUT)
    @PutMapping("/{id}")
    public ResponseEntity<PromptTemplate> updatePrompt(
        @PathVariable String id,
        @RequestBody PromptTemplate updateRequest
    ) {
        if (!promptStore.containsKey(id)) {
            return ResponseEntity.notFound().build();
        }
        PromptTemplate updated = new PromptTemplate(
            id, updateRequest.title(), updateRequest.category(),
            updateRequest.templateText(), updateRequest.defaultTemperature(), Instant.now()
        );
        promptStore.put(id, updated);
        return ResponseEntity.ok(updated);
    }

    // 5. DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrompt(@PathVariable String id) {
        if (promptStore.remove(id) == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}
```

---

## 4. Prerequisite & Supporting Concepts

### Prerequisite / Supporting Concept: The `DispatcherServlet` Execution Pipeline
```
 1. Client HTTP Request ──► [ DispatcherServlet ]
                                  │
                                  ├── 2. Queries HandlerMapping ("Who handles /prompts?")
                                  │   └── Locates: PromptController.getAllPrompts()
                                  │
                                  ├── 3. Executes HandlerAdapter (Invokes method with args)
                                  │   └── Jackson translates JSON <-> Java Record!
                                  │
                                  └── 4. Writes Serialized JSON Response to Client
```

### Prerequisite / Supporting Concept: The Plain English Bridge to Spring Web

| Concept | Under the Hood | Plain English Translation |
| :--- | :--- | :--- |
| **`DispatcherServlet`** | Front-controller servlet mapped to `/`. Catches every request. | The lobby receptionist greeting visitors and directing them to the correct office. |
| **`@RestController`** | Combines `@Controller` + `@ResponseBody`. | *"Return pure data (JSON), never render an HTML web template."* |
| **`@GetMapping`** | Read-only operation; safe and idempotent. | *"Give me data."* (e.g. read prompt history). |
| **`@PostMapping`** | Mutation operation; non-idempotent. | *"Create something new."* (e.g. invoke LLM inference). |
| **`@PathVariable`** | Extracts `/prompts/{id}` from URI path. | Reading the room number on a door in a hallway. |
| **`@RequestBody`** | Deserializes HTTP JSON into Java objects. | Opening a package envelope and translating the letter into Java fields. |

---

## 5. Advanced Depth (Intermediate → Advanced)

### 5.1 Common Mistakes & Misconceptions

#### Mistake 1: Using `@Controller` Instead of `@RestController`
```java
// ❌ BAD: Spring treats return value as a View Name (looks for "prompts.html")!
@Controller
@RequestMapping("/api/prompts")
public class BadPromptController {
    @GetMapping
    public List<String> getPrompts() { return List.of("p1", "p2"); } // Throws 404 View Not Found!
}

// ✅ GOOD: Returns serialized JSON data directly to the HTTP response stream
@RestController
@RequestMapping("/api/prompts")
public class GoodPromptController {
    @GetMapping
    public List<String> getPrompts() { return List.of("p1", "p2"); } // Returns ["p1", "p2"]
}
```

#### Mistake 2: Returning `200 OK` When Resource Creation Succeeded
According to HTTP REST RFC standards, creating a resource with `POST` should return `201 Created` rather than `200 OK`:
```java
// ❌ Sub-optimal REST design
@PostMapping
public PromptTemplate create(@RequestBody PromptTemplate req) { ... } // Defaults to 200 OK

// ✅ Correct Enterprise REST standard
@PostMapping
@ResponseStatus(HttpStatus.CREATED) // Explicitly returns 201 Created
public PromptTemplate create(@RequestBody PromptTemplate req) { ... }
```

#### Mistake 3: Returning Raw `null` for Missing Entities
Never return `null` from a controller method when an entity is absent; Spring Boot will send an empty `200 OK` body, confusing frontends. Always return a `ResponseEntity.notFound().build()` (`404 Not Found`).

---

## 6. Quick Recap

| Concept | Spring Annotation | HTTP Semantics | Enterprise AI Application |
| :--- | :--- | :--- | :--- |
| **Base Routing** | `@RequestMapping("/...")` | Base path prefix | Namespaces API versions (e.g., `/api/v1/ai`) |
| **Read Entity** | `@GetMapping` | Safe, Idempotent (`200 OK`) | Fetching prompt templates or conversation logs |
| **Create Entity** | `@PostMapping` | Non-idempotent (`201 Created`) | Triggering AI text generation, uploading docs |
| **Update Entity** | `@PutMapping` | Idempotent (`200 OK`) | Overwriting prompt templates with new versions |
| **Delete Entity** | `@DeleteMapping` | Idempotent (`204 No Content`) | Purging prompt templates or vector records |
| **Path Variable** | `@PathVariable` | URL Segment | Identifying entity ID in `/prompts/{id}` |
| **JSON Payload** | `@RequestBody` | Request Body | Ingesting prompt configuration objects |

---

## 7. Self-Check Questions & Practice Exercises

### Self-Check Questions

1. **What is the difference between `@Controller` and `@RestController` in Spring Boot?**
   - *Answer*: `@Controller` is intended for traditional web applications where method return values represent view template names (like Thymeleaf HTML). `@RestController` combines `@Controller` and `@ResponseBody`, instructing Spring to serialize returned Java objects directly into JSON/XML payloads.
2. **Why is `GET` considered safe and idempotent, while `POST` is neither?**
   - *Answer*: `GET` is safe because it only reads data without modifying server state. It is idempotent because repeated calls produce the same state. `POST` creates resources or triggers side-effects (such as billing tokens on every inference call), changing server state with each invocation.
3. **When should you choose `@PathVariable` over `@RequestParam`?**
   - *Answer*: Use `@PathVariable` when a parameter uniquely identifies a resource as part of the URI path (e.g., `/prompts/prompt-101`). Use `@RequestParam` for filtering, sorting, or pagination queries in the query string (e.g., `/prompts?category=support&page=2`).
4. **What HTTP status code must be returned upon successful deletion of a resource?**
   - *Answer*: `204 No Content` (indicating the request was successfully processed and no body content is returned).
5. **What role does the `DispatcherServlet` fulfill in the Spring Web architecture?**
   - *Answer*: It acts as the Front Controller, catching every incoming HTTP request, mapping it to the appropriate controller method using `HandlerMapping`, and dispatching execution through `HandlerAdapter`.

---

### Hands-On Practice Exercises

#### 🏋️ Exercise 1: Build a Prompt Render Endpoint
**Objective**: Implement an endpoint `POST /api/v1/prompts/{id}/render` that accepts a `Map<String, String> variables` in the JSON request body, substitutes `{variable_name}` placeholders in the template text with user-supplied values, and returns the rendered prompt string.

```java
package com.javagenai.day15;

import com.javagenai.day15.model.PromptTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/prompts")
public class PromptRenderController {

    @PostMapping("/{id}/render")
    public ResponseEntity<String> renderPrompt(
        @PathVariable String id,
        @RequestBody Map<String, String> variables
    ) {
        // Simulated prompt lookup
        PromptTemplate template = new PromptTemplate(
            id, "Customer Support", "Support",
            "Hello {customer_name}, your order #{order_id} is currently {status}.",
            0.5, null
        );

        String renderedText = template.templateText();
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            renderedText = renderedText.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        return ResponseEntity.ok(renderedText);
    }
}
```

#### 🏋️ Exercise 2: Build a Model Temperature Tuning Endpoint
**Objective**: Build a `PATCH /api/v1/prompts/{id}/temperature` endpoint that updates only the `defaultTemperature` of a prompt template without requiring the entire prompt object in the request body.

```java
package com.javagenai.day15;

import com.javagenai.day15.model.PromptTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/prompts")
public class PromptTemperaturePatchController {

    private final Map<String, PromptTemplate> store = new ConcurrentHashMap<>();

    public record TemperatureUpdateRequest(double temperature) {}

    @PatchMapping("/{id}/temperature")
    public ResponseEntity<PromptTemplate> updateTemperature(
        @PathVariable String id,
        @RequestBody TemperatureUpdateRequest req
    ) {
        PromptTemplate existing = store.get(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        if (req.temperature() < 0.0 || req.temperature() > 2.0) {
            return ResponseEntity.badRequest().build(); // 400 Bad Request
        }

        PromptTemplate updated = new PromptTemplate(
            existing.id(), existing.title(), existing.category(),
            existing.templateText(), req.temperature(), Instant.now()
        );
        store.put(id, updated);

        return ResponseEntity.ok(updated);
    }
}
```

---

| ⬅️ Previous Day | 📚 Course Hub | ➡️ Next Day |
|:---|:---:|---:|
| [← Day 14: Spring Boot Actuator & Production Readiness](../../Phase_02_Spring_Core_and_DI/Day_14_Actuator_Production_Readiness/Day_14_Actuator_Production_Readiness.md) | [All 60 Days Overview](../../README.md) | [Day 16: Request Validation, DTOs & Response Design →](../Day_16_Validation_DTOs_Response_Design/Day_16_Validation_DTOs_Response_Design.md) |
