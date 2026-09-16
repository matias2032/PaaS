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
public class OrganizationResponseDTO {

    private UUID publicUuid;
    private String name;
    private String slug;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}