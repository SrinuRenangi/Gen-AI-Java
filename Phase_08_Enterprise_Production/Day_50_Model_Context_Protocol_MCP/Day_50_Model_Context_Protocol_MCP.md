# Day 50: Model Context Protocol (MCP) in Java

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [Day 49: Building a ReAct Agent in Java](../../Phase_07_LangChain4j/Day_49_Building_ReAct_Agent_in_Java/Day_49_Building_ReAct_Agent_in_Java.md) | [All 60 Days Overview](../../README.md) | [Day 51: Prompt Injection Defense & AI Security](../Day_51_Prompt_Injection_AI_Security/Day_51_Prompt_Injection_AI_Security.md) |

---

## 1. Topic Overview

The **Model Context Protocol (MCP)** is an open, standardized communication protocol introduced by Anthropic that provides a universal standard for connecting Large Language Model (LLM) applications to external tools, data resources, and prompts. In enterprise Java systems, MCP eliminates the fragmentation of bespoke API connectors by establishing a unified JSON-RPC 2.0 interface across local subprocesses and networked microservices.

---

## 2. Basic Foundations (True Zero)

### What Problem Does MCP Solve?

Before MCP, integrating an LLM application with enterprise data sources was an $N \times M$ integration problem:

```
[Claude Desktop]      [ChatGPT]      [Spring AI Agent]      [Cursor IDE]
       │                  │                  │                  │
   (Custom)           (Custom)           (Custom)           (Custom)
       ▼                  ▼                  ▼                  ▼
[PostgreSQL DB]     [Git Repo]        [Slack API]       [Salesforce CRM]
```

Every AI client had its own proprietary plugin or tool-calling schema. If you maintained four AI clients and four data sources, you had to write and maintain 16 separate integrations.

With MCP, the ecosystem converges on a single universal standard:

```
[Claude Desktop]      [ChatGPT]      [Spring AI Agent]      [Cursor IDE]
       │                  │                  │                  │
       └──────────────────┴────────┬─────────┴──────────────────┘
                                   │  (MCP Standard JSON-RPC 2.0)
                                   ▼
                       ┌───────────────────────┐
                       │      MCP Clients      │
                       └───────────┬───────────┘
                                   │  (Stdio / HTTP + SSE)
       ┌──────────────────┬────────┴─────────┬──────────────────┐
       ▼                  ▼                  ▼                  ▼
[PostgreSQL MCP]    [Git MCP]          [Slack MCP]       [Salesforce MCP]
```

Now, each data source or tool provider writes **one** MCP Server, and every MCP-compliant host can immediately discover and use it.

### Relatable Physical Analogy: The USB-C Standard

Think of the consumer electronics industry before USB-C: digital cameras had Mini-USB, older phones used Micro-USB, iPhones used Lightning, printers used USB-B, and laptops had proprietary barrel power jacks. Connecting your devices required a drawer full of specialized adapter cables.

**MCP is the USB-C standard for Generative AI.** Just as any USB-C cable connects any USB-C phone to any USB-C charger, monitor, or external SSD regardless of the manufacturer, MCP allows any AI host to plug into any data source or tool server using one universal wire protocol.

---

### Minimal Beginner-Friendly Example: A Pure Java MCP Ping-Pong Handshake

Here is a minimal, zero-dependency Java program demonstrating the core wire format of an MCP JSON-RPC 2.0 `initialize` request and response:

```java
package com.genai.enterprise.mcp.minimal;

public class MinimalMcpHandshake {

    // 1. MCP client sends an initialize request in JSON-RPC 2.0 format
    public static final String INITIALIZE_REQUEST = """
        {
          "jsonrpc": "2.0",
          "id": 1,
          "method": "initialize",
          "params": {
            "protocolVersion": "2024-11-05",
            "capabilities": { "roots": { "listChanged": true } },
            "clientInfo": { "name": "SpringAiMcpHost", "version": "1.0.0" }
          }
        }
        """;

    // 2. MCP server inspects request and returns capabilities
    public static String handleRpcRequest(String rawJson) {
        if (rawJson.contains("\"method\": \"initialize\"")) {
            return """
                {
                  "jsonrpc": "2.0",
                  "id": 1,
                  "result": {
                    "protocolVersion": "2024-11-05",
                    "capabilities": {
                      "tools": { "listChanged": false },
                      "resources": { "subscribe": false }
                    },
                    "serverInfo": { "name": "MinimalWarehouseMcpServer", "version": "1.0.0" }
                  }
                }
                """;
        }
        return """
            {"jsonrpc": "2.0", "id": 1, "error": {"code": -32601, "message": "Method not found"}}
            """;
    }

    public static void main(String[] args) {
        System.out.println("Client Request:");
        System.out.println(INITIALIZE_REQUEST);

        String serverResponse = handleRpcRequest(INITIALIZE_REQUEST);
        System.out.println("Server Response:");
        System.out.println(serverResponse);
    }
}
```

