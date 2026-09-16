package com.dev58.paasbackend.organization.entity;

import com.dev58.paasbackend.auth.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "organization_members",
        schema = "paas_platform",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_organization", "id_user"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_organization_member")
    private Long idOrganizationMember;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_organization", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_user", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_organization_role", nullable = false)
    private OrganizationRole organizationRole;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private OffsetDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) {
            joinedAt = OffsetDateTime.now();
        }
    }
}