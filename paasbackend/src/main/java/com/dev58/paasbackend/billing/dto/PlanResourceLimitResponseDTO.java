package com.dev58.paasbackend.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanResourceLimitResponseDTO {

    private BigDecimal cpuLimit;
    private Long memoryLimitMb;
    private Long storageLimitMb;
    private Integer maxProjects;
    private Integer maxServices;
    private Integer maxDomains;
    private Integer maxEnvironmentVariables;
    private Long bandwidthLimitMb;
}