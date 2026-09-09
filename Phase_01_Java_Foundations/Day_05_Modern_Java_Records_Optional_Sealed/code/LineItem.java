package com.javagenai.day05;

public record LineItem(String description, int quantity, double unitPrice) {
    public LineItem {
        if (description == null || description.isBlank()) throw new IllegalArgumentException("Description required");
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be > 0");
        if (unitPrice < 0) throw new IllegalArgumentException("Price cannot be negative");
    }

    public double total() {
        return quantity * unitPrice;
    }
}
