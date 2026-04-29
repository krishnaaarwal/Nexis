package com.nexis.websocket_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexis.websocket_service.payload.CodeOperation;
import com.nexis.websocket_service.service.lock.RedissonLockService;
import com.nexis.websocket_service.util.OTEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationalTransformService {

    private final RedissonLockService lockService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public CodeOperation processOperation(UUID workspaceId, CodeOperation incomingOp) {
        String historyKey = "nexis:workspace:" + workspaceId + ":history";
        String versionKey = "nexis:workspace:" + workspaceId + ":version";

        if (!lockService.acquireLock(workspaceId)) {
            log.error("Could not acquire lock for workspace: {}", workspaceId);
            throw new RuntimeException("System busy. Please retry.");
        }

        try {

            String versionStr = (String) redisTemplate.opsForValue().get(versionKey);
            int serverVersion = (versionStr != null) ? Integer.parseInt(versionStr) : 0;

            List<Object> rawHistory = redisTemplate.opsForList().range(historyKey, 0, -1);
            if (rawHistory != null && !rawHistory.isEmpty()) {
                List<CodeOperation> history = rawHistory.stream()
                        .map(obj -> objectMapper.convertValue(obj, CodeOperation.class))
                        .collect(Collectors.toList());

                for (CodeOperation historicalOp : history) {
                    if (historicalOp.getVersion() >= incomingOp.getVersion()) {
                        incomingOp = OTEngine.transform(incomingOp, historicalOp);
                    }
                }
            }

            int newVersion = serverVersion + 1;
            incomingOp.setVersion(newVersion);


            redisTemplate.opsForValue().set(versionKey, String.valueOf(newVersion));
            redisTemplate.opsForList().rightPush(historyKey, incomingOp);
            redisTemplate.opsForList().trim(historyKey, -100, -1);

            log.info("Workspace {} | version {} → {} | op: {} at {}",
                    workspaceId, serverVersion, newVersion,
                    incomingOp.getOperationType(), incomingOp.getPosition());

            return incomingOp;

        } finally {
            lockService.releaseLock(workspaceId);
        }
    }
}
