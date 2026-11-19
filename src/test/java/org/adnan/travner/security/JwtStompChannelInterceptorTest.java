package org.adnan.travner.security;

import org.adnan.travner.entry.ChatConversation;
import org.adnan.travner.repository.ChatConversationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Verifies STOMP authentication and subscription authorization. */
class JwtStompChannelInterceptorTest {

    private static final String USER_ID = "507f1f77bcf86cd799439011";

    private JwtDecoder jwtDecoder;
    private ChatConversationRepository conversationRepository;
    private JwtStompChannelInterceptor interceptor;
    private MessageChannel channel;

    @BeforeEach
    void setUp() {
        jwtDecoder = mock(JwtDecoder.class);
        conversationRepository = mock(ChatConversationRepository.class);
        interceptor = new JwtStompChannelInterceptor(jwtDecoder, conversationRepository);
        channel = mock(MessageChannel.class);
    }

    @Test
    void connectWithBearerToken_setsAuthenticatedPrincipal() {
        when(jwtDecoder.decode("valid-token")).thenReturn(jwt());

        StompHeaderAccessor accessor = accessor(StompCommand.CONNECT, "Bearer valid-token");
        Message<byte[]> message = message(accessor);

        interceptor.preSend(message, channel);

        Authentication authentication = assertInstanceOf(Authentication.class, accessor.getUser());
        UserPrincipal principal = assertInstanceOf(UserPrincipal.class, authentication.getPrincipal());
        assertEquals(USER_ID, principal.getId());
        assertEquals("alice", authentication.getName());
    }

    @Test
    void connectWithInvalidToken_leavesSessionUnauthenticated() {
        when(jwtDecoder.decode("bad-token")).thenThrow(new JwtException("invalid signature"));

        StompHeaderAccessor accessor = accessor(StompCommand.CONNECT, "Bearer bad-token");

        interceptor.preSend(message(accessor), channel);

        assertNull(accessor.getUser());
    }

    @Test
    void connectWithoutBearerHeader_doesNotAttemptDecoding() {
        StompHeaderAccessor accessor = accessor(StompCommand.CONNECT, "Basic abc");

        interceptor.preSend(message(accessor), channel);

        assertNull(accessor.getUser());
        verifyNoInteractions(jwtDecoder);
    }

    @Test
    void nonConnectFrameIsIgnoredWhenNoAuthorizationHeader() {
        StompHeaderAccessor accessor = accessor(StompCommand.DISCONNECT, null);

        interceptor.preSend(message(accessor), channel);

        assertNull(accessor.getUser());
        verifyNoInteractions(jwtDecoder);
    }

    @Test
    void anonymousSendIsRejected() {
        StompHeaderAccessor accessor = accessor(StompCommand.SEND, null);

        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message(accessor), channel));
    }

    @Test
    void participantCanSubscribeToConversationTopic() {
        when(conversationRepository.findById("conv1")).thenReturn(Optional.of(conversation("conv1", "alice")));
        StompHeaderAccessor accessor = accessor(StompCommand.SUBSCRIBE, null);
        accessor.setUser(authenticatedUser());
        accessor.setDestination("/topic/conversation/conv1");

        assertDoesNotThrow(() -> interceptor.preSend(message(accessor), channel));
    }

    @Test
    void nonParticipantCannotSubscribeToConversationTopic() {
        when(conversationRepository.findById("conv1")).thenReturn(Optional.of(conversation("conv1", "bob")));
        StompHeaderAccessor accessor = accessor(StompCommand.SUBSCRIBE, null);
        accessor.setUser(authenticatedUser());
        accessor.setDestination("/topic/conversation/conv1");

        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message(accessor), channel));
    }

    @Test
    void authenticatedUserCanSubscribeToPersonalQueue() {
        StompHeaderAccessor accessor = accessor(StompCommand.SUBSCRIBE, null);
        accessor.setUser(authenticatedUser());
        accessor.setDestination("/user/queue/messages");

        assertDoesNotThrow(() -> interceptor.preSend(message(accessor), channel));
        verifyNoInteractions(conversationRepository);
    }

    private StompHeaderAccessor accessor(StompCommand command, String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setLeaveMutable(true);
        if (authorization != null) {
            accessor.setNativeHeader("Authorization", authorization);
        }
        return accessor;
    }

    private Message<byte[]> message(StompHeaderAccessor accessor) {
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Authentication authenticatedUser() {
        return new UsernamePasswordAuthenticationToken("alice", null, List.of());
    }

    private ChatConversation conversation(String id, String participant) {
        return ChatConversation.builder()
                .id(id)
                .participantIds(List.of(participant))
                .build();
    }

    private Jwt jwt() {
        Instant now = Instant.now();
        return Jwt.withTokenValue("valid-token")
                .header("alg", "RS256")
                .subject(USER_ID)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .claim("username", "alice")
                .claim("roles", List.of("USER"))
                .build();
    }
}
