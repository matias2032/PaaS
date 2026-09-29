package com.dev58.paasbackend.billing.service;

import com.dev58.paasbackend.billing.dto.*;
import com.dev58.paasbackend.billing.entity.Plan;
import com.dev58.paasbackend.billing.entity.PlanPrice;
import com.dev58.paasbackend.billing.entity.PlanResourceLimit;
import com.dev58.paasbackend.billing.entity.Subscription;
import com.dev58.paasbackend.billing.exception.PlanArchivedException;
import com.dev58.paasbackend.billing.exception.PlanNotFoundException;
import com.dev58.paasbackend.billing.exception.PlanPriceNotFoundException;
import com.dev58.paasbackend.billing.exception.PlanSlugAlreadyExistsException;
import com.dev58.paasbackend.billing.exception.SubscriptionAlreadyExistsException;
import com.dev58.paasbackend.billing.exception.SubscriptionNotFoundException;
import com.dev58.paasbackend.billing.repository.PlanPriceRepository;
import com.dev58.paasbackend.billing.repository.PlanRepository;
import com.dev58.paasbackend.billing.repository.PlanResourceLimitRepository;
import com.dev58.paasbackend.billing.repository.SubscriptionRepository;
import com.dev58.paasbackend.organization.entity.Organization;
import com.dev58.paasbackend.organization.entity.OrganizationMember;
import com.dev58.paasbackend.organization.exception.OrganizationInactiveException;
import com.dev58.paasbackend.organization.exception.OrganizationNotFoundException;
import com.dev58.paasbackend.organization.exception.PermissionDeniedException;
import com.dev58.paasbackend.organization.repository.OrganizationMemberRepository;
import com.dev58.paasbackend.organization.repository.OrganizationRepository;
import com.dev58.paasbackend.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Optional;

/**
 * Une Plan/PlanResourceLimit/PlanPrice e Subscription num só serviço —
 * decisão explícita (ao contrário de OrganizationService, que só cobre
 * Organization+OrganizationMember): billing é tratado como um domínio
 * único, não dois módulos separados.
 *
 * Autorização de catálogo (createPlan/updatePlan/deactivatePlan/
 * reactivatePlan/archivePlan/setResourceLimits/addPrice/listAllPlans)
 * vive só no BillingController via @PreAuthorize("hasRole('PLATFORM_ADMIN')")
 * — mesmo padrão de InfrastructureService, ApiKeyService.revokeApiKeyAsAdmin
 * e OrganizationService.suspendOrganization/liftSuspension. Nenhum
 * outro módulo chama estes métodos directamente, por isso uma única
 * barreira no Controller já cobre todo o caminho de chamada.
 */
@Service
@RequiredArgsConstructor
public class BillingService {

    private static final String ROLE_OWNER = "OWNER";

    private static final List<String> LIVE_SUBSCRIPTION_STATUSES =
            List.of("PENDING", "ACTIVE", "PAST_DUE", "SUSPENDED");

    // Mirrors ck_plan_prices_cycle (Phase 3 migration V3).
    private static final Set<String> SELLABLE_BILLING_CYCLES = Set.of("MONTHLY", "YEARLY");

    private final PlanRepository planRepository;
    private final PlanResourceLimitRepository planResourceLimitRepository;
    private final PlanPriceRepository planPriceRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final PaymentService paymentService;

    // ---- Plan: Create/Read/Update ----

    @Transactional
    public PlanResponseDTO createPlan(PlanRequestDTO request) {
        if (planRepository.existsBySlug(request.getSlug())) {
            throw new PlanSlugAlreadyExistsException("Slug already in use: " + request.getSlug());
        }

        Plan plan = Plan.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .description(request.getDescription())
                .build();
        plan = planRepository.save(plan);

        return toPlanResponseDTO(plan);
    }

    public PlanResponseDTO getPlan(UUID publicUuid) {
        return toPlanResponseDTO(findPlanOrThrow(publicUuid));
    }

    // Client-facing pricing listing — só planos ACTIVE. Planos
    // INACTIVE/ARCHIVED só aparecem via getPlan direto (ex: uma
    // subscrição antiga ainda referenciar um plano descontinuado).
    public List<PlanResponseDTO> listActivePlans() {
        return planRepository.findByStatus("ACTIVE").stream()
                .map(this::toPlanResponseDTO)
                .toList();
    }

