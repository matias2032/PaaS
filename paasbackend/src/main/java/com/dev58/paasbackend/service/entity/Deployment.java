// com.dev58.paasbackend.service.entity.Deployment
package com.dev58.paasbackend.service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "deployments", schema = "paas_platform")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Deployment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_deployment")
    private Long idDeployment;

    @Column(name = "public_uuid", nullable = false, updatable = false)
    private UUID publicUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_service", nullable = false)
    private Service service;

    @Column(name = "id_service_server_assignment")
    private Long idServiceServerAssignment;

    @Column(name = "coolify_deployment_uuid", length = 255)
    private String coolifyDeploymentUuid;

    @Column(name = "commit_hash", length = 255)
    private String commitHash;

    @Column(name = "commit_message", columnDefinition = "text")
    private String commitMessage;

    @Column(name = "branch", length = 255)
    private String branch;

    @Column(name = "trigger_type", nullable = false, length = 30)
    private String triggerType;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.publicUuid == null) {
            this.publicUuid = UUID.randomUUID();
        }
        if (this.status == null) {
            this.status = "QUEUED";
        }
        this.createdAt = OffsetDateTime.now();
    }
}