package com.dev58.paasbackend.payment.exception;

// Mapped to 404 in GlobalExceptionHandler.
public class InvoiceNotFoundException extends RuntimeException {

    public InvoiceNotFoundException(String message) {
        super(message);
    }
}