package com.nexis.execution_service.service.impl;

import com.nexis.execution_service.dto.JobRequestDto;
import com.nexis.execution_service.dto.JobResponseDto;
import com.nexis.execution_service.entity.ExecutionJob;
import com.nexis.execution_service.repository.ExecutionRepository;
import com.nexis.execution_service.service.ExecutionService;
import com.nexis.execution_service.config.type.StatusType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

//Security checks has to implement too, like the user submit job belongs to Workspace or not

@Service
@RequiredArgsConstructor
public class ExecutionServiceImplementation implements ExecutionService {

    private final ExecutionRepository executionRepository;

    @Override
    @Transactional
    public JobResponseDto submitJob(JobRequestDto requestDto) {

        ExecutionJob job = ExecutionJob.builder()
                .userId(requestDto.getUserId())
                .workspaceId(requestDto.getWorkspaceId())
                .codeLanguage(requestDto.getCodeLanguage())
                .code(requestDto.getCode())
                .status(StatusType.QUEUED)
                .startedAt(LocalDateTime.now())
                .build();

        Long timeTaken = job.getCompletedAt().getLong() - job.getStartedAt().getLong();

        executionRepository.save(job);
        return new JobResponseDto(job.getId(),job.getStatus(),job.getOutput(), job.getError(),timeTaken);
    }

    @Override
    @Transactional
    public Void killJob(UUID jobId) {
        ExecutionJob job = executionRepository.findById(jobId).orElseThrow(()->new IllegalArgumentException("Job not found!"+jobId));

        job.setStatus(StatusType.FAILED);
        job.setCompletedAt(LocalDateTime.now());
        return null;
    }

    @Override
    @Transactional
    public JobResponseDto getJobStatus(UUID jobId) {
        ExecutionJob job = executionRepository.findById(jobId).orElseThrow(()->new IllegalArgumentException("Job not found!"+jobId));

        Long timeTaken = job.getCompletedAt().getLong() - job.getStartedAt().getLong();
        return new JobResponseDto(job.getId(),job.getStatus(),job.getOutput(), job.getError(),timeTaken);
    }

}
