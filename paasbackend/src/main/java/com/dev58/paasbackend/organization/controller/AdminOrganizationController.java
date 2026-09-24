package com.dev58.paasbackend.organization.controller;

import com.dev58.paasbackend.organization.dto.OrganizationResponseDTO;
import com.dev58.paasbackend.organization.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/organizations")
@RequiredArgsConstructor
public class AdminOrganizationController {

    private final OrganizationService organizationService;

    @GetMapping
    @PreAuthorize("hasRole('SUPPORT')")
    public Page<OrganizationResponseDTO> listAll(Pageable pageable) {
        return organizationService.listAllOrganizations(pageable);
    }

    @GetMapping("/{publicUuid}")
    @PreAuthorize("hasRole('SUPPORT')")
    public OrganizationResponseDTO getAsAdmin(@PathVariable UUID publicUuid) {
        return organizationService.getOrganizationAsAdmin(publicUuid);
    }
}