package com.dev58.paasbackend.audit_log.entity;

import com.dev58.paasbackend.auth.entity.User;
import com.dev58.paasbackend.organization.entity.Organization;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "audit_logs", schema = "paas_platform")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_audit_log")
    private Long idAuditLog;

    // Nullable — uma acção platform-side (ex: revokeApiKeyAsAdmin,
    // suspendOrganization) pode não ter organização "dona" nenhuma,
    // ou pode ter mais do que uma envolvida indirectamente. Quando
    // aplicável, aponta à organização afectada.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_organization")
    private Organization organization;

    // Nullable — acções do sistema (ex: SYSTEM trigger_type em
    // deployments) não têm um utilizador humano por trás.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user")
    private User user;

    // Texto livre por agora (ex: "API_KEY_REVOKED_BY_ADMIN",
    // "ORGANIZATION_SUSPENDED") — sem enum/tabela catálogo, ao
    // contrário de service_types/git_providers, porque a lista de
    // acções cresce à medida que cada módulo adopta record() e não
    // vale a pena uma migration por cada acção nova.
    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "resource_type", length = 100)
    private String resourceType;

    // publicUuid (ou outro identificador estável) do recurso afectado,
    // como texto — nunca o id interno, mesmo padrão de referenciar por
    // publicUuid usado no resto do projecto.
    @Column(name = "resource_identifier", length = 255)
    private String resourceIdentifier;

    // inet no Postgres — mapeado como String; converter para InetAddress
    // não traz vantagem aqui (nunca é usado para range queries).
    @Column(name = "ip_address", columnDefinition = "inet")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    // jsonb — payload livre por acção (ex: {"oldStatus":"ACTIVE",
    // "newStatus":"SUSPENDED"}). String simples + JdbcTypeCode em vez de
    // Map<String,Object>: mantém o Service que chama record() livre de
    // decidir a própria serialização (json já pronto), sem acoplar esta
    // entity a uma lib de JSON específica.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}