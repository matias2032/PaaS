package com.dev58.paasbackend.payment.exception;

// Mapped to 409. Thrown when paying, or marking as paid, an invoice
// that is already PAID.
public class InvoiceAlreadyPaidException extends RuntimeException {

    public InvoiceAlreadyPaidException(String message) {
        super(message);
    }
}