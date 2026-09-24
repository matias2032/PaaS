package com.dev58.paasbackend.project.service;

import com.dev58.paasbackend.common.security.CryptoService;
import com.dev58.paasbackend.organization.entity.Organization;
import com.dev58.paasbackend.organization.entity.OrganizationMember;
import com.dev58.paasbackend.organization.exception.OrganizationInactiveException;
import com.dev58.paasbackend.organization.exception.OrganizationNotFoundException;
import com.dev58.paasbackend.organization.exception.PermissionDeniedException;
import com.dev58.paasbackend.organization.repository.OrganizationMemberRepository;
import com.dev58.paasbackend.organization.repository.OrganizationRepository;
import com.dev58.paasbackend.project.dto.*;
import com.dev58.paasbackend.project.entity.GitConnection;
import com.dev58.paasbackend.project.entity.GitProvider;
import com.dev58.paasbackend.project.entity.Project;
import com.dev58.paasbackend.project.exception.GitConnectionNotFoundException;
import com.dev58.paasbackend.project.exception.GitProviderNotFoundException;
import com.dev58.paasbackend.project.exception.ProjectNotFoundException;
import com.dev58.paasbackend.project.repository.GitConnectionRepository;
import com.dev58.paasbackend.project.repository.GitProviderRepository;
import com.dev58.paasbackend.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private static final String ROLE_OWNER = "OWNER";

    private final ProjectRepository projectRepository;
    private final GitProviderRepository gitProviderRepository;
    private final GitConnectionRepository gitConnectionRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final CryptoService cryptoService;

    // ---- Project: Create ----

    @Transactional
    public ProjectResponseDTO createProject(UUID orgPublicUuid, ProjectRequestDTO request, Long currentUserId) {
        Organization organization = findOrganizationOrThrow(orgPublicUuid);
        requireOwner(organization, currentUserId);
        requireActiveOrganization(organization);

        if (projectRepository.existsByOrganization_IdOrganizationAndSlug(
                organization.getIdOrganization(), request.getSlug())) {
            throw new IllegalArgumentException("Slug already in use in this organization: " + request.getSlug());
        }

        Project project = Project.builder()
                .organization(organization)
                .name(request.getName())
                .slug(request.getSlug())
                .description(request.getDescription())
                .build();
        project = projectRepository.save(project);

        return toResponseDTO(project);
    }

    // ---- Project: Read ----

    public ProjectResponseDTO getProject(UUID publicUuid, Long currentUserId) {
        Project project = findProjectOrThrow(publicUuid);
        requireMembership(project.getOrganization(), currentUserId);
        return toResponseDTO(project);
    }

    public List<ProjectResponseDTO> listProjects(UUID orgPublicUuid, Long currentUserId) {
        Organization organization = findOrganizationOrThrow(orgPublicUuid);
        requireMembership(organization, currentUserId);

        return projectRepository.findByOrganization_IdOrganization(organization.getIdOrganization()).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    // ---- Project: Update ----

    @Transactional
    public ProjectResponseDTO updateProject(UUID publicUuid, ProjectRequestDTO request, Long currentUserId) {
        Project project = findProjectOrThrow(publicUuid);
        requireOwner(project.getOrganization(), currentUserId);
        requireActiveOrganization(project.getOrganization());

        project.setName(request.getName());
        project.setDescription(request.getDescription());
        // Slug intentionally not updated here — same immutability rule
        // applied to Organization.slug, for the same reason (referenced
        // elsewhere, e.g. future service subdomains).
        project = projectRepository.save(project);

        return toResponseDTO(project);
    }

    // ---- Project: Archive / Reactivate ----

    @Transactional
    public ProjectResponseDTO archiveProject(UUID publicUuid, Long currentUserId) {
        Project project = findProjectOrThrow(publicUuid);
        requireOwner(project.getOrganization(), currentUserId);
        requireActiveOrganization(project.getOrganization());

        if ("ARCHIVED".equals(project.getStatus())) {
            throw new IllegalArgumentException("Project is already archived");
        }

        project.setStatus("ARCHIVED");
        project = projectRepository.save(project);

        return toResponseDTO(project);
    }

    @Transactional
    public ProjectResponseDTO reactivateProject(UUID publicUuid, Long currentUserId) {
        Project project = findProjectOrThrow(publicUuid);
        requireOwner(project.getOrganization(), currentUserId);
        requireActiveOrganization(project.getOrganization());

        if (!"ARCHIVED".equals(project.getStatus())) {
            throw new IllegalArgumentException("Project is not archived");
        }

        project.setStatus("ACTIVE");
        project = projectRepository.save(project);

        return toResponseDTO(project);
    }

    // ---- GitProvider: Read-only catalog ----

    public List<GitProviderResponseDTO> listGitProviders() {
        return gitProviderRepository.findAll().stream()
                .map(provider -> GitProviderResponseDTO.builder()
                        .idGitProvider(provider.getIdGitProvider())
                        .code(provider.getCode())
                        .name(provider.getName())
                        .build())
                .toList();
    }

    // ---- GitConnection: Create ----

    @Transactional
    public GitConnectionResponseDTO createGitConnection(UUID orgPublicUuid, GitConnectionRequestDTO request, Long currentUserId) {
        Organization organization = findOrganizationOrThrow(orgPublicUuid);
        requireOwner(organization, currentUserId);
        requireActiveOrganization(organization);

        GitProvider provider = gitProviderRepository.findByCode(request.getGitProviderCode())
                .orElseThrow(() -> new GitProviderNotFoundException("Unknown git provider code: " + request.getGitProviderCode()));

        // Assumption: one active connection per (organization, provider).
        // Not yet confirmed as a hard rule — flagging for review.
        gitConnectionRepository.findByOrganization_IdOrganizationAndGitProvider_IdGitProvider(
                organization.getIdOrganization(), provider.getIdGitProvider())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "This organization already has a connection to " + provider.getCode());
                });

        GitConnection connection = GitConnection.builder()
                .organization(organization)
                .gitProvider(provider)
                .externalAccountId(request.getExternalAccountId())
                .externalAccountName(request.getExternalAccountName())
                .accessTokenEncrypted(
                        request.getAccessToken() != null && !request.getAccessToken().isBlank()
                                ? cryptoService.encrypt(request.getAccessToken())
                                : null)
                .build();
        connection = gitConnectionRepository.save(connection);

        return toConnectionResponseDTO(connection);
    }

    // ---- GitConnection: Read ----

    public List<GitConnectionResponseDTO> listGitConnections(UUID orgPublicUuid, Long currentUserId) {
        Organization organization = findOrganizationOrThrow(orgPublicUuid);
        requireMembership(organization, currentUserId);

        return gitConnectionRepository.findByOrganization_IdOrganization(organization.getIdOrganization()).stream()
                .map(this::toConnectionResponseDTO)
                .toList();
    }

    // ---- GitConnection: Revoke ----

    @Transactional
    public GitConnectionResponseDTO revokeGitConnection(UUID publicUuid, Long currentUserId) {
        GitConnection connection = gitConnectionRepository.findByPublicUuid(publicUuid)
                .orElseThrow(() -> new GitConnectionNotFoundException("Git connection not found: " + publicUuid));
        requireOwner(connection.getOrganization(), currentUserId);
        requireActiveOrganization(connection.getOrganization());

        connection.setStatus("REVOKED");
        // Clear stored credentials on revoke — a revoked connection has
        // no legitimate reason to keep an encrypted token around, even
        // though it's already unusable by application logic elsewhere.
        connection.setAccessTokenEncrypted(null);
        connection.setRefreshTokenEncrypted(null);
        connection = gitConnectionRepository.save(connection);

        return toConnectionResponseDTO(connection);
    }

    // ---- Internal helpers ----

    private Organization findOrganizationOrThrow(UUID publicUuid) {
        return organizationRepository.findByPublicUuid(publicUuid)
                .orElseThrow(() -> new OrganizationNotFoundException("Organization not found: " + publicUuid));
    }

    private Project findProjectOrThrow(UUID publicUuid) {
        return projectRepository.findByPublicUuid(publicUuid)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + publicUuid));
    }

    private OrganizationMember requireMembership(Organization organization, Long currentUserId) {
        return organizationMemberRepository
                .findByOrganization_IdOrganizationAndUser_IdUser(organization.getIdOrganization(), currentUserId)
                .orElseThrow(() -> new PermissionDeniedException("User is not a member of this organization"));
    }

    private void requireOwner(Organization organization, Long currentUserId) {
        OrganizationMember membership = requireMembership(organization, currentUserId);
        if (!ROLE_OWNER.equals(membership.getOrganizationRole().getCode())) {
            throw new PermissionDeniedException("Only OWNER can perform this action");
        }
    }

    // Mirrors OrganizationService.requireActiveOrganization. Blocks
    // project/git-connection writes while the parent organization is
    // INACTIVE or SUSPENDED; reads (getProject, listProjects,
    // listGitConnections, listGitProviders) are intentionally NOT
    // gated, same reasoning as there — a member should still be able
    // to see the project list while the org is inactive/suspended.
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

    private ProjectResponseDTO toResponseDTO(Project project) {
        return ProjectResponseDTO.builder()
                .publicUuid(project.getPublicUuid())
                .organizationPublicUuid(project.getOrganization().getPublicUuid())
                .name(project.getName())
                .slug(project.getSlug())
                .description(project.getDescription())
                .status(project.getStatus())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    private GitConnectionResponseDTO toConnectionResponseDTO(GitConnection connection) {
        return GitConnectionResponseDTO.builder()
                .publicUuid(connection.getPublicUuid())
                .organizationPublicUuid(connection.getOrganization().getPublicUuid())
                .gitProviderCode(connection.getGitProvider().getCode())
                .externalAccountId(connection.getExternalAccountId())
                .externalAccountName(connection.getExternalAccountName())
                .hasAccessToken(connection.getAccessTokenEncrypted() != null)
                .status(connection.getStatus())
                .tokenExpiresAt(connection.getTokenExpiresAt())
                .createdAt(connection.getCreatedAt())
                .updatedAt(connection.getUpdatedAt())
                .build();
    }
}