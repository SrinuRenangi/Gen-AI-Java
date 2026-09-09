package code;

import java.util.List;
import java.util.Map;

/**
 * Metadata model representing OpenAPI 3.1 documentation structures.
 *
 * Models:
 * - OpenAPI root (info, servers, paths, components)
 * - Operations (@Operation, @Tag, @ApiResponse)
 * - Schema properties (@Schema, types, descriptions, examples, constraints)
 */
public class ChatCompletionApiSpec {

    public record ApiInfo(
        String title,
        String version,
        String description,
        String contactEmail,
        String licenseName
    ) {}

    public record ServerInfo(String url, String description) {}

    public record SchemaProperty(
        String name,
        String type,
        String description,
        Object example,
        boolean required,
        List<String> enumValues,
        Double minimum,
        Double maximum
    ) {}

    public record OperationDoc(
        String method,
        String path,
        String summary,
        String description,
        String tag,
        String requestBodySchema,
        Map<Integer, String> responses
    ) {}
}
