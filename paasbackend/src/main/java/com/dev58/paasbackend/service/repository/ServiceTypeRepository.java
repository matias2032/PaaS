// com.dev58.paasbackend.service.repository.ServiceTypeRepository
package com.dev58.paasbackend.service.repository;

import com.dev58.paasbackend.service.entity.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServiceTypeRepository extends JpaRepository<ServiceType, Short> {

    Optional<ServiceType> findByCode(String code);
}