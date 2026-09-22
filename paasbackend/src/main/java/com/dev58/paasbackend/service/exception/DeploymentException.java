// com.dev58.paasbackend.service.exception.DeploymentException
package com.dev58.paasbackend.service.exception;

/**
 * Generic error for the Deployment lifecycle. Two constructors on
 * purpose: the message-only one covers today's validation-level
 * failures (e.g. invalid trigger_type); the (message, cause) one is
 * unused for now but is what a future CoolifyClient failure will
 * wrap (build failed, API unreachable, etc.) once INFRASTRUCTURE
 * exists — kept ready rather than added later under pressure.
 */
public class DeploymentException extends RuntimeException {

    public DeploymentException(String message) {
        super(message);
    }

    public DeploymentException(String message, Throwable cause) {
        super(message, cause);
    }
}