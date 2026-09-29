package com.dev58.paasbackend.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// TODO(ad-hoc invoices): currently dead code, only used by
// CreateInvoiceRequestDTO, which is itself unused for now. See the note there.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItemRequestDTO {

    @NotBlank
    @Size(max = 255)
    private String description;

    @NotNull
    @Min(1)
    private Integer quantity;

    @NotNull
    @PositiveOrZero
    private BigDecimal unitPrice;
}