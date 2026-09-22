// com.dev58.paasbackend.service.dto.DeploymentRequestDTO
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
public class DeploymentRequestDTO {

    @NotBlank
    @Size(max = 30)
    private String triggerType;

    @Size(max = 255)
    private String commitHash;

    private String commitMessage;

    @Size(max = 255)
    private String branch;
}