// com.dev58.paasbackend.service.repository.ServiceRepositoryEntityRepository
package com.dev58.paasbackend.service.repository;

import com.dev58.paasbackend.service.entity.ServiceRepositoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServiceRepositoryEntityRepository extends JpaRepository<ServiceRepositoryEntity, Long> {

    Optional<ServiceRepositoryEntity> findByService_IdService(Long idService);
}