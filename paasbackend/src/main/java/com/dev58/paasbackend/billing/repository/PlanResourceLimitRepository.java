package com.dev58.paasbackend.billing.repository;

import com.dev58.paasbackend.billing.entity.PlanResourceLimit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlanResourceLimitRepository extends JpaRepository<PlanResourceLimit, Long> {

    // One-to-one with Plan (uq_plan_resource_limits_plan) — used to
    // embed the limits into PlanResponseDTO alongside the plan itself.
    Optional<PlanResourceLimit> findByPlan_IdPlan(Long idPlan);
}