package com.nexis.websocket_service.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor stompHeaderAccessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if(stompHeaderAccessor!=null && StompCommand.CONNECT.equals(stompHeaderAccessor.getCommand())){
            String authHeader = stompHeaderAccessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("WebSocket connection rejected: Missing or invalid Authorization header");
                throw new IllegalArgumentException("Unauthorized: Missing JWT");
            }

            String token = authHeader.substring(7);

            try{
                jwtUtil.validateToken(token);
                String userId = jwtUtil.getUserIdFromToken(token);

                stompHeaderAccessor.setUser(()->userId);
            } catch (Exception e) {
                log.error("WebSocket connection rejected: Invalid JWT", e);
                throw new MessagingException("Invalid JWT");
            }

        }


        return message;
    }
}
