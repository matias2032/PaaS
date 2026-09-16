package com.dev58.paasbackend.organization.exception;

public class OrganizationSlugAlreadyExistsException extends RuntimeException {
    public OrganizationSlugAlreadyExistsException(String message) {
        super(message);
    }
}