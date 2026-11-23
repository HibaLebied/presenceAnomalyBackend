package com.company.attendance.security;

import com.company.attendance.entity.dashboard.User;
import com.company.attendance.entity.dashboard.UserRole;
import com.company.attendance.repository.dashboard.UserRepository;
import com.company.attendance.security.jwt.JwtAuthenticationFilter;
import com.company.attendance.security.jwt.JwtService;
import com.company.attendance.security.service.CustomUserDetailsService;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.CorsFilter;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityConfigTest {

    private static final String SECRET = "test-secret-with-at-least-32-characters";
    private static final String ADMIN_EMAIL = "admin@company.com";
    private static final String MANAGER_EMAIL = "manager@company.com";

    private MockMvc mockMvc;
    private JwtService jwtService;

    @BeforeEach
    void setUp() throws Exception {
        UserRepository userRepository = userRepository();
        CustomUserDetailsService userDetailsService = new CustomUserDetailsService(userRepository);
        jwtService = new JwtService(SECRET, 60_000);
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtService, userDetailsService);

        SecurityConfig securityConfig = new SecurityConfig(jwtFilter, userDetailsService, passwordEncoder());
        ObjectPostProcessor<Object> postProcessor = new ObjectPostProcessor<>() {
            @Override
            public <O> O postProcess(O object) {
                return object;
            }
        };
        AuthenticationManagerBuilder authenticationManagerBuilder =
                new AuthenticationManagerBuilder(postProcessor);
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        applicationContext.getBeanFactory().registerSingleton(
                "mvcHandlerMappingIntrospector",
                new HandlerMappingIntrospector()
        );
        applicationContext.refresh();
        CorsConfigurationSource corsConfigurationSource = request -> null;
        HttpSecurity http = new HttpSecurity(
                postProcessor,
                authenticationManagerBuilder,
                Map.of(
                        ApplicationContext.class, applicationContext,
                        CorsConfigurationSource.class, corsConfigurationSource
                )
        );
        SecurityFilterChain configuredChain = securityConfig.securityFilterChain(http);
        SecurityFilterChain chain = new DefaultSecurityFilterChain(
                AnyRequestMatcher.INSTANCE,
                configuredChain.getFilters().stream()
                        .filter(filter -> !(filter instanceof CorsFilter))
                        .toList()
        );
        Filter securityFilter = new FilterChainProxy(chain);

        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .addFilters(securityFilter)
                .build();
    }

    @Test
    void shouldReturn401WhenAProtectedEndpointHasNoAccessToken() throws Exception {
        mockMvc.perform(get("/anomalies/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403WhenAManagerCallsAnAdminEndpoint() throws Exception {
        mockMvc.perform(patch("/anomalies/1/ignore")
                        .header("Authorization", bearerToken(MANAGER_EMAIL, UserRole.MANAGER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAnAdminToCallAnAdminEndpoint() throws Exception {
        mockMvc.perform(patch("/anomalies/1/ignore")
                        .header("Authorization", bearerToken(ADMIN_EMAIL, UserRole.ADMIN_RH)))
                .andExpect(status().isOk())
                .andExpect(content().string("ignored"));
    }

    @Test
    void shouldAllowBothRolesToReadAnomalies() throws Exception {
        mockMvc.perform(get("/anomalies/1")
                        .header("Authorization", bearerToken(ADMIN_EMAIL, UserRole.ADMIN_RH)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/anomalies/1")
                        .header("Authorization", bearerToken(MANAGER_EMAIL, UserRole.MANAGER)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAllowThePublicRefreshEndpointWithoutAnAccessToken() throws Exception {
        mockMvc.perform(post("/auth/refresh"))
                .andExpect(status().isOk())
                .andExpect(content().string("refresh"));
    }

    private String bearerToken(String email, UserRole role) {
        return "Bearer " + jwtService.generateToken(
                new AuthenticatedUser(1L, role.name(), email, "hash", role.name(), true)
        );
    }

    @RestController
    static class TestController {

        @GetMapping("/anomalies/1")
        String readAnomaly() {
            return "read";
        }

        @PatchMapping("/anomalies/1/ignore")
        String ignoreAnomaly() {
            return "ignored";
        }

        @PostMapping("/auth/refresh")
        String refresh() {
            return "refresh";
        }
    }

    private UserRepository userRepository() {
        User admin = user(1L, ADMIN_EMAIL, UserRole.ADMIN_RH);
        User manager = user(2L, MANAGER_EMAIL, UserRole.MANAGER);
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getName().equals("findByEmail")) {
                String email = (String) args[0];
                return List.of(admin, manager).stream()
                        .filter(user -> user.getEmail().equals(email))
                        .findFirst();
            }
            return defaultValue(method.getReturnType());
        };
        return proxy(UserRepository.class, handler);
    }

    private static User user(Long id, String email, UserRole role) {
        LocalDateTime now = LocalDateTime.now();
        return User.builder()
                .id(id)
                .fullName(role.name())
                .email(email)
                .passwordHash("hash")
                .role(role)
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private static PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        return null;
    }
}
