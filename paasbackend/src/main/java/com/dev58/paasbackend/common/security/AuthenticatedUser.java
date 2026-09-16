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

    public AuthenticatedUser(Long idUser, UUID publicUuid, String email, String passwordHash, boolean enabled) {
        this.idUser = idUser;
        this.publicUuid = publicUuid;
        this.email = email;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"));
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