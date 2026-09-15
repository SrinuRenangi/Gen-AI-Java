package com.genai.foundations.day04;

import java.util.*;

/**
 * Day 04: Generics, Collections Framework, and Data Structures in Memory.
 * Demonstrates boxing tax, ArrayList contiguous allocation, HashMap hashing, and safe iteration.
 */
public class CollectionsMemoryDemo {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   DAY 04: GENERICS & COLLECTIONS MEMORY TRACE    ");
        System.out.println("==================================================");

        // 1. Boxing Overhead Demonstration
        int primitiveTokens = 4096; // 4 bytes raw bits on Stack
        Integer boxedTokens = Integer.valueOf(4096); // 24 bytes on Heap + 8-byte pointer!
        System.out.println("1. Boxing Demonstration:");
        System.out.println("   Primitive on Stack: " + primitiveTokens + " (4 bytes)");
        System.out.println("   Boxed on Heap     : " + boxedTokens + " (24 bytes + 8-byte pointer)");
        System.out.println();

        // 2. ArrayList contiguous backing array allocation
        List<String> modelList = new ArrayList<>(4); // Allocates Object[4] on Heap
        modelList.add("gpt-4o");
        modelList.add("claude-3-5-sonnet");
        modelList.add("gemini-1-5-pro");

        System.out.println("2. ArrayList (Contiguous Memory):");
        System.out.println("   Elements: " + modelList);
        System.out.println("   Size    : " + modelList.size());
        System.out.println();

        // 3. HashMap bucket hashing and insertion
        Map<String, Integer> tokenUsageMap = new HashMap<>(16);
        tokenUsageMap.put("gpt-4o", 1500);
        tokenUsageMap.put("claude-3-5-sonnet", 2200);

        System.out.println("3. HashMap (Bucket Table Allocation):");
        System.out.println("   Size                  : " + tokenUsageMap.size());
        System.out.println("   Lookup 'gpt-4o'       : " + tokenUsageMap.get("gpt-4o") + " tokens");
        System.out.println("   Contains 'claude-...' : " + tokenUsageMap.containsKey("claude-3-5-sonnet"));
        System.out.println();

        // 4. Safe modification using removeIf (modCount safe)
        modelList.removeIf(m -> m.contains("gemini"));
        System.out.println("4. ArrayList After Safe removeIf:");
        System.out.println("   Updated List: " + modelList);
        System.out.println("==================================================");
    }
}
