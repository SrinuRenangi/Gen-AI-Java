package code;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Enterprise MockMvc and Web Test Harness Simulator.
 *
 * Recreates Spring Boot's MockMvc fluent API for testing controllers:
 * - mockMvc.perform(post("/path").content(json).header(...))
 *   .andExpect(status(200))
 *   .andExpect(header("Retry-After", "30"))
 *   .andExpect(jsonField("errorCode", "AI_RATE_LIMIT_EXCEEDED"));
 */
public class MockMvcSimulator {

    public static class TestRequest {
        private final String method;
        private final String path;
        private String content = "";
        private final Map<String, String> headers = new HashMap<>();

        public TestRequest(String method, String path) {
            this.method = method;
            this.path = path;
        }

        public TestRequest content(String json) {
            this.content = json;
            return this;
        }

        public TestRequest header(String name, String value) {
            this.headers.put(name, value);
            return this;
        }

        public String getMethod() { return method; }
        public String getPath() { return path; }
        public String getContent() { return content; }
        public Map<String, String> getHeaders() { return headers; }
    }

    public static class TestResponse {
        private final int status;
        private final Map<String, String> headers;
        private final String body;

        public TestResponse(int status, Map<String, String> headers, String body) {
            this.status = status;
            this.headers = headers != null ? headers : Map.of();
            this.body = body != null ? body : "";
        }

        public int getStatus() { return status; }
        public String getHeader(String name) { return headers.get(name); }
        public String getBody() { return body; }

        public TestResponse andExpectStatus(int expectedStatus) {
            if (this.status != expectedStatus) {
                throw new AssertionError("Status mismatch: expected <" + expectedStatus + "> but was <" + this.status + ">\nBody: " + body);
            }
            return this;
        }

        public TestResponse andExpectHeader(String name, String expectedValue) {
            String actual = this.headers.get(name);
            if (!expectedValue.equals(actual)) {
                throw new AssertionError("Header mismatch for '" + name + "': expected <" + expectedValue + "> but was <" + actual + ">");
            }
            return this;
        }

        public TestResponse andExpectBodyContains(String snippet) {
            if (!this.body.contains(snippet)) {
                throw new AssertionError("Body does not contain expected snippet: <" + snippet + ">\nActual body: " + body);
            }
            return this;
        }
    }

    private final Function<TestRequest, TestResponse> dispatcher;

    public MockMvcSimulator(Function<TestRequest, TestResponse> dispatcher) {
        this.dispatcher = dispatcher;
    }

    public static TestRequest post(String path) {
        return new TestRequest("POST", path);
    }

    public static TestRequest get(String path) {
        return new TestRequest("GET", path);
    }

    public TestResponse perform(TestRequest request) {
        return dispatcher.apply(request);
    }
}
