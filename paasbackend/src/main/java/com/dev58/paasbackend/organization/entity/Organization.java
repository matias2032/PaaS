package com.dev58.paasbackend.organization.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "organizations", schema = "paas_platform")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_organization")
    private Long idOrganization;

    @Column(name = "public_uuid", nullable = false, updatable = false)
    private UUID publicUuid;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "slug", nullable = false, length = 150, unique = true)
    private String slug;

    // ACTIVE | SUSPENDED | INACTIVE (see ck_organizations_status)
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    // Only set when status = SUSPENDED (platform-side action). Cleared
    // when liftSuspension() runs. Null in every other state.
    @Column(name = "suspension_reason")
    private String suspensionReason;

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