package com.genai.enterprise.mcp;

import java.util.Map;

/**
 * Executable demonstration of Day 50:
 * Model Context Protocol (MCP) in Java.
 */
public class McpDemo {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("  DAY 50: MODEL CONTEXT PROTOCOL (MCP) IN JAVA DEMO              ");
        System.out.println("==================================================================");

        // 1. Initialize MCP Server
        McpServer server = EnterpriseDatabaseMcpServer.createServer();
        McpClient client = new McpClient(server);

        // 2. Protocol Handshake: 'initialize'
        System.out.println("\n--- 1. Protocol Handshake (initialize) ---");
        McpProtocol.JsonRpcResponse initResponse = client.initialize();
        System.out.println("Protocol Response: " + initResponse.result());

        // 3. Resource Discovery & Inspection: 'resources/list' and 'resources/read'
        System.out.println("\n--- 2. Resource Discovery (resources/list & resources/read) ---");
        McpProtocol.JsonRpcResponse resListResponse = client.listResources();
        System.out.println("Available Resources: " + resListResponse.result());

        McpProtocol.JsonRpcResponse schemaResponse = client.readResource("postgres://warehouse/schema.sql");
        System.out.println("\nRetrieved Database Schema Resource:\n" + schemaResponse.result());

        // 4. Tool Discovery: 'tools/list'
        System.out.println("\n--- 3. Tool Discovery (tools/list) ---");
        McpProtocol.JsonRpcResponse toolsResponse = client.listTools();
        System.out.println("Discovered Tools: " + toolsResponse.result());

        // 5. Remote Tool Invocation: 'tools/call'
        System.out.println("\n--- 4. Remote Tool Execution (tools/call) ---");
        McpProtocol.JsonRpcResponse toolCallResponse = client.callTool(
            "querySalesByRegion",
            Map.of("region", "EMEA", "minVolume", 1000.0)
        );
        System.out.println("Tool Execution Output: " + toolCallResponse.result());

        // 6. Non-Existent Tool Handling
        System.out.println("\n--- 5. Error Handling for Unknown Tool ---");
        McpProtocol.JsonRpcResponse errorResponse = client.callTool("dropDatabase", Map.of());
        System.out.println("Error Payload: code=" + errorResponse.error().code() + ", message=\"" + errorResponse.error().message() + "\"");

        System.out.println("\n==================================================================");
        System.out.println("  MCP SPECIFICATION VERIFICATION COMPLETED SUCCESSFULLY          ");
        System.out.println("==================================================================");
    }
}
