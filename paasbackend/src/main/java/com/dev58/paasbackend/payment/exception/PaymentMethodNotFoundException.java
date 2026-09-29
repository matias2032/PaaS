package com.dev58.paasbackend.payment.exception;

// Mapped to 404 in GlobalExceptionHandler.
public class PaymentMethodNotFoundException extends RuntimeException {

    public PaymentMethodNotFoundException(String message) {
        super(message);
    }
}