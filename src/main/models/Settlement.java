package models;

public record Settlement(
        String paymentId,
        String invoiceId,
        String clientMatcher,
        String payload
) implements TransactionType {

    @Override
    public String toString() {
        return "Settlement type, paymentId" + paymentId + " clientMatcher " + clientMatcher + " payload" + payload;
    }
}
