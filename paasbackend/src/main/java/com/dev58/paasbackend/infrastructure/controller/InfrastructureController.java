package com.dev58.paasbackend.infrastructure.controller;

import com.dev58.paasbackend.infrastructure.dto.CoolifyInstanceResponseDTO;
import com.dev58.paasbackend.infrastructure.dto.CreateCoolifyInstanceRequestDTO;
import com.dev58.paasbackend.infrastructure.dto.CreateServerRequestDTO;
import com.dev58.paasbackend.infrastructure.dto.ServerProviderResponseDTO;
import com.dev58.paasbackend.infrastructure.dto.ServerResponseDTO;
import com.dev58.paasbackend.infrastructure.dto.UpdateCoolifyInstanceStatusRequestDTO;
import com.dev58.paasbackend.infrastructure.dto.UpdateServerStatusRequestDTO;
import com.dev58.paasbackend.infrastructure.service.InfrastructureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Todo este controller é admin-only — não há organização nenhuma a
// verificar aqui (ver o handoff platform_role, secção 1: é exactamente
// para módulos como este que platform_role existe). PLATFORM_ADMIN é
// o mínimo em cada endpoint, leitura incluída — não há caso de uso
// previsto para SUPPORT ver infraestrutura crua por agora.
@RestController
@RequestMapping("/api/infrastructure")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class InfrastructureController {

    private final InfrastructureService infrastructureService;

    // -----------------------------------------------------------
    // CoolifyInstance
    // -----------------------------------------------------------

    @PostMapping("/coolify-instances")
    public ResponseEntity<CoolifyInstanceResponseDTO> createCoolifyInstance(
            @Valid @RequestBody CreateCoolifyInstanceRequestDTO request
    ) {
        CoolifyInstanceResponseDTO response = infrastructureService.createCoolifyInstance(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/coolify-instances")
    public ResponseEntity<List<CoolifyInstanceResponseDTO>> listCoolifyInstances() {
        return ResponseEntity.ok(infrastructureService.listCoolifyInstances());
    }

    @GetMapping("/coolify-instances/{publicUuid}")
    public ResponseEntity<CoolifyInstanceResponseDTO> getCoolifyInstance(
            @PathVariable UUID publicUuid
    ) {
        return ResponseEntity.ok(infrastructureService.getCoolifyInstanceByPublicUuid(publicUuid));
    }

    @PatchMapping("/coolify-instances/{publicUuid}/status")
    public ResponseEntity<CoolifyInstanceResponseDTO> updateCoolifyInstanceStatus(
            @PathVariable UUID publicUuid,
            @Valid @RequestBody UpdateCoolifyInstanceStatusRequestDTO request
    ) {
        CoolifyInstanceResponseDTO response =
                infrastructureService.updateCoolifyInstanceStatus(publicUuid, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/coolify-instances/{publicUuid}/servers")
    public ResponseEntity<List<ServerResponseDTO>> listServersByCoolifyInstance(
            @PathVariable UUID publicUuid
    ) {
        return ResponseEntity.ok(infrastructureService.listServersByCoolifyInstance(publicUuid));
    }

    // -----------------------------------------------------------
    // Server
    // -----------------------------------------------------------

    @PostMapping("/servers")
    public ResponseEntity<ServerResponseDTO> createServer(
            @Valid @RequestBody CreateServerRequestDTO request
    ) {
        ServerResponseDTO response = infrastructureService.createServer(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/servers")
    public ResponseEntity<List<ServerResponseDTO>> listServers() {
        return ResponseEntity.ok(infrastructureService.listServers());
    }

    @GetMapping("/servers/{publicUuid}")
    public ResponseEntity<ServerResponseDTO> getServer(
            @PathVariable UUID publicUuid
    ) {
        return ResponseEntity.ok(infrastructureService.getServerByPublicUuid(publicUuid));
    }

    @PatchMapping("/servers/{publicUuid}/status")
    public ResponseEntity<ServerResponseDTO> updateServerStatus(
            @PathVariable UUID publicUuid,
            @Valid @RequestBody UpdateServerStatusRequestDTO request
    ) {
        ServerResponseDTO response = infrastructureService.updateServerStatus(publicUuid, request);
        return ResponseEntity.ok(response);
    }

    // -----------------------------------------------------------
    // ServerProvider (catálogo, só leitura — ver ServerProviderResponseDTO)
    // -----------------------------------------------------------

    @GetMapping("/server-providers")
    public ResponseEntity<List<ServerProviderResponseDTO>> listServerProviders() {
        return ResponseEntity.ok(infrastructureService.listServerProviders());
    }
}