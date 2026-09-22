// com.dev58.paasbackend.service.dto.ServiceBuildConfigRequestDTO
package com.dev58.paasbackend.service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class ServiceBuildConfigRequestDTO {

    @Size(max = 500)
    private String rootDirectory;

    private String buildCommand;

    private String startCommand;

    @Size(max = 500)
    private String dockerfilePath;

    @Size(max = 500)
    private String healthCheckPath;

    @Min(1)
    @Max(65535)
    private Integer port;
}