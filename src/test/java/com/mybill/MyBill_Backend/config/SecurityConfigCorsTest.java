package com.mybill.MyBill_Backend.config;

import com.mybill.MyBill_Backend.security.RateLimitFilter;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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

    @Test
    void productionConfigurationAllowsFirebaseButRejectsLocalOrigins() {
        SecurityConfig config = new SecurityConfig(null, null,
                new MockEnvironment().withProperty("spring.profiles.active", "prod"));
        ReflectionTestUtils.setField(config, "allowedOrigins",
                "https://mybill-app-04.firebaseapp.com,"
                        + "https://mybill-app-04.web.app");

        CorsConfiguration cors = config.corsConfigurationSource()
                .getCorsConfiguration(preflightRequest("http://localhost:53642"));

        assertThat(cors).isNotNull();
        assertThat(cors.checkOrigin("https://mybill-app-04.firebaseapp.com"))
                .isEqualTo("https://mybill-app-04.firebaseapp.com");
        assertThat(cors.checkOrigin("https://mybill-app-04.web.app"))
                .isEqualTo("https://mybill-app-04.web.app");
        assertThat(cors.checkOrigin("http://localhost:53642"))
                .isNull();
        assertThat(cors.checkOrigin("http://127.0.0.1:61234"))
                .isNull();
        assertThat(cors.checkOrigin("http://[::1]:54231"))
                .isNull();
    }

    @Test
    void rateLimitFilterIsRegisteredAtHighestPrecedence() {
        RateLimitFilter filter = new RateLimitFilter(mock(SecurityUtils.class));
        SecurityConfig config = new SecurityConfig(null, filter, new MockEnvironment());

        var registration = config.rateLimitFilterRegistration();

        assertThat(registration.getFilter()).isSameAs(filter);
        assertThat(registration.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
        assertThat(registration.getUrlPatterns()).containsExactly("/*");
    }

    private HttpServletRequest preflightRequest(String origin) {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/auth/firebase-login");
        request.addHeader("Origin", origin);
        request.addHeader("Access-Control-Request-Method", "POST");
        return request;
    }
}
