package com.dev58.paasbackend.infrastructure.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

// A Coolify control-plane instance the platform talks to. Admin-only
// concept — no id_organization anywhere on this table, which is
// exactly why platform_role exists (see the platform_role handoff):
// requireMembership/requireOwner over organization_members has
// nothing to check here.
@Entity
@Table(name = "coolify_instances", schema = "paas_platform")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoolifyInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_coolify_instance")
    private Long idCoolifyInstance;

    @Column(name = "public_uuid", nullable = false, updatable = false)
    private UUID publicUuid;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;

    // Encrypted with CryptoService (AES-256/GCM) before persisting —
    // never stored or logged in plaintext. Same pattern already used
    // for EnvironmentVariable.valueEncrypted; GitConnection's tokens
    // will follow the same convention. Service layer is responsible
    // for calling CryptoService.encrypt()/decrypt(), not this entity.
    @Column(name = "api_token_encrypted", nullable = false)
    private byte[] apiTokenEncrypted;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (publicUuid == null) {
            publicUuid = UUID.randomUUID();
        }
        if (status == null) {
            status = "ACTIVE";
        }
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}