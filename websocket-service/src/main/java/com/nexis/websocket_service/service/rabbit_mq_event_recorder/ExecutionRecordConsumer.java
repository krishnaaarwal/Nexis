package com.nexis.websocket_service.service.rabbit_mq_event_recorder;

import com.nexis.websocket_service.config.RabbitMqConfig;
import com.nexis.websocket_service.payload.ExecutionResultPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExecutionRecordConsumer {

    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = RabbitMqConfig.RESULT_QUEUE)
    public void consumeExecutionResult(ExecutionResultPayload result) {
        log.info("Received execution result for workspace: {}", result.getWorkspaceId());

        // Push the result to everyone subscribed to this specific workspace's terminal channel
        messagingTemplate.convertAndSend(
                "/topic/workspace/" + result.getWorkspaceId() + "/terminal",
                result
        );
    }
}
