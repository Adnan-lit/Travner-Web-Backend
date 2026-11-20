package org.adnan.travner.service;

import org.adnan.travner.entry.FollowEntry;
import org.adnan.travner.entry.UserEntry;
import org.adnan.travner.exception.ResourceNotFoundException;
import org.adnan.travner.repository.FollowRepository;
import org.adnan.travner.repository.UserRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regression tests for P2-01. Follow operations take database IDs (never
 * usernames), validate their arguments, and surface typed errors so the
 * controller can map them to 400/404 instead of swallowing them.
 */
@ExtendWith(MockitoExtension.class)
class UserFollowServiceTest {

    private static final String MALFORMED_ID = "not-an-object-id";

    @Mock
    private UserRepository userRepository;
    @Mock
    private FollowRepository followRepository;

    @InjectMocks
    private UserService userService;

    private ObjectId followerId;
    private ObjectId targetId;
    private String followerHex;
    private String targetHex;

    @BeforeEach
    void setUp() {
        followerId = new ObjectId();
        targetId = new ObjectId();
        followerHex = followerId.toHexString();
        targetHex = targetId.toHexString();
    }

    @Test
    void follow_createsRelationship_whenBothUsersExist() {
        when(userRepository.existsById(followerId)).thenReturn(true);
        when(userRepository.existsById(targetId)).thenReturn(true);
        when(followRepository.existsByFollowerIdAndFollowingId(followerId, targetId)).thenReturn(false);

        userService.followUser(followerHex, targetHex);

        verify(followRepository).save(any(FollowEntry.class));
    }

    @Test
    void follow_isIdempotent_whenAlreadyFollowing() {
        when(userRepository.existsById(followerId)).thenReturn(true);
        when(userRepository.existsById(targetId)).thenReturn(true);
        when(followRepository.existsByFollowerIdAndFollowingId(followerId, targetId)).thenReturn(true);

        assertDoesNotThrow(() -> userService.followUser(followerHex, targetHex));

        verify(followRepository, never()).save(any(FollowEntry.class));
    }

    @Test
    void follow_treatsConcurrentDuplicateKeyAsSuccess() {
        when(userRepository.existsById(followerId)).thenReturn(true);
        when(userRepository.existsById(targetId)).thenReturn(true);
        when(followRepository.existsByFollowerIdAndFollowingId(followerId, targetId)).thenReturn(false);
        doThrow(new DuplicateKeyException("duplicate key"))
                .when(followRepository).save(any(FollowEntry.class));

        assertDoesNotThrow(() -> userService.followUser(followerHex, targetHex));
    }

    @Test
    void follow_rejectsSelfFollow() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.followUser(followerHex, followerHex));

        assertTrue(ex.getMessage().contains("themselves"));
        verify(followRepository, never()).save(any(FollowEntry.class));
    }

    @Test
    void follow_rejectsMissingTargetWith404Type() {
        when(userRepository.existsById(followerId)).thenReturn(true);
        when(userRepository.existsById(targetId)).thenReturn(false);

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> userService.followUser(followerHex, targetHex));

        assertTrue(ex.getMessage().contains(targetHex));
        verify(followRepository, never()).save(any(FollowEntry.class));
    }

    @Test
    void follow_rejectsMissingAuthenticatedUserWith404Type() {
        when(userRepository.existsById(followerId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> userService.followUser(followerHex, targetHex));

        verify(followRepository, never()).save(any(FollowEntry.class));
    }

    @Test
    void follow_rejectsMalformedFollowerId() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.followUser(MALFORMED_ID, targetHex));

        assertEquals("Invalid user ID: " + MALFORMED_ID, ex.getMessage());
        verifyNoRepositoryInteractions();
    }

    @Test
    void follow_rejectsMalformedTargetId() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.followUser(followerHex, MALFORMED_ID));

        verifyNoRepositoryInteractions();
    }

    @Test
    void follow_rejectsBlankId() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.followUser("  ", targetHex));

        verifyNoRepositoryInteractions();
    }

    @Test
    void unfollow_deletesRelationship() {
        when(userRepository.existsById(targetId)).thenReturn(true);

        userService.unfollowUser(followerHex, targetHex);

        verify(followRepository).deleteByFollowerIdAndFollowingId(followerId, targetId);
    }

    @Test
    void unfollow_isIdempotent_whenRelationshipMissing() {
        when(userRepository.existsById(targetId)).thenReturn(true);

        assertDoesNotThrow(() -> userService.unfollowUser(followerHex, targetHex));

        verify(followRepository).deleteByFollowerIdAndFollowingId(followerId, targetId);
    }

    @Test
    void unfollow_rejectsMissingTargetWith404Type() {
        when(userRepository.existsById(targetId)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> userService.unfollowUser(followerHex, targetHex));

        verify(followRepository, never()).deleteByFollowerIdAndFollowingId(any(), any());
    }

    @Test
    void unfollow_rejectsMalformedId() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.unfollowUser(followerHex, MALFORMED_ID));

        verify(followRepository, never()).deleteByFollowerIdAndFollowingId(any(), any());
    }

    @Test
    void isFollowing_returnsTrue_whenRelationshipExists() {
        when(followRepository.existsByFollowerIdAndFollowingId(followerId, targetId)).thenReturn(true);

        assertTrue(userService.isFollowing(followerHex, targetHex));
    }

    @Test
    void isFollowing_returnsFalse_whenRelationshipMissing() {
        when(followRepository.existsByFollowerIdAndFollowingId(followerId, targetId)).thenReturn(false);

        assertFalse(userService.isFollowing(followerHex, targetHex));
    }

    @Test
    void isFollowing_propagatesMalformedIdInsteadOfSwallowingIt() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.isFollowing(followerHex, MALFORMED_ID));
    }

    @Test
    void followEntry_declaresUniqueCompoundIndex() {
        org.springframework.data.mongodb.core.index.CompoundIndex[] indexes =
                FollowEntry.class.getAnnotationsByType(
                        org.springframework.data.mongodb.core.index.CompoundIndex.class);

        assertTrue(indexes.length > 0, "FollowEntry must declare compound indexes");
        boolean hasUniquePairIndex = java.util.Arrays.stream(indexes).anyMatch(index ->
                index.unique()
                        && index.def().contains("followerId")
                        && index.def().contains("followingId"));
        assertTrue(hasUniquePairIndex,
                "The (followerId, followingId) compound index must be unique");
    }

    private void verifyNoRepositoryInteractions() {
        verify(followRepository, never()).save(any());
        verify(followRepository, never()).deleteByFollowerIdAndFollowingId(any(), any());
        verify(followRepository, never()).existsByFollowerIdAndFollowingId(any(), any());
    }
}
