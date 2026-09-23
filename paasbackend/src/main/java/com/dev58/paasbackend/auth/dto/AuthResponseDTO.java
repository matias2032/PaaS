package com.dev58.paasbackend.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDTO {

    private UUID publicUuid;

    private String firstName;

    private String lastName;

    private String email;

    private String status;

    private OffsetDateTime emailVerifiedAt;

    private OffsetDateTime createdAt;

    // Never client-settable (see CreateStaffUserRequestDTO/
    // UpdatePlatformRoleRequestDTO for the only two ways this value
    // changes) — always CUSTOMER for a self-registered user. Exposed
    // here so the frontend knows whether to render admin-only UI for
    // the currently authenticated user.
    private String platformRole;

    // Presente apenas nas respostas de login/registo (emissão de token)
    private String token;
}