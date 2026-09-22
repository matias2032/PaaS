// com.dev58.paasbackend.service.exception.ServiceResourceConfigNotFoundException
package com.dev58.paasbackend.service.exception;

public class ServiceResourceConfigNotFoundException extends RuntimeException {
    public ServiceResourceConfigNotFoundException(String message) {
        super(message);
    }
}