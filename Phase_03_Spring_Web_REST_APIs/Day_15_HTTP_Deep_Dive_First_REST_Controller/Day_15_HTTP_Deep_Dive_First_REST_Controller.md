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

## 📌 What Will You Learn Today?

Welcome to **Phase 3: Spring Web — Building Production REST APIs**!

You now understand core Java 21, Spring IoC, Dependency Injection, and AOP. But your AI system cannot live inside a command-line script—it must be exposed to the world so web browsers, mobile apps, frontend developers, and enterprise microservices can interact with your models.

Every ChatGPT interface, customer support widget, and AI agent communicates over **HTTP REST APIs**.

Today, we dive into the HTTP protocol from the ground up and build our first production-grade REST API: **A Prompt Library Management Service** that allows AI teams to store, version, retrieve, and delete system prompt templates.

By the end of today, you will master:
- ✅ **The HTTP Protocol**: Request-Response lifecycle, Headers, Verbs, and Status Codes.
- ✅ **Idempotency**: Why `GET`, `PUT`, and `DELETE` are idempotent, but `POST` is not.
- ✅ **`@RestController` vs. `@Controller`**: Why modern APIs use `@RestController`.
- ✅ **URL Mapping Annotations**: `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`.
- ✅ **Extracting Request Data**: `@PathVariable`, `@RequestParam`, `@RequestBody`, `@RequestHeader`.
- ✅ **Hands-on Capstone**: Building an in-memory **Prompt Library CRUD REST API**.

---

## 🗺️ Table of Contents

