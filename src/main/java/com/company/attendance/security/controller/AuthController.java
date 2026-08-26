package com.company.attendance.security.controller;

import com.company.attendance.dto.LoginRequest;
import com.company.attendance.security.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(
                authService.login(
                        request.email(),
                        request.password()
                )
        );
    }
}