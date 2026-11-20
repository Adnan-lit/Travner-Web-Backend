package org.adnan.travner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Public author/user summary. Deliberately excludes email and every other
 * private field; use {@link UserDetailDTO} for the user's own or admin views.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummaryDTO {
    private String id;
    private String userName;
    private String firstName;
    private String lastName;
    private String bio;
    private String location;
    private String profileImageUrl;
    private List<String> roles;
}
