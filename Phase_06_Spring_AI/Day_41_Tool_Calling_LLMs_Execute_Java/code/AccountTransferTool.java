package com.genai.springai.tools;

import java.util.Map;

/**
 * Enterprise Wire Transfer Tool with safety limits and compliance auditing.
 */
public class AccountTransferTool implements FunctionTool {

    public static final double MAX_AUTO_APPROVE_LIMIT = 5000.00;

    @Override
    public ToolDefinition getDefinition() {
        return new ToolDefinition(
            "transferFunds",
            "Initiates an enterprise wire transfer between two accounts with strict compliance and risk auditing.",
            Map.of(
                "fromAccount", new ToolDefinition.ParameterSpec("string", "Source account number or IBAN", true),
                "toAccount", new ToolDefinition.ParameterSpec("string", "Destination account number or IBAN", true),
                "amount", new ToolDefinition.ParameterSpec("number", "Transfer amount in USD", true)
            )
        );
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String from = (String) arguments.get("fromAccount");
        String to = (String) arguments.get("toAccount");
        Object amtObj = arguments.get("amount");
        
        if (amtObj == null) {
            return "{\"status\": \"REJECTED\", \"error\": \"Transfer amount is missing.\"}";
        }

        double amount = amtObj instanceof Number num ? num.doubleValue() : Double.parseDouble(amtObj.toString());

        if (amount <= 0) {
            return "{\"status\": \"REJECTED\", \"error\": \"Transfer amount must be strictly greater than $0.00.\"}";
        }

        if (amount > MAX_AUTO_APPROVE_LIMIT) {
            return String.format(
                "{\"status\": \"REQUIRES_APPROVAL\", \"amount\": %.2f, \"threshold\": %.2f, \"message\": \"Transaction exceeds auto-approval limit. Risk alert triggered. Routing to Human Compliance Officer for manual authorization.\"}",
                amount, MAX_AUTO_APPROVE_LIMIT
            );
        }

        long txnId = 700000L + (long) (Math.random() * 100000L);
        return String.format(
            "{\"status\": \"SUCCESS\", \"transactionId\": \"TXN-%d\", \"from\": \"%s\", \"to\": \"%s\", \"amount\": %.2f, \"currency\": \"USD\", \"timestamp\": \"%s\"}",
            txnId, from, to, amount, java.time.Instant.now()
        );
    }
}
