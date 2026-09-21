package com.dev58.paasbackend.project.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class GitProviderResponseDTO {

    private Short idGitProvider;
    private String code;
    private String name;
}