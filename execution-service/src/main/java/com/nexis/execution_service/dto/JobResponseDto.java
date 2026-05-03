package com.nexis.execution_service.dto;;
import com.nexis.execution_service.type.StatusType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JobResponseDto {
    private UUID id;
    private StatusType status;
    private String output; // STRICTLY for STDOUT
    private String error;  // STRICTLY for STDERR
    private Long executionDurationMs;
}