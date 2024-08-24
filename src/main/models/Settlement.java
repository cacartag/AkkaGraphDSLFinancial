package models;

public record Settlement(
        String paymentId,
        String invoiceId,
        String clientMatcher,
        String payload
) {
}
