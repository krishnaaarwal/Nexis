package com.nexis.storage_service.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class S3Config {

    @Value("${nexis.storage.s3.endpoint}")
    private String endpoint;

    @Value("${nexis.storage.s3.access-key}")
    private String accessKey;

    @Value("${nexis.storage.s3.secret-key}")
    private String secretKey;

    @Bean
    public MinioClient s3Client() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
