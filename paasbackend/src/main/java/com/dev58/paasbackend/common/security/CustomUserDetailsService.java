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

    // AuthenticatedUser carrega idUser/publicUuid para que os Controllers
    // os obtenham via @AuthenticationPrincipal sem query extra. Permissões
    // reais por organização continuam a ser resolvidas no módulo
    // ORGANIZATION via organization_members, não aqui.
    return new AuthenticatedUser(
            user.getIdUser(),
            user.getPublicUuid(),
            user.getEmail(),
            user.getPasswordHash(),
            "ACTIVE".equals(user.getStatus()),
            user.getPlatformRole()
    );
}
}