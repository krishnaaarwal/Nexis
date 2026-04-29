package com.nexis.websocket_service.payload;

import com.nexis.websocket_service.config.type.PresenceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PresencePayload {
    private UUID userId;
    private PresenceType presenceType;
}
