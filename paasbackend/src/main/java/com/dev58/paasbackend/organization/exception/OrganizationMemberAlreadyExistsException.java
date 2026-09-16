package com.dev58.paasbackend.organization.exception;

public class OrganizationMemberAlreadyExistsException extends RuntimeException {
    public OrganizationMemberAlreadyExistsException(String message) {
        super(message);
    }
}