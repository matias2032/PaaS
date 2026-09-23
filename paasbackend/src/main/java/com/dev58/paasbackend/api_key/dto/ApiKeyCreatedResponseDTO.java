package com.dev58.paasbackend.api_key.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

// Resposta exclusiva do momento de criação — é a ÚNICA vez que rawKey
// aparece em qualquer resposta da API. Depois disto, só ApiKeyResponseDTO
// (sem rawKey nem hash) é usado para leitura/listagem.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyCreatedResponseDTO {

    private UUID publicUuid;

    private String name;

    // A chave completa em claro — mostrar ao utilizador UMA vez, avisar
    // no frontend que não será possível voltar a vê-la.
    private String rawKey;

    private String keyPrefix;

    private String status;

    private OffsetDateTime expiresAt;

    private OffsetDateTime createdAt;
}