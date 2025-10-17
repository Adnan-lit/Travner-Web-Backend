package org.adnan.travner.util;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * Utility class for sanitizing user input to prevent XSS and injection attacks
 */
@Component
public class InputSanitizer {

    // Patterns for various types of content
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern SCRIPT_PATTERN = Pattern.compile("(?i)<script[^>]*>.*?</script>");
    private static final Pattern JAVASCRIPT_PATTERN = Pattern.compile("(?i)javascript:");
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile("(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute)");
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[<>\"'&]");

    /**
     * Sanitize text input by removing HTML tags and potentially dangerous content
     */
    public String sanitizeText(String input) {
        if (!StringUtils.hasText(input)) {
            return input;
        }

        String sanitized = input.trim();
        
        // Remove HTML tags
        sanitized = HTML_TAG_PATTERN.matcher(sanitized).replaceAll("");
        
        // Remove script tags
        sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        
        // Remove javascript: protocols
        sanitized = JAVASCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        
        // Escape special characters
        sanitized = escapeHtml(sanitized);
        
        return sanitized;
    }

    /**
     * Sanitize HTML content (allows some HTML but removes dangerous elements)
     */
    public String sanitizeHtml(String input) {
        if (!StringUtils.hasText(input)) {
            return input;
        }

        String sanitized = input.trim();
        
        // Remove script tags and javascript protocols
        sanitized = SCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = JAVASCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        
        return sanitized;
    }

    /**
     * Sanitize search queries to prevent injection attacks
     */
    public String sanitizeSearchQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return query;
        }

        String sanitized = query.trim();
        
        // Remove SQL injection patterns
        sanitized = SQL_INJECTION_PATTERN.matcher(sanitized).replaceAll("");
        
        // Remove special characters that could be used for injection
        sanitized = sanitized.replaceAll("[;\"'\\\\]", "");
        
        return sanitized;
    }

    /**
     * Sanitize username input
     */
    public String sanitizeUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return username;
        }

        String sanitized = username.trim().toLowerCase();
        
        // Remove special characters except underscores
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9_]", "");
        
        return sanitized;
    }

    /**
     * Sanitize email input
     */
    public String sanitizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return email;
        }

        String sanitized = email.trim().toLowerCase();
        
        // Basic email validation and sanitization
        if (!sanitized.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        
        return sanitized;
    }

    /**
     * Sanitize URL input
     */
    public String sanitizeUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return url;
        }

        String sanitized = url.trim();
        
        // Remove javascript: and data: protocols
        sanitized = sanitized.replaceAll("(?i)javascript:", "");
        sanitized = sanitized.replaceAll("(?i)data:", "");
        
        // Basic URL validation
        if (!sanitized.matches("^(https?://)?[\\w\\-]+(\\.[\\w\\-]+)+([\\w\\-\\.,@?^=%&:/~\\+#]*[\\w\\-\\@?^=%&/~\\+#])?$")) {
            throw new IllegalArgumentException("Invalid URL format");
        }
        
        return sanitized;
    }

    /**
     * Escape HTML special characters
     */
    private String escapeHtml(String input) {
        return input.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }

    /**
     * Validate and sanitize file name
     */
    public String sanitizeFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            throw new IllegalArgumentException("File name cannot be empty");
        }

        String sanitized = fileName.trim();
        
        // Remove path traversal attempts
        sanitized = sanitized.replaceAll("\\.\\./", "");
        sanitized = sanitized.replaceAll("\\.\\.\\\\", "");
        
        // Remove special characters
        sanitized = sanitized.replaceAll("[<>:\"|?*]", "");
        
        // Limit length
        if (sanitized.length() > 255) {
            sanitized = sanitized.substring(0, 255);
        }
        
        return sanitized;
    }

    /**
     * Check if input contains potentially dangerous content
     */
    public boolean containsDangerousContent(String input) {
        if (!StringUtils.hasText(input)) {
            return false;
        }

        return SCRIPT_PATTERN.matcher(input).find() ||
               JAVASCRIPT_PATTERN.matcher(input).find() ||
               SQL_INJECTION_PATTERN.matcher(input).find();
    }
}
