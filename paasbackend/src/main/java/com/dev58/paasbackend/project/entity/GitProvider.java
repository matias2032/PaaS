package com.dev58.paasbackend.project.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "git_providers", schema = "paas_platform")
@Getter
@Setter
@NoArgsConstructor
public class GitProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_git_provider")
    private Short idGitProvider;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 80)
    private String name;
}