package com.mybill.MyBill_Backend.config;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigCorsTest {

    @Test
    void localDevOriginsAllowAnyLocalhostPort() {
        SecurityConfig config = new SecurityConfig(null, null,
                new MockEnvironment().withProperty("spring.profiles.active", "dev"));
        ReflectionTestUtils.setField(config, "allowedOrigins",
                "http://localhost:*,http://127.0.0.1:*,http://[::1]:*");

        CorsConfiguration cors = config.corsConfigurationSource()
                .getCorsConfiguration(preflightRequest("http://localhost:53642"));

        assertThat(cors).isNotNull();
        assertThat(cors.checkOrigin("http://localhost:53642"))
                .isEqualTo("http://localhost:53642");
        assertThat(cors.checkOrigin("http://127.0.0.1:61234"))
                .isEqualTo("http://127.0.0.1:61234");
        assertThat(cors.checkOrigin("http://[::1]:54231"))
                .isEqualTo("http://[::1]:54231");
    }

    private HttpServletRequest preflightRequest(String origin) {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/auth/firebase-login");
        request.addHeader("Origin", origin);
        request.addHeader("Access-Control-Request-Method", "POST");
        return request;
    }
}
