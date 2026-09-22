// com.dev58.paasbackend.service.dto.ServiceBuildConfigResponseDTO
package com.dev58.paasbackend.service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
public class ServiceBuildConfigResponseDTO {
    private String rootDirectory;
    private String buildCommand;
    private String startCommand;
    private String dockerfilePath;
    private String healthCheckPath;
    private Integer port;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}