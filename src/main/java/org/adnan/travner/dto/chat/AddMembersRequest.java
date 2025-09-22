package org.adnan.travner.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for adding members to a conversation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddMembersRequest {

    @NotBlank(message = "Conversation ID is required")
    private String conversationId;

    @NotEmpty(message = "At least one user ID is required")
    @Size(max = 10, message = "Cannot add more than 10 members at once")
    private List<String> userIds;
}