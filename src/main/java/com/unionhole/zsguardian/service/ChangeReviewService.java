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

import com.unionhole.zsguardian.dto.ChangeReviewDTO;
import com.unionhole.zsguardian.entity.Bug;
import com.unionhole.zsguardian.entity.ChangeReview;
import com.unionhole.zsguardian.entity.Requirement;
import com.unionhole.zsguardian.entity.User;
import com.unionhole.zsguardian.repository.BugRepository;
import com.unionhole.zsguardian.repository.ChangeReviewRepository;
import com.unionhole.zsguardian.repository.RequirementRepository;
import com.unionhole.zsguardian.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * @author james.zou
 * @Description:
 * @date 6/9/25 6:02 PM
 */
@Service
public class ChangeReviewService {
    @Autowired
    private ChangeReviewRepository repo;
    @Autowired
    private RequirementRepository requirementRepo;
    @Autowired
    private BugRepository bugRepo;
    @Autowired
    private UserRepository userRepo;

    public Page<ChangeReviewDTO> list(Pageable pageable) {
        return repo.findAll(pageable).map(this::toDTO);
    }

    public ChangeReviewDTO get(Long id) {
        Optional<ChangeReview> optional= repo.findById(id);
        return toDTO(optional.get());
    }

    public ChangeReviewDTO save(ChangeReviewDTO dto) {
        Optional<ChangeReview> optional= repo.findById(dto.getId());
        ChangeReview cr = dto.getId() == null ? new ChangeReview() : optional.get();
        cr.setTitle(dto.getTitle());
        cr.setChangeTime(dto.getChangeTime());
        cr.setChangeType(dto.getChangeType());
        cr.setDescription(dto.getDescription());
        cr.setDocumentUrl(dto.getDocumentUrl());
        cr.setExecutorId(dto.getExecutorId());
        cr.setAuditorId(dto.getAuditorId());
        cr.setUpdateTime(LocalDateTime.now());
        if (cr.getCreateTime() == null)
        {cr.setCreateTime(LocalDateTime.now());}
        cr.setRequirements(dto.getRequirementIds() == null ? new ArrayList<>() :
                requirementRepo.findAllById(dto.getRequirementIds()));
        cr.setBugs(dto.getBugIds() == null ? new ArrayList<>() :
                bugRepo.findAllById(dto.getBugIds()));
        repo.save(cr);
        return toDTO(cr);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    private ChangeReviewDTO toDTO(ChangeReview cr) {
        ChangeReviewDTO dto = new ChangeReviewDTO();
        dto.setId(cr.getId());
        dto.setTitle(cr.getTitle());
        dto.setChangeTime(cr.getChangeTime());
        dto.setChangeType(cr.getChangeType());
        dto.setDescription(cr.getDescription());
        dto.setDocumentUrl(cr.getDocumentUrl());
        dto.setExecutorId(cr.getExecutorId());
        dto.setAuditorId(cr.getAuditorId());
        dto.setExecutorName(userRepo.findById(cr.getExecutorId()).map(User::getUsername).orElse(""));
        dto.setAuditorName(userRepo.findById(cr.getAuditorId()).map(User::getUsername).orElse(""));
        dto.setRequirementIds(cr.getRequirements().stream().map(Requirement::getId).collect(Collectors.toList()));
        dto.setRequirementTitles(cr.getRequirements().stream().map(Requirement::getTitle).collect(Collectors.toList()));
        dto.setBugIds(cr.getBugs().stream().map(Bug::getId).collect(Collectors.toList()));
        dto.setBugTitles(cr.getBugs().stream().map(Bug::getTitle).collect(Collectors.toList()));
        return dto;
    }
}