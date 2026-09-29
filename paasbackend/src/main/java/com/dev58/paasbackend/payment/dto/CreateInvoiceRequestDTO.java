package com.dev58.paasbackend.payment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// TODO(ad-hoc invoices): DEAD CODE for now. Nothing consumes this DTO
// today: renewal invoices are built directly by PaymentService.chargeForRenewal
// from the subscription's PlanPrice, without a request body.
// It becomes live if/when platform staff need to issue invoices by hand,
// e.g. a one-off setup fee, a manual adjustment, or an invoice for a
// customer paying outside the renewal cycle. At that point: add an
// admin-only endpoint (@PreAuthorize PLATFORM_ADMIN) plus a
// PaymentService.createInvoiceAsAdmin(...) method that validates this
// DTO and reuses the private buildInvoice(...) helper.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateInvoiceRequestDTO {

    @NotNull
    private UUID subscriptionPublicUuid;

    @NotBlank
    @Size(min = 3, max = 3)
    private String currency;

    // Optional. When null the service applies its default due date.
    private OffsetDateTime dueAt;

    @NotEmpty
    @Valid
    private List<InvoiceItemRequestDTO> items;
}