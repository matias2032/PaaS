package com.dev58.paasbackend.billing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
public class PlanResourceLimitRequestDTO {

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal cpuLimit;

    @NotNull
    @PositiveOrZero
    private Long memoryLimitMb;

    @NotNull
    @PositiveOrZero
    private Long storageLimitMb;

    @NotNull
    @PositiveOrZero
    private Integer maxProjects;

    @NotNull
    @PositiveOrZero
    private Integer maxServices;

    @NotNull
    @PositiveOrZero
    private Integer maxDomains;

    private Integer maxEnvironmentVariables;

    private Long bandwidthLimitMb;
}