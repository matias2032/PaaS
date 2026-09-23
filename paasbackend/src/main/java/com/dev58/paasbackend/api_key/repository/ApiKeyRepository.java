package com.dev58.paasbackend.api_key.repository;

import com.dev58.paasbackend.api_key.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findByPublicUuid(UUID publicUuid);

    List<ApiKey> findByOrganization_IdOrganization(Long idOrganization);

    // Usado pelo revoke client-facing — confirma pertença à organização
    // do chamador numa só query, em vez de findByPublicUuid + comparação
    // manual de organization.getIdOrganization() em memória.
    Optional<ApiKey> findByOrganization_IdOrganizationAndPublicUuid(Long idOrganization, UUID publicUuid);

    boolean existsByOrganization_IdOrganizationAndName(Long idOrganization, String name);
}