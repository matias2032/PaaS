package com.dev58.paasbackend.api_key.entity;

import com.dev58.paasbackend.auth.entity.User;
import com.dev58.paasbackend.organization.entity.Organization;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "api_keys", schema = "paas_platform")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_api_key")
    private Long idApiKey;

    @Column(name = "public_uuid", nullable = false, updatable = false)
    private UUID publicUuid;

    // ManyToOne em vez de Long simples — o caminho client-facing precisa
    // de navegar até Organization para requireOwner()/requireMembership(),
    // tal como GitConnection faz no módulo project.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_organization", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user", nullable = false)
    private User createdByUser;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "key_hash", nullable = false, length = 255)
    private String keyHash;

    @Column(name = "key_prefix", nullable = false, length = 30)
    private String keyPrefix;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "last_used_at")
    private OffsetDateTime lastUsedAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // Preenchido apenas no momento da revogação (admin ou client-facing).
    // Fica NULL enquanto a chave está ACTIVE — ver V3__add_revocation_reason_to_api_keys.sql
    @Column(name = "revocation_reason")
    private String revocationReason;

    @PrePersist
    protected void onCreate() {
        if (publicUuid == null) {
            publicUuid = UUID.randomUUID();
        }
        if (status == null) {
            status = "ACTIVE";
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}