package com.ecom.profile.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Application Configuration
 * 
 * <p>Provides beans for RedisTemplate, etc.
 * 
 * <p>Note: ResilientWebClient is auto-configured by http-client-starter.
 * No need to configure RestTemplate or WebClient manually.
 */
@Configuration
public class AppConfig {

    /**
     * RedisTemplate for token blacklisting
     * Marked as @Primary to ensure it's used by jwt-validation-starter
     */
    @Bean
    @Primary
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}

