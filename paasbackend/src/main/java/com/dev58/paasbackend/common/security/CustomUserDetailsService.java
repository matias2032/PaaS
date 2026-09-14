package com.dev58.paasbackend.common.security;

import com.dev58.paasbackend.auth.entity.User;
import com.dev58.paasbackend.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Utilizador não encontrado: " + email));

        // ROLE_USER é genérico — permissões reais por organização
        // são resolvidas no módulo ORGANIZATION via organization_members,
        // não aqui. Este authority serve só para o Spring Security
        // aceitar o UserDetails como válido.
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .disabled(!"ACTIVE".equals(user.getStatus()))
                .build();
    }
}