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

import com.unionhole.zsguardian.dto.*;
import com.unionhole.zsguardian.entity.ExecutionPlan;
import com.unionhole.zsguardian.entity.TestCase;
import com.unionhole.zsguardian.entity.User;
import com.unionhole.zsguardian.exception.UnauthorizedException;
import com.unionhole.zsguardian.model.TestCaseNode;
import com.unionhole.zsguardian.model.TestCaseTreeNode;
import com.unionhole.zsguardian.service.TestCaseService;
import com.unionhole.zsguardian.service.BugService;
import com.unionhole.zsguardian.service.UserService;
import com.unionhole.zsguardian.service.XMindService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test-cases")
@Slf4j
public class TestCaseController {
    @Autowired
    private TestCaseService testCaseService;
    
    @Autowired
    private BugService bugService;

    @Autowired
    private UserService userService;
    
    @Autowired
    private XMindService xmindService;
    
    @PostMapping
    public TestCaseResponseDTO createTestCase(@RequestBody TestCaseDTO dto) {
        TestCase testCase = testCaseService.createTestCase(dto);
        return convertToDTO(testCase);
      
    }

    
    @GetMapping("/tree/{rootId}")
    public List<TestCaseTreeNode> getTestCaseTreeByRootId(@PathVariable Long rootId) {
        return testCaseService.getTestCaseTree(rootId);
    }
    

    
    @PutMapping("/{id}")
    public TestCase updateTestCase(@PathVariable Long id, @RequestBody TestCaseDTO dto) {
        return testCaseService.updateTestCase(id, dto);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTestCase(@PathVariable Long id) {
        try {
            testCaseService.deleteTestCase(id);
            return ResponseEntity.ok().build();
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(403).body(Collections.singletonMap("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<Page<TestCaseResponseDTO>> getTestCases(
            @RequestParam(required = false) Long requirementId,
            Pageable pageable) {
        Page<TestCase> testCases = testCaseService.getTestCases(requirementId, pageable);
        Page<TestCaseResponseDTO> dtoPage = testCases.map(this::convertToDTO);
        return ResponseEntity.ok(dtoPage);
    }

    private TestCaseResponseDTO convertToDTO(TestCase testCase) {
        TestCaseResponseDTO dto = new TestCaseResponseDTO();
        BeanUtils.copyProperties(testCase, dto);
        
        if (testCase.getRequirement() != null) {
            RequirementDTO reqDTO = new RequirementDTO();
            BeanUtils.copyProperties(testCase.getRequirement(), reqDTO);
            dto.setRequirement(reqDTO);
        }
        
        if (testCase.getBug() != null ) {
           BugDTO bugDTO=new BugDTO();
            BeanUtils.copyProperties(testCase.getBug(), bugDTO);
            dto.setBug(bugDTO);
        }

        if(testCase.getCreatorId()!=null){
            User creator = userService.findById(testCase.getCreatorId());
            String creatorName = creator != null ? creator.getUsername() : "";
            dto.setCreatorName(creatorName);

        }

        
        return dto;
    }

    @DeleteMapping("/requirement/{requirementId}")
    public ResponseEntity<?> deleteTestCasesByRequirement(@PathVariable Long requirementId) {
        testCaseService.deleteTestCasesByRequirement(requirementId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsDTO> getStatistics() {
        StatisticsDTO statistics = new StatisticsDTO();
        
        // 获取测试用例统计
        statistics.setTotalCases(testCaseService.countTotalCases());
        statistics.setRunningCases(testCaseService.countCasesByStatus("IN_PROGRESS"));
        statistics.setCompletedCases(testCaseService.countCasesByStatus("COMPLETED"));
        
        // 获取Bug统计
        statistics.setPendingBugs(bugService.countPendingBugs());
        
        return ResponseEntity.ok(statistics);
    }
    
    public TestCase saveTestCaseNode(@RequestBody TestCaseDTO dto) {
        if (dto.getId() != null) {
            return testCaseService.updateTestCase(dto.getId(), dto);
        } else {
            return testCaseService.createTestCase(dto);
        }
    }

    @GetMapping("/{id}/executions")
    public ResponseEntity<List<ExecutionPlan>> getAssociatedExecutions(@PathVariable Long id) {
        try {
            List<ExecutionPlan> executions = testCaseService.getAssociatedExecutions(id);
            return ResponseEntity.ok(executions);
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(403).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/{id}/tree")
    public ResponseEntity<?> getTestCaseTree(@PathVariable Long id) {
        try {
            TestCase testCase = testCaseService.findById(id);
            if (testCase == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 如果还没有树形数据，创建一个只包含根节点的树
            if (testCase.getTreeData() == null || testCase.getTreeData().isEmpty()) {
                List<TestCaseNode> initialTree = Collections.singletonList(new TestCaseNode(
                    testCase.getId(),
                    testCase.getTitle(),
                    "folder",  // 根节点类型为folder
                    testCase.getPriority(),
                    testCase.getPrecondition(),
                    testCase.getSteps(),
                    testCase.getExpectedResult(),
                    null,  // 根节点没有父节点
                    new ArrayList<>()  // 空的子节点列表
                ));
                return ResponseEntity.ok(initialTree);
            }
            
            return ResponseEntity.ok(testCase.getTreeData());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("获取测试用例树失败：" + e.getMessage());
        }
    }

    @PutMapping("/{id}/tree")
    public ResponseEntity<?> updateTestCaseTree(
            @PathVariable Long id,
            @RequestBody TestCaseTreeUpdateRequest request) {
        try {
            TestCase testCase = testCaseService.findById(id);
            if (testCase == null) {
                return ResponseEntity.notFound().build();
            }

            testCase.setTreeData(request.getTreeData());
            testCaseService.save(testCase);

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("更新测试用例树失败：" + e.getMessage());
        }
    }

    @PostMapping("/{id}/export-xmind")
    public ResponseEntity<?> exportTestCaseXMind(@PathVariable Long id) {
        try {
            TestCase testCase = testCaseService.findById(id);
            if (testCase == null) {
                return ResponseEntity.notFound().build();
            }
            // 取树形数据作为 parseResult，AI建议可为 null 或空
            Map<String, Object> parseResult = new HashMap<>();
            parseResult.put("treeData", testCase.getTreeData());
            String fileName = xmindService.generateImprovedXMind(parseResult, null);
            if (fileName == null) {
                return ResponseEntity.status(500).body(Collections.singletonMap("message", "XMind 文件生成失败"));
            }
            Map<String, Object> result = new HashMap<>();
            result.put("fileName", fileName);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Collections.singletonMap("message", e.getMessage()));
        }
    }
}