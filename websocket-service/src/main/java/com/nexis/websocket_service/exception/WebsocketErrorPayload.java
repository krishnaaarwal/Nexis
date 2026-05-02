package com.nexis.websocket_service.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WebsocketErrorPayload {
    private String type;      // e.g., "VALIDATION_ERROR", "INTERNAL_ERROR"
    private String message;   // Human-readable description
    private String timestamp; // ISO-8601 string
}