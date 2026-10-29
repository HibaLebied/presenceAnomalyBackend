package com.company.attendance.security.service;

import com.company.attendance.entity.dashboard.User;
import com.company.attendance.entity.dashboard.UserRole;
import com.company.attendance.repository.dashboard.UserRepository;
import com.company.attendance.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    @Transactional(transactionManager = "dashboardTransactionManager", readOnly = true)
    public User getCurrentUser() {
        AuthenticatedUser principal = getCurrentPrincipal();
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new AccessDeniedException("Authenticated user was not found"));
    }

    public boolean hasRole(UserRole role) {
        AuthenticatedUser principal = getCurrentPrincipal();
        return role.name().equals(principal.getRole());
    }

    public AuthenticatedUser getCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            throw new AccessDeniedException("No authenticated user found");
        }
        return principal;
    }
}