package com.nexis.websocket_service.service.pub_sub;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
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

            String publishedMessage = new String(message.getBody());
            String channel = new String(message.getChannel());
            String[] parts = channel.split(":");  // Split by ":" → ["nexis", "workspace", "123", "cursor"]

            String destination;

            // Pattern: nexis:workspace:{id}:{type}
            if(parts[1].equals("workspace")){
                String workspaceId = parts[2];
                String channelType = parts[3];
                destination = "/topic/workspace/" + workspaceId + "/"+ channelType;
            }
            // Pattern: nexis:user:{id}:private
            else if (parts[1].equals("user")) {
                String userId = parts[2];
                destination = "/user/queue/" + userId + "/private";
            }else {
                return;
            }

            messagingTemplate.convertAndSend(destination, publishedMessage);

        } catch (Exception e) {
            System.err.println("Failed to process Redis message: " + e.getMessage());
        }
    }
}