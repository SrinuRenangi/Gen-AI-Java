package com.javagenai.day11;

public class DIDeepDemo {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   DAY 11: DEPENDENCY INJECTION IN-DEPTH DEMO     ");
        System.out.println("==================================================");

        // 1. Ambiguity Resolution with Simulated @Qualifier
        System.out.println("1. Multi-Model Qualifier Routing:");
        ChatModel fastOllama = prompt -> "[Ollama Llama-3.2-1B] Quick response to: " + prompt;
        ChatModel deepGpt4o = prompt -> "[OpenAI GPT-4o-Heavy] Deep reasoning analysis of: " + prompt;

        AIModelRouter router = new AIModelRouter(fastOllama, deepGpt4o);

        // Short prompt -> routes to fast model
        String shortAns = router.routeAndExecute("What is DI?");
        System.out.println("   " + shortAns);

        // Long prompt -> routes to deep model
        String longAns = router.routeAndExecute("Provide a comprehensive architectural breakdown of Spring IoC container lifecycle hooks.");
        System.out.println("   " + longAns);
        System.out.println();

        // 2. Type-Safe Configuration Properties with Records
        System.out.println("2. Type-Safe AI Config Binding (@ConfigurationProperties):");
        VectorStoreProperties config = new VectorStoreProperties("pgvector.internal.net", 5432, "documents_v1", 1536);
        System.out.printf("   Host       : %s:%d%n", config.host(), config.port());
        System.out.printf("   Index Name : %s%n", config.indexName());
        System.out.printf("   Dimensions : %d (Valid for text-embedding-3-small)%n", config.vectorDimensions());
        System.out.println("==================================================");
    }
}
