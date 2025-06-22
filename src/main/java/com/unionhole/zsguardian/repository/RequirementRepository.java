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

import com.unionhole.zsguardian.entity.Requirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequirementRepository extends JpaRepository<Requirement, Long> {
    List<Requirement> findByTitleContaining(String title);
    List<Requirement> findByCreatorId(Long creatorId);
    List<Requirement> findByCreatorIdAndTitleContaining(Long creatorId, String title);
    @Query("SELECT r.code FROM Requirement r WHERE r.code LIKE :prefixLike")
    List<String> findCodesByPrefix(@Param("prefixLike") String prefixLike);
    boolean existsByCode(String code);
}