package com.dev58.paasbackend.payment.exception;

// Mapped to 409. Thrown when an invoice already has a PENDING/PROCESSING
// payment and the client submits another one.
public class PaymentAlreadyInProgressException extends RuntimeException {

    public PaymentAlreadyInProgressException(String message) {
        super(message);
    }
}