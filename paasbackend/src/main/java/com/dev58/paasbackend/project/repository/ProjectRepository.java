package com.dev58.paasbackend.project.repository;

import com.dev58.paasbackend.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByPublicUuid(UUID publicUuid);

    List<Project> findByOrganization_IdOrganization(Long idOrganization);

    boolean existsByOrganization_IdOrganizationAndSlug(Long idOrganization, String slug);
}