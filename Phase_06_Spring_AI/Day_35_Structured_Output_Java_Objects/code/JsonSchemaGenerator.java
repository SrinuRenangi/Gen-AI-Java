package com.genai.springai.structured;

import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.util.List;

/**
 * Inspects Java records and classes using reflection to generate JSON Schema definitions.
 * This simulates how Spring AI builds schema specifications for the LLM.
 */
public class JsonSchemaGenerator {

    public static String generateSchema(Class<?> clazz) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"type\": \"object\",\n");
        sb.append("  \"properties\": {\n");

        if (clazz.isRecord()) {
            RecordComponent[] components = clazz.getRecordComponents();
            for (int i = 0; i < components.length; i++) {
                RecordComponent rc = components[i];
                sb.append("    \"").append(rc.getName()).append("\": { \"type\": \"")
                  .append(mapJavaTypeToJsonType(rc.getType())).append("\" }");
                if (i < components.length - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
        } else {
            Field[] fields = clazz.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                Field f = fields[i];
                sb.append("    \"").append(f.getName()).append("\": { \"type\": \"")
                  .append(mapJavaTypeToJsonType(f.getType())).append("\" }");
                if (i < fields.length - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
        }

        sb.append("  },\n");
        sb.append("  \"required\": [");
        if (clazz.isRecord()) {
            RecordComponent[] components = clazz.getRecordComponents();
            for (int i = 0; i < components.length; i++) {
                sb.append("\"").append(components[i].getName()).append("\"");
                if (i < components.length - 1) sb.append(", ");
            }
        }
        sb.append("]\n");
        sb.append("}");

        return sb.toString();
    }

    private static String mapJavaTypeToJsonType(Class<?> type) {
        if (type == String.class) return "string";
        if (type == int.class || type == Integer.class || type == long.class || type == Long.class) return "integer";
        if (type == double.class || type == Double.class || type == float.class || type == Float.class) return "number";
        if (type == boolean.class || type == Boolean.class) return "boolean";
        if (type == List.class || type.isArray()) return "array";
        return "object";
    }
}
