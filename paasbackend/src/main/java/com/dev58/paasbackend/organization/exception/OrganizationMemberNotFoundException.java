package com.dev58.paasbackend.organization.exception;

public class OrganizationMemberNotFoundException extends RuntimeException {
    public OrganizationMemberNotFoundException(String message) {
        super(message);
    }
}