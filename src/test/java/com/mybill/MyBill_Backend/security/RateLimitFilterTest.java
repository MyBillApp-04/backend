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
        var registry = (io.micrometer.core.instrument.MeterRegistry) ReflectionTestUtils.getField(filter, "meterRegistry");
        assertThat(registry.find("security_rate_limit_rejections_total").tag("scope", "ip").counter().count())
                .isEqualTo(1.0);
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
    void throttlesPublicQuotationResponsesMoreAggressively() throws Exception {
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        when(securityUtils.getCurrentUserId()).thenThrow(new RuntimeException("anonymous"));
        RateLimitFilter filter = new RateLimitFilter(securityUtils);
        ReflectionTestUtils.setField(filter, "ipLimitPerMinute", 100);
        ReflectionTestUtils.setField(filter, "publicQuotationActionLimitPerMinute", 1);

        filter.doFilter(request("/q/abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMN012345/respond"), new MockHttpServletResponse(), new MockFilterChain());
        MockHttpServletResponse rejected = new MockHttpServletResponse();
        filter.doFilter(request("/q/abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMN012345/respond"), rejected, new MockFilterChain());

        assertThat(rejected.getStatus()).isEqualTo(429);
    }

    @Test
    void throttlesCloudinaryUploadSignatureAndMetadataEndpoints() throws Exception {
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        when(securityUtils.getCurrentUserId()).thenThrow(new RuntimeException("anonymous"));
        RateLimitFilter filter = new RateLimitFilter(securityUtils);
        ReflectionTestUtils.setField(filter, "ipLimitPerMinute", 100);
        ReflectionTestUtils.setField(filter, "cloudinarySignatureLimitPerMinute", 1);
        ReflectionTestUtils.setField(filter, "cloudinaryMetadataLimitPerMinute", 1);

        filter.doFilter(request("/api/business/upload/logo/signature"), new MockHttpServletResponse(), new MockFilterChain());
        MockHttpServletResponse sigRejected = new MockHttpServletResponse();
        filter.doFilter(request("/api/business/upload/logo/signature"), sigRejected, new MockFilterChain());

        assertThat(sigRejected.getStatus()).isEqualTo(429);
        assertThat(sigRejected.getContentAsString()).contains("Cloudinary upload signature");

        filter.doFilter(request("/api/business/upload/logo/metadata"), new MockHttpServletResponse(), new MockFilterChain());
        MockHttpServletResponse metaRejected = new MockHttpServletResponse();
        filter.doFilter(request("/api/business/upload/logo/metadata"), metaRejected, new MockFilterChain());

        assertThat(metaRejected.getStatus()).isEqualTo(429);
        assertThat(metaRejected.getContentAsString()).contains("Cloudinary image metadata");
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
        ReflectionTestUtils.setField(filter, "trustedProxyIps", "203.0.113.10");

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

    @Test
    void supportsCidrRangesInTrustedProxyConfiguration() throws Exception {
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        when(securityUtils.getCurrentUserId()).thenThrow(new RuntimeException("anonymous"));

        RateLimitFilter filter = new RateLimitFilter(securityUtils);
        ReflectionTestUtils.setField(filter, "ipLimitPerMinute", 1);
        ReflectionTestUtils.setField(filter, "userLimitPerMinute", 100);
        ReflectionTestUtils.setField(filter, "authLimitPerMinute", 100);
        ReflectionTestUtils.setField(filter, "trustForwardedHeaders", true);
        ReflectionTestUtils.setField(filter, "trustedProxyIps", "10.0.0.0/8, 172.16.0.0/12");

        MockHttpServletRequest firstRequest = new MockHttpServletRequest("POST", "/api/dashboard/summary");
        firstRequest.setRemoteAddr("10.1.2.3");
        firstRequest.addHeader("X-Forwarded-For", "198.51.100.1");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(firstRequest, firstResponse, new MockFilterChain());

        MockHttpServletRequest secondRequest = new MockHttpServletRequest("POST", "/api/dashboard/summary");
        secondRequest.setRemoteAddr("172.16.5.9");
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
