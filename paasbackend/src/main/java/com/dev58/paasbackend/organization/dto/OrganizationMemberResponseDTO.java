package com.dev58.paasbackend.organization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationMemberResponseDTO {

    private UUID userPublicUuid;
    private String userFirstName;
    private String userLastName;
    private String userEmail;
    private String roleCode;
    private String roleName;
    private OffsetDateTime joinedAt;
}