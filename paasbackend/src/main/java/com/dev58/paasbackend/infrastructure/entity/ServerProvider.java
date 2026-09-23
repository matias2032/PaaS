package com.dev58.paasbackend.infrastructure.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Catalog table, same shape as GitProvider/ServiceType — no status,
// no timestamps, populated manually (AWS, Hetzner, DigitalOcean, etc.)
// rather than through the app. id_server_provider is nullable on
// Server (a server can exist without a known provider), so this
// entity has no back-reference collection — no need to load every
// Server for a provider just to render a dropdown.
@Entity
@Table(name = "server_providers", schema = "paas_platform")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServerProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_server_provider")
    private Short idServerProvider;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", nullable = false, length = 50)
    private String code;
}