#### Line-by-Line Walkthrough:
1. `public static final String INITIALIZE_REQUEST`: Defines the exact JSON-RPC 2.0 handshake envelope required by the MCP specification (`protocolVersion: 2024-11-05`).
2. `"method": "initialize"`: The mandatory initial RPC method sent by any MCP client before any tools or resources can be accessed.
3. `handleRpcRequest(String rawJson)`: Simulates the server-side RPC dispatcher inspecting the incoming message.
4. `capabilities`: The server declares what features it supports (`tools`, `resources`, `prompts`).
5. `main(String[] args)`: Executes the round-trip handshake demonstrating protocol compliance.

---

## 3. Core Concept Walkthrough (Basic → Intermediate)

### 3.1 The MCP Architectural Roles

An MCP deployment consists of three primary architectural components:

```
┌────────────────────────────────────────────────────────┐
│                        MCP HOST                        │
│  (e.g., Spring Boot AI Service, Claude Desktop, Cursor) │
│                                                        │
│  ┌────────────────────────┐  ┌──────────────────────┐  │
│  │       LLM Engine       │  │      MCP Client      │  │
│  │  (OpenAI / Anthropic)  │  │  (Manages Sessions)  │  │
│  └───────────┬────────────┘  └──────────┬───────────┘  │
└──────────────┼──────────────────────────┼──────────────┘
               │                          │
               │ Decides to call tool     │ Transports JSON-RPC 2.0
               │                          │ (Stdio or HTTP+SSE)
               ▼                          ▼
┌────────────────────────────────────────────────────────┐
│                       MCP SERVER                       │
│  (e.g., PostgreSQL MCP, Git MCP, Jira MCP)             │
│                                                        │
│  ┌────────────────┐ ┌────────────────┐ ┌─────────────┐ │
│  │   Resources    │ │     Tools      │ │   Prompts   │ │
│  │ (Data/Schemas) │ │  (Executable)  │ │ (Templates) │ │
│  └────────────────┘ └────────────────┘ └─────────────┘ │
└────────────────────────────────────────────────────────┘
```

1. **MCP Host**: The application runtime coordinating the user interaction and the LLM (e.g., a Spring Boot service, Claude Desktop, or Cursor).
2. **MCP Client**: The component embedded inside the Host that initiates connections, manages protocol lifecycles, and executes JSON-RPC calls against MCP servers.
3. **MCP Server**: A standalone process or microservice that exposes domain tools, readable data resources, or pre-configured prompts.

---

### 3.2 The Three MCP Primitives: Tools, Resources, Prompts

| Primitive | Nature | Purpose | Example |
|:---|:---|:---|:---|
| **Tools** | Active (Executable) | Functions with JSON Schema inputs that the LLM can decide to invoke. | `querySalesByRegion(region, minVolume)` |
| **Resources** | Passive (Readable) | Read-only contextual data, documents, or schemas identified by URIs. | `postgres://warehouse/schema.sql`, `file:///logs/app.log` |
| **Prompts** | Guided (Templated) | Standardized prompt workflows exposed by the server for user selection. | `audit-security-vulnerability`, `explain-code` |

---

### 3.3 MCP Transport Mechanisms: Stdio vs. HTTP + SSE

MCP defines two official transport layers:

```
1. Standard I/O (Stdio) Transport:
   [Host Application] ──(stdin / stdout)──> [Local Child Process MCP Server]
   * Best for: Local desktop tools, CLI utilities, security-isolated child processes.

2. HTTP with Server-Sent Events (SSE) Transport:
   [Host Application] ──(POST /messages)───> [Remote Networked MCP Server]
   [Host Application] <──(SSE Stream)─────── [Remote Networked MCP Server]
   * Best for: Distributed cloud microservices, shared enterprise databases, Kubernetes.
```