    // Admin-facing listing — every status included (ACTIVE, INACTIVE,
    // ARCHIVED). Needed so an admin panel can find and reactivate a
    // hidden plan, which listActivePlans() would never surface.
    public List<PlanResponseDTO> listAllPlans() {
        return planRepository.findAll().stream()
                .map(this::toPlanResponseDTO)
                .toList();
    }

    @Transactional
    public PlanResponseDTO updatePlan(UUID publicUuid, PlanRequestDTO request) {
        Plan plan = findPlanOrThrow(publicUuid);
        requirePlanNotArchived(plan);

        plan.setName(request.getName());
        plan.setDescription(request.getDescription());
        // Slug intencionalmente não atualizado aqui — mesma decisão que
        // Organization.slug (ver OrganizationService.updateOrganization).
        plan = planRepository.save(plan);

        return toPlanResponseDTO(plan);
    }

    @Transactional
    public PlanResponseDTO deactivatePlan(UUID publicUuid) {
        Plan plan = findPlanOrThrow(publicUuid);
        if (!"ACTIVE".equals(plan.getStatus())) {
            throw new IllegalArgumentException("Only ACTIVE plans can be deactivated");
        }
        plan.setStatus("INACTIVE");
        return toPlanResponseDTO(planRepository.save(plan));
    }

    @Transactional
    public PlanResponseDTO reactivatePlan(UUID publicUuid) {
        Plan plan = findPlanOrThrow(publicUuid);
        if (!"INACTIVE".equals(plan.getStatus())) {
            throw new IllegalArgumentException("Only INACTIVE plans can be reactivated");
        }
        plan.setStatus("ACTIVE");
        return toPlanResponseDTO(planRepository.save(plan));
    }

    // Terminal — ao contrário de deactivate/reactivate, não há "un-archive".
    @Transactional
    public PlanResponseDTO archivePlan(UUID publicUuid) {
        Plan plan = findPlanOrThrow(publicUuid);
        plan.setStatus("ARCHIVED");
        return toPlanResponseDTO(planRepository.save(plan));
    }

    // ---- Plan resource limits ----

    @Transactional
    public PlanResourceLimitResponseDTO setResourceLimits(UUID planPublicUuid, PlanResourceLimitRequestDTO request) {
        Plan plan = findPlanOrThrow(planPublicUuid);
        requirePlanNotArchived(plan);

        PlanResourceLimit limits = planResourceLimitRepository.findByPlan_IdPlan(plan.getIdPlan())
                .orElseGet(() -> PlanResourceLimit.builder().plan(plan).build());

        limits.setCpuLimit(request.getCpuLimit());
        limits.setMemoryLimitMb(request.getMemoryLimitMb());
        limits.setStorageLimitMb(request.getStorageLimitMb());
        // PUT replaces the whole set: an omitted max* field is stored as NULL
        // (= unlimited). TECHNICAL DEBT: these limits are not enforced against
        // real Project/Service usage yet (see PlanResourceLimit).
        limits.setMaxProjects(request.getMaxProjects());
        limits.setMaxServices(request.getMaxServices());
        limits.setMaxDomains(request.getMaxDomains());
        limits.setMaxEnvironmentVariables(request.getMaxEnvironmentVariables());
        limits.setBandwidthLimitMb(request.getBandwidthLimitMb());

        limits = planResourceLimitRepository.save(limits);
        return toResourceLimitResponseDTO(limits);
    }

    // ---- Plan prices ----

    @Transactional
    public PlanPriceResponseDTO addPrice(UUID planPublicUuid, PlanPriceRequestDTO request) {
        if (!SELLABLE_BILLING_CYCLES.contains(request.getBillingCycle())) {
            throw new IllegalArgumentException("billingCycle must be MONTHLY or YEARLY");
        }
        Plan plan = findPlanOrThrow(planPublicUuid);
        requirePlanNotArchived(plan);
        OffsetDateTime now = OffsetDateTime.now();

        // Closes the current price of this cycle, if any; a PlanPrice in
        // force is never updated. Existing subscriptions are NOT migrated to
        // the new price: they keep the old one (grandfathering, see PlanPrice).
        planPriceRepository
                .findByPlan_IdPlanAndBillingCycleAndEffectiveUntilIsNull(plan.getIdPlan(), request.getBillingCycle())
                .ifPresent(current -> {
                    current.setEffectiveUntil(now);
                    planPriceRepository.save(current);
                });

        PlanPrice newPrice = PlanPrice.builder()
                .plan(plan)
                .billingCycle(request.getBillingCycle())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .effectiveFrom(now)
                .build();
        newPrice = planPriceRepository.save(newPrice);

        return toPriceResponseDTO(newPrice);
    }

