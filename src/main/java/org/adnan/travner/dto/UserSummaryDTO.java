package org.adnan.travner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummaryDTO {
    private String id;
    private String userName;
    private String firstName;
    private String lastName;
    private String email;
    private String bio;
    private String location;
    private String profileImageUrl;
    private List<String> roles;
}
