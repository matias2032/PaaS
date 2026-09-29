package com.dev58.paasbackend.payment.exception;

// Mapped to 409. Thrown when refunding a payment that is already REFUNDED.
public class PaymentAlreadyRefundedException extends RuntimeException {

    public PaymentAlreadyRefundedException(String message) {
        super(message);
    }
}