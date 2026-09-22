// com.dev58.paasbackend.service.dto.EnvironmentVariableResponseDTO
package com.dev58.paasbackend.service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
public class EnvironmentVariableResponseDTO {
    // Deliberately no `value`/`valueEncrypted` field — per handoff
    // section 3, never return the plaintext value in a listing.
    private String variableKey;
    private Boolean isSecret;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}