// com.dev58.paasbackend.service.dto.ServiceTypeResponseDTO
package com.dev58.paasbackend.service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ServiceTypeResponseDTO {
    private Short idServiceType;
    private String code;
    private String name;
}