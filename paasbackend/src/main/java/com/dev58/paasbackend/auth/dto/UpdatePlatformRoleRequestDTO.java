// com.dev58.paasbackend.auth.dto.UpdatePlatformRoleRequestDTO
package com.dev58.paasbackend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Used by AuthService.updatePlatformRole() — promotes or demotes an
 * EXISTING user (staff or customer). Unlike CreateStaffUserRequestDTO,
 * CUSTOMER is a valid target here: this is the only way to revoke
 * staff access from someone (demote them back to CUSTOMER) without
 * deleting their account.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePlatformRoleRequestDTO {

    @NotBlank(message = "Platform role is required")
    private String platformRole;
}