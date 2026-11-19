package org.adnan.travner.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Single source of truth for browser cross-origin rules. Spring Security's
 * {@code .cors()} picks up the {@link CorsConfigurationSource} bean; no
 * controller declares its own origin rules.
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins}")
    private String allowedOriginsProp;

    @Value("${app.cors.allowed-methods}")
    private String allowedMethodsProp;

    @Value("${app.cors.allowed-headers}")
    private String allowedHeadersProp;

    @Value("${app.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> origins = splitAndTrim(allowedOriginsProp);
        // Wildcards are only used for pattern matching when explicitly configured;
        // credentials require concrete origins.
        if (origins.stream().anyMatch(o -> o.contains("*"))) {
            config.setAllowedOriginPatterns(origins);
        } else {
            origins.forEach(config::addAllowedOrigin);
        }

        splitAndTrim(allowedMethodsProp).forEach(config::addAllowedMethod);
        splitAndTrim(allowedHeadersProp).forEach(config::addAllowedHeader);
        config.setAllowCredentials(allowCredentials);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private List<String> splitAndTrim(String csv) {
        if (!StringUtils.hasText(csv)) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
