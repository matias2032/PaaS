package com.dev58.paasbackend.auth.controller;

import com.dev58.paasbackend.auth.dto.AuthRequestDTO;
import com.dev58.paasbackend.auth.dto.AuthResponseDTO;
import com.dev58.paasbackend.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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
}