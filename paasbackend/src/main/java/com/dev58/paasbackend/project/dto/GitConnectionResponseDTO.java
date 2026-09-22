package com.dev58.paasbackend.project.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class GitConnectionResponseDTO {

    private UUID publicUuid;
    private UUID organizationPublicUuid;
    private String gitProviderCode;
    private String externalAccountId;
    private String externalAccountName;
    // Whether an access token is stored — never the token itself.
    // Lets the UI show "API access: configured" without ever
    // exposing or re-encrypting anything client-side.
    private Boolean hasAccessToken;
    private String status;
    private OffsetDateTime tokenExpiresAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}