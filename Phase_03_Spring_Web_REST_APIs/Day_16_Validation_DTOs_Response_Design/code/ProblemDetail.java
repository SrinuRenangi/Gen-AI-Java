package code;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RFC 7807 Problem Details for HTTP APIs.
 *
 * Implements the official IETF standard adopted by Spring Boot 3+:
 * - type: URI identifying the problem type (e.g. "https://api.ai-gateway.io/errors/validation-failed")
 * - title: Short, human-readable summary of problem
 * - status: HTTP status code (e.g. 400, 422, 500)
 * - detail: Human-readable explanation specific to this occurrence
 * - instance: URI reference identifying the specific occurrence (e.g. "/api/v1/completions/req_9872")
 * - invalidParams: List of specific field-level validation errors
 * - properties: Extensible map for audit tracking (e.g. timestamp, traceId)
 */
public class ProblemDetail {

    private URI type;
    private String title;
    private int status;
    private String detail;
    private URI instance;
    private final List<InvalidParam> invalidParams = new ArrayList<>();
    private final Map<String, Object> properties = new LinkedHashMap<>();

    public record InvalidParam(String name, String reason, Object rejectedValue) {}

    public ProblemDetail() {
        this.properties.put("timestamp", Instant.now().toString());
    }

    public static ProblemDetail forStatusAndDetail(int status, String detail) {
        ProblemDetail pd = new ProblemDetail();
        pd.status = status;
        pd.detail = detail;
        pd.title = switch (status) {
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 422 -> "Unprocessable Entity";
            case 429 -> "Too Many Requests";
            case 500 -> "Internal Server Error";
            default -> "HTTP " + status;
        };
        pd.type = URI.create("about:blank");
        return pd;
    }

    public static ProblemDetail forValidationFailure(String detail, String instancePath) {
        ProblemDetail pd = forStatusAndDetail(422, detail);
        pd.setTitle("Validation Failed");
        pd.setType(URI.create("https://api.enterprise-ai.internal/errors/validation-failed"));
        pd.setInstance(URI.create(instancePath));
        return pd;
    }

    public void addInvalidParam(String name, String reason, Object rejectedValue) {
        invalidParams.add(new InvalidParam(name, reason, rejectedValue));
    }

    public URI getType() { return type; }
    public void setType(URI type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public URI getInstance() { return instance; }
    public void setInstance(URI instance) { this.instance = instance; }

    public List<InvalidParam> getInvalidParams() {
        return Collections.unmodifiableList(invalidParams);
    }

    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public void setProperty(String key, Object value) {
        this.properties.put(key, value);
    }

    /**
     * Formats the RFC 7807 Problem Details object as a clean JSON representation.
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"type\": \"").append(type).append("\",\n");
        sb.append("  \"title\": \"").append(title).append("\",\n");
        sb.append("  \"status\": ").append(status).append(",\n");
        sb.append("  \"detail\": \"").append(escapeJson(detail)).append("\",\n");
        if (instance != null) {
            sb.append("  \"instance\": \"").append(instance).append("\",\n");
        }
        if (!invalidParams.isEmpty()) {
            sb.append("  \"invalid-params\": [\n");
            for (int i = 0; i < invalidParams.size(); i++) {
                InvalidParam ip = invalidParams.get(i);
                sb.append("    {\n");
                sb.append("      \"name\": \"").append(ip.name()).append("\",\n");
                sb.append("      \"reason\": \"").append(escapeJson(ip.reason())).append("\",\n");
                sb.append("      \"rejectedValue\": ").append(formatRejectedValue(ip.rejectedValue())).append("\n");
                sb.append("    }").append(i < invalidParams.size() - 1 ? "," : "").append("\n");
            }
            sb.append("  ],\n");
        }
        sb.append("  \"properties\": {\n");
        int count = 0;
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            count++;
            sb.append("    \"").append(entry.getKey()).append("\": \"")
              .append(escapeJson(String.valueOf(entry.getValue()))).append("\"")
              .append(count < properties.size() ? "," : "").append("\n");
        }
        sb.append("  }\n");
        sb.append("}");
        return sb.toString();
    }

    private static String formatRejectedValue(Object val) {
        if (val == null) return "null";
        if (val instanceof Number || val instanceof Boolean) return val.toString();
        return "\"" + escapeJson(val.toString()) + "\"";
    }

    private static String escapeJson(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r");
    }
}
