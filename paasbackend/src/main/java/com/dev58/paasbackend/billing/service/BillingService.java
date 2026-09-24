package com.dev58.paasbackend.billing.service;

import com.dev58.paasbackend.billing.dto.*;
import com.dev58.paasbackend.billing.entity.Plan;
import com.dev58.paasbackend.billing.entity.PlanPrice;
import com.dev58.paasbackend.billing.entity.PlanResourceLimit;
import com.dev58.paasbackend.billing.entity.Subscription;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
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

    private final PlanRepository planRepository;
    private final PlanResourceLimitRepository planResourceLimitRepository;
    private final PlanPriceRepository planPriceRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;

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
        plan.setStatus("INACTIVE");
        return toPlanResponseDTO(planRepository.save(plan));
    }

    @Transactional
    public PlanResponseDTO reactivatePlan(UUID publicUuid) {
        Plan plan = findPlanOrThrow(publicUuid);
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

        PlanResourceLimit limits = planResourceLimitRepository.findByPlan_IdPlan(plan.getIdPlan())
                .orElseGet(() -> PlanResourceLimit.builder().plan(plan).build());

        limits.setCpuLimit(request.getCpuLimit());
        limits.setMemoryLimitMb(request.getMemoryLimitMb());
        limits.setStorageLimitMb(request.getStorageLimitMb());
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
        Plan plan = findPlanOrThrow(planPublicUuid);
        OffsetDateTime now = OffsetDateTime.now();

        // Fecha o preço corrente desse ciclo, se existir — nunca se
        // atualiza um PlanPrice em vigor (ver nota na entidade).
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

        Subscription subscription = Subscription.builder()
                .organization(organization)
                .planPrice(planPrice)
                .build();
        subscription = subscriptionRepository.save(subscription);

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
            subscriptionRepository.save(current);
        }

        Subscription newSubscription = Subscription.builder()
                .organization(organization)
                .planPrice(newPlanPrice)
                .build();
        newSubscription = subscriptionRepository.save(newSubscription);

        return toSubscriptionResponseDTO(newSubscription);
    }

    // ---- Internal helpers ----

    private Plan findPlanOrThrow(UUID publicUuid) {
        return planRepository.findByPublicUuid(publicUuid)
                .orElseThrow(() -> new PlanNotFoundException("Plan not found: " + publicUuid));
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