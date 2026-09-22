// com.dev58.paasbackend.service.dto.ServiceResourceConfigRequestDTO
package com.dev58.paasbackend.service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class ServiceResourceConfigRequestDTO {

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal cpuLimit;

    @NotNull
    @Positive
    private Long memoryLimitMb;

    @NotNull
    @Positive
    private Long storageLimitMb;
}