package com.dev58.paasbackend.organization.repository;

import com.dev58.paasbackend.organization.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Optional<Organization> findByPublicUuid(UUID publicUuid);

    boolean existsBySlug(String slug);
}