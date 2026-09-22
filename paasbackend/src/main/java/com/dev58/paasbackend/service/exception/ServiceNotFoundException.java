// com.dev58.paasbackend.service.exception.ServiceNotFoundException
package com.dev58.paasbackend.service.exception;

public class ServiceNotFoundException extends RuntimeException {
    public ServiceNotFoundException(String message) {
        super(message);
    }
}