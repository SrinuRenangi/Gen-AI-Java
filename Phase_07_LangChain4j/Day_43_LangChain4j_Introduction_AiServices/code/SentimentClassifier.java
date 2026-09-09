package com.genai.langchain4j.aiservices;

/**
 * Declarative AI Service interface that returns an Enum directly from the LLM.
 */
public interface SentimentClassifier {

    enum Sentiment { POSITIVE, NEUTRAL, NEGATIVE }

    @SystemMessage("You are a strict sentiment classification system. Analyze the customer text and respond with POSITIVE, NEUTRAL, or NEGATIVE.")
    @UserMessage("Classify customer review: {{reviewText}}")
    Sentiment classify(@V("reviewText") String reviewText);
}
