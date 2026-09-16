package com.dev58.paasbackend.organization.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationRoleResponseDTO {

    private Short idOrganizationRole;
    private String code;
    private String name;
}