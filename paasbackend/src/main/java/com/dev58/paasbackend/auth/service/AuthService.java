package com.dev58.paasbackend.auth.service;

import com.dev58.paasbackend.auth.dto.AuthRequestDTO;
import com.dev58.paasbackend.auth.dto.AuthResponseDTO;
import com.dev58.paasbackend.auth.dto.ChangePasswordRequestDTO;
import com.dev58.paasbackend.auth.dto.ForgotPasswordRequestDTO;
import com.dev58.paasbackend.auth.dto.MessageResponseDTO;
import com.dev58.paasbackend.auth.dto.ResetPasswordRequestDTO;
import com.dev58.paasbackend.auth.dto.UpdateProfileRequestDTO;
import com.dev58.paasbackend.auth.entity.PasswordResetToken;
import com.dev58.paasbackend.auth.entity.User;
import com.dev58.paasbackend.auth.exception.InvalidCredentialsException;
import com.dev58.paasbackend.auth.exception.InvalidResetTokenException;
import com.dev58.paasbackend.auth.exception.UserAlreadyExistsException;
import com.dev58.paasbackend.auth.exception.UserNotFoundException;
import com.dev58.paasbackend.auth.repository.PasswordResetTokenRepository;
import com.dev58.paasbackend.auth.repository.UserRepository;
import com.dev58.paasbackend.common.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int RESET_TOKEN_TTL_MINUTES = 30;

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

    @Transactional
    public AuthResponseDTO updateProfile(String email, UpdateProfileRequestDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());

        User saved = userRepository.save(user);

        return toResponseDTO(saved, null);
    }

    @Transactional
    public AuthResponseDTO changePassword(String email, ChangePasswordRequestDTO request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));

        User saved = userRepository.save(user);

        return toResponseDTO(saved, null);
    }

    @Transactional
    public MessageResponseDTO forgotPassword(ForgotPasswordRequestDTO request) {
        // Always return the same generic message regardless of whether
        // the email exists — avoids leaking which emails are registered.
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String rawToken = generateRawToken();
            String tokenHash = hashToken(rawToken);

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(tokenHash)
                    .expiresAt(OffsetDateTime.now().plusMinutes(RESET_TOKEN_TTL_MINUTES))
                    .build();

            passwordResetTokenRepository.save(resetToken);

            // DEV ONLY — no SMTP wired up yet (see handoff section 6).
            // In prod this must become a real email send instead of a log line.
            log.info("Password reset requested for {}. Reset link (dev only): " +
                            "http://localhost:5173/reset-password?token={}",
                    user.getEmail(), rawToken);
        });

        return MessageResponseDTO.builder()
                .message("If that email exists, a password reset link has been sent.")
                .build();
    }

    @Transactional
    public MessageResponseDTO resetPassword(ResetPasswordRequestDTO request) {
        String tokenHash = hashToken(request.getToken());

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidResetTokenException("Invalid or expired reset token"));

        if (resetToken.getUsedAt() != null) {
            throw new InvalidResetTokenException("This reset token has already been used");
        }

        if (resetToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new InvalidResetTokenException("This reset token has expired");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsedAt(OffsetDateTime.now());
        passwordResetTokenRepository.save(resetToken);

        return MessageResponseDTO.builder()
                .message("Password has been reset successfully.")
                .build();
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available on every JVM — this
            // branch is unreachable in practice.
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
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