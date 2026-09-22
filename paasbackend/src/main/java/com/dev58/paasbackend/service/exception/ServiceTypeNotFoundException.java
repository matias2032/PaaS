// com.dev58.paasbackend.service.exception.ServiceTypeNotFoundException
package com.dev58.paasbackend.service.exception;

public class ServiceTypeNotFoundException extends RuntimeException {
    public ServiceTypeNotFoundException(String message) {
        super(message);
    }
}