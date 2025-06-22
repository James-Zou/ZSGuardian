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

import com.unionhole.zsguardian.dto.BugDTO;
import com.unionhole.zsguardian.model.TestCaseNode;
import com.unionhole.zsguardian.service.BugService;
import com.unionhole.zsguardian.service.DeepSeekService;
import com.unionhole.zsguardian.service.TestCaseService;
import com.unionhole.zsguardian.entity.Bug;
import com.unionhole.zsguardian.entity.TestCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unionhole.zsguardian.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/bugs")
@Slf4j
public class BugController {
    @Autowired
    private BugService bugService;
    
    @Autowired
    private DeepSeekService deepSeekService;
    
    @Autowired
    private TestCaseService testCaseService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @GetMapping
    public ResponseEntity<List<BugDTO>> getBugs(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Long requirementId) {
        return ResponseEntity.ok(bugService.getBugs(title, requirementId));
    }
    
    @PostMapping
    public ResponseEntity<BugDTO> createBug(@RequestBody BugDTO dto) {
        return ResponseEntity.ok(bugService.createBug(dto));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<BugDTO> updateBug(
            @PathVariable Long id, 
            @RequestBody BugDTO dto) {
        return ResponseEntity.ok(bugService.updateBug(id, dto));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBug(@PathVariable Long id) {
        bugService.deleteBug(id);
        return ResponseEntity.ok().build();
    }


    @PostMapping("/{id}/generate-test-cases")
    public ResponseEntity<?> generateTestCasesForBug(@PathVariable Long id) {
        try {

            List<TestCase> testCaseList=testCaseService.findByBugId(id);
            if(!CollectionUtils.isEmpty(testCaseList)){
                return ResponseEntity.badRequest().body(Collections.singletonMap(
                        "message", "此需求已存在关联测试用例，请先解除关联，才能重新进行AI解析"
                ));
            }

            // 获取需求
            Bug bug = bugService.findById(id);



            // 校验bug状态
            if ("FIXED".equals(bug.getStatus()) || "CLOSED".equals(bug.getStatus())) {
                return ResponseEntity.badRequest().body(Collections.singletonMap(
                        "message", "已完成的bug无法进行AI分析"
                ));
            }


            // 校验bug描述字数
            if (bug.getDescription() == null || bug.getDescription().length() < 10) {
                return ResponseEntity.badRequest().body(Collections.singletonMap(
                        "message", "bug描述太少无法进行AI分析，请完善需求内容，至少大于10字"
                ));
            }

            // 调用 DeepSeek 生成测试用例
            String aiResponse = deepSeekService.generateTestOutline(bug.getTitle()+bug.getDescription());
            List<TestCaseNode> treeData = objectMapper.readValue(aiResponse, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, TestCaseNode.class));

            // 创建测试用例
            TestCase testCase = new TestCase();
            testCase.setTitle(bug.getTitle() + " - AI生成的测试用例");
            testCase.setBug(bug);
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
            log.error("Failed to generate test cases for bug: " + id, e);
            return ResponseEntity.status(500).body(Collections.singletonMap(
                    "message", "生成测试用例失败：" + e.getMessage()
            ));
        }
    }
} 