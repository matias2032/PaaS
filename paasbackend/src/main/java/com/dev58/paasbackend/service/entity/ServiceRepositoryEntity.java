// com.dev58.paasbackend.service.entity.ServiceRepositoryEntity
package com.dev58.paasbackend.service.entity;

import com.dev58.paasbackend.project.entity.GitConnection;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "service_repositories", schema = "paas_platform")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceRepositoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_service_repository")
    private Long idServiceRepository;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_service", nullable = false, unique = true)
    private Service service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_git_connection")
    private GitConnection gitConnection;

    @Column(name = "repository_url", nullable = false, length = 1000)
    private String repositoryUrl;

    @Column(name = "repository_owner", length = 255)
    private String repositoryOwner;

    @Column(name = "repository_name", length = 255)
    private String repositoryName;

    @Column(name = "branch", nullable = false, length = 255)
    private String branch;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.branch == null) {
            this.branch = "main";
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