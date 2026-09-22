// com.dev58.paasbackend.service.controller.ServiceController
package com.dev58.paasbackend.service.controller;

import com.dev58.paasbackend.common.security.AuthenticatedUser;
import com.dev58.paasbackend.service.dto.*;
import com.dev58.paasbackend.service.service.ServiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceService serviceService;

    // ---- ServiceType (read-only catalog) ----

    @GetMapping("/api/service-types")
    public List<ServiceTypeResponseDTO> listServiceTypes() {
        return serviceService.listServiceTypes();
    }

    // ---- Service ----

    @PostMapping("/api/projects/{projectPublicUuid}/services")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceResponseDTO createService(
            @PathVariable UUID projectPublicUuid,
            @Valid @RequestBody ServiceRequestDTO request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return serviceService.createService(projectPublicUuid, request, currentUser.getIdUser());
    }

    @GetMapping("/api/projects/{projectPublicUuid}/services")
    public List<ServiceResponseDTO> listServices(
            @PathVariable UUID projectPublicUuid,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return serviceService.listServices(projectPublicUuid, currentUser.getIdUser());
    }

    @GetMapping("/api/services/{publicUuid}")
    public ServiceResponseDTO getService(
            @PathVariable UUID publicUuid,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return serviceService.getService(publicUuid, currentUser.getIdUser());
    }

    @PatchMapping("/api/services/{publicUuid}")
    public ServiceResponseDTO updateService(
            @PathVariable UUID publicUuid,
            @Valid @RequestBody ServiceRequestDTO request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return serviceService.updateService(publicUuid, request, currentUser.getIdUser());
    }

    @PostMapping("/api/services/{publicUuid}/archive")
    public ServiceResponseDTO archiveService(
            @PathVariable UUID publicUuid,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return serviceService.archiveService(publicUuid, currentUser.getIdUser());
    }

    @PostMapping("/api/services/{publicUuid}/reactivate")
    public ServiceResponseDTO reactivateService(
            @PathVariable UUID publicUuid,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return serviceService.reactivateService(publicUuid, currentUser.getIdUser());
    }

    // ---- ServiceRepositoryEntity (1:1) ----

    @PutMapping("/api/services/{servicePublicUuid}/repository")
    public ServiceRepositoryResponseDTO upsertServiceRepository(
            @PathVariable UUID servicePublicUuid,
            @Valid @RequestBody ServiceRepositoryRequestDTO request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return serviceService.upsertServiceRepository(servicePublicUuid, request, currentUser.getIdUser());
    }

    @GetMapping("/api/services/{servicePublicUuid}/repository")
    public ServiceRepositoryResponseDTO getServiceRepository(
            @PathVariable UUID servicePublicUuid,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return serviceService.getServiceRepository(servicePublicUuid, currentUser.getIdUser());
    }

    // ---- ServiceBuildConfig (1:1) ----

    @PutMapping("/api/services/{servicePublicUuid}/build-config")
    public ServiceBuildConfigResponseDTO upsertServiceBuildConfig(
            @PathVariable UUID servicePublicUuid,
            @Valid @RequestBody ServiceBuildConfigRequestDTO request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return serviceService.upsertServiceBuildConfig(servicePublicUuid, request, currentUser.getIdUser());
    }

    @GetMapping("/api/services/{servicePublicUuid}/build-config")
    public ServiceBuildConfigResponseDTO getServiceBuildConfig(
            @PathVariable UUID servicePublicUuid,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return serviceService.getServiceBuildConfig(servicePublicUuid, currentUser.getIdUser());
    }

    // ---- ServiceResourceConfig (1:1) ----

    @PutMapping("/api/services/{servicePublicUuid}/resource-config")
    public ServiceResourceConfigResponseDTO upsertServiceResourceConfig(
            @PathVariable UUID servicePublicUuid,
            @Valid @RequestBody ServiceResourceConfigRequestDTO request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return serviceService.upsertServiceResourceConfig(servicePublicUuid, request, currentUser.getIdUser());
    }

    @GetMapping("/api/services/{servicePublicUuid}/resource-config")
    public ServiceResourceConfigResponseDTO getServiceResourceConfig(
            @PathVariable UUID servicePublicUuid,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return serviceService.getServiceResourceConfig(servicePublicUuid, currentUser.getIdUser());
    }

    // ---- EnvironmentVariable (N, upsert by key) ----

    @PutMapping("/api/services/{servicePublicUuid}/environment-variables")
    public EnvironmentVariableResponseDTO upsertEnvironmentVariable(
            @PathVariable UUID servicePublicUuid,
            @Valid @RequestBody EnvironmentVariableRequestDTO request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return serviceService.upsertEnvironmentVariable(servicePublicUuid, request, currentUser.getIdUser());
    }

    @GetMapping("/api/services/{servicePublicUuid}/environment-variables")
    public List<EnvironmentVariableResponseDTO> listEnvironmentVariables(
            @PathVariable UUID servicePublicUuid,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return serviceService.listEnvironmentVariables(servicePublicUuid, currentUser.getIdUser());
    }

    @DeleteMapping("/api/services/{servicePublicUuid}/environment-variables/{variableKey}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEnvironmentVariable(
            @PathVariable UUID servicePublicUuid,
            @PathVariable String variableKey,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        serviceService.deleteEnvironmentVariable(servicePublicUuid, variableKey, currentUser.getIdUser());
    }

    // ---- Domain (N) ----

    @PostMapping("/api/services/{servicePublicUuid}/domains")
    @ResponseStatus(HttpStatus.CREATED)
    public DomainResponseDTO createDomain(
            @PathVariable UUID servicePublicUuid,
            @Valid @RequestBody DomainRequestDTO request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return serviceService.createDomain(servicePublicUuid, request, currentUser.getIdUser());
    }

    @GetMapping("/api/services/{servicePublicUuid}/domains")
    public List<DomainResponseDTO> listDomains(
            @PathVariable UUID servicePublicUuid,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return serviceService.listDomains(servicePublicUuid, currentUser.getIdUser());
    }

    @DeleteMapping("/api/domains/{domainPublicUuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDomain(
            @PathVariable UUID domainPublicUuid,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        serviceService.deleteDomain(domainPublicUuid, currentUser.getIdUser());
    }

    // ---- Deployment (N, registo/histórico) ----

    @PostMapping("/api/services/{servicePublicUuid}/deployments")
    @ResponseStatus(HttpStatus.CREATED)
    public DeploymentResponseDTO createDeployment(
            @PathVariable UUID servicePublicUuid,
            @Valid @RequestBody DeploymentRequestDTO request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return serviceService.createDeployment(servicePublicUuid, request, currentUser.getIdUser());
    }

    @GetMapping("/api/services/{servicePublicUuid}/deployments")
    public List<DeploymentResponseDTO> listDeployments(
            @PathVariable UUID servicePublicUuid,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return serviceService.listDeployments(servicePublicUuid, currentUser.getIdUser());
    }

    @GetMapping("/api/deployments/{publicUuid}")
    public DeploymentResponseDTO getDeployment(
            @PathVariable UUID publicUuid,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return serviceService.getDeployment(publicUuid, currentUser.getIdUser());
    }
}