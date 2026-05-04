package com.nexis.execution_service.dto;

import com.nexis.execution_service.config.type.CodeLanguage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JobMessageDto {
    private UUID jobId;
    private CodeLanguage codeLanguage;
}
