// com.dev58.paasbackend.service.repository.DomainRepository
package com.dev58.paasbackend.service.repository;

import com.dev58.paasbackend.service.entity.Domain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DomainRepository extends JpaRepository<Domain, Long> {

    Optional<Domain> findByPublicUuid(UUID publicUuid);

    List<Domain> findByService_IdService(Long idService);

    Optional<Domain> findByService_IdServiceAndIsPrimaryTrue(Long idService);

    boolean existsByHostname(String hostname);
}