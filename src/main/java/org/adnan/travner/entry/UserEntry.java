package org.adnan.travner.entry;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEntry {

    @Id
    private ObjectId id;
    @Indexed(unique = true)
    @NonNull
    private String userName;
    private String firstName;
    private String lastName;
    private String email;
    @NonNull
    private String password;
    private List<String> roles;

    // Profile enhancement fields
    private String bio;
    private String profileImageUrl;
    private String location;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private boolean isActive = true;
}
