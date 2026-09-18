package com.dev58.paasbackend.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

// Representa sempre um preço CORRENTE (effectiveUntil == null) quando
// embutido em PlanResponseDTO. O histórico fechado só aparece no
// endpoint admin de histórico (camada de controller, mais à frente).
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanPriceResponseDTO {

    private UUID publicUuid;
    private String billingCycle;
    private BigDecimal amount;
    private String currency;
    private OffsetDateTime effectiveFrom;
}