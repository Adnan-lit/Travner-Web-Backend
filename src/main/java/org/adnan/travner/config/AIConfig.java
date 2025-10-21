package org.adnan.travner.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for AI Service
 * Note: ObjectMapper is auto-configured by Spring Boot, no need to create a bean
 */
@Configuration
@EnableCaching
public class AIConfig {
    // ObjectMapper is automatically provided by Spring Boot
    // No need to define it here to avoid conflicts
}


