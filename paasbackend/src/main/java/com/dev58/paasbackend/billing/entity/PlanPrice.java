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

// No public_uuid and no updated_at (see schema: plan_prices has no
// updated_at column) — a price row is treated as an immutable
// historical fact once created; "changing the price" means inserting
// a new row and closing the old one's effectiveUntil, not updating
// this one in place.
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

    @Column(name = "public_uuid", nullable = false, updatable = false)
    private UUID publicUuid;

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