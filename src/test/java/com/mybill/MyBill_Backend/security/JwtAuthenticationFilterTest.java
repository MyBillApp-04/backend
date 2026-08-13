package com.mybill.MyBill_Backend.security;

import com.mybill.MyBill_Backend.entity.Role;
import com.mybill.MyBill_Backend.entity.User;
import com.mybill.MyBill_Backend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private JwtUtil jwtUtil;
    private UserRepository userRepository;
    private JwtTokenDenylist tokenDenylist;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtUtil = mock(JwtUtil.class);
        userRepository = mock(UserRepository.class);
        tokenDenylist = mock(JwtTokenDenylist.class);
        filter = new JwtAuthenticationFilter(jwtUtil, userRepository, tokenDenylist);
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletResponse run(String path, String authHeader) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        if (authHeader != null) {
            request.addHeader("Authorization", authHeader);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        return response;
    }

    @Test
    void authRoutesSkipJwtValidationEntirely() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setServletPath("/api/auth/login");
        request.addHeader("Authorization", "Bearer junk-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(tokenDenylist, never()).isDenied(org.mockito.ArgumentMatchers.anyString());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void missingBearerHeaderPassesThroughUnauthenticated() throws Exception {
        MockHttpServletResponse response = run("/api/invoices", "Basic abc");
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void noAuthorizationHeaderPassesThroughUnauthenticated() throws Exception {
        MockHttpServletResponse response = run("/api/invoices", null);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void deniedTokenIsRejectedWithUnauthorized() throws Exception {
        when(tokenDenylist.isDenied("t")).thenReturn(true);

        MockHttpServletResponse response = run("/api/invoices", "Bearer t");

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getErrorMessage()).contains("logged out");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void invalidTokenPassesThroughAsUnauthenticated() throws Exception {
        when(tokenDenylist.isDenied("t")).thenReturn(false);
        when(jwtUtil.validateToken("t")).thenReturn(false);

        MockHttpServletResponse response = run("/api/invoices", "Bearer t");

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void validTokenWithMissingUserIsRejected() throws Exception {
        when(tokenDenylist.isDenied("t")).thenReturn(false);
        when(jwtUtil.validateToken("t")).thenReturn(true);
        when(jwtUtil.extractEmail("t")).thenReturn("ghost@example.com");
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        MockHttpServletResponse response = run("/api/invoices", "Bearer t");

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getErrorMessage()).contains("no longer exists");
    }

    @Test
    void roleMismatchBetweenTokenAndPersistedUserIsRejected() throws Exception {
        User user = User.builder().id(1L).email("a@b.com").role(Role.OWNER).build();
        when(tokenDenylist.isDenied("t")).thenReturn(false);
        when(jwtUtil.validateToken("t")).thenReturn(true);
        when(jwtUtil.extractEmail("t")).thenReturn("a@b.com");
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(jwtUtil.extractRole("t")).thenReturn("ROLE_ADMIN");

        MockHttpServletResponse response = run("/api/invoices", "Bearer t");

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getErrorMessage()).contains("no longer valid");
    }

    @Test
    void validTokenPopulatesSecurityContextWithUserAndScopes() throws Exception {
        User user = User.builder().id(7L).email("owner@example.com").role(Role.OWNER).build();
        when(tokenDenylist.isDenied("t")).thenReturn(false);
        when(jwtUtil.validateToken("t")).thenReturn(true);
        when(jwtUtil.extractEmail("t")).thenReturn("owner@example.com");
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.extractRole("t")).thenReturn("ROLE_USER");
        when(jwtUtil.extractAuthorities("t")).thenReturn(List.of("ROLE_USER"));
        when(jwtUtil.extractScopes("t")).thenReturn(List.of("invoice:read", "invoice:write"));

        MockHttpServletResponse response = run("/api/invoices", "Bearer t");

        assertThat(response.getStatus()).isEqualTo(200);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo("owner@example.com");
        assertThat(auth.getAuthorities())
                .extracting("authority")
                .contains("ROLE_USER", "SCOPE_invoice:read", "SCOPE_invoice:write");
        assertThat(auth.getDetails()).isEqualTo(7L);
    }
}