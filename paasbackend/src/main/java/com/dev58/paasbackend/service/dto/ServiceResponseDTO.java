// com.dev58.paasbackend.service.dto.ServiceResponseDTO
package com.dev58.paasbackend.service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class ServiceResponseDTO {
    private UUID publicUuid;
    private UUID projectPublicUuid;
    private UUID organizationPublicUuid;
    private String serviceTypeCode;
    private String serviceTypeName;
    private String name;
    private String slug;
    private String status;
    private Boolean autoDeploy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}