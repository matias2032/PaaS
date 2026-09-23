package com.dev58.paasbackend.infrastructure.exception;

public class CoolifyInstanceNameAlreadyExistsException extends RuntimeException {
    public CoolifyInstanceNameAlreadyExistsException(String message) {
        super(message);
    }
}