package com.dev58.paasbackend.infrastructure.service;

import com.dev58.paasbackend.common.security.CryptoService;
import com.dev58.paasbackend.infrastructure.dto.CoolifyInstanceResponseDTO;
import com.dev58.paasbackend.infrastructure.dto.CreateCoolifyInstanceRequestDTO;
import com.dev58.paasbackend.infrastructure.dto.CreateServerRequestDTO;
import com.dev58.paasbackend.infrastructure.dto.ServerProviderResponseDTO;
import com.dev58.paasbackend.infrastructure.dto.ServerResponseDTO;
import com.dev58.paasbackend.infrastructure.dto.UpdateCoolifyInstanceStatusRequestDTO;
import com.dev58.paasbackend.infrastructure.dto.UpdateServerStatusRequestDTO;
import com.dev58.paasbackend.infrastructure.entity.CoolifyInstance;
import com.dev58.paasbackend.infrastructure.entity.Server;
import com.dev58.paasbackend.infrastructure.entity.ServerProvider;
import com.dev58.paasbackend.infrastructure.exception.CoolifyInstanceNameAlreadyExistsException;
import com.dev58.paasbackend.infrastructure.exception.CoolifyInstanceNotFoundException;
import com.dev58.paasbackend.infrastructure.exception.CoolifyInstanceUrlAlreadyExistsException;
import com.dev58.paasbackend.infrastructure.exception.ServerNotFoundException;
import com.dev58.paasbackend.infrastructure.exception.ServerProviderNotFoundException;
import com.dev58.paasbackend.infrastructure.repository.CoolifyInstanceRepository;
import com.dev58.paasbackend.infrastructure.repository.ServerProviderRepository;
import com.dev58.paasbackend.infrastructure.repository.ServerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InfrastructureService {

    private final CoolifyInstanceRepository coolifyInstanceRepository;
    private final ServerRepository serverRepository;
    private final ServerProviderRepository serverProviderRepository;
    private final CryptoService cryptoService;

    // Espelham exactamente ck_coolify_instances_status e
    // ck_servers_status no schema — validados aqui para devolver um
    // 400 claro (IllegalArgumentException, já mapeado no
    // GlobalExceptionHandler) em vez de deixar a BD rejeitar a
    // transacção inteira com uma mensagem de CHECK constraint crua.
    private static final Set<String> VALID_COOLIFY_INSTANCE_STATUSES =
            Set.of("ACTIVE", "INACTIVE", "MAINTENANCE", "UNAVAILABLE");

    private static final Set<String> VALID_SERVER_STATUSES =
            Set.of("ACTIVE", "MAINTENANCE", "OFFLINE", "FULL", "INACTIVE");

    // ---------------------------------------------------------------
    // CoolifyInstance
    // ---------------------------------------------------------------

    @Transactional
    public CoolifyInstanceResponseDTO createCoolifyInstance(CreateCoolifyInstanceRequestDTO request) {
        if (coolifyInstanceRepository.existsByName(request.getName())) {
            throw new CoolifyInstanceNameAlreadyExistsException(
                    "A Coolify instance with this name already exists");
        }

        if (coolifyInstanceRepository.existsByBaseUrl(request.getBaseUrl())) {
            throw new CoolifyInstanceUrlAlreadyExistsException(
                    "A Coolify instance with this base URL already exists");
        }

        CoolifyInstance instance = CoolifyInstance.builder()
                .name(request.getName())
                .baseUrl(request.getBaseUrl())
                .apiTokenEncrypted(cryptoService.encrypt(request.getApiToken()))
                .build();

        CoolifyInstance saved = coolifyInstanceRepository.save(instance);

        return toCoolifyInstanceResponseDTO(saved);
    }

    @Transactional
    public CoolifyInstanceResponseDTO updateCoolifyInstanceStatus(
            UUID publicUuid, UpdateCoolifyInstanceStatusRequestDTO request) {

        if (!VALID_COOLIFY_INSTANCE_STATUSES.contains(request.getStatus())) {
            throw new IllegalArgumentException("Invalid Coolify instance status: " + request.getStatus());
        }

        CoolifyInstance instance = coolifyInstanceRepository.findByPublicUuid(publicUuid)
                .orElseThrow(() -> new CoolifyInstanceNotFoundException("Coolify instance not found"));

        instance.setStatus(request.getStatus());

        CoolifyInstance saved = coolifyInstanceRepository.save(instance);

        return toCoolifyInstanceResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public CoolifyInstanceResponseDTO getCoolifyInstanceByPublicUuid(UUID publicUuid) {
        CoolifyInstance instance = coolifyInstanceRepository.findByPublicUuid(publicUuid)
                .orElseThrow(() -> new CoolifyInstanceNotFoundException("Coolify instance not found"));

        return toCoolifyInstanceResponseDTO(instance);
    }

    @Transactional(readOnly = true)
    public List<CoolifyInstanceResponseDTO> listCoolifyInstances() {
        return coolifyInstanceRepository.findAll().stream()
                .map(this::toCoolifyInstanceResponseDTO)
                .collect(Collectors.toList());
    }

    // ---------------------------------------------------------------
    // Server
    // ---------------------------------------------------------------

    @Transactional
    public ServerResponseDTO createServer(CreateServerRequestDTO request) {
        CoolifyInstance instance = coolifyInstanceRepository
                .findByPublicUuid(request.getCoolifyInstancePublicUuid())
                .orElseThrow(() -> new CoolifyInstanceNotFoundException("Coolify instance not found"));

        ServerProvider provider = null;
        if (request.getIdServerProvider() != null) {
            provider = serverProviderRepository.findById(request.getIdServerProvider())
                    .orElseThrow(() -> new ServerProviderNotFoundException("Server provider not found"));
        }

        // uq_servers_coolify_uuid é composto (id_coolify_instance,
        // coolify_server_uuid) — o mesmo coolify_server_uuid pode
        // repetir-se entre instâncias diferentes, por isso o check
        // usa sempre os dois campos juntos, nunca só um.
        if (serverRepository.existsByIdCoolifyInstanceAndCoolifyServerUuid(
                instance.getIdCoolifyInstance(), request.getCoolifyServerUuid())) {
            throw new IllegalArgumentException(
                    "A server with this Coolify server UUID already exists on this instance");
        }

        Server server = Server.builder()
                .idCoolifyInstance(instance.getIdCoolifyInstance())
                .idServerProvider(request.getIdServerProvider())
                .name(request.getName())
                .coolifyServerUuid(request.getCoolifyServerUuid())
                .hostname(request.getHostname())
                .publicIp(request.getPublicIp())
                .region(request.getRegion())
                .totalCpu(request.getTotalCpu())
                .totalMemoryMb(request.getTotalMemoryMb())
                .totalStorageMb(request.getTotalStorageMb())
                .build();

        Server saved = serverRepository.save(server);

        return toServerResponseDTO(saved, instance, provider);
    }

    @Transactional
    public ServerResponseDTO updateServerStatus(UUID publicUuid, UpdateServerStatusRequestDTO request) {
        if (!VALID_SERVER_STATUSES.contains(request.getStatus())) {
            throw new IllegalArgumentException("Invalid server status: " + request.getStatus());
        }

        Server server = serverRepository.findByPublicUuid(publicUuid)
                .orElseThrow(() -> new ServerNotFoundException("Server not found"));

        server.setStatus(request.getStatus());

        Server saved = serverRepository.save(server);

        return toServerResponseDTO(saved, null, null);
    }

    @Transactional(readOnly = true)
    public ServerResponseDTO getServerByPublicUuid(UUID publicUuid) {
        Server server = serverRepository.findByPublicUuid(publicUuid)
                .orElseThrow(() -> new ServerNotFoundException("Server not found"));

        return toServerResponseDTO(server, null, null);
    }

    @Transactional(readOnly = true)
    public List<ServerResponseDTO> listServers() {
        // N+1 aceite deliberadamente aqui — toServerResponseDTO faz um
        // findById por servidor para preencher coolifyInstanceName/
        // serverProviderName. Fica para otimizar com um JOIN/DTO
        // projection só se a lista de servidores crescer o suficiente
        // para importar; não é o caso previsto nesta sub-tarefa.
        return serverRepository.findAll().stream()
                .map(server -> toServerResponseDTO(server, null, null))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ServerResponseDTO> listServersByCoolifyInstance(UUID coolifyInstancePublicUuid) {
        CoolifyInstance instance = coolifyInstanceRepository.findByPublicUuid(coolifyInstancePublicUuid)
                .orElseThrow(() -> new CoolifyInstanceNotFoundException("Coolify instance not found"));

        return serverRepository.findByIdCoolifyInstance(instance.getIdCoolifyInstance()).stream()
                .map(server -> toServerResponseDTO(server, instance, null))
                .collect(Collectors.toList());
    }

    // ---------------------------------------------------------------
    // ServerProvider (read-only — ver ServerProviderResponseDTO)
    // ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ServerProviderResponseDTO> listServerProviders() {
        return serverProviderRepository.findAll().stream()
                .map(provider -> ServerProviderResponseDTO.builder()
                        .idServerProvider(provider.getIdServerProvider())
                        .name(provider.getName())
                        .code(provider.getCode())
                        .build())
                .collect(Collectors.toList());
    }

    // ---------------------------------------------------------------
    // Mapping
    // ---------------------------------------------------------------

    private CoolifyInstanceResponseDTO toCoolifyInstanceResponseDTO(CoolifyInstance instance) {
        return CoolifyInstanceResponseDTO.builder()
                .publicUuid(instance.getPublicUuid())
                .name(instance.getName())
                .baseUrl(instance.getBaseUrl())
                .status(instance.getStatus())
                .createdAt(instance.getCreatedAt())
                .updatedAt(instance.getUpdatedAt())
                .build();
    }

    /**
     * knownInstance/knownProvider evitam uma query redundante quando o
     * chamador já tem a entidade em mãos (createServer,
     * listServersByCoolifyInstance) — passa null para os casos em que
     * ainda não foi carregada (updateServerStatus, getServerByPublicUuid,
     * listServers), e este método resolve-a com findById().
     */
    private ServerResponseDTO toServerResponseDTO(Server server, CoolifyInstance knownInstance, ServerProvider knownProvider) {
        CoolifyInstance instance = knownInstance != null
                ? knownInstance
                : coolifyInstanceRepository.findById(server.getIdCoolifyInstance())
                        .orElseThrow(() -> new CoolifyInstanceNotFoundException("Coolify instance not found"));

        ServerProvider provider = null;
        if (server.getIdServerProvider() != null) {
            provider = knownProvider != null
                    ? knownProvider
                    : serverProviderRepository.findById(server.getIdServerProvider())
                            .orElseThrow(() -> new ServerProviderNotFoundException("Server provider not found"));
        }

        return ServerResponseDTO.builder()
                .publicUuid(server.getPublicUuid())
                .coolifyInstancePublicUuid(instance.getPublicUuid())
                .coolifyInstanceName(instance.getName())
                .idServerProvider(server.getIdServerProvider())
                .serverProviderName(provider != null ? provider.getName() : null)
                .name(server.getName())
                .coolifyServerUuid(server.getCoolifyServerUuid())
                .hostname(server.getHostname())
                .publicIp(server.getPublicIp())
                .region(server.getRegion())
                .totalCpu(server.getTotalCpu())
                .totalMemoryMb(server.getTotalMemoryMb())
                .totalStorageMb(server.getTotalStorageMb())
                .status(server.getStatus())
                .createdAt(server.getCreatedAt())
                .updatedAt(server.getUpdatedAt())
                .build();
    }
}