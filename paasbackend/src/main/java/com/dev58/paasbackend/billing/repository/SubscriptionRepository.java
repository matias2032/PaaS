package com.dev58.paasbackend.billing.repository;

import com.dev58.paasbackend.billing.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByPublicUuid(UUID publicUuid);

    // Full history of subscriptions for an organization (past +
    // current) — used by admin/billing-history views.
    List<Subscription> findByOrganization_IdOrganization(Long idOrganization);

    // Mirrors uq_active_subscription_per_organization (partial unique
    // index on PENDING/ACTIVE/PAST_DUE/SUSPENDED) — the service layer
    // must call this before creating a subscription, the same way
    // OrganizationService.createOrganization checks existsBySlug
    // before insert, since JPA can't enforce a partial unique
    // constraint on its own.
    Optional<Subscription> findByOrganization_IdOrganizationAndStatusIn(Long idOrganization, List<String> statuses);
}