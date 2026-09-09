package com.genai.enterprise.mcp;

import java.util.Map;

/**
 * Concrete enterprise MCP Server exposing SQL database schemas as resources
 * and query execution services as tools.
 */
public class EnterpriseDatabaseMcpServer {

    public static McpServer createServer() {
        McpServer server = new McpServer("acme-postgres-mcp", "1.4.0");

        // 1. Expose database schema as a readable Resource
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

        // 2. Expose sales telemetry as an executable Tool
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
                        "required", java.util.List.of("region")
                    )
                );
            }

            @Override
            public String execute(Map<String, Object> arguments) {
                String region = (String) arguments.getOrDefault("region", "NA");
                double minVol = arguments.get("minVolume") instanceof Number n ? n.doubleValue() : 0.0;

                return String.format(
                    "{\"region\": \"%s\", \"totalOrders\": 1420, \"grossRevenue\": 482000.00, \"averageOrder\": 339.43, \"filteredByMinVolume\": %.2f}",
                    region.toUpperCase(), minVol
                );
            }
        });

        // 3. Expose index health auditor tool
        server.registerTool(new McpTool() {
            @Override
            public McpProtocol.ToolDescriptor getDescriptor() {
                return new McpProtocol.ToolDescriptor(
                    "auditIndexPerformance",
                    "Analyzes index bloat and scan latency on a specific table",
                    Map.of(
                        "type", "object",
                        "properties", Map.of("tableName", Map.of("type", "string", "description", "Target table name")),
                        "required", java.util.List.of("tableName")
                    )
                );
            }

            @Override
            public String execute(Map<String, Object> arguments) {
                String table = (String) arguments.getOrDefault("tableName", "orders");
                return "{\"table\": \"" + table + "\", \"indexStatus\": \"OPTIMAL\", \"scanTimeMs\": 1.4, \"bloatRatio\": 0.04}";
            }
        });

        return server;
    }
}
