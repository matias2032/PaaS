package com.dev58.paasbackend.billing.controller;

import com.dev58.paasbackend.billing.dto.*;
import com.dev58.paasbackend.billing.service.BillingService;
import com.dev58.paasbackend.common.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Sem @RequestMapping de classe porque une dois recursos com raízes
 * diferentes: /api/plans (catálogo) e
 * /api/organizations/{orgPublicUuid}/subscription* (por organização) —
 * cada método declara o path completo.
 *
 * NOTA: ainda sem distinção admin/cliente (ver handoff do frontend,
 * secção 6 — depende de platform_role, por decidir). Por agora todos
 * os endpoints de escrita em /api/plans ficam apenas atrás de
 * autenticação normal (SecurityConfig), sem verificação de role de
 * plataforma — reforçar assim que platform_role existir.
 */
@RestController
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    // ---- Plans ----

    @PostMapping("/api/plans")
    @ResponseStatus(HttpStatus.CREATED)
    public PlanResponseDTO createPlan(@Valid @RequestBody PlanRequestDTO request) {
        return billingService.createPlan(request);
    }

    @GetMapping("/api/plans")
    public List<PlanResponseDTO> listActivePlans() {
        return billingService.listActivePlans();
    }

    @GetMapping("/api/plans/{publicUuid}")
    public PlanResponseDTO getPlan(@PathVariable UUID publicUuid) {
        return billingService.getPlan(publicUuid);
    }

    @PutMapping("/api/plans/{publicUuid}")
    public PlanResponseDTO updatePlan(
            @PathVariable UUID publicUuid,
            @Valid @RequestBody PlanRequestDTO request) {
        return billingService.updatePlan(publicUuid, request);
    }

    @DeleteMapping("/api/plans/{publicUuid}")
    public PlanResponseDTO deactivatePlan(@PathVariable UUID publicUuid) {
        return billingService.deactivatePlan(publicUuid);
    }

    @PostMapping("/api/plans/{publicUuid}/reactivate")
    public PlanResponseDTO reactivatePlan(@PathVariable UUID publicUuid) {
        return billingService.reactivatePlan(publicUuid);
    }

    @PostMapping("/api/plans/{publicUuid}/archive")
    public PlanResponseDTO archivePlan(@PathVariable UUID publicUuid) {
        return billingService.archivePlan(publicUuid);
    }

    @PutMapping("/api/plans/{publicUuid}/resource-limits")
    public PlanResourceLimitResponseDTO setResourceLimits(
            @PathVariable UUID publicUuid,
            @Valid @RequestBody PlanResourceLimitRequestDTO request) {
        return billingService.setResourceLimits(publicUuid, request);
    }

    @PostMapping("/api/plans/{publicUuid}/prices")
    @ResponseStatus(HttpStatus.CREATED)
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