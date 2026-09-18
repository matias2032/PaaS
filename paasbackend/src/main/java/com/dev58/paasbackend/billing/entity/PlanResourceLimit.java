package com.dev58.paasbackend.billing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

// One-to-one with Plan (uq_plan_resource_limits_plan). No public_uuid —
// this row is never addressed directly by clients, only ever read as
// part of a Plan (mirrors how OrganizationMember has no public_uuid
// of its own).
@Entity
@Table(name = "plan_resource_limits", schema = "paas_platform")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanResourceLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_plan_resource_limit")
    private Long idPlanResourceLimit;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_plan", nullable = false, unique = true)
    private Plan plan;

    @Column(name = "cpu_limit", nullable = false, precision = 8, scale = 3)
    private BigDecimal cpuLimit;

    @Column(name = "memory_limit_mb", nullable = false)
    private Long memoryLimitMb;

    @Column(name = "storage_limit_mb", nullable = false)
    private Long storageLimitMb;

    @Column(name = "max_projects", nullable = false)
    private Integer maxProjects;

    @Column(name = "max_services", nullable = false)
    private Integer maxServices;

    @Column(name = "max_domains", nullable = false)
    private Integer maxDomains;

    @Column(name = "max_environment_variables")
    private Integer maxEnvironmentVariables;

    @Column(name = "bandwidth_limit_mb")
    private Long bandwidthLimitMb;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}