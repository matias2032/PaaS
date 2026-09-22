// com.dev58.paasbackend.service.entity.Service
package com.dev58.paasbackend.service.entity;

import com.dev58.paasbackend.project.entity.Project;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "services", schema = "paas_platform")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_service")
    private Long idService;

    @Column(name = "public_uuid", nullable = false, updatable = false)
    private UUID publicUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_project", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_service_type", nullable = false)
    private ServiceType serviceType;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "slug", nullable = false, length = 150)
    private String slug;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "auto_deploy", nullable = false)
    private Boolean autoDeploy;

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
            this.status = "CREATED";
        }
        if (this.autoDeploy == null) {
            this.autoDeploy = true;
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