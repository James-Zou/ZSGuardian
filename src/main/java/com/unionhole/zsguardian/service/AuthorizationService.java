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

import com.unionhole.zsguardian.entity.Requirement;
import com.unionhole.zsguardian.entity.TestCase;
import com.unionhole.zsguardian.entity.User;
import com.unionhole.zsguardian.repository.RequirementRepository;
import com.unionhole.zsguardian.repository.TestCaseRepository;
import com.unionhole.zsguardian.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

    @Autowired
    private RequirementRepository requirementRepository;

    @Autowired
    private TestCaseRepository testCaseRepository;


    public boolean hasPermission(String resourceType, Long resourceId, String operation) {
        // 如果是管理员，直接返回true
        if (isAdmin()) {
            return true;
        }

        Long currentUserId = getCurrentUserId();

        // 根据资源类型判断权限
        switch (resourceType) {
            case "REQUIREMENT":
                return hasRequirementPermission(resourceId, currentUserId, operation);
            case "TEST_CASE":
                return hasTestCasePermission(resourceId, currentUserId, operation);
            // 可以添加其他资源类型的权限判断
            default:
                return false;
        }
    }

    public boolean isAdmin() {
        User currentUser = UserHolder.getUser();
        return currentUser != null && currentUser.hasRole("ADMIN");
    }

    public boolean hasRole(String roleCode) {
        User currentUser = UserHolder.getUser();
        return currentUser != null && currentUser.hasRole(roleCode);
    }

    public Long getCurrentUserId() {
        User currentUser = UserHolder.getUser();
        return currentUser != null ? currentUser.getId() : null;
    }

    private boolean hasRequirementPermission(Long id, Long userId, String operation) {
        // 检查是否是需求创建人
        Requirement requirement=findRequirementById(id);
        return requirement != null && requirement.getCreatorId().equals(userId);
    }

    private boolean hasTestCasePermission(Long id, Long userId, String operation) {
        // 检查是否是测试用例创建人
        TestCase testCase=findTestCaseById(id);
        return testCase != null && testCase.getCreatorId().equals(userId);
    }

    public Requirement findRequirementById(Long id) {
        return requirementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Requirement not found with id: " + id));
    }

    public TestCase findTestCaseById(Long id) {
        return testCaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("testcase not found with id: " + id));
    }



} 