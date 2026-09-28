package com.dev58.paasbackend.audit_log.service;

import com.dev58.paasbackend.audit_log.dto.AuditLogResponseDTO;
import com.dev58.paasbackend.audit_log.entity.AuditLog;
import com.dev58.paasbackend.audit_log.repository.AuditLogRepository;
import com.dev58.paasbackend.auth.entity.User;
import com.dev58.paasbackend.auth.repository.UserRepository;
import com.dev58.paasbackend.common.security.AuthenticatedUser;
import com.dev58.paasbackend.organization.entity.Organization;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final int MAX_USER_AGENT_LENGTH = 500;
    private static final Pattern IPV4 = Pattern.compile("^(\\d{1,3}\\.){3}\\d{1,3}$");
    private static final Pattern IPV6_CHARS = Pattern.compile("^[0-9a-fA-F:.]+$");

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // ==================== Admin (platform-side) ====================
    // Autorização é @PreAuthorize("hasRole('SUPPORT')") no controller.
    @Transactional(readOnly = true)
    public Page<AuditLogResponseDTO> listAll(Pageable pageable) {
        return auditLogRepository.findAll(pageable)
                .map(this::toResponseDTO);
    }

    // ==================== Internal (chamado por outros Services) ====================

    /**
     * Grava uma entrada na MESMA transacção da acção (REQUIRED): se a acção
     * falhar, o log desaparece com ela; se o log falhar, a acção reverte
     * (fail-closed, de propósito).
     *
     * Actor: SecurityContext (null em acções sem utilizador autenticado,
     * ex: login — usar o overload com User explícito quando existir).
     * IP/user agent: pedido HTTP corrente (null fora de um pedido).
     * Nunca meter rawKey, keyHash ou palavras-passe em metadata.
     */
    @Transactional
    public void record(
            Organization organization,
            String action,
            String resourceType,
            String resourceIdentifier,
            Map<String, Object> metadata) {

        HttpServletRequest request = currentRequest();

        AuditLog entry = AuditLog.builder()
                .organization(organization)
                .user(currentUserReference())
                .action(action)
                .resourceType(resourceType)
                .resourceIdentifier(resourceIdentifier)
                .ipAddress(request != null ? sanitizeIp(request.getRemoteAddr()) : null)
                .userAgent(request != null ? truncate(request.getHeader("User-Agent")) : null)
                .metadata(toJson(metadata))
                .build();

        auditLogRepository.save(entry);
    }

    /** Atalho para montar metadata sem Map.of (que rejeita null): meta("k1", v1, "k2", v2). */
    public static Map<String, Object> meta(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    // ==================== Internal helpers ====================

    private HttpServletRequest currentRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs
                ? attrs.getRequest()
                : null;
    }

    // getReferenceById: proxy sem SELECT — só precisamos do id para o FK.
    private User currentUserReference() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser principal) {
            return userRepository.getReferenceById(principal.getIdUser());
        }
        return null;
    }

    // Um valor inválido rebentaria o cast ?::inet e abortaria a transacção
    // da acção. Só aceita literais (nunca faz DNS): IPv4 estrito, ou IPv6
    // com ':' (getByName trata-o como literal).
    private String sanitizeIp(String candidate) {
        if (candidate == null) return null;
        String ip = candidate.trim();
        try {
            if (IPV4.matcher(ip).matches() || (ip.contains(":") && IPV6_CHARS.matcher(ip).matches())) {
                return InetAddress.getByName(ip).getHostAddress();
            }
        } catch (Exception ignored) {
            // cai para null
        }
        return null;
    }

    private String truncate(String value) {
        if (value == null) return null;
        return value.length() <= MAX_USER_AGENT_LENGTH ? value : value.substring(0, MAX_USER_AGENT_LENGTH);
    }

    private String toJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JacksonException e) {
            throw new IllegalStateException("Could not serialize audit metadata", e);
        }
    }

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