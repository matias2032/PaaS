package com.dev58.paasbackend.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// The amount is deliberately NOT part of the request: it is always the
// invoice total, computed server-side, so a client can never underpay.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubmitPaymentRequestDTO {

    @NotBlank
    @Size(max = 30)
    private String paymentMethodCode;

    @Size(max = 255)
    private String transactionReference;
}