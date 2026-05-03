package com.nexis.execution_service.contoller;

import com.nexis.execution_service.dto.JobRequestDto;
import com.nexis.execution_service.dto.JobResponseDto;
import com.nexis.execution_service.service.ExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/execute")
public class ExecutionController {

    private final ExecutionService executionService;

    @PostMapping("/run")
    public ResponseEntity<JobResponseDto> submitJob(@RequestBody JobRequestDto requestDto){
        return ResponseEntity.ok(executionService.submitJob(requestDto));
    }

    @PostMapping("/kill/{jobId}")
    public ResponseEntity<Void> killJob(@PathVariable UUID jobId){
        return ResponseEntity.ok(executionService.killJob(jobId));
    }

    @GetMapping("/status/{jobId}")
    public ResponseEntity<JobResponseDto> getJobStatus(@PathVariable UUID jobId){
        return ResponseEntity.ok(executionService.getJobStatus(jobId));
    }
}
