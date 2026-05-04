package com.nexis.execution_service.service.impl;

import com.nexis.execution_service.config.RabbitMqConfig;
import com.nexis.execution_service.dto.JobMessageDto;
import com.nexis.execution_service.dto.JobRequestDto;
import com.nexis.execution_service.dto.JobResponseDto;
import com.nexis.execution_service.entity.ExecutionJob;
import com.nexis.execution_service.repository.ExecutionRepository;
import com.nexis.execution_service.service.ExecutionService;
import com.nexis.execution_service.config.type.StatusType;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

//Security checks has to implement too, like the user submit job belongs to Workspace or not

@Service
@RequiredArgsConstructor
public class ExecutionServiceImplementation implements ExecutionService {

    private final ExecutionRepository executionRepository;
    private final RabbitTemplate rabbitTemplate;

    @Override
    @Transactional
    public JobResponseDto submitJob(JobRequestDto requestDto) {

        ExecutionJob job = ExecutionJob.builder()
                .userId(requestDto.getUserId())
                .workspaceId(requestDto.getWorkspaceId())
                .codeLanguage(requestDto.getCodeLanguage())
                .code(requestDto.getCode())
                .status(StatusType.QUEUED)
                .createdAt(LocalDateTime.now())
                .build();

        JobMessageDto messageDto = new JobMessageDto(job.getId(),job.getCodeLanguage());

        executionRepository.save(job);
        rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE_NAME,RabbitMqConfig.ROUTING_KEY,messageDto);

        return new JobResponseDto(job.getId(),job.getStatus(),job.getOutput(), job.getError(),null);
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

        long durationMs = 0;
        if(job.getStartedAt() != null && job.getCompletedAt() != null){
            durationMs = Duration.between(job.getStartedAt(), job.getCompletedAt()).toMillis();
        }
        return new JobResponseDto(job.getId(),job.getStatus(),job.getOutput(), job.getError(),durationMs);
    }

}
