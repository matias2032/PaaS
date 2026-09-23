package com.dev58.paasbackend.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Used only by the platform-side suspendOrganization action — not the
// self-service deactivateOrganization, which takes no body.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationSuspendRequestDTO {

    @NotBlank
    @Size(max = 2000)
    private String reason;
}