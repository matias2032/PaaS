package com.dev58.paasbackend.project.repository;

import com.dev58.paasbackend.project.entity.GitConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GitConnectionRepository extends JpaRepository<GitConnection, Long> {

    Optional<GitConnection> findByPublicUuid(UUID publicUuid);

    List<GitConnection> findByOrganization_IdOrganization(Long idOrganization);

    Optional<GitConnection> findByOrganization_IdOrganizationAndGitProvider_IdGitProvider(
            Long idOrganization, Short idGitProvider);
}