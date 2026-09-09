package com.genai.springai.tools;

import java.util.List;
import java.util.Map;

/**
 * Enterprise Read-Only SQL Tool.
 * Demonstrates security hardening by blocking data-mutation operations (DROP, DELETE, UPDATE).
 */
public class SqlExecutorTool implements FunctionTool {

    private final Map<String, List<Map<String, Object>>> mockDatabase = Map.of(
        "products", List.of(
            Map.of("id", 101, "name", "Quantum Laptop Pro", "category", "Electronics", "price", 1899.99, "stock", 14),
            Map.of("id", 102, "name", "Ergonomic Mesh Chair", "category", "Furniture", "price", 349.50, "stock", 42),
            Map.of("id", 103, "name", "Noise-Cancelling Headphones", "category", "Electronics", "price", 299.00, "stock", 85)
        ),
        "orders", List.of(
            Map.of("order_id", "ORD-8821", "customer", "Alice Smith", "total", 1899.99, "status", "SHIPPED"),
            Map.of("order_id", "ORD-8822", "customer", "Bob Jones", "total", 648.50, "status", "PROCESSING")
        )
    );

    @Override
    public ToolDefinition getDefinition() {
        return new ToolDefinition(
            "executeReadOnlySql",
            "Executes a sanitized, read-only SELECT SQL query against the enterprise business data warehouse.",
            Map.of(
                "query", new ToolDefinition.ParameterSpec("string", "The read-only SELECT SQL statement to execute", true)
            )
        );
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String query = (String) arguments.get("query");
        if (query == null || query.isBlank()) {
            return "{\"error\": \"Empty SQL query supplied.\"}";
        }

        String normalized = query.trim().toUpperCase();

        // Security Guardrails: Enforce read-only access
        if (!normalized.startsWith("SELECT")) {
            return "{\"error\": \"SECURITY VIOLATION: Only read-only SELECT statements are permitted by database policy.\"}";
        }

        if (normalized.contains("DROP") || normalized.contains("DELETE") || normalized.contains("UPDATE") ||
            normalized.contains("INSERT") || normalized.contains("ALTER") || normalized.contains("TRUNCATE")) {
            return "{\"error\": \"SECURITY VIOLATION: Mutation keyword detected in query.\"}";
        }

        if (normalized.contains("PRODUCTS")) {
            return "{\"table\": \"products\", \"rowCount\": 3, \"rows\": " + mockDatabase.get("products") + "}";
        } else if (normalized.contains("ORDERS")) {
            return "{\"table\": \"orders\", \"rowCount\": 2, \"rows\": " + mockDatabase.get("orders") + "}";
        }

        return "{\"table\": \"custom\", \"rowCount\": 0, \"rows\": [], \"message\": \"Query executed successfully. 0 rows returned.\"}";
    }
}
