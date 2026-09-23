package com.dev58.paasbackend.infrastructure.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Separado de um "update geral" propositadamente: name/baseUrl/
// apiToken raramente mudam depois de criados (mudar o baseUrl de uma
// instância em produção é uma operação distinta, não um PATCH
// trivial); status (ACTIVE/INACTIVE/MAINTENANCE/UNAVAILABLE) é o que
// muda com frequência operacional.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCoolifyInstanceStatusRequestDTO {

    @NotBlank(message = "Status is required")
    private String status;
}