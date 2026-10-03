package com.nexis.storage_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import org.testcontainers.utility.DockerImageName;



@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop"
        }
)
@Testcontainers
public class StorageIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");



    @Container
    static MinIOContainer minio = new MinIOContainer(
            DockerImageName.parse("pgsty/silo:RELEASE.2026-09-16T00-00-00Z")
                    .asCompatibleSubstituteFor("minio/minio")
    );

    @DynamicPropertySource
    static void minioProperties(DynamicPropertyRegistry registry) {
        registry.add("nexis.storage.s3.endpoint", minio::getS3URL);
        registry.add("nexis.storage.s3.access-key", minio::getUserName);
        registry.add("nexis.storage.s3.secret-key", minio::getPassword);
        registry.add("nexis.storage.s3.bucket-name", () -> "nexis-workspaces");
        registry.add("nexis.storage.s3.region", () -> "ap-south-1");
    }
    @Test
    void storageContextLoadsAndContainersAreRunning() {
        assertThat(postgres.isRunning()).isTrue();
        assertThat(minio.isRunning()).isTrue();

        System.out.println("Silo running at: " + minio.getS3URL());
        System.out.println("Silo Access Key: " + minio.getUserName());
    }
}