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
package com.unionhole.zsguardian.entity;

import com.unionhole.zsguardian.model.TestCaseNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.Data;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "test_case")
@TypeDef(name = "json", typeClass = JsonType.class)
@Data
public class TestCase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requirement_id")
    private Requirement requirement;
    
    private Long parentId;
    private String type; // "folder" 或 "case"
    private Integer level;
    private Integer sortOrder;
    
    private String title;
    private String priority;
    private String precondition;

    private String expectedResult;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(columnDefinition = "TEXT")
    private String configuration;  // Stores API configuration as JSON
    
    @Column(columnDefinition = "TEXT")
    private String steps;  // Stores test steps as JSON
    
    @Column
    private String status;  // PENDING, IN_PROGRESS, COMPLETED, etc.
    
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @Column(name = "creator_id")
    private Long creatorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bug_id")
    private Bug bug;

    private String category;  // 添加此字段

    @Type(type = "json")
    @Column(columnDefinition = "json")
    private List<TestCaseNode> treeData;


}