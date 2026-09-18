package com.dev58.paasbackend.billing.entity;

import com.dev58.paasbackend.organization.entity.Organization;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "subscriptions", schema = "paas_platform")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_subscription")
    private Long idSubscription;

    @Column(name = "public_uuid", nullable = false, updatable = false)
    private UUID publicUuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_organization", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_plan_price", nullable = false)
    private PlanPrice planPrice;

    // PENDING | ACTIVE | PAST_DUE | SUSPENDED | CANCELLED | EXPIRED
    // (ck_subscriptions_status). Only one of PENDING/ACTIVE/PAST_DUE/
    // SUSPENDED may exist per organization at a time — enforced by
    // uq_active_subscription_per_organization (partial unique index),
    // not by JPA — the service layer must check for a live subscription
    // before creating a new one.
    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "current_period_start")
    private OffsetDateTime currentPeriodStart;

    @Column(name = "current_period_end")
    private OffsetDateTime currentPeriodEnd;

    @Column(name = "auto_renew", nullable = false)
    private Boolean autoRenew;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

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
            status = "PENDING";
        }
        if (autoRenew == null) {
            autoRenew = true;
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