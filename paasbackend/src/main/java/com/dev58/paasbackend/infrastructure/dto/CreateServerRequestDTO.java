package com.dev58.paasbackend.infrastructure.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * idCoolifyInstance é referenciado pelo publicUuid (nunca o bigint
 * interno, mesmo padrão usado em toda a API — ver AuthController).
 * idServerProvider é a exceção: ServerProvider não tem public_uuid
 * (é catálogo), por isso é referenciado pelo próprio id (Short),
 * exactamente como devolvido por ServerProviderResponseDTO. Opcional,
 * porque id_server_provider é nullable na tabela.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateServerRequestDTO {

    @NotNull(message = "Coolify instance is required")
    private UUID coolifyInstancePublicUuid;

    private Short idServerProvider;

    @NotBlank(message = "Name is required")
    @Size(max = 120, message = "Name must be at most 120 characters long")
    private String name;

    @NotBlank(message = "Coolify server UUID is required")
    @Size(max = 255, message = "Coolify server UUID must be at most 255 characters long")
    private String coolifyServerUuid;

    @Size(max = 255, message = "Hostname must be at most 255 characters long")
    private String hostname;

    // Validado como formato IPv4/IPv6 no service (Server.publicIp é
    // escrito com o cast ?::inet — um valor malformado só falha lá,
    // por isso vale a pena confirmar cedo aqui também; @Pattern com
    // regex de IP fica para o Service, mais simples de manter num
    // só sítio do que duplicar a regex aqui).
    private String publicIp;

    @Size(max = 100, message = "Region must be at most 100 characters long")
    private String region;

    @NotNull(message = "Total CPU is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Total CPU must be greater than zero")
    private BigDecimal totalCpu;

    @NotNull(message = "Total memory is required")
    @Positive(message = "Total memory must be greater than zero")
    private Long totalMemoryMb;

    @NotNull(message = "Total storage is required")
    @Positive(message = "Total storage must be greater than zero")
    private Long totalStorageMb;
}