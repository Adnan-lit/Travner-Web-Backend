package org.adnan.travner.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Central CORS configuration.
 * Reads values from application.yml under app.cors.* so that environments can
 * override
 * allowed origins without code changes.
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:4200,http://localhost:5173,https://travner.vercel.app}")
    private String allowedOriginsProp;

    @Value("${app.cors.allowed-methods:GET,POST,PUT,DELETE,PATCH,OPTIONS}")
    private String allowedMethodsProp;

    @Value("${app.cors.allowed-headers:*}")
    private String allowedHeadersProp;

    @Value("${app.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Origins: support both exact matches and wildcard patterns via
        // setAllowedOriginPatterns
        List<String> origins = splitAndTrim(allowedOriginsProp);
        // If any origin contains a wildcard, use patterns; otherwise set as exact
        // origins
        if (origins.stream().anyMatch(o -> o.contains("*"))) {
            config.setAllowedOriginPatterns(origins);
        } else {
            origins.forEach(config::addAllowedOrigin);
        }

        // Methods
        splitAndTrim(allowedMethodsProp).forEach(config::addAllowedMethod);

        // Headers
        splitAndTrim(allowedHeadersProp).forEach(config::addAllowedHeader);

        config.setAllowCredentials(allowCredentials);
        // Cache preflight response for 1 hour to reduce OPTIONS traffic
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    private List<String> splitAndTrim(String csv) {
        if (!StringUtils.hasText(csv))
            return List.of();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
