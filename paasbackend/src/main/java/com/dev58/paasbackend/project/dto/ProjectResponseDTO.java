package com.dev58.paasbackend.project.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class ProjectResponseDTO {

    private UUID publicUuid;
    private UUID organizationPublicUuid;
    private String name;
    private String slug;
    private String description;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}