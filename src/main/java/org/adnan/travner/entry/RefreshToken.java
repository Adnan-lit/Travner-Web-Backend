package org.adnan.travner.entry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * A hashed, single-use refresh token. The raw token is only ever returned to
 * the client in a cookie; the database stores its SHA-256 digest.
 */
@Document(collection = "refresh_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    private ObjectId id;

    @Indexed(unique = true)
    private String tokenHash;

    @Indexed
    private String userId;

    private Instant createdAt;

    private Instant expiresAt;

    private Instant revokedAt;

    /** Hash of the replacement token, set when this token is rotated. */
    private String replacedByHash;
}
