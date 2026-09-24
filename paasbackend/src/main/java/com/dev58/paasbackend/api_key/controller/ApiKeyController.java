package com.dev58.paasbackend.api_key.controller;

import com.dev58.paasbackend.api_key.dto.AdminRevokeApiKeyRequestDTO;
import com.dev58.paasbackend.api_key.dto.ApiKeyCreatedResponseDTO;
import com.dev58.paasbackend.api_key.dto.ApiKeyRequestDTO;
import com.dev58.paasbackend.api_key.dto.ApiKeyResponseDTO;
import com.dev58.paasbackend.api_key.service.ApiKeyService;
import com.dev58.paasbackend.common.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    // ==================== Client-facing ====================
    // Sem @PreAuthorize com hasRole aqui — a autorização (OWNER da
    // organização) é feita dentro do ApiKeyService via requireOwner()/
    // requireMembership(), tal como acontece em BillingController para
    // subscribe(). Só exige utilizador autenticado.
    //
    // AuthenticatedUser (não UserDetails puro) porque já expõe
    // getIdUser() directamente — ApiKeyService pede Long currentUserId,
    // não um email a resolver, ao contrário dos métodos mais antigos
    // do AuthController (updateProfile/changePassword) que ainda usam
    // userDetails.getUsername() + lookup.

    @PostMapping("/api/organizations/{orgPublicUuid}/api-keys")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiKeyCreatedResponseDTO> createApiKey(
            @PathVariable UUID orgPublicUuid,
            @Valid @RequestBody ApiKeyRequestDTO request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        ApiKeyCreatedResponseDTO response = apiKeyService.createApiKey(
                orgPublicUuid, request, currentUser.getIdUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/organizations/{orgPublicUuid}/api-keys")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ApiKeyResponseDTO>> listApiKeys(
            @PathVariable UUID orgPublicUuid,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        List<ApiKeyResponseDTO> response = apiKeyService.listApiKeys(
                orgPublicUuid, currentUser.getIdUser());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/api/organizations/{orgPublicUuid}/api-keys/{keyPublicUuid}/revoke")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiKeyResponseDTO> revokeApiKey(
            @PathVariable UUID orgPublicUuid,
            @PathVariable UUID keyPublicUuid,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        ApiKeyResponseDTO response = apiKeyService.revokeApiKey(
                orgPublicUuid, keyPublicUuid, currentUser.getIdUser());
        return ResponseEntity.ok(response);
    }

    // ==================== Admin (platform-side) ====================
    // Revogação de qualquer api_key, em resposta a incidente de
    // segurança — mesmo padrão de InfrastructureController.

    @PatchMapping("/api/admin/api-keys/{publicUuid}/revoke")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<ApiKeyResponseDTO> revokeApiKeyAsAdmin(
            @PathVariable UUID publicUuid,
            @Valid @RequestBody AdminRevokeApiKeyRequestDTO request) {
        ApiKeyResponseDTO response = apiKeyService.revokeApiKeyAsAdmin(publicUuid, request);
        return ResponseEntity.ok(response);
    }

    // Busca cross-organização (A.4) — mesmo padrão de
    // revokeApiKeyAsAdmin acima: sem verificação de membership,
    // autorização inteiramente via @PreAuthorize.
    @GetMapping("/api/admin/organizations/{orgPublicUuid}/api-keys")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<List<ApiKeyResponseDTO>> listApiKeysByOrganizationAsAdmin(
            @PathVariable UUID orgPublicUuid) {
        List<ApiKeyResponseDTO> response = apiKeyService.listApiKeysByOrganizationAsAdmin(orgPublicUuid);
        return ResponseEntity.ok(response);
    }
}