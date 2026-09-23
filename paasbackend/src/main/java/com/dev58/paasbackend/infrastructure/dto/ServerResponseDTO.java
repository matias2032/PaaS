package com.dev58.paasbackend.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * coolifyInstance e serverProvider são achatados aqui (nome +
 * identificador) em vez de aninhar CoolifyInstanceResponseDTO/
 * ServerProviderResponseDTO inteiros — o consumidor deste DTO (ex:
 * lista de servidores no admin) raramente precisa do apiToken/status
 * completo da instância só para mostrar "a que Coolify pertence este
 * servidor". Se um dia for preciso, adiciona-se um endpoint dedicado
 * GET /coolify-instances/{uuid} em vez de inchar este DTO sempre.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServerResponseDTO {

    private UUID publicUuid;

    private UUID coolifyInstancePublicUuid;

    private String coolifyInstanceName;

    private Short idServerProvider;

    private String serverProviderName;

    private String name;

    private String coolifyServerUuid;

    private String hostname;

    private String publicIp;

    private String region;

    private BigDecimal totalCpu;

    private Long totalMemoryMb;

    private Long totalStorageMb;

    private String status;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}