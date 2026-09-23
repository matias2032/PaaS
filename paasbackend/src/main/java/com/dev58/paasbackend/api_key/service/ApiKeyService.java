package com.dev58.paasbackend.api_key.service;

import com.dev58.paasbackend.api_key.dto.AdminRevokeApiKeyRequestDTO;
import com.dev58.paasbackend.api_key.dto.ApiKeyCreatedResponseDTO;
import com.dev58.paasbackend.api_key.dto.ApiKeyRequestDTO;
import com.dev58.paasbackend.api_key.dto.ApiKeyResponseDTO;
import com.dev58.paasbackend.api_key.entity.ApiKey;
import com.dev58.paasbackend.api_key.exception.ApiKeyNameAlreadyExistsException;
import com.dev58.paasbackend.api_key.exception.ApiKeyNotFoundException;
import com.dev58.paasbackend.api_key.repository.ApiKeyRepository;
import com.dev58.paasbackend.auth.entity.User;
import com.dev58.paasbackend.auth.exception.UserNotFoundException;
import com.dev58.paasbackend.auth.repository.UserRepository;
import com.dev58.paasbackend.organization.entity.Organization;
import com.dev58.paasbackend.organization.entity.OrganizationMember;
import com.dev58.paasbackend.organization.exception.OrganizationInactiveException;
import com.dev58.paasbackend.organization.exception.OrganizationNotFoundException;
import com.dev58.paasbackend.organization.exception.PermissionDeniedException;
import com.dev58.paasbackend.organization.repository.OrganizationMemberRepository;
import com.dev58.paasbackend.organization.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private static final String ROLE_OWNER = "OWNER";
    private static final String KEY_PREFIX_MARKER = "sk_live_";
    private static final int PREFIX_VISIBLE_CHARS = 8;

    private final ApiKeyRepository apiKeyRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final UserRepository userRepository;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // ==================== Client-facing ====================

    @Transactional
    public ApiKeyCreatedResponseDTO createApiKey(UUID orgPublicUuid, ApiKeyRequestDTO request, Long currentUserId) {
        Organization organization = findOrganizationOrThrow(orgPublicUuid);
        requireOwner(organization, currentUserId);
        requireActiveOrganization(organization);

        if (apiKeyRepository.existsByOrganization_IdOrganizationAndName(
                organization.getIdOrganization(), request.getName())) {
            throw new ApiKeyNameAlreadyExistsException(
                    "An API key with this name already exists in this organization: " + request.getName());
        }

        User creator = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + currentUserId));

        String rawKey = generateRawKey();
        String keyPrefix = extractKeyPrefix(rawKey);
        String keyHash = hashKey(rawKey);

        ApiKey apiKey = ApiKey.builder()
                .organization(organization)
                .createdByUser(creator)
                .name(request.getName())
                .keyHash(keyHash)
                .keyPrefix(keyPrefix)
                .expiresAt(request.getExpiresAt())
                .build();
        apiKey = apiKeyRepository.save(apiKey);

        // Única resposta em toda a API onde rawKey aparece — nunca mais
        // recuperável depois deste ponto (só key_hash fica persistido).
        return toCreatedResponseDTO(apiKey, rawKey);
    }

    public List<ApiKeyResponseDTO> listApiKeys(UUID orgPublicUuid, Long currentUserId) {
        Organization organization = findOrganizationOrThrow(orgPublicUuid);
        requireMembership(organization, currentUserId);

        return apiKeyRepository.findByOrganization_IdOrganization(organization.getIdOrganization()).stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional
    public ApiKeyResponseDTO revokeApiKey(UUID orgPublicUuid, UUID keyPublicUuid, Long currentUserId) {
        Organization organization = findOrganizationOrThrow(orgPublicUuid);
        requireOwner(organization, currentUserId);
        requireActiveOrganization(organization);

        ApiKey apiKey = apiKeyRepository
                .findByOrganization_IdOrganizationAndPublicUuid(organization.getIdOrganization(), keyPublicUuid)
                .orElseThrow(() -> new ApiKeyNotFoundException("API key not found: " + keyPublicUuid));

        if ("REVOKED".equals(apiKey.getStatus())) {
            throw new IllegalArgumentException("API key is already revoked");
        }

        // Auto-revogação — sem reason, ao contrário do caminho admin
        // (ver AdminRevokeApiKeyRequestDTO / revokeApiKeyAsAdmin).
        apiKey.setStatus("REVOKED");
        apiKey = apiKeyRepository.save(apiKey);

        return toResponseDTO(apiKey);
    }

    // ==================== Admin (platform-side) ====================

    // Sem requireOwner/requireMembership — autorização é
    // @PreAuthorize("hasRole('PLATFORM_ADMIN')") no controller, mesmo
    // padrão de OrganizationService.suspendOrganization().
    @Transactional
    public ApiKeyResponseDTO revokeApiKeyAsAdmin(UUID keyPublicUuid, AdminRevokeApiKeyRequestDTO request) {
        ApiKey apiKey = apiKeyRepository.findByPublicUuid(keyPublicUuid)
                .orElseThrow(() -> new ApiKeyNotFoundException("API key not found: " + keyPublicUuid));

        if ("REVOKED".equals(apiKey.getStatus())) {
            throw new IllegalArgumentException("API key is already revoked");
        }

        apiKey.setStatus("REVOKED");
        apiKey.setRevocationReason(request.getReason());
        apiKey = apiKeyRepository.save(apiKey);

        return toResponseDTO(apiKey);
    }

    // ==================== Key generation ====================

    private String generateRawKey() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return KEY_PREFIX_MARKER + secret;
    }

    // Prefixo visível guardado em claro (key_prefix) — reconhecimento em
    // listagens sem nunca expor o segredo completo outra vez.
    private String extractKeyPrefix(String rawKey) {
        String secretPart = rawKey.substring(KEY_PREFIX_MARKER.length());
        int end = Math.min(PREFIX_VISIBLE_CHARS, secretPart.length());
        return KEY_PREFIX_MARKER + secretPart.substring(0, end);
    }

    // Hash da chave completa, não só do segredo — mesmo padrão de
    // AuthService.hashToken() para os password reset tokens.
    private String hashKey(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    // ==================== Internal helpers ====================

    private Organization findOrganizationOrThrow(UUID publicUuid) {
        return organizationRepository.findByPublicUuid(publicUuid)
                .orElseThrow(() -> new OrganizationNotFoundException("Organization not found: " + publicUuid));
    }

    private OrganizationMember requireMembership(Organization organization, Long currentUserId) {
        return organizationMemberRepository
                .findByOrganization_IdOrganizationAndUser_IdUser(organization.getIdOrganization(), currentUserId)
                .orElseThrow(() -> new PermissionDeniedException("User is not a member of this organization"));
    }

    private void requireOwner(Organization organization, Long currentUserId) {
        OrganizationMember membership = requireMembership(organization, currentUserId);
        if (!ROLE_OWNER.equals(membership.getOrganizationRole().getCode())) {
            throw new PermissionDeniedException("Only OWNER can manage API keys for this organization");
        }
    }

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

    private ApiKeyCreatedResponseDTO toCreatedResponseDTO(ApiKey apiKey, String rawKey) {
        return ApiKeyCreatedResponseDTO.builder()
                .publicUuid(apiKey.getPublicUuid())
                .name(apiKey.getName())
                .rawKey(rawKey)
                .keyPrefix(apiKey.getKeyPrefix())
                .status(apiKey.getStatus())
                .expiresAt(apiKey.getExpiresAt())
                .createdAt(apiKey.getCreatedAt())
                .build();
    }

    private ApiKeyResponseDTO toResponseDTO(ApiKey apiKey) {
        return ApiKeyResponseDTO.builder()
                .publicUuid(apiKey.getPublicUuid())
                .organizationPublicUuid(apiKey.getOrganization().getPublicUuid())
                .name(apiKey.getName())
                .keyPrefix(apiKey.getKeyPrefix())
                .status(apiKey.getStatus())
                .lastUsedAt(apiKey.getLastUsedAt())
                .expiresAt(apiKey.getExpiresAt())
                .createdAt(apiKey.getCreatedAt())
                .build();
    }
}