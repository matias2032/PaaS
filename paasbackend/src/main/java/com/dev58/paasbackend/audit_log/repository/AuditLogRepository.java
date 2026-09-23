package com.dev58.paasbackend.audit_log.repository;

import com.dev58.paasbackend.audit_log.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // findAll(Pageable) já vem do JpaRepository — cobre o caso base do
    // admin (sem filtro nenhum, paginado), que é o único consumidor
    // deste item (secção 6.2 do handoff). Sem métodos derivados extra
    // por agora: nenhum filtro por organização/utilizador/acção/data
    // foi pedido nesta sub-tarefa — Page<AuditLog> findAll(Pageable)
    // é suficiente para AuditLogService.listAll(pageable).
}