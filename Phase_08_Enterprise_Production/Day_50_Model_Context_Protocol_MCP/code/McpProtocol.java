package com.genai.enterprise.mcp;

import java.util.Map;

/**
 * Standard JSON-RPC 2.0 protocol structures defining the Model Context Protocol specification.
 */
public class McpProtocol {

    public static final String PROTOCOL_VERSION = "2024-11-05";

    public record JsonRpcRequest(String jsonrpc, String id, String method, Map<String, Object> params) {
        public static JsonRpcRequest of(String id, String method, Map<String, Object> params) {
            return new JsonRpcRequest("2.0", id, method, params);
        }
    }

    public record JsonRpcResponse(String jsonrpc, String id, Object result, JsonRpcError error) {
        public static JsonRpcResponse success(String id, Object result) {
            return new JsonRpcResponse("2.0", id, result, null);
        }

        public static JsonRpcResponse error(String id, int code, String message) {
            return new JsonRpcResponse("2.0", id, null, new JsonRpcError(code, message));
        }
    }

    public record JsonRpcError(int code, String message) {}

    public record ToolDescriptor(String name, String description, Map<String, Object> inputSchema) {}

    public record ResourceDescriptor(String uri, String name, String mimeType, String description) {}

    public record InitializeResult(
        String protocolVersion,
        ServerCapabilities capabilities,
        ServerInfo serverInfo
    ) {}

    public record ServerCapabilities(boolean tools, boolean resources, boolean prompts) {}

    public record ServerInfo(String name, String version) {}
}
