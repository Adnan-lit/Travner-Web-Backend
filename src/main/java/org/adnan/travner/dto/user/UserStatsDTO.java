package org.adnan.travner.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user statistics
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStatsDTO {
    private long postsCount;
    private long followersCount;
    private long followingCount;
    private long commentsCount;
    private long likesReceived;
    private String memberSince;
}



