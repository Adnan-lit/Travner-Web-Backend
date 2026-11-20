package org.adnan.travner.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/**
 * Central validation for client-controlled pagination and sorting parameters.
 * All failures throw {@link IllegalArgumentException}, which
 * {@code GlobalExceptionHandler} maps to HTTP 400 before controller bodies run.
 */
public final class RequestValidation {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private RequestValidation() {
    }

    /** Zero-based page index; negative pages are rejected. */
    public static int page(int page) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be zero or greater");
        }
        return page;
    }

    /** Positive page size, clamped to {@link #MAX_SIZE} so queries stay bounded. */
    public static int size(int size) {
        if (size < 1) {
            throw new IllegalArgumentException("size must be at least 1");
        }
        return Math.min(size, MAX_SIZE);
    }

    /** Accepts only {@code asc}/{@code desc}, case-insensitively. */
    public static Sort.Direction direction(String direction) {
        if (direction == null || direction.isBlank() || direction.equalsIgnoreCase("desc")) {
            return Sort.Direction.DESC;
        }
        if (direction.equalsIgnoreCase("asc")) {
            return Sort.Direction.ASC;
        }
        throw new IllegalArgumentException("direction must be 'asc' or 'desc'");
    }

    /** Rejects sort fields outside the endpoint's whitelist instead of sorting on a non-existent key. */
    public static String sortBy(String sortBy, Set<String> allowed, String defaultField) {
        String field = (sortBy == null || sortBy.isBlank()) ? defaultField : sortBy;
        if (!allowed.contains(field)) {
            throw new IllegalArgumentException("Unsupported sort field: " + field);
        }
        return field;
    }

    /** Validated pageable with a whitelisted sort field and direction. */
    public static Pageable pageable(int page, int size, String sortBy, String direction,
                                    Set<String> allowed, String defaultField) {
        return PageRequest.of(page(page), size(size),
                Sort.by(direction(direction), sortBy(sortBy, allowed, defaultField)));
    }

    /** Validated pageable without sorting. */
    public static Pageable pageable(int page, int size) {
        return PageRequest.of(page(page), size(size));
    }
}
