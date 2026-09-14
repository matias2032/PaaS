package com.dev58.paasbackend.auth.service;

import com.dev58.paasbackend.auth.dto.AuthRequestDTO;
import com.dev58.paasbackend.auth.dto.AuthResponseDTO;
import com.dev58.paasbackend.auth.entity.User;
import com.dev58.paasbackend.auth.exception.InvalidCredentialsException;
import com.dev58.paasbackend.auth.exception.UserAlreadyExistsException;
import com.dev58.paasbackend.auth.exception.UserNotFoundException;
import com.dev58.paasbackend.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dev58.paasbackend.common.security.JwtService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponseDTO register(AuthRequestDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(
                    "Já existe um utilizador com este email");
        }

        if (request.getFirstName() == null || request.getFirstName().isBlank()) {
            throw new IllegalArgumentException("Nome é obrigatório");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        User saved = userRepository.save(user);

        String token = jwtService.generateToken(saved.getEmail(), saved.getPublicUuid());

        return toResponseDTO(saved, token);
    }

    @Transactional(readOnly = true)
    public AuthResponseDTO login(AuthRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException(
                        "Email ou password incorretos"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Email ou password incorretos");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getPublicUuid());

        return toResponseDTO(user, token);
    }

    @Transactional(readOnly = true)
    public AuthResponseDTO getByPublicUuid(UUID publicUuid) {
        User user = userRepository.findByPublicUuid(publicUuid)
                .orElseThrow(() -> new UserNotFoundException("Utilizador não encontrado"));

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