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
package com.unionhole.zsguardian.repository;

import com.unionhole.zsguardian.entity.TestCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Transactional
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {
    List<TestCase> findByParentId(Long parentId);
    List<TestCase> findByRequirementId(Long requirementId);
    List<TestCase> findByBugId(Long bugId);


    long countByStatus(String status);

    @Query(value = "SELECT tc FROM TestCase tc WHERE tc.requirement.id = :requirementId",
           countQuery = "SELECT COUNT(tc) FROM TestCase tc WHERE tc.requirement.id = :requirementId")
    Page<TestCase> findByRequirementId(@Param("requirementId") Long requirementId, Pageable pageable);

    @Modifying
    @Transactional
    void deleteByRequirementId(Long requirementId);


}