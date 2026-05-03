package com.nexis.execution_service.dto;

import com.nexis.execution_service.type.CodeLanguage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JobRequestDto {
    private UUID userId;

    private UUID workspaceId;

    private CodeLanguage codeLanguage;

    private String code;
}
