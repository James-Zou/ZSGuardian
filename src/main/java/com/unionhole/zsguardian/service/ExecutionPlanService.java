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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thoughtworks.xstream.core.SecurityUtils;
import com.unionhole.zsguardian.dto.*;
import com.unionhole.zsguardian.entity.*;
import com.unionhole.zsguardian.model.ExecutionCaseUpdateRequest;
import com.unionhole.zsguardian.model.ExecutionPlanCompleteRequest;
import com.unionhole.zsguardian.model.ExecutionPlanCreateRequest;
import com.unionhole.zsguardian.model.ExecutionPlanUpdateRequest;
import com.unionhole.zsguardian.repository.ExecutionCaseRepository;
import com.unionhole.zsguardian.repository.ExecutionPlanRepository;
import com.unionhole.zsguardian.repository.UserRepository;
import com.unionhole.zsguardian.utils.UserHolder;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import java.time.Duration;

@Service
@Slf4j
public class ExecutionPlanService {
    @Autowired
    private ExecutionPlanRepository executionPlanRepository;
    
    @Autowired
    private ExecutionCaseRepository executionCaseRepository;

    @Autowired
    private TestCaseService testCaseService;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
  
    
    @Transactional(readOnly = true)
    public Page<ExecutionPlan> findAll(Pageable pageable, String status, Long testCaseId, String creatorName, String executorName, String testCaseTitle) {
        // 构建动态查询
        Specification<ExecutionPlan> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 状态过滤
            if (status != null && !status.isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            
            // 测试用例ID过滤
            if (testCaseId != null) {
                predicates.add(cb.equal(root.get("testCase").get("id"), testCaseId));
            }
            
            // 创建人名称过滤
            if (creatorName != null && !creatorName.isEmpty()) {
                Join<ExecutionPlan, User> creatorJoin = root.join("creator");
                predicates.add(cb.like(creatorJoin.get("username"), "%" + creatorName + "%"));
            }
            
            // 执行人名称过滤
            if (executorName != null && !executorName.isEmpty()) {
                Join<ExecutionPlan, User> executorJoin = root.join("executor");
                predicates.add(cb.like(executorJoin.get("username"), "%" + executorName + "%"));
            }
            
            // 测试用例名称模糊查询
            if (testCaseTitle != null && !testCaseTitle.isEmpty()) {
                Join<ExecutionPlan, TestCase> testCaseJoin = root.join("testCase");
                predicates.add(cb.like(testCaseJoin.get("title"), "%" + testCaseTitle + "%"));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        return executionPlanRepository.findAll(spec, pageable);
    }
    
    @Transactional(readOnly = true)
    public ExecutionPlan findById(Long id) {
        return executionPlanRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Execution plan not found"));
    }
    
    @Transactional
    public void updateExecutionCase(Long planId, Long caseId, ExecutionCaseUpdateRequest request) {
        ExecutionCase executionCase = executionCaseRepository.findByExecutionPlanIdAndId(planId, caseId)
            .orElseThrow(() -> new RuntimeException("Execution case not found"));
        
        executionCase.setResult(request.getResult());
        executionCase.setActualResult(request.getActualResult());
        executionCase.setBugDescription(request.getBugDescription());
        executionCase.setStatus("COMPLETED");
        
        executionCaseRepository.save(executionCase);
        
        // 更新执行计划状态
        updateExecutionPlanStatus(planId);
    }
    
    @Transactional
    public void completeExecutionPlan(Long id, ExecutionPlanCompleteRequest request) {
        ExecutionPlan plan = findById(id);
        
        // 更新统计信息
        plan.setStatus("COMPLETED");
        plan.setUpdateTime(LocalDateTime.now());
        
        // 可以在这里保存统计信息到单独的统计表中
        
        executionPlanRepository.save(plan);
    }
    
    private void updateExecutionPlanStatus(Long planId) {
        ExecutionPlan plan = findById(planId);
        List<ExecutionCase> testCases = plan.getCases().stream()
            .filter(c -> "case".equals(c.getType()))
            .collect(Collectors.toList());
            
        long completedCases = testCases.stream()
            .filter(c -> "COMPLETED".equals(c.getStatus()))
            .count();
        
        if (completedCases == testCases.size()) {
            plan.setStatus("COMPLETED");
            plan.setCompletionTime(LocalDateTime.now());
            
            // 计算通过率和评分
            long passedCases = testCases.stream()
                .filter(c -> "PASS".equals(c.getResult()))
                .count();
            
            double passRate = (double) passedCases / testCases.size() * 100;
            
            // 设置评分
            if (passRate == 100) {
                plan.setExecutionScore("完美");
            } else if (passRate > 90) {
                plan.setExecutionScore("优秀");
            } else if (passRate > 80) {
                plan.setExecutionScore("良好");
            } else if (passRate >= 60) {
                plan.setExecutionScore("一般");
            } else {
                plan.setExecutionScore("不及格");
            }
        } else if (completedCases > 0) {
            plan.setStatus("IN_PROGRESS");
        }
        
        plan.setUpdateTime(LocalDateTime.now());
        executionPlanRepository.save(plan);
    }

    @Transactional
    public ExecutionPlan createExecutionPlan(ExecutionPlanCreateRequest request) {
        // 获取测试用例
        TestCase testCase = testCaseService.findById(request.getTestCaseId());
        if (testCase == null) {
            throw new RuntimeException("Test case not found");
        }

        // 获取当前用户
        User currentUser = userRepository.findById(UserHolder.getUser().getId())
            .orElseThrow(() -> new RuntimeException("User not found"));

        // 创建执行计划
        ExecutionPlan plan = new ExecutionPlan();
        plan.setTestCase(testCase);
        plan.setTitle(request.getTitle() != null ? request.getTitle() : testCase.getTitle());
        plan.setStatus("PENDING");
        plan.setCreateTime(LocalDateTime.now());
        plan.setUpdateTime(LocalDateTime.now());
        plan.setCreator(currentUser);
        plan.setExecutor(currentUser);

        // 保存执行计划
        plan = executionPlanRepository.save(plan);

        // 创建执行用例
        List<ExecutionCase> executionCases = new ArrayList<>();
        if (testCase.getTreeData() != null) {
            List<Map<String, Object>> treeData = objectMapper.convertValue(testCase.getTreeData(), 
                new TypeReference<List<Map<String, Object>>>() {});
            createExecutionCases(treeData, null, plan, request.getSelectedNodes(), executionCases);
        }

        // 更新统计信息
        plan.setTotalCases((int) executionCases.stream()
            .filter(c -> "case".equals(c.getType()))
            .count());
        
        // 保存执行用例
        executionCaseRepository.saveAll(executionCases);
        plan.setCases(executionCases);

        return executionPlanRepository.save(plan);
    }



    private void createExecutionCases(
            List<Map<String, Object>> nodes,
            Long parentId,
            ExecutionPlan plan,
            List<Long> selectedNodeIds,
            List<ExecutionCase> executionCases) {
        
        if (nodes == null) {return;}

        for (Map<String, Object> node : nodes) {
            Long nodeId = Long.valueOf(String.valueOf(node.get("id")));
            
            // 如果不是选中的节点，且不是文件夹，则跳过
            if (!"folder".equals(node.get("type")) && 
                !selectedNodeIds.contains(nodeId)) {
                continue;
            }

            ExecutionCase executionCase = new ExecutionCase();
            executionCase.setExecutionPlan(plan);
            executionCase.setTitle(String.valueOf(node.get("title")));
            executionCase.setType(String.valueOf(node.get("type")));
            executionCase.setPriority(String.valueOf(node.get("priority")));
            executionCase.setPrecondition((String) node.get("precondition"));
            executionCase.setSteps((String) node.get("steps"));
            executionCase.setExpectedResult((String) node.get("expectedResult"));
            executionCase.setStatus("PENDING");
            executionCase.setParentId(parentId);
            
            executionCases.add(executionCase);

            // 递归处理子节点
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("children");
            if (children != null && !children.isEmpty()) {
                createExecutionCases(children, nodeId, plan, selectedNodeIds, executionCases);
            }
        }
    }

    @Transactional
    public void startExecutionPlan(Long id) {
        ExecutionPlan plan = findById(id);
        plan.setStatus("IN_PROGRESS");
        plan.setUpdateTime(LocalDateTime.now());
        executionPlanRepository.save(plan);
    }

    @Transactional
    public void abandonExecutionPlan(Long id) {
        ExecutionPlan plan = findById(id);
        plan.setStatus("ABANDONED");
        plan.setUpdateTime(LocalDateTime.now());
        executionPlanRepository.save(plan);
    }

    @Transactional
    public void deleteExecutionPlan(Long id) {
        ExecutionPlan plan = findById(id);
        executionPlanRepository.delete(plan);
    }

    @Transactional
    public void updateExecutionPlan(Long id, ExecutionPlanUpdateRequest request) {
        ExecutionPlan plan = findById(id);
        
        if (request.getTitle() != null) {
            plan.setTitle(request.getTitle());
        }
        
        if (request.getExecutorId() != null) {
            User executor = userRepository.findById(request.getExecutorId())
                .orElseThrow(() -> new RuntimeException("Executor not found"));
            plan.setExecutor(executor);
        }
        
        plan.setUpdateTime(LocalDateTime.now());
        executionPlanRepository.save(plan);
    }

    public ExecutionPlanDTO convertToDTO(ExecutionPlan plan) {
        ExecutionPlanDTO dto = new ExecutionPlanDTO();
        dto.setId(plan.getId());
        dto.setTitle(plan.getTitle());
        dto.setStatus(plan.getStatus());
        dto.setCreateTime(plan.getCreateTime());
        dto.setCompletionTime(plan.getCompletionTime());
        dto.setExecutionScore(plan.getExecutionScore());
        
        // 计算执行时长
        if (plan.getCompletionTime() != null) {
            Duration duration = Duration.between(plan.getCreateTime(), plan.getCompletionTime());
            long hours = duration.toHours();
            long minutes = (duration.toMinutes() % 60);
            dto.setExecutionDuration(String.format("%d小时%d分钟", hours, minutes));
        }
        
        // 设置创建人和执行人信息
        if (plan.getCreator() != null) {
            dto.setCreatorName(plan.getCreator().getUsername());
            dto.setCreatorId(plan.getCreator().getId());
        }
        if (plan.getExecutor() != null) {
            dto.setExecutorName(plan.getExecutor().getUsername());
            dto.setExecutorId(plan.getExecutor().getId());
        }
        
        // 转换用例列表
        if (plan.getCases() != null) {
            dto.setCases(plan.getCases().stream()
                .map(this::convertToCaseDTO)
                .collect(Collectors.toList()));
        }
        
        return dto;
    }

    private ExecutionCaseDTO convertToCaseDTO(ExecutionCase executionCase) {
        ExecutionCaseDTO dto = new ExecutionCaseDTO();
        BeanUtils.copyProperties(executionCase, dto);
        return dto;
    }
}