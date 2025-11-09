package com.ecom.profile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.ecom.jwt.config.JwtValidationProperties;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling // For JWKS cache refresh (required by jwt-validation-starter)
@EnableConfigurationProperties(JwtValidationProperties.class)
public class UserProfileApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserProfileApplication.class, args);
    }
}

