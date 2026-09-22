// com.dev58.paasbackend.service.dto.DeploymentResponseDTO
package com.dev58.paasbackend.service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class DeploymentResponseDTO {
    private UUID publicUuid;
    private String coolifyDeploymentUuid;
    private String commitHash;
    private String commitMessage;
    private String branch;
    private String triggerType;
    private String status;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private OffsetDateTime createdAt;
}