    // ---- Subscriptions ----

    @Transactional
    public SubscriptionResponseDTO subscribe(UUID orgPublicUuid, SubscriptionRequestDTO request, Long currentUserId) {
        Organization organization = findOrganizationOrThrow(orgPublicUuid);
        requireOwner(organization, currentUserId);
        requireActiveOrganization(organization);

        if (subscriptionRepository
                .findByOrganization_IdOrganizationAndStatusIn(organization.getIdOrganization(), LIVE_SUBSCRIPTION_STATUSES)
                .isPresent()) {
            throw new SubscriptionAlreadyExistsException("Organization already has an active or pending subscription");
        }

 PlanPrice planPrice = planPriceRepository.findByPublicUuid(request.getPlanPricePublicUuid())
                .orElseThrow(() -> new PlanPriceNotFoundException("Plan price not found: " + request.getPlanPricePublicUuid()));
        requirePlanAvailable(planPrice);

        Subscription subscription = Subscription.builder()
                .organization(organization)
                .planPrice(planPrice)
                .build();
        subscription = subscriptionRepository.save(subscription);
        startSubscription(subscription);

        return toSubscriptionResponseDTO(subscription);
    }

    public SubscriptionResponseDTO getCurrentSubscription(UUID orgPublicUuid, Long currentUserId) {
        Organization organization = findOrganizationOrThrow(orgPublicUuid);
        requireMembership(organization, currentUserId);

        Subscription subscription = subscriptionRepository
                .findByOrganization_IdOrganizationAndStatusIn(organization.getIdOrganization(), LIVE_SUBSCRIPTION_STATUSES)
                .orElseThrow(() -> new SubscriptionNotFoundException("Organization has no active subscription"));

        return toSubscriptionResponseDTO(subscription);
    }

    public List<SubscriptionResponseDTO> listSubscriptionHistory(UUID orgPublicUuid, Long currentUserId) {
        Organization organization = findOrganizationOrThrow(orgPublicUuid);
        requireMembership(organization, currentUserId);

        return subscriptionRepository.findByOrganization_IdOrganization(organization.getIdOrganization()).stream()
                .map(this::toSubscriptionResponseDTO)
                .toList();
    }

    @Transactional
    public SubscriptionResponseDTO cancelSubscription(UUID orgPublicUuid, Long currentUserId) {
        Organization organization = findOrganizationOrThrow(orgPublicUuid);
        requireOwner(organization, currentUserId);
        requireActiveOrganization(organization);

        Subscription subscription = subscriptionRepository
                .findByOrganization_IdOrganizationAndStatusIn(organization.getIdOrganization(), LIVE_SUBSCRIPTION_STATUSES)
                .orElseThrow(() -> new SubscriptionNotFoundException("Organization has no active subscription"));

        subscription.setStatus("CANCELLED");
        subscription.setCancelledAt(OffsetDateTime.now());
        subscription.setAutoRenew(false);
        subscription = subscriptionRepository.save(subscription);
        paymentService.voidOpenInvoices(subscription, "Subscription cancelled");
        paymentService.voidOpenInvoices(subscription, "Subscription cancelled");

        return toSubscriptionResponseDTO(subscription);
    }


