package com.dev58.paasbackend.audit_log.controller;

import com.dev58.paasbackend.audit_log.dto.AuditLogResponseDTO;
import com.dev58.paasbackend.audit_log.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    // Nível mínimo dos quatro módulos do handoff — SUPPORT, não
    // PLATFORM_ADMIN. RoleHierarchy (SecurityConfig) garante que
    // PLATFORM_ADMIN/PLATFORM_OWNER passam automaticamente.
    //
    // @PageableDefault(size=20, sort="createdAt", direction=DESC) —
    // sem isto, o Spring paginaria sem ordem definida por omissão;
    // logs mais recentes primeiro é o comportamento óbvio para este
    // caso de uso (mesma lógica do idx_audit_logs_created_at DESC
    // já existente no schema).
    @GetMapping
    @PreAuthorize("hasRole('SUPPORT')")
    public ResponseEntity<Page<AuditLogResponseDTO>> listAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {
        Page<AuditLogResponseDTO> response = auditLogService.listAll(pageable);
        return ResponseEntity.ok(response);
    }
}