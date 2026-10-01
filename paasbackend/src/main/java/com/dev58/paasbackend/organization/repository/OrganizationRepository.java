package com.dev58.paasbackend.organization.repository;

import com.dev58.paasbackend.organization.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Optional<Organization> findByPublicUuid(UUID publicUuid);

    boolean existsBySlug(String slug);

    // Admin search: case-insensitive "contains" on name OR slug. Spring Data
    // escapes % and _ in the term, so user input is never treated as a wildcard.
    Page<Organization> findByNameContainingIgnoreCaseOrSlugContainingIgnoreCase(
            String name, String slug, Pageable pageable);
}