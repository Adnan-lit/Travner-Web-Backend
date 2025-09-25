package org.adnan.travner.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filter to validate URL encoding and provide better error messages
 */
@Slf4j
@Component
public class UrlValidationFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String queryString = httpRequest.getQueryString();

        try {
            // Check for common problematic characters in query parameters
            if (queryString != null && containsInvalidCharacters(queryString)) {
                log.warn("Invalid characters detected in query string: {}", queryString);

                httpResponse.setStatus(400);
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write(
                        "{\"success\": false, \"message\": \"Invalid URL encoding. Special characters like [, ], {, } must be properly URL encoded.\", \"hint\": \"Use encodeURIComponent() in JavaScript or similar encoding method.\"}");
                return;
            }

            chain.doFilter(request, response);

        } catch (Exception e) {
            log.error("Error in URL validation filter: {}", e.getMessage());
            chain.doFilter(request, response);
        }
    }

    private boolean containsInvalidCharacters(String queryString) {
        // Check for common unencoded characters that cause issues
        return queryString.contains("[") ||
                queryString.contains("]") ||
                queryString.contains("{") ||
                queryString.contains("}") ||
                queryString.contains(" ") || // Space should be %20
                queryString.contains("\""); // Quote should be encoded
    }
}