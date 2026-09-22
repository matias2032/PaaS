// com.dev58.paasbackend.service.repository.ServiceRepository
package com.dev58.paasbackend.service.repository;

import com.dev58.paasbackend.service.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceRepository extends JpaRepository<Service, Long> {

    Optional<Service> findByPublicUuid(UUID publicUuid);

    List<Service> findByProject_IdProject(Long idProject);

    boolean existsByProject_IdProjectAndSlug(Long idProject, String slug);
}