package com.genai.enterprise.capstone;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Model Context Protocol (MCP) Tool Registry enforcing RBAC execution policies.
 */
public class EnterpriseMcpToolRegistry {

    public interface McpToolHandler {
        String execute(EnterpriseSecurityContext sec, Map<String, Object> params);
    }

    private final Map<String, McpToolHandler> tools = new ConcurrentHashMap<>();

    public EnterpriseMcpToolRegistry() {
        registerTools();
    }

    private void registerTools() {
        // Tool 1: Query Portfolio (Analyst or Admin)
        tools.put("QueryCustomerPortfolio", (sec, params) -> {
            String accountId = (String) params.getOrDefault("accountId", "ACC-UNKNOWN");
            return "{\"accountId\": \"" + accountId + "\", \"totalAum\": 1850000.00, \"status\": \"ACTIVE\", \"riskProfile\": \"MODERATE\"}";
        });

        // Tool 2: Execute Transfer (Admin or Finance Officer only)
        tools.put("ExecuteFinancialTransfer", (sec, params) -> {
            if (!sec.canExecuteFinancialTransfers()) {
                throw new SecurityException("ACCESS DENIED: Caller [" + sec.userId() + "] lacks ROLE_FINANCE_OFFICER for fund transfer");
            }
            double amount = ((Number) params.getOrDefault("amount", 0.0)).doubleValue();
            String destination = (String) params.getOrDefault("destination", "EXT-UNKNOWN");
            return "{\"status\": \"CONFIRMED\", \"transferId\": \"TXN-998811\", \"amount\": " + amount + ", \"destination\": \"" + destination + "\"}";
        });
    }

    public String invokeTool(EnterpriseSecurityContext sec, String toolName, Map<String, Object> params) {
        McpToolHandler handler = tools.get(toolName);
        if (handler == null) {
            throw new IllegalArgumentException("Unknown MCP Tool: " + toolName);
        }
        return handler.execute(sec, params);
    }
}
