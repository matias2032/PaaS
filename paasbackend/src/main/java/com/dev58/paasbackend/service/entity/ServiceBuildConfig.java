// com.dev58.paasbackend.service.entity.ServiceBuildConfig
package com.dev58.paasbackend.service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "service_build_configs", schema = "paas_platform")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceBuildConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_service_build_config")
    private Long idServiceBuildConfig;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_service", nullable = false, unique = true)
    private Service service;

    @Column(name = "root_directory", nullable = false, length = 500)
    private String rootDirectory;

    @Column(name = "build_command", columnDefinition = "text")
    private String buildCommand;

    @Column(name = "start_command", columnDefinition = "text")
    private String startCommand;

    @Column(name = "dockerfile_path", length = 500)
    private String dockerfilePath;

    @Column(name = "health_check_path", length = 500)
    private String healthCheckPath;

    @Column(name = "port")
    private Integer port;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.rootDirectory == null) {
            this.rootDirectory = "/";
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