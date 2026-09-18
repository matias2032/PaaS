package com.dev58.paasbackend.billing.repository;

import com.dev58.paasbackend.billing.entity.PlanPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

import java.util.List;
import java.util.Optional;

public interface PlanPriceRepository extends JpaRepository<PlanPrice, Long> {

    Optional<PlanPrice> findByPublicUuid(UUID publicUuid);

    List<PlanPrice> findByPlan_IdPlan(Long idPlan);

    Optional<PlanPrice> findByPlan_IdPlanAndBillingCycleAndEffectiveUntilIsNull(Long idPlan, String billingCycle);
}