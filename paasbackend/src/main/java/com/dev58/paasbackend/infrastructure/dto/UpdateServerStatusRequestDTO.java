package com.dev58.paasbackend.infrastructure.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Mesmo racional que UpdateCoolifyInstanceStatusRequestDTO: status
// (ACTIVE/MAINTENANCE/OFFLINE/FULL/INACTIVE) muda com frequência
// operacional; specs de hardware (totalCpu/totalMemoryMb/...) não
// têm endpoint de update por agora — mudar a capacidade de um
// servidor já registado não é um caso de uso previsto nesta
// sub-tarefa (fica para se/quando for preciso).
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateServerStatusRequestDTO {

    @NotBlank(message = "Status is required")
    private String status;
}