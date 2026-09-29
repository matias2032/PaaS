package com.dev58.paasbackend.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Same pattern as AdminRevokeApiKeyRequestDTO: reason is mandatory.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminRefundRequestDTO {

    @NotBlank
    @Size(max = 500)
    private String reason;
}