    @Transactional
    public SubscriptionResponseDTO switchSubscription(UUID orgPublicUuid, SubscriptionRequestDTO request, Long currentUserId) {
        Organization organization = findOrganizationOrThrow(orgPublicUuid);
        requireOwner(organization, currentUserId);
        requireActiveOrganization(organization);

        PlanPrice newPlanPrice = planPriceRepository.findByPublicUuid(request.getPlanPricePublicUuid())
                .orElseThrow(() -> new PlanPriceNotFoundException("Plan price not found: " + request.getPlanPricePublicUuid()));

        Optional<Subscription> currentSubscription = subscriptionRepository
                .findByOrganization_IdOrganizationAndStatusIn(organization.getIdOrganization(), LIVE_SUBSCRIPTION_STATUSES);

        if (currentSubscription.isPresent()) {
            Subscription current = currentSubscription.get();

            // Guarda contra "trocar" para o mesmo PlanPrice já ativo — sem
            // isto, uma chamada direta ao endpoint (o frontend já desativa
            // o botão nesse caso, mas o endpoint em si não se protegia)
            // conseguiria cancelar e recriar a mesma subscrição sem
            // necessidade, perdendo currentPeriodStart e a continuidade do
            // histórico sem qualquer benefício real.
            if (current.getPlanPrice().getPublicUuid().equals(newPlanPrice.getPublicUuid())) {
                throw new SubscriptionAlreadyExistsException(
                        "Organization is already subscribed to this plan price");
            }

            current.setStatus("CANCELLED");
            current.setCancelledAt(OffsetDateTime.now());
            current.setAutoRenew(false);
            // Flushed now so the old subscription leaves the "live" unique
            // index before a free replacement is activated below.
            subscriptionRepository.saveAndFlush(current);
            paymentService.voidOpenInvoices(current, "Subscription replaced by another plan");
        }

        Subscription newSubscription = Subscription.builder()
                .organization(organization)
                .planPrice(newPlanPrice)
                .build();
        newSubscription = subscriptionRepository.save(newSubscription);
        startSubscription(newSubscription);

        return toSubscriptionResponseDTO(newSubscription);
    }

    // ---- Renewal (system action) ----

    /**
     * Renews one subscription whose period ended. Called only by
     * SubscriptionRenewalJob: a system action, so no user and no
     * membership check, and it is not exposed through any controller.
     *
     * Outcomes:
     *  - plan no longer ACTIVE -> subscription becomes EXPIRED (the
     *    customer must pick another plan); never throws in that case,
     *    otherwise the daily job would retry and fail forever;
     *  - charge succeeded -> next period, stays ACTIVE;
     *  - charge failed -> PAST_DUE, period untouched.
     *
     * TODO(gateway): only ACTIVE subscriptions are renewed and PAST_DUE
     * ones are never retried. A retry/dunning flow arrives with the real
     * gateway, since nothing can fail before that.
     */
    @Transactional
    public SubscriptionResponseDTO renewSubscription(UUID subscriptionPublicUuid) {
        Subscription subscription = subscriptionRepository.findByPublicUuid(subscriptionPublicUuid)
                .orElseThrow(() -> new SubscriptionNotFoundException(
                        "Subscription not found: " + subscriptionPublicUuid));

        if (!"ACTIVE".equals(subscription.getStatus())) {
            throw new IllegalArgumentException("Only ACTIVE subscriptions can be renewed");
        }

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime previousEnd = subscription.getCurrentPeriodEnd();

        // Guards against a double renewal (job re-run, overlapping runs).
        if (previousEnd != null && previousEnd.isAfter(now)) {
            throw new IllegalArgumentException("Subscription is not due for renewal yet");
        }

        PlanPrice planPrice = subscription.getPlanPrice();

        if (!isPlanAvailable(planPrice)) {
            subscription.setStatus("EXPIRED");
            subscription.setAutoRenew(false);
            return toSubscriptionResponseDTO(subscriptionRepository.save(subscription));
        }

        // Free plans have nothing to invoice: they only move to the next period.
        if (planPrice.getAmount().signum() > 0) {
            PaymentService.RenewalChargeResult result = paymentService.chargeForRenewal(subscription);

            if (!result.success()) {
                subscription.setStatus("PAST_DUE");
                return toSubscriptionResponseDTO(subscriptionRepository.save(subscription));
            }
        }

        // The new period starts where the previous one ended (not at
        // "now"), so a late job run does not shift the billing anchor.
        // Known wrinkle: after a short month (Jan 31 -> Feb 28) the day of
        // month stays at 28. Acceptable for now.
        OffsetDateTime newStart = previousEnd != null ? previousEnd : now;
        subscription.setCurrentPeriodStart(newStart);
        subscription.setCurrentPeriodEnd(addBillingCycle(newStart, planPrice.getBillingCycle()));
        return toSubscriptionResponseDTO(subscriptionRepository.save(subscription));
    }

