package com.nexis.websocket_service.controller;


import com.nexis.websocket_service.dto.CodeOperation;
import com.nexis.websocket_service.service.OperationalTransformService;
import com.nexis.websocket_service.service.pub_sub.RedisMessagePublisher;
import com.nexis.websocket_service.service.rabbit_mq_event_recorder.RabbitMqEventPublisher;
import jakarta.websocket.server.PathParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Controller// Controller for Websocket , RestController for RestAPI
public class WebsocketController {

    private final OperationalTransformService operationalTransformService;
    private final RedisMessagePublisher redisMessagePublisher;
    private final RabbitMqEventPublisher rabbitMqEventPublisher;

    @MessageMapping("/workspace/{workspaceId}/code")
    public void handleCodeChange(@Payload CodeOperation code, @DestinationVariable UUID workspaceId){   //DesinationVariable instead of Path Variable

        log.info("Code change received | workspace: {} | op: {} | position: {}",
                workspaceId,
                code.getOperationType(),
                code.getPosition()
        );

        CodeOperation transformedOp = operationalTransformService.processOperation(workspaceId,code);

        redisMessagePublisher.publish("nexis:workspace:" + workspaceId,transformedOp);

        rabbitMqEventPublisher.publishCodeEvent(transformedOp);

    }
}
