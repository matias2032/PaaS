// com.dev58.paasbackend.service.exception.DomainNotFoundException
package com.dev58.paasbackend.service.exception;

public class DomainNotFoundException extends RuntimeException {
    public DomainNotFoundException(String message) {
        super(message);
    }
}