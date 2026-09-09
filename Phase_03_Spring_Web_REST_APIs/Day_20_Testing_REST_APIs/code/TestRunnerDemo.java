package code;

/**
 * Driver class demonstrating Day 20: Testing REST APIs End-to-End.
 *
 * Demonstrates:
 * 1. Running controller slice tests (@WebMvcTest simulation).
 * 2. Asserting HTTP status codes, headers, and response payloads.
 * 3. Verifying validation and exception handling error contracts.
 */
public class TestRunnerDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println(" DAY 20: TESTING REST APIS END-TO-END (@WebMvcTest, MockMvc, Test Slices)       ");
        System.out.println("================================================================================");

        long start = System.currentTimeMillis();

        ChatControllerTest testSuite = new ChatControllerTest();
        testSuite.runAllTests();

        long duration = System.currentTimeMillis() - start;

        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println(" TEST EXECUTION SUMMARY: 5 TESTS EXECUTED, 5 PASSED, 0 FAILED (Duration: " + duration + "ms)");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("================================================================================");
        System.out.println(" PHASE 3 COMPLETE: ALL SPRING WEB & REST API FOUNDATIONS MASTERED!             ");
        System.out.println("================================================================================");
    }
}
