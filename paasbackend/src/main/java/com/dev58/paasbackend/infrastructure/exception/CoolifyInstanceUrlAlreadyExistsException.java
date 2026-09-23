package com.dev58.paasbackend.infrastructure.exception;

public class CoolifyInstanceUrlAlreadyExistsException extends RuntimeException {
    public CoolifyInstanceUrlAlreadyExistsException(String message) {
        super(message);
    }
}