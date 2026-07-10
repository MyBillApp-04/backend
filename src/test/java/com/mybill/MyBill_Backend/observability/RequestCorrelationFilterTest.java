package com.mybill.MyBill_Backend.observability;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RequestCorrelationFilterTest {

    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @Test
    void preservesSafeCallerRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ping");
        request.addHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "mobile:sync-42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER))
                .isEqualTo("mobile:sync-42");
    }

    @Test
    void replacesUnsafeRequestId() {
        assertThat(filter.resolveRequestId("bad\nheader")).doesNotContain("\n");
        assertThat(filter.resolveRequestId("bad\nheader")).matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }
}
