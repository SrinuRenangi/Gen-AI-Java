# 🚀 60-Day Java Generative AI Masterclass — From Zero to Senior Engineer

> **For developers who want to master Java, Spring Boot, Spring AI & LangChain4j from scratch.**
>
> Every concept explained through **real-world analogies, runnable Java code, ASCII diagrams, and Mermaid flowcharts.**
> No unexplained annotations. No assumed knowledge. No shortcuts.
>
> After completing this course, your knowledge matches a **3-year experienced Java backend engineer building production AI systems**.

---

## 📋 Course Rules

| Rule | Description |
| :---: | :--- |
| **1** | No concept is introduced without a **real-world analogy FIRST**. |
| **2** | Every concept is shown as **runnable Java code** (no unexplained annotations). |
| **3** | Each day builds **ONLY on what previous days taught**. |
| **4** | **ASCII diagrams, Mermaid flowcharts, and architecture visuals** for every concept. |
| **5** | Every Spring annotation is explained **WHY it exists**, not just how. |
| **6** | Every day includes **exercises with full solutions**. |

---

## 🛠️ Prerequisites & Setup

### What You Need

| Tool | Version | Purpose |
| :--- | :--- | :--- |
| **Java JDK** | 21 (LTS) | Core language runtime |
| **Maven** | 3.9+ | Build tool & dependency management |
| **IntelliJ IDEA** | Community (Free) or Ultimate | IDE (VS Code with Java Extension Pack also works) |
| **Docker Desktop** | Latest | Running PostgreSQL, pgvector, Ollama locally |
| **Git** | Latest | Version control |
| **Ollama** | Latest | Running open-weight LLMs locally (100% free) |

### Quick Setup (Day 01 covers this in detail)

```bash
# 1. Verify Java 21
java --version    # Should show 21.x.x

# 2. Verify Maven
mvn --version     # Should show 3.9+

# 3. Start local infrastructure
docker-compose up -d    # PostgreSQL + pgvector + Ollama

# 4. Pull a local LLM (free, no API key needed!)
ollama pull llama3.2
```

---

## 🗺️ 60-Day Roadmap

### 🟢 Phase 1: Java Foundations for AI Engineers (Days 01–08)

> *From basic syntax to Virtual Threads — every concept taught through the lens of "you'll need this when building AI systems."*

| Day | Topic | Status |
| :---: | :--- | :---: |
| [Day 01](Phase_01_Java_Foundations/Day_01_Java_Ecosystem_and_Setup/Day_01_Java_Ecosystem_and_Setup.md) | Java Ecosystem & Setup — JDK 21, Maven, Project Structure | ✅ Complete |
| [Day 02](Phase_01_Java_Foundations/Day_02_OOP_Classes_Objects_Memory/Day_02_OOP_Classes_Objects_Memory.md) | OOP — Classes, Objects & Memory (Stack vs Heap) | ✅ Complete |
| [Day 03](Phase_01_Java_Foundations/Day_03_Inheritance_Interfaces_Polymorphism/Day_03_Inheritance_Interfaces_Polymorphism.md) | Inheritance, Interfaces & Polymorphism | ✅ Complete |
| [Day 04](Phase_01_Java_Foundations/Day_04_Generics_Collections_DataStructures/Day_04_Generics_Collections_DataStructures.md) | Generics, Collections & Data Structures | ✅ Complete |
| [Day 05](Phase_01_Java_Foundations/Day_05_Modern_Java_Records_Optional_Sealed/Day_05_Modern_Java_Records_Optional_Sealed.md) | Modern Java: Records, Optional & Sealed Types | ✅ Complete |
| [Day 06](Phase_01_Java_Foundations/Day_06_Functional_Programming_Streams/Day_06_Functional_Programming_Streams.md) | Functional Programming & Stream API | ✅ Complete |
| [Day 07](Phase_01_Java_Foundations/Day_07_Concurrency_Virtual_Threads/Day_07_Concurrency_Virtual_Threads.md) | Concurrency & Virtual Threads (Project Loom) | ✅ Complete |
| [Day 08](Phase_01_Java_Foundations/Day_08_IO_HTTP_JSON_Testing/Day_08_IO_HTTP_JSON_Testing.md) | I/O, HTTP Client, JSON & Testing (JUnit 5 + Mockito) | ✅ Complete |

### 🟢 Phase 2: Spring Core & Dependency Injection (Days 09–14)

> *Understand WHY Spring exists. Master IoC/DI from scratch. This phase makes all of Spring Boot, Spring AI, and enterprise Java click.*

