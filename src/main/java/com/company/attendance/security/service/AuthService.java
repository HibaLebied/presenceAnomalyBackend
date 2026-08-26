package com.company.attendance.security.service;

import com.company.attendance.dto.LoginResponse;
import com.company.attendance.security.jwt.JwtService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public LoginResponse login(String email, String password) {

        // 1. Spring Security authentifie l'utilisateur
        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                email,
                                password
                        )
                );

        // 2. Récupérer le UserDetails authentifié
        UserDetails userDetails =
                (UserDetails) authentication.getPrincipal();

        // 3. Générer le JWT
        String token = jwtService.generateToken(userDetails);

        // 4. Retourner le token
        return new LoginResponse(token);
    }
}
