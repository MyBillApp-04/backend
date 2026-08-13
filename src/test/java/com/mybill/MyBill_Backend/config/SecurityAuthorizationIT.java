package com.mybill.MyBill_Backend.config;

import com.mybill.MyBill_Backend.MigrationPreprocessor;
import com.mybill.MyBill_Backend.entity.AuthProvider;
import com.mybill.MyBill_Backend.entity.Role;
import com.mybill.MyBill_Backend.entity.User;
import com.mybill.MyBill_Backend.repository.UserRepository;
import com.mybill.MyBill_Backend.security.JwtUtil;
import com.mybill.MyBill_Backend.security.RateLimitFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:security_authz_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.security.public-api-docs=false"
})
@Transactional
class SecurityAuthorizationIT {

    static {
        MigrationPreprocessor.process();
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    private User ownerUser;
    private User adminUser;
    private String ownerToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(rateLimitFilter, "authLimitPerMinute", 10000);
        ReflectionTestUtils.setField(rateLimitFilter, "ipLimitPerMinute", 10000);
        com.github.benmanes.caffeine.cache.Cache<?, ?> cache =
                (com.github.benmanes.caffeine.cache.Cache<?, ?>) ReflectionTestUtils.getField(rateLimitFilter, "counters");
        if (cache != null) {
            cache.invalidateAll();
        }

        ownerUser = userRepository.save(User.builder()
                .name("Owner")
                .email("authz_owner@example.com")
                .password("securePassword123")
                .role(Role.OWNER)
                .provider(AuthProvider.LOCAL)
                .build());
        adminUser = userRepository.save(User.builder()
                .name("Admin")
                .email("authz_admin@example.com")
                .password("securePassword123")
                .role(Role.ADMIN)
                .provider(AuthProvider.LOCAL)
                .build());

        ownerToken = jwtUtil.generateToken(ownerUser.getEmail());
        adminToken = jwtUtil.generateToken(adminUser.getEmail(), Role.ADMIN);
    }

    @Test
    void authEndpointsArePublic() throws Exception {
        mockMvc.perform(get("/api/auth/ping"))
                .andExpect(status().isOk());
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void appVersionEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/app-version"))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void apiRouteWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/work/all"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiRouteWithOwnerTokenIsAuthorized() throws Exception {
        mockMvc.perform(get("/api/work/all").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminRouteRejectsOwnerToken() throws Exception {
        mockMvc.perform(get("/api/admin/app-releases")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminRouteAllowsAdminToken() throws Exception {
        mockMvc.perform(get("/api/admin/app-releases").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void swaggerDocsAreNotPublicWhenDisabled() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isUnauthorized());
    }
}
