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

import com.unionhole.zsguardian.exception.UnauthorizedException;
import com.unionhole.zsguardian.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import com.unionhole.zsguardian.entity.Bug;
import com.unionhole.zsguardian.entity.User;
import com.unionhole.zsguardian.repository.BugRepository;
import com.unionhole.zsguardian.repository.RequirementRepository;
import com.unionhole.zsguardian.repository.TestCaseRepository;
import com.unionhole.zsguardian.repository.UserRepository;
import com.unionhole.zsguardian.dto.BugDTO;

import java.util.Arrays;

@Service
@Slf4j
public class BugService {
    @Autowired
    private BugRepository bugRepository;

    @Autowired
    private RequirementRepository requirementRepository;

    @Autowired
    private TestCaseRepository testCaseRepository;

    @Autowired
    private UserRepository userRepository;

    public List<BugDTO> getBugs(String title, Long requirementId) {
        List<Bug> bugs;
        if (StringUtils.hasText(title) && requirementId != null) {
            bugs = bugRepository.findByTitleContainingAndRequirementId(title, requirementId);
        } else if (StringUtils.hasText(title)) {
            bugs = bugRepository.findByTitleContaining(title);
        } else if (requirementId != null) {
            bugs = bugRepository.findByRequirementId(requirementId);
        } else {
            bugs = bugRepository.findAll();
        }
        return bugs.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional
    public BugDTO createBug(BugDTO dto) {
        if (dto.getCode() != null && bugRepository.existsByCode(dto.getCode())) {
            throw new RuntimeException("bug编号已存在: " + dto.getCode());
        }
        if (dto.getCode() == null || dto.getCode().trim().isEmpty()) {
            String code = this.generateRequirementCode();
            dto.setCode(code);
        }

        Bug bug = new Bug();
        updateBugFromDTO(bug, dto);
        bug = bugRepository.save(bug);
        return convertToDTO(bug);
    }

    @Transactional
    public BugDTO updateBug(Long id, BugDTO dto) {
        Bug bug = bugRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bug not found"));
        if (dto.getCode() != null && !dto.getCode().equals(bug.getCode()) && bugRepository.existsByCode(dto.getCode())) {
            throw new UnauthorizedException("bug编号已存在: " + dto.getCode());
        }
        updateBugFromDTO(bug, dto);
        bug = bugRepository.save(bug);
        return convertToDTO(bug);
    }

    @Transactional
    public void deleteBug(Long id) {
        bugRepository.deleteById(id);
    }

    private void updateBugFromDTO(Bug bug, BugDTO dto) {
        bug.setTitle(dto.getTitle());
        bug.setDescription(dto.getDescription());
        bug.setRootCause(dto.getRootCause());
        bug.setStatus(dto.getStatus());
        bug.setPriority(dto.getPriority());
        User tester=new User();
        tester.setId(dto.getTesterId());
        tester.setUsername(dto.getTesterName());
        bug.setTester(tester);
        User assignee=new User();
        assignee.setId(dto.getAssigneeId());
        assignee.setUsername(dto.getAssigneeName());
        bug.setAssignee(assignee);
        bug.setCreater(UserHolder.getUser());
        bug.setCode(dto.getCode());
        if(dto.getCreateTime() ==null){
            bug.setCreateTime(LocalDateTime.now());
        }
        if(dto.getUpdateTime()==null){
            bug.setUpdateTime(LocalDateTime.now());
        }

        if (dto.getRequirementId() != null) {
            bug.setRequirement(requirementRepository.findById(dto.getRequirementId())
                    .orElseThrow(() -> new RuntimeException("Requirement not found")));
        }

//        if (dto.getTestCaseIds() != null) {
//            Set<TestCase> testCases = dto.getTestCaseIds().stream()
//                    .map(id -> testCaseRepository.findById(id)
//                            .orElseThrow(() -> new RuntimeException("TestCase not found")))
//                    .collect(Collectors.toSet());
//        }
    }

    private BugDTO convertToDTO(Bug bug) {

        return new BugDTO(
                bug.getId(),
                bug.getTitle(),
                bug.getDescription(),
                bug.getCreateTime(),
                bug.getUpdateTime(),
                bug.getTester().getId(),
                bug.getTester().getUsername(),
                bug.getAssignee().getId(),
                bug.getAssignee().getUsername(),
                bug.getCreater().getId(),
                bug.getCreater().getUsername(),
                bug.getRootCause(),
                bug.getStatus(),
                bug.getPriority(),
                bug.getRequirement() != null ? bug.getRequirement().getId() : null,
                bug.getRequirement() != null ? bug.getRequirement().getTitle() : null,
                bug.getCode()

        );
    }

    public long countPendingBugs() {
        return bugRepository.countByStatusIn(Arrays.asList("PENDING", "IN_PROGRESS"));
    }

    public Bug findById(Long id) {
        return bugRepository.findById(id).orElse(null);
    }

    public Bug save(Bug bug) {
        return bugRepository.save(bug);
    }

    // 生成唯一bug编号，格式 BUG-yyMMddNNN
    public String generateRequirementCode() {
        String prefix = "BUG-";
        String datePart = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd"));
        // 查询今天已有的最大编号
        String todayPrefix = prefix + datePart;
        List<String> codes = bugRepository.findCodesByPrefix(todayPrefix + "%");
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