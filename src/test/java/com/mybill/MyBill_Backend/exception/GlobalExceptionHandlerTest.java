package com.mybill.MyBill_Backend.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new BrokenController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void returnsStandardJsonErrorResponse() throws Exception {
        mockMvc.perform(get("/broken"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid request. Please check the submitted data"))
                .andExpect(jsonPath("$.path").value("/broken"));
    }

    @Test
    void hidesSecurityExceptionDetails() throws Exception {
        mockMvc.perform(get("/security"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Request failed security validation"))
                .andExpect(content().string(containsString("timestamp")));
    }

    @Test
    void incrementsMeterRegistryOnError() throws Exception {
        io.micrometer.core.instrument.MeterRegistry mockRegistry = org.mockito.Mockito.mock(io.micrometer.core.instrument.MeterRegistry.class);
        io.micrometer.core.instrument.Counter mockCounter = org.mockito.Mockito.mock(io.micrometer.core.instrument.Counter.class);

        org.mockito.Mockito.when(mockRegistry.counter(
                org.mockito.Mockito.eq("mybill.api.errors"),
                org.mockito.Mockito.any(String[].class)
        )).thenReturn(mockCounter);

        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        org.springframework.test.util.ReflectionTestUtils.setField(handler, "meterRegistry", mockRegistry);

        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new BrokenController())
                .setControllerAdvice(handler)
                .build();

        mvc.perform(get("/broken"))
                .andExpect(status().isBadRequest());

        org.mockito.Mockito.verify(mockCounter, org.mockito.Mockito.times(1)).increment();
    }

    @RestController
    static class BrokenController {
        @GetMapping("/broken")
        void broken() {
            throw new IllegalArgumentException("raw validation detail");
        }

        @GetMapping("/security")
        void security() {
            throw new SecurityException("metadata mismatch");
        }
    }
}
