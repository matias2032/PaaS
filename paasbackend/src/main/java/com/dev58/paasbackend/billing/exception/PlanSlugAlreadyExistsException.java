package com.dev58.paasbackend.billing.exception;

public class PlanSlugAlreadyExistsException extends RuntimeException {
    public PlanSlugAlreadyExistsException(String message) {
        super(message);
    }
}