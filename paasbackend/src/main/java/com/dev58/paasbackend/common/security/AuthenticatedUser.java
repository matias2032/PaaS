package com.dev58.paasbackend.common.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

// Custom principal so Controllers can access idUser/publicUuid directly
// via @AuthenticationPrincipal, without an extra DB lookup per request
// (CustomUserDetailsService already loads the User row once).
@Getter
public class AuthenticatedUser implements UserDetails {

    private final Long idUser;
    private final UUID publicUuid;
    private final String email;
    private final String passwordHash;
    private final boolean enabled;
    private final String platformRole;
    private final boolean firstPasswordPending;

    public AuthenticatedUser(
            Long idUser, UUID publicUuid, String email, String passwordHash,
            boolean enabled, String platformRole, boolean firstPasswordPending) {
        this.idUser = idUser;
        this.publicUuid = publicUuid;
        this.email = email;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
        this.platformRole = platformRole;
        this.firstPasswordPending = firstPasswordPending;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Every authenticated user gets ROLE_USER (unchanged — existing
        // isAuthenticated() checks elsewhere keep working). On top of
        // that, one extra authority mirroring platformRole exactly —
        // "ROLE_CUSTOMER" | "ROLE_SUPPORT" | "ROLE_PLATFORM_ADMIN" |
        // "ROLE_PLATFORM_OWNER" — so @PreAuthorize("hasRole('PLATFORM_ADMIN')")
        // on an InfrastructureController method works without extra mapping.
        return List.of(
                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"),
                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + platformRole)
        );
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}