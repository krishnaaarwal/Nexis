package com.nexis.execution_service;

import com.nexis.execution_service.config.type.CodeLanguage;
import com.nexis.execution_service.config.type.StatusType;
import com.nexis.execution_service.dto.JobRequestDto;
import com.nexis.execution_service.dto.JobResponseDto;
import com.nexis.execution_service.entity.ExecutionJob;
import com.nexis.execution_service.repository.ExecutionRepository;
import com.nexis.execution_service.service.ExecutionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop"
        }
)
@Testcontainers
public class ExecutionIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3-management-alpine");

    @Autowired
    private ExecutionService executionService;

    @Autowired
    private ExecutionRepository jobRepository;

    @Test
    void shouldProcessCodeExecutionJobAsynchronously() {

        JobRequestDto request = new JobRequestDto();
        request.setUserId(UUID.randomUUID());
        request.setWorkspaceId(UUID.randomUUID());
        request.setCodeLanguage(CodeLanguage.PYTHON);
        request.setCode("print('Hello from Testcontainers!')");


        JobResponseDto response = executionService.submitJob(request);
        UUID jobId = response.getId();

//  We use Awaitility to wait for the background worker to consume it
        await()
                .atMost(30, TimeUnit.SECONDS) // Generous timeout in case Docker needs to pull the Python image
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {

                    ExecutionJob job = jobRepository.findById(jobId).orElseThrow();

                    assertThat(job.getStatus()).isNotEqualTo(StatusType.QUEUED);

                    System.out.println("Worker successfully processed job! Final status: " + job.getStatus());
                });
    }
}