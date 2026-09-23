package com.dev58.paasbackend.audit_log.service;

import com.dev58.paasbackend.audit_log.dto.AuditLogResponseDTO;
import com.dev58.paasbackend.audit_log.entity.AuditLog;
import com.dev58.paasbackend.audit_log.repository.AuditLogRepository;
import com.dev58.paasbackend.auth.entity.User;
import com.dev58.paasbackend.organization.entity.Organization;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    // ==================== Admin (platform-side) ====================
    // Sem requireOwner/requireMembership — autorização é
    // @PreAuthorize("hasRole('SUPPORT')") no controller (nível mínimo
    // dos quatro módulos do handoff — ler logs é razoável para SUPPORT,
    // ao contrário de gerir planos ou suspender organizações). Sem
    // filtro nenhum — é exactamente esse o âmbito deste item (secção
    // 6.1): o filtro-por-organização é outro caminho, fora do âmbito.
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDTO> listAll(Pageable pageable) {
        return auditLogRepository.findAll(pageable)
                .map(this::toResponseDTO);
    }

    // ==================== Internal (chamado por outros módulos) ====================
    // Não exposto via Controller nenhum — chamado directamente por
    // outros Services (AuthService, OrganizationService, ApiKeyService,
    // etc.) quando/se decidirem gravar aqui. Ninguém chama isto ainda
    // (pergunta em aberto, secção 6.3 do handoff) — ligar as chamadas
    // é trabalho desses módulos, fora do âmbito desta sub-tarefa.
    //
    // organization/user recebidos já como entidades (não ids) para
    // evitar um lookup extra aqui — quem chama já os tem em mão na
    // maior parte dos casos (ex: dentro de suspendOrganization, a
    // Organization já está carregada).
    @Transactional
    public void record(
            Organization organization,
            User user,
            String action,
            String resourceType,
            String resourceIdentifier,
            String ipAddress,
            String userAgent,
            String metadataJson) {

        AuditLog log = AuditLog.builder()
                .organization(organization)
                .user(user)
                .action(action)
                .resourceType(resourceType)
                .resourceIdentifier(resourceIdentifier)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .metadata(metadataJson)
                .build();

        auditLogRepository.save(log);
    }

    // ==================== Internal helpers ====================

    private AuditLogResponseDTO toResponseDTO(AuditLog log) {
        return AuditLogResponseDTO.builder()
                .idAuditLog(log.getIdAuditLog())
                .organizationPublicUuid(
                        log.getOrganization() != null ? log.getOrganization().getPublicUuid() : null)
                .userPublicUuid(
                        log.getUser() != null ? log.getUser().getPublicUuid() : null)
                .action(log.getAction())
                .resourceType(log.getResourceType())
                .resourceIdentifier(log.getResourceIdentifier())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .metadata(log.getMetadata())
                .createdAt(log.getCreatedAt())
                .build();
    }
}