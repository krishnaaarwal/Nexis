package com.nexis.execution_service.service;

import com.nexis.execution_service.dto.JobRequestDto;
import com.nexis.execution_service.dto.JobResponseDto;

import java.util.UUID;

public interface ExecutionService {
    JobResponseDto submitJob(JobRequestDto requestDto);

    Void killJob(UUID jobId);

    JobResponseDto getJobStatus(UUID jobId);
}
