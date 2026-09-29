package com.dev58.paasbackend.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminMarkPaidRequestDTO {

    @NotBlank
    @Size(max = 30)
    private String paymentMethodCode;

    @Size(max = 255)
    private String transactionReference;

    // Optional note. Not persisted on the payment row (no column for it):
    // it goes to audit_log by the service.
    @Size(max = 500)
    private String reason;
}