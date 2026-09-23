package com.dev58.paasbackend.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

// apiToken nunca aparece aqui, nem sequer o prefixo — ao contrário de
// ApiKey (que expõe key_prefix), o token da Coolify é usado só pelo
// backend para chamar a API dela; não há caso de uso para o admin
// precisar de o ver de volta depois de criado (se precisar de o
// mudar, gera um novo e atualiza a instância).
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoolifyInstanceResponseDTO {

    private UUID publicUuid;

    private String name;

    private String baseUrl;

    private String status;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}