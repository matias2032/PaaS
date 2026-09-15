package com.dev58.paasbackend.auth.service;

import com.dev58.paasbackend.auth.dto.AuthRequestDTO;
import com.dev58.paasbackend.auth.dto.AuthResponseDTO;
import com.dev58.paasbackend.auth.entity.User;
import com.dev58.paasbackend.auth.exception.InvalidCredentialsException;
import com.dev58.paasbackend.auth.exception.UserAlreadyExistsException;
import com.dev58.paasbackend.auth.exception.UserNotFoundException;
import com.dev58.paasbackend.auth.repository.UserRepository;
import com.dev58.paasbackend.common.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // Driven by application-dev.yml / application-prod.yml — see the
    // TODO comment in application-prod.yml for what's still missing
    // (actually sending the activation email) before this can be
    // safely turned on in production.
    @Value("${app.registration.require-email-verification:false}")
    private boolean requireEmailVerification;

    @Transactional
    public AuthResponseDTO register(AuthRequestDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(
                    "A user with this email already exists");
        }

        if (request.getFirstName() == null || request.getFirstName().isBlank()) {
            throw new IllegalArgumentException("First name is required");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status(requireEmailVerification ? "PENDING_VERIFICATION" : "ACTIVE")
                .build();

        User saved = userRepository.save(user);

        String token = jwtService.generateToken(saved.getEmail(), saved.getPublicUuid());

        return toResponseDTO(saved, token);
    }

    @Transactional(readOnly = true)
    public AuthResponseDTO login(AuthRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException(
                        "Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getPublicUuid());

        return toResponseDTO(user, token);
    }

    @Transactional(readOnly = true)
    public AuthResponseDTO getByPublicUuid(UUID publicUuid) {
        User user = userRepository.findByPublicUuid(publicUuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        return toResponseDTO(user, null);
    }

    private AuthResponseDTO toResponseDTO(User user, String token) {
        return AuthResponseDTO.builder()
                .publicUuid(user.getPublicUuid())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .status(user.getStatus())
                .emailVerifiedAt(user.getEmailVerifiedAt())
                .createdAt(user.getCreatedAt())
                .token(token)
                .build();
    }
}