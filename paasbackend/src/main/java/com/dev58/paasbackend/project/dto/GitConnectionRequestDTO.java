package com.dev58.paasbackend.project.dto;

import jakarta.validation.constraints.NotBlank;
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
public class GitConnectionRequestDTO {

    @NotBlank
    private String gitProviderCode;

    private String externalAccountId;

    private String externalAccountName;

    // Optional Personal Access Token, plaintext over the wire only —
    // never stored or returned as-is. Encrypted with CryptoService
    // before persisting (see ProjectService.createGitConnection) and
    // never echoed back in GitConnectionResponseDTO, same pattern as
    // EnvironmentVariableRequestDTO.value. Left optional rather than
    // @NotBlank: a connection can still be registered "manually" with
    // just account info, same as before this change, for organizations
    // that don't need real API access yet.
    private String accessToken;
}