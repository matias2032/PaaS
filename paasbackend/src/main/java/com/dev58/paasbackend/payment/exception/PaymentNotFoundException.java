package com.dev58.paasbackend.payment.exception;

// Mapped to 404 in GlobalExceptionHandler.
public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String message) {
        super(message);
    }
}