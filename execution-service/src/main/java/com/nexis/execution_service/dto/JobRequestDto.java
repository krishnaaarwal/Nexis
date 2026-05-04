package com.nexis.execution_service.dto;

import com.nexis.execution_service.config.type.CodeLanguage;
import lombok.*;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class JobRequestDto {
    private UUID userId;

    private UUID workspaceId;

    private CodeLanguage codeLanguage;

    private String code;
}
