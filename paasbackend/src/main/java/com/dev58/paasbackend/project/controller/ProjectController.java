package com.dev58.paasbackend.project.controller;

import com.dev58.paasbackend.common.security.AuthenticatedUser;
import com.dev58.paasbackend.project.dto.*;
import com.dev58.paasbackend.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    // ---- Project ----

    @PostMapping("/api/organizations/{orgPublicUuid}/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponseDTO createProject(
            @PathVariable UUID orgPublicUuid,
            @Valid @RequestBody ProjectRequestDTO request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return projectService.createProject(orgPublicUuid, request, currentUser.getIdUser());
    }

    @GetMapping("/api/organizations/{orgPublicUuid}/projects")
    public List<ProjectResponseDTO> listProjects(
            @PathVariable UUID orgPublicUuid,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return projectService.listProjects(orgPublicUuid, currentUser.getIdUser());
    }

    @GetMapping("/api/projects/{publicUuid}")
    public ProjectResponseDTO getProject(
            @PathVariable UUID publicUuid,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return projectService.getProject(publicUuid, currentUser.getIdUser());
    }

    @PatchMapping("/api/projects/{publicUuid}")
    public ProjectResponseDTO updateProject(
            @PathVariable UUID publicUuid,
            @Valid @RequestBody ProjectRequestDTO request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return projectService.updateProject(publicUuid, request, currentUser.getIdUser());
    }

    @PostMapping("/api/projects/{publicUuid}/archive")
    public ProjectResponseDTO archiveProject(
            @PathVariable UUID publicUuid,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return projectService.archiveProject(publicUuid, currentUser.getIdUser());
    }

    @PostMapping("/api/projects/{publicUuid}/reactivate")
    public ProjectResponseDTO reactivateProject(
            @PathVariable UUID publicUuid,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return projectService.reactivateProject(publicUuid, currentUser.getIdUser());
    }

    // ---- GitProvider ----

    @GetMapping("/api/git-providers")
    public List<GitProviderResponseDTO> listGitProviders() {
        return projectService.listGitProviders();
    }

    // ---- GitConnection ----

    @PostMapping("/api/organizations/{orgPublicUuid}/git-connections")
    @ResponseStatus(HttpStatus.CREATED)
    public GitConnectionResponseDTO createGitConnection(
            @PathVariable UUID orgPublicUuid,
            @Valid @RequestBody GitConnectionRequestDTO request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return projectService.createGitConnection(orgPublicUuid, request, currentUser.getIdUser());
    }

    @GetMapping("/api/organizations/{orgPublicUuid}/git-connections")
    public List<GitConnectionResponseDTO> listGitConnections(
            @PathVariable UUID orgPublicUuid,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return projectService.listGitConnections(orgPublicUuid, currentUser.getIdUser());
    }

    @PostMapping("/api/git-connections/{publicUuid}/revoke")
    public GitConnectionResponseDTO revokeGitConnection(
            @PathVariable UUID publicUuid,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return projectService.revokeGitConnection(publicUuid, currentUser.getIdUser());
    }
}