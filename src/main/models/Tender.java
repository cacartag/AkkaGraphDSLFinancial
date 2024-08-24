package models;

public record Tender(
        String paymentId,
        String clientMatcher,
        String invoiceId,
        String payload
) {
}
