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
// NOTE (frontend handoff): only the "add member" use is currently wired
// to an endpoint (POST /api/organizations/{publicUuid}/members). There is
// no PUT/PATCH on OrganizationController for changing a member's role yet,
// so the "role change" reuse described above is aspirational until that
// endpoint exists.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationMemberRequestDTO {

    @NotBlank
    @Email
    private String userEmail;

    @NotBlank
    private String roleCode;
}