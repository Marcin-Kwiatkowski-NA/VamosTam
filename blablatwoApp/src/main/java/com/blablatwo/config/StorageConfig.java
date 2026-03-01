package com.blablatwo.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig {

    @Bean
    S3Presigner s3Presigner(
            @org.springframework.beans.factory.annotation.Value("${spring.cloud.aws.s3.endpoint}") String endpoint,
            @org.springframework.beans.factory.annotation.Value("${spring.cloud.aws.credentials.access-key}") String accessKey,
            @org.springframework.beans.factory.annotation.Value("${spring.cloud.aws.credentials.secret-key}") String secretKey,
            @org.springframework.beans.factory.annotation.Value("${spring.cloud.aws.region.static}") String region
    ) {
        return S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    @Bean("avatarCleanupExecutor")
    TaskExecutor avatarCleanupExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("avatar-cleanup-");
        return executor;
    }
}
