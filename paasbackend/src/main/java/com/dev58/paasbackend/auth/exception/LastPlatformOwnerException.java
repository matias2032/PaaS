// com.dev58.paasbackend.auth.exception.LastPlatformOwnerException
package com.dev58.paasbackend.auth.exception;

/**
 * Thrown by AuthService.updatePlatformRole() when the requested change
 * would leave the platform with zero PLATFORM_OWNER users — whether
 * the owner is demoting themselves or being demoted by another owner.
 * The platform must always have at least one PLATFORM_OWNER able to
 * manage staff and infrastructure.
 */
public class LastPlatformOwnerException extends RuntimeException {
    public LastPlatformOwnerException(String message) {
        super(message);
    }
}