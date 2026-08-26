package com.company.attendance.security;

import com.company.attendance.security.jwt.JwtAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(
            org.springframework.security.config.annotation.web.builders.HttpSecurity http
    ) throws Exception {

        http
                // API REST → pas de CSRF
                .csrf(csrf -> csrf.disable())

                // JWT → pas de session
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // Public
                        .requestMatchers("/auth/**").permitAll()

                        // Admin RH uniquement
                        .requestMatchers("synchronization/**",
                                "anomalies/*/ignore",
                                "anomalies/*/activate")
                        .hasRole("ADMIN_RH")
                        // Admin RH + Manager
                        .requestMatchers(
                                "/anomalies/**",
                                "/dashboard/**",
                                "/departments/**",
                                "/supervisors/**",
                                "/filters/**"
                        )
                        .hasAnyRole("ADMIN_RH", "MANAGER")

                        // Tout le reste doit être authentifié
                        .anyRequest()
                        .authenticated()
                )
                .exceptionHandling(exception -> exception

                        .authenticationEntryPoint(
                                (request, response, authException) ->
                                        response.sendError(
                                                HttpServletResponse.SC_UNAUTHORIZED,
                                                "Unauthorized"
                                        )
                        )

                        .accessDeniedHandler(
                                (request, response, accessDeniedException) ->
                                        response.sendError(
                                                HttpServletResponse.SC_FORBIDDEN,
                                                "Forbidden"
                                        )
                        )
                )
                // Notre filtre JWT doit être exécuté
                // avant le filtre UsernamePasswordAuthenticationFilter
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {

        return configuration.getAuthenticationManager();
    }
}