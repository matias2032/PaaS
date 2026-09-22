// com.dev58.paasbackend.service.exception.ServiceRepositoryNotFoundException
package com.dev58.paasbackend.service.exception;

public class ServiceRepositoryNotFoundException extends RuntimeException {
    public ServiceRepositoryNotFoundException(String message) {
        super(message);
    }
}