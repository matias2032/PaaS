// com.dev58.paasbackend.service.exception.DeploymentNotFoundException
package com.dev58.paasbackend.service.exception;

public class DeploymentNotFoundException extends RuntimeException {
    public DeploymentNotFoundException(String message) {
        super(message);
    }
}