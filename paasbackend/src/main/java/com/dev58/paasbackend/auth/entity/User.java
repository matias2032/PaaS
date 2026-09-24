package com.dev58.paasbackend.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;

import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users", schema = "paas_platform")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_user")
    private Long idUser;

    @Column(name = "public_uuid", nullable = false, updatable = false)
    private UUID publicUuid;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

@ColumnTransformer(write = "?::paas_platform.citext")
@Column(name = "email", nullable = false, unique = true, columnDefinition = "citext")
private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "platform_role", nullable = false, length = 20)
    private String platformRole;

    // true = utilizador ainda está a usar a password temporária gerada
    // pelo owner (ver AuthService.DEFAULT_STAFF_PASSWORD); nunca true
    // para clientes (auto-registo já entra com false).
    @Column(name = "first_password", nullable = false)
    private boolean firstPassword;

    @Column(name = "email_verified_at")
    private OffsetDateTime emailVerifiedAt;

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
            status = "PENDING_VERIFICATION";
        }
        // Every user starts as CUSTOMER. Promotion to SUPPORT/
        // PLATFORM_ADMIN/PLATFORM_OWNER is a deliberate manual action
        // (createStaffUser/updatePlatformRole, or a direct UPDATE) —
        // never set here.
        if (platformRole == null) {
            platformRole = "CUSTOMER";
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