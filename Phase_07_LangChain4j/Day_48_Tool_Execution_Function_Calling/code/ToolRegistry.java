package com.genai.langchain4j.tools;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * Registry scanning Java objects for @Tool annotations, generating ToolSpecifications,
 * and dispatching ToolExecutionRequests safely with exception containment.
 */
public class ToolRegistry {

    private record ToolHandler(Object target, Method method, ToolSpecification spec) {}

    private final Map<String, ToolHandler> handlers = new LinkedHashMap<>();

    public ToolRegistry registerTools(Object toolObject) {
        for (Method m : toolObject.getClass().getDeclaredMethods()) {
            if (m.isAnnotationPresent(Tool.class)) {
                Tool toolAnn = m.getAnnotation(Tool.class);
                String toolName = !toolAnn.name().isBlank() ? toolAnn.name() : m.getName();
                String description = toolAnn.value();

                Map<String, ToolSpecification.ParameterInfo> paramMap = new LinkedHashMap<>();
                for (Parameter p : m.getParameters()) {
                    String pName = p.getName();
                    String pDesc = p.isAnnotationPresent(P.class) ? p.getAnnotation(P.class).value() : "parameter " + pName;
                    paramMap.put(pName, new ToolSpecification.ParameterInfo(p.getType().getSimpleName().toLowerCase(), pDesc));
                }

                ToolSpecification spec = new ToolSpecification(toolName, description, paramMap);
                handlers.put(toolName, new ToolHandler(toolObject, m, spec));
            }
        }
        return this;
    }

    public List<ToolSpecification> getSpecifications() {
        return handlers.values().stream().map(ToolHandler::spec).toList();
    }

    public ToolExecutionResultMessage execute(ToolExecutionRequest request) {
        ToolHandler handler = handlers.get(request.name());
        if (handler == null) {
            return new ToolExecutionResultMessage(request.id(), request.name(), "{\"error\": \"No tool registered with name: " + request.name() + "\"}");
        }

        try {
            Method m = handler.method();
            Parameter[] params = m.getParameters();
            Object[] args = new Object[params.length];

            for (int i = 0; i < params.length; i++) {
                String pName = params[i].getName();
                Object raw = request.arguments().get(pName);
                if (raw == null && !request.arguments().isEmpty()) {
                    // Fallback to order-based argument match
                    raw = new ArrayList<>(request.arguments().values()).get(Math.min(i, request.arguments().size() - 1));
                }
                args[i] = coerce(raw, params[i].getType());
            }

            m.setAccessible(true);
            Object result = m.invoke(handler.target(), args);
            return new ToolExecutionResultMessage(request.id(), request.name(), String.valueOf(result));

        } catch (Exception ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            return new ToolExecutionResultMessage(request.id(), request.name(), "{\"error\": \"" + cause.getClass().getSimpleName() + ": " + cause.getMessage() + "\"}");
        }
    }

    private Object coerce(Object raw, Class<?> targetType) {
        if (raw == null) return null;
        if (targetType == String.class) return String.valueOf(raw);
        if (targetType == int.class || targetType == Integer.class) return ((Number) raw).intValue();
        if (targetType == double.class || targetType == Double.class) return ((Number) raw).doubleValue();
        if (targetType == boolean.class || targetType == Boolean.class) return Boolean.parseBoolean(raw.toString());
        return raw;
    }
}
