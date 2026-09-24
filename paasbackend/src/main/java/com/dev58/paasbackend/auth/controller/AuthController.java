package com.dev58.paasbackend.auth.controller;

import com.dev58.paasbackend.auth.dto.AuthRequestDTO;
import com.dev58.paasbackend.auth.dto.AuthResponseDTO;
import com.dev58.paasbackend.auth.dto.UpdateProfileRequestDTO;
import com.dev58.paasbackend.auth.dto.UpdateUserStatusRequestDTO;
import com.dev58.paasbackend.auth.dto.ChangePasswordRequestDTO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import com.dev58.paasbackend.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.dev58.paasbackend.auth.dto.ForgotPasswordRequestDTO;
import com.dev58.paasbackend.auth.dto.MessageResponseDTO;
import com.dev58.paasbackend.auth.dto.ResetPasswordRequestDTO;
import com.dev58.paasbackend.auth.dto.CreateStaffUserRequestDTO;
import com.dev58.paasbackend.auth.dto.UpdatePlatformRoleRequestDTO;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody AuthRequestDTO request) {
        AuthResponseDTO response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO request) {
        AuthResponseDTO response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{publicUuid}")
    public ResponseEntity<AuthResponseDTO> getByPublicUuid(@PathVariable UUID publicUuid) {
        AuthResponseDTO response = authService.getByPublicUuid(publicUuid);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/me")
    public ResponseEntity<AuthResponseDTO> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequestDTO request
    ) {
        AuthResponseDTO response = authService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/password")
    public ResponseEntity<AuthResponseDTO> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequestDTO request
    ) {
        AuthResponseDTO response = authService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponseDTO> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDTO request
    ) {
        MessageResponseDTO response = authService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponseDTO> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDTO request
    ) {
        MessageResponseDTO response = authService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

        @PostMapping("/staff")
    @PreAuthorize("hasRole('PLATFORM_OWNER')")
    public ResponseEntity<AuthResponseDTO> createStaffUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateStaffUserRequestDTO request
    ) {
        AuthResponseDTO response = authService.createStaffUser(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/staff")
    @PreAuthorize("hasRole('PLATFORM_OWNER')")
    public ResponseEntity<List<AuthResponseDTO>> listStaffUsers(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<AuthResponseDTO> response = authService.listStaffUsers(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/staff/{publicUuid}/active")
    @PreAuthorize("hasRole('PLATFORM_OWNER')")
    public ResponseEntity<AuthResponseDTO> updateUserActiveStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID publicUuid,
            @Valid @RequestBody UpdateUserStatusRequestDTO request
    ) {
        AuthResponseDTO response = authService.updateUserActiveStatus(
                userDetails.getUsername(), publicUuid, request.getActive());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{publicUuid}/platform-role")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<AuthResponseDTO> updatePlatformRole(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID publicUuid,
            @Valid @RequestBody UpdatePlatformRoleRequestDTO request
    ) {
        AuthResponseDTO response = authService.updatePlatformRole(
                userDetails.getUsername(), publicUuid, request);
        return ResponseEntity.ok(response);
    }
}