---

### 3.4 Java 21 Implementation: Protocol Records & Dispatcher

Let's inspect the real-world Java 21 implementation from our lesson companion code (`Phase_08_Enterprise_Production/Day_50_Model_Context_Protocol_MCP/code/`).

#### Step 1: Defining Immutable Protocol Records (`McpProtocol.java`)

```java
package com.genai.enterprise.mcp;

import java.util.List;
import java.util.Map;

public final class McpProtocol {

    public static final String PROTOCOL_VERSION = "2024-11-05";

    public record JsonRpcRequest(
        String jsonrpc,
        Object id,
        String method,
        Map<String, Object> params
    ) {
        public static JsonRpcRequest of(Object id, String method, Map<String, Object> params) {
            return new JsonRpcRequest("2.0", id, method, params);
        }
    }

    public record JsonRpcResponse(
        String jsonrpc,
        Object id,
        Object result,
        JsonRpcError error
    ) {
        public static JsonRpcResponse success(Object id, Object result) {
            return new JsonRpcResponse("2.0", id, result, null);
        }

        public static JsonRpcResponse error(Object id, int code, String message) {
            return new JsonRpcResponse("2.0", id, null, new JsonRpcError(code, message, null));
        }
    }

    public record JsonRpcError(int code, String message, Object data) {}

    public record ServerCapabilities(boolean tools, boolean resources, boolean prompts) {}
    public record ServerInfo(String name, String version) {}
    public record InitializeResult(String protocolVersion, ServerCapabilities capabilities, ServerInfo serverInfo) {}

    public record ToolDescriptor(String name, String description, Map<String, Object> inputSchema) {}
    public record ResourceDescriptor(String uri, String name, String mimeType, String description) {}
}
```

#### Step 2: Defining the Executable Tool Contract (`McpTool.java`)

```java
package com.genai.enterprise.mcp;

import java.util.Map;

public interface McpTool {
    McpProtocol.ToolDescriptor getDescriptor();
    String execute(Map<String, Object> arguments) throws Exception;
}
```

#### Step 3: Dispatching Protocol Server (`McpServer.java`)

```java
package com.genai.enterprise.mcp;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class McpServer {

    private final String serverName;
    private final String version;
    private final Map<String, McpTool> registeredTools = new ConcurrentHashMap<>();
    private final Map<String, McpResource> registeredResources = new ConcurrentHashMap<>();

    public McpServer(String serverName, String version) {
        this.serverName = serverName;
        this.version = version;
    }

    public void registerTool(McpTool tool) {
        registeredTools.put(tool.getDescriptor().name(), tool);
    }

    public void registerResource(McpResource resource) {
        registeredResources.put(resource.getDescriptor().uri(), resource);
    }

    public McpProtocol.JsonRpcResponse handleRequest(McpProtocol.JsonRpcRequest request) {
        return switch (request.method()) {
            case "initialize" -> handleInitialize(request);
            case "tools/list" -> handleToolsList(request);
            case "tools/call" -> handleToolsCall(request);
            case "resources/list" -> handleResourcesList(request);
            case "resources/read" -> handleResourcesRead(request);
            default -> McpProtocol.JsonRpcResponse.error(
                request.id(), -32601, "Method not found: " + request.method()
            );
        };
    }

    private McpProtocol.JsonRpcResponse handleInitialize(McpProtocol.JsonRpcRequest req) {
        var capabilities = new McpProtocol.ServerCapabilities(!registeredTools.isEmpty(), !registeredResources.isEmpty(), false);
        var serverInfo = new McpProtocol.ServerInfo(serverName, version);
        return McpProtocol.JsonRpcResponse.success(req.id(), new McpProtocol.InitializeResult(McpProtocol.PROTOCOL_VERSION, capabilities, serverInfo));
    }

    private McpProtocol.JsonRpcResponse handleToolsList(McpProtocol.JsonRpcRequest req) {
        List<McpProtocol.ToolDescriptor> descriptors = registeredTools.values().stream()
            .map(McpTool::getDescriptor)
            .toList();
        return McpProtocol.JsonRpcResponse.success(req.id(), Map.of("tools", descriptors));
    }

    private McpProtocol.JsonRpcResponse handleToolsCall(McpProtocol.JsonRpcRequest req) {
        String toolName = (String) req.params().get("name");
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) req.params().getOrDefault("arguments", Map.of());

        McpTool tool = registeredTools.get(toolName);
        if (tool == null) {
            return McpProtocol.JsonRpcResponse.error(req.id(), -32601, "Tool not found: " + toolName);
        }

        try {
            String output = tool.execute(arguments);
            return McpProtocol.JsonRpcResponse.success(req.id(), Map.of(
                "content", List.of(Map.of("type", "text", "text", output)),
                "isError", false
            ));
        } catch (Exception ex) {
            return McpProtocol.JsonRpcResponse.error(req.id(), -32603, "Internal tool error: " + ex.getMessage());
        }
    }

    private McpProtocol.JsonRpcResponse handleResourcesList(McpProtocol.JsonRpcRequest req) {
        List<McpProtocol.ResourceDescriptor> descriptors = registeredResources.values().stream()
            .map(McpResource::getDescriptor)
            .toList();
        return McpProtocol.JsonRpcResponse.success(req.id(), Map.of("resources", descriptors));
    }

    private McpProtocol.JsonRpcResponse handleResourcesRead(McpProtocol.JsonRpcRequest req) {
        String uri = (String) req.params().get("uri");
        McpResource resource = registeredResources.get(uri);
        if (resource == null) {
            return McpProtocol.JsonRpcResponse.error(req.id(), -32602, "Resource URI not found: " + uri);
        }
        return McpProtocol.JsonRpcResponse.success(req.id(), Map.of(
            "contents", List.of(Map.of(
                "uri", uri,
                "mimeType", resource.getDescriptor().mimeType(),
                "text", resource.read()
            ))
        ));
    }
}
```

