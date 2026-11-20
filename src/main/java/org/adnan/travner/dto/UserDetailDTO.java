package org.adnan.travner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.adnan.travner.entry.UserEntry;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Safe, explicit view of a user for self-service and admin endpoints.
 *
 * <p>Never return {@link UserEntry} directly: it is a persistence object whose
 * password hash and other internal fields must not become part of the HTTP
 * contract. Mapping is intentional so adding a sensitive field to the entity
 * cannot silently leak it through an existing endpoint.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDetailDTO {

    private String id;
    private String userName;
    private String email;
    private String firstName;
    private String lastName;
    private String bio;
    private String location;
    private String profileImageUrl;
    private List<String> roles;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    public static UserDetailDTO from(UserEntry user) {
        if (user == null) {
            return null;
        }
        return UserDetailDTO.builder()
                .id(user.getId() != null ? user.getId().toString() : null)
                .userName(user.getUserName())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .bio(user.getBio())
                .location(user.getLocation())
                .profileImageUrl(user.getProfileImageUrl())
                .roles(user.getRoles())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
