/**
 * Day 01 - Exercise 1: Gen AI Hardware & Runtime Inspector
 * Inspects system capabilities for running local LLMs (Ollama) vs Cloud APIs.
 */
public class GenAIRuntimeInfo {

    public static void main(String[] args) {
        String javaVersion = System.getProperty("java.version");
        String javaVendor = System.getProperty("java.vendor");
        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");

        int cores = Runtime.getRuntime().availableProcessors();
        long maxMemoryBytes = Runtime.getRuntime().maxMemory();
        long maxMemoryMB = maxMemoryBytes / (1024 * 1024);
        double maxMemoryGB = maxMemoryMB / 1024.0;

        System.out.println("==================================================");
        System.out.println("       ENTERPRISE GEN AI HOST DIAGNOSTIC          ");
        System.out.println("==================================================");
        System.out.printf("Java Runtime : %s (%s)%n", javaVersion, javaVendor);
        System.out.printf("Operating Sys: %s (%s)%n", osName, osArch);
        System.out.printf("CPU Cores    : %d threads available%n", cores);
        System.out.printf("Max Memory   : %d MB (%.2f GB)%n", maxMemoryMB, maxMemoryGB);
        System.out.println("--------------------------------------------------");

        if (cores >= 4 && maxMemoryMB >= 2048) {
            System.out.println("✅ [STATUS]: Host is READY for local Ollama LLM execution!");
        } else {
            System.out.println("⚠️  [STATUS]: Host resources are constrained for large local models.");
            System.out.println("   Recommendation: Use cloud endpoints (OpenAI/Anthropic) or 1B quant models.");
        }
        System.out.println("==================================================");
    }
}
