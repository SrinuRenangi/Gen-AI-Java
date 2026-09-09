package com.javagenai.day05;

import java.util.List;

public record ExtractedInvoice(String invoiceId, String vendorName, List<LineItem> items) {
    public ExtractedInvoice {
        if (invoiceId == null || invoiceId.isBlank()) throw new IllegalArgumentException("invoiceId required");
        if (vendorName == null || vendorName.isBlank()) throw new IllegalArgumentException("vendorName required");
        items = (items != null) ? List.copyOf(items) : List.of();
    }

    public double grandTotal() {
        double sum = 0.0;
        for (LineItem item : items) {
            sum += item.total();
        }
        return sum;
    }
}
