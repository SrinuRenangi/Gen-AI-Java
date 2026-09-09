package com.genai.enterprise.mcp;

import java.util.*;

/**
 * Standard MCP Server handling JSON-RPC 2.0 protocol dispatching.
 */
public class McpServer {

    private final String serverName;
    private final String serverVersion;
    private final Map<String, McpTool> tools = new LinkedHashMap<>();
    private final Map<String, McpResource> resources = new LinkedHashMap<>();

    public McpServer(String serverName, String serverVersion) {
        this.serverName = serverName;
        this.serverVersion = serverVersion;
    }

    public McpServer registerTool(McpTool tool) {
        tools.put(tool.getDescriptor().name(), tool);
        return this;
    }

    public McpServer registerResource(McpResource resource) {
        resources.put(resource.getDescriptor().uri(), resource);
        return this;
    }

    public McpProtocol.JsonRpcResponse handleRequest(McpProtocol.JsonRpcRequest request) {
        String id = request.id();
        String method = request.method();

        try {
            return switch (method) {
                case "initialize" -> McpProtocol.JsonRpcResponse.success(id, new McpProtocol.InitializeResult(
                    McpProtocol.PROTOCOL_VERSION,
                    new McpProtocol.ServerCapabilities(!tools.isEmpty(), !resources.isEmpty(), false),
                    new McpProtocol.ServerInfo(serverName, serverVersion)
                ));

                case "tools/list" -> {
                    List<McpProtocol.ToolDescriptor> list = tools.values().stream()
                        .map(McpTool::getDescriptor).toList();
                    yield McpProtocol.JsonRpcResponse.success(id, Map.of("tools", list));
                }

                case "tools/call" -> {
                    String toolName = (String) request.params().get("name");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> arguments = (Map<String, Object>) request.params().getOrDefault("arguments", Map.of());

                    McpTool tool = tools.get(toolName);
                    if (tool == null) {
                        yield McpProtocol.JsonRpcResponse.error(id, -32601, "Tool not found: " + toolName);
                    }

                    String result = tool.execute(arguments);
                    yield McpProtocol.JsonRpcResponse.success(id, Map.of(
                        "content", List.of(Map.of("type", "text", "text", result)),
                        "isError", false
                    ));
                }

                case "resources/list" -> {
                    List<McpProtocol.ResourceDescriptor> list = resources.values().stream()
                        .map(McpResource::getDescriptor).toList();
                    yield McpProtocol.JsonRpcResponse.success(id, Map.of("resources", list));
                }

                case "resources/read" -> {
                    String uri = (String) request.params().get("uri");
                    McpResource resource = resources.get(uri);
                    if (resource == null) {
                        yield McpProtocol.JsonRpcResponse.error(id, -32002, "Resource not found: " + uri);
                    }

                    String content = resource.read();
                    yield McpProtocol.JsonRpcResponse.success(id, Map.of(
                        "contents", List.of(Map.of("uri", uri, "mimeType", resource.getDescriptor().mimeType(), "text", content))
                    ));
                }

                default -> McpProtocol.JsonRpcResponse.error(id, -32601, "Method not supported: " + method);
            };
        } catch (Exception ex) {
            return McpProtocol.JsonRpcResponse.error(id, -32603, "Internal tool error: " + ex.getMessage());
        }
    }
}
