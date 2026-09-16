package com.dev58.paasbackend.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Used for both create and update. On update, slug is typically not
// changed via this DTO (decide in Service whether slug is mutable).
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationRequestDTO {

    @NotBlank
    @Size(max = 150)
    private String name;

    @NotBlank
    @Size(max = 150)
    private String slug;
}