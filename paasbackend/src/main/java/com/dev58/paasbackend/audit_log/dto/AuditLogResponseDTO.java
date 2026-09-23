package com.dev58.paasbackend.audit_log.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

// Read-only — sem DTO de criação, como definido na secção 6.2 do
// handoff (a escrita, quando existir, é interna a cada módulo via
// AuditLogService.record(...), nunca uma API pública de "criar log").
//
// organizationPublicUuid/userPublicUuid em vez dos ids internos —
// mesmo padrão de referenciar por publicUuid usado no resto do
// projecto (ApiKeyResponseDTO.organizationPublicUuid, etc.). Ambos
// nullable, espelhando o nullable de AuditLog.organization/user —
// uma acção platform-side sem organização "dona" ou sem utilizador
// humano por trás (ex: trigger_type SYSTEM) fica com o campo a null.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponseDTO {

    private Long idAuditLog;

    private UUID organizationPublicUuid;

    private UUID userPublicUuid;

    private String action;

    private String resourceType;

    private String resourceIdentifier;

    private String ipAddress;

    private String userAgent;

    // Passado tal como vem da BD (jsonb como texto) — sem desserializar
    // para Map/Object aqui; quem consome a resposta (frontend) decide
    // como interpretar. Mantém o DTO simples e sem acoplar a uma lib
    // de JSON específica no lado da leitura também.
    private String metadata;

    private OffsetDateTime createdAt;
}