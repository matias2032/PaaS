// com.dev58.paasbackend.service.repository.ServiceResourceConfigRepository
package com.dev58.paasbackend.service.repository;

import com.dev58.paasbackend.service.entity.ServiceResourceConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServiceResourceConfigRepository extends JpaRepository<ServiceResourceConfig, Long> {

    Optional<ServiceResourceConfig> findByService_IdService(Long idService);
}