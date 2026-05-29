package com.codemong.be.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;

@Configuration
@ConditionalOnProperty(name = "aws.kms.enabled", havingValue = "true")
public class KmsConfig {

    @Bean
    public KmsClient kmsClient(
            @Value("${aws.region}") String region,
            @Value("${aws.credentials.access-key-id:}") String accessKeyId,
            @Value("${aws.credentials.secret-access-key:}") String secretAccessKey
    ) {
        return KmsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider(accessKeyId, secretAccessKey))
                .build();
    }

    private AwsCredentialsProvider credentialsProvider(String accessKeyId, String secretAccessKey) {
        if (accessKeyId.isBlank() || secretAccessKey.isBlank()) {
            return DefaultCredentialsProvider.create();
        }

        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKeyId, secretAccessKey)
        );
    }
}
