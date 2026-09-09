package com.javagenai.day07;

import java.util.concurrent.atomic.AtomicLong;

public class AtomicTokenBudget {
    private final AtomicLong tokensConsumed = new AtomicLong(0);
    private final long maxBudgetTokens;

    public AtomicTokenBudget(long maxBudgetTokens) {
        this.maxBudgetTokens = maxBudgetTokens;
    }

    public boolean tryConsume(long tokens) {
        while (true) {
            long current = tokensConsumed.get();
            if (current + tokens > maxBudgetTokens) {
                return false;
            }
            if (tokensConsumed.compareAndSet(current, current + tokens)) {
                return true;
            }
        }
    }

    public long getTokensConsumed() {
        return tokensConsumed.get();
    }
}
