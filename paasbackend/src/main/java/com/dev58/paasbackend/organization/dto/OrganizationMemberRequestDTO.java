package com.dev58.paasbackend.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Used to add a member (identifies the user by email — the org admin
// knows their colleague's email, not an internal UUID) and to change
// an existing member's role (roleCode only, in that case; email is
// ignored/optional for that operation — decide exact contract in
// Service/Controller).
//
// NOTE (frontend handoff): both uses are now wired to endpoints —
// "add member" via POST /api/organizations/{publicUuid}/members and
// "change role" via PATCH /api/organizations/{publicUuid}/members/{userPublicUuid}.
// Both are OWNER-only (not OWNER+ADMIN) — decided 2026: registration and
// role changes are sensitive enough to restrict to OWNER alone.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationMemberRequestDTO {

    // Not @NotBlank: required for "add member" (enforced manually in
    // OrganizationService.addMember) but unused/ignored for "change role".
    @Email
    private String userEmail;

    @NotBlank
    private String roleCode;
}