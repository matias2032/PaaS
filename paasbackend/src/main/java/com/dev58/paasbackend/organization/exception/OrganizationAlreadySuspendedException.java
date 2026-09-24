package com.dev58.paasbackend.organization.exception;

/**
 * Thrown by OrganizationService.suspendOrganization() when the
 * organization is already SUSPENDED. Symmetric with
 * OrganizationNotSuspendedException (thrown by liftSuspension() for
 * the opposite case) — kept as its own type rather than
 * IllegalArgumentException so both halves of the suspend/lift pair
 * read the same way in code and in logs.
 */
public class OrganizationAlreadySuspendedException extends RuntimeException {
    public OrganizationAlreadySuspendedException(String message) {
        super(message);
    }
}