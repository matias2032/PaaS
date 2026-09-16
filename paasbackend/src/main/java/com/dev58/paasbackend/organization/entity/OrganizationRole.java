package com.dev58.paasbackend.organization.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "organization_roles", schema = "paas_platform")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_organization_role")
    private Short idOrganizationRole;

    @Column(name = "code", nullable = false, length = 30, unique = true)
    private String code;

    @Column(name = "name", nullable = false, length = 60)
    private String name;
}