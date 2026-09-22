// com.dev58.paasbackend.service.exception.ServiceBuildConfigNotFoundException
package com.dev58.paasbackend.service.exception;

public class ServiceBuildConfigNotFoundException extends RuntimeException {
    public ServiceBuildConfigNotFoundException(String message) {
        super(message);
    }
}