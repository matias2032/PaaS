package com.dev58.paasbackend.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Usado para create e update do Plan em si — não toca em limites nem
// em preços, que têm o seu próprio DTO/fluxo de admin (tal como
// OrganizationRequestDTO não mexe em membros).
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanRequestDTO {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Size(max = 100)
    private String slug;

    private String description;
}