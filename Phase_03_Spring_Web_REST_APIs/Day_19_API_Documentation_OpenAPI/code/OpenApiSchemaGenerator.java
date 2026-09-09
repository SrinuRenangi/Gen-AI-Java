package code;

import java.util.List;
import java.util.Map;

/**
 * Generates OpenAPI 3.1 JSON specifications and LLM Tool-Calling JSON Schemas.
 *
 * Demonstrates:
 * 1. Building the official OpenAPI 3.1 specification for Spring Boot AI endpoints.
 * 2. Exporting OpenAI / Claude compatible Tool-Calling schemas from the same contract.
 * 3. Documenting enterprise constraints (min, max, enums, required fields).
 */
public class OpenApiSchemaGenerator {

    /**
     * Builds the complete OpenAPI 3.1 document as formatted JSON.
     */
    public String generateOpenApiJson(
        ChatCompletionApiSpec.ApiInfo info,
        List<ChatCompletionApiSpec.ServerInfo> servers,
        List<ChatCompletionApiSpec.OperationDoc> operations,
        Map<String, List<ChatCompletionApiSpec.SchemaProperty>> schemas
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"openapi\": \"3.1.0\",\n");

        // 1. Info block
        sb.append("  \"info\": {\n");
        sb.append("    \"title\": \"").append(escape(info.title())).append("\",\n");
        sb.append("    \"version\": \"").append(escape(info.version())).append("\",\n");
        sb.append("    \"description\": \"").append(escape(info.description())).append("\",\n");
        sb.append("    \"contact\": { \"email\": \"").append(escape(info.contactEmail())).append("\" },\n");
        sb.append("    \"license\": { \"name\": \"").append(escape(info.licenseName())).append("\" }\n");
        sb.append("  },\n");

        // 2. Servers block
        sb.append("  \"servers\": [\n");
        for (int i = 0; i < servers.size(); i++) {
            var s = servers.get(i);
            sb.append("    { \"url\": \"").append(s.url()).append("\", \"description\": \"").append(escape(s.description())).append("\" }")
              .append(i < servers.size() - 1 ? "," : "").append("\n");
        }
        sb.append("  ],\n");

        // 3. Paths block
        sb.append("  \"paths\": {\n");
        for (int i = 0; i < operations.size(); i++) {
            var op = operations.get(i);
            sb.append("    \"").append(op.path()).append("\": {\n");
            sb.append("      \"").append(op.method().toLowerCase()).append("\": {\n");
            sb.append("        \"tags\": [\"").append(escape(op.tag())).append("\"],\n");
            sb.append("        \"summary\": \"").append(escape(op.summary())).append("\",\n");
            sb.append("        \"description\": \"").append(escape(op.description())).append("\",\n");
            if (op.requestBodySchema() != null) {
                sb.append("        \"requestBody\": {\n");
                sb.append("          \"required\": true,\n");
                sb.append("          \"content\": {\n");
                sb.append("            \"application/json\": {\n");
                sb.append("              \"schema\": { \"$ref\": \"#/components/schemas/").append(op.requestBodySchema()).append("\" }\n");
                sb.append("            }\n");
                sb.append("          }\n");
                sb.append("        },\n");
            }
            sb.append("        \"responses\": {\n");
            int rCount = 0;
            for (Map.Entry<Integer, String> resp : op.responses().entrySet()) {
                rCount++;
                sb.append("          \"").append(resp.getKey()).append("\": {\n");
                sb.append("            \"description\": \"").append(escape(resp.getValue())).append("\"\n");
                sb.append("          }").append(rCount < op.responses().size() ? "," : "").append("\n");
            }
            sb.append("        }\n");
            sb.append("      }\n");
            sb.append("    }").append(i < operations.size() - 1 ? "," : "").append("\n");
        }
        sb.append("  },\n");

