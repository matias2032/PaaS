package com.dev58.paasbackend.billing.controller;

import com.dev58.paasbackend.billing.dto.*;
import com.dev58.paasbackend.billing.service.BillingService;
import com.dev58.paasbackend.common.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * No class-level @RequestMapping because this controller unites two
 * resources with different root paths: /api/plans (catalog) and
 * /api/organizations/{orgPublicUuid}/subscription* (per organization)
 * — each method declares its full path.
 *
 * Writes to /api/plans (catalog) are PLATFORM_ADMIN — see @PreAuthorize
 * on each catalog endpoint below. Subscriptions remain authorized via
 * BillingService.requireOwner (the organization's own OWNER), with no
 * platform-level @PreAuthorize at all.
 */

@RestController
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    // ---- Plans ----

@PostMapping("/api/plans")
@ResponseStatus(HttpStatus.CREATED)
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public PlanResponseDTO createPlan(@Valid @RequestBody PlanRequestDTO request) {
    return billingService.createPlan(request);
}

// Admin-facing — todos os status (ACTIVE/INACTIVE/ARCHIVED), ao
// contrário de qualquer listagem pública que só mostre ACTIVE.
@GetMapping("/api/plans/all")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public List<PlanResponseDTO> listAllPlans() {
    return billingService.listAllPlans();
}

@PutMapping("/api/plans/{publicUuid}")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public PlanResponseDTO updatePlan(
        @PathVariable UUID publicUuid,
        @Valid @RequestBody PlanRequestDTO request) {
    return billingService.updatePlan(publicUuid, request);
}

@DeleteMapping("/api/plans/{publicUuid}")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public PlanResponseDTO deactivatePlan(@PathVariable UUID publicUuid) {
    return billingService.deactivatePlan(publicUuid);
}

@PostMapping("/api/plans/{publicUuid}/reactivate")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public PlanResponseDTO reactivatePlan(@PathVariable UUID publicUuid) {
    return billingService.reactivatePlan(publicUuid);
}

@PostMapping("/api/plans/{publicUuid}/archive")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public PlanResponseDTO archivePlan(@PathVariable UUID publicUuid) {
    return billingService.archivePlan(publicUuid);
}

@PutMapping("/api/plans/{publicUuid}/resource-limits")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public PlanResourceLimitResponseDTO setResourceLimits(
        @PathVariable UUID publicUuid,
        @Valid @RequestBody PlanResourceLimitRequestDTO request) {
    return billingService.setResourceLimits(publicUuid, request);
}

@PostMapping("/api/plans/{publicUuid}/prices")
@ResponseStatus(HttpStatus.CREATED)
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public PlanPriceResponseDTO addPrice(
        @PathVariable UUID publicUuid,
        @Valid @RequestBody PlanPriceRequestDTO request) {
    return billingService.addPrice(publicUuid, request);
}

    // ---- Subscriptions ----

    @PostMapping("/api/organizations/{orgPublicUuid}/subscription")
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionResponseDTO subscribe(
            @PathVariable UUID orgPublicUuid,
            @Valid @RequestBody SubscriptionRequestDTO request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return billingService.subscribe(orgPublicUuid, request, currentUser.getIdUser());
    }

    @GetMapping("/api/organizations/{orgPublicUuid}/subscription")
    public SubscriptionResponseDTO getCurrentSubscription(
            @PathVariable UUID orgPublicUuid,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return billingService.getCurrentSubscription(orgPublicUuid, currentUser.getIdUser());
    }

    @GetMapping("/api/organizations/{orgPublicUuid}/subscriptions")
    public List<SubscriptionResponseDTO> listSubscriptionHistory(
            @PathVariable UUID orgPublicUuid,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return billingService.listSubscriptionHistory(orgPublicUuid, currentUser.getIdUser());
    }

    @DeleteMapping("/api/organizations/{orgPublicUuid}/subscription")
    public SubscriptionResponseDTO cancelSubscription(
            @PathVariable UUID orgPublicUuid,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return billingService.cancelSubscription(orgPublicUuid, currentUser.getIdUser());
    }

    @PutMapping("/api/organizations/{orgPublicUuid}/subscription")
public SubscriptionResponseDTO switchSubscription(
        @PathVariable UUID orgPublicUuid,
        @Valid @RequestBody SubscriptionRequestDTO request,
        @AuthenticationPrincipal AuthenticatedUser currentUser) {
    return billingService.switchSubscription(orgPublicUuid, request, currentUser.getIdUser());
}
}