---

## 4. Prerequisite & Supporting Concepts

### Prerequisite / Supporting Concept: JSON-RPC 2.0 Protocol
MCP is strictly built on top of [JSON-RPC 2.0](https://www.jsonrpc.org/specification). Every message is a JSON object containing:
- `"jsonrpc": "2.0"` (protocol version declaration)
- `"id"`: Unique correlation identifier matching requests to responses.
- `"method"`: Procedure name to invoke (e.g. `initialize`, `tools/list`, `tools/call`).
- `"params"`: Structured parameter payload.
- In responses: Either `"result"` or `"error"`, never both. Standard JSON-RPC error codes include:
  - `-32700`: Parse error
  - `-32600`: Invalid Request
  - `-32601`: Method not found
  - `-32602`: Invalid params
  - `-32603`: Internal error

### Prerequisite / Supporting Concept: Stdio vs. Server-Sent Events (SSE)
- **Stdio Transport**: The MCP Host executes the MCP Server as a subprocess (`java -jar mcp-server.jar`) and communicates over standard input (`System.in`) and standard output (`System.out`). Log messages MUST go to `System.err` so they do not corrupt the JSON-RPC stream.
- **HTTP + SSE Transport**: The MCP Server runs as a web service. The client opens an HTTP GET connection requesting `text/event-stream` to receive server messages, and sends JSON-RPC commands via HTTP POST requests to an endpoint (e.g., `/messages`).

### Prerequisite / Supporting Concept: Java 21 Records and Pattern Matching
Java 21 `record` declarations provide concise, immutable data carriers with automatic `equals()`, `hashCode()`, and `toString()` implementations. Pattern-matching `switch` expressions allow safe, exhaustive handling of protocol methods without brittle string parsing chains.

---

## 5. Advanced Depth (Intermediate → Advanced)

### 5.1 Real-World Enterprise Database MCP Server

In production, an MCP server can expose relational warehouse schemas as **Resources** and analytic aggregations as **Tools**:

```java
package com.genai.enterprise.mcp;

import java.util.Map;

public class EnterpriseDatabaseMcpServer {

    public static McpServer createServer() {
        McpServer server = new McpServer("acme-postgres-mcp", "1.4.0");

        // 1. Register Warehouse DDL Schema as a Resource (Read-only context)
        server.registerResource(new McpResource() {
            @Override
            public McpProtocol.ResourceDescriptor getDescriptor() {
                return new McpProtocol.ResourceDescriptor(
                    "postgres://warehouse/schema.sql",
                    "PostgreSQL Warehouse Schema",
                    "text/x-sql",
                    "DDL definition for enterprise orders, products, and customers tables"
                );
            }

            @Override
            public String read() {
                return """
                    CREATE TABLE customers (id SERIAL PRIMARY KEY, name VARCHAR(100), tier VARCHAR(20));
                    CREATE TABLE orders (id SERIAL PRIMARY KEY, customer_id INT, total NUMERIC(10,2), region VARCHAR(20));
                    CREATE TABLE products (id SERIAL PRIMARY KEY, sku VARCHAR(50), stock INT, price NUMERIC(10,2));
                    """;
            }
        });

        // 2. Register Analytics Tool (Executable function)
        server.registerTool(new McpTool() {
            @Override
            public McpProtocol.ToolDescriptor getDescriptor() {
                return new McpProtocol.ToolDescriptor(
                    "querySalesByRegion",
                    "Aggregates total enterprise sales volume and order count for a geographic region",
                    Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "region", Map.of("type", "string", "description", "Geographic sales region: NA, EMEA, APAC"),
                            "minVolume", Map.of("type", "number", "description", "Minimum order volume threshold in USD")
                        ),
                        "required", List.of("region")
                    )
                );
            }

            @Override
            public String execute(Map<String, Object> arguments) {
                String region = (String) arguments.getOrDefault("region", "NA");
                return String.format(
                    "{\"region\": \"%s\", \"totalOrders\": 1420, \"grossRevenue\": 482000.00, \"status\": \"VERIFIED\"}",
                    region.toUpperCase()
                );
            }
        });

        return server;
    }
}
```

---

### 5.2 Common Mistakes & Misconceptions: Bad vs. Good

#### Mistake 1: Logging to `System.out` in Stdio Transport
In Stdio transport, standard output is the dedicated JSON-RPC communication channel. Printing debug statements to `System.out` corrupts the JSON parser on the client.

```java
// ❌ BAD: Polluting stdout breaks the client's JSON-RPC stream
System.out.println("Processing tool call: " + toolName);

// ✅ GOOD: Direct all diagnostic logs to stderr or SLF4J stderr appenders
System.err.println("[DEBUG MCP] Processing tool call: " + toolName);
```

#### Mistake 2: Exposing Read-Only Static Data as Tools Instead of Resources
Models have to guess tool arguments and spend extra reasoning cycles when data should simply be available as context.

```java
// ❌ BAD: Exposing static DDL schema as an executable tool
public class GetSchemaTool implements McpTool {
    public String execute(Map<String, Object> args) { return ddlSchema; }
}

// ✅ GOOD: Expose schema as an McpResource with a distinct URI
public class SchemaResource implements McpResource {
    public McpProtocol.ResourceDescriptor getDescriptor() {
        return new McpProtocol.ResourceDescriptor("postgres://schema", "DB Schema", "text/x-sql", "DDL");
    }
    public String read() { return ddlSchema; }
}
```

#### Mistake 3: Unbounded Parameter Validation
Failing to validate inputs before executing database queries or shell processes creates critical security vectors.

```java
// ❌ BAD: Executing raw SQL directly from unvalidated tool arguments
String tableName = (String) args.get("table");
jdbcTemplate.execute("SELECT * FROM " + tableName); // SQL Injection!

// ✅ GOOD: Strictly whitelist parameters against known entities
String tableName = (String) args.get("table");
if (!ALLOWED_TABLES.contains(tableName)) {
    throw new IllegalArgumentException("Unauthorized table name: " + tableName);
}
```

---

### 5.3 Complete Verification Suite & Demo Execution

Execute the verification suite in `Phase_08_Enterprise_Production/Day_50_Model_Context_Protocol_MCP/code/`:

```bash
javac -d out Phase_08_Enterprise_Production/Day_50_Model_Context_Protocol_MCP/code/*.java
java -cp out com.genai.enterprise.mcp.McpDemo
```

```
==================================================================
  DAY 50: MODEL CONTEXT PROTOCOL (MCP) IN JAVA DEMO              
==================================================================

--- 1. Protocol Handshake (initialize) ---
Protocol Response: InitializeResult[protocolVersion=2024-11-05, capabilities=ServerCapabilities[tools=true, resources=true, prompts=false], serverInfo=ServerInfo[name=acme-postgres-mcp, version=1.4.0]]

--- 2. Resource Discovery (resources/list & resources/read) ---
Available Resources: {resources=[ResourceDescriptor[uri=postgres://warehouse/schema.sql, name=PostgreSQL Warehouse Schema, mimeType=text/x-sql, description=DDL definition for enterprise orders, products, and customers tables]]}

Retrieved Database Schema Resource:
{contents=[{mimeType=text/x-sql, uri=postgres://warehouse/schema.sql, text=CREATE TABLE customers (id SERIAL PRIMARY KEY, name VARCHAR(100), tier VARCHAR(20));
CREATE TABLE orders (id SERIAL PRIMARY KEY, customer_id INT, total NUMERIC(10,2), region VARCHAR(20));
CREATE TABLE products (id SERIAL PRIMARY KEY, sku VARCHAR(50), stock INT, price NUMERIC(10,2));
}]}

--- 3. Tool Discovery (tools/list) ---
Discovered Tools: {tools=[ToolDescriptor[name=querySalesByRegion, description=Aggregates total enterprise sales volume and order count for a geographic region, inputSchema={properties={region={description=Geographic sales region: NA, EMEA, APAC, type=string}, minVolume={description=Minimum order volume threshold in USD, type=number}}, type=object, required=[region]}]]}

--- 4. Remote Tool Execution (tools/call) ---
Tool Execution Output: {content=[{text={"region": "EMEA", "totalOrders": 1420, "grossRevenue": 482000.00, "status": "VERIFIED"}, type=text}], isError=false}

--- 5. Error Handling for Unknown Tool ---
Error Payload: code=-32601, message="Tool not found: dropDatabase"

==================================================================
  MCP SPECIFICATION VERIFICATION COMPLETED SUCCESSFULLY          
==================================================================
```

---

## 6. Quick Recap

| Concept | Description | Enterprise Value |
|:---|:---|:---|
| **MCP (Model Context Protocol)** | Open JSON-RPC 2.0 standard for LLM-tool-data interoperability | Eliminates proprietary $N \times M$ custom integration code. |
| **Tools Primitive** | Dynamic executable functions with strict JSON schema signatures | Allows LLM agents to safely invoke operations across microservices. |
| **Resources Primitive** | Read-only contextual data documents identified by unique URIs | Efficiently provides database DDLs, docs, and logs as background context. |
| **Prompts Primitive** | Server-managed, pre-engineered prompt templates | Standardizes domain workflows across multiple client UIs. |
| **Stdio Transport** | Standard input/output communication for local subprocesses | Zero network overhead, high performance for desktop & CLI tools. |
| **HTTP + SSE Transport** | Networked transport using HTTP POST and Server-Sent Events | Standardized, firewall-friendly microservice connectivity for cloud systems. |

---

## 7. Self-Check Questions & Practice Exercises

### Conceptual Self-Check Questions

#### Question 1: What is the primary architectural goal of the Model Context Protocol?
- A) To replace SQL queries with raw natural language.
- B) To provide a standardized JSON-RPC 2.0 protocol enabling any AI model to discover and execute tools, read resources, and fetch prompts across heterogeneous systems without custom adapters.
- C) To compress large prompt strings into binary zip files.
- D) To run LLMs locally on CPU chips without GPUs.