| Day | Topic | Status |
| :---: | :--- | :---: |
| [Day 09](Phase_02_Spring_Core_and_DI/Day_09_Problem_Spring_Solves_Dependency_Hell/Day_09_Problem_Spring_Solves_Dependency_Hell.md) | The Problem Spring Solves — Dependency Hell | ✅ Complete |
| [Day 10](Phase_02_Spring_Core_and_DI/Day_10_Spring_IoC_Container_Bean_Lifecycle/Day_10_Spring_IoC_Container_Bean_Lifecycle.md) | Spring IoC Container & Bean Lifecycle | ✅ Complete |
| [Day 11](Phase_02_Spring_Core_and_DI/Day_11_Dependency_Injection_In_Depth/Day_11_Dependency_Injection_In_Depth.md) | Dependency Injection In-Depth | ✅ Complete |
| [Day 12](Phase_02_Spring_Core_and_DI/Day_12_Spring_Boot_Auto_Configuration/Day_12_Spring_Boot_Auto_Configuration.md) | Spring Boot Auto-Configuration Magic | ✅ Complete |
| [Day 13](Phase_02_Spring_Core_and_DI/Day_13_AOP_Cross_Cutting_Concerns/Day_13_AOP_Cross_Cutting_Concerns.md) | AOP — Cross-Cutting Concerns | ✅ Complete |
| [Day 14](Phase_02_Spring_Core_and_DI/Day_14_Actuator_Production_Readiness/Day_14_Actuator_Production_Readiness.md) | Spring Boot Actuator & Production Readiness | ✅ Complete |

### 🟢 Phase 3: Spring Web — Building REST APIs (Days 15–20)

| Day | Topic | Status |
| :---: | :--- | :---: |
| [Day 15](Phase_03_Spring_Web_REST_APIs/Day_15_HTTP_Deep_Dive_First_REST_Controller/Day_15_HTTP_Deep_Dive_First_REST_Controller.md) | HTTP Deep Dive & Your First REST Controller | ✅ Complete |
| [Day 16](Phase_03_Spring_Web_REST_APIs/Day_16_Validation_DTOs_Response_Design/Day_16_Validation_DTOs_Response_Design.md) | Request Validation, DTOs & Response Design | ✅ Complete |
| [Day 17](Phase_03_Spring_Web_REST_APIs/Day_17_Exception_Handling_Global_Strategy/Day_17_Exception_Handling_Global_Strategy.md) | Exception Handling & Global Error Strategy | ✅ Complete |
| [Day 18](Phase_03_Spring_Web_REST_APIs/Day_18_Async_Streaming_SSE/Day_18_Async_Streaming_SSE.md) | Async APIs, Streaming & SSE | ✅ Complete |
| [Day 19](Phase_03_Spring_Web_REST_APIs/Day_19_API_Documentation_OpenAPI/Day_19_API_Documentation_OpenAPI.md) | API Documentation & OpenAPI | ✅ Complete |
| [Day 20](Phase_03_Spring_Web_REST_APIs/Day_20_Testing_REST_APIs/Day_20_Testing_REST_APIs.md) | Testing REST APIs End-to-End | ✅ Complete |

### 🟡 Phase 4: Spring Data JPA & Database Mastery (Days 21–26)

| Day | Topic | Status |
| :---: | :--- | :---: |
| [Day 21](Phase_04_Spring_Data_JPA_Database/Day_21_JPA_Hibernate_Foundations/Day_21_JPA_Hibernate_Foundations.md) | JPA & Hibernate Foundations | ✅ Complete |
| [Day 22](Phase_04_Spring_Data_JPA_Database/Day_22_Spring_Data_Repositories_Queries/Day_22_Spring_Data_Repositories_Queries.md) | Spring Data Repositories & Query Methods | ⬜ |
| [Day 23](Phase_04_Spring_Data_JPA_Database/Day_23_Entity_Relationships_Fetch_Strategies/Day_23_Entity_Relationships_Fetch_Strategies.md) | Entity Relationships & Fetch Strategies | ⬜ |
| [Day 24](Phase_04_Spring_Data_JPA_Database/Day_24_Transactions_Concurrency_Auditing/Day_24_Transactions_Concurrency_Auditing.md) | Transactions, Concurrency & Auditing | ⬜ |
| [Day 25](Phase_04_Spring_Data_JPA_Database/Day_25_Database_Migrations_Docker/Day_25_Database_Migrations_Docker.md) | Database Migrations (Flyway) & Docker | ⬜ |
| [Day 26](Phase_04_Spring_Data_JPA_Database/Day_26_PostgreSQL_pgvector_Vector_Database/Day_26_PostgreSQL_pgvector_Vector_Database.md) | PostgreSQL pgvector — Your Vector Database | ⬜ |

### 🟠 Phase 5: Spring Security (Days 27–31)

