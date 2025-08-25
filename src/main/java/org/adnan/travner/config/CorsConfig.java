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

        // Allow frontend origins
        config.addAllowedOrigin("http://localhost:4200");
        config.addAllowedOrigin("http://localhost:4201");
//        config.addAllowedOrigin("https://your-frontend-domain.com"); // Add your production domain

        // Allow all HTTP methods
        config.addAllowedMethod("*");

        // Allow all headers
        config.addAllowedHeader("*");

        // CRITICAL: Allow credentials for Basic Auth
        config.setAllowCredentials(true);

        // Apply to all endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
