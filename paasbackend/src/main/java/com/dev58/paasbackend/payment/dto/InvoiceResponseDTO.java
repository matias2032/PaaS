package com.dev58.paasbackend.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceResponseDTO {

    private UUID publicUuid;
    private UUID subscriptionPublicUuid;
    private String invoiceNumber;
    private String currency;

    // DRAFT | PENDING | PAID | OVERDUE | CANCELLED | VOID
    private String status;

    // Sum of all items' lineTotal, computed by the service.
    private BigDecimal totalAmount;

    private OffsetDateTime issuedAt;
    private OffsetDateTime dueAt;
    private OffsetDateTime paidAt;
    private OffsetDateTime createdAt;

    private List<InvoiceItemResponseDTO> items;
    private List<PaymentResponseDTO> payments;
}