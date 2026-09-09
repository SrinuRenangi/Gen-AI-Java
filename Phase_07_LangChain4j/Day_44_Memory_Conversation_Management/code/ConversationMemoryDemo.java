package com.genai.langchain4j.memory;

/**
 * Executable demonstration of Day 44:
 * LangChain4j Memory and Conversation Management.
 */
public class ConversationMemoryDemo {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("  DAY 44: LANGCHAIN4J CHAT MEMORY & CONVERSATION MANAGEMENT DEMO  ");
        System.out.println("==================================================================");

        PersistentChatMemoryStore store = new PersistentChatMemoryStore();

        // 1. MessageWindowChatMemory: Evicting turns while preserving SystemMessage
        System.out.println("\n--- 1. MessageWindowChatMemory (Max 5 Messages) ---");
        ChatMemory messageWindow = new MessageWindowChatMemory("sess-win-01", 5, store);

        messageWindow.add(ChatMessage.system("System Rule: You are a strict compliance agent."));
        messageWindow.add(ChatMessage.user("Turn 1: My name is Alice."));
        messageWindow.add(ChatMessage.ai("Turn 1: Hello Alice, registered."));
        messageWindow.add(ChatMessage.user("Turn 2: What is policy 101?"));
        messageWindow.add(ChatMessage.ai("Turn 2: Policy 101 covers MFA."));

        System.out.println("Current Message Count: " + messageWindow.messages().size());
        printMessages("Before Overflow", messageWindow);

        System.out.println("\nAdding Turn 3 (Triggers eviction of oldest conversational turns)...");
        messageWindow.add(ChatMessage.user("Turn 3: Update my email to alice@acme.com."));
        messageWindow.add(ChatMessage.ai("Turn 3: Email updated."));

        printMessages("After Overflow (Notice System Prompt is Intact!)", messageWindow);

        // 2. TokenWindowChatMemory: Strict Token Budgeting
        System.out.println("\n--- 2. TokenWindowChatMemory (Max 50 Tokens) ---");
        TokenWindowChatMemory tokenWindow = new TokenWindowChatMemory("sess-tok-01", 50, store);

        tokenWindow.add(ChatMessage.system("System: Concise Assistant."));
        tokenWindow.add(ChatMessage.user("A brief message about Kubernetes."));
        tokenWindow.add(ChatMessage.ai("Kubernetes automates container deployment."));

        System.out.printf("Tokens: %d / %d\n", tokenWindow.totalTokens(), 50);
        printMessages("Token Window Initial", tokenWindow);

        System.out.println("\nAdding a large message that forces token-based eviction...");
        tokenWindow.add(ChatMessage.user("Can you explain in extensive detail every single Kubernetes control plane component including etcd, kube-apiserver, kube-scheduler, and kube-controller-manager?"));
        tokenWindow.add(ChatMessage.ai("The control plane manages cluster state."));

        System.out.printf("Tokens after eviction: %d / %d\n", tokenWindow.totalTokens(), 50);
        printMessages("Token Window Post-Eviction", tokenWindow);

        // 3. Multi-Tenant Per-User Session Isolation
        System.out.println("\n--- 3. Multi-Tenant Per-User Memory Isolation ---");
        PerUserChatManager userChatManager = new PerUserChatManager(memoryId ->
            new MessageWindowChatMemory(memoryId, 10, store)
        );

        userChatManager.chat("user_alice_404", "Hello, I am Alice from Accounting.");
        userChatManager.chat("user_bob_505", "Hi, I am Bob from DevOps.");
        userChatManager.chat("user_alice_404", "Where is the ledger file?");

        ChatMemory aliceMem = userChatManager.getMemoryForUser("user_alice_404");
        ChatMemory bobMem = userChatManager.getMemoryForUser("user_bob_505");

        System.out.println("Alice's Memory Turn Count: " + aliceMem.messages().size());
        printMessages("Alice Session (ID: user_alice_404)", aliceMem);

        System.out.println("\nBob's Memory Turn Count: " + bobMem.messages().size());
        printMessages("Bob Session (ID: user_bob_505)", bobMem);

        System.out.println("\nTotal Active Sessions in Persistent Store: " + store.activeSessionCount());

        System.out.println("\n==================================================================");
        System.out.println("  CONVERSATION MEMORY VERIFICATION COMPLETED SUCCESSFULLY         ");
        System.out.println("==================================================================");
    }

    private static void printMessages(String title, ChatMemory memory) {
        System.out.println("[" + title + "]");
        for (ChatMessage msg : memory.messages()) {
            System.out.printf("   - %-7s: %s\n", msg.role(), msg.text());
        }
    }
}
