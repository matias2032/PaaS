// com.dev58.paasbackend.service.repository.EnvironmentVariableRepository
package com.dev58.paasbackend.service.repository;

import com.dev58.paasbackend.service.entity.EnvironmentVariable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnvironmentVariableRepository extends JpaRepository<EnvironmentVariable, Long> {

    List<EnvironmentVariable> findByService_IdService(Long idService);

    Optional<EnvironmentVariable> findByService_IdServiceAndVariableKey(Long idService, String variableKey);

    boolean existsByService_IdServiceAndVariableKey(Long idService, String variableKey);
}