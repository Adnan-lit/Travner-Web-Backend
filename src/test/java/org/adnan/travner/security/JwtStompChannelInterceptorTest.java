package org.adnan.travner.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Verifies that STOMP CONNECT frames carry the same JWT identity as REST calls. */
class JwtStompChannelInterceptorTest {

    private static final String USER_ID = "507f1f77bcf86cd799439011";

    private JwtDecoder jwtDecoder;
    private JwtStompChannelInterceptor interceptor;
    private MessageChannel channel;

    @BeforeEach
    void setUp() {
        jwtDecoder = mock(JwtDecoder.class);
        interceptor = new JwtStompChannelInterceptor(jwtDecoder);
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
    void nonConnectFrame_isIgnored() {
        StompHeaderAccessor accessor = accessor(StompCommand.SEND, "Bearer valid-token");

        interceptor.preSend(message(accessor), channel);

        assertNull(accessor.getUser());
        verifyNoInteractions(jwtDecoder);
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
