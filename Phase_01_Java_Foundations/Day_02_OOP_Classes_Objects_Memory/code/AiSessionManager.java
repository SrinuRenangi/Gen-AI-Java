package com.genai.foundations.day02;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Day 02: OOP Classes, Objects, and Object Lifecycle in Memory
 * Demonstrates encapsulation, defensive copying, pointer copying, and GC eligibility.
 */
public class AiSessionManager {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   DAY 02: OOP & MEMORY LIFECYCLE DEMONSTRATION   ");
        System.out.println("==================================================");

        // Step 1: Primitive stack allocation
        int tokenLimit = 2048;

        // Step 2: Local list allocation on Heap
        List<String> initialPrompts = new ArrayList<>();
        initialPrompts.add("System: You are an enterprise AI assistant.");

        // Step 3: Instantiation of ChatSession (Memory allocation -> Zero-init -> Constructor)
        // 'session1' pointer on Stack points to Heap address (e.g. 0x10AA)
        ChatSession session1 = new ChatSession("session-abc", tokenLimit, initialPrompts);
        System.out.println("1. Session instantiated on Heap: " + session1.getSessionId());
        System.out.println("   History: " + session1.getHistory());

        // Step 4: Demonstrating Defensive Copying
        initialPrompts.clear(); // Attacker tries to clear original list
        System.out.println("2. External list cleared. Internal history remains safe:");
        System.out.println("   History after external clear: " + session1.getHistory());

        // Step 5: Pointer Copying (NOT object duplication)
        ChatSession session2 = session1;
        System.out.println("3. Pointer copied (session1 == session2): " + (session1 == session2));

        // Step 6: Dereferencing session1
        session1 = null;
        System.out.println("4. session1 set to null. Is object still reachable via session2?");
        System.out.println("   session2.getSessionId(): " + session2.getSessionId());

        // Step 7: Dereferencing session2
        session2 = null;
        System.out.println("5. session2 set to null. Object has 0 incoming GC Roots.");
        System.out.println("   Object is now orphaned and eligible for Garbage Collection!");
        System.out.println("==================================================");
    }
}

class ChatSession {
    // Instance fields living on the Heap inside each ChatSession object payload
    private final String sessionId;
    private final int maxTokens;
    private final List<String> history;

    public ChatSession(String sessionId, int maxTokens, List<String> initialHistory) {
        // Validate invariants
        this.sessionId = Objects.requireNonNull(sessionId, "Session ID cannot be null");
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("Token limit must be positive");
        }
        this.maxTokens = maxTokens;

        // DEFENSIVE COPY: Prevents external callers from mutating our internal Heap state!
        this.history = new ArrayList<>(Objects.requireNonNull(initialHistory));
    }

    public String getSessionId() {
        return sessionId; // Strings are immutable in Java, safe to return directly
    }

    public int getMaxTokens() {
        return maxTokens; // Primitives return raw values by copy
    }

    public List<String> getHistory() {
        // DEFENSIVE VIEW: Callers cannot alter internal history
        return Collections.unmodifiableList(this.history);
    }
}
