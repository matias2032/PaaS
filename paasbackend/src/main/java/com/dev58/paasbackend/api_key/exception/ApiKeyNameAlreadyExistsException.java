package com.dev58.paasbackend.api_key.exception;

public class ApiKeyNameAlreadyExistsException extends RuntimeException {
    public ApiKeyNameAlreadyExistsException(String message) {
        super(message);
    }
}