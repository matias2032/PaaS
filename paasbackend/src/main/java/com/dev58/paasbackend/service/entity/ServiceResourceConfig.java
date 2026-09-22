// com.dev58.paasbackend.service.entity.ServiceResourceConfig
package com.dev58.paasbackend.service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "service_resource_configs", schema = "paas_platform")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceResourceConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_service_resource_config")
    private Long idServiceResourceConfig;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_service", nullable = false, unique = true)
    private Service service;

    @Column(name = "cpu_limit", nullable = false, precision = 8, scale = 3)
    private BigDecimal cpuLimit;

    @Column(name = "memory_limit_mb", nullable = false)
    private Long memoryLimitMb;

    @Column(name = "storage_limit_mb", nullable = false)
    private Long storageLimitMb;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}