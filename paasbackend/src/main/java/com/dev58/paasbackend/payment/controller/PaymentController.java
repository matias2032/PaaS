package com.dev58.paasbackend.payment.controller;

import com.dev58.paasbackend.common.security.AuthenticatedUser;
import com.dev58.paasbackend.payment.dto.*;
import com.dev58.paasbackend.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Client-facing routes authorize inside PaymentService (membership/owner).
 * Admin routes authorize ONLY here via @PreAuthorize; nobody else calls
 * the …AsAdmin service methods, so this single barrier covers the whole
 * call path (same reasoning as BillingController/InfrastructureController).
 *
 * TODO(gateway): a public webhook endpoint for gateway callbacks
 * (M-Pesa / e-Mola / card) will live here or in a dedicated controller.
 * It cannot use the normal JWT auth and must verify the gateway's own
 * signature instead. Not built: there is no gateway yet.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // ==================== Client-facing ====================

    @GetMapping("/organizations/{orgPublicUuid}/invoices")
    public ResponseEntity<List<InvoiceResponseDTO>> listInvoices(
            @PathVariable UUID orgPublicUuid,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(paymentService.listInvoices(orgPublicUuid, currentUser.getIdUser()));
    }

    @GetMapping("/invoices/{publicUuid}")
    public ResponseEntity<InvoiceResponseDTO> getInvoice(
            @PathVariable UUID publicUuid,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(paymentService.getInvoice(publicUuid, currentUser.getIdUser()));
    }

    @PostMapping("/invoices/{publicUuid}/payments")
    public ResponseEntity<PaymentResponseDTO> submitPayment(
            @PathVariable UUID publicUuid,
            @Valid @RequestBody SubmitPaymentRequestDTO request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        PaymentResponseDTO created = paymentService.submitPayment(publicUuid, request, currentUser.getIdUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Catalog, same level as GET /api/plans: any authenticated user.
    @GetMapping("/payment-methods")
    public ResponseEntity<List<PaymentMethodResponseDTO>> listPaymentMethods() {
        return ResponseEntity.ok(paymentService.listPaymentMethods());
    }

    // ==================== Admin-facing (platform-side) ====================

    @GetMapping("/admin/organizations/{orgPublicUuid}/invoices")
    @PreAuthorize("hasRole('SUPPORT')")
    public ResponseEntity<List<InvoiceResponseDTO>> listInvoicesByOrganizationAsAdmin(
            @PathVariable UUID orgPublicUuid) {
        return ResponseEntity.ok(paymentService.listInvoicesByOrganizationAsAdmin(orgPublicUuid));
    }

    @PatchMapping("/admin/invoices/{publicUuid}/mark-paid")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<InvoiceResponseDTO> markInvoicePaidAsAdmin(
            @PathVariable UUID publicUuid,
            @Valid @RequestBody AdminMarkPaidRequestDTO request) {
        return ResponseEntity.ok(paymentService.markInvoicePaidAsAdmin(publicUuid, request));
    }

    @PatchMapping("/admin/payments/{publicUuid}/refund")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<PaymentResponseDTO> refundPaymentAsAdmin(
            @PathVariable UUID publicUuid,
            @Valid @RequestBody AdminRefundRequestDTO request) {
        return ResponseEntity.ok(paymentService.refundPaymentAsAdmin(publicUuid, request));
    }
}