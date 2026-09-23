package com.dev58.paasbackend.organization.exception;

// Thrown by liftSuspension() when the organization's status isn't
// SUSPENDED — same family as OrganizationInactiveException, mapped to
// 409 in GlobalExceptionHandler.
public class OrganizationNotSuspendedException extends RuntimeException {
    public OrganizationNotSuspendedException(String message) {
        super(message);
    }
}