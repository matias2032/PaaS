package com.dev58.paasbackend.api_key.repository;

import com.dev58.paasbackend.api_key.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // Runtime validation: lookup by SHA-256 of the full raw key.
    Optional<ApiKey> findByKeyHash(String keyHash);

    // Bulk revoke used when the platform suspends an organization with
    // "revoke API keys" chosen. Only touches ACTIVE keys, so keys the
    // owner already revoked keep their own (empty) revocationReason.
    @Modifying(flushAutomatically = true)
    @Query("update ApiKey k set k.status = 'REVOKED', k.revocationReason = :reason "
            + "where k.organization.idOrganization = :orgId and k.status = 'ACTIVE'")
    int revokeAllActiveByOrganization(@Param("orgId") Long orgId, @Param("reason") String reason);
}