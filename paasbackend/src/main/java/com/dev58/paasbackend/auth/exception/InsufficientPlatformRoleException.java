// com.dev58.paasbackend.auth.exception.InsufficientPlatformRoleException
package com.dev58.paasbackend.auth.exception;

/**
 * Thrown by AuthService when an authenticated actor (already past
 * @PreAuthorize("hasRole('PLATFORM_ADMIN')") on the controller) tries
 * to grant/assign a platformRole ranked higher than their own — e.g.
 * a PLATFORM_ADMIN trying to create or promote someone to
 * PLATFORM_OWNER. Distinct from PermissionDeniedException
 * (organization module) because this is about platform-role rank,
 * not organization membership — mapped to 403 the same way, but kept
 * as its own type so the two concerns stay traceable separately in
 * logs/stack traces.
 */
public class InsufficientPlatformRoleException extends RuntimeException {
    public InsufficientPlatformRoleException(String message) {
        super(message);
    }
}