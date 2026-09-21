package com.dev58.paasbackend.project.exception;

public class GitConnectionNotFoundException extends RuntimeException {
    public GitConnectionNotFoundException(String message) {
        super(message);
    }
}