package com.dev58.paasbackend.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Catalog table — no public_uuid column on server_providers (same as
// GitProvider/ServiceType), so idServerProvider itself is exposed
// here. No CreateServerProviderRequestDTO: this table is populated
// manually (see comment on the entity), not through the API — only
// read, e.g. to populate a dropdown when creating/updating a Server.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServerProviderResponseDTO {

    private Short idServerProvider;

    private String name;

    private String code;
}