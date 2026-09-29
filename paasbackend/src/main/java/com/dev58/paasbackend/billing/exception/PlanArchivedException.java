package com.dev58.paasbackend.billing.exception;

public class PlanArchivedException extends RuntimeException {

    public PlanArchivedException(String message) {
        super(message);
    }
}