*Answer*: **B**. MCP acts as the "USB-C of AI", standardizing the wire protocol so any AI host (Spring AI, Claude, Cursor) can communicate with any tool or data provider.

---

#### Question 2: Why should database DDL schemas be exposed as an MCP "Resource" rather than an MCP "Tool"?
- A) Relational databases do not support JSON formats.
- B) Tools cannot return strings longer than 100 characters.
- C) Resources represent passive, read-only contextual information that the host can inject into system context directly without executing a function call cycle.
- D) Resources run faster on GPU hardware.

*Answer*: **C**. Resources provide passive, cacheable context (schemas, logs, documentation) directly to the prompt context, avoiding unnecessary LLM tool-calling loops.

---

#### Question 3: In an MCP server communicating over standard I/O (Stdio), where must application log messages be written?
- A) To `System.out` alongside JSON responses.
- B) To `System.err` so that JSON-RPC messages on standard out are not corrupted.
- C) Stdio MCP servers are forbidden from producing log messages.
- D) To an external floppy drive.

*Answer*: **B**. Standard out (`stdout`) is reserved strictly for JSON-RPC messages. Any raw logging sent to `stdout` breaks the client's JSON parser. Logs must always be routed to `stderr`.

---

#### Question 4: What are the two official transports defined in the MCP specification?
- A) WebSocket and gRPC.
- B) Stdio (Standard I/O) and HTTP with Server-Sent Events (SSE).
- C) FTP and Telnet.
- D) SMTP and SOAP.

