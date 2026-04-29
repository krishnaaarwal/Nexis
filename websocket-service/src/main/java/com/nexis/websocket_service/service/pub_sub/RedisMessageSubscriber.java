package com.nexis.websocket_service.service.pub_sub;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.lang.Nullable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisMessageSubscriber implements MessageListener {
    //                                          Spring Data Redis interface
    //                                          It has ONE method: onMessage()
    //                                          Redis calls this whenever a message arrives


    private final SimpMessagingTemplate messagingTemplate;      //"Simp" = Simple Messaging Protocol.
                                                                // SimpMessagingTemplate allows us to programmatically send STOMP messages

    @Override
    public void onMessage(Message message, byte[] pattern) {   // This method fires on EVERY instance whenever Redis broadcasts
        try {
            // 1. Extract raw data from Redis
            String publishedMessage = new String(message.getBody());
            String channel = new String(message.getChannel());

            // 2. Map the Redis channel back to your WebSocket STOMP topic
            // 2. Figure out WHERE to send on WebSocket
            String workspaceId = channel.replace("nexis:workspace:", "");
            String destination = "/topic/workspace/" + workspaceId + "/code";

            // 3. Push to all WebSocket clients on THIS instance
            messagingTemplate.convertAndSend(destination, publishedMessage);

        } catch (Exception e) {
            // Handle deserialization errors
            System.err.println("Failed to process Redis message: " + e.getMessage());
        }
    }
}