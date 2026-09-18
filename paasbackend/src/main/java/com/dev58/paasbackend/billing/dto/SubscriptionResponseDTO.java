package com.dev58.paasbackend.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionResponseDTO {

    private UUID publicUuid;
    private UUID organizationPublicUuid;
    // Plano/preço achatados na resposta (evita o cliente ter de fazer
    // um segundo GET a /plans/{uuid} só para saber o que a org
    // realmente contratou).
    private UUID planPublicUuid;
    private String planName;
    private String billingCycle;
    private String status;
    private OffsetDateTime currentPeriodStart;
    private OffsetDateTime currentPeriodEnd;
    private Boolean autoRenew;
    private OffsetDateTime cancelledAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}