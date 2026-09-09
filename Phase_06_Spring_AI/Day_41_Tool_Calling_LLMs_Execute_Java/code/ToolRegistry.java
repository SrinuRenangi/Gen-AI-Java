package com.genai.springai.tools;

import java.util.*;

/**
 * Registry managing tool definitions, schema exports, and safe execution dispatching.
 */
public class ToolRegistry {

    private final Map<String, FunctionTool> tools = new LinkedHashMap<>();

    public ToolRegistry register(FunctionTool tool) {
        tools.put(tool.getDefinition().name(), tool);
        return this;
    }

    public Optional<FunctionTool> getTool(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public List<ToolDefinition> getDefinitions() {
        return new ArrayList<>(tools.values().stream().map(FunctionTool::getDefinition).toList());
    }

    public String execute(String toolName, Map<String, Object> arguments) {
        FunctionTool tool = tools.get(toolName);
        if (tool == null) {
            return String.format("{\"error\": \"Tool '%s' is not registered in ToolRegistry.\"}", toolName);
        }
        try {
            return tool.execute(arguments);
        } catch (Exception e) {
            return String.format("{\"error\": \"Execution failure in tool '%s': %s\"}", toolName, e.getMessage());
        }
    }

    public String exportJsonSchemas() {
        StringBuilder sb = new StringBuilder("[\n");
        int count = 0;
        for (ToolDefinition def : getDefinitions()) {
            if (count > 0) sb.append(",\n");
            sb.append("  {\n");
            sb.append("    \"name\": \"").append(def.name()).append("\",\n");
            sb.append("    \"description\": \"").append(def.description()).append("\",\n");
            sb.append("    \"parameters\": {\n");
            sb.append("      \"type\": \"object\",\n");
            sb.append("      \"properties\": {\n");
            int pCount = 0;
            for (var entry : def.parameters().entrySet()) {
                if (pCount > 0) sb.append(",\n");
                sb.append("        \"").append(entry.getKey()).append("\": {");
                sb.append("\"type\": \"").append(entry.getValue().type()).append("\", ");
                sb.append("\"description\": \"").append(entry.getValue().description()).append("\"}");
                pCount++;
            }
            sb.append("\n      }\n");
            sb.append("    }\n");
            sb.append("  }");
            count++;
        }
        sb.append("\n]");
        return sb.toString();
    }
}
