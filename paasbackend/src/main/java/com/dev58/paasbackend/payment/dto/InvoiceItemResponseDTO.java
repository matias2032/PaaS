package com.dev58.paasbackend.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItemResponseDTO {

    private String description;
    private Integer quantity;
    private BigDecimal unitPrice;

    // quantity * unitPrice, computed by the service so clients never
    // have to redo the arithmetic.
    private BigDecimal lineTotal;
}