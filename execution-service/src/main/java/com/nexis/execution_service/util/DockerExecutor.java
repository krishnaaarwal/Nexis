package com.nexis.execution_service.util;

import com.nexis.execution_service.config.type.CodeLanguage;
import com.nexis.execution_service.entity.ExecutionResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DockerExecutor {

    public ExecutionResult execute(UUID jobId , CodeLanguage codeLanguage , String code){
        return null;
    }

    private  String pullImage(CodeLanguage codeLanguage){
        return switch (codeLanguage) {
            case PYTHON -> "python:3.11.15-trixie";
            case JAVA -> "openjdk"
        }
    }
}
