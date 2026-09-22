// com.dev58.paasbackend.service.entity.Domain
package com.dev58.paasbackend.service.entity;

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
@Table(name = "domains", schema = "paas_platform")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Domain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_domain")
    private Long idDomain;

    @Column(name = "public_uuid", nullable = false, updatable = false)
    private UUID publicUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_service", nullable = false)
    private Service service;

    @JdbcTypeCode(SqlTypes.OTHER)
    @Column(name = "hostname", nullable = false)
    private String hostname;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary;

    @Column(name = "verification_status", nullable = false, length = 30)
    private String verificationStatus;

    @Column(name = "ssl_status", nullable = false, length = 30)
    private String sslStatus;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.publicUuid == null) {
            this.publicUuid = UUID.randomUUID();
        }
        if (this.isPrimary == null) {
            this.isPrimary = false;
        }
        if (this.verificationStatus == null) {
            this.verificationStatus = "PENDING";
        }
        if (this.sslStatus == null) {
            this.sslStatus = "PENDING";
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