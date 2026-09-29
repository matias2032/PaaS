package com.dev58.paasbackend.payment.service;

import com.dev58.paasbackend.audit_log.service.AuditLogService;
import com.dev58.paasbackend.billing.entity.PlanPrice;
import com.dev58.paasbackend.billing.entity.Subscription;
import com.dev58.paasbackend.organization.entity.Organization;
import com.dev58.paasbackend.organization.entity.OrganizationMember;
import com.dev58.paasbackend.organization.exception.OrganizationNotFoundException;
import com.dev58.paasbackend.organization.exception.PermissionDeniedException;
import com.dev58.paasbackend.organization.repository.OrganizationMemberRepository;
import com.dev58.paasbackend.organization.repository.OrganizationRepository;
import com.dev58.paasbackend.payment.dto.*;
import com.dev58.paasbackend.payment.entity.Invoice;
import com.dev58.paasbackend.payment.entity.InvoiceItem;
import com.dev58.paasbackend.payment.entity.Payment;
import com.dev58.paasbackend.payment.entity.PaymentMethod;
import com.dev58.paasbackend.payment.exception.InvoiceAlreadyPaidException;
import com.dev58.paasbackend.payment.exception.InvoiceNotFoundException;
import com.dev58.paasbackend.payment.exception.PaymentAlreadyInProgressException;
import com.dev58.paasbackend.payment.exception.PaymentAlreadyRefundedException;
import com.dev58.paasbackend.payment.exception.PaymentMethodNotFoundException;
import com.dev58.paasbackend.payment.exception.PaymentNotFoundException;
import com.dev58.paasbackend.payment.repository.InvoiceItemRepository;
import com.dev58.paasbackend.payment.repository.InvoiceRepository;
import com.dev58.paasbackend.payment.repository.PaymentMethodRepository;
import com.dev58.paasbackend.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Invoices and payments. Independent from BillingService by design:
 * payment reads Subscription/PlanPrice, BillingService calls
 * chargeForRenewal(), and payment never calls billing.
 *
 * Dual authorization, same pattern as ApiKeyService/OrganizationService:
 *  - client-facing methods use requireMembership/requireOwner here;
 *  - admin-facing methods (…AsAdmin) have NO membership check, their
 *    authorization is @PreAuthorize on PaymentController only.
 *
 * NO REAL GATEWAY YET. submitPayment only records a PENDING payment that
 * an admin later confirms (markInvoicePaidAsAdmin), and refunds only flip
 * a status. TODO(gateway): M-Pesa / e-Mola / card / PayPal integration
 * will plug in at submitPayment, chargeForRenewal and refundPaymentAsAdmin,
 * plus a public webhook endpoint (which cannot use the normal JWT auth).
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String ROLE_OWNER = "OWNER";

    private static final String INVOICE_PENDING = "PENDING";
    private static final String INVOICE_PAID = "PAID";
    private static final String INVOICE_VOID = "VOID";

    private static final String PAYMENT_PENDING = "PENDING";
    private static final String PAYMENT_PAID = "PAID";
    private static final String PAYMENT_REFUNDED = "REFUNDED";

    private static final String METHOD_ACTIVE = "ACTIVE";

    // An invoice can only receive a payment while it is still owed.
    private static final List<String> PAYABLE_INVOICE_STATUSES = List.of("PENDING", "OVERDUE");

    // A payment that was submitted but not yet settled or failed.
    private static final List<String> IN_FLIGHT_PAYMENT_STATUSES = List.of("PENDING", "PROCESSING");

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Outcome of a renewal charge, consumed by BillingService.renewSubscription().
     * failureReason is only set when success = false.
     */
    public record RenewalChargeResult(boolean success, UUID invoicePublicUuid, String failureReason) {

        public static RenewalChargeResult succeeded(UUID invoicePublicUuid) {
            return new RenewalChargeResult(true, invoicePublicUuid, null);
        }

        // TODO(gateway): unused while there is no gateway, because nothing
        // can fail today. A real gateway declining the charge will return this,
        // and renewSubscription() then marks the subscription PAST_DUE.
        public static RenewalChargeResult failed(UUID invoicePublicUuid, String failureReason) {
            return new RenewalChargeResult(false, invoicePublicUuid, failureReason);
        }
    }

    /**
     * Published when an invoice becomes PAID, so other modules (billing)
     * can react without payment knowing about them. Listeners run
     * synchronously inside the publisher's transaction (plain
     * @EventListener), so a failing listener rolls the payment back too.
     *
     * TODO(gateway): the gateway webhook will publish this as well.
     */
    public record InvoicePaidEvent(UUID subscriptionPublicUuid, UUID invoicePublicUuid) {
    }

    // ==================== Client-facing ====================

    @Transactional(readOnly = true)
    public List<InvoiceResponseDTO> listInvoices(UUID orgPublicUuid, Long currentUserId) {
        Organization organization = findOrganizationOrThrow(orgPublicUuid);
        requireMembership(organization, currentUserId);

        return invoiceRepository.findBySubscription_Organization_IdOrganization(organization.getIdOrganization())
                .stream()
                .sorted(Comparator.comparing(Invoice::getCreatedAt, Comparator.reverseOrder()))
                .map(this::toInvoiceResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public InvoiceResponseDTO getInvoice(UUID invoicePublicUuid, Long currentUserId) {
        Invoice invoice = findInvoiceOrThrow(invoicePublicUuid);
        requireMembership(invoice.getSubscription().getOrganization(), currentUserId);

        return toInvoiceResponseDTO(invoice);
    }

    // Deliberately NOT gated by requireActiveOrganization (unlike
    // BillingService.subscribe/cancel/switch): a SUSPENDED or INACTIVE
    // organization must still be able to settle what it owes, otherwise a
    // suspension caused by non-payment could never be resolved by paying.
    @Transactional
    public PaymentResponseDTO submitPayment(UUID invoicePublicUuid, SubmitPaymentRequestDTO request, Long currentUserId) {
        Invoice invoice = findInvoiceOrThrow(invoicePublicUuid);
        Organization organization = invoice.getSubscription().getOrganization();
        requireOwner(organization, currentUserId);
        requirePayable(invoice);

        boolean alreadyInFlight = paymentRepository.findByInvoice_IdInvoice(invoice.getIdInvoice()).stream()
                .anyMatch(p -> IN_FLIGHT_PAYMENT_STATUSES.contains(p.getStatus()));
        if (alreadyInFlight) {
            throw new PaymentAlreadyInProgressException(
                    "This invoice already has a payment awaiting confirmation");
        }

        PaymentMethod method = findActiveMethodOrThrow(request.getPaymentMethodCode());

        // The amount is never taken from the client: always the invoice total.
        BigDecimal total = computeTotal(invoiceItemRepository.findByInvoice_IdInvoice(invoice.getIdInvoice()));
        if (total.signum() <= 0) {
            throw new IllegalArgumentException("This invoice has nothing to pay");
        }

        // TODO(gateway): with a real gateway, call it here, store its id in
        // externalPaymentId and set PROCESSING; the webhook then settles it.
        // Today the payment stays PENDING until an admin confirms it.
        Payment payment = Payment.builder()
                .invoice(invoice)
                .paymentMethod(method)
                .amount(total)
                .currency(invoice.getCurrency())
                .transactionReference(request.getTransactionReference())
                .status(PAYMENT_PENDING)
                .build();
        payment = paymentRepository.save(payment);

        auditLogService.record(
                organization,
                "PAYMENT_SUBMITTED",
                "PAYMENT",
                payment.getPublicUuid().toString(),
                AuditLogService.meta(
                        "invoicePublicUuid", invoice.getPublicUuid().toString(),
                        "paymentMethodCode", method.getCode(),
                        "amount", total,
                        "currency", invoice.getCurrency()));

        return toPaymentResponseDTO(payment);
    }

    // Catalog, read-only. Only ACTIVE methods are offered.
    public List<PaymentMethodResponseDTO> listPaymentMethods() {
        return paymentMethodRepository.findAll().stream()
                .filter(m -> METHOD_ACTIVE.equals(m.getStatus()))
                .map(this::toPaymentMethodResponseDTO)
                .toList();
    }

    // ==================== Admin-facing (platform-side) ====================
    // Authorization is @PreAuthorize on the controller; no membership check.

    @Transactional(readOnly = true)
    public List<InvoiceResponseDTO> listInvoicesByOrganizationAsAdmin(UUID orgPublicUuid) {
        Organization organization = findOrganizationOrThrow(orgPublicUuid);

        return invoiceRepository.findBySubscription_Organization_IdOrganization(organization.getIdOrganization())
                .stream()
                .sorted(Comparator.comparing(Invoice::getCreatedAt, Comparator.reverseOrder()))
                .map(this::toInvoiceResponseDTO)
                .toList();
    }

    // Manual confirmation (e.g. bank transfer received). If the customer
    // already submitted a payment for this invoice, that payment is
    // settled instead of creating a second one, so the invoice never ends
    // up PAID with a dangling PENDING payment next to it.
    @Transactional
    public InvoiceResponseDTO markInvoicePaidAsAdmin(UUID invoicePublicUuid, AdminMarkPaidRequestDTO request) {
        Invoice invoice = findInvoiceOrThrow(invoicePublicUuid);
        requirePayable(invoice);

        PaymentMethod method = findActiveMethodOrThrow(request.getPaymentMethodCode());
        BigDecimal total = computeTotal(invoiceItemRepository.findByInvoice_IdInvoice(invoice.getIdInvoice()));
        OffsetDateTime now = OffsetDateTime.now();

        Optional<Payment> inFlight = paymentRepository.findByInvoice_IdInvoice(invoice.getIdInvoice()).stream()
                .filter(p -> IN_FLIGHT_PAYMENT_STATUSES.contains(p.getStatus()))
                .findFirst();

        Payment payment = inFlight.orElseGet(() -> Payment.builder()
                .invoice(invoice)
                .amount(total)
                .currency(invoice.getCurrency())
                .build());

        payment.setPaymentMethod(method);
        payment.setStatus(PAYMENT_PAID);
        payment.setPaidAt(now);
        if (request.getTransactionReference() != null && !request.getTransactionReference().isBlank()) {
            payment.setTransactionReference(request.getTransactionReference());
        }
        paymentRepository.save(payment);

        invoice.setStatus(INVOICE_PAID);
        invoice.setPaidAt(now);
        // No reassignment: `invoice` is captured by the lambda above, so it
        // must stay effectively final. save() on a managed entity returns
        // the same instance anyway.
        invoiceRepository.save(invoice);

        // NOTE: this does not touch the subscription. If a PAST_DUE
        // subscription should return to ACTIVE after a manual payment, that
        // belongs to BillingService (payment never calls billing). To decide in Phase 2.
        auditLogService.record(
                invoice.getSubscription().getOrganization(),
                "INVOICE_MARKED_PAID_BY_ADMIN",
                "INVOICE",
                invoice.getPublicUuid().toString(),
                AuditLogService.meta(
                        "paymentPublicUuid", payment.getPublicUuid().toString(),
                        "paymentMethodCode", method.getCode(),
                        "amount", payment.getAmount(),
                        "currency", invoice.getCurrency(),
                        "reason", request.getReason()));

        // Synchronous listeners (billing activates the subscription) run in
        // this same transaction: if one fails, the payment rolls back too.
        eventPublisher.publishEvent(new InvoicePaidEvent(
                invoice.getSubscription().getPublicUuid(), invoice.getPublicUuid()));

        return toInvoiceResponseDTO(invoice);
    }

    // Status change only: no money moves back today.
    // TODO(gateway): a real gateway refund call goes here, before the
    // status change. The invoice status is intentionally left as it is
    // (refunds are always full, since payments are always the invoice total).
    @Transactional
    public PaymentResponseDTO refundPaymentAsAdmin(UUID paymentPublicUuid, AdminRefundRequestDTO request) {
        Payment payment = paymentRepository.findByPublicUuid(paymentPublicUuid)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentPublicUuid));

        if (PAYMENT_REFUNDED.equals(payment.getStatus())) {
            throw new PaymentAlreadyRefundedException("Payment was already refunded");
        }
        if (!PAYMENT_PAID.equals(payment.getStatus())) {
            throw new IllegalArgumentException("Only PAID payments can be refunded");
        }

        payment.setStatus(PAYMENT_REFUNDED);
        payment = paymentRepository.save(payment);

        auditLogService.record(
                payment.getInvoice().getSubscription().getOrganization(),
                "PAYMENT_REFUNDED_BY_ADMIN",
                "PAYMENT",
                payment.getPublicUuid().toString(),
                AuditLogService.meta(
                        "invoicePublicUuid", payment.getInvoice().getPublicUuid().toString(),
                        "amount", payment.getAmount(),
                        "currency", payment.getCurrency(),
                        "reason", request.getReason()));

        return toPaymentResponseDTO(payment);
    }

    // ==================== Internal (called only by BillingService) ====================

    /**
     * Invoices one renewal of the subscription at ITS OWN PlanPrice
     * (grandfathered: never the plan's current price).
     *
     * Joins the caller's transaction: if renewSubscription() rolls back,
     * the invoice disappears with it.
     *
     * GATEWAY BOUNDARY. TODO(gateway): this is where a real gateway would
     * charge the customer's stored payment method and return
     * RenewalChargeResult.failed(...) on decline. There is no gateway and
     * no stored payment method, so today this always reports success and
     * the invoice is left PENDING, i.e. billed but not collected. It is
     * settled later through submitPayment + markInvoicePaidAsAdmin.
     * No PAID payment is fabricated for money that never moved.
     */
    @Transactional
    public RenewalChargeResult chargeForRenewal(Subscription subscription) {
        PlanPrice price = subscription.getPlanPrice();
        OffsetDateTime now = OffsetDateTime.now();

        String description = "%s plan (%s) subscription renewal"
                .formatted(price.getPlan().getName(), price.getBillingCycle());

Invoice invoice = buildInvoice(
        subscription,
        price.getCurrency(),
        now,
        now,
        List.of(InvoiceItem.builder()
                        .description(description)
                        .quantity(1)
                        .unitPrice(price.getAmount())
                        .build()));

        return RenewalChargeResult.succeeded(invoice.getPublicUuid());
    }

    /**
     * Issues the FIRST invoice of a subscription, at its own PlanPrice.
     * Called by BillingService.subscribe/switchSubscription; joins the
     * caller's transaction, so if the subscription rolls back the invoice
     * disappears with it. Paying it (markInvoicePaidAsAdmin) publishes
     * InvoicePaidEvent, which activates the subscription.
     */
    @Transactional
    public UUID issueInitialInvoice(Subscription subscription) {
        PlanPrice price = subscription.getPlanPrice();
        OffsetDateTime now = OffsetDateTime.now();

        String description = "%s plan (%s) subscription"
                .formatted(price.getPlan().getName(), price.getBillingCycle());

        Invoice invoice = buildInvoice(
                subscription,
                price.getCurrency(),
                now,
                now,
                List.of(InvoiceItem.builder()
                        .description(description)
                        .quantity(1)
                        .unitPrice(price.getAmount())
                        .build()));

        return invoice.getPublicUuid();
    }

    /**
     * Voids the unpaid invoices of a subscription that was cancelled or
     * replaced, so they can no longer be paid. Called by BillingService and
     * joins its transaction (audit rows roll back together with it).
     *
     * Invoices with a payment already in flight are left untouched: the
     * customer may have transferred the money, so an admin must decide
     * (mark-paid, then refund if needed) instead of losing that trail.
     */
    @Transactional
    public void voidOpenInvoices(Subscription subscription, String reason) {
        Long organizationId = subscription.getOrganization().getIdOrganization();

        List<Invoice> openInvoices = invoiceRepository
                .findBySubscription_Organization_IdOrganization(organizationId)
                .stream()
                .filter(i -> i.getSubscription().getIdSubscription().equals(subscription.getIdSubscription()))
                .filter(i -> PAYABLE_INVOICE_STATUSES.contains(i.getStatus()))
                .toList();

        for (Invoice invoice : openInvoices) {
            boolean paymentInFlight = paymentRepository.findByInvoice_IdInvoice(invoice.getIdInvoice()).stream()
                    .anyMatch(p -> IN_FLIGHT_PAYMENT_STATUSES.contains(p.getStatus()));
            if (paymentInFlight) {
                continue;
            }

            invoice.setStatus(INVOICE_VOID);
            invoiceRepository.save(invoice);

            auditLogService.record(
                    subscription.getOrganization(),
                    "INVOICE_VOIDED",
                    "INVOICE",
                    invoice.getPublicUuid().toString(),
                    AuditLogService.meta(
                            "subscriptionPublicUuid", subscription.getPublicUuid().toString(),
                            "reason", reason));
        }
    }

    // ==================== Internal helpers ====================

    // Persists an invoice with its items. Used by chargeForRenewal today.
    // TODO(ad-hoc invoices): also meant for a future
    // createInvoiceAsAdmin(CreateInvoiceRequestDTO), see that DTO.
