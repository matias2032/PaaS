// com.dev58.paasbackend.service.dto.EnvironmentVariableRequestDTO
package com.dev58.paasbackend.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnvironmentVariableRequestDTO {

    @NotBlank
    @Size(max = 255)
    private String variableKey;

    @NotBlank
    private String value;

    private Boolean isSecret;
}