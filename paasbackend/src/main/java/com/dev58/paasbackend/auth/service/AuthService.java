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
import com.dev58.paasbackend.auth.dto.CreateStaffUserRequestDTO;
import com.dev58.paasbackend.auth.dto.UpdatePlatformRoleRequestDTO;
import com.dev58.paasbackend.auth.exception.AccountDeactivatedException;
import com.dev58.paasbackend.auth.exception.InsufficientPlatformRoleException;
import com.dev58.paasbackend.auth.exception.LastPlatformOwnerException;
import java.util.List;

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

        // Password temporária fixa atribuída a todo o staff criado por um
    // owner (regra 1) — nunca vem do request, nunca é devolvida pela
    // API; a UI mostra este valor estaticamente após a criação.
    private static final String DEFAULT_STAFF_PASSWORD = "12345678";

    // Driven by application-dev.yml / application-prod.yml — see the
    // TODO comment in application-prod.yml for what's still missing
    // (actually sending the activation email) before this can be
    // safely turned on in production.

        @Value("${app.registration.require-email-verification:false}")
    private boolean requireEmailVerification;

    // Placeholder para um futuro fluxo de activação por email dedicado
    // a contas de staff. Fica sempre false por agora (não há serviço de
    // email para staff também) — ver createStaffUser(). Propositadamente
    // separado de app.registration.require-email-verification: a
    // activação de staff pode acabar por precisar de regras diferentes
    // da do registo público.
    @Value("${app.staff.require-email-verification:false}")
    private boolean requireStaffEmailVerification;

    // Espelha a ordem da CHECK constraint em
    // V2__add_platform_role_to_users.sql e do RoleHierarchy em
    // SecurityConfig — mantido aqui também porque a guarda de
    // escalonamento (regra 5, secção 2 do handoff) precisa de comparar
    // ranks numericamente, não só de "X implica Y" (isso é o que o
    // RoleHierarchy do Spring já faz, mas só em tempo de autorização).
    private static final List<String> PLATFORM_ROLE_HIERARCHY =
            List.of("CUSTOMER", "SUPPORT", "PLATFORM_ADMIN", "PLATFORM_OWNER");

    private int rankOf(String platformRole) {
        int rank = PLATFORM_ROLE_HIERARCHY.indexOf(platformRole);
        if (rank == -1) {
            throw new IllegalArgumentException("Invalid platform role: " + platformRole);
        }
        return rank;
    }

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
                .firstPassword(false) // cliente define a própria password — nunca precisa deste fluxo
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

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new AccountDeactivatedException("This account has been deactivated.");
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
        user.setFirstPassword(false); // liberta o acesso — regra 1

        User saved = userRepository.save(user);

        return toResponseDTO(saved, null);
    }

        @Transactional
    public AuthResponseDTO createStaffUser(String actingUserEmail, CreateStaffUserRequestDTO request) {
        User actingUser = userRepository.findByEmail(actingUserEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if ("CUSTOMER".equals(request.getPlatformRole())) {
            // CUSTOMER não é um papel de staff válido — para isso já
            // existe o registo público (regra 6, secção 2 do handoff).
            throw new IllegalArgumentException(
                    "CUSTOMER is not a valid role for createStaffUser — use public registration instead");
        }

        if (rankOf(request.getPlatformRole()) > rankOf(actingUser.getPlatformRole())) {
            // Guarda de escalonamento (regra 5): só se pode atribuir um
            // papel igual ou inferior ao próprio.
            throw new InsufficientPlatformRoleException(
                    "Cannot create a staff user with a platform role higher than your own");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("A user with this email already exists");
        }

        User staffUser = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(DEFAULT_STAFF_PASSWORD))
                .status(requireStaffEmailVerification ? "PENDING_VERIFICATION" : "ACTIVE")
                .platformRole(request.getPlatformRole())
                .firstPassword(true) // regra 1 — obrigado a mudar no primeiro login
                .build();

        User saved = userRepository.save(staffUser);

        return toResponseDTO(saved, null);
    }

    @Transactional(readOnly = true)
    public List<AuthResponseDTO> listStaffUsers(String actingUserEmail) {
        return userRepository.findByPlatformRoleNot("CUSTOMER").stream()
                .filter(user -> !user.getEmail().equalsIgnoreCase(actingUserEmail))
                .map(user -> toResponseDTO(user, null))
                .toList();
    }

    @Transactional
    public AuthResponseDTO updatePlatformRole(
            String actingUserEmail, UUID targetPublicUuid, UpdatePlatformRoleRequestDTO request) {

        User actingUser = userRepository.findByEmail(actingUserEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        User targetUser = userRepository.findByPublicUuid(targetPublicUuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        String newRole = request.getPlatformRole();

        if (rankOf(newRole) > rankOf(actingUser.getPlatformRole())) {
            // Mesma guarda de escalonamento — aplica-se tanto a criar
            // como a promover/despromover um utilizador existente.
            throw new InsufficientPlatformRoleException(
                    "Cannot assign a platform role higher than your own");
        }

        boolean isDemotingAnOwner = "PLATFORM_OWNER".equals(targetUser.getPlatformRole())
                && !"PLATFORM_OWNER".equals(newRole);

        if (isDemotingAnOwner) {
            long ownerCount = userRepository.countByPlatformRole("PLATFORM_OWNER");
            if (ownerCount <= 1) {
                // Tem de existir sempre pelo menos um PLATFORM_OWNER —
                // aplica-se quer o owner se esteja a auto-despromover,
                // quer esteja a ser despromovido por outro owner
                // (confirmado no handoff: "deve existir pelo menos um
                // owner para que o owner actual destitua ou seja
                // destituído por terceiros").
                throw new LastPlatformOwnerException(
                        "Cannot remove the last remaining PLATFORM_OWNER");
            }
        }

        targetUser.setPlatformRole(newRole);
        User saved = userRepository.save(targetUser);

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
                .platformRole(user.getPlatformRole())
                .firstPassword(user.isFirstPassword())
                .emailVerifiedAt(user.getEmailVerifiedAt())
                .createdAt(user.getCreatedAt())
                .token(token)
                .build();
    }

        @Transactional
    public AuthResponseDTO updateUserActiveStatus(
            String actingUserEmail, UUID targetPublicUuid, boolean active) {

        User actingUser = userRepository.findByEmail(actingUserEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        User targetUser = userRepository.findByPublicUuid(targetPublicUuid)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (actingUser.getIdUser().equals(targetUser.getIdUser())) {
            throw new IllegalArgumentException("You cannot change your own active status");
        }

        if (!active && "PLATFORM_OWNER".equals(targetUser.getPlatformRole())) {
            long activeOwnerCount =
                    userRepository.countByPlatformRoleAndStatus("PLATFORM_OWNER", "ACTIVE");
            if (activeOwnerCount <= 1) {
                throw new LastPlatformOwnerException(
                        "Cannot deactivate the last remaining active PLATFORM_OWNER");
            }
        }

        targetUser.setStatus(active ? "ACTIVE" : "INACTIVE");
        User saved = userRepository.save(targetUser);

        return toResponseDTO(saved, null);
    }
}