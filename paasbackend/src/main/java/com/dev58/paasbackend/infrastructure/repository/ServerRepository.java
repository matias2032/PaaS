package com.dev58.paasbackend.infrastructure.repository;

import com.dev58.paasbackend.infrastructure.entity.Server;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServerRepository extends JpaRepository<Server, Long> {

    Optional<Server> findByPublicUuid(UUID publicUuid);

    // Lista servidores de uma instância Coolify — usado ao listar/gerir
    // servidores por instância, e pela guarda de "não apagar uma
    // CoolifyInstance com servidores ainda associados" no Service.
    List<Server> findByIdCoolifyInstance(Long idCoolifyInstance);

    // uq_servers_coolify_uuid é composto (id_coolify_instance,
    // coolify_server_uuid) — mesmo UUID pode repetir-se entre
    // instâncias diferentes, por isso o check tem de levar ambos.
    boolean existsByIdCoolifyInstanceAndCoolifyServerUuid(
            Long idCoolifyInstance, String coolifyServerUuid);
}