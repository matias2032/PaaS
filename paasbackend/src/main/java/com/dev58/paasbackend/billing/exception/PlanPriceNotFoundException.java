package com.dev58.paasbackend.billing.exception;

public class PlanPriceNotFoundException extends RuntimeException {
    public PlanPriceNotFoundException(String message) {
        super(message);
    }
}