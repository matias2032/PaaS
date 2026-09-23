package com.dev58.paasbackend.infrastructure.exception;

public class CoolifyInstanceNotFoundException extends RuntimeException {
    public CoolifyInstanceNotFoundException(String message) {
        super(message);
    }
}