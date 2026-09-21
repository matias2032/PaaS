package com.dev58.paasbackend.project.entity;

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
import java.util.UUID;

@Entity
@Table(name = "git_connections", schema = "paas_platform")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_git_connection")
    private Long idGitConnection;

    @Column(name = "public_uuid", nullable = false, updatable = false)
    private UUID publicUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_organization", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_git_provider", nullable = false)
    private GitProvider gitProvider;

    @Column(name = "external_account_id")
    private String externalAccountId;

    @Column(name = "external_account_name")
    private String externalAccountName;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "access_token_encrypted")
    private byte[] accessTokenEncrypted;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "refresh_token_encrypted")
    private byte[] refreshTokenEncrypted;

    @Column(name = "token_expires_at")
    private OffsetDateTime tokenExpiresAt;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.publicUuid == null) {
            this.publicUuid = UUID.randomUUID();
        }
        if (this.status == null) {
            this.status = "ACTIVE";
        }
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}