// com.dev58.paasbackend.service.repository.DeploymentRepository
package com.dev58.paasbackend.service.repository;

import com.dev58.paasbackend.service.entity.Deployment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeploymentRepository extends JpaRepository<Deployment, Long> {

    Optional<Deployment> findByPublicUuid(UUID publicUuid);

    List<Deployment> findByService_IdServiceOrderByCreatedAtDesc(Long idService);
}