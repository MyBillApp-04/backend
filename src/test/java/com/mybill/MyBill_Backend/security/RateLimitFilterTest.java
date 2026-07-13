package com.mybill.MyBill_Backend.security;

import com.mybill.MyBill_Backend.util.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimitFilterTest {

    @Test
    void throttlesRequestsByIp() throws Exception {
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        when(securityUtils.getCurrentUserId()).thenThrow(new RuntimeException("anonymous"));

        RateLimitFilter filter = new RateLimitFilter(securityUtils);
        ReflectionTestUtils.setField(filter, "ipLimitPerMinute", 1);
        ReflectionTestUtils.setField(filter, "userLimitPerMinute", 100);
        ReflectionTestUtils.setField(filter, "authLimitPerMinute", 100);

        MockHttpServletRequest firstRequest = request("/api/dashboard/summary");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(firstRequest, firstResponse, new MockFilterChain());

        MockHttpServletRequest secondRequest = request("/api/dashboard/summary");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(secondRequest, secondResponse, new MockFilterChain());

        assertThat(firstResponse.getStatus()).isEqualTo(200);
        assertThat(secondResponse.getStatus()).isEqualTo(429);
        assertThat(secondResponse.getContentAsString()).contains("Too Many Requests");
    }

    @Test
    void throttlesLoginAttemptsMoreAggressively() throws Exception {
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        when(securityUtils.getCurrentUserId()).thenThrow(new RuntimeException("anonymous"));

        RateLimitFilter filter = new RateLimitFilter(securityUtils);
        ReflectionTestUtils.setField(filter, "ipLimitPerMinute", 100);
        ReflectionTestUtils.setField(filter, "userLimitPerMinute", 100);
        ReflectionTestUtils.setField(filter, "authLimitPerMinute", 1);

        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(request("/api/auth/firebase-login"), firstResponse, new MockFilterChain());

        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(request("/api/auth/firebase-login"), secondResponse, new MockFilterChain());

        assertThat(firstResponse.getStatus()).isEqualTo(200);
        assertThat(secondResponse.getStatus()).isEqualTo(429);
        assertThat(secondResponse.getContentAsString()).contains("Too many login attempts");
    }

    @Test
    void ignoresSpoofedForwardedForHeaderUnlessExplicitlyTrusted() throws Exception {
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        when(securityUtils.getCurrentUserId()).thenThrow(new RuntimeException("anonymous"));

        RateLimitFilter filter = new RateLimitFilter(securityUtils);
        ReflectionTestUtils.setField(filter, "ipLimitPerMinute", 1);
        ReflectionTestUtils.setField(filter, "userLimitPerMinute", 100);
        ReflectionTestUtils.setField(filter, "authLimitPerMinute", 100);
        ReflectionTestUtils.setField(filter, "trustForwardedHeaders", false);

        MockHttpServletRequest firstRequest = request("/api/dashboard/summary");
        firstRequest.addHeader("X-Forwarded-For", "198.51.100.1");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(firstRequest, firstResponse, new MockFilterChain());

        MockHttpServletRequest secondRequest = request("/api/dashboard/summary");
        secondRequest.addHeader("X-Forwarded-For", "198.51.100.2");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(secondRequest, secondResponse, new MockFilterChain());

        assertThat(firstResponse.getStatus()).isEqualTo(200);
        assertThat(secondResponse.getStatus()).isEqualTo(429);
    }

    @Test
    void usesForwardedForHeaderWhenTrustedAndValid() throws Exception {
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        when(securityUtils.getCurrentUserId()).thenThrow(new RuntimeException("anonymous"));

        RateLimitFilter filter = new RateLimitFilter(securityUtils);
        ReflectionTestUtils.setField(filter, "ipLimitPerMinute", 1);
        ReflectionTestUtils.setField(filter, "userLimitPerMinute", 100);
        ReflectionTestUtils.setField(filter, "authLimitPerMinute", 100);
        ReflectionTestUtils.setField(filter, "trustForwardedHeaders", true);

        MockHttpServletRequest firstRequest = request("/api/dashboard/summary");
        firstRequest.addHeader("X-Forwarded-For", "198.51.100.1");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(firstRequest, firstResponse, new MockFilterChain());

        MockHttpServletRequest secondRequest = request("/api/dashboard/summary");
        secondRequest.addHeader("X-Forwarded-For", "198.51.100.2");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(secondRequest, secondResponse, new MockFilterChain());

        assertThat(firstResponse.getStatus()).isEqualTo(200);
        assertThat(secondResponse.getStatus()).isEqualTo(200);
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr("203.0.113.10");
        return request;
    }
}
