package com.genai.enterprise.graduation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Enterprise Competency Matrix tracking the learner's skill mastery across all 9 curriculum phases.
 */
public class EngineerCompetencyMatrix {

    public record PhaseMastery(String phaseName, int daysCompleted, int totalDays, String coreCompetencies) {}

    public static Map<Integer, PhaseMastery> getCurriculumMastery() {
        Map<Integer, PhaseMastery> map = new LinkedHashMap<>();
        map.put(1, new PhaseMastery("Phase 1: Java Foundations", 8, 8,
                "OOP, Memory Model, Generics, Records, Streams, Virtual Threads, HTTP Client"));
        map.put(2, new PhaseMastery("Phase 2: Spring Core & DI", 6, 6,
                "IoC Container, Bean Lifecycle, Deep DI, Auto-Configuration, AOP, Actuator"));
        map.put(3, new PhaseMastery("Phase 3: Spring Web REST APIs", 6, 6,
                "REST Controllers, DTOs, Global Error Handling, SSE Streaming, OpenAPI, MockMvc"));
        map.put(4, new PhaseMastery("Phase 4: Spring Data JPA & Databases", 6, 6,
                "Hibernate ORM, Query Methods, Entity Fetching, Transactions, Flyway, pgvector"));
        map.put(5, new PhaseMastery("Phase 5: Spring Security", 5, 5,
                "SecurityFilterChain, JWT from scratch, RBAC Method Security, OAuth2, API Filters"));
        map.put(6, new PhaseMastery("Phase 6: Spring AI Framework", 11, 11,
                "ChatClient Fluent API, Structured Outputs, Embeddings, Vector Stores, RAG, Tools, Multimodal"));
        map.put(7, new PhaseMastery("Phase 7: LangChain4j Ecosystem", 7, 7,
                "AiServices, Conversational Memory, Guardrails, Advanced RAG, Tool Calling, ReAct Agent"));
        map.put(8, new PhaseMastery("Phase 8: Enterprise Production", 6, 6,
                "MCP Protocol, Injection Defense, OpenTelemetry/Langfuse, Cost Caching, Docker, Capstone"));
        map.put(9, new PhaseMastery("Phase 9: Advanced Topics & Graduation", 5, 5,
                "Ollama Local Sovereign AI, Multi-Agent Supervisor, RAG Triad Evals, HNSW Tuning, Career"));
        return map;
    }

    public static String evaluateSeniorityLevel() {
        int totalDays = getCurriculumMastery().values().stream().mapToInt(PhaseMastery::daysCompleted).sum();
        if (totalDays == 60) {
            return "Senior Enterprise Java Generative AI Systems Engineer (3+ Years Experience Equivalent)";
        } else if (totalDays >= 45) {
            return "Mid-Level Java AI Engineer";
        } else {
            return "Associate Java Developer";
        }
    }
}
