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

import com.unionhole.zsguardian.dto.RequirementDTO;
import com.unionhole.zsguardian.entity.Requirement;
import com.unionhole.zsguardian.entity.TestCase;
import com.unionhole.zsguardian.entity.User;
import com.unionhole.zsguardian.repository.RequirementRepository;
import com.unionhole.zsguardian.exception.UnauthorizedException;
import com.unionhole.zsguardian.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RequirementService {
    @Autowired
    private RequirementRepository requirementRepository;



    @Autowired
    private TestCaseService testCaseService;  // 需要注入 TestCaseService

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private UserService userService;  // 注入UserService

    public Requirement createRequirement(RequirementDTO dto) {
        if (dto.getCode() != null && requirementRepository.existsByCode(dto.getCode())) {
            throw new RuntimeException("需求编号已存在: " + dto.getCode());
        }
        if (dto.getCode() == null || dto.getCode().trim().isEmpty()) {
            String code = this.generateRequirementCode();
            dto.setCode(code);
        }
        Requirement requirement = new Requirement();
        requirement.setTitle(dto.getTitle());
        requirement.setDescription(dto.getDescription());
        requirement.setDocumentPath(dto.getDocumentUrl());
        requirement.setCreateTime(LocalDateTime.now());
        requirement.setUpdateTime(LocalDateTime.now());
        requirement.setCreatorId(dto.getCreatorId());
        requirement.setUpdatorId(dto.getUpdatorId());
        requirement.setCode(dto.getCode());
        requirement = requirementRepository.save(requirement);
        return requirement;
    }

    public List<Requirement> getAllRequirements() {
        return requirementRepository.findAll();
    }



    public List<RequirementDTO> getRequirements(String title) {
        List<Requirement> requirements;
        if (StringUtils.hasText(title)) {
            requirements = requirementRepository.findByTitleContaining(title);
        } else {
            requirements = requirementRepository.findAll();
        }
        return requirements.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private RequirementDTO convertToDTO(Requirement requirement) {
        String creatorName = "";
        String updatorName = "";
        if (requirement.getCreatorId() != null) {
            User creator = userService.findById(requirement.getCreatorId());
            creatorName = creator != null ? creator.getUsername() : "";
        }
        if (requirement.getUpdatorId() != null) {
            User updator = userService.findById(requirement.getUpdatorId());
            updatorName = updator != null ? updator.getUsername() : "";
        }

        return new RequirementDTO(
                requirement.getId(),
                requirement.getTitle(),
                requirement.getDescription(),
                requirement.getDocumentPath(),
                requirement.getCreateTime(),
                requirement.getCreatorId(),
                creatorName,
                requirement.getUpdateTime(),
                requirement.getUpdatorId(),
                updatorName,
                requirement.getCode()

        );
    }

    public Requirement findById(Long id) {
        return requirementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Requirement not found with id: " + id));
    }

    public List<TestCase> getAssociatedTestCases(Long requirementId) {
        // 获取需求关联的测试用例
        return testCaseService.findByRequirementId(requirementId);
    }

    public void deleteRequirement(Long id) {
        // 检查权限
        if (!authorizationService.hasPermission("REQUIREMENT", id, "DELETE")) {
            throw new UnauthorizedException("没有权限删除此需求");
        }

        // 检查是否存在关联的测试用例
        List<TestCase> associatedTestCases = getAssociatedTestCases(id);
        if (!associatedTestCases.isEmpty()) {
            throw new RuntimeException("该需求存在关联的测试用例，无法删除");
        }
        requirementRepository.deleteById(id);
    }

    public Requirement updateRequirement(Long id, RequirementDTO dto) {
        // 检查权限
        if (!authorizationService.hasPermission("REQUIREMENT", id, "UPDATE")) {
            throw new UnauthorizedException("没有权限修改此需求");
        }
        Requirement requirement = findById(id);
        if (dto.getCode() != null && !dto.getCode().equals(requirement.getCode()) && requirementRepository.existsByCode(dto.getCode())) {
            throw new UnauthorizedException("需求编号已存在: " + dto.getCode());
        }


        requirement.setTitle(dto.getTitle());
        requirement.setDescription(dto.getDescription());
        requirement.setDocumentPath(dto.getDocumentUrl());
        requirement.setUpdateTime(LocalDateTime.now());
        Long userId= UserHolder.getUser().getId();
        requirement.setUpdatorId(userId);
        requirement.setCode(dto.getCode());
        return requirementRepository.save(requirement);
    }

    // 生成唯一需求编号，格式 REQ-yyMMddNNN
    public String generateRequirementCode() {
        String prefix = "REQ-";
        String datePart = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd"));
        // 查询今天已有的最大编号
        String todayPrefix = prefix + datePart;
        List<String> codes = requirementRepository.findCodesByPrefix(todayPrefix + "%");
        int max = 0;
        for (String code : codes) {
            if (code != null && code.startsWith(todayPrefix)) {
                String nnn = code.substring(todayPrefix.length());
                try {
                    int num = Integer.parseInt(nnn);
                    if (num > max) {max = num;}
                } catch (Exception ignored) {}
            }
        }
        String nnn = String.format("%03d", max + 1);
        return todayPrefix + nnn;
    }

}