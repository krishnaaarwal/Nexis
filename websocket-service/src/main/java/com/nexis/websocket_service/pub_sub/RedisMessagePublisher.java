package com.nexis.websocket_service.pub_sub;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisMessagePublisher {
    private final RedisTemplate<String, Object> redisTemplate;  //It handles connection pooling, serialization, everything

    public void publish(String channel, Object payload) {
        // Broadcasts the message to the Redis channel
        redisTemplate.convertAndSend(channel, payload);
        //            ↑ This method does TWO things:
        //            1. "convert"  → serializes payload (Object → JSON bytes)
        //            2. "AndSend" → publishes to Redis channel
    }
}