        // 4. Components / Schemas block
        sb.append("  \"components\": {\n");
        sb.append("    \"schemas\": {\n");
        int sIdx = 0;
        for (Map.Entry<String, List<ChatCompletionApiSpec.SchemaProperty>> entry : schemas.entrySet()) {
            sIdx++;
            sb.append("      \"").append(entry.getKey()).append("\": {\n");
            sb.append("        \"type\": \"object\",\n");

            // Required list
            List<String> required = entry.getValue().stream()
                .filter(ChatCompletionApiSpec.SchemaProperty::required)
                .map(ChatCompletionApiSpec.SchemaProperty::name)
                .toList();
            if (!required.isEmpty()) {
                sb.append("        \"required\": [");
                for (int r = 0; r < required.size(); r++) {
                    sb.append("\"").append(required.get(r)).append("\"").append(r < required.size() - 1 ? ", " : "");
                }
                sb.append("],\n");
            }

            // Properties list
            sb.append("        \"properties\": {\n");
            for (int p = 0; p < entry.getValue().size(); p++) {
                var prop = entry.getValue().get(p);
                sb.append("          \"").append(prop.name()).append("\": {\n");
                sb.append("            \"type\": \"").append(prop.type()).append("\",\n");
                sb.append("            \"description\": \"").append(escape(prop.description())).append("\"");
                if (prop.example() != null) {
                    sb.append(",\n            \"example\": ").append(formatExample(prop.example()));
                }
                if (prop.minimum() != null) {
                    sb.append(",\n            \"minimum\": ").append(prop.minimum());
                }
                if (prop.maximum() != null) {
                    sb.append(",\n            \"maximum\": ").append(prop.maximum());
                }
                if (prop.enumValues() != null && !prop.enumValues().isEmpty()) {
                    sb.append(",\n            \"enum\": [");
                    for (int e = 0; e < prop.enumValues().size(); e++) {
                        sb.append("\"").append(prop.enumValues().get(e)).append("\"")
                          .append(e < prop.enumValues().size() - 1 ? ", " : "");
                    }
                    sb.append("]");
                }
                sb.append("\n          }").append(p < entry.getValue().size() - 1 ? "," : "").append("\n");
            }
            sb.append("        }\n");
            sb.append("      }").append(sIdx < schemas.size() ? "," : "").append("\n");
        }
        sb.append("    }\n");
        sb.append("  }\n");
        sb.append("}");

        return sb.toString();
    }

    /**
     * Converts a schema definition into OpenAI / Anthropic Function Calling JSON Schema.
     */
    public String generateToolCallingSchema(String functionName, String functionDescription, List<ChatCompletionApiSpec.SchemaProperty> properties) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"type\": \"function\",\n");
        sb.append("  \"function\": {\n");
        sb.append("    \"name\": \"").append(functionName).append("\",\n");
        sb.append("    \"description\": \"").append(escape(functionDescription)).append("\",\n");
        sb.append("    \"parameters\": {\n");
        sb.append("      \"type\": \"object\",\n");

        List<String> required = properties.stream().filter(ChatCompletionApiSpec.SchemaProperty::required).map(ChatCompletionApiSpec.SchemaProperty::name).toList();
        if (!required.isEmpty()) {
            sb.append("      \"required\": [");
            for (int r = 0; r < required.size(); r++) {
                sb.append("\"").append(required.get(r)).append("\"").append(r < required.size() - 1 ? ", " : "");
            }
            sb.append("],\n");
        }

        sb.append("      \"properties\": {\n");
        for (int p = 0; p < properties.size(); p++) {
            var prop = properties.get(p);
            sb.append("        \"").append(prop.name()).append("\": {\n");
            sb.append("          \"type\": \"").append(prop.type()).append("\",\n");
            sb.append("          \"description\": \"").append(escape(prop.description())).append("\"");
            if (prop.enumValues() != null && !prop.enumValues().isEmpty()) {
                sb.append(",\n          \"enum\": [");
                for (int e = 0; e < prop.enumValues().size(); e++) {
                    sb.append("\"").append(prop.enumValues().get(e)).append("\"").append(e < prop.enumValues().size() - 1 ? ", " : "");
                }
                sb.append("]");
            }
            sb.append("\n        }").append(p < properties.size() - 1 ? "," : "").append("\n");
        }
        sb.append("      }\n");
        sb.append("    }\n");
        sb.append("  }\n");
        sb.append("}");
        return sb.toString();
    }

    private static String formatExample(Object ex) {
        if (ex instanceof Number || ex instanceof Boolean) return ex.toString();
        return "\"" + escape(ex.toString()) + "\"";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"").replace("\n", "\\n");
    }
}
