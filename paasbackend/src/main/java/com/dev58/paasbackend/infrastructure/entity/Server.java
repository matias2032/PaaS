package com.dev58.paasbackend.infrastructure.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

// A physical/virtual machine registered under a CoolifyInstance.
// id_server_provider is nullable (a server can be added before its
// provider is known/catalogued), id_coolify_instance is not (a
// server always belongs to exactly one Coolify control plane).
@Entity
@Table(name = "servers", schema = "paas_platform")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Server {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_server")
    private Long idServer;

    @Column(name = "public_uuid", nullable = false, updatable = false)
    private UUID publicUuid;

    @Column(name = "id_coolify_instance", nullable = false)
    private Long idCoolifyInstance;

    @Column(name = "id_server_provider")
    private Short idServerProvider;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "coolify_server_uuid", nullable = false, length = 255)
    private String coolifyServerUuid;

    @Column(name = "hostname", length = 255)
    private String hostname;

    // Stored as native Postgres inet. Same cast-on-write pattern as
    // User.email (citext): Java sees a plain String, Postgres enforces
    // the inet type. No read-side transform needed — inet reads back
    // as text through the JDBC driver.
    @ColumnTransformer(write = "?::inet")
    @Column(name = "public_ip", columnDefinition = "inet")
    private String publicIp;

    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "total_cpu", nullable = false, precision = 8, scale = 3)
    private BigDecimal totalCpu;

    @Column(name = "total_memory_mb", nullable = false)
    private Long totalMemoryMb;

    @Column(name = "total_storage_mb", nullable = false)
    private Long totalStorageMb;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (publicUuid == null) {
            publicUuid = UUID.randomUUID();
        }
        if (status == null) {
            status = "ACTIVE";
        }
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}