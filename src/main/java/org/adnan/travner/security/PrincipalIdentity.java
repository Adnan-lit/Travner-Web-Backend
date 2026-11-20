package org.adnan.travner.security;

import org.adnan.travner.exception.ForbiddenException;
import org.springframework.security.core.Authentication;

import java.security.Principal;
import java.util.Optional;

/**
 * Resolves the stable user ID from an authenticated principal. JWT and STOMP
 * principals carry the database ID in {@link UserPrincipal#getId()} (the token
 * subject); {@code getName()} is only the display username and must never be
 * used as a chat participant or sender ID (D-06 / P2-02).
 */
public final class PrincipalIdentity {

    private static final String OBJECT_ID_PATTERN = "[0-9a-fA-F]{24}";

    private PrincipalIdentity() {
    }

    /** Returns the user ID when the principal carries one, otherwise empty. */
    public static Optional<String> findUserId(Principal principal) {
        if (principal == null) {
            return Optional.empty();
        }
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof UserPrincipal userPrincipal
                && userPrincipal.getId() != null && !userPrincipal.getId().isBlank()) {
            return Optional.of(userPrincipal.getId());
        }
        // Legacy principals whose name is already an ObjectId string.
        String name = principal.getName();
        if (name != null && name.matches(OBJECT_ID_PATTERN)) {
            return Optional.of(name);
        }
        return Optional.empty();
    }

    /** Strict variant for REST/STOMP handlers that require a user ID. */
    public static String requireUserId(Principal principal) {
        return findUserId(principal).orElseThrow(
                () -> new ForbiddenException("Authenticated user has no user ID"));
    }
}
