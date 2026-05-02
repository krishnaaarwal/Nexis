package com.nexis.websocket_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.time.Instant;

@Slf4j
@ControllerAdvice
public class GlobalWebsocketExceptionHandler {
    /**
     * Catches validation errors or bad payloads sent to @MessageMapping endpoints.
     */
    @MessageExceptionHandler(IllegalArgumentException.class)
    @SendToUser("/queue/errors") // Routes exclusively to the user who caused the error
    public WebsocketErrorPayload handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("WebSocket Validation Error: {}", ex.getMessage());
        return new WebsocketErrorPayload(
                "VALIDATION_ERROR",
                ex.getMessage(),
                Instant.now().toString()
        );
    }

    /**
     * Catches anything else (e.g., Redis down, OT Math errors).
     */
    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public WebsocketErrorPayload handleGeneralException(Exception ex) {
        log.error("WebSocket Internal Error: ", ex);
        return new WebsocketErrorPayload(
                "INTERNAL_ERROR",
                "An unexpected error occurred processing your message.",
                Instant.now().toString()
        );
    }
}