package models;

public record Accounting(
        String paymentId,
        String invoiceId,
        String payload
) implements TransactionType { }
