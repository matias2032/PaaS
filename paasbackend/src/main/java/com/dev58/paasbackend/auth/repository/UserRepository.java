package com.dev58.paasbackend.auth.repository;

import com.dev58.paasbackend.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPublicUuid(UUID publicUuid);

    boolean existsByEmail(String email);

    // Usado pela guarda do último PLATFORM_OWNER em
    // AuthService.updatePlatformRole() — precisa de saber quantos
    // owners existem antes de aceitar uma demoção.
    long countByPlatformRole(String platformRole);
}