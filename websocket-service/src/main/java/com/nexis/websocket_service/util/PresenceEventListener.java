package com.nexis.websocket_service.util;

import com.nexis.websocket_service.config.type.PresenceType;
import com.nexis.websocket_service.payload.PresencePayload;
import com.nexis.websocket_service.service.pub_sub.RedisMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class PresenceEventListener {
    private final RedisTemplate<String,Object> redisTemplate;
    private final RedisMessagePublisher redisMessagePublisher;

    @EventListener
    public void handleSessionJoinEvent(SessionSubscribeEvent event){
       StompHeaderAccessor stompHeaderAccessor = StompHeaderAccessor.wrap(event.getMessage());
       String sessionId =  stompHeaderAccessor.getSessionId();
       String destination = stompHeaderAccessor.getDestination();   // "/topic/workspace/123/presence"

       if(destination!=null && destination.endsWith("/presence")){
           try{
               // ["", "topic", "workspace", "123", "presence"]
               String[] parts = destination.split("/");
               String workspaceId = parts[3];

               String userIdstr = stompHeaderAccessor.getFirstNativeHeader("userId");

               if (userIdstr == null) return; // Prevent crashes if header is missing during testing

               UUID userId = UUID.fromString(userIdstr);

               log.info("User {} joined workspace {} (Session: {})", userId, workspaceId, sessionId);

               redisTemplate.opsForValue().set("nexis:session:" + sessionId, workspaceId + ":" + userId, 12, TimeUnit.HOURS);

               redisTemplate.opsForSet().add("nexis:workspace:" + workspaceId + ":users", userId);

               PresencePayload payload = new PresencePayload(userId, PresenceType.JOINED);
               redisMessagePublisher.publish("nexis:workspace:" + workspaceId + ":presence", payload);
           }catch (Exception e) {
               log.error("Error processing subscribe event", e);
           }
       }
    }

    @EventListener
    public void handleSessionDisconnectEvent(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        String sessionData = (String) redisTemplate.opsForValue().get("nexis:session:" + sessionId);

        if (sessionData != null) {
            String[] parts = sessionData.split(":");
            String workspaceId = parts[0];
            String userIdstr = parts[1];

            UUID userId = UUID.fromString(userIdstr);

            log.info("User {} left workspace {} (Session: {})", userId, workspaceId, sessionId);

            // 1. Remove them from the active Workspace Set
            redisTemplate.opsForSet().remove("nexis:workspace:" + workspaceId + ":users", userId);

            // 2. Delete the session mapping to clean up RAM
            redisTemplate.delete("nexis:session:" + sessionId);

            // 3. Broadcast the "LEFT" event to everyone else
            PresencePayload payload = new PresencePayload(userId, PresenceType.LEFT);
            redisMessagePublisher.publish("nexis:workspace:" + workspaceId + ":presence", payload);
        }
    }
}
