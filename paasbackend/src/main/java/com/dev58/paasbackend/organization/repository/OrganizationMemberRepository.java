package com.dev58.paasbackend.organization.repository;

import com.dev58.paasbackend.organization.entity.Organization;
import com.dev58.paasbackend.organization.entity.OrganizationMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, Long> {

    // Members of one organization (used by "list members")
    List<OrganizationMember> findByOrganization_IdOrganization(Long idOrganization);

    // Organizations a given user belongs to (used by "list user's organizations")
    List<OrganizationMember> findByUser_IdUser(Long idUser);

    // Used for permission checks and for add-member (avoid duplicates)
    Optional<OrganizationMember> findByOrganization_IdOrganizationAndUser_IdUser(Long idOrganization, Long idUser);

    boolean existsByOrganization_IdOrganizationAndUser_IdUser(Long idOrganization, Long idUser);

    // Used to populate OrganizationResponseDTO.memberCount — a plain
    // COUNT query, not a full row fetch.
    long countByOrganization_IdOrganization(Long idOrganization);
}