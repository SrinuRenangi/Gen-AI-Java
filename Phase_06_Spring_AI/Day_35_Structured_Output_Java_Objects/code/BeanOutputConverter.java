package com.genai.springai.structured;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;

/**
 * Simulates Spring AI's BeanOutputConverter<T>.
 * 1. Generates getFormat() schema directives for prompts.
 * 2. Parses model JSON output into strongly-typed Java records.
 */
public class BeanOutputConverter<T> {

    private final Class<T> targetClass;

    public BeanOutputConverter(Class<T> targetClass) {
        this.targetClass = targetClass;
    }

    public String getFormat() {
        String schema = JsonSchemaGenerator.generateSchema(targetClass);
        return """
            Your response should be in JSON format.
            Do not include any explanations, markdown code fences, or text outside the JSON.
            Adhere strictly to this JSON Schema:
            %s
            """.formatted(schema);
    }

    @SuppressWarnings("unchecked")
    public T convert(String rawLlmResponse) {
        String cleanJson = MarkdownJsonSanitizer.clean(rawLlmResponse);

        if (!targetClass.isRecord()) {
            throw new UnsupportedOperationException("Simulation supports Java 21 Records directly.");
        }

        try {
            RecordComponent[] components = targetClass.getRecordComponents();
            Class<?>[] paramTypes = new Class<?>[components.length];
            Object[] args = new Object[components.length];

            for (int i = 0; i < components.length; i++) {
                RecordComponent rc = components[i];
                paramTypes[i] = rc.getType();
                args[i] = extractFieldFromJson(cleanJson, rc.getName(), rc.getType());
            }

            Constructor<T> constructor = targetClass.getDeclaredConstructor(paramTypes);
            constructor.setAccessible(true);
            return constructor.newInstance(args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert LLM output into record: " + targetClass.getSimpleName(), e);
        }
    }

    private Object extractFieldFromJson(String json, String fieldName, Class<?> type) {
        // Match "fieldName" \s* : \s* ...
        java.util.regex.Pattern stringPat = java.util.regex.Pattern.compile("\"" + fieldName + "\"\\s*:\\s*\"([^\"]*)\"");
        java.util.regex.Matcher m = stringPat.matcher(json);
        if (type == String.class) {
            if (m.find()) {
                return m.group(1);
            }
            return "";
        }

        if (type == boolean.class || type == Boolean.class) {
            java.util.regex.Pattern boolPat = java.util.regex.Pattern.compile("\"" + fieldName + "\"\\s*:\\s*(true|false)");
            java.util.regex.Matcher bm = boolPat.matcher(json);
            if (bm.find()) {
                return Boolean.parseBoolean(bm.group(1));
            }
            return false;
        }

        if (type == double.class || type == Double.class) {
            java.util.regex.Pattern numPat = java.util.regex.Pattern.compile("\"" + fieldName + "\"\\s*:\\s*([0-9.]+)");
            java.util.regex.Matcher nm = numPat.matcher(json);
            if (nm.find()) {
                return Double.parseDouble(nm.group(1));
            }
            return 0.0;
        }

        if (type == int.class || type == Integer.class) {
            java.util.regex.Pattern intPat = java.util.regex.Pattern.compile("\"" + fieldName + "\"\\s*:\\s*([0-9]+)");
            java.util.regex.Matcher im = intPat.matcher(json);
            if (im.find()) {
                return Integer.parseInt(im.group(1));
            }
            return 0;
        }

        if (type == List.class) {
            java.util.regex.Pattern listPat = java.util.regex.Pattern.compile("\"" + fieldName + "\"\\s*:\\s*\\[([^\\]]*)\\]");
            java.util.regex.Matcher lm = listPat.matcher(json);
            if (lm.find()) {
                String content = lm.group(1).trim();
                if (content.isEmpty()) return List.of();
                String[] items = content.split(",");
                List<String> result = new ArrayList<>();
                for (String item : items) {
                    result.add(item.replace("\"", "").trim());
                }
                return result;
            }
            return List.of();
        }
        return null;
    }
}
