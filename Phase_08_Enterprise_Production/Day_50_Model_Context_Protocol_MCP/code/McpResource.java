package com.genai.enterprise.mcp;

/**
 * Functional contract for readable context resources exposed over MCP.
 */
public interface McpResource {
    McpProtocol.ResourceDescriptor getDescriptor();
    String read();
}
