package com.genai.enterprise.mcp;

import java.util.Map;

/**
 * Standard MCP Client adapter connecting to an MCP Server.
 */
public class McpClient {

    private final McpServer server;

    public McpClient(McpServer server) {
        this.server = server;
    }

    public McpProtocol.JsonRpcResponse initialize() {
        return server.handleRequest(McpProtocol.JsonRpcRequest.of("req-init-1", "initialize", Map.of()));
    }

    public McpProtocol.JsonRpcResponse listTools() {
        return server.handleRequest(McpProtocol.JsonRpcRequest.of("req-tools-list", "tools/list", Map.of()));
    }

    public McpProtocol.JsonRpcResponse callTool(String name, Map<String, Object> arguments) {
        return server.handleRequest(McpProtocol.JsonRpcRequest.of(
            "req-tool-call-" + System.currentTimeMillis(),
            "tools/call",
            Map.of("name", name, "arguments", arguments)
        ));
    }

    public McpProtocol.JsonRpcResponse listResources() {
        return server.handleRequest(McpProtocol.JsonRpcRequest.of("req-res-list", "resources/list", Map.of()));
    }

    public McpProtocol.JsonRpcResponse readResource(String uri) {
        return server.handleRequest(McpProtocol.JsonRpcRequest.of(
            "req-res-read",
            "resources/read",
            Map.of("uri", uri)
        ));
    }
}
