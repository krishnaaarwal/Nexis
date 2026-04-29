package com.nexis.websocket_service.payload;

import lombok.Data;
import java.util.UUID;

@Data
public class TypingPayload {
    private UUID userId;
    private boolean isTyping;
}
