package com.dev58.paasbackend.organization.repository;

import com.dev58.paasbackend.organization.entity.OrganizationRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganizationRoleRepository extends JpaRepository<OrganizationRole, Short> {

    Optional<OrganizationRole> findByCode(String code);
}