*Answer*: **B**. Stdio is used for local subprocesses (desktop, CLI), while HTTP + SSE is used for distributed cloud microservices.

---

### Hands-on Practice Exercises

#### Exercise 1: Git Branch Auditor MCP Resource
**Task**: Implement an `McpResource` that exposes the active Git branch and latest commit SHA under the URI `git://repo/status`.

**Solution**:
```java
package com.genai.enterprise.exercises;

import com.genai.enterprise.mcp.McpProtocol;
import com.genai.enterprise.mcp.McpResource;

public class GitStatusMcpResource implements McpResource {

    @Override
    public McpProtocol.ResourceDescriptor getDescriptor() {
        return new McpProtocol.ResourceDescriptor(
            "git://repo/status",
            "Git Repository Working Status",
            "application/json",
            "Returns active branch, HEAD commit SHA, and clean/dirty working tree status"
        );
    }

    @Override
    public String read() {
        // In production, execute `git status --porcelain` or read JGit
        return """
            {
              "branch": "main",
              "headCommit": "9a282b9",
              "isDirty": false,
              "upstream": "origin/main"
            }
            """;
    }
}
```

---

#### Exercise 2: Server Protocol Compatibility Guard
**Task**: Build a static security validator `boolean isServerCompatible(McpProtocol.InitializeResult result)` that verifies the server conforms to protocol version `2024-11-05` and supports tools.

