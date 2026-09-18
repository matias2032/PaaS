package com.dev58.paasbackend.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanResponseDTO {

    private UUID publicUuid;
    private String name;
    private String slug;
    private String description;
    private String status;
    private PlanResourceLimitResponseDTO resourceLimits;
    // Um por billing_cycle correntemente em vigor (nunca o histórico).
    private List<PlanPriceResponseDTO> prices;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}