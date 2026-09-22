// com.dev58.paasbackend.service.exception.EnvironmentVariableNotFoundException
package com.dev58.paasbackend.service.exception;

public class EnvironmentVariableNotFoundException extends RuntimeException {
    public EnvironmentVariableNotFoundException(String message) {
        super(message);
    }
}