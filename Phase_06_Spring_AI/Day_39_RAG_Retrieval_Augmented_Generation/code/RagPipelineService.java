package com.genai.springai.rag;

import com.genai.springai.chatclient.ChatClient;
import com.genai.springai.vectorstore.Document;
import com.genai.springai.vectorstore.SearchRequest;
import com.genai.springai.vectorstore.VectorStore;

import java.util.List;

/**
 * End-to-end Retrieval-Augmented Generation (RAG) orchestration service.
 */
public class RagPipelineService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final TokenTextSplitter splitter;

    public RagPipelineService(VectorStore vectorStore, ChatClient chatClient) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClient;
        this.splitter = new TokenTextSplitter(300, 40);
    }

    public void ingestDocuments(List<Document> rawDocuments) {
        List<Document> chunks = splitter.split(rawDocuments);
        vectorStore.add(chunks);
    }

    public record RagAnswer(String answer, List<Document> citedSources) {}

    public RagAnswer answerQuestion(String question, int topK, double threshold) {
        // 1. Retrieve top-K relevant chunks from vector store
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(topK)
                .similarityThreshold(threshold)
                .build();

        List<Document> retrievedSources = vectorStore.similaritySearch(request);

        // 2. Format augmented prompt with context & guardrails
        String augmentedPrompt = RagContextAugmenter.buildAugmentedPrompt(question, retrievedSources);

        // 3. Invoke LLM via ChatClient
        String rawAnswer = chatClient.prompt()
                .user(augmentedPrompt)
                .call()
                .content();

        // 4. Return answer + cited sources for full transparency
        return new RagAnswer(rawAnswer, retrievedSources);
    }
}
