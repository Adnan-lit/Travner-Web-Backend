package org.adnan.travner.security;

import org.adnan.travner.entry.ChatConversation;
import org.adnan.travner.repository.ChatConversationRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Authenticates STOMP CONNECT frames with the same bearer access token used by
 * REST calls, then rejects anonymous SEND/SUBSCRIBE frames and conversation
 * subscriptions the user is not a participant of.
 */
@Component
public class JwtStompChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final Pattern CONVERSATION_DESTINATION =
            Pattern.compile("^/topic/conversation/([^/]+)(?:/typing)?$");

    private final JwtDecoder jwtDecoder;
    private final ChatConversationRepository conversationRepository;
    private final Converter<Jwt, ? extends Authentication> authenticationConverter;

    public JwtStompChannelInterceptor(JwtDecoder jwtDecoder,
                                      ChatConversationRepository conversationRepository) {
        this.jwtDecoder = jwtDecoder;
        this.conversationRepository = conversationRepository;
        this.authenticationConverter = new JwtAuthenticationConverter();
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticateConnect(accessor);
            return message;
        }

        if (StompCommand.SEND.equals(accessor.getCommand())
                || StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            Principal user = accessor.getUser();
            if (!(user instanceof Authentication) || "anonymousUser".equals(user.getName())) {
                throw new AccessDeniedException("WebSocket authentication required");
            }
            if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                authorizeConversationSubscription(accessor.getDestination(), user.getName());
            }
        }
        return message;
    }

    private void authenticateConnect(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return;
        }
        try {
            Jwt jwt = jwtDecoder.decode(authorization.substring(BEARER_PREFIX.length()));
            accessor.setUser(authenticationConverter.convert(jwt));
        } catch (JwtException | IllegalArgumentException ex) {
            // Leave the session unauthenticated; SEND/SUBSCRIBE will be rejected.
            // No token details are logged.
        }
    }

    private void authorizeConversationSubscription(String destination, String username) {
        if (destination == null) {
            return;
        }
        Matcher matcher = CONVERSATION_DESTINATION.matcher(destination);
        if (!matcher.matches()) {
            return; // e.g. /user/queue/messages is scoped by the broker to this user
        }

        String conversationId = matcher.group(1);
        ChatConversation conversation = conversationRepository.findById(conversationId).orElse(null);
        if (conversation == null || conversation.getParticipantIds() == null
                || !conversation.getParticipantIds().contains(username)) {
            throw new AccessDeniedException("Not a participant in this conversation");
        }
    }
}