    private OffsetDateTime addBillingCycle(OffsetDateTime from, String billingCycle) {
        return switch (billingCycle) {
            case "MONTHLY" -> from.plusMonths(1);
            case "YEARLY" -> from.plusYears(1);
            default -> throw new IllegalStateException("Unsupported billing cycle: " + billingCycle);
        };
    }

    // ---- Activation (reacts to payment events) ----

    /**
     * Runs synchronously inside the transaction that marked the invoice
     * paid: if this fails, the payment rolls back with it.
     *
     *  - PENDING  -> ACTIVE, first period starts now;
     *  - PAST_DUE -> ACTIVE, and the period that was overdue is now paid,
     *    so it advances one cycle (otherwise the renewal job would bill the
     *    same period again on its next run);
     *  - anything else is ignored: ACTIVE means a renewal invoice was paid
     *    (its period already advanced); CANCELLED/EXPIRED/SUSPENDED must
     *    never be reactivated by a payment.
     */
    @EventListener
    @Transactional
    public void onInvoicePaid(PaymentService.InvoicePaidEvent event) {
        Subscription subscription = subscriptionRepository.findByPublicUuid(event.subscriptionPublicUuid())
                .orElseThrow(() -> new SubscriptionNotFoundException(
                        "Subscription not found: " + event.subscriptionPublicUuid()));

        OffsetDateTime now = OffsetDateTime.now();

        switch (subscription.getStatus()) {
            case "PENDING" -> activate(subscription, now);
            case "PAST_DUE" -> {
                OffsetDateTime overdueEnd = subscription.getCurrentPeriodEnd();
                activate(subscription, overdueEnd != null ? overdueEnd : now);
            }
            default -> { }
        }
    }

    // Paid plans wait for the first invoice to be paid (onInvoicePaid
    // activates them). Free plans have nothing to pay, so they start now
    // and no invoice is issued (a 0 invoice could never be paid:
    // ck_payments_amount requires amount > 0).
    private void startSubscription(Subscription subscription) {
        if (subscription.getPlanPrice().getAmount().signum() == 0) {
            activate(subscription, OffsetDateTime.now());
        } else {
            paymentService.issueInitialInvoice(subscription);
        }
    }

    private void activate(Subscription subscription, OffsetDateTime periodStart) {
        subscription.setStatus("ACTIVE");
        subscription.setCurrentPeriodStart(periodStart);
        subscription.setCurrentPeriodEnd(
                addBillingCycle(periodStart, subscription.getPlanPrice().getBillingCycle()));
        subscriptionRepository.save(subscription);
    }

    // ---- Internal helpers ----

    private Plan findPlanOrThrow(UUID publicUuid) {
        return planRepository.findByPublicUuid(publicUuid)
                .orElseThrow(() -> new PlanNotFoundException("Plan not found: " + publicUuid));
    }

    // ARCHIVED is terminal (there is no un-archive), so an archived plan is
    // frozen: no edits, no new limits, no new prices. INACTIVE plans stay
    // editable so an admin can prepare them before reactivating.
    private void requirePlanNotArchived(Plan plan) {
        if ("ARCHIVED".equals(plan.getStatus())) {
            throw new PlanArchivedException("This plan is archived and can no longer be modified");
        }
    }
    private Organization findOrganizationOrThrow(UUID publicUuid) {
        return organizationRepository.findByPublicUuid(publicUuid)
                .orElseThrow(() -> new OrganizationNotFoundException("Organization not found: " + publicUuid));
    }

    private OrganizationMember requireMembership(Organization organization, Long currentUserId) {
        return organizationMemberRepository
                .findByOrganization_IdOrganizationAndUser_IdUser(organization.getIdOrganization(), currentUserId)
                .orElseThrow(() -> new PermissionDeniedException("User is not a member of this organization"));
    }

    private void requireOwner(Organization organization, Long currentUserId) {
        OrganizationMember membership = requireMembership(organization, currentUserId);
        if (!ROLE_OWNER.equals(membership.getOrganizationRole().getCode())) {
            throw new PermissionDeniedException("Only OWNER can manage billing for this organization");
        }
    }

