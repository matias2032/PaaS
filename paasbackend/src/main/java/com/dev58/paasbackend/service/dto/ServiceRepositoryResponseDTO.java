// com.dev58.paasbackend.service.dto.ServiceRepositoryResponseDTO
package com.dev58.paasbackend.service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class ServiceRepositoryResponseDTO {
    private UUID gitConnectionPublicUuid;
    private String repositoryUrl;
    private String repositoryOwner;
    private String repositoryName;
    private String branch;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}