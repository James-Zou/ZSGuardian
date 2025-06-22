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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unionhole.zsguardian.dto.RequirementDTO;
import com.unionhole.zsguardian.entity.Requirement;
import com.unionhole.zsguardian.entity.TestCase;
import com.unionhole.zsguardian.model.TestCaseNode;
import com.unionhole.zsguardian.service.DeepSeekService;
import com.unionhole.zsguardian.service.RequirementService;
import com.unionhole.zsguardian.service.TestCaseService;
import com.unionhole.zsguardian.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

@RestController
@RequestMapping("/api/requirements")
@Slf4j
public class RequirementController {
    @Autowired
    private RequirementService requirementService;
      
    @Autowired
    private DeepSeekService deepSeekService;

    @Autowired
    private TestCaseService testCaseService;

    @Autowired
    private ObjectMapper objectMapper;


    
    @PostMapping("/save")
    public ResponseEntity<?> createRequirement(@RequestBody RequirementDTO dto) {
        try {
            log.info("Creating requirement: {}", new ObjectMapper().writeValueAsString(dto));
            Long userId=UserHolder.getUser().getId();
            dto.setCreatorId(userId);
            dto.setUpdatorId(userId);
            Requirement requirement = requirementService.createRequirement(dto);
            RequirementDTO responseDto = new RequirementDTO();
            responseDto.setCreateTime( requirement.getCreateTime());
            responseDto.setCreatorId(requirement.getCreatorId());
            responseDto.setUpdateTime(requirement.getUpdateTime());
            responseDto.setUpdatorId(requirement.getUpdatorId());
            responseDto.setCode(requirement.getCode());
            responseDto.setId(requirement.getId());
            responseDto.setTitle(requirement.getTitle());
            responseDto.setDescription(requirement.getDescription());
            responseDto.setDocumentUrl(requirement.getDocumentPath());

            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            log.error("Failed to create requirement", e);
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    @GetMapping
    public ResponseEntity<List<RequirementDTO>> getRequirements(
            @RequestParam(required = false) String title) {
        return ResponseEntity.ok(requirementService.getRequirements(title));
    }

    @GetMapping("/{id}/test-cases")
    public ResponseEntity<List<TestCase>> getAssociatedTestCases(@PathVariable Long id) {
        try {
            List<TestCase> testCases = requirementService.getAssociatedTestCases(id);
            return ResponseEntity.ok(testCases);
        } catch (Exception e) {
            log.error("Failed to get associated test cases for requirement: " + id, e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRequirement(@PathVariable Long id) {
        try {
            requirementService.deleteRequirement(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to delete requirement: " + id, e);
            return ResponseEntity.status(500).body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateRequirement(@PathVariable Long id, @RequestBody RequirementDTO dto) {
        try {
            Requirement requirement = requirementService.updateRequirement(id, dto);
            RequirementDTO responseDto = new RequirementDTO();
            responseDto.setUpdateTime( requirement.getCreateTime());
            Long userId=UserHolder.getUser().getId();
            responseDto.setUpdatorId(userId);
            responseDto.setId(requirement.getId());
            responseDto.setTitle(requirement.getTitle());
            responseDto.setDescription(requirement.getDescription());
            responseDto.setDocumentUrl(requirement.getDocumentPath());
            responseDto.setCode(requirement.getCode());
            return ResponseEntity.ok(responseDto);

        } catch (Exception e) {
            log.error("Failed to update requirement: " + id, e);
            return ResponseEntity.status(500).body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/generate-test-cases")
    public ResponseEntity<?> generateTestCasesForRequirement(@PathVariable Long id) {
        try {

            List<TestCase> testCaseList=testCaseService.findByRequirementId(id);
            if(!CollectionUtils.isEmpty(testCaseList)){
                return ResponseEntity.badRequest().body(Collections.singletonMap(
                        "message", "此需求已存在关联测试用例，请先解除关联，才能重新进行AI解析"
                ));
            }

            // 获取需求
            Requirement requirement = requirementService.findById(id);

            // 校验需求描述字数
            if (requirement.getDescription() == null || requirement.getDescription().length() < 30) {
                return ResponseEntity.badRequest().body(Collections.singletonMap(
                    "message", "需求描述太少无法进行AI分析，请完善需求内容，至少大于30字"
                ));
            }

            // 调用 DeepSeek 生成测试用例
            String aiResponse = deepSeekService.generateTestOutline(requirement.getDescription());
            List<TestCaseNode> treeData = objectMapper.readValue(aiResponse, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, TestCaseNode.class));
            
            // 创建测试用例
            TestCase testCase = new TestCase();
            testCase.setTitle(requirement.getTitle() + " - AI生成的测试用例");
            testCase.setRequirement(requirement);
            testCase.setTreeData(treeData);
            testCase.setCreatorId(UserHolder.getUser().getId());
            testCase.setCreateTime(LocalDateTime.now());
            testCase.setUpdateTime(LocalDateTime.now());
            
            // 保存测试用例
            TestCase savedTestCase = testCaseService.save(testCase);
            if(savedTestCase.getBug()!=null && savedTestCase.getBug().getCreater()!=null){
                savedTestCase.getBug().setCreater(null);
            }
            return ResponseEntity.ok(Collections.singletonMap("testCase", true));
            
        } catch (Exception e) {
            log.error("Failed to generate test cases for requirement: " + id, e);
            return ResponseEntity.status(500).body(Collections.singletonMap(
                "message", "生成测试用例失败：" + e.getMessage()
            ));
        }
    }

}