- [1. Real-World Analogy: The Postal Mail Order Desk](#1-real-world-analogy-the-postal-mail-order-desk)
- [2. The HTTP Protocol Deep Dive](#2-the-http-protocol-deep-dive)
  - [2.1 The HTTP Request & Response Anatomy](#21-the-http-request--response-anatomy)
  - [2.2 HTTP Verbs & The Concept of Idempotency](#22-http-verbs--the-concept-of-idempotency)
  - [2.3 Enterprise Status Codes: Speaking the Universal Language](#23-enterprise-status-codes-speaking-the-universal-language)
- [3. Spring Web Architecture: The `DispatcherServlet`](#3-spring-web-architecture-the-dispatcherservlet)
- [4. Building Your First REST Controller](#4-building-your-first-rest-controller)
  - [4.1 `@RestController` and `@ResponseBody`](#41-restcontroller-and-responsebody)
  - [4.2 Handling `@PathVariable` vs. `@RequestParam`](#42-handling-pathvariable-vs-requestparam)
  - [4.3 Consuming JSON with `@RequestBody`](#43-consuming-json-with-requestbody)
- [5. Hands-on Project: The Prompt Library CRUD API](#5-hands-on-project-the-prompt-library-crud-api)
  - [5.1 The `PromptTemplate` Model](#51-the-prompttemplate-model)
  - [5.2 The `PromptController`](#52-the-promptcontroller)
  - [5.3 Testing with cURL & HTTPie](#53-testing-with-curl--httpie)
- [6. Key Takeaways & Summary](#6-key-takeaways--summary)
- [7. Practice Exercises & Full Solutions](#7-practice-exercises--full-solutions)
- [8. Self-Check Quiz](#8-self-check-quiz)

---

# 1. Real-World Analogy: The Postal Mail Order Desk

```
                     THE REST API POSTAL DESK
┌─────────────────────────────────────────────────────────────────────────────┐
│ 1. The Customer (Web Browser / Python Client) writes a letter:              │
│    • Envelope Address: "https://api.myai.com/v1/prompts/42" (URI)           │
│    • Action Stamp: "GET" (Please read and reply with prompt #42)            │
│    • Headers: "Accept: application/json" (Please reply in English/JSON)     │
│                                                                             │
│ 2. The Mail Clerk (Spring DispatcherServlet) receives the letter:          │
│    • Looks up who handles "/v1/prompts/{id}" (HandlerMapping)               │
│    • Delivers it to the PromptController desk.                              │
│                                                                             │
│ 3. The Clerk replies with a return parcel:                                  │
│    • Status Code: "200 OK"                                                  │
│    • Content-Type: "application/json"                                       │
│    • Body: { "id": "42", "text": "You are an expert AI..." }                │
└─────────────────────────────────────────────────────────────────────────────┘
```

A REST API is simply a standardized set of postal conventions over TCP/IP sockets.

---

# 2. The HTTP Protocol Deep Dive

### 2.1 The HTTP Request & Response Anatomy

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

---

### 2.2 HTTP Verbs & The Concept of Idempotency

**Idempotency** means: *If you execute the identical operation 1 time or 1,000 times, the resulting state on the server is exactly the same.*

| Verb | Purpose in AI Services | Idempotent? | Safe? |
| :--- | :--- | :---: | :---: |
| **`GET`** | Retrieve prompt templates, list models, fetch embeddings | **YES** | **YES** (Read-only) |
| **`POST`** | Submit user chat prompt to LLM, create new prompt template | **NO** (Every call creates/bills new tokens!) | **NO** |
| **`PUT`** | Completely replace an existing prompt template | **YES** | **NO** |
| **`PATCH`** | Partially update a field (e.g. modify temperature only) | **NO** | **NO** |
| **`DELETE`** | Delete an outdated prompt or document chunk | **YES** | **NO** |

---

### 2.3 Enterprise Status Codes

| Code | Name | When to Use in AI Applications |
| :---: | :--- | :--- |
| **`200 OK`** | Success | Returned on successful `GET`, `PUT`, or `POST` queries. |
| **`201 Created`** | Created | Returned when a new prompt template or document is saved. |
| **`204 No Content`** | No Content | Returned on successful `DELETE` (no body returned). |
| **`400 Bad Request`** | Bad Request | Missing required prompt parameters, malformed JSON. |
| **`404 Not Found`** | Not Found | Requested prompt ID or Vector index does not exist. |
| **`429 Too Many Requests`** | Rate Limited | User exceeded their allowed tokens-per-minute quota. |
| **`500 Internal Error`** | Server Error | Unhandled runtime exception in Java code. |
| **`503 Unavailable`** | Service Down | Downstream LLM (OpenAI / Claude / Ollama) is unreachable. |

---

# 3. Spring Web Architecture: The `DispatcherServlet`

How does an incoming HTTP network packet reach your Java method?

Through the **Front Controller Pattern (`DispatcherServlet`)**:

```
 1. Client HTTP Request ──► [ DispatcherServlet ]
                                  │
                                  ├── 2. Queries HandlerMapping ("Who handles /prompts?")
                                  │   └── Finds: PromptController.getAllPrompts()
                                  │
                                  ├── 3. Executes HandlerAdapter (Invokes method with args)
                                  │   └── Jackson converts JSON <-> Java Record!
                                  │
                                  └── 4. Returns JSON Response to Client
```

---

# 4. Building Your First REST Controller

### 4.1 `@RestController` and `@ResponseBody`

In traditional Spring MVC (for rendering HTML websites), `@Controller` methods return the name of a web page template (like `index.html`).

In modern REST APIs, we annotate the class with **`@RestController`**:
- `@RestController` = `@Controller` + `@ResponseBody`.
- It tells Spring: *"Whatever object this method returns (a List, a Record, an Object), automatically serialize it directly into JSON and write it into the HTTP response body!"*

```java
@RestController
@RequestMapping("/api/v1/models")
public class ModelDiscoveryController {

    @GetMapping
    public List<String> getAvailableModels() {
        return List.of("gpt-4o", "claude-3-5-sonnet", "llama-3.2");
        // Spring automatically serializes this List into JSON: ["gpt-4o","claude-3-5-sonnet","llama-3.2"]
    }
}
```

---

### 4.2 Handling `@PathVariable` vs. `@RequestParam`

- **`@PathVariable`**: Used to identify a **specific resource by ID** in the URL path.
  `GET /api/v1/prompts/prompt-101` $\rightarrow$ `@GetMapping("/{id}") public Prompt get(@PathVariable String id)`
- **`@RequestParam`**: Used for **filtering, pagination, and sorting query parameters**.
  `GET /api/v1/prompts?category=finance&limit=10` $\rightarrow$ `@RequestParam(defaultValue = "10") int limit`

---

# 5. Hands-on Project: The Prompt Library CRUD API

Let's build a complete, production-ready Prompt Library API.

### 5.1 The `PromptTemplate` Model

```java
package com.javagenai.day15;

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

---

### 5.2 The `PromptController`

```java
package com.javagenai.day15;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/prompts")
public class PromptController {

    // Thread-safe in-memory storage
    private final Map<String, PromptTemplate> promptStore = new ConcurrentHashMap<>();

    public PromptController() {
        // Seed default prompt
        String id = "prompt-001";
        promptStore.put(id, new PromptTemplate(
            id, "Java Senior Code Reviewer", "Engineering",
            "Review the following Java 21 code for thread safety and memory leaks: {code}",
            0.2, Instant.now()
        ));
    }

    // 1. GET ALL (with optional category filter)
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
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 404
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

# 6. Key Takeaways & Summary

```
                  ┌─────────────────────────────────┐
                  │       DAY 15 CHEAT SHEET        │
                  └────────────────┬────────────────┘
                                   │
         ┌─────────────────────────┼─────────────────────────┐
         ▼                         ▼                         ▼
  [ HTTP Semantics ]       [ Spring Web Annotations] [ Status Codes ]
  • GET: Safe & Idempotent • @RestController =       • 200: OK
  • POST: Non-idempotent     Controller + ResponseBody• 201: Resource Created
  • PUT: Full replace      • @PathVariable for IDs   • 204: Deleted (No Content)
  • DELETE: Remove item    • @RequestParam for query • 404: Not Found
  • DispatcherServlet routes • @RequestBody for JSON  • 429: Rate Limit Hit
```

---

# 7. Practice Exercises & Full Solutions

### 🏋️ Exercise 1: Build a Prompt Render Endpoint
**Objective**: Add an endpoint `POST /api/v1/prompts/{id}/render` that takes a `Map<String, String> variables` in the request body, replaces `{key}` placeholders in the template text with incoming values, and returns the rendered prompt string.

#### Solution:
```java
package com.javagenai.day15;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

public class PromptRenderExtension {

    public static ResponseEntity<String> renderTemplate(PromptTemplate template, Map<String, String> vars) {
        if (template == null) {
            return ResponseEntity.notFound().build();
        }
        String rendered = template.templateText();
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            rendered = rendered.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return ResponseEntity.ok(rendered);
    }
}
```

---

## 8. Self-Check Quiz

1. **What is the difference between `@Controller` and `@RestController`?**
   - *Answer*: `@Controller` returns view names (HTML templates), whereas `@RestController` automatically serializes returned Java objects into JSON using Jackson and writes them directly to the HTTP response body.
2. **Why is `GET` considered safe and idempotent, while `POST` is neither?**
   - *Answer*: `GET` only retrieves data without modifying server state (safe). Calling `GET` repeatedly produces the exact same outcome without side-effects (idempotent). `POST` submits data to create resources or perform actions, modifying server state on every invocation (neither safe nor idempotent).
3. **When should you use `@PathVariable` vs `@RequestParam`?**
   - *Answer*: Use `@PathVariable` when extracting values that identify a specific resource path (e.g., `/prompts/123`). Use `@RequestParam` for query string parameters used for filtering, searching, or pagination (e.g., `/prompts?category=code&page=1`).
4. **What HTTP status code should be returned when a resource is successfully created via `POST`?**
   - *Answer*: `201 Created` (optionally with a `Location` header pointing to the new resource).
5. **What is the role of the `DispatcherServlet` in Spring Boot?**
   - *Answer*: It acts as the central Front Controller, intercepting all incoming HTTP requests, determining which controller method should handle them via `HandlerMapping`, and dispatching execution to that method.

---

<p align="center">
  <b>Congratulations on completing Day 15! 🎉</b><br>
  Tomorrow on <b>Day 16</b>, we master <b>Request Validation, DTOs & Response Design</b>: Bean Validation (<code>@NotNull</code>, <code>@Size</code>), RFC 7807 Problem Details, and why you never expose database entities to the outside world!
</p>
