package com.nexis.auth_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "jwt.secretkey=this-is-a-very-long-dummy-test-secret-key-for-ci-cd-pipeline-12345!"
        }
)
@Testcontainers
public class AuthIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    @ServiceConnection
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired
    private TestRestTemplate restTemplate;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @Test
    void shouldSuccessfullySignUpNewUser() {
        Map<String, String> signupRequest = Map.of(
                "email", "test@nexis.com",
                "password", "password123",
                "fullname", "Test User"
        );

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/signup",
                signupRequest,
                String.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }
}