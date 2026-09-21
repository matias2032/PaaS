package com.dev58.paasbackend.project.repository;

import com.dev58.paasbackend.project.entity.GitProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GitProviderRepository extends JpaRepository<GitProvider, Short> {

    Optional<GitProvider> findByCode(String code);

    boolean existsByCode(String code);
}