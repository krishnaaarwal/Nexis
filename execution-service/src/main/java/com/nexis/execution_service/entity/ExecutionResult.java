package com.nexis.execution_service.entity;

import com.nexis.execution_service.config.type.StatusType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExecutionResult {
    private UUID id;
    private StatusType statusType;
    private String output; // STRICTLY for STDOUT
    private String error;  // STRICTLY for STDERR
}
