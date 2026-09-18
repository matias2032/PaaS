package com.dev58.paasbackend.billing.repository;

import com.dev58.paasbackend.billing.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    Optional<Plan> findByPublicUuid(UUID publicUuid);

    boolean existsBySlug(String slug);

    // Used by the client-facing "list plans" endpoint — only ACTIVE
    // plans are offered for new subscriptions (mirrors how
    // OrganizationService filters by status, but here it's the query
    // itself that filters, since there's no per-row authorization check
    // like membership).
    List<Plan> findByStatus(String status);
}