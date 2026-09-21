package com.dev58.paasbackend.project.exception;

public class GitProviderNotFoundException extends RuntimeException {
    public GitProviderNotFoundException(String message) {
        super(message);
    }
}