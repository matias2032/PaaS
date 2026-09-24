package com.dev58.paasbackend.organization.service;

import com.dev58.paasbackend.auth.entity.User;
import com.dev58.paasbackend.auth.exception.UserNotFoundException;
import com.dev58.paasbackend.auth.repository.UserRepository;
import com.dev58.paasbackend.organization.dto.*;
import com.dev58.paasbackend.organization.entity.Organization;
import com.dev58.paasbackend.organization.entity.OrganizationMember;
import com.dev58.paasbackend.organization.entity.OrganizationRole;
import com.dev58.paasbackend.organization.exception.OrganizationAlreadySuspendedException;
import com.dev58.paasbackend.organization.exception.OrganizationInactiveException;
import com.dev58.paasbackend.organization.exception.OrganizationMemberNotFoundException;
import com.dev58.paasbackend.organization.exception.OrganizationNotFoundException;
import com.dev58.paasbackend.organization.exception.PermissionDeniedException;
import com.dev58.paasbackend.organization.exception.OrganizationMemberAlreadyExistsException;
import com.dev58.paasbackend.organization.exception.OrganizationSlugAlreadyExistsException;
import com.dev58.paasbackend.organization.repository.OrganizationMemberRepository;
import com.dev58.paasbackend.organization.repository.OrganizationRepository;
import com.dev58.paasbackend.organization.repository.OrganizationRoleRepository;
import com.dev58.paasbackend.organization.exception.OrganizationNotSuspendedException;
import com.dev58.paasbackend.organization.exception.OrganizationAlreadySuspendedException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private static final String ROLE_OWNER = "OWNER";
    private static final String ROLE_ADMIN = "ADMIN";

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final OrganizationRoleRepository organizationRoleRepository;
    private final UserRepository userRepository;

    // ---- Create ----

    @Transactional
    public OrganizationResponseDTO createOrganization(OrganizationRequestDTO request, Long currentUserId) {
        if (organizationRepository.existsBySlug(request.getSlug())) {
            throw new OrganizationSlugAlreadyExistsException("Slug already in use: " + request.getSlug());
        }

        Organization organization = Organization.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .build();
        organization = organizationRepository.save(organization);

        User creator = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + currentUserId));

        OrganizationRole ownerRole = organizationRoleRepository.findByCode(ROLE_OWNER)
                .orElseThrow(() -> new IllegalStateException("Role OWNER not seeded in organization_roles"));

        OrganizationMember ownerMembership = OrganizationMember.builder()
                .organization(organization)
                .user(creator)
                .organizationRole(ownerRole)
                .build();
        organizationMemberRepository.save(ownerMembership);

        return toResponseDTO(organization);
    }

    // ---- Read ----

    public OrganizationResponseDTO getOrganization(UUID publicUuid, Long currentUserId) {
        Organization organization = findOrganizationOrThrow(publicUuid);
        requireMembership(organization, currentUserId);
        return toResponseDTO(organization);
    }

    public List<OrganizationResponseDTO> listOrganizationsForCurrentUser(Long currentUserId) {
        return organizationMemberRepository.findByUser_IdUser(currentUserId).stream()
                .map(member -> toResponseDTO(member.getOrganization()))
                .toList();
    }

    // Platform-side — sem requireMembership, autorização é
    // @PreAuthorize("hasRole('SUPPORT')") no controller. Ao contrário de
    // listOrganizationsForCurrentUser(), devolve TODAS as organizações.
    public Page<OrganizationResponseDTO> listAllOrganizations(Pageable pageable) {
        return organizationRepository.findAll(pageable)
                .map(this::toResponseDTO);
    }

    // Variante de getOrganization() sem requireMembership — permite ver
    // o detalhe de uma organização da qual não se é membro (necessário
    // antes de a suspender).
    public OrganizationResponseDTO getOrganizationAsAdmin(UUID publicUuid) {
        Organization organization = findOrganizationOrThrow(publicUuid);
        return toResponseDTO(organization);
    }

    public List<OrganizationRoleResponseDTO> listRoles() {
        return organizationRoleRepository.findAll().stream()
                .map(role -> OrganizationRoleResponseDTO.builder()
                        .idOrganizationRole(role.getIdOrganizationRole())
                        .code(role.getCode())
                        .name(role.getName())
                        .build())
                .toList();
    }

    // ---- Update ----

    @Transactional
    public OrganizationResponseDTO updateOrganization(UUID publicUuid, OrganizationRequestDTO request, Long currentUserId) {
        Organization organization = findOrganizationOrThrow(publicUuid);
        requireOwnerOrAdmin(organization, currentUserId);
        requireActiveOrganization(organization);

        organization.setName(request.getName());
        // Slug intentionally not updated here — treat as immutable after
        // creation unless a dedicated flow is decided later (affects
        // URLs/references elsewhere in the platform).
        organization = organizationRepository.save(organization);

        return toResponseDTO(organization);
    }

    // ---- Members ----

    @Transactional
    public OrganizationMemberResponseDTO addMember(UUID orgPublicUuid, OrganizationMemberRequestDTO request, Long currentUserId) {
        Organization organization = findOrganizationOrThrow(orgPublicUuid);
        requireOwner(organization, currentUserId);
        requireActiveOrganization(organization);

        if (request.getUserEmail() == null || request.getUserEmail().isBlank()) {
            // userEmail is no longer @NotBlank at the DTO level (shared with
            // role-change, which doesn't need it) — enforce it here instead.
            throw new IllegalArgumentException("userEmail is required to add a member");
        }

        User userToAdd = userRepository.findByEmail(request.getUserEmail())
                .orElseThrow(() -> new UserNotFoundException("No user registered with email: " + request.getUserEmail()));

        if (organizationMemberRepository.existsByOrganization_IdOrganizationAndUser_IdUser(
                organization.getIdOrganization(), userToAdd.getIdUser())) {
            throw new OrganizationMemberAlreadyExistsException("User is already a member of this organization");
        }

        OrganizationRole role = organizationRoleRepository.findByCode(request.getRoleCode())
                .orElseThrow(() -> new IllegalArgumentException("Unknown role code: " + request.getRoleCode()));

        OrganizationMember member = OrganizationMember.builder()
                .organization(organization)
                .user(userToAdd)
                .organizationRole(role)
                .build();
        member = organizationMemberRepository.save(member);

        return toMemberResponseDTO(member);
    }


        @Transactional
    public OrganizationResponseDTO deactivateOrganization(UUID publicUuid, Long currentUserId) {
        Organization organization = findOrganizationOrThrow(publicUuid);
        requireOwner(organization, currentUserId);

        if ("INACTIVE".equals(organization.getStatus())) {
            throw new IllegalArgumentException("Organization is already inactive");
        }

        // Soft-delete. Reactivation is exposed via reactivateOrganization().
        organization.setStatus("INACTIVE");
        organization = organizationRepository.save(organization);

        return toResponseDTO(organization);
    }

    @Transactional
    public OrganizationResponseDTO reactivateOrganization(UUID publicUuid, Long currentUserId) {
        Organization organization = findOrganizationOrThrow(publicUuid);
        requireOwner(organization, currentUserId);

        if (!"INACTIVE".equals(organization.getStatus())) {
            throw new IllegalArgumentException("Organization is not inactive");
        }

        // Reactivation always goes back to ACTIVE — a SUSPENDED
        // organization (platform-side action, see handoff section 6)
        // is a separate concept and is not touched by this method.
        organization.setStatus("ACTIVE");
        organization = organizationRepository.save(organization);

        return toResponseDTO(organization);
    }

        // Platform-side action — no requireOwner/requireMembership here,
    // authorization is @PreAuthorize("hasRole('PLATFORM_ADMIN')") on the
    // controller. There's no "owning organization" to check membership
    // against; the platform is acting on the organization, not a member
    // of it.
    @Transactional
    public OrganizationResponseDTO suspendOrganization(UUID publicUuid, String reason) {
        Organization organization = findOrganizationOrThrow(publicUuid);

        if ("SUSPENDED".equals(organization.getStatus())) {
            throw new OrganizationAlreadySuspendedException(
                    "Organization is already suspended: " + publicUuid);
        }

        organization.setStatus("SUSPENDED");
        organization.setSuspensionReason(reason);
        organization = organizationRepository.save(organization);

        return toResponseDTO(organization);
    }

    @Transactional
    public OrganizationResponseDTO liftSuspension(UUID publicUuid) {
        Organization organization = findOrganizationOrThrow(publicUuid);

        if (!"SUSPENDED".equals(organization.getStatus())) {
            throw new OrganizationNotSuspendedException("Organization is not suspended: " + publicUuid);
        }

        organization.setStatus("ACTIVE");
        organization.setSuspensionReason(null);
        organization = organizationRepository.save(organization);

        return toResponseDTO(organization);
    }

    @Transactional
    public OrganizationMemberResponseDTO changeMemberRole(
            UUID orgPublicUuid, UUID memberUserPublicUuid, OrganizationMemberRequestDTO request, Long currentUserId) {
        Organization organization = findOrganizationOrThrow(orgPublicUuid);
        requireOwner(organization, currentUserId);
        requireActiveOrganization(organization);

        User targetUser = userRepository.findByPublicUuid(memberUserPublicUuid)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + memberUserPublicUuid));

        OrganizationMember member = organizationMemberRepository
                .findByOrganization_IdOrganizationAndUser_IdUser(organization.getIdOrganization(), targetUser.getIdUser())
                .orElseThrow(() -> new OrganizationMemberNotFoundException("User is not a member of this organization"));

        OrganizationRole newRole = organizationRoleRepository.findByCode(request.getRoleCode())
                .orElseThrow(() -> new IllegalArgumentException("Unknown role code: " + request.getRoleCode()));

        boolean isDemotingOwner = ROLE_OWNER.equals(member.getOrganizationRole().getCode())
                && !ROLE_OWNER.equals(newRole.getCode());

        if (isDemotingOwner) {
            long ownerCount = organizationMemberRepository
                    .findByOrganization_IdOrganization(organization.getIdOrganization()).stream()
                    .filter(m -> ROLE_OWNER.equals(m.getOrganizationRole().getCode()))
                    .count();
            if (ownerCount <= 1) {
                throw new IllegalArgumentException("Cannot demote the last remaining OWNER");
            }
        }

        member.setOrganizationRole(newRole);
        member = organizationMemberRepository.save(member);

        return toMemberResponseDTO(member);
    }

