package com.genai.foundations.day03;

import java.util.Objects;

/**
 * Day 03: Inheritance, Interfaces, Polymorphism, and Dynamic Dispatch in Memory.
 * Demonstrates interfaces, abstract templates, contiguous heap memory, and vtable dispatch.
 */

// 1. PURE CAPABILITY CONTRACT (Interface)
interface ChatModel {
    // Abstract capability required from all implementers
    String generate(String prompt);

    // Default method: API evolution without breaking implementers
    default void logCall(String prompt) {
        System.out.println("[AUDIT LOG] Dispatching prompt: \"" + prompt + "\"");
    }

    // Static factory utility method
    static void printSpecification() {
        System.out.println("[SPEC] ChatModel Standard v1.0 -- Enterprise Compliant");
    }
}

// 2. SKELETAL BASE TEMPLATE (Abstract Class with State & Template Pattern)
abstract class AbstractLanguageModel implements ChatModel {
    // Superclass fields: Contiguously allocated FIRST in Heap memory payload
    private final String modelId;
    private final int maxTokens;

    public AbstractLanguageModel(String modelId, int maxTokens) {
        // Defensive Invariant Checks
        this.modelId = Objects.requireNonNull(modelId, "modelId cannot be null");
        if (maxTokens <= 0) throw new IllegalArgumentException("maxTokens must be > 0");
        this.maxTokens = maxTokens;
    }

    public String getModelId() { return modelId; }
    public int getMaxTokens() { return maxTokens; }

    // Logical Equality Contract
    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // Pointer address equality!
        if (!(o instanceof AbstractLanguageModel that)) return false;
        return maxTokens == that.maxTokens && Objects.equals(modelId, that.modelId);
    }

    // HashCode Contract consistent with equals()
    @Override
    public int hashCode() {
        return Objects.hash(modelId, maxTokens);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[modelId='" + modelId + "', maxTokens=" + maxTokens + "]";
    }
}

// 3. CONCRETE SUBCLASS (Extends State & Implements Specific Behavior)
class OpenAiModel extends AbstractLanguageModel {
    // Subclass field: Allocated immediately following superclass fields on Heap
    private final String apiKey;

    public OpenAiModel(String modelId, int maxTokens, String apiKey) {
        super(modelId, maxTokens); // Explicit constructor chaining!
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey cannot be null");
    }

    @Override
    public String generate(String prompt) {
        logCall(prompt); // Calls inherited interface default method
        return "OpenAI (" + getModelId() + ") response for: \"" + prompt + "\"";
    }

    public String getApiKeyMasked() {
        return apiKey.substring(0, Math.min(apiKey.length(), 6)) + "...";
    }
}

// 4. MAIN RUNNER (Tracking Stack Frames, Heap Allocations, and vtable Dispatch)
public class PolymorphismRunner {
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("    DAY 03: POLYMORPHISM & VTABLE DISPATCH DEMO    ");
        System.out.println("==================================================");

        // 1. Static utility call directly on interface
        ChatModel.printSpecification();

        // 2. Polymorphic Allocation:
        // Stack Reference Type : ChatModel
        // Actual Heap Instance : OpenAiModel @ 0x5A00 (Header + Super Fields + Child Fields)
        ChatModel model = new OpenAiModel("gpt-4o", 4096, "sk-proj-live-token-12345");

        // 3. Dynamic Dispatch via Metaspace vtable:
        // Reads Klass Word -> OpenAiModel.class in Metaspace -> invokes OpenAiModel.generate()
        String response = model.generate("Explain quantum computing in one sentence");
        System.out.println("Output: " + response);
        System.out.println();

        // 4. Verifying Object Contract (equals, hashCode, toString)
        ChatModel duplicateModel = new OpenAiModel("gpt-4o", 4096, "sk-proj-different-key");
        
        System.out.println("Model 1 toString() : " + model);
        System.out.println("Model 2 toString() : " + duplicateModel);
        System.out.println("Address (==) Equality      : " + (model == duplicateModel)); // false (different Heap pointers)
        System.out.println("Logical (.equals) Equality : " + model.equals(duplicateModel)); // true (same modelId & tokens)
        System.out.println("HashCode 1 : " + model.hashCode());
        System.out.println("HashCode 2 : " + duplicateModel.hashCode());
        System.out.println("HashCodes Match?           : " + (model.hashCode() == duplicateModel.hashCode())); // true!
        System.out.println();

        // 5. Pattern Matching for instanceof
        if (model instanceof OpenAiModel openAi) {
            System.out.println("Downcast Safe! Masked API Key: " + openAi.getApiKeyMasked());
        }

        System.out.println("==================================================");
    }
}
