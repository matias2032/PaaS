package com.dev58.paasbackend.api_key.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyRequestDTO {

    @NotBlank(message = "Name is required")
    @Size(max = 120, message = "Name must be at most 120 characters long")
    private String name;

    // Opcional — uma api_key sem expiresAt nunca expira por tempo,
    // só é desativada por revogação manual (status = REVOKED).
    private OffsetDateTime expiresAt;
}