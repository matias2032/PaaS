// com.dev58.paasbackend.service.service.ServiceService
package com.dev58.paasbackend.service.service;

import com.dev58.paasbackend.common.security.CryptoService;
import com.dev58.paasbackend.organization.entity.Organization;
import com.dev58.paasbackend.organization.entity.OrganizationMember;
import com.dev58.paasbackend.organization.exception.OrganizationInactiveException;
import com.dev58.paasbackend.organization.exception.PermissionDeniedException;
import com.dev58.paasbackend.organization.repository.OrganizationMemberRepository;
import com.dev58.paasbackend.project.entity.GitConnection;
import com.dev58.paasbackend.project.entity.Project;
import com.dev58.paasbackend.project.exception.GitConnectionNotFoundException;
import com.dev58.paasbackend.project.exception.ProjectNotFoundException;
import com.dev58.paasbackend.project.repository.GitConnectionRepository;
import com.dev58.paasbackend.project.repository.ProjectRepository;
import com.dev58.paasbackend.service.dto.*;
import com.dev58.paasbackend.service.entity.*;
import com.dev58.paasbackend.service.exception.*;
import com.dev58.paasbackend.service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

// Fully-qualified @org.springframework.stereotype.Service below to
// avoid a name clash with our own Service entity in this same file.
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ServiceService {

    private static final String ROLE_OWNER = "OWNER";
    private static final Set<String> ALLOWED_TRIGGER_TYPES =
            Set.of("MANUAL", "GIT_PUSH", "WEBHOOK", "API", "SYSTEM");

    private final ServiceTypeRepository serviceTypeRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceRepositoryEntityRepository serviceRepositoryEntityRepository;
    private final ServiceBuildConfigRepository serviceBuildConfigRepository;
    private final ServiceResourceConfigRepository serviceResourceConfigRepository;
    private final EnvironmentVariableRepository environmentVariableRepository;
    private final DomainRepository domainRepository;
    private final DeploymentRepository deploymentRepository;

    private final ProjectRepository projectRepository;
    private final GitConnectionRepository gitConnectionRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final CryptoService cryptoService;

    // ==================== ServiceType (read-only catalog) ====================

    public List<ServiceTypeResponseDTO> listServiceTypes() {
        return serviceTypeRepository.findAll().stream()
                .map(type -> ServiceTypeResponseDTO.builder()
                        .idServiceType(type.getIdServiceType())
                        .code(type.getCode())
                        .name(type.getName())
                        .build())
                .toList();
    }

    // ==================== Service ====================

    @Transactional
    public ServiceResponseDTO createService(UUID projectPublicUuid, ServiceRequestDTO request, Long currentUserId) {
        Project project = findProjectOrThrow(projectPublicUuid);
        requireOwner(project.getOrganization(), currentUserId);
        requireActiveOrganization(project.getOrganization());

        if (serviceRepository.existsByProject_IdProjectAndSlug(project.getIdProject(), request.getSlug())) {
            throw new IllegalArgumentException("Slug already in use in this project: " + request.getSlug());
        }

        ServiceType serviceType = findServiceTypeOrThrow(request.getServiceTypeCode());

        Service service = Service.builder()
                .project(project)
                .serviceType(serviceType)
                .name(request.getName())
                .slug(request.getSlug())
                .autoDeploy(request.getAutoDeploy())
                .build();
        service = serviceRepository.save(service);

        return toServiceResponseDTO(service);
    }

    public ServiceResponseDTO getService(UUID publicUuid, Long currentUserId) {
        Service service = findServiceOrThrow(publicUuid);
        requireMembership(getOrganization(service), currentUserId);
        return toServiceResponseDTO(service);
    }

    public List<ServiceResponseDTO> listServices(UUID projectPublicUuid, Long currentUserId) {
        Project project = findProjectOrThrow(projectPublicUuid);
        requireMembership(project.getOrganization(), currentUserId);

        return serviceRepository.findByProject_IdProject(project.getIdProject()).stream()
                .map(this::toServiceResponseDTO)
                .toList();
    }

    @Transactional
    public ServiceResponseDTO updateService(UUID publicUuid, ServiceRequestDTO request, Long currentUserId) {
        Service service = findServiceOrThrow(publicUuid);
        Organization organization = getOrganization(service);
        requireOwner(organization, currentUserId);
        requireActiveOrganization(organization);

        service.setName(request.getName());
        if (request.getAutoDeploy() != null) {
            service.setAutoDeploy(request.getAutoDeploy());
        }
        // slug and serviceType intentionally not updated — same
        // immutability rule as Project.slug / Organization.slug.
        service = serviceRepository.save(service);

        return toServiceResponseDTO(service);
    }

    @Transactional
    public ServiceResponseDTO archiveService(UUID publicUuid, Long currentUserId) {
        Service service = findServiceOrThrow(publicUuid);
        Organization organization = getOrganization(service);
        requireOwner(organization, currentUserId);
        requireActiveOrganization(organization);

        if ("ARCHIVED".equals(service.getStatus())) {
            throw new IllegalArgumentException("Service is already archived");
        }

        service.setStatus("ARCHIVED");
        service = serviceRepository.save(service);

        return toServiceResponseDTO(service);
    }

    @Transactional
    public ServiceResponseDTO reactivateService(UUID publicUuid, Long currentUserId) {
        Service service = findServiceOrThrow(publicUuid);
        Organization organization = getOrganization(service);
        requireOwner(organization, currentUserId);
        requireActiveOrganization(organization);

        if (!"ARCHIVED".equals(service.getStatus())) {
            throw new IllegalArgumentException("Service is not archived");
        }

        // Back to CREATED, not RUNNING — this module never talks to
        // Coolify yet, so there is no real running state to restore.
        service.setStatus("CREATED");
        service = serviceRepository.save(service);

        return toServiceResponseDTO(service);
    }

    // ==================== ServiceRepositoryEntity (1:1) ====================

    @Transactional
    public ServiceRepositoryResponseDTO upsertServiceRepository(
            UUID servicePublicUuid, ServiceRepositoryRequestDTO request, Long currentUserId) {
        Service service = findServiceOrThrow(servicePublicUuid);
        Organization organization = getOrganization(service);
        requireOwner(organization, currentUserId);
        requireActiveOrganization(organization);

        GitConnection connection = null;
        if (request.getGitConnectionPublicUuid() != null) {
            connection = gitConnectionRepository.findByPublicUuid(request.getGitConnectionPublicUuid())
                    .orElseThrow(() -> new GitConnectionNotFoundException(
                            "Git connection not found: " + request.getGitConnectionPublicUuid()));
        }

        ServiceRepositoryEntity repository = serviceRepositoryEntityRepository
                .findByService_IdService(service.getIdService())
                .orElseGet(() -> ServiceRepositoryEntity.builder().service(service).build());

        repository.setGitConnection(connection);
        repository.setRepositoryUrl(request.getRepositoryUrl());
        repository.setRepositoryOwner(request.getRepositoryOwner());
        repository.setRepositoryName(request.getRepositoryName());
        if (request.getBranch() != null) {
            repository.setBranch(request.getBranch());
        }
        repository = serviceRepositoryEntityRepository.save(repository);

        return toServiceRepositoryResponseDTO(repository);
    }

    public ServiceRepositoryResponseDTO getServiceRepository(UUID servicePublicUuid, Long currentUserId) {
        Service service = findServiceOrThrow(servicePublicUuid);
        requireMembership(getOrganization(service), currentUserId);

        ServiceRepositoryEntity repository = serviceRepositoryEntityRepository
                .findByService_IdService(service.getIdService())
                .orElseThrow(() -> new ServiceRepositoryNotFoundException(
                        "No repository configured for this service"));

        return toServiceRepositoryResponseDTO(repository);
    }

    // ==================== ServiceBuildConfig (1:1) ====================

    @Transactional
    public ServiceBuildConfigResponseDTO upsertServiceBuildConfig(
            UUID servicePublicUuid, ServiceBuildConfigRequestDTO request, Long currentUserId) {
        Service service = findServiceOrThrow(servicePublicUuid);
        Organization organization = getOrganization(service);
        requireOwner(organization, currentUserId);
        requireActiveOrganization(organization);

        ServiceBuildConfig config = serviceBuildConfigRepository
                .findByService_IdService(service.getIdService())
                .orElseGet(() -> ServiceBuildConfig.builder().service(service).build());

        if (request.getRootDirectory() != null) {
            config.setRootDirectory(request.getRootDirectory());
        }
        config.setBuildCommand(request.getBuildCommand());
        config.setStartCommand(request.getStartCommand());
        config.setDockerfilePath(request.getDockerfilePath());
        config.setHealthCheckPath(request.getHealthCheckPath());
        config.setPort(request.getPort());
        config = serviceBuildConfigRepository.save(config);

        return toServiceBuildConfigResponseDTO(config);
    }

    public ServiceBuildConfigResponseDTO getServiceBuildConfig(UUID servicePublicUuid, Long currentUserId) {
        Service service = findServiceOrThrow(servicePublicUuid);
        requireMembership(getOrganization(service), currentUserId);

        ServiceBuildConfig config = serviceBuildConfigRepository
                .findByService_IdService(service.getIdService())
                .orElseThrow(() -> new ServiceBuildConfigNotFoundException(
                        "No build config configured for this service"));

        return toServiceBuildConfigResponseDTO(config);
    }

    // ==================== ServiceResourceConfig (1:1) ====================

    @Transactional
    public ServiceResourceConfigResponseDTO upsertServiceResourceConfig(
            UUID servicePublicUuid, ServiceResourceConfigRequestDTO request, Long currentUserId) {
        Service service = findServiceOrThrow(servicePublicUuid);
        Organization organization = getOrganization(service);
        requireOwner(organization, currentUserId);
        requireActiveOrganization(organization);

        // Not validated against the org's Plan limits here on purpose —
        // that check belongs to BILLING (PlanResourceLimit) and isn't
        // wired up cross-module yet; flagging for when it is.
        ServiceResourceConfig config = serviceResourceConfigRepository
                .findByService_IdService(service.getIdService())
                .orElseGet(() -> ServiceResourceConfig.builder().service(service).build());

        config.setCpuLimit(request.getCpuLimit());
        config.setMemoryLimitMb(request.getMemoryLimitMb());
        config.setStorageLimitMb(request.getStorageLimitMb());
        config = serviceResourceConfigRepository.save(config);

        return toServiceResourceConfigResponseDTO(config);
    }

    public ServiceResourceConfigResponseDTO getServiceResourceConfig(UUID servicePublicUuid, Long currentUserId) {
        Service service = findServiceOrThrow(servicePublicUuid);
        requireMembership(getOrganization(service), currentUserId);

        ServiceResourceConfig config = serviceResourceConfigRepository
                .findByService_IdService(service.getIdService())
                .orElseThrow(() -> new ServiceResourceConfigNotFoundException(
                        "No resource config configured for this service"));

        return toServiceResourceConfigResponseDTO(config);
    }

    // ==================== EnvironmentVariable (N, upsert by key) ====================

    @Transactional
    public EnvironmentVariableResponseDTO upsertEnvironmentVariable(
            UUID servicePublicUuid, EnvironmentVariableRequestDTO request, Long currentUserId) {
        Service service = findServiceOrThrow(servicePublicUuid);
        Organization organization = getOrganization(service);
        requireOwner(organization, currentUserId);
        requireActiveOrganization(organization);

        EnvironmentVariable variable = environmentVariableRepository
                .findByService_IdServiceAndVariableKey(service.getIdService(), request.getVariableKey())
                .orElseGet(() -> EnvironmentVariable.builder()
                        .service(service)
                        .variableKey(request.getVariableKey())
                        .build());

        variable.setValueEncrypted(cryptoService.encrypt(request.getValue()));
        if (request.getIsSecret() != null) {
            variable.setIsSecret(request.getIsSecret());
        }
        variable = environmentVariableRepository.save(variable);

        return toEnvironmentVariableResponseDTO(variable);
    }

    public List<EnvironmentVariableResponseDTO> listEnvironmentVariables(UUID servicePublicUuid, Long currentUserId) {
        Service service = findServiceOrThrow(servicePublicUuid);
        requireMembership(getOrganization(service), currentUserId);

        // Values are never decrypted here — response DTO has no value
        // field at all, per handoff section 3.
        return environmentVariableRepository.findByService_IdService(service.getIdService()).stream()
                .map(this::toEnvironmentVariableResponseDTO)
                .toList();
    }

    @Transactional
    public void deleteEnvironmentVariable(UUID servicePublicUuid, String variableKey, Long currentUserId) {
        Service service = findServiceOrThrow(servicePublicUuid);
        Organization organization = getOrganization(service);
        requireOwner(organization, currentUserId);
        requireActiveOrganization(organization);

        EnvironmentVariable variable = environmentVariableRepository
                .findByService_IdServiceAndVariableKey(service.getIdService(), variableKey)
                .orElseThrow(() -> new EnvironmentVariableNotFoundException(
                        "Environment variable not found: " + variableKey));

        environmentVariableRepository.delete(variable);
    }

    // ==================== Domain (N) ====================

    @Transactional
    public DomainResponseDTO createDomain(UUID servicePublicUuid, DomainRequestDTO request, Long currentUserId) {
        Service service = findServiceOrThrow(servicePublicUuid);
        Organization organization = getOrganization(service);
        requireOwner(organization, currentUserId);
        requireActiveOrganization(organization);

        if (domainRepository.existsByHostname(request.getHostname())) {
            throw new IllegalArgumentException("Hostname already in use: " + request.getHostname());
        }

        boolean makePrimary = Boolean.TRUE.equals(request.getIsPrimary());
        if (makePrimary) {
            unsetExistingPrimaryDomain(service.getIdService());
        }

        Domain domain = Domain.builder()
                .service(service)
                .hostname(request.getHostname())
                .isPrimary(makePrimary)
                .build();
        domain = domainRepository.save(domain);

        return toDomainResponseDTO(domain);
    }

    public List<DomainResponseDTO> listDomains(UUID servicePublicUuid, Long currentUserId) {
        Service service = findServiceOrThrow(servicePublicUuid);
        requireMembership(getOrganization(service), currentUserId);

        return domainRepository.findByService_IdService(service.getIdService()).stream()
                .map(this::toDomainResponseDTO)
                .toList();
    }

    @Transactional
    public void deleteDomain(UUID domainPublicUuid, Long currentUserId) {
        Domain domain = domainRepository.findByPublicUuid(domainPublicUuid)
                .orElseThrow(() -> new DomainNotFoundException("Domain not found: " + domainPublicUuid));
        Organization organization = getOrganization(domain.getService());
        requireOwner(organization, currentUserId);
        requireActiveOrganization(organization);

        domainRepository.delete(domain);
    }

    private void unsetExistingPrimaryDomain(Long idService) {
        domainRepository.findByService_IdServiceAndIsPrimaryTrue(idService)
                .ifPresent(existing -> {
                    existing.setIsPrimary(false);
                    domainRepository.save(existing);
                });
    }

    // ==================== Deployment (N, registo/histórico — sem Coolify real) ====================

    @Transactional
    public DeploymentResponseDTO createDeployment(
            UUID servicePublicUuid, DeploymentRequestDTO request, Long currentUserId) {
        Service service = findServiceOrThrow(servicePublicUuid);
        Organization organization = getOrganization(service);
        requireOwner(organization, currentUserId);
        requireActiveOrganization(organization);

        if (!ALLOWED_TRIGGER_TYPES.contains(request.getTriggerType())) {
            throw new DeploymentException("Invalid trigger type: " + request.getTriggerType());
        }

        // Registration only — no CoolifyClient call. status stays at
        // its default (QUEUED, set in @PrePersist) and is never moved
        // forward here; that belongs to INFRASTRUCTURE.
        Deployment deployment = Deployment.builder()
                .service(service)
                .triggerType(request.getTriggerType())
                .commitHash(request.getCommitHash())
                .commitMessage(request.getCommitMessage())
                .branch(request.getBranch())
                .build();
        deployment = deploymentRepository.save(deployment);

        return toDeploymentResponseDTO(deployment);
    }

    public List<DeploymentResponseDTO> listDeployments(UUID servicePublicUuid, Long currentUserId) {
        Service service = findServiceOrThrow(servicePublicUuid);
        requireMembership(getOrganization(service), currentUserId);

        return deploymentRepository.findByService_IdServiceOrderByCreatedAtDesc(service.getIdService()).stream()
                .map(this::toDeploymentResponseDTO)
                .toList();
    }

    public DeploymentResponseDTO getDeployment(UUID publicUuid, Long currentUserId) {
        Deployment deployment = deploymentRepository.findByPublicUuid(publicUuid)
                .orElseThrow(() -> new DeploymentNotFoundException("Deployment not found: " + publicUuid));
        requireMembership(getOrganization(deployment.getService()), currentUserId);

        return toDeploymentResponseDTO(deployment);
    }

    // ==================== Internal helpers ====================

    private Project findProjectOrThrow(UUID publicUuid) {
        return projectRepository.findByPublicUuid(publicUuid)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + publicUuid));
    }

    private Service findServiceOrThrow(UUID publicUuid) {
        return serviceRepository.findByPublicUuid(publicUuid)
                .orElseThrow(() -> new ServiceNotFoundException("Service not found: " + publicUuid));
    }

    private ServiceType findServiceTypeOrThrow(String code) {
        return serviceTypeRepository.findByCode(code)
                .orElseThrow(() -> new ServiceTypeNotFoundException("Unknown service type code: " + code));
    }

    // Three-level authorization climb: Service -> Project -> Organization.
    // Kept as a one-line helper rather than duplicating the chain at
    // every call site.
    private Organization getOrganization(Service service) {
        return service.getProject().getOrganization();
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

    private void requireActiveOrganization(Organization organization) {
        if ("INACTIVE".equals(organization.getStatus())) {
            throw new OrganizationInactiveException(
                    "This organization is inactive; no changes are allowed until it is reactivated");
        }
    }

    // ==================== DTO mapping ====================

    private ServiceResponseDTO toServiceResponseDTO(Service service) {
        return ServiceResponseDTO.builder()
                .publicUuid(service.getPublicUuid())
                .projectPublicUuid(service.getProject().getPublicUuid())
                .organizationPublicUuid(getOrganization(service).getPublicUuid())
                .serviceTypeCode(service.getServiceType().getCode())
                .serviceTypeName(service.getServiceType().getName())
                .name(service.getName())
                .slug(service.getSlug())
                .status(service.getStatus())
                .autoDeploy(service.getAutoDeploy())
                .createdAt(service.getCreatedAt())
                .updatedAt(service.getUpdatedAt())
                .build();
    }

    private ServiceRepositoryResponseDTO toServiceRepositoryResponseDTO(ServiceRepositoryEntity repository) {
        return ServiceRepositoryResponseDTO.builder()
                .gitConnectionPublicUuid(
                        repository.getGitConnection() != null
                                ? repository.getGitConnection().getPublicUuid()
                                : null)
                .repositoryUrl(repository.getRepositoryUrl())
                .repositoryOwner(repository.getRepositoryOwner())
                .repositoryName(repository.getRepositoryName())
                .branch(repository.getBranch())
                .createdAt(repository.getCreatedAt())
                .updatedAt(repository.getUpdatedAt())
                .build();
    }

    private ServiceBuildConfigResponseDTO toServiceBuildConfigResponseDTO(ServiceBuildConfig config) {
        return ServiceBuildConfigResponseDTO.builder()
                .rootDirectory(config.getRootDirectory())
                .buildCommand(config.getBuildCommand())
                .startCommand(config.getStartCommand())
                .dockerfilePath(config.getDockerfilePath())
                .healthCheckPath(config.getHealthCheckPath())
                .port(config.getPort())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    private ServiceResourceConfigResponseDTO toServiceResourceConfigResponseDTO(ServiceResourceConfig config) {
        return ServiceResourceConfigResponseDTO.builder()
                .cpuLimit(config.getCpuLimit())
                .memoryLimitMb(config.getMemoryLimitMb())
                .storageLimitMb(config.getStorageLimitMb())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    private EnvironmentVariableResponseDTO toEnvironmentVariableResponseDTO(EnvironmentVariable variable) {
        return EnvironmentVariableResponseDTO.builder()
                .variableKey(variable.getVariableKey())
                .isSecret(variable.getIsSecret())
                .createdAt(variable.getCreatedAt())
                .updatedAt(variable.getUpdatedAt())
                .build();
    }

    private DomainResponseDTO toDomainResponseDTO(Domain domain) {
        return DomainResponseDTO.builder()
                .publicUuid(domain.getPublicUuid())
                .hostname(domain.getHostname())
                .isPrimary(domain.getIsPrimary())
                .verificationStatus(domain.getVerificationStatus())
                .sslStatus(domain.getSslStatus())
                .verifiedAt(domain.getVerifiedAt())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    private DeploymentResponseDTO toDeploymentResponseDTO(Deployment deployment) {
        return DeploymentResponseDTO.builder()
                .publicUuid(deployment.getPublicUuid())
                .coolifyDeploymentUuid(deployment.getCoolifyDeploymentUuid())
                .commitHash(deployment.getCommitHash())
                .commitMessage(deployment.getCommitMessage())
                .branch(deployment.getBranch())
                .triggerType(deployment.getTriggerType())
                .status(deployment.getStatus())
                .startedAt(deployment.getStartedAt())
                .finishedAt(deployment.getFinishedAt())
                .createdAt(deployment.getCreatedAt())
                .build();
    }
}