private Invoice buildInvoice(Subscription subscription, String currency,
                             OffsetDateTime issuedAt, OffsetDateTime dueAt, List<InvoiceItem> items) {
    Invoice invoice = Invoice.builder()
            .subscription(subscription)
            .invoiceNumber(generateInvoiceNumber())
            .currency(currency)
            .status(INVOICE_PENDING)
            .issuedAt(issuedAt)
            .dueAt(dueAt)
            .build();
        invoice = invoiceRepository.save(invoice);

        for (InvoiceItem item : items) {
            item.setInvoice(invoice);
            invoiceItemRepository.save(item);
        }
        return invoice;
    }

    // INV-{year}-{6-digit global sequence}. The sequence comes from the DB
    // so concurrent creations never collide; it can skip numbers on rollback.
    private String generateInvoiceNumber() {
        long next = invoiceRepository.nextInvoiceSequenceValue();
        return "INV-%d-%06d".formatted(OffsetDateTime.now().getYear(), next);
    }

    private Invoice findInvoiceOrThrow(UUID publicUuid) {
        return invoiceRepository.findByPublicUuid(publicUuid)
                .orElseThrow(() -> new InvoiceNotFoundException("Invoice not found: " + publicUuid));
    }

    private Organization findOrganizationOrThrow(UUID publicUuid) {
        return organizationRepository.findByPublicUuid(publicUuid)
                .orElseThrow(() -> new OrganizationNotFoundException("Organization not found: " + publicUuid));
    }

    private PaymentMethod findActiveMethodOrThrow(String code) {
        PaymentMethod method = paymentMethodRepository.findByCode(code)
                .orElseThrow(() -> new PaymentMethodNotFoundException("Payment method not found: " + code));
        if (!METHOD_ACTIVE.equals(method.getStatus())) {
            throw new IllegalArgumentException("This payment method is not available");
        }
        return method;
    }

    private void requirePayable(Invoice invoice) {
        if (INVOICE_PAID.equals(invoice.getStatus())) {
            throw new InvoiceAlreadyPaidException("Invoice is already paid");
        }
        if (!PAYABLE_INVOICE_STATUSES.contains(invoice.getStatus())) {
            throw new IllegalArgumentException(
                    "An invoice in status " + invoice.getStatus() + " cannot be paid");
        }
    }

    private OrganizationMember requireMembership(Organization organization, Long currentUserId) {
        return organizationMemberRepository
                .findByOrganization_IdOrganizationAndUser_IdUser(organization.getIdOrganization(), currentUserId)
                .orElseThrow(() -> new PermissionDeniedException("User is not a member of this organization"));
    }

    private void requireOwner(Organization organization, Long currentUserId) {
        OrganizationMember membership = requireMembership(organization, currentUserId);
        if (!ROLE_OWNER.equals(membership.getOrganizationRole().getCode())) {
            throw new PermissionDeniedException("Only OWNER can pay invoices for this organization");
        }
    }

    private BigDecimal computeTotal(List<InvoiceItem> items) {
        return items.stream()
                .map(this::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal lineTotal(InvoiceItem item) {
        return item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
    }

    private InvoiceResponseDTO toInvoiceResponseDTO(Invoice invoice) {
        List<InvoiceItem> items = invoiceItemRepository.findByInvoice_IdInvoice(invoice.getIdInvoice());

        List<InvoiceItemResponseDTO> itemsDTO = items.stream()
                .map(item -> InvoiceItemResponseDTO.builder()
                        .description(item.getDescription())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .lineTotal(lineTotal(item))
                        .build())
                .toList();

        List<PaymentResponseDTO> paymentsDTO = paymentRepository.findByInvoice_IdInvoice(invoice.getIdInvoice())
                .stream()
                .sorted(Comparator.comparing(Payment::getCreatedAt, Comparator.reverseOrder()))
                .map(this::toPaymentResponseDTO)
                .toList();

        return InvoiceResponseDTO.builder()
                .publicUuid(invoice.getPublicUuid())
                .subscriptionPublicUuid(invoice.getSubscription().getPublicUuid())
                .invoiceNumber(invoice.getInvoiceNumber())
                .currency(invoice.getCurrency())
                .status(invoice.getStatus())
                .totalAmount(computeTotal(items))
                .issuedAt(invoice.getIssuedAt())
                .dueAt(invoice.getDueAt())
                .paidAt(invoice.getPaidAt())
                .createdAt(invoice.getCreatedAt())
                .items(itemsDTO)
                .payments(paymentsDTO)
                .build();
    }

    private PaymentResponseDTO toPaymentResponseDTO(Payment payment) {
        return PaymentResponseDTO.builder()
                .publicUuid(payment.getPublicUuid())
                .invoicePublicUuid(payment.getInvoice().getPublicUuid())
                .paymentMethodCode(payment.getPaymentMethod().getCode())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .transactionReference(payment.getTransactionReference())
                .externalPaymentId(payment.getExternalPaymentId())
                .status(payment.getStatus())
                .failureReason(payment.getFailureReason())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }

    private PaymentMethodResponseDTO toPaymentMethodResponseDTO(PaymentMethod method) {
        return PaymentMethodResponseDTO.builder()
                .code(method.getCode())
                .name(method.getName())
                .status(method.getStatus())
                .build();
    }
}