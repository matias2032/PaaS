// com.dev58.paasbackend.service.dto.ServiceRequestDTO
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
public class ServiceRequestDTO {

    @NotBlank
    @Size(max = 150)
    private String name;

    @NotBlank
    @Size(max = 150)
    private String slug;

    @NotBlank
    @Size(max = 40)
    private String serviceTypeCode;

    private Boolean autoDeploy;
}