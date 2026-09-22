// com.dev58.paasbackend.service.dto.ServiceRepositoryRequestDTO
package com.dev58.paasbackend.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRepositoryRequestDTO {

    private UUID gitConnectionPublicUuid;

    @NotBlank
    @Size(max = 1000)
    private String repositoryUrl;

    @Size(max = 255)
    private String repositoryOwner;

    @Size(max = 255)
    private String repositoryName;

    @Size(max = 255)
    private String branch;
}