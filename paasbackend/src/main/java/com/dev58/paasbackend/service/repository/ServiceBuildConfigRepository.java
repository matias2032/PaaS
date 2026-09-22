// com.dev58.paasbackend.service.repository.ServiceBuildConfigRepository
package com.dev58.paasbackend.service.repository;

import com.dev58.paasbackend.service.entity.ServiceBuildConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServiceBuildConfigRepository extends JpaRepository<ServiceBuildConfig, Long> {

    Optional<ServiceBuildConfig> findByService_IdService(Long idService);
}