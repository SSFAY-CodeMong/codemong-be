package com.codemong.be.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.kms.KmsClient;

@Configuration
public class KmsConfig {

    @Bean
    public KmsClient kmsClient() {
        return KmsClient.create();
    }
}