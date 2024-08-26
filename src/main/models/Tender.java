package models;

public record Tender(
        String paymentId,
        String clientMatcher,
        String invoiceId,
        String payload
) implements TransactionType{

    @Override
    public String toString() {
        return "Tender type, paymentId: " + paymentId + " clientMatcher: " + clientMatcher + " payload: " + payload;
    }
}