        // Only ACTIVE plans accept new subscriptions or switches. Existing
    // subscriptions on a deactivated plan are left untouched.
    private void requirePlanAvailable(PlanPrice planPrice) {
        if (!isPlanAvailable(planPrice)) {
            throw new IllegalArgumentException("This plan is not available for new subscriptions");
        }
    }

    // Boolean form, also used by renewSubscription(), which must not throw
    // inside a batch job when a plan was deactivated after subscribing.
    private boolean isPlanAvailable(PlanPrice planPrice) {
        return "ACTIVE".equals(planPrice.getPlan().getStatus());
    }

    // Mirrors OrganizationService/ProjectService/ServiceService/
    // ApiKeyService.requireActiveOrganization. Blocks subscribe/cancel/
    // switch while the organization is INACTIVE or SUSPENDED —
    // getCurrentSubscription/listSubscriptionHistory (reads) are
    // intentionally NOT gated, same reasoning as elsewhere: a member
    // should still be able to see the subscription while the org is
    // inactive/suspended, just not change it.
    private void requireActiveOrganization(Organization organization) {
        String status = organization.getStatus();
        if ("INACTIVE".equals(status)) {
            throw new OrganizationInactiveException(
                    "This organization is inactive; no changes are allowed until it is reactivated");
        }
        if ("SUSPENDED".equals(status)) {
            throw new OrganizationInactiveException(
                    "This organization is suspended; no changes are allowed until the suspension is lifted");
        }
    }

    private PlanResponseDTO toPlanResponseDTO(Plan plan) {
        PlanResourceLimitResponseDTO limitsDTO = planResourceLimitRepository
                .findByPlan_IdPlan(plan.getIdPlan())
                .map(this::toResourceLimitResponseDTO)
                .orElse(null);

        List<PlanPriceResponseDTO> pricesDTO = planPriceRepository.findByPlan_IdPlan(plan.getIdPlan()).stream()
                .filter(price -> price.getEffectiveUntil() == null)
                .map(this::toPriceResponseDTO)
                .toList();

        return PlanResponseDTO.builder()
                .publicUuid(plan.getPublicUuid())
                .name(plan.getName())
                .slug(plan.getSlug())
                .description(plan.getDescription())
                .status(plan.getStatus())
                .resourceLimits(limitsDTO)
                .prices(pricesDTO)
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }

    private PlanResourceLimitResponseDTO toResourceLimitResponseDTO(PlanResourceLimit limits) {
        return PlanResourceLimitResponseDTO.builder()
                .cpuLimit(limits.getCpuLimit())
                .memoryLimitMb(limits.getMemoryLimitMb())
                .storageLimitMb(limits.getStorageLimitMb())
                .maxProjects(limits.getMaxProjects())
                .maxServices(limits.getMaxServices())
                .maxDomains(limits.getMaxDomains())
                .maxEnvironmentVariables(limits.getMaxEnvironmentVariables())
                .bandwidthLimitMb(limits.getBandwidthLimitMb())
                .build();
    }

    private PlanPriceResponseDTO toPriceResponseDTO(PlanPrice price) {
        return PlanPriceResponseDTO.builder()
                .publicUuid(price.getPublicUuid())
                .billingCycle(price.getBillingCycle())
                .amount(price.getAmount())
                .currency(price.getCurrency())
                .effectiveFrom(price.getEffectiveFrom())
                .build();
    }

    private SubscriptionResponseDTO toSubscriptionResponseDTO(Subscription subscription) {
        Plan plan = subscription.getPlanPrice().getPlan();
        return SubscriptionResponseDTO.builder()
                .publicUuid(subscription.getPublicUuid())
                .organizationPublicUuid(subscription.getOrganization().getPublicUuid())
                .planPublicUuid(plan.getPublicUuid())
                .planName(plan.getName())
                .billingCycle(subscription.getPlanPrice().getBillingCycle())
                .status(subscription.getStatus())
                .currentPeriodStart(subscription.getCurrentPeriodStart())
                .currentPeriodEnd(subscription.getCurrentPeriodEnd())
                .autoRenew(subscription.getAutoRenew())
                .cancelledAt(subscription.getCancelledAt())
                .createdAt(subscription.getCreatedAt())
                .updatedAt(subscription.getUpdatedAt())
                .build();
    }
}