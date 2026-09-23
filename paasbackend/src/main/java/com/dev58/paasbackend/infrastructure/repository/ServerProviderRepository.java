package com.dev58.paasbackend.infrastructure.repository;

import com.dev58.paasbackend.infrastructure.entity.ServerProvider;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServerProviderRepository extends JpaRepository<ServerProvider, Short> {

    boolean existsByCode(String code);
}