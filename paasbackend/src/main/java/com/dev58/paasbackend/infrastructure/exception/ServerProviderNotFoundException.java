package com.dev58.paasbackend.infrastructure.exception;

public class ServerProviderNotFoundException extends RuntimeException {
    public ServerProviderNotFoundException(String message) {
        super(message);
    }
}