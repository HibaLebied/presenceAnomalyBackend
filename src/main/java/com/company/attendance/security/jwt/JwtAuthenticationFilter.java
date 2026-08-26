package com.company.attendance.security.jwt;

import com.company.attendance.security.service.CustomUserDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader =
                request.getHeader("Authorization");

        // Pas de header Authorization
        if (authHeader == null ||
                !authHeader.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        // Récupérer le JWT
        final String jwt =
                authHeader.substring(7);

        try {

            // Extraire l'email
            final String email =
                    jwtService.extractEmail(jwt);

            // Vérifier qu'aucun utilisateur n'est
            // déjà authentifié
            if (email != null &&
                    SecurityContextHolder
                            .getContext()
                            .getAuthentication() == null) {

                // Chercher l'utilisateur en DB
                UserDetails userDetails =
                        userDetailsService
                                .loadUserByUsername(email);

                // Vérifier le JWT
                if (jwtService.isTokenValid(
                        jwt,
                        userDetails
                )) {

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    // Ajouter l'utilisateur au SecurityContext
                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(authentication);
                }
            }

        } catch (Exception e) {

            // JWT invalide ou expiré
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}