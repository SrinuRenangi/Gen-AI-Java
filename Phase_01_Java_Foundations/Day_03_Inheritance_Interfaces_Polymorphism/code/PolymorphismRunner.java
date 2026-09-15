package com.genai.foundations.day03;

import java.util.Objects;

/**
 * Day 03: Inheritance, Interfaces, Polymorphism, and Dynamic Dispatch in Memory.
 * Demonstrates interfaces, abstract templates, contiguous heap memory, and vtable dispatch.
 */

// 1. Interface defining pure capability contract
interface ChatModel {
    String generate(String prompt);

    default void logCall(String prompt) {
        System.out.println("[AUDIT LOG] Prompt dispatched: " + prompt);
    }
}

// 2. Abstract base class providing shared state & template
abstract class AbstractLanguageModel implements ChatModel {
    // Superclass fields: Allocated first in contiguous Heap memory
    private final String modelId;
    private final int maxTokens;

    public AbstractLanguageModel(String modelId, int maxTokens) {
        this.modelId = Objects.requireNonNull(modelId, "modelId cannot be null");
        this.maxTokens = maxTokens;
    }

    public String getModelId() { return modelId; }
    public int getMaxTokens() { return maxTokens; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // Pointer address equality!
        if (!(o instanceof AbstractLanguageModel that)) return false;
        return maxTokens == that.maxTokens && Objects.equals(modelId, that.modelId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelId, maxTokens);
    }
}

// 3. Concrete subclass extending state and overriding behavior
class OpenAiModel extends AbstractLanguageModel {
    // Subclass field: Allocated immediately following superclass fields in Heap
    private final String apiKey;

    public OpenAiModel(String modelId, int maxTokens, String apiKey) {
        super(modelId, maxTokens); // Explicit constructor chaining!
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey cannot be null");
    }

    @Override
    public String generate(String prompt) {
        logCall(prompt); // Calls inherited default method
        return "OpenAI (" + getModelId() + ") generated response for: " + prompt;
    }
}

// 4. Main runner tracking stack and heap memory
public class PolymorphismRunner {
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("    DAY 03: POLYMORPHISM & VTABLE DISPATCH DEMO    ");
        System.out.println("==================================================");

        // Polymorphic reference:
        // Stack Reference Type: ChatModel
        // Actual Heap Instance: OpenAiModel (Header + super fields + child fields)
        ChatModel model = new OpenAiModel("gpt-4o", 4096, "sk-proj-live-token");

        // Dynamic dispatch via vtable: Calls OpenAiModel.generate()
        String result = model.generate("Explain quantum computing");
        System.out.println("Output: " + result);

        // Verifying Object contract
        ChatModel modelDuplicate = new OpenAiModel("gpt-4o", 4096, "sk-proj-another-key");
        System.out.println("Address equality (==)      : " + (model == modelDuplicate)); // false (different pointers)
        System.out.println("Logical equality (.equals) : " + model.equals(modelDuplicate)); // true (same modelId & maxTokens)
        System.out.println("==================================================");
    }
}
