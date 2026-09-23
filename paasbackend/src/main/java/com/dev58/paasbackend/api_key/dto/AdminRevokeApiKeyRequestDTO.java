package com.dev58.paasbackend.api_key.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Corpo do PATCH /api/admin/api-keys/{publicUuid}/revoke — reason
// obrigatório do lado admin (é uma ação sobre a chave de outra
// organização, motivada por incidente de segurança; ao contrário da
// auto-revogação client-facing, aqui faz sentido exigir justificação).
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminRevokeApiKeyRequestDTO {

    @NotBlank(message = "Reason is required")
    private String reason;
}