@Transactional
public void removeMember(
        UUID orgPublicUuid,
        UUID memberUserPublicUuid,
        Long currentUserId) {

    Organization organization = findOrganizationOrThrow(orgPublicUuid);
    requireActiveOrganization(organization);

    User targetUser = userRepository.findByPublicUuid(memberUserPublicUuid)
            .orElseThrow(() ->
                    new UserNotFoundException(
                            "User not found: " + memberUserPublicUuid));

    OrganizationMember member = organizationMemberRepository
            .findByOrganization_IdOrganizationAndUser_IdUser(
                    organization.getIdOrganization(),
                    targetUser.getIdUser())
            .orElseThrow(() ->
                    new OrganizationMemberNotFoundException(
                            "User is not a member of this organization"));

    boolean isSelf = targetUser.getIdUser().equals(currentUserId);

    if (!isSelf) {
        requireOwner(organization, currentUserId);

        if (ROLE_OWNER.equals(member.getOrganizationRole().getCode())) {
            throw new IllegalArgumentException(
                    "The organization OWNER cannot be removed");
        }
    } else if (ROLE_OWNER.equals(member.getOrganizationRole().getCode())) {

        long ownerCount = organizationMemberRepository
                .findByOrganization_IdOrganization(
                        organization.getIdOrganization())
                .stream()
                .filter(m ->
                        ROLE_OWNER.equals(
                                m.getOrganizationRole().getCode()))
                .count();

        if (ownerCount <= 1) {
            throw new IllegalArgumentException(
                    "The last OWNER cannot leave the organization");
        }
    }

    organizationMemberRepository.delete(member);
}

    public List<OrganizationMemberResponseDTO> listMembers(UUID orgPublicUuid, Long currentUserId) {
        Organization organization = findOrganizationOrThrow(orgPublicUuid);
        requireMembership(organization, currentUserId);

        return organizationMemberRepository.findByOrganization_IdOrganization(organization.getIdOrganization()).stream()
                .map(this::toMemberResponseDTO)
                .toList();
    }

    // ---- Internal helpers ----

    private Organization findOrganizationOrThrow(UUID publicUuid) {
        return organizationRepository.findByPublicUuid(publicUuid)
                .orElseThrow(() -> new OrganizationNotFoundException("Organization not found: " + publicUuid));
    }

    private OrganizationMember requireMembership(Organization organization, Long currentUserId) {
        return organizationMemberRepository
                .findByOrganization_IdOrganizationAndUser_IdUser(organization.getIdOrganization(), currentUserId)
                .orElseThrow(() -> new PermissionDeniedException("User is not a member of this organization"));
    }

    private void requireOwnerOrAdmin(Organization organization, Long currentUserId) {
        OrganizationMember membership = requireMembership(organization, currentUserId);
        String roleCode = membership.getOrganizationRole().getCode();
        if (!ROLE_OWNER.equals(roleCode) && !ROLE_ADMIN.equals(roleCode)) {
            throw new PermissionDeniedException("Only OWNER or ADMIN can perform this action");
        }
    }

    private void requireOwner(Organization organization, Long currentUserId) {
        OrganizationMember membership = requireMembership(organization, currentUserId);
        if (!ROLE_OWNER.equals(membership.getOrganizationRole().getCode())) {
            throw new PermissionDeniedException("Only OWNER can perform this action");
        }
    }

    // Blocks any write operation while the organization is INACTIVE or
    // SUSPENDED. Reads (getOrganization, listMembers, listRoles,
    // listOrganizationsForCurrentUser) are intentionally NOT gated by
    // this — a member should still be able to see that their org is
    // inactive/suspended, and the OWNER needs to see it in order to
    // reactivate it (SUSPENDED can only be lifted platform-side, though —
    // see liftSuspension).
    private void requireActiveOrganization(Organization organization) {
        String status = organization.getStatus();
        if ("INACTIVE".equals(status)) {
            throw new OrganizationInactiveException(
                    "This organization is inactive; no changes are allowed until it is reactivated");
        }
        if ("SUSPENDED".equals(status)) {
            throw new OrganizationInactiveException(
                    "This organization is suspended; no changes are allowed until the suspension is lifted");
        }
    }

    private OrganizationResponseDTO toResponseDTO(Organization organization) {
        long memberCount = organizationMemberRepository
                .countByOrganization_IdOrganization(organization.getIdOrganization());

        return OrganizationResponseDTO.builder()
                .publicUuid(organization.getPublicUuid())
                .name(organization.getName())
                .slug(organization.getSlug())
                .status(organization.getStatus())
                .suspensionReason(organization.getSuspensionReason())
                .memberCount(memberCount)
                .createdAt(organization.getCreatedAt())
                .updatedAt(organization.getUpdatedAt())
                .build();
    }

    private OrganizationMemberResponseDTO toMemberResponseDTO(OrganizationMember member) {
        User user = member.getUser();
        OrganizationRole role = member.getOrganizationRole();
        return OrganizationMemberResponseDTO.builder()
                .userPublicUuid(user.getPublicUuid())
                .userFirstName(user.getFirstName())
                .userLastName(user.getLastName())
                .userEmail(user.getEmail())
                .roleCode(role.getCode())
                .roleName(role.getName())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}