/*
 * ZSGuardian - A super checking system
 * Copyright (C) 2024 James Zou
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.unionhole.zsguardian.service;

import com.unionhole.zsguardian.dto.ExecutionRecordDTO;
import com.unionhole.zsguardian.entity.CaseExecution;
import com.unionhole.zsguardian.entity.ExecutionRecord;
import com.unionhole.zsguardian.entity.Requirement;
import com.unionhole.zsguardian.repository.CaseExecutionRepository;
import com.unionhole.zsguardian.repository.ExecutionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ExecutionService {
    @Autowired
    private ExecutionRepository executionRecordRepository;
    
    @Autowired
    private CaseExecutionRepository caseExecutionRepository;

    public ExecutionService() {
    }

    public ExecutionRecord startExecution(Long requirementId) {
        ExecutionRecord record = new ExecutionRecord();
        Requirement requirement=new Requirement();
        requirement.setId(requirementId);
        record.setRequirement(requirement);
        record.setStatus("IN_PROGRESS");
        record.setProgress(0);
        record.setStartTime(LocalDateTime.now());
        record.setExecutor("system");
        return executionRecordRepository.save(record);
    }
    
    public void updateCaseStatus(Long caseExecutionId, String status) {
        CaseExecution caseExecution = caseExecutionRepository.findById(caseExecutionId)
            .orElseThrow(() -> new RuntimeException("Case execution not found"));
            
        caseExecution.setStatus(status);
        caseExecution.setExecutionTime(LocalDateTime.now());
        caseExecutionRepository.save(caseExecution);
        
        // 更新执行记录进度
        updateExecutionProgress(caseExecution.getExecutionRecord().getId());
    }
    
    private void updateExecutionProgress(Long executionId) {
        List<CaseExecution> caseExecutions = caseExecutionRepository.findByExecutionRecordId(executionId);
        long completedCount = caseExecutions.stream()
            .filter(ce -> "COMPLETED".equals(ce.getStatus()) || "ABANDONED".equals(ce.getStatus()))
            .count();
            
        int progress = (int) ((completedCount * 100) / caseExecutions.size());
        
        ExecutionRecord record = executionRecordRepository.findById(executionId)
            .orElseThrow(() -> new RuntimeException("Execution record not found"));
            
        record.setProgress(progress);
        if (progress == 100) {
            record.setStatus("COMPLETED");
            record.setEndTime(LocalDateTime.now());
        }
        executionRecordRepository.save(record);
    }

    public List<ExecutionRecordDTO> getExecutions(Long requirementId, String status) {
        List<ExecutionRecord> executions;
        if (requirementId != null && status != null) {
            executions = executionRecordRepository.findByRequirementIdAndStatus(requirementId, status);
        } else if (requirementId != null) {
            executions = executionRecordRepository.findByRequirementId(requirementId);
        } else if (status != null) {
            executions = executionRecordRepository.findByStatus(status);
        } else {
            executions = executionRecordRepository.findAll();
        }
        return executions.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    private ExecutionRecordDTO convertToDTO(ExecutionRecord execution) {
        return new ExecutionRecordDTO(
            execution.getId(),
            execution.getRequirement().getId(),
            execution.getRequirement().getTitle(),
            execution.getProgress(),
            execution.getStatus(),
            execution.getStartTime()
        );
    }
}