package com.genai.langchain4j.react;

/**
 * Business services accessible to the autonomous ReAct agent.
 */
public class OrderManagementTools {

    public String fetchOrder(String orderId) {
        if ("ORD-991".equalsIgnoreCase(orderId)) {
            return "{\"orderId\": \"ORD-991\", \"customerId\": \"CUST-881\", \"trackingNumber\": \"TRK-7712\", \"status\": \"DELIVERED\", \"promisedDelivery\": \"2026-09-02\"}";
        }
        return "{\"error\": \"Order not found in database\"}";
    }

    public String checkCarrierTracking(String trackingNumber) {
        if ("TRK-7712".equalsIgnoreCase(trackingNumber)) {
            return "{\"trackingNumber\": \"TRK-7712\", \"carrier\": \"FedEx Express\", \"actualDelivery\": \"2026-09-05\", \"delayHours\": 72, \"delayCause\": \"Severe Hub Blizzard\"}";
        }
        return "{\"error\": \"Tracking record not found with carrier\"}";
    }

    public String issueWalletCredit(String customerId, double amount) {
        return String.format("{\"status\": \"CREDITED\", \"customerId\": \"%s\", \"amount\": %.2f, \"currency\": \"USD\", \"newBalance\": 150.00, \"txnId\": \"TXN-CREDIT-404\"}",
            customerId, amount);
    }
}
