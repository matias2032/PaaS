package com.dev58.paasbackend.billing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

// No updated_at (plan_prices has no such column): a price row is an
// immutable historical fact once created. "Changing the price" means
// inserting a new row and closing the old one's effectiveUntil, never
// updating it in place.
//
// PRODUCT DECISION (grandfathering): an existing Subscription keeps
// pointing at the PlanPrice it subscribed to, so it is billed at that
// price on every renewal even after the plan gets a new price. Only new
// subscriptions and plan switches pick up the current price. This is
// intentional, not a side effect: nothing moves a subscription to a newer
// PlanPrice on its own (see PaymentService.chargeForRenewal).
@Entity
@Table(name = "plan_prices", schema = "paas_platform")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class PlanPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_plan_price")
    private Long idPlanPrice;

    @Builder.Default
    @Column(name = "public_uuid", nullable = false, updatable = false, unique = true)
    private UUID publicUuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_plan", nullable = false)
    private Plan plan;

    // MONTHLY | QUARTERLY | SEMIANNUAL | YEARLY (ck_plan_prices_cycle)
    @Column(name = "billing_cycle", nullable = false, length = 20)
    private String billingCycle;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "effective_from", nullable = false)
    private OffsetDateTime effectiveFrom;

    @Column(name = "effective_until")
    private OffsetDateTime effectiveUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (effectiveFrom == null) {
            effectiveFrom = now;
        }
        if (currency == null) {
            currency = "MZN";
        }
        createdAt = now;
    }
}