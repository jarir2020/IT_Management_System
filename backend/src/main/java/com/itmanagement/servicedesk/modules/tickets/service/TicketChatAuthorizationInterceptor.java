package com.itmanagement.servicedesk.modules.tickets.service;

import com.itmanagement.servicedesk.modules.users.model.UserAccount;
import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class TicketChatAuthorizationInterceptor implements ChannelInterceptor {
    private static final Pattern SEND_DESTINATION = Pattern.compile("^/app/tickets/(\\d+)/chat$");
    private static final Pattern SUBSCRIBE_DESTINATION = Pattern.compile("^/topic/tickets/(\\d+)/chat$");

    private final TicketService tickets;

    public TicketChatAuthorizationInterceptor(TicketService tickets) {
        this.tickets = tickets;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        if (command == StompCommand.CONNECT) {
            requireAuthenticated(accessor.getUser());
            return message;
        }
        if (command != StompCommand.SEND && command != StompCommand.SUBSCRIBE) return message;

        Principal principal = accessor.getUser();
        UserAccount actor = requireAuthenticated(principal);
        String destination = accessor.getDestination();
        Pattern pattern = command == StompCommand.SEND ? SEND_DESTINATION : SUBSCRIBE_DESTINATION;
        Matcher matcher = destination == null ? null : pattern.matcher(destination);
        if (matcher == null || !matcher.matches()) {
            throw new AccessDeniedException("Only authorized ticket chat destinations are available");
        }
        if (command == StompCommand.SEND && !hasAuthority(actor, "tickets:comment")) {
            throw new AccessDeniedException("You cannot post in ticket chat");
        }
        tickets.assertCanReadTicket(Long.valueOf(matcher.group(1)), actor);
        return message;
    }

    private static UserAccount requireAuthenticated(Principal principal) {
        UserAccount actor = principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof UserAccount account ? account : null;
        if (actor != null && actor.isEnabled()) return actor;
        throw new AccessDeniedException("Sign in to use ticket chat");
    }

    private static boolean hasAuthority(UserAccount actor, String authority) {
        return actor.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(authority::equals);
    }
}
