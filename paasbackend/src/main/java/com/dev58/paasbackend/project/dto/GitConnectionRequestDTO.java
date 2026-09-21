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
}