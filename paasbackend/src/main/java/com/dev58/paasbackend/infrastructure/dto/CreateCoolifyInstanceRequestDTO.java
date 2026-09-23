package com.dev58.paasbackend.infrastructure.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * apiToken chega em texto plano aqui — é a única forma de o admin o
 * fornecer. O InfrastructureService encripta-o com CryptoService antes
 * de persistir (mesmo padrão de EnvironmentVariable.valueEncrypted);
 * este DTO nunca é usado para devolver o valor de volta, só para o
 * receber na criação.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCoolifyInstanceRequestDTO {

    @NotBlank(message = "Name is required")
    @Size(max = 120, message = "Name must be at most 120 characters long")
    private String name;

    @NotBlank(message = "Base URL is required")
    @Size(max = 500, message = "Base URL must be at most 500 characters long")
    private String baseUrl;

    @NotBlank(message = "API token is required")
    private String apiToken;
}