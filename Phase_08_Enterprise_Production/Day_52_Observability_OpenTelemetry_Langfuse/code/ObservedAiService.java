package com.genai.enterprise.observability;

/**
 * Enterprise service demonstrating OpenTelemetry semantic conventions in a RAG + Tool execution workflow.
 */
public class ObservedAiService {

    private final TelemetryCollector collector;

    public ObservedAiService(TelemetryCollector collector) {
        this.collector = collector;
    }

    public String handleUserQuery(String userId, String userQuery) {
        // Ensure trace context exists
        AiTraceContext.getTraceId();
        AiSpan rootSpan = AiTraceContext.startSpan("enterprise.rag.pipeline");
        rootSpan.setAttribute("user.id", userId);
        rootSpan.setAttribute("query.length", userQuery.length());

        try {
            // 1. Guardrail / Injection Scan
            AiSpan guardSpan = AiTraceContext.startSpan("security.prompt_guard");
            simulateWork(15);
            guardSpan.setAttribute("guard.result", "PASSED");
            guardSpan.setAttribute("guard.risk_score", 0.02);
            AiTraceContext.endCurrentSpan(collector);

            // 2. Query Embedding Generation
            AiSpan embedSpan = AiTraceContext.startSpan("rag.embedding_generation");
            simulateWork(40);
            int embedTokens = 18;
            double embedCost = TokenCostCalculator.calculateCost("text-embedding-3-small", embedTokens, 0);
            embedSpan.setAttribute("ai.model", "text-embedding-3-small");
            embedSpan.setAttribute("ai.prompt.tokens", embedTokens);
            embedSpan.setAttribute("ai.total.tokens", embedTokens);
            embedSpan.setAttribute("ai.cost.usd", embedCost);
            AiTraceContext.endCurrentSpan(collector);

            // 3. Vector Database Retrieval
            AiSpan vectorSpan = AiTraceContext.startSpan("rag.vector_database_search");
            simulateWork(35);
            vectorSpan.setAttribute("db.system", "postgresql_pgvector");
            vectorSpan.setAttribute("db.index", "hnsw_cosine_idx");
            vectorSpan.setAttribute("rag.top_k", 3);
            vectorSpan.setAttribute("rag.min_score", 0.88);
            AiTraceContext.endCurrentSpan(collector);

            // 4. LLM Generation
            AiSpan llmSpan = AiTraceContext.startSpan("llm.chat_completion");
            simulateWork(180);
            String model = "gpt-4o";
            int promptTokens = 420;
            int completionTokens = 95;
            double llmCost = TokenCostCalculator.calculateCost(model, promptTokens, completionTokens);
            llmSpan.setAttribute("ai.model", model);
            llmSpan.setAttribute("ai.system", "openai");
            llmSpan.setAttribute("ai.prompt.tokens", promptTokens);
            llmSpan.setAttribute("ai.completion.tokens", completionTokens);
            llmSpan.setAttribute("ai.total.tokens", promptTokens + completionTokens);
            llmSpan.setAttribute("ai.cost.usd", llmCost);

            // 4.1 Nested Tool Call inside LLM processing
            AiSpan toolSpan = AiTraceContext.startSpan("tool.execution.fetch_account_balance");
            simulateWork(25);
            toolSpan.setAttribute("tool.name", "FetchAccountBalance");
            toolSpan.setAttribute("tool.status", "SUCCESS");
            toolSpan.setAttribute("tool.account_id", "ACC-99214");
            AiTraceContext.endCurrentSpan(collector); // ends toolSpan

            AiTraceContext.endCurrentSpan(collector); // ends llmSpan

            return "Your account balance for ACC-99214 is $14,250.00 USD as verified by real-time bank ledger.";

        } finally {
            AiTraceContext.endCurrentSpan(collector); // ends rootSpan
        }
    }

    private void simulateWork(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
