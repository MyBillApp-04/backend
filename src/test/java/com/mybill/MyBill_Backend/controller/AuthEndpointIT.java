package com.mybill.MyBill_Backend.controller;

import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class AuthEndpointIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockitoSpyBean
    private AuthController authController;

    private FirebaseToken firebaseToken;

    @BeforeEach
    void setUp() throws Exception {
        firebaseToken = mock(FirebaseToken.class);
    }

    @Test
    void testSuccessfulLoginReturnsJwt() throws Exception {
        // Arrange
        when(firebaseToken.getEmail()).thenReturn("integration@example.com");
        Map<String, Object> claims = new HashMap<>();
        claims.put("name", "Integration User");
        when(firebaseToken.getClaims()).thenReturn(claims);
        doReturn(firebaseToken).when(authController).verifyIdToken(anyString());

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("token", "dummy-firebase-token");

        // Act
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/auth/firebase-login",
                requestBody,
                Map.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("token")).isNotNull();
    }

    @Test
    void testMissingTokenReturnsBadRequest() {
        // Arrange
        Map<String, String> requestBody = new HashMap<>();

        // Act
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/auth/firebase-login",
                requestBody,
                Map.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("error")).isEqualTo("Missing token in request body");
    }

    @Test
    void testInvalidTokenReturnsInternalServerError() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Invalid token")).when(authController).verifyIdToken(anyString());

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("token", "invalid-firebase-token");

        // Act
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/auth/firebase-login",
                requestBody,
                Map.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("error")).isEqualTo("Server-side login failure");
    }
}