| Day | Topic | Status |
| :---: | :--- | :---: |
| [Day 27](Phase_05_Spring_Security/Day_27_Security_Fundamentals_Architecture/Day_27_Security_Fundamentals_Architecture.md) | Security Fundamentals & Architecture | ⬜ |
| [Day 28](Phase_05_Spring_Security/Day_28_JWT_Authentication/Day_28_JWT_Authentication.md) | JWT Authentication from Scratch | ⬜ |
| [Day 29](Phase_05_Spring_Security/Day_29_RBAC_Method_Level_Security/Day_29_RBAC_Method_Level_Security.md) | Role-Based Access Control (RBAC) | ⬜ |
| [Day 30](Phase_05_Spring_Security/Day_30_OAuth2_Social_Login/Day_30_OAuth2_Social_Login.md) | OAuth2 & Social Login | ⬜ |
| [Day 31](Phase_05_Spring_Security/Day_31_Rate_Limiting_CORS_API_Security/Day_31_Rate_Limiting_CORS_API_Security.md) | Rate Limiting, CORS & API Security | ⬜ |

### 🔴 Phase 6: Spring AI — Enterprise AI Framework (Days 32–42)

> *THE CORE. Where Java meets LLMs, embeddings, vector stores, RAG, tool calling, and multimodal AI.*

| Day | Topic | Status |
| :---: | :--- | :---: |
| [Day 32](Phase_06_Spring_AI/Day_32_Introduction_to_Spring_AI/Day_32_Introduction_to_Spring_AI.md) | Introduction to Spring AI — The Big Picture | ⬜ |
| [Day 33](Phase_06_Spring_AI/Day_33_ChatClient_Fluent_Conversational_API/Day_33_ChatClient_Fluent_Conversational_API.md) | ChatClient — The Fluent Conversational API | ⬜ |
| [Day 34](Phase_06_Spring_AI/Day_34_Prompt_Engineering_in_Java/Day_34_Prompt_Engineering_in_Java.md) | Prompt Engineering in Java | ⬜ |
| [Day 35](Phase_06_Spring_AI/Day_35_Structured_Output_Java_Objects/Day_35_Structured_Output_Java_Objects.md) | Structured Output — LLMs That Return Java Objects | ⬜ |
| [Day 36](Phase_06_Spring_AI/Day_36_Streaming_Responses/Day_36_Streaming_Responses.md) | Streaming Responses — The ChatGPT Typewriter Effect | ⬜ |
| [Day 37](Phase_06_Spring_AI/Day_37_Embedding_Models_Text_to_Vectors/Day_37_Embedding_Models_Text_to_Vectors.md) | Embedding Models — Turning Text into Vectors | ⬜ |
| [Day 38](Phase_06_Spring_AI/Day_38_Vector_Stores_Semantic_Memory/Day_38_Vector_Stores_Semantic_Memory.md) | Vector Stores — Semantic Memory for Your App | ⬜ |
| [Day 39](Phase_06_Spring_AI/Day_39_RAG_Retrieval_Augmented_Generation/Day_39_RAG_Retrieval_Augmented_Generation.md) | RAG — Retrieval-Augmented Generation | ⬜ |
| [Day 40](Phase_06_Spring_AI/Day_40_Advanced_RAG_Query_ReRanking/Day_40_Advanced_RAG_Query_ReRanking.md) | Advanced RAG — Query Transformation & Re-Ranking | ⬜ |
| [Day 41](Phase_06_Spring_AI/Day_41_Tool_Calling_LLMs_Execute_Java/Day_41_Tool_Calling_LLMs_Execute_Java.md) | Tool Calling — LLMs That Execute Java Methods | ⬜ |
| [Day 42](Phase_06_Spring_AI/Day_42_Multimodal_AI_Vision_Audio_Images/Day_42_Multimodal_AI_Vision_Audio_Images.md) | Multimodal AI — Vision, Audio & Images | ⬜ |

### 🔴 Phase 7: LangChain4j — The Community Powerhouse (Days 43–49)

| Day | Topic | Status |
| :---: | :--- | :---: |
| [Day 43](Phase_07_LangChain4j/Day_43_LangChain4j_Introduction_AiServices/Day_43_LangChain4j_Introduction_AiServices.md) | LangChain4j Introduction & AiServices | ⬜ |
| [Day 44](Phase_07_LangChain4j/Day_44_Memory_Conversation_Management/Day_44_Memory_Conversation_Management.md) | Memory & Conversation Management | ⬜ |
| [Day 45](Phase_07_LangChain4j/Day_45_Structured_Extraction_Guardrails/Day_45_Structured_Extraction_Guardrails.md) | Structured Extraction & Guardrails | ⬜ |
| [Day 46](Phase_07_LangChain4j/Day_46_RAG_Pipeline_in_LangChain4j/Day_46_RAG_Pipeline_in_LangChain4j.md) | RAG Pipeline in LangChain4j | ⬜ |
| [Day 47](Phase_07_LangChain4j/Day_47_Advanced_RAG_Chunking_ReRanking/Day_47_Advanced_RAG_Chunking_ReRanking.md) | Advanced RAG — Chunking, Scoring & Re-Ranking | ⬜ |
| [Day 48](Phase_07_LangChain4j/Day_48_Tool_Execution_Function_Calling/Day_48_Tool_Execution_Function_Calling.md) | Tool Execution & Function Calling | ⬜ |
| [Day 49](Phase_07_LangChain4j/Day_49_Building_ReAct_Agent_in_Java/Day_49_Building_ReAct_Agent_in_Java.md) | Building a ReAct Agent in Java | ⬜ |

