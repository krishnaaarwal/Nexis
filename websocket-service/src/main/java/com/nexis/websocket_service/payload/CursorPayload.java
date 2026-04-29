package com.nexis.websocket_service.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CursorPayload {
    private UUID userId;
    private Integer line;
    private Integer characterIndex;
}
