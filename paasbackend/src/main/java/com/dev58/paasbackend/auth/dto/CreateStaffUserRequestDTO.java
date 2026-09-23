// com.dev58.paasbackend.auth.dto.CreateStaffUserRequestDTO
package com.dev58.paasbackend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Used only by AuthService.createStaffUser(), called by an existing
 * SUPPORT-or-above user (see AuthController — the endpoint is
 * @PreAuthorize("hasRole('PLATFORM_ADMIN')")). Deliberately NOT the
 * same DTO as AuthRequestDTO (public self-registration): that one has
 * no platformRole field at all, on purpose, so a public registration
 * can never touch this value. This DTO exists specifically to carry
 * it, and only reaches AuthService through an already-authenticated,
 * already-privileged call path.
 * <p>
 * platformRole here is intentionally NOT validated against the full
 * CHECK-constraint set — CUSTOMER is excluded on purpose (that's what
 * the public /api/auth/register endpoint is for). Enforcement of
 * "CUSTOMER not allowed here" plus the rank-based escalation guard
 * (an actor can't grant a role higher than their own) both live in
 * AuthService, not here — they need the acting user's own role to
 * decide, which a DTO has no business knowing about.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateStaffUserRequestDTO {

    @NotBlank(message = "Email is required")
    @Email(message = "Email is invalid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;

    @NotBlank(message = "First name is required")
    private String firstName;

    private String lastName;

    private String phone;

    @NotBlank(message = "Platform role is required")
    private String platformRole;
}