### 🟣 Phase 8: Enterprise Production & Deployment (Days 50–55)

| Day | Topic | Status |
| :---: | :--- | :---: |
| [Day 50](Phase_08_Enterprise_Production/Day_50_Model_Context_Protocol_MCP/Day_50_Model_Context_Protocol_MCP.md) | Model Context Protocol (MCP) in Java | ⬜ |
| [Day 51](Phase_08_Enterprise_Production/Day_51_Prompt_Injection_AI_Security/Day_51_Prompt_Injection_AI_Security.md) | Prompt Injection Defense & AI Security | ⬜ |
| [Day 52](Phase_08_Enterprise_Production/Day_52_Observability_OpenTelemetry_Langfuse/Day_52_Observability_OpenTelemetry_Langfuse.md) | Observability — OpenTelemetry & Langfuse | ⬜ |
| [Day 53](Phase_08_Enterprise_Production/Day_53_Caching_Rate_Limiting_Cost_Optimization/Day_53_Caching_Rate_Limiting_Cost_Optimization.md) | Caching, Rate Limiting & Cost Optimization | ⬜ |
| [Day 54](Phase_08_Enterprise_Production/Day_54_Docker_CICD_Cloud_Deployment/Day_54_Docker_CICD_Cloud_Deployment.md) | Docker, CI/CD & Cloud Deployment | ⬜ |
| [Day 55](Phase_08_Enterprise_Production/Day_55_Capstone_Enterprise_AI_Platform/Day_55_Capstone_Enterprise_AI_Platform.md) | Capstone — Enterprise AI Platform Architecture | ⬜ |

### 🟣 Phase 9: Advanced Topics & Graduation (Days 56–60)

| Day | Topic | Status |
| :---: | :--- | :---: |
| [Day 56](Phase_09_Advanced_Topics_Graduation/Day_56_Running_Local_Models_Ollama/Day_56_Running_Local_Models_Ollama.md) | Running Open-Weight Models Locally (Ollama) | ⬜ |
| [Day 57](Phase_09_Advanced_Topics_Graduation/Day_57_Multi_Agent_Orchestration/Day_57_Multi_Agent_Orchestration.md) | Multi-Agent Orchestration | ⬜ |
| [Day 58](Phase_09_Advanced_Topics_Graduation/Day_58_Evaluation_Testing_AI_Systems/Day_58_Evaluation_Testing_AI_Systems.md) | Evaluation & Testing AI Systems | ⬜ |
| [Day 59](Phase_09_Advanced_Topics_Graduation/Day_59_Vector_Database_Deep_Dive/Day_59_Vector_Database_Deep_Dive.md) | Vector Database Deep Dive | ⬜ |
| [Day 60](Phase_09_Advanced_Topics_Graduation/Day_60_Graduation_Portfolio_Career/Day_60_Graduation_Portfolio_Career.md) | Graduation — Portfolio & Career Roadmap | ⬜ |

---

## 📂 Repository Structure

```
JAVA GEN AI COURSE/
├── README.md                              ← You are here (Course Hub)
├── pom.xml                                ← Root Maven Multi-Module POM
├── .gitignore                             ← Java/Maven/IDE gitignore
├── docker-compose.yml                     ← PostgreSQL + pgvector + Ollama
│
├── Phase_01_Java_Foundations/             ← Days 01–08
├── Phase_02_Spring_Core_and_DI/           ← Days 09–14
├── Phase_03_Spring_Web_REST_APIs/         ← Days 15–20
├── Phase_04_Spring_Data_JPA_Database/     ← Days 21–26
├── Phase_05_Spring_Security/              ← Days 27–31
├── Phase_06_Spring_AI/                    ← Days 32–42
├── Phase_07_LangChain4j/                  ← Days 43–49
├── Phase_08_Enterprise_Production/        ← Days 50–55
└── Phase_09_Advanced_Topics_Graduation/   ← Days 56–60
```

---

## 🔗 Sister Course

This course is the **Java companion** to the [50-Day Python Gen AI Masterclass](../GEN%20AI%20COURSE/README.md). All AI/ML concepts are the same — this course focuses on **enterprise Java implementation**.

---

<p align="center">
  <b>Made with ❤️ for every developer who wants to build enterprise AI systems in Java.</b>
</p>
