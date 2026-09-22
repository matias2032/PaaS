// com.dev58.paasbackend.service.dto.DomainResponseDTO
package com.dev58.paasbackend.service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class DomainResponseDTO {
    private UUID publicUuid;
    private String hostname;
    private Boolean isPrimary;
    private String verificationStatus;
    private String sslStatus;
    private OffsetDateTime verifiedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}