package com.dev58.paasbackend.infrastructure.repository;

import com.dev58.paasbackend.infrastructure.entity.CoolifyInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CoolifyInstanceRepository extends JpaRepository<CoolifyInstance, Long> {

    Optional<CoolifyInstance> findByPublicUuid(UUID publicUuid);

    boolean existsByName(String name);

    boolean existsByBaseUrl(String baseUrl);
}