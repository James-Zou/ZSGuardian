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

import com.unionhole.zsguardian.dto.TestCaseDTO;
import com.unionhole.zsguardian.entity.ExecutionPlan;
import com.unionhole.zsguardian.entity.Requirement;
import com.unionhole.zsguardian.entity.TestCase;
import com.unionhole.zsguardian.entity.Bug;
import com.unionhole.zsguardian.exception.UnauthorizedException;
import com.unionhole.zsguardian.model.TestCaseTreeNode;
import com.unionhole.zsguardian.repository.RequirementRepository;
import com.unionhole.zsguardian.repository.TestCaseRepository;
import com.unionhole.zsguardian.repository.BugRepository;
import com.unionhole.zsguardian.repository.ExecutionPlanRepository;
import com.unionhole.zsguardian.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Objects;
import java.util.ArrayList;

@Service
@Slf4j
public class TestCaseService {
    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private RequirementRepository requirementRepository;


    @Autowired
    private BugRepository bugRepository;

    @Autowired
    private AuthorizationService authorizationService;



    @Autowired
    private ExecutionPlanRepository executionPlanRepository;

    /**
     * 创建测试用例节点
     */
    @Transactional
    public TestCase createTestCase(TestCaseDTO dto) {
        TestCase testCase = new TestCase();
        testCase.setTitle(dto.getTitle());
        testCase.setDescription(dto.getDescription());
        testCase.setPriority(dto.getPriority());
        testCase.setPrecondition(dto.getPrecondition());
        testCase.setSteps(dto.getSteps());
        testCase.setExpectedResult(dto.getExpectedResult());
        testCase.setCreateTime(LocalDateTime.now());
        testCase.setCreatorId(UserHolder.getUser().getId());


        // 处理需求关联
        if (dto.getRequirementId() != null) {
            Requirement requirement = requirementRepository.findById(dto.getRequirementId())
                    .orElseThrow(() -> new EntityNotFoundException("Requirement not found"));
            testCase.setRequirement(requirement);
        }

        // 处理 Bug 关联
        if (dto.getBugId() != null) {
            Bug bug = bugRepository.findById(dto.getBugId())
                    .orElseThrow(() -> new EntityNotFoundException("Bug not found"));
            testCase.setBug(bug);
        }

        return testCaseRepository.save(testCase);
    }

    /**
     * 更新测试用例
     */
    @Transactional
    public TestCase updateTestCase(Long id, TestCaseDTO dto) {
        TestCase testCase = testCaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("测试用例不存在"));

        // 根节点只能更新基本信息
        if (testCase.getParentId() == null) {
            testCase.setTitle(dto.getTitle());
            testCase.setDescription(dto.getDescription());
        } else {
            // 非根节点可以更新所有信息
            testCase.setTitle(dto.getTitle());
            testCase.setType(dto.getType());
            testCase.setPriority(dto.getPriority());
            testCase.setPrecondition(dto.getPrecondition());
            testCase.setSteps(dto.getSteps());
            testCase.setExpectedResult(dto.getExpectedResult());
            testCase.setDescription(dto.getDescription());
        }

