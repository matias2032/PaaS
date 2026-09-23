package com.dev58.paasbackend.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // enables @PreAuthorize on controller methods —
                       // first real use: AuthController's staff-creation/
                       // role-update endpoints, then InfrastructureController
                       // and BillingController.listAllPlans() later, all
                       // gated with @PreAuthorize("hasRole('PLATFORM_ADMIN')")
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Linear platform-staff hierarchy — mirrors the CHECK constraint
    // order in V2__add_platform_role_to_users.sql exactly. A
    // PLATFORM_OWNER passes any @PreAuthorize("hasRole('SUPPORT')")
    // check automatically, without needing hasAnyRole(...) everywhere.
    // `static` is required here: RoleHierarchy must be available before
    // Spring's method-security infrastructure beans are created.
    @Bean
    static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("""
                ROLE_PLATFORM_OWNER > ROLE_PLATFORM_ADMIN
                ROLE_PLATFORM_ADMIN > ROLE_SUPPORT
                ROLE_SUPPORT > ROLE_CUSTOMER
                """);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // Sem allowCredentials porque ainda não usamos cookies (sem refresh
        // token por agora). Se adicionarmos cookie httpOnly no futuro,
        // isto passa a true e o React precisa de withCredentials: true.
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                            "/api/auth/register",
                            "/api/auth/login",
                            "/api/auth/{publicUuid}",
                            "/api/auth/forgot-password",
                            "/api/auth/reset-password",
                            "/api/plans",
                            "/api/plans/{publicUuid}",
                            "/error"
                    ).permitAll()
                    .anyRequest().authenticated()
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}