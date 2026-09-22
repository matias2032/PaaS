// com.dev58.paasbackend.service.dto.ServiceResourceConfigResponseDTO
package com.dev58.paasbackend.service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
public class ServiceResourceConfigResponseDTO {
    private BigDecimal cpuLimit;
    private Long memoryLimitMb;
    private Long storageLimitMb;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}