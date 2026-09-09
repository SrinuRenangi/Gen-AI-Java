package com.genai.enterprise.mcp;

import java.util.Map;

/**
 * Functional contract for executable tools exposed over MCP.
 */
public interface McpTool {
    McpProtocol.ToolDescriptor getDescriptor();
    String execute(Map<String, Object> arguments) throws Exception;
}
