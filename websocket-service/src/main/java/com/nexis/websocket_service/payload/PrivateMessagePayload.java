package com.nexis.websocket_service.payload;

import com.nexis.websocket_service.config.type.MessageType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PrivateMessagePayload {
    private UUID senderId;
    private UUID receiverId;
    private MessageType messageType;
    private String content;
}
