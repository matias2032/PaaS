package com.dev58.paasbackend.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Standard error body returned by GlobalExceptionHandler for every
 * mapped exception across the API. Keeps error responses consistent
 * regardless of which module/exception triggered them.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponseDTO {

    private OffsetDateTime timestamp;

    private int status;

    private String error;

    private String message;

    private String path;
}