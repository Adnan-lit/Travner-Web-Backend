package org.adnan.travner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    private String title;

    @NotBlank(message = "Content is required")
    @Size(min = 10, max = 10000, message = "Content must be between 10 and 10000 characters")
    private String content;

    @Size(max = 100, message = "Location must not exceed 100 characters")
    private String location;

    @Size(max = 10, message = "Maximum 10 tags allowed")
    private List<@Size(max = 50, message = "Each tag must not exceed 50 characters") String> tags;

    @NotNull(message = "Published status is required")
    private boolean published;
}
