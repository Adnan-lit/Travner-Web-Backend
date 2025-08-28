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

        // Allow frontend origins - development ports
        config.addAllowedOrigin("http://localhost:3000"); // React default
        config.addAllowedOrigin("http://localhost:4200"); // Angular default
        config.addAllowedOrigin("http://localhost:4201"); // Angular alternative
        config.addAllowedOrigin("http://localhost:5173"); // Vite default
        config.addAllowedOrigin("http://localhost:8080"); // Common dev port
        config.addAllowedOrigin("http://localhost:8081"); // Alternative port
        config.addAllowedOrigin("http://localhost:9000"); // Additional dev port

        // Allow 127.0.0.1 variants
        config.addAllowedOrigin("http://127.0.0.1:3000");
        config.addAllowedOrigin("http://127.0.0.1:4200");
        config.addAllowedOrigin("http://127.0.0.1:4201");
        config.addAllowedOrigin("http://127.0.0.1:5173");
        config.addAllowedOrigin("http://127.0.0.1:8080");
        config.addAllowedOrigin("http://127.0.0.1:8081");

        // Production domains - Vercel deployment
        config.addAllowedOrigin("https://travner.vercel.app"); // Primary domain
        config.addAllowedOrigin("https://travner-b372v97oy-jabale-nur-adnans-projects.vercel.app"); // Deployment URL

        // Add wildcard for Vercel preview deployments (branch previews)
        config.addAllowedOriginPattern("https://travner-*.vercel.app");
        config.addAllowedOriginPattern("https://*-jabale-nur-adnans-projects.vercel.app");

        // Allow all HTTP methods
        config.addAllowedMethod("*");

        // Allow all headers
        config.addAllowedHeader("*");

        // Expose headers that frontend might need
        config.addExposedHeader("Authorization");
        config.addExposedHeader("Content-Type");
        config.addExposedHeader("X-Requested-With");
        config.addExposedHeader("Accept");
        config.addExposedHeader("Origin");
        config.addExposedHeader("Access-Control-Request-Method");
        config.addExposedHeader("Access-Control-Request-Headers");

        // CRITICAL: Allow credentials for authentication (Basic Auth, cookies, etc.)
        config.setAllowCredentials(true);

        // Set max age for preflight requests (1 hour)
        config.setMaxAge(3600L);

        // Apply to all endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
