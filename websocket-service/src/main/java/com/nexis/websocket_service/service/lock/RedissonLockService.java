package com.nexis.websocket_service.service.lock;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedissonLockService {

    private final RedissonClient redissonClient;

    public boolean acquireLock(UUID workspaceId) {
        RLock lock = redissonClient.getLock("doc:" + workspaceId + ":lock");
        try {
            // waitTime  = 500ms max wait to acquire
            // leaseTime = 5s auto-release if service crashes
            return lock.tryLock(500, 5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void releaseLock(UUID workspaceId) {
        RLock lock = redissonClient.getLock("doc:" + workspaceId + ":lock");
        // Only release if THIS thread holds it
        // prevents releasing someone else's lock
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
