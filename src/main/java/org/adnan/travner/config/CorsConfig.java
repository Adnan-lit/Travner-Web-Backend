package org.adnan.travner.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow frontend origins - common localhost ports
        config.addAllowedOrigin("http://localhost:3000"); // React default
        config.addAllowedOrigin("http://localhost:4200"); // Angular default
        config.addAllowedOrigin("http://localhost:4201");
        config.addAllowedOrigin("http://localhost:5173"); // Vite default
        config.addAllowedOrigin("http://localhost:8080"); // Common dev port
        config.addAllowedOrigin("http://localhost:8081"); // Alternative port
        config.addAllowedOrigin("http://127.0.0.1:3000");
        config.addAllowedOrigin("http://127.0.0.1:4200");
        config.addAllowedOrigin("http://127.0.0.1:5173");
        // config.addAllowedOrigin("https://your-frontend-domain.com"); // Add your
        // production domain

        // Allow all HTTP methods
        config.addAllowedMethod("*");

        // Allow all headers
        config.addAllowedHeader("*");

        // Expose common headers that frontend might need
        config.addExposedHeader("Authorization");
        config.addExposedHeader("Content-Type");
        config.addExposedHeader("X-Requested-With");
        config.addExposedHeader("Accept");
        config.addExposedHeader("Origin");
        config.addExposedHeader("Access-Control-Request-Method");
        config.addExposedHeader("Access-Control-Request-Headers");

        // CRITICAL: Allow credentials for Basic Auth
        config.setAllowCredentials(true);

        // Set max age for preflight requests (1 hour)
        config.setMaxAge(3600L);

        // Apply to all endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
