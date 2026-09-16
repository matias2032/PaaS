package com.dev58.paasbackend.organization.controller;

import com.dev58.paasbackend.common.security.AuthenticatedUser;
import com.dev58.paasbackend.organization.dto.*;
import com.dev58.paasbackend.organization.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationResponseDTO create(
            @Valid @RequestBody OrganizationRequestDTO request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return organizationService.createOrganization(request, currentUser.getIdUser());
    }

    @GetMapping("/{publicUuid}")
    public OrganizationResponseDTO get(
            @PathVariable UUID publicUuid,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return organizationService.getOrganization(publicUuid, currentUser.getIdUser());
    }

    @PutMapping("/{publicUuid}")
    public OrganizationResponseDTO update(
            @PathVariable UUID publicUuid,
            @Valid @RequestBody OrganizationRequestDTO request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return organizationService.updateOrganization(publicUuid, request, currentUser.getIdUser());
    }

        @DeleteMapping("/{publicUuid}")
    public OrganizationResponseDTO deactivate(
            @PathVariable UUID publicUuid,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return organizationService.deactivateOrganization(publicUuid, currentUser.getIdUser());
    }

    @GetMapping
    public List<OrganizationResponseDTO> listMine(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return organizationService.listOrganizationsForCurrentUser(currentUser.getIdUser());
    }

    @GetMapping("/roles")
    public List<OrganizationRoleResponseDTO> listRoles() {
        return organizationService.listRoles();
    }

    @PostMapping("/{publicUuid}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationMemberResponseDTO addMember(
            @PathVariable UUID publicUuid,
            @Valid @RequestBody OrganizationMemberRequestDTO request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return organizationService.addMember(publicUuid, request, currentUser.getIdUser());
    }

    @DeleteMapping("/{publicUuid}/members/{userPublicUuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @PathVariable UUID publicUuid,
            @PathVariable UUID userPublicUuid,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        organizationService.removeMember(publicUuid, userPublicUuid, currentUser.getIdUser());
    }

        @PatchMapping("/{publicUuid}/members/{userPublicUuid}")
    public OrganizationMemberResponseDTO changeMemberRole(
            @PathVariable UUID publicUuid,
            @PathVariable UUID userPublicUuid,
            @Valid @RequestBody OrganizationMemberRequestDTO request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return organizationService.changeMemberRole(publicUuid, userPublicUuid, request, currentUser.getIdUser());
    }

    @GetMapping("/{publicUuid}/members")
    public List<OrganizationMemberResponseDTO> listMembers(
            @PathVariable UUID publicUuid,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return organizationService.listMembers(publicUuid, currentUser.getIdUser());
    }
}