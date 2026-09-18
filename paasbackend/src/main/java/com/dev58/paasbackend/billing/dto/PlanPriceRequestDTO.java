package com.dev58.paasbackend.billing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// Cria um novo preço para um ciclo. O Service fecha o preço anterior
// desse mesmo ciclo (define effectiveUntil = agora) em vez de o
// atualizar — ver nota no PlanPrice entity.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanPriceRequestDTO {

    @NotBlank
    private String billingCycle;

    @NotNull
    @DecimalMin(value = "0.0")
    private BigDecimal amount;

    private String currency;
}