        testCase.setUpdateTime(LocalDateTime.now());
        return testCaseRepository.save(testCase);
    }

    /**
     * 删除测试用例
     */
    @Transactional
    public void deleteTestCase(Long id) {
        // 检查权限
        if (!authorizationService.hasPermission("TEST_CASE", id, "DELETE")) {
            throw new UnauthorizedException("没有权限删除此测试用例");
        }
        
        // 检查是否存在关联的执行计划
        List<ExecutionPlan> associatedExecutions = getAssociatedExecutions(id);
        if (!associatedExecutions.isEmpty()) {
            throw new RuntimeException("该测试用例存在关联的执行计划，无法删除");
        }
        
        testCaseRepository.deleteById(id);
    }

    /**
     * 移动节点
     */
    @Transactional
    public void moveNode(Long nodeId, Long newParentId) {
        TestCase node = testCaseRepository.findById(nodeId)
                .orElseThrow(() -> new RuntimeException("节点不存在"));

        // 检查是否为根节点
        if (node.getParentId() == null) {
            throw new RuntimeException("根节点不能移动");
        }

        // 检查新父节点是否存在
        TestCase newParent = testCaseRepository.findById(newParentId)
                .orElseThrow(() -> new RuntimeException("目标父节点不存在"));

        // 检查是否移动到自己的子节点下
        if (isDescendant(newParentId, nodeId)) {
            throw new RuntimeException("不能移动到子节点下");
        }

        // 更新节点层级
        int levelDiff = newParent.getLevel() + 1 - node.getLevel();
        updateNodeAndChildrenLevel(node, levelDiff);

        // 更新父节点引用
        node.setParentId(newParentId);
        testCaseRepository.save(node);
    }

    /**
     * 检查是否是子孙节点
     */
    private boolean isDescendant(Long nodeId, Long ancestorId) {
        TestCase node = testCaseRepository.findById(nodeId).orElse(null);
        while (node != null && node.getParentId() != null) {
            if (node.getParentId().equals(ancestorId)) {
                return true;
            }
            node = testCaseRepository.findById(node.getParentId()).orElse(null);
        }
        return false;
    }

    /**
     * 递归更新节点及其子节点的层级
     */
    private void updateNodeAndChildrenLevel(TestCase node, int levelDiff) {
        node.setLevel(node.getLevel() + levelDiff);
        testCaseRepository.save(node);

        List<TestCase> children = testCaseRepository.findByParentId(node.getId());
        for (TestCase child : children) {
            updateNodeAndChildrenLevel(child, levelDiff);
        }
    }

    public long countTotalCases() {
        return testCaseRepository.count();
    }

    public long countCasesByStatus(String status) {
        return testCaseRepository.countByStatus(status);
    }

    public List<TestCaseTreeNode> getTestCaseTree(Long rootId) {
        TestCase root = testCaseRepository.findById(rootId)
                .orElseThrow(() -> new RuntimeException("Root node not found"));

        List<TestCase> allCases = testCaseRepository.findByParentId(rootId);
        TestCaseTreeNode rootNode = new TestCaseTreeNode(root);
        rootNode.setChildren(buildTree(allCases, rootId));

        return Collections.singletonList(rootNode);
    }

    private List<TestCaseTreeNode> buildTree(List<TestCase> cases, Long parentId) {
        return cases.stream()
                .filter(c -> Objects.equals(c.getParentId(), parentId))
                .map(c -> {
                    TestCaseTreeNode node = new TestCaseTreeNode(c);
                    // 复制更多属性
                    node.setPrecondition(c.getPrecondition());
                    node.setSteps(c.getSteps());
                    node.setExpectedResult(c.getExpectedResult());
                    node.setDescription(c.getDescription());
                    node.setChildren(buildTree(cases, c.getId()));
                    return node;
                })
                .collect(Collectors.toList());
    }



    public Page<TestCase> getTestCases(Long requirementId, Pageable pageable) {
        Page<TestCase> testCases;
        if (requirementId != null) {
            testCases = testCaseRepository.findByRequirementId(requirementId, pageable);
        } else {
            testCases = testCaseRepository.findAll(pageable);
        }

        // 确保关联的数据被加载
        testCases.getContent().forEach(testCase -> {
            if (testCase.getRequirement() != null) {
                testCase.getRequirement().getId(); // 触发懒加载
            }
            if (testCase.getBug() != null) {
                testCase.getBug().getId(); // 触发懒加载
            }
        });

        return testCases;
    }

    public void deleteTestCasesByRequirement(Long requirementId) {
        testCaseRepository.deleteByRequirementId(requirementId);
    }

    public List<TestCase> createTestCasesFromAIResponse(String aiResponse, Requirement requirement) {
        List<TestCase> testCases = new ArrayList<>();

        // 按行分割 AI 响应
        String[] lines = aiResponse.split("\n");
        for (String line : lines) {
            line = line.trim();
            // 忽略空行
            if (line.isEmpty()) {
                continue;
            }

            // 创建测试用例
            TestCase testCase = new TestCase();
            testCase.setTitle(line);
            testCase.setRequirement(requirement);
            testCase.setPriority("MEDIUM");  // 默认优先级
            testCase.setCreateTime(LocalDateTime.now());
            testCase.setUpdateTime(LocalDateTime.now());

            testCases.add(testCaseRepository.save(testCase));
        }

        return testCases;
    }

    public List<TestCase> findByRequirementId(Long requirementId) {
        return testCaseRepository.findByRequirementId(requirementId);
    }

    public List<TestCase> findByBugId(Long bugId) {
        return testCaseRepository.findByBugId(bugId);
    }

    public List<ExecutionPlan> getAssociatedExecutions(Long testCaseId) {
        return executionPlanRepository.findByTestCaseId(testCaseId);
    }



    public TestCase findById(Long id) {
        return testCaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("testcase not found with id: " + id));
    }



    @Transactional
    public TestCase save(TestCase testCase) {
        return testCaseRepository.save(testCase);
    }
}