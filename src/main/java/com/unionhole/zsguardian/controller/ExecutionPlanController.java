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
package com.unionhole.zsguardian.controller;

import com.unionhole.zsguardian.dto.ExecutionCaseDTO;
import com.unionhole.zsguardian.dto.ExecutionPlanDTO;
import com.unionhole.zsguardian.entity.ExecutionCase;
import com.unionhole.zsguardian.entity.ExecutionPlan;
import com.unionhole.zsguardian.model.ExecutionCaseUpdateRequest;
import com.unionhole.zsguardian.model.ExecutionPlanCompleteRequest;
import com.unionhole.zsguardian.model.ExecutionPlanCreateRequest;
import com.unionhole.zsguardian.model.ExecutionPlanUpdateRequest;
import com.unionhole.zsguardian.service.ExecutionPlanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/execution-plans")
@Slf4j
public class ExecutionPlanController {
    @Autowired
    private ExecutionPlanService executionPlanService;
    
    @GetMapping
    public ResponseEntity<?> getExecutionPlans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long testCaseId,
            @RequestParam(required = false) String creatorName,
            @RequestParam(required = false) String executorName,
            @RequestParam(required = false) String testCaseTitle
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<ExecutionPlan> plans = executionPlanService.findAll(pageable, status, testCaseId, creatorName, executorName, testCaseTitle);
            Page<ExecutionPlanDTO> dtoPage = plans.map(this::convertToDTO);
            return ResponseEntity.ok(dtoPage);
        } catch (Exception e) {
            log.error("Failed to get execution plans: {}", e.getMessage());
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ExecutionPlanDTO> getExecutionPlan(@PathVariable Long id) {
        try {
            ExecutionPlan plan = executionPlanService.findById(id);
            ExecutionPlanDTO dto = convertToDTO(plan);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("Failed to get execution plan: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/{id}/detail")
    public ResponseEntity<ExecutionPlanDTO> getExecutionPlanDetail(@PathVariable Long id) {
        try {
            ExecutionPlan plan = executionPlanService.findById(id);
            ExecutionPlanDTO dto = convertToDTO(plan);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("Failed to get execution plan detail: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
    
    @PutMapping("/{planId}/cases/{caseId}")
    public ResponseEntity<?> updateExecutionCase(
            @PathVariable Long planId,
            @PathVariable Long caseId,
            @RequestBody ExecutionCaseUpdateRequest request) {
        try {
            executionPlanService.updateExecutionCase(planId, caseId, request);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to update execution case: {}", e.getMessage());
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
    
    @PutMapping("/{id}/complete")
    public ResponseEntity<?> completeExecutionPlan(
            @PathVariable Long id,
            @RequestBody ExecutionPlanCompleteRequest request) {
        try {
            executionPlanService.completeExecutionPlan(id, request);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to complete execution plan: {}", e.getMessage());
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<?> startExecutionPlan(@PathVariable Long id) {
        try {
            executionPlanService.startExecutionPlan(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to start execution plan: {}", e.getMessage());
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/{id}/abandon")
    public ResponseEntity<?> abandonExecutionPlan(@PathVariable Long id) {
        try {
            executionPlanService.abandonExecutionPlan(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to abandon execution plan: {}", e.getMessage());
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteExecutionPlan(@PathVariable Long id) {
        try {
            executionPlanService.deleteExecutionPlan(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to delete execution plan: {}", e.getMessage());
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
    
    @PostMapping
    public ResponseEntity<?> createExecutionPlan(@RequestBody ExecutionPlanCreateRequest request) {
        try {
            ExecutionPlan plan = executionPlanService.createExecutionPlan(request);
            return ResponseEntity.ok(convertToDTO(plan));
        } catch (Exception e) {
            log.error("Failed to create execution plan: {}", e.getMessage());
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateExecutionPlan(
            @PathVariable Long id,
            @RequestBody ExecutionPlanUpdateRequest request) {
        try {
            executionPlanService.updateExecutionPlan(id, request);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to update execution plan: {}", e.getMessage());
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
    
    private ExecutionPlanDTO convertToDTO(ExecutionPlan plan) {
        ExecutionPlanDTO dto = new ExecutionPlanDTO();
        BeanUtils.copyProperties(plan, dto);
        
        if (plan.getCases() != null) {
            List<ExecutionCaseDTO> caseDTOs = plan.getCases().stream()
                .map(this::convertToCaseDTO)
                .collect(Collectors.toList());
            dto.setCases(caseDTOs);
        }
        
        if (plan.getExecutor() != null) {
            dto.setExecutorName(plan.getExecutor().getUsername());
            dto.setExecutorId(plan.getExecutor().getId());
        }

        if(plan.getCreator()!=null){
            dto.setCreatorName(plan.getCreator().getUsername());
            dto.setCreatorId(plan.getCreator().getId());
        }

        // 计算执行时长
        if (plan.getCompletionTime() != null) {
            Duration duration = Duration.between(plan.getCreateTime(), plan.getCompletionTime());
            long hours = duration.toHours();
            long minutes = (duration.toMinutes() % 60);
            dto.setExecutionDuration(String.format("%d小时%d分钟", hours, minutes));
        }


        
        return dto;
    }
    
    private ExecutionCaseDTO convertToCaseDTO(ExecutionCase executionCase) {
        ExecutionCaseDTO dto = new ExecutionCaseDTO();
        BeanUtils.copyProperties(executionCase, dto);
        return dto;
    }
}