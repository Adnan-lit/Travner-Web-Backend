package org.adnan.travner.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies the central CORS allowlist used by Spring Security. */
class CorsConfigTest {

    @Test
    void onlyConfiguredOriginsAreAllowedWithCredentials() {
        CorsConfig config = config("https://app.example,https://preview.example");

        CorsConfiguration cors = config.corsConfigurationSource()
                .getCorsConfiguration(new MockHttpServletRequest());

        assertEquals(List.of("https://app.example", "https://preview.example"), cors.getAllowedOrigins());
        assertTrue(cors.getAllowCredentials());
        assertEquals(List.of("Content-Type", "Authorization"), cors.getAllowedHeaders());
        assertEquals("https://app.example",
                cors.checkOrigin("https://app.example"));
        assertFalse(cors.getAllowedOrigins().contains("*"));
    }

    @Test
    void unknownOriginIsRejected() {
        CorsConfiguration cors = config("https://app.example").corsConfigurationSource()
                .getCorsConfiguration(new MockHttpServletRequest());

        assertNull(cors.checkOrigin("https://evil.example"));
    }

    private CorsConfig config(String origins) {
        CorsConfig config = new CorsConfig();
        ReflectionTestUtils.setField(config, "allowedOriginsProp", origins);
        ReflectionTestUtils.setField(config, "allowedMethodsProp", "GET,POST,PUT,DELETE,PATCH,OPTIONS");
        ReflectionTestUtils.setField(config, "allowedHeadersProp", "Content-Type,Authorization");
        ReflectionTestUtils.setField(config, "allowCredentials", true);
        return config;
    }
}
