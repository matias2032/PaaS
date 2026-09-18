package com.dev58.paasbackend.billing.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

// O cliente escolhe um PlanPrice (que já fixa o plano + ciclo), não um
// Plan solto — evita um segundo campo "billingCycle" que teria de ser
// validado contra o plano escolhido.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionRequestDTO {

    @NotNull
    private UUID planPricePublicUuid;
}