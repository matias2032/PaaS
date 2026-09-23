package com.dev58.paasbackend.api_key.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

// Usado em getKey/listKeys/revoke — nunca inclui rawKey nem keyHash.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyResponseDTO {

    private UUID publicUuid;

    private UUID organizationPublicUuid;

    private String name;

    private String keyPrefix;

    private String status;

    private OffsetDateTime lastUsedAt;

    private OffsetDateTime expiresAt;

    private OffsetDateTime createdAt;
}