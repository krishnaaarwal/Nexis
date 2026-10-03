package com.nexis.recording_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {
				"spring.jpa.hibernate.ddl-auto=create-drop"
		}
)
@Testcontainers
public class RecordingIntegrationTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

	@Container
	@ServiceConnection
	static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3-management-alpine");

	@Test
	void recordingContextLoadsAndContainersAreRunning() {
		assertThat(postgres.isRunning()).isTrue();
		assertThat(rabbitmq.isRunning()).isTrue();

		System.out.println("Recording Service linked to ephemeral Postgres & RabbitMQ!");
	}
}