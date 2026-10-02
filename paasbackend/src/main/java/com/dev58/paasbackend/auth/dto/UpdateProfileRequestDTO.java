package com.dev58.paasbackend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequestDTO {

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must be at most 100 characters long")
    private String firstName;

    @Size(max = 100, message = "Last name must be at most 100 characters long")
    private String lastName;

    @Size(max = 30, message = "Phone must be at most 30 characters long")
    private String phone;
}