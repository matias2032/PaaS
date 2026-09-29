package com.dev58.paasbackend.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponseDTO {

    private UUID publicUuid;
    private UUID invoicePublicUuid;
    private String paymentMethodCode;
    private BigDecimal amount;
    private String currency;
    private String transactionReference;

    // Gateway-side payment id. Always null while no real gateway is wired
    // in (M-Pesa / e-Mola / card / PayPal are not connected yet).
    // TODO(gateway): populated by the gateway integration once it exists;
    // the field is already here so the API contract does not change then.
    private String externalPaymentId;

    // PENDING | PROCESSING | PAID | FAILED | CANCELLED | REFUNDED
    private String status;

    // Only present when status = FAILED.
    private String failureReason;

    private OffsetDateTime paidAt;
    private OffsetDateTime createdAt;
}