**Solution**:
```java
package com.genai.enterprise.exercises;

import com.genai.enterprise.mcp.McpProtocol;

public class McpCompatibilityValidator {

    public static boolean isServerCompatible(McpProtocol.InitializeResult result) {
        if (result == null) {
            return false;
        }
        boolean versionMatches = McpProtocol.PROTOCOL_VERSION.equals(result.protocolVersion());
        boolean hasToolCapability = result.capabilities() != null && result.capabilities().tools();
        return versionMatches && hasToolCapability;
    }
}
```

---

#### Exercise 3: Resilient MCP Tool Invoker with Virtual Thread Timeout
**Task**: Create a resilient invoker method that calls an MCP tool and enforces a timeout using Java 21 Virtual Threads, returning an appropriate JSON-RPC error response if the tool times out.

**Solution**:
```java
package com.genai.enterprise.exercises;

import com.genai.enterprise.mcp.McpClient;
import com.genai.enterprise.mcp.McpProtocol;

import java.util.Map;
import java.util.concurrent.*;

public class ResilientMcpInvoker {

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public McpProtocol.JsonRpcResponse callWithTimeout(
            McpClient client, 
            String toolName, 
            Map<String, Object> arguments, 
            long timeoutMs) {
        
        Future<McpProtocol.JsonRpcResponse> future = executor.submit(() -> client.callTool(toolName, arguments));
        
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException te) {
            future.cancel(true);
            return McpProtocol.JsonRpcResponse.error(
                "timeout-" + System.currentTimeMillis(),
                -32000,
                "MCP tool call timed out after " + timeoutMs + " ms"
            );
        } catch (Exception ex) {
            return McpProtocol.JsonRpcResponse.error(
                "err-" + System.currentTimeMillis(),
                -32603,
                "Tool execution failed: " + ex.getMessage()
            );
        }
    }
}
```

---

| Previous Day | Course Hub | Next Day |
|:---|:---:|---:|
| [Day 49: Building a ReAct Agent in Java](../../Phase_07_LangChain4j/Day_49_Building_ReAct_Agent_in_Java/Day_49_Building_ReAct_Agent_in_Java.md) | [All 60 Days Overview](../../README.md) | [Day 51: Prompt Injection Defense & AI Security](../Day_51_Prompt_Injection_AI_Security/Day_51_Prompt_Injection_AI_Security.md) |
