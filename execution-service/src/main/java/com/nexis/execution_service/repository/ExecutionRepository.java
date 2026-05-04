package com.nexis.execution_service.repository;

import com.nexis.execution_service.entity.ExecutionJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@EnableJpaRepositories
public interface ExecutionRepository extends JpaRepository<ExecutionJob, UUID> {
}
